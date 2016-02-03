/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    WekaClassifierHadoopMapper
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.hadoop;

import distributed.core.DistributedJobConfig;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import weka.classifiers.Classifier;
import weka.classifiers.UpdateableClassifier;
import weka.core.Attribute;
import weka.core.Environment;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.WekaClassifierMapTask;
import weka.filters.Filter;
import weka.filters.PreconstructedFilter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Mapper implementation for the WekaClassifierHadoopJob
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class WekaClassifierHadoopMapper extends
  Mapper<LongWritable, Text, Text, BytesWritable> {

  /**
   * The key in the Configuration that the options for this task are associated
   * with
   */
  public static final String CLASSIFIER_MAP_TASK_OPTIONS =
    "*weka.distributed.weka_classifier_map_task_opts";

  /** The underlying general Weka classifier map task */
  protected WekaClassifierMapTask m_task = null;

  /** Helper Weka CSV map task - used simply for parsing CSV entering the map */
  protected CSVToARFFHeaderMapTask m_rowHelper = null;

  /** The ARFF header of the data */
  protected Instances m_trainingHeader;

  /**
   * Helper method that loads the training data header
   * 
   * @param filename the name of the ARFF header to load
   * @return Instances containing only header information
   * @throws IOException if a problem occurs
   */
  protected static Instances loadTrainingHeader(String filename)
    throws IOException {
    File f = new File(filename);

    if (!f.exists()) {
      throw new IOException("The ARFF header file '" + filename
        + "' does not seem to exist in the distributed cache!");
    }

    Instances insts = null;
    // load the training header
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(filename));
      insts = new Instances(br);
      br.close();
      br = null;
    } finally {
      if (br != null) {
        br.close();
      }
    }

    return insts;
  }

  /**
   * Helper method for loading a serialized classifier from the distributed
   * cache
   * 
   * @param filename the name of the classifier to load
   * @return the deserialized Classifier
   * @throws Exception if a problem occurs
   */
  protected static Classifier loadClassifier(String filename) throws Exception {
    File f = new File(filename);

    if (!f.exists()) {
      throw new IOException("The classifier model file '" + filename
        + "' does not seem to exist in the distributed cache!");
    }

    Classifier classifier = null;
    ObjectInputStream ois = null;
    try {
      ois =
        new ObjectInputStream(new BufferedInputStream(new FileInputStream(f)));

      classifier = (Classifier) ois.readObject();
    } finally {
      if (ois != null) {
        ois.close();
      }
    }

    return classifier;
  }

  /**
   * Helper method for loading a serialized PreconstructedFilter from the
   * distributed cache
   * 
   * @param filename the name of the filter to load
   * @return the deserialized PreconstructedFilter
   * @throws Exception if a problem occurs
   */
  protected static Filter loadPreconstructedFilter(String filename)
    throws Exception {
    File f = new File(filename);

    if (!f.exists()) {
      throw new IOException("The pre-constructed filter '" + filename
        + "' does not seem to exist in the distributed cache!");
    }

    Filter filter = null;

    ObjectInputStream ois = null;
    try {
      ois =
        new ObjectInputStream(new BufferedInputStream(new FileInputStream(f)));

      filter = (Filter) ois.readObject();

      // check that its a PreconstructedFilter
      if (!(filter instanceof PreconstructedFilter)) {
        throw new Exception("The filter in file '" + filename
          + "' is not a PreconstructedFilter!");
      }
    } finally {
      if (ois != null) {
        ois.close();
      }
    }

    return filter;
  }

  /**
   * Helper method for setting the class index in the supplied Instances object
   * 
   * @param classNameOrIndex name or index of the class attribute (may be the
   *          special 'first' or 'last' strings)
   * @param data the data to set the class index in
   * @param defaultToLast true if the data should have the last attribute set to
   *          the class if no class name/index is supplied
   * @throws Exception if a problem occurs
   */
  public static void setClassIndex(String classNameOrIndex, Instances data,
    boolean defaultToLast) throws Exception {
    if (!DistributedJobConfig.isEmpty(classNameOrIndex)) {
      // try as a 1-based index first
      try {
        int classIndex = Integer.parseInt(classNameOrIndex);
        classIndex--;
        data.setClassIndex(classIndex);
      } catch (NumberFormatException e) {
        // named attribute?
        Attribute classAttribute = data.attribute(classNameOrIndex.trim());
        if (classAttribute != null) {
          data.setClass(classAttribute);
        } else if (classNameOrIndex.toLowerCase().equals("first")) {
          data.setClassIndex(0);
        } else if (classNameOrIndex.toLowerCase().equals("last")) {
          data.setClassIndex(data.numAttributes() - 1);
        } else {
          throw new Exception("Can't find class attribute: " + classNameOrIndex
            + " in ARFF header!");
        }
      }
    } else if (defaultToLast) {
      // nothing specified? Default to last attribute
      data.setClassIndex(data.numAttributes() - 1);
    }
  }

  /**
   * Helper method to set the class index in a supplied Instances object
   * 
   * @param options an options string that potentially contains the -class
   *          option for specifying the index/name of the class attribute
   * @param data the Instances to set the class index for
   * @param defaultToLast true to set the last attribute as the class if no
   *          -class option is present in the options
   * @throws Exception if a problem occurs
   */
  public static void setClassIndex(String[] options, Instances data,
    boolean defaultToLast) throws Exception {
    // class attribute/index
    String classAtt = Utils.getOption("class", options);
    setClassIndex(classAtt, data, defaultToLast);
  }

  @Override
  public void setup(Context context) throws IOException {
    m_task = new WekaClassifierMapTask();
    m_rowHelper = new CSVToARFFHeaderMapTask();

    Configuration conf = context.getConfiguration();
    String taskOptsS = conf.get(CLASSIFIER_MAP_TASK_OPTIONS);
    String csvOptsS =
      conf.get(CSVToArffHeaderHadoopMapper.CSV_TO_ARFF_HEADER_MAP_TASK_OPTIONS);
    try {
      if (!DistributedJobConfig.isEmpty(csvOptsS)) {
        String[] csvOpts = Utils.splitOptions(csvOptsS);
        m_rowHelper.setOptions(csvOpts);
      }
      // m_rowHelper.initParserOnly();

      if (!DistributedJobConfig.isEmpty(taskOptsS)) {
        String[] taskOpts = Utils.splitOptions(taskOptsS);

        // name of the training ARFF header file
        String arffHeaderFileName = Utils.getOption("arff-header", taskOpts);
        if (DistributedJobConfig.isEmpty(arffHeaderFileName)) {
          throw new IOException(
            "Can't continue without the name of the ARFF header file!");
        }
        m_trainingHeader =
          CSVToARFFHeaderReduceTask
            .stripSummaryAtts(loadTrainingHeader(arffHeaderFileName));

        setClassIndex(taskOpts, m_trainingHeader, true);

        m_rowHelper.initParserOnly(CSVToARFFHeaderMapTask
          .instanceHeaderToAttributeNameList(m_trainingHeader));

        // multiple iterations over the training data for an updateable
        // classifier?
        boolean continueTrainingUpdateable =
          Utils.getFlag("continue-training-updateable", taskOpts);
        m_task
          .setContinueTrainingUpdateableClassifier(continueTrainingUpdateable);

        // Preconstructed filter to use?
        String preconstructedFilter =
          Utils.getOption("preconstructed-filter", taskOpts);
        if (!DistributedJobConfig.isEmpty(preconstructedFilter)) {
          // try and load this from the distributed cache
          Filter preConstructedF =
            loadPreconstructedFilter(preconstructedFilter);
          m_task
            .addPreconstructedFilterToUse((PreconstructedFilter) preConstructedF);
        }

        // pass on the number of maps to the task
        Environment env = new Environment();
        int totalNumMaps = conf.getInt("mapred.map.tasks", 1);
        env.addVariable(WekaClassifierMapTask.TOTAL_NUMBER_OF_MAPS, ""
          + totalNumMaps);
        m_task.setEnvironment(env);

        // set any remaining options on the task itself
        m_task.setOptions(taskOpts);

        // if we are continuing to train then there should be
        // a name for the classifier to load from the distributed
        // cache specified. We set it here *after* m_task.setOptions()
        // so that setOptions() does not overwrite the classifier with
        // a newly created one
        if (continueTrainingUpdateable) {
          String classifierFileName =
            Utils.getOption("model-file-name", taskOpts);
          if (DistributedJobConfig.isEmpty(classifierFileName)) {
            throw new IOException(
              "Continued training of incremental classifier "
                + "has been specified but no file name for the classifier "
                + "to load has been specified. Can't continue.");
          }

          System.err
            .println("WekaClassifierHadoopMapper - loading staged model...");
          Classifier toUse = loadClassifier(classifierFileName);
          if (!(toUse instanceof UpdateableClassifier)) {
            throw new Exception(
              "The classifier loaded is not an UpdateableClassifier (incremental)");
          }

          // System.err.println("Initial classifier for continued training:\n"
          // + toUse.toString());

          m_task.setClassifier(toUse);
        }

        // initialize
        m_task.setup(m_trainingHeader);
      } else {
        throw new IOException(
          "Can't continue without the name of the ARFF header file!");
      }

    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  /**
   * Constructs an instance given the array of parsed CSV values
   * 
   * @param rowHelper the CSV map task that was used to parse the supplied array
   *          of values
   * @param trainingHeader the header of the training data
   * @param classifierIsUpdateable true if the classifier being used is
   *          updateable (this affects how string attribute values are set and
   *          stored).
   * @param forceBatchLearningForUpdateable true if the classifier is updateable
   *          but it is being batch trained
   * @param parsed the array of parsed CSV values
   * @return an Instance
   * @throws Exception if a problem occurs
   */
  protected static Instance makeInstance(CSVToARFFHeaderMapTask rowHelper,
    Instances trainingHeader, boolean classifierIsUpdateable,
    boolean forceBatchLearningForUpdateable, String[] parsed) throws Exception {

    return rowHelper.makeInstance(trainingHeader, classifierIsUpdateable
      && !forceBatchLearningForUpdateable, parsed);
  }

  @Override
  public void map(LongWritable key, Text value, Context context)
    throws IOException {

    if (value != null) {
      processRow(value.toString());
    }
  }

  /**
   * Processes a row of data
   * 
   * @param row the incoming row to process
   * @throws IOException if a problem occurs
   */
  protected void processRow(String row) throws IOException {
    if (row != null) {
      String[] parsed = m_rowHelper.parseRowOnly(row);

      if (parsed.length != m_trainingHeader.numAttributes()) {
        throw new IOException(
          "Parsed a row that contains a different number of values than "
            + "there are attributes in the training ARFF header: " + row);
      }
      try {
        Instance toProcess =
          makeInstance(m_rowHelper, m_trainingHeader,
            (m_task.getClassifier() instanceof UpdateableClassifier),
            m_task.getForceBatchLearningForUpdateableClassifiers(), parsed);

        m_task.processInstance(toProcess);
      } catch (Exception ex) {
        throw new IOException(ex);
      }
    }
  }

  /**
   * Helper method that serializes and compresses a classifier to an array of
   * bytes
   * 
   * @param finalClassifier the classifier to serialize
   * @param numTrainingInstances the number of training instances this
   *          classifier has seen
   * @return an array of bytes containing the compressed serialized classifier
   * @throws IOException if a problem occurs
   */
  protected static byte[] classifierToBytes(Classifier finalClassifier,
    int numTrainingInstances) throws IOException {
    ObjectOutputStream p = null;
    byte[] bytes = null;

    try {
      ByteArrayOutputStream ostream = new ByteArrayOutputStream();
      OutputStream os = ostream;

      p =
        new ObjectOutputStream(new BufferedOutputStream(
          new GZIPOutputStream(os)));
      p.writeObject(finalClassifier);
      p.writeObject(new Integer(numTrainingInstances));
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

  @Override
  public void cleanup(Context context) throws IOException, InterruptedException {
    try {
      m_task.finalizeTask();
      // System.err.println("Model after training:\n"
      // + m_task.getClassifier().toString());

      byte[] bytes =
        classifierToBytes(m_task.getClassifier(),
          m_task.getNumTrainingInstances());

      // make sure all classifiers go to the same reducer
      String constantKey = "classifier";

      Text key = new Text();
      key.set(constantKey);
      BytesWritable value = new BytesWritable();
      value.set(bytes, 0, bytes.length);

      context.write(key, value);
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }
}
