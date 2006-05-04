/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 * CheckClusterer.java
 * Copyright (C) 2006 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.clusterers;

import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.TestInstances;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;
import weka.core.MultiInstanceCapabilitiesHandler;

import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

/**
 * Class for examining the capabilities and finding problems with 
 * clusterers. If you implement a clusterer using the WEKA.libraries,
 * you should run the checks on it to ensure robustness and correct
 * operation. Passing all the tests of this object does not mean
 * bugs in the clusterer don't exist, but this will help find some
 * common ones. <p/>
 * 
 * Typical usage: <p/>
 * <code>java weka.clusterers.CheckClusterer -W clusterer_name 
 * -- clusterer_options </code><p/>
 * 
 * CheckClusterer reports on the following:
 * <ul>
 *    <li> Clusterer abilities 
 *      <ul>
 *         <li> Possible command line options to the clusterer </li>
 *         <li> Whether the clusterer can predict nominal, numeric, string, 
 *              date or relational class attributes.</li>
 *         <li> Whether the clusterer can handle numeric predictor attributes </li>
 *         <li> Whether the clusterer can handle nominal predictor attributes </li>
 *         <li> Whether the clusterer can handle string predictor attributes </li>
 *         <li> Whether the clusterer can handle date predictor attributes </li>
 *         <li> Whether the clusterer can handle relational predictor attributes </li>
 *         <li> Whether the clusterer can handle multi-instance data </li>
 *         <li> Whether the clusterer can handle missing predictor values </li>
 *         <li> Whether the clusterer can handle instance weights </li>
 *      </ul>
 *    </li>
 *    <li> Correct functioning 
 *      <ul>
 *         <li> Correct initialisation during buildClusterer (i.e. no result
 *              changes when buildClusterer called repeatedly) </li>
 *         <li> Whether the clusterer alters the data pased to it 
 *              (number of instances, instance order, instance weights, etc) </li>
 *      </ul>
 *    </li>
 *    <li> Degenerate cases 
 *      <ul>
 *         <li> building clusterer with zero training instances </li>
 *         <li> all but one predictor attribute values missing </li>
 *         <li> all predictor attribute values missing </li>
 *         <li> all but one class values missing </li>
 *         <li> all class values missing </li>
 *      </ul>
 *    </li>
 * </ul>
 * Running CheckClassifier with the debug option set will output the 
 * training and test datasets for any failed tests.<p/>
 *
 * The <code>weka.clusterers.AbstractClustererTest</code> uses this
 * class to test all the clusterers. Any changes here, have to be 
 * checked in that abstract test class, too. <p/>
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -D
 *  Turn on debugging output.</pre>
 * 
 * <pre> -S
 *  Silent mode - prints nothing to stdout.</pre>
 * 
 * <pre> -N &lt;num&gt;
 *  The number of instances in the datasets (default 40).</pre>
 * 
 * <pre> -W
 *  Full name of the clusterer analyzed.
 *  eg: weka.clusterers.SimpleKMeans</pre>
 * 
 * <pre> 
 * Options specific to clusterer weka.clusterers.SimpleKMeans:
 * </pre>
 * 
 * <pre> -N &lt;num&gt;
 *  number of clusters. (default = 2).</pre>
 * 
 * <pre> -S &lt;num&gt;
 *  random number seed.
 *  (default 10)</pre>
 * 
 <!-- options-end -->
 *
 * Options after -- are passed to the designated clusterer.<p/>
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.3 $
 * @see TestInstances
 */
