/*
 *    PART.java
 *    Copyright (C) 1999 Eibe Frank
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
package weka.classifiers.j48;

import java.util.*;
import weka.core.*;
import weka.classifiers.*;

/**
 * Class for generating a PART decision list.
 *
 * Reference:
 * Eibe Frank and Ian H. Witten (1998).  <a
 * href="http://www.cs.waikato.ac.nz/~eibe/pubs/ML98-57.ps.gz">Generating
 * Accurate Rule Sets Without Global Optimization.</a> In Shavlik, J.,
 * ed., <i>Machine Learning: Proceedings of the Fifteenth
 * International Conference</i>, Morgan Kaufmann Publishers, San
 * Francisco, CA.
 *
 * Valid options are: <p>
 *
 * -C confidence <br>
 * Set confidence threshold for pruning. (Default: 0.25) <p>
 *
 * -M number <br>
 * Set minimum number of instances per leaf. (Default: 2) <p>
 *
 * -R <br>
 * Use reduced error pruning. <p>
 *
 * -N number <br>
 * Set number of folds for reduced error pruning. One fold is
 * used as the pruning set. (Default: 3) <p>
 *
 * -B <br>
 * Use binary splits for nominal attributes. <p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public class PART extends DistributionClassifier implements OptionHandler,
  WeightedInstancesHandler {

  /** The decision list */
  private MakeDecList m_root;

  /** Confidence level */
  private float m_CF = 0.25f;

  /** Minimum number of objects */
  private int m_minNumObj = 2;

  /** Use reduced error pruning? */
  private boolean m_reducedErrorPruning = false;

  /** Number of folds for reduced error pruning. */
  private int m_numFolds = 3;

  /** Binary splits on nominal attributes? */
  private boolean binarySplits = false;
  
  /**
   * Generates the classifier.
   *
   * @exception Exception if classifier can't be built successfully
   */
  public void buildClassifier(Instances instances) 
       throws Exception{

    ModelSelection modSelection;	 

    if (binarySplits)
      modSelection = new BinC45ModelSelection(m_minNumObj, instances);
    else
      modSelection = new C45ModelSelection(m_minNumObj, instances);
    if (m_reducedErrorPruning) 
      m_root = new MakeDecList(modSelection, m_numFolds, m_minNumObj);
    else
      m_root = new MakeDecList(modSelection, m_CF, m_minNumObj);
    m_root.buildClassifier(instances);
  }

  /**
   * Classifies an instance.
   *
   * @exception Exception if instance can't be classified successfully
   */
  public double classifyInstance(Instance instance) 
       throws Exception {

    return m_root.classifyInstance(instance);
  }

  /** 
   * Returns class probabilities for an instance.
   *
   * @exception Exception if the distribution can't be computed successfully
   */
  public final double [] distributionForInstance(Instance instance) 
       throws Exception {

    return m_root.distributionForInstance(instance);
  }

  /**
   * Returns an enumeration describing the available options
   *
   * Valid options are: <p>
   *
   * -C confidence <br>
   * Set confidence threshold for pruning. (Default: 0.25) <p>
   *
   * -M number <br>
   * Set minimum number of instances per leaf. (Default: 2) <p>
   *
   * -R <br>
   * Use reduced error pruning. <p>
   *
   * -N number <br>
   * Set number of folds for reduced error pruning. One fold is
   * used as the pruning set. (Default: 3) <p>
   *
   * -B <br>
   * Use binary splits for nominal attributes. <p>
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(5);

    newVector.
	addElement(new Option("\tSet confidence threshold for pruning.\n" +
			      "\t(default 0.25)",
			      "C", 1, "-C <pruning confidence>"));
    newVector.
	addElement(new Option("\tSet minimum number of objects per leaf.\n" +
			      "\t(default 2)",
			      "M", 1, "-M <minimum number of objects>"));
    newVector.
	addElement(new Option("\tUse reduced error pruning.",
			      "R", 0, "-R"));
    newVector.
	addElement(new Option("\tSet number of folds for reduced error\n" +
			      "\tpruning. One fold is used as pruning set.\n" +
			      "\t(default 3)",
			      "N", 1, "-N <number of folds>"));
    newVector.
	addElement(new Option("\tUse binary splits only.",
			      "B", 0, "-B"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options.
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception{

    // Pruning options
    m_reducedErrorPruning = Utils.getFlag('R', options);
    binarySplits = Utils.getFlag('B', options);
    String confidenceString = Utils.getOption('C', options);
    if (confidenceString.length() != 0) {
      if (m_reducedErrorPruning) {
	throw new Exception("Setting CF doesn't make sense " +
			    "for reduced error pruning.");
      } else {
	m_CF = (new Float(confidenceString)).floatValue();
	if ((m_CF <= 0) || (m_CF >= 1)) {
	  throw new Exception("CF has to be greater than zero and smaller than one!");
	} 
      }
    } else {
      m_CF = 0.25f;
    }
    String numFoldsString = Utils.getOption('N', options);
    if (numFoldsString.length() != 0) {
      if (!m_reducedErrorPruning) {
	throw new Exception("Setting the number of folds" +
			    " does only make sense for" +
			    " reduced error pruning.");
      } else {
	m_numFolds = Integer.parseInt(numFoldsString);
      }
    } else {
      m_numFolds = 3;
    }

    // Other options
    String minNumString = Utils.getOption('M', options);
    if (minNumString.length() != 0) {
      m_minNumObj = Integer.parseInt(minNumString);
    } else {
      m_minNumObj = 2;
    }
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [6];
    int current = 0;

    if (m_reducedErrorPruning) {
      options[current++] = "-R";
      options[current++] = "-N"; options[current++] = "" + m_numFolds;
    } else {
      options[current++] = "-C"; options[current++] = "" + m_CF;
    }
    if (binarySplits) {
      options[current++] = "-B";
    }
    options[current++] = "-M"; options[current++] = "" + m_minNumObj;

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Returns a description of the classifier
   */
  public String toString() {
    
    return "PART decision list\n------------------\n\n" + m_root.toString();
  }
  
  /**
   * Main method for testing this class.
   *
   * @param String options 
   */
  public static void main(String [] argv){

    try {
      System.out.println(Evaluation.evaluateModel(new PART(), argv));
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }
}


  






