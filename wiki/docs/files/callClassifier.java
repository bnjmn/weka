/*
 * Example class which creates a classifier and evaluates it via cross-
 * validation (or test set), outputting the complete class
 * probability distributions
 */

import java.io.*;
import java.util.*;
import weka.core.*;
import weka.classifiers.Classifier;

public class callClassifier {

  public static void main(String [] argv) {

    try {
      int numFolds=0; int cIdx=-1; int seed=1;
      
      if (argv.length==0||Utils.getFlag('h',argv)) {
        throw new Exception("Usage: callClassifier [ClassifierName] -t [TrainFile] [-T [TestFile]] [-e delim] [-x numFolds] [-s randomSeed] [-c classIndex] ...\n       outputs probability distributions for test instances\n       If no test file is given, does a cross-validation on training data.\n       Format: InstanceID PredClass Confidence TrueClass [class probabilities]\n       (first four fields are similar to those in WEKA 3-3-5)\n       Field delimiter can be changed via -e (default: space)\n");
      }
      String classifierName=argv[0];
      argv[0]="";

      String delim = Utils.getOption('e',argv);
      if (delim.length()==0) {
        delim=" ";
      }

      String trainFile = Utils.getOption('t',argv);
      if (trainFile.length()==0) throw new Exception("No train file given!");
      String testFile  = Utils.getOption('T',argv);

      String cv = Utils.getOption('x',argv);
      if (cv.length()!=0) {
        numFolds=Integer.parseInt(cv);
      } else {
        numFolds=10; // default
      }

      String classIdx = Utils.getOption('c',argv);
      String seedS = Utils.getOption('s',argv);
      if (seedS.length()!=0) {
        seed=Integer.parseInt(seedS);
      }

      Classifier c = Classifier.forName(classifierName,argv);

      Instances trainData = new Instances(new FileReader(trainFile));
      Instances testData = null;

      if (classIdx.length()!=0) {
        cIdx=Integer.parseInt(classIdx)-1;
        if ((cIdx<0)||(cIdx>=trainData.numAttributes())) throw new Exception("Invalid value for class index!");
      } else {
        cIdx=trainData.numAttributes()-1;
      }

      if (testFile.length()!=0)
        testData  = new Instances(new FileReader(testFile));
      
      trainData.setClassIndex(cIdx);

      if (testData==null) {
        if (numFolds<2||numFolds>trainData.numInstances()) {
          throw new Exception("Invalid number of cross-validation folds!");
        }

        // generate pseudo-dataset with instance ids, to get the same reordering..
        FastVector attInfo = new FastVector(2);

        attInfo.addElement(new Attribute("Idx_20011004"));
        attInfo.addElement(trainData.classAttribute());

        Instances indices = new Instances("Indices",attInfo,trainData.numInstances());
        indices.setClass((Attribute)attInfo.elementAt(1));

        for (int k = 0; k < trainData.numInstances(); k++) {
          Instance inst = new Instance(2);
          inst.setDataset(indices);
          inst.setClassValue(trainData.instance(k).classValue());
          inst.setValue(0,k);
          indices.add(inst);
        }

        Random random = new Random(seed);
        random.setSeed(seed);
        indices.randomize(random);

        random = new Random(seed);
        random.setSeed(seed);
        trainData.randomize(random);

        if (trainData.classAttribute().isNominal()) {
          trainData.stratify(numFolds);
          indices.stratify(numFolds);
        }

        for (int i=0; i<numFolds; i++) {
          Instances train = trainData.trainCV(numFolds,i);
          c.buildClassifier(train);
          testData = trainData.testCV(numFolds,i);
          Instances indicesTest = indices.testCV(numFolds,i);
          for (int j=0; j<testData.numInstances(); j++) {
            Instance instance = testData.instance(j);
            Instance withMissing = (Instance)instance.copy();
            withMissing.setDataset(testData);
            double predValue=((Classifier)c).classifyInstance(instance);
            int idx=(int)indicesTest.instance(j).value(0);
            double trueValue=indicesTest.instance(j).value(1);

            if (testData.classAttribute().isNumeric()) {
              if (Instance.isMissingValue(predValue)) {
                System.out.print(idx + delim + "missing" + delim);
              } else {
                System.out.print(idx + delim + predValue + delim);
              }
              if (instance.classIsMissing()) {
	        System.out.print("missing");
              } else {
                System.out.print(instance.classValue());
              }
	      System.out.print("\n");
            } else {
              if (Instance.isMissingValue(predValue)) {
                System.out.print(idx + delim + "missing" + delim);
              } else {
                System.out.print(idx + delim
	      	  + testData.classAttribute().value((int)predValue) + delim);
              }
              if (Instance.isMissingValue(predValue)) {
                System.out.print("missing" + delim);
              } else {
                System.out.print(c.distributionForInstance(withMissing)[(int)predValue]+delim);
              }
              System.out.print(testData.classAttribute().value((int)trueValue));
              double[] dist=c.distributionForInstance(instance);
              for (int k=0; k<dist.length; k++)
                System.out.print(delim+dist[k]);
            /* Note: the order of class probabilities corresponds
               to the order of class values in the training file */
              System.out.print("\n");
            }
          }
        }
      } else {
        testData.setClassIndex(cIdx);
        c.buildClassifier(trainData);

        for (int i=0; i<testData.numInstances(); i++) {
          Instance instance = testData.instance(i);
          Instance withMissing = (Instance)instance.copy();
          withMissing.setDataset(testData);
          double predValue=((Classifier)c).classifyInstance(instance);
          int idx=i;
          double trueValue=instance.classValue();

          if (testData.classAttribute().isNumeric()) {
            if (Instance.isMissingValue(predValue)) {
                System.out.print(idx + delim + "missing" + delim);
            } else {
                System.out.print(idx + delim + predValue + delim);
            }
            if (instance.classIsMissing()) {
	      System.out.print("missing");
            } else {
              System.out.print(instance.classValue());
            }
	    System.out.print("\n");
          } else {
            if (Instance.isMissingValue(predValue)) {
              System.out.print(idx + delim + "missing" + delim);
            } else {
              System.out.print(idx + delim
	        + testData.classAttribute().value((int)predValue) + delim);
            }
            if (Instance.isMissingValue(predValue)) {
              System.out.print("missing" + delim);
            } else {
              System.out.print(c.
                distributionForInstance(withMissing)[(int)predValue]+delim);
            }
            System.out.print(testData.classAttribute().value((int)trueValue));
            double[] dist=(c.distributionForInstance(instance));
            for (int k=0; k<dist.length; k++)
              System.out.print(delim+dist[k]);
            /* Note: the order of class probabilities corresponds
               to the order of class values in the training file */
            System.out.print("\n");
          }
        }
      }
    } catch (Exception e) {
        System.err.println(e.getMessage());
    }
  }
}
