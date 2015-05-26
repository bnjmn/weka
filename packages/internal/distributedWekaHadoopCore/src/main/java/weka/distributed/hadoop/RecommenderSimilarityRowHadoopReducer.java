package weka.distributed.hadoop;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;

import weka.distributed.DistributedWekaException;
import weka.distributed.RecommenderSimilarityMapTask.PartialSimilarity;

public class RecommenderSimilarityRowHadoopReducer extends
  RecommenderSimilarityHadoopReducer {

  @Override
  public void reduce(LongWritable key, Iterable<BytesWritable> values,
    Context context) throws IOException {

    m_task.newMatrixRow();
    int matrixRow = (int) key.get();

    for (BytesWritable b : values) {
      byte[] payload = b.getBytes();
      try {
        Map<Integer, PartialSimilarity> colMapForRow = deserializeColMapForRow(payload);

        for (Map.Entry<Integer, PartialSimilarity> e : colMapForRow.entrySet()) {
          m_task.nextParialSimilarityForMatrixRow(matrixRow, e.getValue());
        }
      } catch (ClassNotFoundException ex) {
        throw new IOException(ex);
      } catch (DistributedWekaException e) {
        throw new IOException(e);
      }
    }

    if (!m_runningAsCombiner) {
      try {
        m_task.reduceModeFinalizeMatrixRow(matrixRow);
      } catch (DistributedWekaException e) {
        throw new IOException(e);
      }
    }
  }

  protected Map<Integer, PartialSimilarity> deserializeColMapForRow(byte[] bytes)
    throws IOException, ClassNotFoundException {

    Map<Integer, PartialSimilarity> mapForRow = null;

    ByteArrayInputStream istream = new ByteArrayInputStream(bytes);
    ObjectInputStream p = null;
    try {
      p = new ObjectInputStream(new BufferedInputStream(new GZIPInputStream(
        istream)));
      mapForRow = (Map<Integer, PartialSimilarity>) p.readObject();
    } finally {
      if (p != null) {
        p.close();
      }
    }

    return mapForRow;
  }
}
