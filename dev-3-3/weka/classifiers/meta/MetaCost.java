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
 *    MetaCost.java
 *    Copyright (C) 1999 Intelligenesis Corp.
 *
 */

package weka.classifiers;

import java.io.*;
import java.util.*;
import weka.core.*;
import weka.filters.Filter;


/**
 * This metaclassifier makes its base classifier cost-sensitive using the
 * method specified in <p>
 *
 * Pedro Domingos (1999). <i>MetaCost: A general method for making classifiers
 * cost-sensitive</i>, Proceedings of the Fifth International Conference on 
 * Knowledge Discovery and Data Mining, pp. 155-164. Also available online at
 * <a href="http://www.cs.washington.edu/homes/pedrod/kdd99.ps.gz">
 * http://www.cs.washington.edu/homes/pedrod/kdd99.ps.gz</a>. <p>
 *
 * This classifier should produce similar results to one created by
 * passing the base learner to Bagging, which is in turn passed to a
 * CostSensitiveClassifier operating on minimum expected cost. The difference
 * is that MetaCost produces a single cost-sensitive classifier of the
 * base learner, giving the benefits of fast classification and interpretable
 * output (if the base learner itself is interpretable). This implementation 
 * uses all bagging iterations when reclassifying training data (the MetaCost
 * paper reports a marginal improvement when only those iterations containing
 * each training instance are used in reclassifying that instance). <p>
 *
 * Valid options are:<p>
 *
 * -W classname <br>
 * Specify the full class name of a classifier (required).<p>
 *
 * -C cost file <br>
 * File name of a cost matrix to use. If this is not supplied, a cost
 * matrix will be loaded on demand. The name of the on-demand file
 * is the relation name of the training data plus ".cost", and the
 * path to the on-demand file is specified with the -D option.<p>
 *
 * -D directory <br>
 * Name of a directory to search for cost files when loading costs on demand
 * (default current directory). <p>
 *
 * -I num <br>
 * Set the number of bagging iterations (default 10). <p>
 *
 * -S seed <br>
 * Random number seed used when reweighting by resampling (default 1).<p>
 *
 * -P num <br>
 * Size of each bag, as a percentage of the training size (default 100). <p>
 *
 * Options after -- are passed to the designated classifier.<p>
 *
 * @author Len Trigg (len@intelligenesis.net)
 * @version $Revision: 1.7 $ 
 */
