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
 *    WekaClassifierEvaluationHadoopReducer
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.distributed.hadoop;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import weka.classifiers.evaluation.Evaluation;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.CSVSaver;
import weka.core.converters.Saver;
import weka.distributed.CSVToARFFHeaderMapTask;
import weka.distributed.CSVToARFFHeaderReduceTask;
import weka.distributed.WekaClassifierEvaluationReduceTask;
import distributed.core.DistributedJobConfig;

/**
 * Hadoop reducer implementaton for the evaluation job. Aggregates the
 * Evaluation objects from the individual map tasks (which in turn have
 * aggregated over their fold(s)).
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class WekaClassifierEvaluationHadoopReducer extends
  Reducer<Text, BytesWritable, Text, Text> {

  /** The underlying general reduce task */
  protected WekaClassifierEvaluationReduceTask m_task;

  @Override
  public void setup(Context context) throws IOException {
    m_task = new WekaClassifierEvaluationReduceTask();
  }

  @Override
  public void reduce(Text key, Iterable<BytesWritable> values, Context context)
    throws IOException {

    Configuration conf = context.getConfiguration();
    String mapTaskOptsS = conf
      .get(WekaClassifierHadoopMapper.CLASSIFIER_MAP_TASK_OPTIONS);

    Instances trainingHeader = null;
    Instances headerWithoutSummaryAtts = null;
    // double[] priors = null;
    // double priorsCount = 0;
    String totalFolds = "";
    String seed = "1";
    String separateTestSet = "";
    try {
      String[] taskOpts = Utils.splitOptions(mapTaskOptsS);
      String arffHeaderFileName = Utils.getOption("arff-header", taskOpts);
      totalFolds = Utils.getOption("total-folds", taskOpts);
      seed = Utils.getOption("seed", taskOpts);
      separateTestSet = Utils.getOption("test-set-path", taskOpts);

      trainingHeader = WekaClassifierHadoopMapper
        .loadTrainingHeader(arffHeaderFileName);

      headerWithoutSummaryAtts = CSVToARFFHeaderReduceTask
        .stripSummaryAtts(trainingHeader);
      WekaClassifierHadoopMapper.setClassIndex(taskOpts,
        headerWithoutSummaryAtts, true);

      Attribute classAtt = headerWithoutSummaryAtts.classAttribute();
      if (classAtt == null) {
        throw new Exception("Class attribute is null!!");
      }

      // now look for the summary stats att with this name so that we can set up
      // priors for evaluation properly
      String classAttSummaryName = CSVToARFFHeaderMapTask.ARFF_SUMMARY_ATTRIBUTE_PREFIX
        + classAtt.name();
      Attribute summaryClassAtt = trainingHeader.attribute(classAttSummaryName);

      if (summaryClassAtt == null) {
        throw new Exception(
          "WekaClassifierEvaluationHadoopReducer - was unable to find "
            + "the summary meta data attribute for the class attribute in the header");
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }

    List<Evaluation> evalsToAgg = new ArrayList<Evaluation>();

    try {
      for (BytesWritable b : values) {
        byte[] bytes = b.getBytes();
        evalsToAgg.add(deserialize(bytes));
      }
    } catch (Exception ex) {
      throw new IOException(ex);
    }

    int totalFoldsI = 1;
    if (!DistributedJobConfig.isEmpty(totalFolds)) {
      try {
        totalFoldsI = Integer.parseInt(totalFolds);
      } catch (NumberFormatException ex) {
      }
    }
    try {
      Evaluation aggregated = m_task.aggregate(evalsToAgg);

      Text outkey = new Text();
      String info = "Summary - ";
      if (!DistributedJobConfig.isEmpty(separateTestSet)) {
        info += "separate test set";
      } else if (totalFoldsI == 1) {
        info += "test on training";
      } else {
        info += totalFolds + " fold cross-validation (seed=" + seed
          + ")\n(note: relative measures might be slightly "
          + "pessimistic due to the mean/mode of the target being computed on "
          + "all the data rather than on training folds)";
      }
      info += ":\n";
      if (aggregated.predictions() != null) {
        info += "Number of predictions retained for computing AUC/AUPRC: "
          + aggregated.predictions().size() + "\n";
      }
      outkey.set(info);
      Text outVal = new Text();
      outVal.set(aggregated.toSummaryString() + "\n");
      context.write(outkey, outVal);

      outVal = new Text();
      if (aggregated.getHeader().classAttribute().isNominal()) {
        outVal.set(aggregated.toClassDetailsString() + "\n");
        context.write(null, outVal);

        outVal = new Text();
        outVal.set(aggregated.toMatrixString() + "\n");
        context.write(null, outVal);
      }

      // convert the evaluation into an ARFF or CSV file
      String outputDir = context.getConfiguration().get("mapred.output.dir");
      if (DistributedJobConfig.isEmpty(outputDir)) {
        throw new Exception(
          "WekaClassifierEvaluationReducer - unable to get the output directory "
            + "for some reason!");
      }

      writeEvalAsStructured(aggregated, outputDir, context.getConfiguration());
    } catch (Exception ex) {
      throw new IOException(ex);
    }
  }

  /**
   * Writes the aggregated evaluation results out to the output directory as
   * single row CSV and ARFF files
   * 
   * @param aggregated the final aggregated Evaluation object
   * @param outputPath the path to write to
   * @param conf the Configuration for the job
   * @throws Exception if a problem occurs
   */
  protected void writeEvalAsStructured(Evaluation aggregated,
    String outputPath, Configuration conf) throws Exception {

    // basic stats - always output all of these (regardless of class attribute
    // type)
    double numCorrect = aggregated.correct();
    double numIncorrect = aggregated.incorrect();
    double MAE = aggregated.meanAbsoluteError();
    double RMSE = aggregated.rootMeanSquaredError();
    double RAE = aggregated.relativeAbsoluteError();
    double RRSE = aggregated.rootRelativeSquaredError();
    double totalNumberOfInstances = aggregated.numInstances();

    ArrayList<Attribute> atts = new ArrayList<Attribute>();
    atts.add(new Attribute("Correctly classified instances"));
    atts.add(new Attribute("Incorrectly classified instances"));
    atts.add(new Attribute("Mean absolute error"));
    atts.add(new Attribute("Root mean squared error"));
    atts.add(new Attribute("Relative absolute error"));
    atts.add(new Attribute("Root relative squared error"));
    atts.add(new Attribute("Total number of instances"));

    // int capacity = 1;

    // info retrieval stats (only add these if class is nominal)
    if (aggregated.getHeader().classAttribute().isNominal()) {
      // capacity = aggregated.getHeader().classAttribute().numValues();

      atts.add(new Attribute("Kappa statistic"));

      for (int i = 0; i < aggregated.getHeader().classAttribute().numValues(); i++) {
        String classLabel = aggregated.getHeader().classAttribute().value(i)
          + "_";
        atts.add(new Attribute(classLabel + "TP Rate"));
        atts.add(new Attribute(classLabel + "FP Rate"));
        atts.add(new Attribute(classLabel + "Precision"));
        atts.add(new Attribute(classLabel + "Recall"));
        atts.add(new Attribute(classLabel + "F-Measure"));
        atts.add(new Attribute(classLabel + "MCC"));
        atts.add(new Attribute(classLabel + "ROC Area"));
        atts.add(new Attribute(classLabel + "PRC Area"));
      }
    }

    Instances evalInsts = new Instances("Evaluation results: "
      + aggregated.getHeader().relationName(), atts, 1);

    // make instances

    double[] vals = new double[atts.size()];

    vals[0] = numCorrect;
    vals[1] = numIncorrect;
    vals[2] = MAE;
    vals[3] = RMSE;
    vals[4] = RAE;
    vals[5] = RRSE;
    vals[6] = totalNumberOfInstances;

    int offset = 7;
    if (aggregated.getHeader().classAttribute().isNominal()) {
      vals[offset++] = aggregated.kappa();
      for (int i = 0; i < aggregated.getHeader().classAttribute().numValues(); i++) {
        vals[offset++] = aggregated.truePositiveRate(i);
        vals[offset++] = aggregated.falseNegativeRate(i);
        vals[offset++] = aggregated.precision(i);
        vals[offset++] = aggregated.recall(i);
        vals[offset++] = aggregated.fMeasure(i);
        vals[offset++] = aggregated.areaUnderROC(i);
        vals[offset++] = aggregated.areaUnderPRC(i);
      }
    }

    Instance inst = new DenseInstance(1.0, vals);
    evalInsts.add(inst);
    evalInsts.compactify();

    // if (!outputPath.startsWith("hdfs://")) {
    // outputPath = WekaClassifierHadoopReducer.constructHDFSURI(outputPath,
    // conf);
    // }
    String arffFileName = outputPath + "/evaluation.arff";
    // String arffFileName = WekaClassifierHadoopReducer.constructHDFSURI(
    // outputPath + "/evaluation.arff", conf);
    String csvFileName = outputPath + "/evaluation.csv";
    // String csvFileName = WekaClassifierHadoopReducer.constructHDFSURI(
    // outputPath + "/evaluation.csv", conf);

    PrintWriter pr = null;
    try {

      // write the instances file
      Path pt = new Path(arffFileName);
      FileSystem fs = FileSystem.get(conf);
      FSDataOutputStream fout = fs.create(pt);

      pr = new PrintWriter(new BufferedWriter(new OutputStreamWriter(fout)));
      pr.print(evalInsts.toString() + "\n");

      pr.flush();
      pr.close();

      pr = null;

      pt = new Path(csvFileName);
      fs = FileSystem.get(conf);

      fout = fs.create(pt);
      CSVSaver saver = new CSVSaver();
      saver.setRetrieval(Saver.BATCH);
      saver.setInstances(evalInsts);
      saver.setDestination(fout);
      saver.writeBatch();

      fout.close();

    } finally {
      if (pr != null) {
        pr.close();
      }
    }
  }

  /**
   * Helper function to deserialize a Evaluation object from an array of bytes
   * 
   * @param bytes the array containing the compressed serialized Evaluation
   *          object
   * @return the deserialized Evaluation object
   * @throws Exception if a problem occurs
   */
  protected Evaluation deserialize(byte[] bytes) throws Exception {
    ByteArrayInputStream istream = new ByteArrayInputStream(bytes);
    ObjectInputStream p = null;
    Object toReturn = null;

    try {
      p = new ObjectInputStream(new BufferedInputStream(new GZIPInputStream(
        istream)));

      toReturn = p.readObject();
      if (!(toReturn instanceof Evaluation)) {
        throw new Exception("Object deserialized was not an Evaluation object!");
      }
    } finally {
      if (p != null) {
        p.close();
      }
    }

    return (Evaluation) toReturn;
  }
}
