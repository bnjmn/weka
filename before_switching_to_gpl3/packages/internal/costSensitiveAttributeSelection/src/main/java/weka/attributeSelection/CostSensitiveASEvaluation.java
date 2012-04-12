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
 *    CostSensitiveASEvaluation.java
 *    Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
 *
 */

package  weka.attributeSelection;

import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.classifiers.CostMatrix;
import weka.core.WeightedInstancesHandler;
import weka.core.RevisionUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;
import java.util.ArrayList;

/**
 * Abstract base class for cost-sensitive subset and attribute evaluators.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public abstract class CostSensitiveASEvaluation
  extends ASEvaluation
  implements OptionHandler, Serializable {

  /** for serialization */
  static final long serialVersionUID = -7045833833363396977L;

  /** load cost matrix on demand */
  public static final int MATRIX_ON_DEMAND = 1;
  /** use explicit cost matrix */
  public static final int MATRIX_SUPPLIED = 2;
  /** Specify possible sources of the cost matrix */
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

  /** The base evaluator to use */
  protected ASEvaluation m_evaluator;

  /** random number seed */
  protected int m_seed = 1;

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(4);

    newVector.addElement(new Option(
                                    "\tFile name of a cost matrix to use. If this is not supplied,\n"
                                    +"\ta cost matrix will be loaded on demand. The name of the\n"
                                    +"\ton-demand file is the relation name of the training data\n"
                                    +"\tplus \".cost\", and the path to the on-demand file is\n"
                                    +"\tspecified with the -N option.",
                                    "C", 1, "-C <cost file name>"));
    newVector.addElement(new Option(
                                    "\tName of a directory to search for cost files when loading\n"
                                    +"\tcosts on demand (default current directory).",
                                    "N", 1, "-N <directory>"));
    newVector.addElement(new Option(
                                    "\tThe cost matrix in Matlab single line format.",
                                    "cost-matrix", 1, "-cost-matrix <matrix>"));
    newVector.addElement(new Option(
                                    "\tThe seed to use for random number generation.",
                                    "S", 1, "-S <integer>"));

    newVector.addElement(new Option(
                                    "\tFull name of base evaluator. Options after -- are "
                                    +"passed to the evaluator.\n"
                                    + "\t(default: " + defaultEvaluatorString() +")",
                                    "W", 1, "-W"));

    if (m_evaluator instanceof OptionHandler) {
      newVector.addElement(new Option(
                                      "",
                                      "", 0, "\nOptions specific to evaluator "
                                      + m_evaluator.getClass().getName() + ":"));
      Enumeration enu = ((OptionHandler)m_evaluator).listOptions();
      while (enu.hasMoreElements()) {
        newVector.addElement(enu.nextElement());
      }
    }


    return newVector.elements();
  }

  /**
   * Parses a given list of options. <p/>
   *
   * Valid options are: <p/>
   * 
   * <pre> -C &lt;cost file name&gt;
   *  File name of a cost matrix to use. If this is not supplied,
   *  a cost matrix will be loaded on demand. The name of the
   *  on-demand file is the relation name of the training data
   *  plus ".cost", and the path to the on-demand file is
   *  specified with the -N option.</pre>
   * 
   * <pre> -N &lt;directory&gt;
   *  Name of a directory to search for cost files when loading
   *  costs on demand (default current directory).</pre>
   * 
   * <pre> -cost-matrix &lt;matrix&gt;
   *  The cost matrix in Matlab single line format.</pre>
   * 
   * <pre> -S &lt;integer&gt;
   *  The seed to use for random number generation.</pre>
   * 
   * <pre> -W
   *  Full name of base evaluator.
   *  (default: weka.attributeSelection.CfsSubsetEval)</pre>
   *
   * Options after -- are passed to the designated evaluator.<p>
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    String costFile = Utils.getOption('C', options);
    if (costFile.length() != 0) {
      try {
	setCostMatrix(new CostMatrix(new BufferedReader(
                                                        new FileReader(costFile))));
      } catch (Exception ex) {
	// now flag as possible old format cost matrix. Delay cost matrix
	// loading until buildClassifer is called
	setCostMatrix(null);
      }
      setCostMatrixSource(new SelectedTag(MATRIX_SUPPLIED,
                                          TAGS_MATRIX_SOURCE));
      m_CostFile = costFile;
    } else {
      setCostMatrixSource(new SelectedTag(MATRIX_ON_DEMAND, 
                                          TAGS_MATRIX_SOURCE));
    }
    
    String demandDir = Utils.getOption('N', options);
    if (demandDir.length() != 0) {
      setOnDemandDirectory(new File(demandDir));
    }

    String cost_matrix = Utils.getOption("cost-matrix", options);
    if (cost_matrix.length() != 0) {
      StringWriter writer = new StringWriter();
      CostMatrix.parseMatlab(cost_matrix).write(writer);
      setCostMatrix(new CostMatrix(new StringReader(writer.toString())));
      setCostMatrixSource(new SelectedTag(MATRIX_SUPPLIED,
                                          TAGS_MATRIX_SOURCE));
    }

    String seed = Utils.getOption('S', options);
    if (seed.length() != 0) {
      setSeed(Integer.parseInt(seed));
    } else {
      setSeed(1);
    }

    String evaluatorName = Utils.getOption('W', options);
    
    if (evaluatorName.length() > 0) { 
      
      // This is just to set the evaluator in case the option 
      // parsing fails.
      setEvaluator(ASEvaluation.forName(evaluatorName, null));
      setEvaluator(ASEvaluation.forName(evaluatorName,
                                        Utils.partitionOptions(options)));
    } else {
      
      // This is just to set the classifier in case the option 
      // parsing fails.
      setEvaluator(ASEvaluation.forName(defaultEvaluatorString(), null));
      setEvaluator(ASEvaluation.forName(defaultEvaluatorString(),
                                        Utils.partitionOptions(options)));
    }
  }

  /**
   * Gets the current settings of the subset evaluator.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    ArrayList<String> options = new ArrayList<String>();

    if (m_MatrixSource == MATRIX_SUPPLIED) {
      if (m_CostFile != null) {
        options.add("-C");
        options.add("" + m_CostFile);
      }
      else {
        options.add("-cost-matrix");
        options.add(getCostMatrix().toMatlab());
      }
    } else {
      options.add("-N");
      options.add("" + getOnDemandDirectory());
    }

    options.add("-S");
    options.add("" + getSeed());

    options.add("-W");
    options.add(m_evaluator.getClass().getName());

    if (m_evaluator instanceof OptionHandler) {
      String[] evaluatorOptions = ((OptionHandler)m_evaluator).getOptions();
      if (evaluatorOptions.length > 0) {
        options.add("--");
        for (int i = 0; i < evaluatorOptions.length; i++) {
          options.add(evaluatorOptions[i]);
        }
      }
    }

    return options.toArray(new String[0]);
  }

  /**
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "A meta subset evaluator that makes its base subset evaluator cost-sensitive. ";
  }

  /**
   * Return the name of the default evaluator.
   *
   * @return the name of the default evaluator
   */
  public String defaultEvaluatorString() {
    return "weka.attributeSelection.CfsSubsetEval";
  }

  /**
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String costMatrixSourceTipText() {

    return "Sets where to get the cost matrix. The two options are"
      + "to use the supplied explicit cost matrix (the setting of the "
      + "costMatrix property), or to load a cost matrix from a file when "
      + "required (this file will be loaded from the directory set by the "
      + "onDemandDirectory property and will be named relation_name" 
      + CostMatrix.FILE_EXTENSION + ").";
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
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String onDemandDirectoryTipText() {

    return "Sets the directory where cost files are loaded from. This option "
      + "is used when the costMatrixSource is set to \"On Demand\".";
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
   * Gets the evaluator specification string, which contains the class name of
   * the evaluator and any options to the evaluator
   *
   * @return the evaluator string.
   */
  protected String getEvaluatorSpec() {
    
    ASEvaluation ase = getEvaluator();
    if (ase instanceof OptionHandler) {
      return ase.getClass().getName() + " "
	+ Utils.joinOptions(((OptionHandler)ase).getOptions());
    }
    return ase.getClass().getName();
  }

  /**
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String costMatrixTipText() {
    return "Sets the cost matrix explicitly. This matrix is used if the "
      + "costMatrixSource property is set to \"Supplied\".";
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
   * @param newCostMatrix the cost matrix
   */
  public void setCostMatrix(CostMatrix newCostMatrix) {
    
    m_CostMatrix = newCostMatrix;
    m_MatrixSource = MATRIX_SUPPLIED;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String seedTipText() {
    return "The random number seed to be used.";
  }

  /**
   * Set the seed for random number generation.
   *
   * @param seed the seed 
   */
  public void setSeed(int seed) {

    m_seed = seed;
  }

  /**
   * Gets the seed for the random number generations.
   *
   * @return the seed for the random number generation
   */
  public int getSeed() {
    
    return m_seed;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String evaluatorTipText() {
    return "The base evaluator to be used.";
  }

  /**
   * Set the base evaluator.
   *
   * @param newEvaluator the evaluator to use.
   * @throws IllegalArgumentException if the evaluator is of the wrong type
   */
  public void setEvaluator(ASEvaluation newEvaluator) throws IllegalArgumentException {

    m_evaluator = newEvaluator;
  }

  /**
   * Get the evaluator used as the base evaluator.
   *
   * @return the evaluator used as the base evaluator
   */
  public ASEvaluation getEvaluator() {

    return m_evaluator;
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result;

    if (getEvaluator() != null) {
      result = getEvaluator().getCapabilities();
    } else {
      result = new Capabilities(this);
      result.disableAll();
    }

    // class
    result.disableAllClasses();
    result.disableAllClassDependencies();
    result.enable(Capability.NOMINAL_CLASS);
    
    return result;
  }

  /**
   * Generates a attribute evaluator. Has to initialize all fields of the 
   * evaluator that are not being set via options.
   *
   * @param data set of instances serving as training data 
   * @exception Exception if the evaluator has not been 
   * generated successfully
   */
  public void buildEvaluator(Instances data) throws Exception {
    // can evaluator handle the data?
    getCapabilities().testWithFail(data);

    // remove instances with missing class
    data = new Instances(data);
    data.deleteWithMissingClass();

    if (m_evaluator == null) {
      throw new Exception("No base evaluator has been set!");
    }

    if (m_MatrixSource == MATRIX_ON_DEMAND) {
      String costName = data.relationName() + CostMatrix.FILE_EXTENSION;
      File costFile = new File(getOnDemandDirectory(), costName);
      if (!costFile.exists()) {
        throw new Exception("On-demand cost file doesn't exist: " + costFile);
      }
      setCostMatrix(new CostMatrix(new BufferedReader(
                                                      new FileReader(costFile))));
    } else if (m_CostMatrix == null) {
      // try loading an old format cost file
      m_CostMatrix = new CostMatrix(data.numClasses());
      m_CostMatrix.readOldFormat(new BufferedReader(
                                                    new FileReader(m_CostFile)));
    }
    
    Random random = null;
    if (!(m_evaluator instanceof WeightedInstancesHandler)) {
      random = new Random(m_seed);
    }
    data = m_CostMatrix.applyCostMatrix(data, random);
    m_evaluator.buildEvaluator(data);
  }

  /**
   * Provides a chance for a attribute evaluator to do any special
   * post processing of the selected attribute set.
   *
   * @param attributeSet the set of attributes found by the search
   * @return a possibly ranked list of postprocessed attributes
   * @exception Exception if postprocessing fails for some reason
   */
  public int [] postProcess(int [] attributeSet) 
    throws Exception {
    return m_evaluator.postProcess(attributeSet);
  }

  /**
   * Output a representation of this evaluator
   * 
   * @return a string representation of the classifier
   */
  public String toString() {

    if (m_evaluator == null) {
      return "CostSensitiveASEvaluation: No model built yet.";
    }
  
    String result = (m_evaluator instanceof AttributeEvaluator)
      ? "CostSensitiveAttributeEval using "
      : "CostSensitiveSubsetEval using ";

    result += "\n\n" + getEvaluatorSpec()
      + "\n\nEvaluator\n"
      + m_evaluator.toString()
      + "\n\nCost Matrix\n"
      + m_CostMatrix.toString();
    
    return result;
  }

  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }
}