public class MetaCost extends Classifier
  implements OptionHandler {

  /* Specify possible sources of the cost matrix */
  public static final int MATRIX_ON_DEMAND = 1;
  public static final int MATRIX_SUPPLIED = 2;
  public static final Tag [] TAGS_MATRIX_SOURCE = {
    new Tag(MATRIX_ON_DEMAND, "Load cost matrix on demand"),
    new Tag(MATRIX_SUPPLIED, "Use explicit cost matrix")
  };

  /** Indicates the current cost matrix source */
  protected int m_MatrixSource = MATRIX_ON_DEMAND;

  /** 
   * The directory used when loading cost files on demand, null indicates
   * current directory 
   */
  protected File m_OnDemandDirectory = new File(System.getProperty("user.dir"));

  /** The name of the cost file, for command line options */
  protected String m_CostFile;

  /** The classifier */
  protected Classifier m_Classifier = new weka.classifiers.ZeroR();

  /** The cost matrix */
  protected CostMatrix m_CostMatrix = new CostMatrix(1);

  /** The number of iterations. */
  protected int m_NumIterations = 10;

  /** Seed for reweighting using resampling. */
  protected int m_Seed = 1;

  /** The size of each bag sample, as a percentage of the training size */
  protected int m_BagSizePercent = 100;

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(6);

    newVector.addElement(new Option(
	      "\tNumber of bagging iterations.\n"
	      + "\t(default 10)",
	      "I", 1, "-I <num>"));
    newVector.addElement(new Option(
	      "\tFull class name of classifier to use. (required)\n"
	      + "\teg: weka.classifiers.NaiveBayes",
	      "W", 1, "-W <class name>"));
    newVector.addElement(new Option(
	      "\tFile name of a cost matrix to use. If this is not supplied,\n"
              +"\ta cost matrix will be loaded on demand. The name of the\n"
              +"\ton-demand file is the relation name of the training data\n"
              +"\tplus \".cost\", and the path to the on-demand file is\n"
              +"\tspecified with the -D option.",
	      "C", 1, "-C <cost file name>"));
    newVector.addElement(new Option(
              "\tName of a directory to search for cost files when loading\n"
              +"\tcosts on demand (default current directory).",
              "D", 1, "-D <directory>"));
    newVector.addElement(new Option(
	      "\tSeed used when reweighting via resampling. (Default 1)",
	      "S", 1, "-S <num>"));
    newVector.addElement(new Option(
              "\tSize of each bag, as a percentage of the\n" 
              + "\ttraining set size. (default 100)",
              "P", 1, "-P"));
    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -W classname <br>
   * Specify the full class name of a classifier (required).<p>
   *
   * -C cost file <br>
   * File name of a cost matrix to use. If this is not supplied, a cost
   * matrix will be loaded on demand. The name of the on-demand file
   * is the relation name of the training data plus ".cost", and the
   * path to the on-demand file is specified with the -D option.<p>
   *
   * -D directory <br>
   * Name of a directory to search for cost files when loading costs on demand
   * (default current directory). <p>
   *
   * -I num <br>
   * Set the number of bagging iterations (default 10). <p>
   *
   * -S seed <br>
   * Random number seed used when reweighting by resampling (default 1).<p>
   *
   * -P num <br>
   * Size of each bag, as a percentage of the training size (default 100). <p>
   *
   * Options after -- are passed to the designated classifier.<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String bagIterations = Utils.getOption('I', options);
    if (bagIterations.length() != 0) {
      setNumIterations(Integer.parseInt(bagIterations));
    } else {
      setNumIterations(10);
    }

    String seedString = Utils.getOption('S', options);
    if (seedString.length() != 0) {
      setSeed(Integer.parseInt(seedString));
    } else {
      setSeed(1);
    }

    String bagSize = Utils.getOption('P', options);
    if (bagSize.length() != 0) {
      setBagSizePercent(Integer.parseInt(bagSize));
    } else {
      setBagSizePercent(100);
    }

    String classifierName = Utils.getOption('W', options);
    if (classifierName.length() == 0) {
      throw new Exception("A classifier must be specified with"
			  + " the -W option.");
    }
    setClassifier(Classifier.forName(classifierName,
				     Utils.partitionOptions(options)));

    String costFile = Utils.getOption('C', options);
    if (costFile.length() != 0) {
      setCostMatrix(new CostMatrix(new BufferedReader(
                                   new FileReader(costFile))));
      setCostMatrixSource(new SelectedTag(MATRIX_SUPPLIED,
                                          TAGS_MATRIX_SOURCE));
      m_CostFile = costFile;
    } else {
      setCostMatrixSource(new SelectedTag(MATRIX_ON_DEMAND, 
                                          TAGS_MATRIX_SOURCE));
    }
    
    String demandDir = Utils.getOption('D', options);
    if (demandDir.length() != 0) {
      setOnDemandDirectory(new File(demandDir));
    }
  }


  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] classifierOptions = new String [0];
    if ((m_Classifier != null) && 
	(m_Classifier instanceof OptionHandler)) {
      classifierOptions = ((OptionHandler)m_Classifier).getOptions();
    }

    String [] options = new String [classifierOptions.length + 12];
    int current = 0;

    if (m_MatrixSource == MATRIX_SUPPLIED) {
      if (m_CostFile != null) {
        options[current++] = "-C";
        options[current++] = "" + m_CostFile;
      }
    } else {
      options[current++] = "-D";
      options[current++] = "" + getOnDemandDirectory();
    }
    options[current++] = "-I"; options[current++] = "" + getNumIterations();
    options[current++] = "-S"; options[current++] = "" + getSeed();
    options[current++] = "-P"; options[current++] = "" + getBagSizePercent();
    if (getClassifier() != null) {
      options[current++] = "-W";
      options[current++] = getClassifier().getClass().getName();
    }
    options[current++] = "--";

    System.arraycopy(classifierOptions, 0, options, current, 
		     classifierOptions.length);
    current += classifierOptions.length;

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Gets the source location method of the cost matrix. Will be one of
   * MATRIX_ON_DEMAND or MATRIX_SUPPLIED.
   *
   * @return the cost matrix source.
   */
  public SelectedTag getCostMatrixSource() {

    return new SelectedTag(m_MatrixSource, TAGS_MATRIX_SOURCE);
  }
  
  /**
   * Sets the source location of the cost matrix. Values other than
   * MATRIX_ON_DEMAND or MATRIX_SUPPLIED will be ignored.
   *
   * @param newMethod the cost matrix location method.
   */
  public void setCostMatrixSource(SelectedTag newMethod) {
    
    if (newMethod.getTags() == TAGS_MATRIX_SOURCE) {
      m_MatrixSource = newMethod.getSelectedTag().getID();
    }
  }

  /**
   * Returns the directory that will be searched for cost files when
   * loading on demand.
   *
   * @return The cost file search directory.
   */
  public File getOnDemandDirectory() {

    return m_OnDemandDirectory;
  }

  /**
   * Sets the directory that will be searched for cost files when
   * loading on demand.
   *
   * @param newDir The cost file search directory.
   */
  public void setOnDemandDirectory(File newDir) {

    if (newDir.isDirectory()) {
      m_OnDemandDirectory = newDir;
    } else {
      m_OnDemandDirectory = new File(newDir.getParent());
    }
    m_MatrixSource = MATRIX_ON_DEMAND;
  }

  
  /**
   * Sets the distribution classifier
   *
   * @param classifier the distribution classifier with all options set.
   */
  public void setClassifier(Classifier classifier) {

    m_Classifier = classifier;
  }

  /**
   * Gets the distribution classifier used.
   *
   * @return the classifier
   */
  public Classifier getClassifier() {

    return m_Classifier;
  }
  

  /**
   * Gets the classifier specification string, which contains the class name of
   * the classifier and any options to the classifier
   *
   * @return the classifier string.
   */
  protected String getClassifierSpec() {
    
    Classifier c = getClassifier();
    if (c instanceof OptionHandler) {
      return c.getClass().getName() + " "
	+ Utils.joinOptions(((OptionHandler)c).getOptions());
    }
    return c.getClass().getName();
  }


  /**
   * Gets the size of each bag, as a percentage of the training set size.
   *
   * @return the bag size, as a percentage.
   */
  public int getBagSizePercent() {

    return m_BagSizePercent;
  }
  
  /**
   * Sets the size of each bag, as a percentage of the training set size.
   *
   * @param newBagSizePercent the bag size, as a percentage.
   */
  public void setBagSizePercent(int newBagSizePercent) {

    m_BagSizePercent = newBagSizePercent;
  }
  
  /**
   * Sets the number of bagging iterations
   */
  public void setNumIterations(int numIterations) {

    m_NumIterations = numIterations;
  }

  /**
   * Gets the number of bagging iterations
   *
   * @return the maximum number of bagging iterations
   */
  public int getNumIterations() {
    
    return m_NumIterations;
  }


  /**
   * Gets the misclassification cost matrix.
   *
   * @return the cost matrix
   */
  public CostMatrix getCostMatrix() {
    
    return m_CostMatrix;
  }
  
  /**
   * Sets the misclassification cost matrix.
   *
   * @param the cost matrix
   */
  public void setCostMatrix(CostMatrix newCostMatrix) {
    
    m_CostMatrix = newCostMatrix;
    m_MatrixSource = MATRIX_SUPPLIED;
  }
  
  /**
   * Set seed for resampling.
   *
   * @param seed the seed for resampling
   */
  public void setSeed(int seed) {

    m_Seed = seed;
  }

  /**
   * Get seed for resampling.
   *
   * @return the seed for resampling
   */
  public int getSeed() {

    return m_Seed;
  }


  /**
   * Builds the model of the base learner.
   *
   * @param data the training data
   * @exception Exception if the classifier could not be built successfully
   */
  public void buildClassifier(Instances data) throws Exception {

    if (m_Classifier == null) {
      throw new Exception("No base classifier has been set!");
    }
    if (!data.classAttribute().isNominal()) {
      throw new Exception("Class attribute must be nominal!");
    }
    if (m_MatrixSource == MATRIX_ON_DEMAND) {
      String costName = data.relationName() + CostMatrix.FILE_EXTENSION;
      File costFile = new File(getOnDemandDirectory(), costName);
      if (!costFile.exists()) {
        throw new Exception("On-demand cost file doesn't exist: " + costFile);
      }
      setCostMatrix(new CostMatrix(new BufferedReader(
                                   new FileReader(costFile))));
    }

    // Set up the bagger
    Bagging bagger = new Bagging();
    bagger.setClassifier(getClassifier());
    bagger.setSeed(getSeed());
    bagger.setNumIterations(getNumIterations());
    bagger.setBagSizePercent(getBagSizePercent());
    bagger.buildClassifier(data);
    
    // Use the bagger to reassign class values according to minimum expected
    // cost
    Instances newData = new Instances(data);
    for (int i = 0; i < newData.numInstances(); i++) {
      Instance current = newData.instance(i);
      double [] pred = bagger.distributionForInstance(current);
      int minCostPred = Utils.minIndex(m_CostMatrix.expectedCosts(pred));
      current.setClassValue(minCostPred);
    }

    // Build a classifier using the reassigned data
    m_Classifier.buildClassifier(newData);
  }

  /**
   * Classifies a given test instance.
   *
   * @param instance the instance to be classified
   * @exception Exception if instance could not be classified
   * successfully
   */
  public double classifyInstance(Instance instance) throws Exception {

    return m_Classifier.classifyInstance(instance);
  }

  /**
   * Output a representation of this classifier
   */
  public String toString() {

    if (m_Classifier == null) {
      return "MetaCost: No model built yet.";
    }

    String result = "MetaCost cost sensitive classifier induction";
    result += "\nOptions: " + Utils.joinOptions(getOptions());
    result += "\nBase learner: " + getClassifierSpec()
      + "\n\nClassifier Model\n"
      + m_Classifier.toString()
      + "\n\nCost Matrix\n"
      + m_CostMatrix.toString();
    
    return result;
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain the following arguments:
   * -t training file [-T test file] [-c class index]
   */
  public static void main(String [] argv) {

    try {
      System.out.println(Evaluation
			 .evaluateModel(new MetaCost(),
					argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }

}
