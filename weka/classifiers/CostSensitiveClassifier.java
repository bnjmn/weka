/*
 *    CostSensitiveClassifier.java
 *    Copyright (C) 1999 Intelligenesis Corp.
 *
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
package weka.classifiers;

import java.io.*;
import java.util.*;
import weka.core.*;
import weka.filters.Filter;


/**
 * This metaclassifier makes its base classifier cost-sensitive. Two methods
 * can be used to introduce cost-sensitivity: reweighting training instances 
 * according to the total cost assigned to each class; or predicting the class
 * with minimum expected misclassification cost (rather than the most likely 
 * class). The minimum expected cost approach requires that the base classifier
 * be a DistributionClassifier. <p>
 *
 * Valid options are:<p>
 *
 * -M <br>
 * Minimize expected misclassification cost. The base classifier must 
 * produce probability estimates i.e. a DistributionClassifier).
 * (default is to reweight training instances according to costs per class)<p>
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
 * -S seed <br>
 * Random number seed used when reweighting by resampling (default 1).<p>
 *
 * Options after -- are passed to the designated classifier.<p>
 *
 * @author Len Trigg (len@intelligenesis.net)
 * @version $Revision: 1.2 $
 */
public class CostSensitiveClassifier extends Classifier
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

  /** The cost matrix */
  protected CostMatrix m_CostMatrix = new CostMatrix(1);

  /** The classifier */
  protected Classifier m_Classifier = new weka.classifiers.ZeroR();

  /** Seed for reweighting using resampling. */
  protected int m_Seed = 1;

  /** 
   * True if the costs should be used by selecting the minimum expected
   * cost (false means weight training data by the costs)
   */
  protected boolean m_MinimizeExpectedCost;
  
  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(5);

    newVector.addElement(new Option(
	      "\tMinimize expected misclassification cost. The\n"
	      +"\tbase classifier must produce probability estimates\n"
	      +"\t(i.e. a DistributionClassifier). Default is to\n"
	      +"\treweight training instances according to costs per class",
	      "M", 0, "-M"));
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
    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -M <br>
   * Minimize expected misclassification cost. The base classifier must 
   * produce probability estimates i.e. a DistributionClassifier).
   * (default is to reweight training instances according to costs per class)<p>
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
   * -S seed <br>
   * Random number seed used when reweighting by resampling (default 1).<p>
   *
   * Options after -- are passed to the designated classifier.<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    setMinimizeExpectedCost(Utils.getFlag('M', options));

    String seedString = Utils.getOption('S', options);
    if (seedString.length() != 0) {
      setSeed(Integer.parseInt(seedString));
    } else {
      setSeed(1);
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

    String [] options = new String [classifierOptions.length + 9];
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
    options[current++] = "-S"; options[current++] = "" + getSeed();
    if (getMinimizeExpectedCost()) {
      options[current++] = "-M";
    }
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

    try {
      return new SelectedTag(m_MatrixSource, TAGS_MATRIX_SOURCE);
    } catch (Exception ex) {
      return null;
    }
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
  }

  /**
   * Gets the value of MinimizeExpectedCost.
   *
   * @return Value of MinimizeExpectedCost.
   */
  public boolean getMinimizeExpectedCost() {
    
    return m_MinimizeExpectedCost;
  }
  
  /**
   * Set the value of MinimizeExpectedCost.
   *
   * @param newMinimizeExpectedCost Value to assign to MinimizeExpectedCost.
   */
  public void setMinimizeExpectedCost(boolean newMinimizeExpectedCost) {
    
    m_MinimizeExpectedCost = newMinimizeExpectedCost;
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
    if (m_MinimizeExpectedCost 
	&& !(m_Classifier instanceof DistributionClassifier)) {
      throw new Exception("Classifier must be a DistributionClassifier to use"
			  + " minimum expected cost method");
    }
    if (!data.classAttribute().isNominal()) {
      throw new Exception("Class attribute must be nominal!");
    }
    if (m_MatrixSource == MATRIX_ON_DEMAND) {
      String costName = data.relationName() + ".cost";
      File costFile = new File(getOnDemandDirectory(), costName);
      if (!costFile.exists()) {
        throw new Exception("On-demand cost file doesn't exist: " + costFile);
      }
      setCostMatrix(new CostMatrix(new BufferedReader(
                                   new FileReader(costFile))));
    }

    if (!m_MinimizeExpectedCost) {
      Random random = null;
      if (!(m_Classifier instanceof WeightedInstancesHandler)) {
	random = new Random(m_Seed);
      }
      data = m_CostMatrix.applyCostMatrix(data, random);
    }
    m_Classifier.buildClassifier(data);
  }

  /**
   * Classifies a given instance by choosing the class with the minimum
   * expected misclassification cost.
   *
   * @param instance the instance to be classified
   * @exception Exception if instance could not be classified
   * successfully
   */
  public double classifyInstance(Instance instance) throws Exception {

    if (!m_MinimizeExpectedCost) {
      return m_Classifier.classifyInstance(instance);
    }
    double [] pred = ((DistributionClassifier) m_Classifier)
      .distributionForInstance(instance);
    double [] costs = m_CostMatrix.expectedCosts(pred);
    /*
    for (int i = 0; i < pred.length; i++) {
      System.out.print(pred[i] + " ");
    }
    System.out.println();
    for (int i = 0; i < costs.length; i++) {
      System.out.print(costs[i] + " ");
    }
    System.out.println("\n");
    */
    
    return Utils.minIndex(costs);
  }

  /**
   * Output a representation of this classifier
   */
  public String toString() {

    if (m_Classifier == null) {
      return "CostSensitiveClassifier: No model built yet.";
    }

    String result = "CostSensitiveClassifier using ";
      if (m_MinimizeExpectedCost) {
	result += "minimized expected misclasification cost\n";
      } else {
	result += "reweighted training instances\n";
      }
      result += "\n" + getClassifierSpec()
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
			 .evaluateModel(new CostSensitiveClassifier(),
					argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }

}
