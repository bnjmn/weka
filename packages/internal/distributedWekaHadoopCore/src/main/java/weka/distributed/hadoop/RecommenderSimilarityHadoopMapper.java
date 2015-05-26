package weka.distributed.hadoop;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ArffLoader.ArffReader;
import weka.distributed.RecommenderSimilarityMapTask;
import weka.distributed.RecommenderSimilarityMapTask.PartialSimilarity;
import distributed.core.DistributedJobConfig;

public class RecommenderSimilarityHadoopMapper extends
  Mapper<LongWritable, Text, LongWritable, BytesWritable> {

  /**
   * The key in the Configuration that the options for this task are associated
   * with
   */
  public static final String SIMILARITY_MAP_TASK_OPTIONS = "*weka.distributed.weka_similarity_map_task_opts";

  protected Instances m_denormalizedHeader;

  protected LongWritable m_outKey = new LongWritable();
  protected BytesWritable m_outValue = new BytesWritable();

  public static final int DEFAULT_BUFFER_SIZE = 200;

  protected int m_bufferSize = DEFAULT_BUFFER_SIZE;

  protected List<String> m_rowBuffer;

  protected RecommenderSimilarityMapTask m_task;

  protected ArffReader m_sparseReader;

  protected int m_totalRecordsOutput = 0;
  protected int m_totalInputInstances = 0;

  protected int m_heapSpace = 200; // default Mb

  @Override
  protected void setup(Context context) throws IOException {
    m_rowBuffer = new ArrayList<String>(m_bufferSize);
    m_task = new RecommenderSimilarityMapTask();

    Configuration conf = context.getConfiguration();

    String taskOptsS = conf.get(SIMILARITY_MAP_TASK_OPTIONS);

    try {
      if (!DistributedJobConfig.isEmpty(taskOptsS)) {
        String[] taskOpts = Utils.splitOptions(taskOptsS);

        m_task.setOptions(taskOpts);
      }

    } catch (Exception ex) {
      throw new IOException(ex);
    }

    // load the denormalized header from the distributed cache
    m_denormalizedHeader = WekaClassifierHadoopMapper
      .loadTrainingHeader(DenormalizeHadoopReducer.DENORMALIZED_HEADER_FILE_NAME);

    // See if we can find out how much heap space we have
    String heap = context.getConfiguration().get("mapred.child.java.opts");
    if (!DistributedJobConfig.isEmpty(heap)) {
      heap = heap.toLowerCase().replace("-xmx", "");
      heap = heap.substring(0, heap.length() - 1);
      try {
        m_heapSpace = Integer.parseInt(heap);
      } catch (NumberFormatException e) {
      }
    }
  }

  @Override
  protected void map(LongWritable key, Text value, Context context)
    throws IOException {
    if (m_rowBuffer.size() == DEFAULT_BUFFER_SIZE) {
      processBuffer(context);
    }

    m_rowBuffer.add(value.toString());
  }

  @Override
  public void cleanup(Context context) throws IOException {
    if (m_rowBuffer.size() > 0) {
      processBuffer(context);
    }

    System.err.println("Total instances seen by mapper "
      + m_totalInputInstances);
    System.err.println("Total number of records output by mapper "
      + m_totalRecordsOutput);
  }

  protected void processBuffer(Context context) throws IOException {
    // first convert buffer to sparse instances
    m_denormalizedHeader.clear();

    StringBuilder b = new StringBuilder();
    for (String s : m_rowBuffer) {
      b.append(s).append("\n");
    }

    StringReader reader = new StringReader(b.toString());
    m_sparseReader = new ArffReader(reader, m_denormalizedHeader, 0,
      DEFAULT_BUFFER_SIZE, false);

    Instance tempI = null;
    while ((tempI = m_sparseReader.readInstance(m_denormalizedHeader)) != null) {
      // TODO - need to set the instance weight here to be the mean
      // of this user's item ratings (just in case the cosine measure
      // is being used
      nextInstance(tempI, context);
      m_totalInputInstances++;
    }

    m_rowBuffer.clear();
  }

  protected void nextInstance(Instance instance, Context context)
    throws IOException {
    m_task.newInstance(instance);

    PartialSimilarity sim = null;
    while ((sim = m_task.nextPartialSimilarity()) != null) {
      m_outKey.set(sim.m_i);

      // now serialize the partial sim
      byte[] b = simToBytes(sim);
      m_outValue.set(b, 0, b.length);

      try {
        m_totalRecordsOutput++;
        context.write(m_outKey, m_outValue);
      } catch (InterruptedException e) {
        throw new IOException(e);
      }
    }
  }

  protected static byte[] simToBytes(PartialSimilarity sim) throws IOException {
    ObjectOutputStream p = null;
    byte[] bytes = null;

    try {
      ByteArrayOutputStream ostream = new ByteArrayOutputStream();
      OutputStream os = ostream;

      p = new ObjectOutputStream(new BufferedOutputStream(new GZIPOutputStream(
        os)));

      p.writeObject(sim);
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
