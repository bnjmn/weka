import weka.core.*;
import weka.classifiers.*;
import weka.classifiers.meta.*;
import weka.classifiers.trees.*;

import java.io.*;

/**
 * A little example for optimizing J48's confidence parameter with 
 * CVPArameterSelection meta-classifier.
 * The class expects a dataset as first parameter, class attribute is
 * assumed to be the last attribute.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class CVParam {
   public static void main(String[] args) throws Exception {
      // load data
      BufferedReader reader = new BufferedReader(new FileReader(args[0]));
      Instances data = new Instances(reader);
      reader.close();
      data.setClassIndex(data.numAttributes() - 1);

      // setup classifier
      CVParameterSelection ps = new CVParameterSelection();
      ps.setClassifier(new J48());
      ps.setNumFolds(5);  // using 5-fold CV
      ps.addCVParameter("C 0.1 0.5 5");

      // build and output best options
      ps.buildClassifier(data);
      System.out.println(Utils.joinOptions(ps.getBestClassifierOptions()));
   }
}
