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
 *    Resample.java
 *    Copyright (C) 2002 University of Waikato
 *
 */


package weka.filters.supervised.instance;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.SupervisedFilter;

import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

/** 
 <!-- globalinfo-start -->
 * Produces a random subsample of a dataset using sampling with replacement.The original dataset must fit entirely in memory. The number of instances in the generated dataset may be specified. The dataset must have a nominal class attribute. If not, use the unsupervised version. The filter can be made to maintain the class distribution in the subsample, or to bias the class distribution toward a uniform distribution. When used in batch mode (i.e. in the FilteredClassifier), subsequent batches are NOTE resampled.
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -S &lt;num&gt;
 *  Specify the random number seed (default 1)</pre>
 * 
 * <pre> -Z &lt;num&gt;
 *  The size of the output dataset, as a percentage of
 *  the input dataset (default 100)</pre>
 * 
 * <pre> -B &lt;num&gt;
 *  Bias factor towards uniform class distribution.
 *  0 = distribution in input data -- 1 = uniform distribution.
 *  (default 0)</pre>
 * 
 <!-- options-end -->
 *
 * @author Len Trigg (len@reeltwo.com)
 * @version $Revision: 1.5 $ 
 */
public class Resample
  extends Filter 
  implements SupervisedFilter, OptionHandler {
  
  /** for serialization */
  static final long serialVersionUID = 7079064953548300681L;

  /** The subsample size, percent of original set, default 100% */
  private double m_SampleSizePercent = 100;
  
  /** The random number generator seed */
  private int m_RandomSeed = 1;
  
  /** The degree of bias towards uniform (nominal) class distribution */
  private double m_BiasToUniformClass = 0;

  /** True if the first batch has been done */
  private boolean m_FirstBatchDone = false;

  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "Produces a random subsample of a dataset using sampling with replacement."
      + "The original dataset must "
      + "fit entirely in memory. The number of instances in the generated "
      + "dataset may be specified. The dataset must have a nominal class "
      + "attribute. If not, use the unsupervised version. The filter can be "
      + "made to maintain the class distribution in the subsample, or to bias "
      + "the class distribution toward a uniform distribution. When used in batch "
      + "mode (i.e. in the FilteredClassifier), subsequent batches are NOTE resampled.";
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(1);

    newVector.addElement(new Option(
              "\tSpecify the random number seed (default 1)",
              "S", 1, "-S <num>"));
    newVector.addElement(new Option(
              "\tThe size of the output dataset, as a percentage of\n"
              +"\tthe input dataset (default 100)",
              "Z", 1, "-Z <num>"));
    newVector.addElement(new Option(
              "\tBias factor towards uniform class distribution.\n"
              +"\t0 = distribution in input data -- 1 = uniform distribution.\n"
              +"\t(default 0)",
              "B", 1, "-B <num>"));

    return newVector.elements();
  }


  /**
   * Parses a given list of options. <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -S &lt;num&gt;
   *  Specify the random number seed (default 1)</pre>
   * 
   * <pre> -Z &lt;num&gt;
   *  The size of the output dataset, as a percentage of
   *  the input dataset (default 100)</pre>
   * 
   * <pre> -B &lt;num&gt;
   *  Bias factor towards uniform class distribution.
   *  0 = distribution in input data -- 1 = uniform distribution.
   *  (default 0)</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    String seedString = Utils.getOption('S', options);
    if (seedString.length() != 0) {
      setRandomSeed(Integer.parseInt(seedString));
    } else {
      setRandomSeed(1);
    }

    String biasString = Utils.getOption('B', options);
    if (biasString.length() != 0) {
      setBiasToUniformClass(Double.valueOf(biasString).doubleValue());
    } else {
      setBiasToUniformClass(0);
    }

    String sizeString = Utils.getOption('Z', options);
    if (sizeString.length() != 0) {
      setSampleSizePercent(Double.valueOf(sizeString).doubleValue());
    } else {
      setSampleSizePercent(100);
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

    String [] options = new String [6];
    int current = 0;

    options[current++] = "-B"; 
    options[current++] = "" + getBiasToUniformClass();

    options[current++] = "-S"; options[current++] = "" + getRandomSeed();

    options[current++] = "-Z"; options[current++] = "" + getSampleSizePercent();

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }
    
  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String biasToUniformClassTipText() {
    return "Whether to use bias towards a uniform class. A value of 0 leaves the class "
      + "distribution as-is, a value of 1 ensures the class distribution is "
      + "uniform in the output data.";
  }
    
  /**
   * Gets the bias towards a uniform class. A value of 0 leaves the class
   * distribution as-is, a value of 1 ensures the class distributions are
   * uniform in the output data.
   *
   * @return the current bias
   */
  public double getBiasToUniformClass() {

    return m_BiasToUniformClass;
  }
  
  /**
   * Sets the bias towards a uniform class. A value of 0 leaves the class
   * distribution as-is, a value of 1 ensures the class distributions are
   * uniform in the output data.
   *
   * @param newBiasToUniformClass the new bias value, between 0 and 1.
   */
  public void setBiasToUniformClass(double newBiasToUniformClass) {

    m_BiasToUniformClass = newBiasToUniformClass;
  }
    
  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String randomSeedTipText() {
    return "Sets the random number seed for subsampling.";
  }
  
  /**
   * Gets the random number seed.
   *
   * @return the random number seed.
   */
  public int getRandomSeed() {

    return m_RandomSeed;
  }
  
  /**
   * Sets the random number seed.
   *
   * @param newSeed the new random number seed.
   */
  public void setRandomSeed(int newSeed) {

    m_RandomSeed = newSeed;
  }
    
  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String sampeSizePercentTipText() {
    return "The subsample size as a percentage of the original set.";
  }
  
  /**
   * Gets the subsample size as a percentage of the original set.
   *
   * @return the subsample size
   */
  public double getSampleSizePercent() {

    return m_SampleSizePercent;
  }
  
  /**
   * Sets the size of the subsample, as a percentage of the original set.
   *
   * @param newSampleSizePercent the subsample set size, between 0 and 100.
   */
  public void setSampleSizePercent(double newSampleSizePercent) {

    m_SampleSizePercent = newSampleSizePercent;
  }
  
  /**
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input 
   * instance structure (any instances contained in the object are 
   * ignored - only the structure is required).
   * @return true if the outputFormat may be collected immediately
   * @throws Exception if the input format can't be set 
   * successfully
   */
  public boolean setInputFormat(Instances instanceInfo) 
       throws Exception {

    if (instanceInfo.classIndex() < 0 || !instanceInfo.classAttribute().isNominal()) {
      throw new IllegalArgumentException("Supervised resample requires nominal class");
    }

    super.setInputFormat(instanceInfo);
    setOutputFormat(instanceInfo);
    m_FirstBatchDone = false;
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
   * Signify that this batch of input to the filter is finished. 
   * If the filter requires all instances prior to filtering,
   * output() may now be called to retrieve the filtered instances.
   *
   * @return true if there are instances pending output
   * @throws IllegalStateException if no input structure has been defined
   */
  public boolean batchFinished() {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }

    if (!m_FirstBatchDone) {
      // Do the subsample, and clear the input instances.
      createSubsample();
    }
    flushInput();

    m_NewBatch = true;
    m_FirstBatchDone = true;
    return (numPendingOutput() != 0);
  }


  /**
   * Creates a subsample of the current set of input instances. The output
   * instances are pushed onto the output queue for collection.
   */
  private void createSubsample() {

    int origSize = getInputFormat().numInstances();
    int sampleSize = (int) (origSize * m_SampleSizePercent / 100);

    // Subsample that takes class distribution into consideration

    // Sort according to class attribute.
    getInputFormat().sort(getInputFormat().classIndex());
    
    // Create an index of where each class value starts
    int [] classIndices = new int [getInputFormat().numClasses() + 1];
    int currentClass = 0;
    classIndices[currentClass] = 0;
    for (int i = 0; i < getInputFormat().numInstances(); i++) {
      Instance current = getInputFormat().instance(i);
      if (current.classIsMissing()) {
	for (int j = currentClass + 1; j < classIndices.length; j++) {
	  classIndices[j] = i;
	}
	break;
      } else if (current.classValue() != currentClass) {
	for (int j = currentClass + 1; j <= current.classValue(); j++) {
	  classIndices[j] = i;
	}          
	currentClass = (int) current.classValue();
      }
    }
    if (currentClass <= getInputFormat().numClasses()) {
      for (int j = currentClass + 1; j < classIndices.length; j++) {
	classIndices[j] = getInputFormat().numInstances();
      }
    }
    
    int actualClasses = 0;
    for (int i = 0; i < classIndices.length - 1; i++) {
      if (classIndices[i] != classIndices[i + 1]) {
	actualClasses++;
      }
    }
    // Create the new sample
    
    Random random = new Random(m_RandomSeed);
    // Convert pending input instances
    for(int i = 0; i < sampleSize; i++) {
      int index = 0;
      if (random.nextDouble() < m_BiasToUniformClass) {
	// Pick a random class (of those classes that actually appear)
	int cIndex = random.nextInt(actualClasses);
	for (int j = 0, k = 0; j < classIndices.length - 1; j++) {
	  if ((classIndices[j] != classIndices[j + 1]) 
	      && (k++ >= cIndex)) {
	    // Pick a random instance of the designated class
	    index = classIndices[j] 
	      + random.nextInt(classIndices[j + 1] - classIndices[j]);
	    break;
	  }
	}
      } else {
	index = random.nextInt(origSize);
      }
      push((Instance)getInputFormat().instance(index).copy());
    }
  }
  
  
  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: 
   * use -h for help
   */
  public static void main(String [] argv) {

    try {
      if (Utils.getFlag('b', argv)) {
 	Filter.batchFilterFile(new Resample(), argv);
      } else {
	Filter.filterFile(new Resample(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}