public class CheckClusterer 
  implements OptionHandler {

  /*
   * Note about test methods:
   * - methods return array of booleans
   * - first index: success or not
   * - second index: acceptable or not (e.g., Exception is OK)
   *
   * FracPete (fracpete at waikato dot ac dot nz)
   */
  
  /*** The clusterer to be examined */
  protected Clusterer m_Clusterer = new SimpleKMeans();
  
  /** The options to be passed to the base clusterer. */
  protected String[] m_ClustererOptions;
  
  /** The results of the analysis as a string */
  protected String m_AnalysisResults;
  
  /** Debugging mode, gives extra output if true */
  protected boolean m_Debug = false;
  
  /** Silent mode, for no output at all to stdout */
  protected boolean m_Silent = false;
  
  /** The number of instances in the datasets */
  protected int m_NumInstances = 40;
  
  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    
    Vector newVector = new Vector(2);
    
    newVector.addElement(new Option(
        "\tTurn on debugging output.",
        "D", 0, "-D"));
    
    newVector.addElement(new Option(
        "\tSilent mode - prints nothing to stdout.",
        "S", 0, "-S"));
    
    newVector.addElement(new Option(
        "\tThe number of instances in the datasets (default 40).",
        "N", 1, "-N <num>"));
    
    newVector.addElement(new Option(
        "\tFull name of the clusterer analyzed.\n"
        +"\teg: weka.clusterers.SimpleKMeans",
        "W", 1, "-W"));
    
    if ((m_Clusterer != null) 
        && (m_Clusterer instanceof OptionHandler)) {
      newVector.addElement(new Option("", "", 0, 
          "\nOptions specific to clusterer "
          + m_Clusterer.getClass().getName()
          + ":"));
      Enumeration enu = ((OptionHandler)m_Clusterer).listOptions();
      while (enu.hasMoreElements())
        newVector.addElement(enu.nextElement());
    }
    
    return newVector.elements();
  }
  
  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -D
   *  Turn on debugging output.</pre>
   * 
   * <pre> -S
   *  Silent mode - prints nothing to stdout.</pre>
   * 
   * <pre> -N &lt;num&gt;
   *  The number of instances in the datasets (default 40).</pre>
   * 
   * <pre> -W
   *  Full name of the clusterer analyzed.
   *  eg: weka.clusterers.SimpleKMeans</pre>
   * 
   * <pre> 
   * Options specific to clusterer weka.clusterers.SimpleKMeans:
   * </pre>
   * 
   * <pre> -N &lt;num&gt;
   *  number of clusters. (default = 2).</pre>
   * 
   * <pre> -S &lt;num&gt;
   *  random number seed.
   *  (default 10)</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    String      tmpStr;
    
    setDebug(Utils.getFlag('D', options));
    
    setSilent(Utils.getFlag('S', options));
    
    tmpStr = Utils.getOption('N', options);
    if (tmpStr.length() != 0)
      setNumInstances(Integer.parseInt(tmpStr));
    else
      setNumInstances(40);
    
    tmpStr = Utils.getOption('W', options);
    if (tmpStr.length() == 0)
      throw new Exception("A clusterer must be specified with the -W option.");
    setClusterer(Clusterer.forName(tmpStr, Utils.partitionOptions(options)));
  }
  
  /**
   * Gets the current settings of the CheckClusterer.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    Vector        result;
    String[]      options;
    int           i;
    
    result = new Vector();
    
    if (getDebug())
      result.add("-D");
    
    if (getSilent())
      result.add("-S");
    
    result.add("-N");
    result.add("" + getNumInstances());
    
    if (getClusterer() != null) {
      result.add("-W");
      result.add(getClusterer().getClass().getName());
    }
    
    if ((m_Clusterer != null) && (m_Clusterer instanceof OptionHandler))
      options = ((OptionHandler) m_Clusterer).getOptions();
    else
      options = new String[0];
    
    if (options.length > 0) {
      result.add("--");
      for (i = 0; i < options.length; i++)
        result.add(options[i]);
    }
    
    return (String[]) result.toArray(new String[result.size()]);
  }
  
  /**
   * Begin the tests, reporting results to System.out
   */
  public void doTests() {
    
    if (getClusterer() == null) {
      println("\n=== No clusterer set ===");
      return;
    }
    println("\n=== Check on Clusterer: "
        + getClusterer().getClass().getName()
        + " ===\n");
    
    // Start tests
    canTakeOptions();
    boolean weightedInstancesHandler = weightedInstancesHandler()[0];
    boolean multiInstanceHandler = multiInstanceHandler()[0];
    runTests(weightedInstancesHandler, multiInstanceHandler);
  }
  
  /**
   * Set debugging mode
   *
   * @param debug true if debug output should be printed
   */
  public void setDebug(boolean debug) {
    m_Debug = debug;
    // disable silent mode, if necessary
    if (getDebug())
      setSilent(false);
  }
  
  /**
   * Get whether debugging is turned on
   *
   * @return true if debugging output is on
   */
  public boolean getDebug() {
    return m_Debug;
  }
  
  /**
   * Set slient mode, i.e., no output at all to stdout
   *
   * @param value whether silent mode is active or not
   */
  public void setSilent(boolean value) {
    m_Silent = value;
  }
  
  /**
   * Get whether silent mode is turned on
   *
   * @return true if silent mode is on
   */
  public boolean getSilent() {
    return m_Silent;
  }
  
  /**
   * Sets the number of instances to use in the datasets (some clusterers
   * might require more instances).
   *
   * @param value the number of instances to use
   */
  public void setNumInstances(int value) {
    m_NumInstances = value;
  }
  
  /**
   * Gets the current number of instances to use for the datasets.
   *
   * @return the number of instances
   */
  public int getNumInstances() {
    return m_NumInstances;
  }
  
  /**
   * Set the clusterer for testing. 
   *
   * @param newClusterer the Clusterer to use.
   */
  public void setClusterer(Clusterer newClusterer) {
    m_Clusterer = newClusterer;
  }
  
  /**
   * Get the clusterer used as the clusterer
   *
   * @return the clusterer used as the clusterer
   */
  public Clusterer getClusterer() {
    return m_Clusterer;
  }
  
  /**
   * prints the given message to stdout, if not silent mode
   * 
   * @param msg         the text to print to stdout
   */
  protected void print(Object msg) {
    if (!getSilent())
      System.out.print(msg);
  }
  
  /**
   * prints the given message (+ LF) to stdout, if not silent mode
   * 
   * @param msg         the message to println to stdout
   */
  protected void println(Object msg) {
    print(msg + "\n");
  }
  
  /**
   * prints a LF to stdout, if not silent mode
   */
  protected void println() {
    print("\n");
  }
  
  /**
   * Run a battery of tests
   *
   * @param weighted true if the clusterer says it handles weights
   * @param multiInstance true if the clusterer is a multi-instance clusterer
   */
  protected void runTests(boolean weighted, boolean multiInstance) {
    
    boolean PNom = canPredict(true,  false, false, false, false, multiInstance)[0];
    boolean PNum = canPredict(false, true,  false, false, false, multiInstance)[0];
    boolean PStr = canPredict(false, false, true,  false, false, multiInstance)[0];
    boolean PDat = canPredict(false, false, false, true,  false, multiInstance)[0];
    boolean PRel;
    if (!multiInstance)
      PRel = canPredict(false, false, false, false,  true, multiInstance)[0];
    else
      PRel = false;

    if (PNom || PNum || PStr || PDat || PRel) {
      if (weighted)
        instanceWeights(PNom, PNum, PStr, PDat, PRel, multiInstance);
      
      canHandleZeroTraining(PNom, PNum, PStr, PDat, PRel, multiInstance);
      boolean handleMissingPredictors = canHandleMissing(PNom, PNum, PStr, PDat, PRel, 
          multiInstance, true, 20)[0];
      if (handleMissingPredictors)
        canHandleMissing(PNom, PNum, PStr, PDat, PRel, multiInstance, true, 100);
      
      correctBuildInitialisation(PNom, PNum, PStr, PDat, PRel, multiInstance);
      datasetIntegrity(PNom, PNum, PStr, PDat, PRel, multiInstance, handleMissingPredictors);
    }
  }
  
  /**
   * Checks whether the scheme can take command line options.
   *
   * @return index 0 is true if the clusterer can take options
   */
  protected boolean[] canTakeOptions() {
    
    boolean[] result = new boolean[2];
    
    print("options...");
    if (m_Clusterer instanceof OptionHandler) {
      println("yes");
      if (m_Debug) {
        println("\n=== Full report ===");
        Enumeration enu = ((OptionHandler)m_Clusterer).listOptions();
        while (enu.hasMoreElements()) {
          Option option = (Option) enu.nextElement();
          print(option.synopsis() + "\n" 
              + option.description() + "\n");
        }
        println("\n");
      }
      result[0] = true;
    }
    else {
      println("no");
      result[0] = false;
    }
    
    return result;
  }
  
  /**
   * Checks whether the scheme says it can handle instance weights.
   *
   * @return true if the clusterer handles instance weights
   */
  protected boolean[] weightedInstancesHandler() {
    
    boolean[] result = new boolean[2];
    
    print("weighted instances clusterer...");
    if (m_Clusterer instanceof WeightedInstancesHandler) {
      println("yes");
      result[0] = true;
    }
    else {
      println("no");
      result[0] = false;
    }
    
    return result;
  }
  
  /**
   * Checks whether the scheme handles multi-instance data.
   * 
   * @return true if the clusterer handles multi-instance data
   */
  protected boolean[] multiInstanceHandler() {
    boolean[] result = new boolean[2];
    
    print("multi-instance clusterer...");
    if (m_Clusterer instanceof MultiInstanceCapabilitiesHandler) {
      println("yes");
      result[0] = true;
    }
    else {
      println("no");
      result[0] = false;
    }
    
    return result;
  }
  
  /**
   * Checks basic prediction of the scheme, for simple non-troublesome
   * datasets.
   *
   * @param nominalPredictor if true use nominal predictor attributes
   * @param numericPredictor if true use numeric predictor attributes
   * @param stringPredictor if true use string predictor attributes
   * @param datePredictor if true use date predictor attributes
   * @param relationalPredictor if true use relational predictor attributes
   * @param multiInstance whether multi-instance is needed
   * @return index 0 is true if the test was passed, index 1 is true if test 
   *         was acceptable
   */
  protected boolean[] canPredict(
      boolean nominalPredictor,
      boolean numericPredictor, 
      boolean stringPredictor, 
      boolean datePredictor,
      boolean relationalPredictor,
      boolean multiInstance) {
    
    print("basic predict");
    printAttributeSummary(
        nominalPredictor, numericPredictor, stringPredictor, datePredictor, relationalPredictor, multiInstance);
    print("...");
    FastVector accepts = new FastVector();
    accepts.addElement("nominal");
    accepts.addElement("numeric");
    accepts.addElement("string");
    accepts.addElement("date");
    accepts.addElement("relational");
    accepts.addElement("multi-instance");
    accepts.addElement("not in classpath");
    int numTrain = getNumInstances(), numTest = getNumInstances(), 
    missingLevel = 0;
    boolean predictorMissing = false;
    
    return runBasicTest(nominalPredictor, numericPredictor, stringPredictor, 
        datePredictor, relationalPredictor, 
        multiInstance,
        missingLevel, predictorMissing, 
        numTrain, numTest, 
        accepts);
  }
  
  /**
   * Checks whether the scheme can handle zero training instances.
   *
   * @param nominalPredictor if true use nominal predictor attributes
   * @param numericPredictor if true use numeric predictor attributes
   * @param stringPredictor if true use string predictor attributes
   * @param datePredictor if true use date predictor attributes
   * @param relationalPredictor if true use relational predictor attributes
   * @param multiInstance whether multi-instance is needed
   * @return index 0 is true if the test was passed, index 1 is true if test 
   *         was acceptable
   */
  protected boolean[] canHandleZeroTraining(
      boolean nominalPredictor,
      boolean numericPredictor, 
      boolean stringPredictor, 
      boolean datePredictor,
      boolean relationalPredictor,
      boolean multiInstance) {
    
    print("handle zero training instances");
    printAttributeSummary(
        nominalPredictor, numericPredictor, stringPredictor, datePredictor, relationalPredictor, multiInstance);
    print("...");
    FastVector accepts = new FastVector();
    accepts.addElement("train");
    accepts.addElement("value");
    int numTrain = 0, numTest = getNumInstances(), 
    missingLevel = 0;
    boolean predictorMissing = false;
    
    return runBasicTest(
              nominalPredictor, numericPredictor, stringPredictor, 
              datePredictor, relationalPredictor, 
              multiInstance,
              missingLevel, predictorMissing,
              numTrain, numTest, 
              accepts);
  }
  
  /**
   * Checks whether the scheme correctly initialises models when 
   * buildClusterer is called. This test calls buildClusterer with
   * one training dataset and records performance on a test set. 
   * buildClusterer is then called on a training set with different
   * structure, and then again with the original training set. The
   * performance on the test set is compared with the original results
   * and any performance difference noted as incorrect build initialisation.
   *
   * @param nominalPredictor if true use nominal predictor attributes
   * @param numericPredictor if true use numeric predictor attributes
   * @param stringPredictor if true use string predictor attributes
   * @param datePredictor if true use date predictor attributes
   * @param relationalPredictor if true use relational predictor attributes
   * @param multiInstance whether multi-instance is needed
   * @return index 0 is true if the test was passed
   */
  protected boolean[] correctBuildInitialisation(
      boolean nominalPredictor,
      boolean numericPredictor, 
      boolean stringPredictor, 
      boolean datePredictor,
      boolean relationalPredictor,
      boolean multiInstance) {

    boolean[] result = new boolean[2];
    
    print("correct initialisation during buildClusterer");
    printAttributeSummary(
        nominalPredictor, numericPredictor, stringPredictor, datePredictor, relationalPredictor, multiInstance);
    print("...");
    int numTrain = getNumInstances(), numTest = getNumInstances(), missingLevel = 0;
    boolean predictorMissing = false;
    
    Instances train1 = null;
    Instances test1 = null;
    Instances train2 = null;
    Instances test2 = null;
    Clusterer clusterer = null;
    ClusterEvaluation evaluation1A = null;
    ClusterEvaluation evaluation1B = null;
    ClusterEvaluation evaluation2 = null;
    boolean built = false;
    int stage = 0;
    try {
      
      // Make two sets of train/test splits with different 
      // numbers of attributes
      train1 = makeTestDataset(42, numTrain, 
                               nominalPredictor ? 2 : 0,
                               numericPredictor ? 1 : 0, 
                               stringPredictor ? 1 : 0, 
                               datePredictor ? 1 : 0, 
                               relationalPredictor ? 1 : 0, 
                               multiInstance);
      train2 = makeTestDataset(84, numTrain, 
                               nominalPredictor ? 3 : 0,
                               numericPredictor ? 2 : 0, 
                               stringPredictor ? 1 : 0, 
                               datePredictor ? 1 : 0, 
                               relationalPredictor ? 1 : 0, 
                               multiInstance);
      test1 = makeTestDataset(24, numTest,
                              nominalPredictor ? 2 : 0,
                              numericPredictor ? 1 : 0, 
                              stringPredictor ? 1 : 0, 
                              datePredictor ? 1 : 0, 
                              relationalPredictor ? 1 : 0, 
                              multiInstance);
      test2 = makeTestDataset(48, numTest,
                              nominalPredictor ? 3 : 0,
                              numericPredictor ? 2 : 0, 
                              stringPredictor ? 1 : 0, 
                              datePredictor ? 1 : 0, 
                              relationalPredictor ? 1 : 0, 
                              multiInstance);
      if (nominalPredictor && !multiInstance) {
        train1.deleteAttributeAt(0);
        test1.deleteAttributeAt(0);
        train2.deleteAttributeAt(0);
        test2.deleteAttributeAt(0);
      }
      if (missingLevel > 0) {
        addMissing(train1, missingLevel, predictorMissing);
        addMissing(test1, Math.min(missingLevel,50), predictorMissing);
        addMissing(train2, missingLevel, predictorMissing);
        addMissing(test2, Math.min(missingLevel,50), predictorMissing);
      }
      
      clusterer = Clusterer.makeCopies(getClusterer(), 1)[0];
      evaluation1A = new ClusterEvaluation();
      evaluation1B = new ClusterEvaluation();
      evaluation2 = new ClusterEvaluation();
    } catch (Exception ex) {
      throw new Error("Error setting up for tests: " + ex.getMessage());
    }
    try {
      stage = 0;
      clusterer.buildClusterer(train1);
      evaluation1A.setClusterer(clusterer);
      evaluation1A.evaluateClusterer(train1);
      built = true;
      
      stage = 1;
      built = false;
      clusterer.buildClusterer(train2);
      evaluation2.setClusterer(clusterer);
      evaluation2.evaluateClusterer(train2);
      built = true;
      
      stage = 2;
      built = false;
      clusterer.buildClusterer(train1);
      evaluation1B.setClusterer(clusterer);
      evaluation1B.evaluateClusterer(train1);
      built = true;
      
      stage = 3;
      if (!evaluation1A.equals(evaluation1B)) {
        if (m_Debug) {
          println("\n=== Full report ===\n");
          println("First buildClusterer()");
          println(evaluation1A.clusterResultsToString() + "\n\n");
          println("Second buildClusterer()");
          println(evaluation1B.clusterResultsToString() + "\n\n");
        }
        throw new Exception("Results differ between buildClusterer calls");
      }
      println("yes");
      result[0] = true;
      
      if (false && m_Debug) {
        println("\n=== Full report ===\n");
        println("First buildClusterer()");
        println(evaluation1A.clusterResultsToString() + "\n\n");
        println("Second buildClusterer()");
        println(evaluation1B.clusterResultsToString() + "\n\n");
      }
    } 
    catch (Exception ex) {
      println("no");
      result[0] = false;
      if (m_Debug) {
        println("\n=== Full Report ===");
        print("Problem during");
        if (built) {
          print(" testing");
        } else {
          print(" training");
        }
        switch (stage) {
          case 0:
            print(" of dataset 1");
            break;
          case 1:
            print(" of dataset 2");
            break;
          case 2:
            print(" of dataset 1 (2nd build)");
            break;
          case 3:
            print(", comparing results from builds of dataset 1");
            break;	  
        }
        println(": " + ex.getMessage() + "\n");
        println("here are the datasets:\n");
        println("=== Train1 Dataset ===\n"
            + train1.toString() + "\n");
        println("=== Test1 Dataset ===\n"
            + test1.toString() + "\n\n");
        println("=== Train2 Dataset ===\n"
            + train2.toString() + "\n");
        println("=== Test2 Dataset ===\n"
            + test2.toString() + "\n\n");
      }
    }
    
    return result;
  }
  
  /**
   * Checks basic missing value handling of the scheme. If the missing
   * values cause an exception to be thrown by the scheme, this will be
   * recorded.
   *
   * @param nominalPredictor if true use nominal predictor attributes
   * @param numericPredictor if true use numeric predictor attributes
   * @param stringPredictor if true use string predictor attributes
   * @param datePredictor if true use date predictor attributes
   * @param relationalPredictor if true use relational predictor attributes
   * @param multiInstance whether multi-instance is needed
   * @param predictorMissing true if the missing values may be in 
   * the predictors
   * @param missingLevel the percentage of missing values
   * @return index 0 is true if the test was passed, index 1 is true if test 
   *         was acceptable
   */
  protected boolean[] canHandleMissing(
      boolean nominalPredictor,
      boolean numericPredictor, 
      boolean stringPredictor, 
      boolean datePredictor,
      boolean relationalPredictor,
      boolean multiInstance,
      boolean predictorMissing,
      int missingLevel) {
    
    if (missingLevel == 100)
      print("100% ");
    print("missing");
    if (predictorMissing) {
      print(" predictor");
    }
    print(" values");
    printAttributeSummary(
        nominalPredictor, numericPredictor, stringPredictor, datePredictor, relationalPredictor, multiInstance);
    print("...");
    FastVector accepts = new FastVector();
    accepts.addElement("missing");
    accepts.addElement("value");
    accepts.addElement("train");
    int numTrain = getNumInstances(), numTest = getNumInstances();
    
    return runBasicTest(nominalPredictor, numericPredictor, stringPredictor, 
        datePredictor, relationalPredictor, 
        multiInstance,
        missingLevel, predictorMissing,
        numTrain, numTest, 
        accepts);
  }
  
  /**
   * Checks whether the clusterer can handle instance weights.
   * This test compares the clusterer performance on two datasets
   * that are identical except for the training weights. If the 
   * results change, then the clusterer must be using the weights. It
   * may be possible to get a false positive from this test if the 
   * weight changes aren't significant enough to induce a change
   * in clusterer performance (but the weights are chosen to minimize
   * the likelihood of this).
   *
   * @param nominalPredictor if true use nominal predictor attributes
   * @param numericPredictor if true use numeric predictor attributes
   * @param stringPredictor if true use string predictor attributes
   * @param datePredictor if true use date predictor attributes
   * @param relationalPredictor if true use relational predictor attributes
   * @param multiInstance whether multi-instance is needed
   * @return index 0 true if the test was passed
   */
  protected boolean[] instanceWeights(
      boolean nominalPredictor,
      boolean numericPredictor, 
      boolean stringPredictor, 
      boolean datePredictor,
      boolean relationalPredictor,
      boolean multiInstance) {
    
    print("clusterer uses instance weights");
    printAttributeSummary(
        nominalPredictor, numericPredictor, stringPredictor, datePredictor, relationalPredictor, multiInstance);
    print("...");
    int numTrain = 2*getNumInstances(), numTest = getNumInstances(), missingLevel = 0;
    boolean predictorMissing = false;
    
    boolean[] result = new boolean[2];
    Instances train = null;
    Instances test = null;
    Clusterer [] clusterers = null;
    ClusterEvaluation evaluationB = null;
    ClusterEvaluation evaluationI = null;
    boolean built = false;
    boolean evalFail = false;
    try {
      train = makeTestDataset(42, numTrain, 
                              nominalPredictor ? 3 : 0,
                              numericPredictor ? 2 : 0, 
                              stringPredictor ? 1 : 0, 
                              datePredictor ? 1 : 0, 
                              relationalPredictor ? 1 : 0, 
                              multiInstance);
      test = makeTestDataset(24, numTest,
                             nominalPredictor ? 3 : 0,
                             numericPredictor ? 2 : 0, 
                             stringPredictor ? 1 : 0, 
                             datePredictor ? 1 : 0, 
                             relationalPredictor ? 1 : 0, 
                             multiInstance);
      if (nominalPredictor && !multiInstance) {
        train.deleteAttributeAt(0);
        test.deleteAttributeAt(0);
      }
      if (missingLevel > 0) {
        addMissing(train, missingLevel, predictorMissing);
        addMissing(test, Math.min(missingLevel, 50), predictorMissing);
      }
      clusterers = Clusterer.makeCopies(getClusterer(), 2);
      evaluationB = new ClusterEvaluation();
      evaluationI = new ClusterEvaluation();
      clusterers[0].buildClusterer(train);
    } catch (Exception ex) {
      throw new Error("Error setting up for tests: " + ex.getMessage());
    }
    try {
      
      // Now modify instance weights and re-built/test
      for (int i = 0; i < train.numInstances(); i++) {
        train.instance(i).setWeight(0);
      }
      Random random = new Random(1);
      for (int i = 0; i < train.numInstances() / 2; i++) {
        int inst = Math.abs(random.nextInt()) % train.numInstances();
        int weight = Math.abs(random.nextInt()) % 10 + 1;
        train.instance(inst).setWeight(weight);
      }
      clusterers[1].buildClusterer(train);
      built = true;
      if (evaluationB.equals(evaluationI)) {
        //	println("no");
        evalFail = true;
        throw new Exception("evalFail");
      }
      
      println("yes");
      result[0] = true;
    } catch (Exception ex) {
      println("no");
      result[0] = false;
      
      if (m_Debug) {
        println("\n=== Full Report ===");
        
        if (evalFail) {
          println("Results don't differ between non-weighted and "
              + "weighted instance models.");
          println("Here are the results:\n");
          println("\nboth methods\n");
          println(evaluationB.clusterResultsToString());
        } else {
          print("Problem during");
          if (built) {
            print(" testing");
          } else {
            print(" training");
          }
          println(": " + ex.getMessage() + "\n");
        }
        println("Here are the datasets:\n");
        println("=== Train Dataset ===\n"
            + train.toString() + "\n");
        println("=== Train Weights ===\n");
        for (int i = 0; i < train.numInstances(); i++) {
          println(" " + (i + 1) 
              + "    " + train.instance(i).weight());
        }
        println("=== Test Dataset ===\n"
            + test.toString() + "\n\n");	
        println("(test weights all 1.0\n");
      }
    }
    
    return result;
  }
  
  /**
   * Checks whether the scheme alters the training dataset during
   * training. If the scheme needs to modify the training
   * data it should take a copy of the training data. Currently checks
   * for changes to header structure, number of instances, order of
   * instances, instance weights.
   *
   * @param nominalPredictor if true use nominal predictor attributes
   * @param numericPredictor if true use numeric predictor attributes
   * @param stringPredictor if true use string predictor attributes
   * @param datePredictor if true use date predictor attributes
   * @param relationalPredictor if true use relational predictor attributes
   * @param multiInstance whether multi-instance is needed
   * @param predictorMissing true if we know the clusterer can handle
   * (at least) moderate missing predictor values
   * @return index 0 is true if the test was passed
   */
  protected boolean[] datasetIntegrity(
      boolean nominalPredictor,
      boolean numericPredictor, 
      boolean stringPredictor, 
      boolean datePredictor,
      boolean relationalPredictor,
      boolean multiInstance,
      boolean predictorMissing) {
    
    print("clusterer doesn't alter original datasets");
    printAttributeSummary(
        nominalPredictor, numericPredictor, stringPredictor, datePredictor, relationalPredictor, multiInstance);
    print("...");
    int numTrain = getNumInstances(), numTest = getNumInstances(), missingLevel = 20;
    
    boolean[] result = new boolean[2];
    Instances train = null;
    Instances test = null;
    Clusterer clusterer = null;
    boolean built = false;
    try {
      train = makeTestDataset(42, numTrain, 
                              nominalPredictor ? 2 : 0,
                              numericPredictor ? 1 : 0, 
                              stringPredictor ? 1 : 0, 
                              datePredictor ? 1 : 0, 
                              relationalPredictor ? 1 : 0, 
                              multiInstance);
      test = makeTestDataset(24, numTest,
                             nominalPredictor ? 2 : 0,
                             numericPredictor ? 1 : 0, 
                             stringPredictor ? 1 : 0, 
                             datePredictor ? 1 : 0, 
                             relationalPredictor ? 1 : 0, 
                             multiInstance);
      if (nominalPredictor && !multiInstance) {
        train.deleteAttributeAt(0);
        test.deleteAttributeAt(0);
      }
      if (missingLevel > 0) {
        addMissing(train, missingLevel, predictorMissing);
        addMissing(test, Math.min(missingLevel, 50), predictorMissing);
      }
      clusterer = Clusterer.makeCopies(getClusterer(), 1)[0];
    } catch (Exception ex) {
      throw new Error("Error setting up for tests: " + ex.getMessage());
    }
    try {
      Instances trainCopy = new Instances(train);
      Instances testCopy = new Instances(test);
      clusterer.buildClusterer(trainCopy);
      compareDatasets(train, trainCopy);
      built = true;
      compareDatasets(test, testCopy);
      
      println("yes");
      result[0] = true;
    } catch (Exception ex) {
      println("no");
      result[0] = false;
      
      if (m_Debug) {
        println("\n=== Full Report ===");
        print("Problem during");
        if (built) {
          print(" testing");
        } else {
          print(" training");
        }
        println(": " + ex.getMessage() + "\n");
        println("Here are the datasets:\n");
        println("=== Train Dataset ===\n"
            + train.toString() + "\n");
        println("=== Test Dataset ===\n"
            + test.toString() + "\n\n");
      }
    }
    
    return result;
  }
  
  /**
   * Runs a text on the datasets with the given characteristics.
   * 
   * @param nominalPredictor if true use nominal predictor attributes
   * @param numericPredictor if true use numeric predictor attributes
   * @param stringPredictor if true use string predictor attributes
   * @param datePredictor if true use date predictor attributes
   * @param relationalPredictor if true use relational predictor attributes
   * @param multiInstance whether multi-instance is needed
   * @param missingLevel the percentage of missing values
   * @param predictorMissing true if the missing values may be in 
   * the predictors
   * @param numTrain the number of instances in the training set
   * @param numTest the number of instaces in the test set
   * @param accepts the acceptable string in an exception
   * @return index 0 is true if the test was passed, index 1 is true if test 
   *         was acceptable
   */
  protected boolean[] runBasicTest(boolean nominalPredictor,
      boolean numericPredictor, 
      boolean stringPredictor,
      boolean datePredictor,
      boolean relationalPredictor,
      boolean multiInstance,
      int missingLevel,
      boolean predictorMissing,
      int numTrain,
      int numTest,
      FastVector accepts) {
    
    boolean[] result = new boolean[2];
    Instances train = null;
    Instances test = null;
    Clusterer clusterer = null;
    boolean built = false;
    try {
      train = makeTestDataset(42, numTrain, 
                              nominalPredictor     ? 2 : 0,
                              numericPredictor     ? 1 : 0, 
                              stringPredictor      ? 1 : 0,
                              datePredictor        ? 1 : 0,
                              relationalPredictor  ? 1 : 0,
                              multiInstance);
      test = makeTestDataset(24, numTest,
                             nominalPredictor     ? 2 : 0,
                             numericPredictor     ? 1 : 0, 
                             stringPredictor      ? 1 : 0,
                             datePredictor        ? 1 : 0,
                             relationalPredictor  ? 1 : 0,
                             multiInstance);
      if (nominalPredictor && !multiInstance) {
        train.deleteAttributeAt(0);
        test.deleteAttributeAt(0);
      }
      if (missingLevel > 0) {
        addMissing(train, missingLevel, predictorMissing);
        addMissing(test, Math.min(missingLevel, 50), predictorMissing);
      }
      clusterer = Clusterer.makeCopies(getClusterer(), 1)[0];
    } catch (Exception ex) {
      ex.printStackTrace();
      throw new Error("Error setting up for tests: " + ex.getMessage());
    }
    try {
      clusterer.buildClusterer(train);
      built = true;
      
      println("yes");
      result[0] = true;
    } 
    catch (Exception ex) {
      boolean acceptable = false;
      String msg = ex.getMessage().toLowerCase();
      for (int i = 0; i < accepts.size(); i++) {
        if (msg.indexOf((String)accepts.elementAt(i)) >= 0) {
          acceptable = true;
        }
      }
      
      println("no" + (acceptable ? " (OK error message)" : ""));
      result[1] = acceptable;
      
      if (m_Debug) {
        println("\n=== Full Report ===");
        print("Problem during");
        if (built) {
          print(" testing");
        } else {
          print(" training");
        }
        println(": " + ex.getMessage() + "\n");
        if (!acceptable) {
          if (accepts.size() > 0) {
            print("Error message doesn't mention ");
            for (int i = 0; i < accepts.size(); i++) {
              if (i != 0) {
                print(" or ");
              }
              print('"' + (String)accepts.elementAt(i) + '"');
            }
          }
          println("here are the datasets:\n");
          println("=== Train Dataset ===\n"
              + train.toString() + "\n");
          println("=== Test Dataset ===\n"
              + test.toString() + "\n\n");
        }
      }
    }
    
    return result;
  }
  
  /**
   * Compare two datasets to see if they differ.
   *
   * @param data1 one set of instances
   * @param data2 the other set of instances
   * @throws Exception if the datasets differ
   */
  protected void compareDatasets(Instances data1, Instances data2)
  throws Exception {
    if (!data2.equalHeaders(data1)) {
      throw new Exception("header has been modified");
    }
    if (!(data2.numInstances() == data1.numInstances())) {
      throw new Exception("number of instances has changed");
    }
    for (int i = 0; i < data2.numInstances(); i++) {
      Instance orig = data1.instance(i);
      Instance copy = data2.instance(i);
      for (int j = 0; j < orig.numAttributes(); j++) {
        if (orig.isMissing(j)) {
          if (!copy.isMissing(j)) {
            throw new Exception("instances have changed");
          }
        } else if (orig.value(j) != copy.value(j)) {
          throw new Exception("instances have changed");
        }
        if (orig.weight() != copy.weight()) {
          throw new Exception("instance weights have changed");
        }	  
      }
    }
  }
  
  /**
   * Add missing values to a dataset.
   *
   * @param data the instances to add missing values to
   * @param level the level of missing values to add (if positive, this
   * is the probability that a value will be set to missing, if negative
   * all but one value will be set to missing (not yet implemented))
   * @param predictorMissing if true, predictor attributes will be modified
   */
  protected void addMissing(Instances data, int level, boolean predictorMissing) {
    
    Random random = new Random(1);
    for (int i = 0; i < data.numInstances(); i++) {
      Instance current = data.instance(i);
      for (int j = 0; j < data.numAttributes(); j++) {
        if (predictorMissing) {
          if (Math.abs(random.nextInt()) % 100 < level)
            current.setMissing(j);
        }
      }
    }
  }
  
  /**
   * Make a simple set of instances with variable position of the class 
   * attribute, which can later be modified for use in specific tests.
   *
   * @param seed the random number seed
   * @param numInstances the number of instances to generate
   * @param numNominal the number of nominal attributes
   * @param numNumeric the number of numeric attributes
   * @param numString the number of string attributes
   * @param numDate the number of date attributes
   * @param numRelational the number of relational attributes
   * @param multiInstance whether the dataset should a multi-instance dataset
   * @return the test dataset
   * @throws Exception if the dataset couldn't be generated
   * @see TestInstances#CLASS_IS_LAST
   */
  protected Instances makeTestDataset(int seed, int numInstances, 
                                      int numNominal, int numNumeric, 
                                      int numString, int numDate,
                                      int numRelational,
                                      boolean multiInstance)
  throws Exception {
    
    TestInstances dataset = new TestInstances();
    
    dataset.setSeed(seed);
    dataset.setNumInstances(numInstances);
    dataset.setNumNominal(numNominal);
    dataset.setNumNumeric(numNumeric);
    dataset.setNumString(numString);
    dataset.setNumDate(numDate);
    dataset.setNumRelational(numRelational);
    dataset.setClassIndex(TestInstances.NO_CLASS);
    dataset.setMultiInstance(multiInstance);
    
    return dataset.generate();
  }
  
  /**
   * Print out a short summary string for the dataset characteristics
   *
   * @param nominalPredictor true if nominal predictor attributes are present
   * @param numericPredictor true if numeric predictor attributes are present
   * @param stringPredictor true if string predictor attributes are present
   * @param datePredictor true if date predictor attributes are present
   * @param relationalPredictor true if relational predictor attributes are present
   * @param multiInstance whether multi-instance is needed
   */
  protected void printAttributeSummary(boolean nominalPredictor, 
                                       boolean numericPredictor, 
                                       boolean stringPredictor, 
                                       boolean datePredictor, 
                                       boolean relationalPredictor, 
                                       boolean multiInstance) {
    
    String str = "";

    if (numericPredictor)
      str += "numeric";
    
    if (nominalPredictor) {
      if (str.length() > 0)
        str += " & ";
      str += "nominal";
    }
    
    if (stringPredictor) {
      if (str.length() > 0)
        str += " & ";
      str += "string";
    }
    
    if (datePredictor) {
      if (str.length() > 0)
        str += " & ";
      str += "date";
    }
    
    if (relationalPredictor) {
      if (str.length() > 0)
        str += " & ";
      str += "relational";
    }
    
    str = " (" + str + " predictors)";
    
    print(str);
  }
  
  /**
   * Test method for this class
   * 
   * @param args the commandline options
   */
  public static void main(String [] args) {
    try {
      CheckClusterer check = new CheckClusterer();
      
      try {
        check.setOptions(args);
        Utils.checkForRemainingOptions(args);
      } catch (Exception ex) {
        String result = ex.getMessage() + "\n\n" + check.getClass().getName().replaceAll(".*\\.", "") + " Options:\n\n";
        Enumeration enu = check.listOptions();
        while (enu.hasMoreElements()) {
          Option option = (Option) enu.nextElement();
          result += option.synopsis() + "\n" + option.description() + "\n";
        }
        throw new Exception(result);
      }
      
      check.doTests();
    } catch (Exception ex) {
      System.err.println(ex.getMessage());
    }
  }
}
