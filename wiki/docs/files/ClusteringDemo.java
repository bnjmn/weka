import weka.core.Instances;
import weka.clusterers.DensityBasedClusterer;
import weka.clusterers.EM;
import weka.clusterers.ClusterEvaluation;

import java.io.BufferedReader;
import java.io.FileReader;

/**
 * An example class that shows the use of Weka clusterers from Java.
 *
 * @author  FracPete
 */

public class ClusteringDemo {
  /**
   * Run clusterers
   *
   * @param filename      the name of the ARFF file to run on
   */
  public ClusteringDemo(String filename) throws Exception {
    ClusterEvaluation eval;
    Instances               data;
    String[]                options;
    DensityBasedClusterer   cl;    

    data = new Instances(new BufferedReader(new FileReader(filename)));
    
    // normal
    System.out.println("\n--> normal");
    options    = new String[2];
    options[0] = "-t";
    options[1] = filename;
    System.out.println(
        ClusterEvaluation.evaluateClusterer(new EM(), options));
    
    // manual call
    System.out.println("\n--> manual");
    cl   = new EM();
    cl.buildClusterer(data);
    eval = new ClusterEvaluation();
    eval.setClusterer(cl);
    eval.evaluateClusterer(new Instances(data));
    System.out.println("# of clusters: " + eval.getNumClusters());

    // density based
    System.out.println("\n--> density (CV)");
    cl   = new EM();
    eval = new ClusterEvaluation();
    eval.setClusterer(cl);
    eval.crossValidateModel(
           cl, data, 10, data.getRandomNumberGenerator(1));
    System.out.println("# of clusters: " + eval.getNumClusters());
  }

  /**
   * usage:
   *   ClusteringDemo arff-file
   */
  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.out.println("usage: " + ClusteringDemo.class.getName() + " <arff-file>");
      System.exit(1);
    }

    new ClusteringDemo(args[0]);
  }
}
