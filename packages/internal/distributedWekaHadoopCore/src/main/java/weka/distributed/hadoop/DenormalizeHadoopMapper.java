package weka.distributed.hadoop;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import weka.core.Instances;
import weka.core.Utils;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderReduceTask;
import distributed.core.DistributedJobConfig;

public class DenormalizeHadoopMapper extends
  Mapper<LongWritable, Text, LongWritable, Text> {

  /**
   * The key in the Configuration that the options for this task are associated
   * with
   */
  public static final String DENORMALIZE_MAP_TASK_OPTIONS = "*weka.distributed.weka_denormalize_map_task_opts";

  /** Helper Weka CSV map task - used simply for parsing CSV entering the map */
  protected CSVToARFFHeaderMapTask m_rowHelper = null;

  protected Instances m_trainingHeader;

  protected LongWritable m_outKey = new LongWritable();
  protected Text m_outValue = new Text();

  @Override
  public void setup(Context context) throws IOException {

    m_rowHelper = new CSVToARFFHeaderMapTask();

    Configuration conf = context.getConfiguration();

    String csvOptsS = conf
      .get(CSVToArffHeaderHadoopMapper.CSV_TO_ARFF_HEADER_MAP_TASK_OPTIONS);
    String taskOptsS = conf.get(DENORMALIZE_MAP_TASK_OPTIONS);

    try {
      if (!DistributedJobConfig.isEmpty(csvOptsS)) {
        String[] csvOpts = Utils.splitOptions(csvOptsS);
        m_rowHelper.setOptions(csvOpts);
      }

      if (!DistributedJobConfig.isEmpty(taskOptsS)) {
        String[] taskOpts = Utils.splitOptions(taskOptsS);

        // name of the training ARFF header file
        String arffHeaderFileName = Utils.getOption("arff-header", taskOpts);
        // String minMaxItemNumbers = Utils.getOption("item-id-range",
        // taskOpts);

        if (DistributedJobConfig.isEmpty(arffHeaderFileName)
        /* && DistributedJobConfig.isEmpty(minMaxItemNumbers) */) {
          throw new IOException(
            "Can't continue without the name of the ARFF header file or item ID range!");
        }

        if (!DistributedJobConfig.isEmpty(arffHeaderFileName)) {
          m_trainingHeader = CSVToARFFHeaderReduceTask
            .stripSummaryAtts(WekaClassifierHadoopMapper
              .loadTrainingHeader(arffHeaderFileName));
        } else {
          throw new IOException(
            "Can't continue without the name of the ARFF header file!");
        }

        m_rowHelper.initParserOnly(CSVToARFFHeaderMapTask
          .instanceHeaderToAttributeNameList(m_trainingHeader));

      } else {
        throw new IOException(
          "Can't continue without the name of the ARFF header file!");
      }

    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  @Override
  public void map(LongWritable key, Text value, Context context)
    throws IOException {
    if (value != null) {
      String[] parsed = m_rowHelper.parseRowOnly(value.toString());

      if (parsed.length != m_trainingHeader.numAttributes()) {
        throw new IOException(
          "Parsed a row that contains a different number of values than "
            + "there are attributes in the training ARFF header: "
            + value.toString());
      }

      if (parsed[0] != null && parsed[0].length() > 0) {
        long userID = Long.parseLong(parsed[0]);

        m_outKey.set(userID);
        m_outValue.set(value.toString());
        try {
          context.write(m_outKey, m_outValue);
        } catch (InterruptedException e) {
          throw new IOException(e);
        }
      }
    }
  }
}
