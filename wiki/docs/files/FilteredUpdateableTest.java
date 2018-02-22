import weka.core.*;
import weka.classifiers.*;
import weka.classifiers.bayes.*;
import weka.classifiers.meta.*;
import weka.filters.supervised.attribute.*;

import java.io.*;
import java.util.*;

/**
 * Tests the incremental version of the FilteredClassifier.
 * Takes a dataset as only parameter, loads this dataset (class attribute
 * is assumed to be the last), trains the classifier in batch mode with
 * 10% of the data and the incrementally trains the classifier with the
 * rest of the data.
 * The classifier is first trained in batch mode to initialize the
 * internal filter with sensible values.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz )
 */
public class FilteredUpdateableTest {
  public static void main(String[] args) throws Exception {
    // load data
    System.out.println("Loading data...");
    BufferedReader reader = new BufferedReader(new FileReader(args[0]));
    Instances train = new Instances(reader);
    train.randomize(new Random(1));
    reader.close();
    train.setClassIndex(train.numAttributes() - 1);

    // batch train classifier with 50%
    int numBatch = train.numInstances() / 10;
    System.out.println("Batch-training classifier with " + numBatch + " instances...");
    Instances trainBatch = new Instances(train, numBatch);
    for (int i = 0; i < numBatch; i++)
      trainBatch.add(new Instance(train.instance(i)));
    FilteredClassifierUpdateable cls = new FilteredClassifierUpdateable();
    cls.setClassifier(new NaiveBayesUpdateable());
    cls.setFilter(new Discretize());
    cls.buildClassifier(trainBatch);

    // evaluate 50% classifier on full dataset
    Evaluation eval = new Evaluation(train);
    eval.evaluateModel(cls, train);
    System.out.println(eval.toSummaryString("=== 10% trained ===", false));

    // incrementally training classifier
    for (int i = numBatch; i < train.numInstances(); i++)
      cls.updateClassifier(train.instance(i));

    // evaluate incrementally trained classifier again on full dataset
    eval = new Evaluation(train);
    eval.evaluateModel(cls, train);
    System.out.println(eval.toSummaryString("=== fully trained ===", false));
  }
}
