package weka.distributed.hadoop;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import weka.core.Instance;
import weka.distributed.DistributedWekaException;
import weka.distributed.RecommenderSimilarityMapTask.PartialSimilarity;

public class RecommenderSimilarityRowHadoopMapper extends
  RecommenderSimilarityHadoopMapper {

  protected Map<Integer, Map<Integer, PartialSimilarity>> m_partialMatrix = new HashMap<Integer, Map<Integer, PartialSimilarity>>();

  protected int m_matrixCellCount = 0;

  // TODO - need to set the instance weight to be the mean
  // of this user's item ratings (just in case the cosine measure
  // is being used) - this should be done in the super class

  @Override
  protected void nextInstance(Instance instance, Context context)
    throws IOException {
    m_task.newInstance(instance);

    PartialSimilarity sim = null;

    while ((sim = m_task.nextPartialSimilarity()) != null) {
      Map<Integer, PartialSimilarity> colMap = m_partialMatrix.get(sim.m_i);
      if (colMap == null) {
        colMap = new HashMap<Integer, PartialSimilarity>();
        m_partialMatrix.put(sim.m_i, colMap);
      }

      PartialSimilarity simToUpdate = colMap.get(sim.m_j);
      if (simToUpdate == null) {
        simToUpdate = new PartialSimilarity();
        simToUpdate.m_i = sim.m_i;
        simToUpdate.m_j = sim.m_j;
        colMap.put(sim.m_j, simToUpdate);

        m_matrixCellCount++;
      }

      try {
        m_task.aggregatePartialSimilarity(simToUpdate, sim);
      } catch (DistributedWekaException e) {
        throw new IOException(e);
      }
    }

    // now check on matrix density. PartialSim without any list is 166 bytes
    // when serialized (this is just an approximate for object size).
    // TODO adjust this if the Euclidean distance is being used (in which
    // case the PartialSim objects will be storing a list of co-occurring
    // ratings
    double maxSize = (m_heapSpace * 1000000.0);
    maxSize -= (0.25 * maxSize);

    if (m_matrixCellCount * 166 > maxSize) {
      // purge the matrix and start afresh
      purgeMatrixRows(context);
    }
  }

  @Override
  public void cleanup(Context context) throws IOException {
    super.cleanup(context);

    purgeMatrixRows(context);

    System.err.println("Total number of records output by mapper "
      + m_totalRecordsOutput);
  }

  protected void purgeMatrixRows(Context context) throws IOException {
    for (Map.Entry<Integer, Map<Integer, PartialSimilarity>> e : m_partialMatrix
      .entrySet()) {

      m_outKey.set(e.getKey().intValue());
      Map<Integer, PartialSimilarity> colMapForRow = e.getValue();
      byte[] b = rowToBytes(colMapForRow);
      m_outValue.set(b, 0, b.length);

      try {
        m_totalRecordsOutput++;
        context.write(m_outKey, m_outValue);
      } catch (InterruptedException ex) {
        throw new IOException(ex);
      }
    }

    m_partialMatrix.clear();
    m_matrixCellCount = 0;
  }

  protected static byte[] rowToBytes(
    Map<Integer, PartialSimilarity> colMapForRow) throws IOException {
    ObjectOutputStream p = null;
    byte[] bytes = null;

    try {
      ByteArrayOutputStream ostream = new ByteArrayOutputStream();
      OutputStream os = ostream;

      p = new ObjectOutputStream(new BufferedOutputStream(new GZIPOutputStream(
        os)));

      p.writeObject(colMapForRow);
      p.flush();
      p.close();

      bytes = ostream.toByteArray();

      p = null;
    } finally {
      if (p != null) {
        p.close();
      }
    }

    return bytes;
  }
}
