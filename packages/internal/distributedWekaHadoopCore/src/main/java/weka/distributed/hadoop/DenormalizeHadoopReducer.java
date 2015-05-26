package weka.distributed.hadoop;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.core.Utils;
import weka.core.stats.ArffSummaryNumericMetric;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.recommender.RecommenderUtils;
import distributed.core.DistributedJobConfig;

public class DenormalizeHadoopReducer extends
  Reducer<LongWritable, Text, Text, Text> {

  /** Default name of the subdirectory in which to store header and stats */
  public static final String AUX_ITEM_STATS_SUBDIR_NAME = "_headerAndStats";

  /** Full path at which to store header, item means and counts for later use */
  public static final String AUX_ITEM_STATS_WRITE_PATH = "*weka.distributed.similarity.aux_item_stats_write_path";

  /**
   * Name of the file to output the denormalized header to * public static final
   */
  public static final String DENORMALIZED_HEADER_FILE_NAME = "denormalized.arff";

  /** The name of the file to output the serialized array of item counts to */
  public static final String ITEM_COUNTS_FILE_NAME = "item_counts.ser";

  /** The name of the file to output the serialized array of item means to */
  public static final String ITEM_MEANS_FILE_NAME = "item_means.ser";

  /** Need this for the min and max item ID */
  protected Instances m_normalizedHeaderWithSummary;

  /** We'll write this out in the cleanup() method */
  protected Instances m_denormalizedHeader;

  /** We'll serialize and write this in the cleanup() method */
  protected double[] m_itemMeans;

  /** We'll serialize and write this in the cleanup() method */
  protected int[] m_itemCounts;

  /** Minumum item number */
  protected int m_itemMin;

  /** Maximum item number */
  protected int m_itemMax;

  /**
   * Helper Weka CSV map task - used simply for parsing CSV entering the reducer
   */
  protected CSVToARFFHeaderMapTask m_rowHelper;

  /** TODO ignore rating value (if present) and just set values to 1 */
  protected boolean m_encodeValuesAsBinary;

  /**
   * TODO if encoding as binary, should we make denormalized attributes nominal?
   */
  protected boolean m_nominalBinary;

  /** Reusable Text for outputing sparse instances */
  protected Text m_outValue = new Text();

  @Override
  public void setup(Context context) throws IOException {
    m_rowHelper = new CSVToARFFHeaderMapTask();

    Configuration conf = context.getConfiguration();

    String csvOptsS = conf
      .get(CSVToArffHeaderHadoopMapper.CSV_TO_ARFF_HEADER_MAP_TASK_OPTIONS);
    String taskOptsS = conf
      .get(DenormalizeHadoopMapper.DENORMALIZE_MAP_TASK_OPTIONS);

    try {
      if (!DistributedJobConfig.isEmpty(csvOptsS)) {
        String[] csvOpts = Utils.splitOptions(csvOptsS);
        m_rowHelper.setOptions(csvOpts);
      }

      if (!DistributedJobConfig.isEmpty(taskOptsS)) {
        String[] taskOpts = Utils.splitOptions(taskOptsS);

        // name of the training ARFF header file
        String arffHeaderFileName = Utils.getOption("arff-header", taskOpts);
        String minMaxItemNumbers = Utils.getOption("item-id-range", taskOpts);

        if (DistributedJobConfig.isEmpty(arffHeaderFileName)
          && DistributedJobConfig.isEmpty(minMaxItemNumbers)) {
          throw new IOException(
            "Can't continue without the name of the ARFF header file or item ID range!");
        }

        int numItems = 0;
        if (!DistributedJobConfig.isEmpty(arffHeaderFileName)) {
          m_normalizedHeaderWithSummary = WekaClassifierHadoopMapper
            .loadTrainingHeader(arffHeaderFileName);

          // grab the stats for the itemID attribute
          Attribute itemAtt = null;
          for (int i = 0; i < m_normalizedHeaderWithSummary.numAttributes(); i++) {
            if (m_normalizedHeaderWithSummary.attribute(i).name()
              .startsWith(CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX)) {

              // first one will be the user ID
              itemAtt = m_normalizedHeaderWithSummary.attribute(i + 1);
              break;
            }
          }

          if (itemAtt == null) {
            throw new IOException(
              "Unable to find summary stats attribute for the "
                + "user ID field");
          }

          m_itemMin = (int) ArffSummaryNumericMetric.MIN
            .valueFromAttribute(itemAtt);
          m_itemMax = (int) ArffSummaryNumericMetric.MAX
            .valueFromAttribute(itemAtt);

          m_normalizedHeaderWithSummary = CSVToARFFHeaderReduceTask
            .stripSummaryAtts(m_normalizedHeaderWithSummary);
        } else {
          String[] minMax = minMaxItemNumbers.split(",");
          if (minMax.length != 2) {
            throw new IOException(
              "Was expecting two comma-separated values for item ID "
                + "range - instead got: " + minMaxItemNumbers);
          }

          m_itemMin = Integer.parseInt(minMax[0].trim());
          m_itemMax = Integer.parseInt(minMax[1].trim());
        }
        numItems = m_itemMax - m_itemMin + 1;

        m_rowHelper.initParserOnly(CSVToARFFHeaderMapTask
          .instanceHeaderToAttributeNameList(m_normalizedHeaderWithSummary));

        // make the denormalized header
        ArrayList<Attribute> atts = new ArrayList<Attribute>();
        atts.add(new Attribute("user_id"));
        for (int i = m_itemMin; i <= m_itemMax; i++) {
          atts.add(new Attribute("item_" + i));
        }
        m_denormalizedHeader = new Instances(
          RecommenderUtils.getDenormalizedRelationName(null, m_itemMin,
            m_itemMax), atts, 0);

        // +1 for the user ID - not needed of course, but makes
        // it easier not having to subtract 1 from all indexes when
        // processing sparse instances (just to account for the user
        // ID
        m_itemMeans = new double[numItems + 1];
        m_itemCounts = new int[numItems + 1];

      } else {
        throw new IOException(
          "Can't continue without the name of the ARFF header file!");
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  @Override
  public void reduce(LongWritable key, Iterable<Text> values, Context context)
    throws IOException {
    // key = userID, value = remaining fields in normalized format (including
    // userID)

    double[] vals = new double[m_denormalizedHeader.numAttributes()];
    vals[0] = key.get(); // set the user ID

    for (Text row : values) {
      String rowS = row.toString();

      String[] parsed = m_rowHelper.parseRowOnly(rowS);
      String itemID = parsed[1];
      if (DistributedJobConfig.isEmpty(itemID.trim())) {
        continue;
      }

      itemID = itemID.replace("item_", "");
      int itemNumber = Integer.parseInt(itemID);

      int itemIndex = itemNumber - m_itemMin + 1; // +1 for userID offset

      if (itemIndex < m_denormalizedHeader.numAttributes()) {
        if (parsed.length == 2 || m_encodeValuesAsBinary) {
          vals[itemIndex] = 1;
        } else {
          String rating = parsed[2];
          if (!DistributedJobConfig.isEmpty(rating.trim())) {
            double ratingV = Double.parseDouble(rating);
            vals[itemIndex] = ratingV;
          } else {
            vals[itemIndex] = 1;
          }
        }
      }
    }

    // make the instance
    Instance newInst = new SparseInstance(1.0, vals);

    double userCount = 0;
    double userMean = 0;
    // update means and counts
    for (int i = 0; i < newInst.numValues(); i++) {
      int index = newInst.index(i);
      if (index == 0) {
        continue; // skip userID
      }

      double value = newInst.valueSparse(i);
      if (!Utils.isMissingValue(value)) {
        m_itemMeans[index] += value;
        m_itemCounts[index]++;
        userMean += value;
        userCount++;
      }
    }

    if (userCount > 0) {
      // store the user mean in the instance's weight.
      userMean /= userCount;
      newInst.setWeight(userMean);
    }

    newInst.setDataset(m_denormalizedHeader);

    m_outValue.set(newInst.toString());

    try {
      context.write(null, m_outValue);
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void cleanup(Context context) throws IOException {
    String outputDestination = context.getConfiguration().get(
      AUX_ITEM_STATS_WRITE_PATH);

    for (int i = 1; i < m_itemCounts.length; i++) {
      if (m_itemCounts[i] > 0) {
        m_itemMeans[i] /= m_itemCounts[i];
      }
    }

    String headerPath = outputDestination + "/" + DENORMALIZED_HEADER_FILE_NAME;
    String itemMeansPath = outputDestination + "/" + ITEM_MEANS_FILE_NAME;
    String itemCountsPath = outputDestination + "/" + ITEM_COUNTS_FILE_NAME;

    // write the denormalized header
    CSVToArffHeaderHadoopReducer.writeHeaderToDestination(m_denormalizedHeader,
      headerPath, context.getConfiguration());

    Path path = new Path(itemMeansPath);
    FileSystem fs = FileSystem.get(context.getConfiguration());
    if (fs.exists(path)) {
      fs.delete(path, true);
    }

    FSDataOutputStream fout = fs.create(path);
    BufferedOutputStream bos = new BufferedOutputStream(fout);
    ObjectOutputStream oos = null;
    try {
      oos = new ObjectOutputStream(bos);
      oos.writeObject(m_itemMeans);
      oos.flush();
      oos.close();

      path = new Path(itemCountsPath);
      if (fs.exists(path)) {
        fs.delete(path, true);
      }
      fout = fs.create(path);
      bos = new BufferedOutputStream(fout);
      oos = new ObjectOutputStream(bos);
      oos.writeObject(m_itemCounts);
    } finally {
      if (oos != null) {
        oos.flush();
        oos.close();
      }
    }
  }
}
