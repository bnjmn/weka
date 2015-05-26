package weka.distributed.hadoop;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Reducer;

import weka.core.Instances;
import weka.core.Utils;
import weka.distributed.DistributedWekaException;
import weka.distributed.RecommenderSimilarityMapTask.PartialSimilarity;
import weka.distributed.RecommenderSimilarityReduceTask;
import weka.recommender.ItemSimilarityRecommender;
import distributed.core.DistributedJobConfig;

public class RecommenderSimilarityHadoopReducer extends
  Reducer<LongWritable, BytesWritable, LongWritable, BytesWritable> {

  /**
   * Path at which to store the serialized similarity matrix and serialized
   * item-based recommender
   */
  public static final String SIMILARITY_WRITE_PATH = "*weka.distributed.recommender.similarity.item_sim_write_path";

  /** The name of the file to output the serialized matrix to */
  public static final String MATRIX_FILE_NAME = "sim_matrix.ser.gz";

  /** The name of the file to output the serialized item-based recommender to */
  public static final String SIM_RECOMMENDER_FILE_NAME = "itemSimRecommender.model.gz";

  protected RecommenderSimilarityReduceTask m_task;

  protected Instances m_denormalizedHeader;

  protected boolean m_runningAsCombiner = false;

  @Override
  public void setup(Context context) throws IOException {
    Configuration conf = context.getConfiguration();

    String taskOptsS = conf
      .get(RecommenderSimilarityHadoopMapper.SIMILARITY_MAP_TASK_OPTIONS);

    if (DistributedJobConfig.isEmpty(taskOptsS)) {
      throw new IOException(
        "Can't continue without knowing the name of the similarity metric used");
    }

    try {
      String simMetric = Utils.getOption("sim-metric",
        Utils.splitOptions(taskOptsS));

      if (DistributedJobConfig.isEmpty(simMetric)) {
        throw new Exception(
          "Can't continue without knowing the name of the similarity metric used");
      }

      /** load the denormalized header from the distributed cache */
      m_denormalizedHeader = WekaClassifierHadoopMapper
        .loadTrainingHeader(DenormalizeHadoopReducer.DENORMALIZED_HEADER_FILE_NAME);

      m_task = new RecommenderSimilarityReduceTask(simMetric,
        m_runningAsCombiner, m_denormalizedHeader);

    } catch (Exception ex) {
      throw new IOException(ex);
    }

  }

  @Override
  public void reduce(LongWritable key, Iterable<BytesWritable> values,
    Context context) throws IOException {

    m_task.newMatrixRow();
    int matrixRow = (int) key.get();

    for (BytesWritable b : values) {
      byte[] payload = b.getBytes();

      try {
        PartialSimilarity sim = deserializeSim(payload);
        m_task.nextParialSimilarityForMatrixRow(matrixRow, sim);
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

  @Override
  public void cleanup(Context context) throws IOException {
    if (!m_runningAsCombiner) {
      System.err.println("Total number of matrix entries: "
        + m_task.getNumMatrixEntries());

      // grab the final sparse matrix and serialize it to the output directory
      Object matrix = m_task.getSimilarityMatrixGeneric();
      ItemSimilarityRecommender recommender = new ItemSimilarityRecommender();
      try {
        recommender.setSimilarityMatrix(matrix);

        // initialize with just the header
        recommender.buildRecommender(m_denormalizedHeader);

        // now we need to load the item means and counts
        int[] counts = loadItemCounts(context.getConfiguration());
        double[] means = loadItemMeans(context.getConfiguration());

        recommender.setItemCounts(counts);
        recommender.setItemMeans(means);
      } catch (Exception ex) {
        throw new IOException(ex);
      }

      String outputDestination = context.getConfiguration().get(
        SIMILARITY_WRITE_PATH);
      String matrixPath = outputDestination + "/" + MATRIX_FILE_NAME;
      String recommenderPath = outputDestination + "/"
        + SIM_RECOMMENDER_FILE_NAME;

      Path path = new Path(matrixPath);
      FileSystem fs = FileSystem.get(context.getConfiguration());
      if (fs.exists(path)) {
        fs.delete(path, true);
      }

      FSDataOutputStream fout = fs.create(path);
      GZIPOutputStream gzo = new GZIPOutputStream(fout);
      BufferedOutputStream bos = new BufferedOutputStream(gzo);
      ObjectOutputStream oos = null;
      try {
        // write the matrix
        oos = new ObjectOutputStream(bos);
        oos.writeObject(matrix);
        oos.flush();
        oos.close();

        // write the recommender model
        path = new Path(recommenderPath);
        if (fs.exists(path)) {
          fs.delete(path, true);
        }
        fout = fs.create(path);
        gzo = new GZIPOutputStream(fout);
        bos = new BufferedOutputStream(gzo);
        oos = new ObjectOutputStream(bos);
        oos.writeObject(recommender);
        oos.flush();
        oos.close();

      } finally {
        if (oos != null) {
          oos.flush();
          oos.close();
        }
      }
    }
  }

  protected double[] loadItemMeans(Configuration conf) throws IOException,
    ClassNotFoundException {
    double[] means = null;

    File f = new File(DenormalizeHadoopReducer.ITEM_MEANS_FILE_NAME);
    if (!f.exists()) {
      throw new IOException("The item counts file '"
        + DenormalizeHadoopReducer.ITEM_MEANS_FILE_NAME
        + "' does not seem to exist in the distributed cache!");
    }

    ObjectInputStream ois = null;

    try {
      ois = new ObjectInputStream(new BufferedInputStream(
        new FileInputStream(f)));
      means = (double[]) ois.readObject();
    } finally {
      if (ois != null) {
        ois.close();
      }
    }

    return means;
  }

  protected int[] loadItemCounts(Configuration conf) throws IOException,
    ClassNotFoundException {
    int[] counts = null;

    File f = new File(DenormalizeHadoopReducer.ITEM_COUNTS_FILE_NAME);
    if (!f.exists()) {
      throw new IOException("The item counts file '"
        + DenormalizeHadoopReducer.ITEM_COUNTS_FILE_NAME
        + "' does not seem to exist in the distributed cache!");
    }

    ObjectInputStream ois = null;
    try {
      ois = new ObjectInputStream(new BufferedInputStream(
        new FileInputStream(f)));
      counts = (int[]) ois.readObject();
    } finally {
      if (ois != null) {
        ois.close();
      }
    }

    return counts;
  }

  protected PartialSimilarity deserializeSim(byte[] bytes) throws IOException,
    ClassNotFoundException {
    PartialSimilarity sim = null;

    ByteArrayInputStream istream = new ByteArrayInputStream(bytes);
    ObjectInputStream p = null;
    try {
      p = new ObjectInputStream(new BufferedInputStream(new GZIPInputStream(
        istream)));
      sim = (PartialSimilarity) p.readObject();
    } finally {
      if (p != null) {
        p.close();
      }
    }

    return sim;
  }

}
