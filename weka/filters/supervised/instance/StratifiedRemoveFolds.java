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
 *    StratifiedRemoveFolds.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */


package weka.filters.supervised.instance;

import weka.filters.*;
import weka.core.*;
import java.util.*;

/**
 * This filter takes a dataset and outputs folds suitable for cross validation.
 * If you do not want the folds to be stratified then use the unsupervised 
 * version.
 *
 * Valid options are: <p>
 *
 * -V <br>
 * Specifies if inverse of selection is to be output.<p>
 *
 * -N number of folds <br>
 * Specifies number of folds dataset is split into (default 10). <p>
 *
 * -F fold <br>
 * Specifies which fold is selected. (default 1)<p>
 *
 * -S seed <br>
 * Specifies a random number seed for shuffling the dataset.
 * (default 0, don't randomize)<p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.1.2.1 $ 
*/
public class StratifiedRemoveFolds extends Filter implements SupervisedFilter,
						      OptionHandler {

  /** Indicates if inverse of selection is to be output. */
  private boolean m_Inverse = false;

  /** Number of folds to split dataset into */
  private int m_NumFolds = 10;

  /** Fold to output */
  private int m_Fold = 1;

  /** Random number seed. */
  private long m_Seed = 0;

  /**
   * Gets an enumeration describing the available options..
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(6);

    newVector.addElement(new Option(
	      "\tSpecifies if inverse of selection is to be output.\n",
	      "V", 0, "-V"));

    newVector.addElement(new Option(
              "\tSpecifies number of folds dataset is split into. \n"
	      + "\t(default 10)\n",
              "N", 1, "-N <number of folds>"));

    newVector.addElement(new Option(
	      "\tSpecifies which fold is selected. (default 1)\n",
	      "F", 1, "-F <fold>"));

    newVector.addElement(new Option(
	      "\tSpecifies random number seed. (default 0, no randomizing)\n",
	      "S", 1, "-S <seed>"));

    return newVector.elements();
  }

  /**
   * Parses the options for this object. Valid options are: <p>
   *
   * -V <br>
   * Specifies if inverse of selection is to be output.<p>
   *
   * -N number of folds <br>
   * Specifies number of folds dataset is split into (default 10). <p>
   *
   * -F fold <br>
   * Specifies which fold is selected. (default 1)<p>
   *
   * -S seed <br>
   * Specifies a random number seed for shuffling the dataset.
   * (default 0, no randomizing)<p>
   *
   * -A <br>
   * If set, data will not be stratified. <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    setInvertSelection(Utils.getFlag('V', options));
    String numFolds = Utils.getOption('N', options);
    if (numFolds.length() != 0) {
      setNumFolds(Integer.parseInt(numFolds));
    } else {
      setNumFolds(10);
    }
    String fold = Utils.getOption('F', options);
    if (fold.length() != 0) {
      setFold(Integer.parseInt(fold));
    } else {
      setFold(1);
    }
    String seed = Utils.getOption('S', options);
    if (seed.length() != 0) {
      setSeed(Integer.parseInt(seed));
    } else {
      setSeed(0);
    }
    if (getInputFormat() != null) {
      setInputFormat(getInputFormat());
    }
  }

  /**
   * Gets the current settings of the filter.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [8];
    int current = 0;

    options[current++] = "-S"; options[current++] = "" + getSeed();
    if (getInvertSelection()) {
      options[current++] = "-V";
    }
    options[current++] = "-N"; options[current++] = "" + getNumFolds();
    options[current++] = "-F"; options[current++] = "" + getFold();
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "This filter takes a dataset and outputs a specified fold for cross validation. If you do not want the folds to be stratified use the unsupervised version.";
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String invertSelectionTipText() {

    return "Whether to invert the selection.";
  }

  /**
   * Gets if selection is to be inverted.
   *
   * @return true if the selection is to be inverted
   */
  public boolean getInvertSelection() {

    return m_Inverse;
  }

  /**
   * Sets if selection is to be inverted.
   *
   * @param inverse true if inversion is to be performed
   */
  public void setInvertSelection(boolean inverse) {
    
    m_Inverse = inverse;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numFoldsTipText() {

    return "The number of folds to split the dataset into.";
  }

  /**
   * Gets the number of folds in which dataset is to be split into.
   * 
   * @return the number of folds the dataset is to be split into.
   */
  public int getNumFolds() {

    return m_NumFolds;
  }

  /**
   * Sets the number of folds the dataset is split into. If the number
   * of folds is zero, it won't split it into folds. 
   *
   * @param numFolds number of folds dataset is to be split into
   * @exception IllegalArgumentException if number of folds is negative
   */
  public void setNumFolds(int numFolds) {

    if (numFolds < 0) {
      throw new IllegalArgumentException("Number of folds has to be positive or zero.");
    }
    m_NumFolds = numFolds;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String foldTipText() {

    return "The fold which is selected.";
  }

  /**
   * Gets the fold which is selected.
   *
   * @return the fold which is selected
   */
  public int getFold() {

    return m_Fold;
  }

  /**
   * Selects a fold.
   *
   * @param fold the fold to be selected.
   * @exception IllegalArgumentException if fold's index is smaller than 1
   */
  public void setFold(int fold) {

    if (fold < 1) {
      throw new IllegalArgumentException("Fold's index has to be greater than 0.");
    }
    m_Fold = fold;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String seedTipText() {

    return "the random number seed for shuffling the dataset. If seed is negative, shuffling will not be performed.";
  }

  /**
   * Gets the random number seed used for shuffling the dataset.
   *
   * @return the random number seed
   */
  public long getSeed() {

    return m_Seed;
  }

  /**
   * Sets the random number seed for shuffling the dataset. If seed
   * is negative, shuffling won't be performed.
   *
   * @param seed the random number seed
   */
  public void setSeed(long seed) {
    
    m_Seed = seed;
  }

  /**
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input instance
   * structure (any instances contained in the object are ignored - only the
   * structure is required).
   * @return true because outputFormat can be collected immediately
   * @exception Exception if the input format can't be set successfully
   */  
  public boolean setInputFormat(Instances instanceInfo) throws Exception {

    if ((m_NumFolds > 0) && (m_NumFolds < m_Fold)) {
      throw new IllegalArgumentException("Fold has to be smaller or equal to "+
                                         "number of folds.");
    }
    super.setInputFormat(instanceInfo);
    setOutputFormat(instanceInfo);
    return true;
  }

  /**
   * Input an instance for filtering. Filter requires all
   * training instances be read before producing output.
   *
   * @param instance the input instance
   * @return true if the filtered instance may now be
   * collected with output().
   * @throws IllegalStateException if no input structure has been defined
   */
  public boolean input(Instance instance) {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }
    if (m_FirstBatchDone) {
      push(instance);
      return true;
    } else {
      bufferInput(instance);
      return false;
    }
  }

  /**
   * Signify that this batch of input to the filter is
   * finished. Output() may now be called to retrieve the filtered
   * instances.
   *
   * @return true if there are instances pending output
   * @exception IllegalStateException if no input structure has been defined 
   */
  public boolean batchFinished() {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_Seed > 0) {
      // User has provided a random number seed.
      getInputFormat().randomize(new Random(m_Seed));
    }

    Instances instances;
    if (!m_FirstBatchDone) {
      // Select out a fold
      getInputFormat().stratify(m_NumFolds);
      if (!m_Inverse) {
	instances = getInputFormat().testCV(m_NumFolds, m_Fold - 1);
      } else {
	instances = getInputFormat().trainCV(m_NumFolds, m_Fold - 1);
      }
      for (int i = 0; i < instances.numInstances(); i++) {
	push(instances.instance(i));
      }
    }
    else {
      instances = getInputFormat();
    }
    
    flushInput();

    m_NewBatch = true;
    m_FirstBatchDone = true;
    return (numPendingOutput() != 0);
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: use -h for help
   */
  public static void main(String [] argv) {

    try {
      if (Utils.getFlag('b', argv)) {
 	Filter.batchFilterFile(new StratifiedRemoveFolds(), argv);
      } else {
	Filter.filterFile(new StratifiedRemoveFolds(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}
