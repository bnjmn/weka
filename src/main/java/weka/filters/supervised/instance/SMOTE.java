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
 * SMOTE.java
 * 
 * Copyright (C) 2008 Ryan Lichtenwalter 
 * Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.supervised.instance;

import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.filters.Filter;
import weka.filters.SupervisedFilter;

import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * Resamples a dataset by applying the Synthetic Minority Oversampling TEchnique (SMOTE). The original dataset must fit entirely in memory. The amount of SMOTE and number of nearest neighbors may be specified. For more information, see <br/>
 * <br/>
 * Nitesh V. Chawla et. al. (2002). Synthetic Minority Over-sampling Technique. Journal of Artificial Intelligence Research. 16:321-357.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;article{al.2002,
 *    author = {Nitesh V. Chawla et. al.},
 *    journal = {Journal of Artificial Intelligence Research},
 *    pages = {321-357},
 *    title = {Synthetic Minority Over-sampling Technique},
 *    volume = {16},
 *    year = {2002}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -S &lt;num&gt;
 *  Specifies the random number seed
 *  (default 1)</pre>
 * 
 * <pre> -P &lt;percentage&gt;
 *  Specifies percentage of SMOTE instances to create.
 *  (default 100.0)
 * </pre>
 * 
 * <pre> -K &lt;nearest-neighbors&gt;
 *  Specifies the number of nearest neighbors to use.
 *  (default 5)
 * </pre>
 * 
 * <pre> -C &lt;value-index&gt;
 *  Specifies the index of the nominal class value to SMOTE
 *  (default 0: auto-detect non-empty minority class))
 * </pre>
 * 
 <!-- options-end -->
 *  
 * @author Ryan Lichtenwalter (rlichtenwalter@gmail.com)
 * @version $Revision$
 */
public class SMOTE
  extends Filter 
  implements SupervisedFilter, OptionHandler, TechnicalInformationHandler {

  /** for serialization. */
  static final long serialVersionUID = -1653880819059250364L;

  /** the number of neighbors to use. */
  protected int m_NearestNeighbors = 5;
  
  /** the random seed to use. */
  protected int m_RandomSeed = 1;
  
  /** the percentage of SMOTE instances to create. */
  protected double m_Percentage = 100.0;
  
  /** the index of the class value. */
  protected String m_ClassValueIndex = "0";
  
  /** whether to detect the minority class automatically. */
  protected boolean m_DetectMinorityClass = true;

  /**
   * Returns a string describing this classifier.
   * 
   * @return 		a description of the classifier suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Resamples a dataset by applying the Synthetic Minority Oversampling TEchnique (SMOTE)." +
    " The original dataset must fit entirely in memory." +
    " The amount of SMOTE and number of nearest neighbors may be specified." +
    " For more information, see \n\n" 
    + getTechnicalInformation().toString();
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing 
   * detailed information about the technical background of this class,
   * e.g., paper reference or book this class is based on.
   * 
   * @return 		the technical information about this class
   */
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation result = new TechnicalInformation(Type.ARTICLE);

    result.setValue(Field.AUTHOR, "Nitesh V. Chawla et. al.");
    result.setValue(Field.TITLE, "Synthetic Minority Over-sampling Technique");
    result.setValue(Field.JOURNAL, "Journal of Artificial Intelligence Research");
    result.setValue(Field.YEAR, "2002");
    result.setValue(Field.VOLUME, "16");
    result.setValue(Field.PAGES, "321-357");

    return result;
  }

  /**
   * Returns the revision string.
   * 
   * @return 		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }

  /** 
   * Returns the Capabilities of this filter.
   *
   * @return            the capabilities of this object
   * @see               Capabilities
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enableAllAttributes();
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);

    return result;
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector newVector = new Vector();
    
    newVector.addElement(new Option(
	"\tSpecifies the random number seed\n"
	+ "\t(default 1)",
	"S", 1, "-S <num>"));
    
    newVector.addElement(new Option(
	"\tSpecifies percentage of SMOTE instances to create.\n"
	+ "\t(default 100.0)\n",
	"P", 1, "-P <percentage>"));
    
    newVector.addElement(new Option(
	"\tSpecifies the number of nearest neighbors to use.\n"
	+ "\t(default 5)\n",
	"K", 1, "-K <nearest-neighbors>"));
    
    newVector.addElement(new Option(
	"\tSpecifies the index of the nominal class value to SMOTE\n"
	+"\t(default 0: auto-detect non-empty minority class))\n",
	"C", 1, "-C <value-index>"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options.
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -S &lt;num&gt;
   *  Specifies the random number seed
   *  (default 1)</pre>
   * 
   * <pre> -P &lt;percentage&gt;
   *  Specifies percentage of SMOTE instances to create.
   *  (default 100.0)
   * </pre>
   * 
   * <pre> -K &lt;nearest-neighbors&gt;
   *  Specifies the number of nearest neighbors to use.
   *  (default 5)
   * </pre>
   * 
   * <pre> -C &lt;value-index&gt;
   *  Specifies the index of the nominal class value to SMOTE
   *  (default 0: auto-detect non-empty minority class))
   * </pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    String seedStr = Utils.getOption('S', options);
    if (seedStr.length() != 0) {
      setRandomSeed(Integer.parseInt(seedStr));
    } else {
      setRandomSeed(1);
    }

    String percentageStr = Utils.getOption('P', options);
    if (percentageStr.length() != 0) {
      setPercentage(new Double(percentageStr).doubleValue());
    } else {
      setPercentage(100.0);
    }

    String nnStr = Utils.getOption('K', options);
    if (nnStr.length() != 0) {
      setNearestNeighbors(Integer.parseInt(nnStr));
    } else {
      setNearestNeighbors(5);
    }

    String classValueIndexStr = Utils.getOption( 'C', options);
    if (classValueIndexStr.length() != 0) {
      setClassValue(classValueIndexStr);
    } else {
      m_DetectMinorityClass = true;
    }
  }

  /**
   * Gets the current settings of the filter.
   *
   * @return an array 	of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    Vector<String>	result;
    
    result = new Vector<String>();
    
    result.add("-C");
    result.add(getClassValue());
    
    result.add("-K");
    result.add("" + getNearestNeighbors());
    
    result.add("-P");
    result.add("" + getPercentage());
    
    result.add("-S");
    result.add("" + getRandomSeed());
    
    return result.toArray(new String[result.size()]);
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String randomSeedTipText() {
    return "The seed used for random sampling.";
  }

  /**
   * Gets the random number seed.
   *
   * @return 		the random number seed.
   */
  public int getRandomSeed() {
    return m_RandomSeed;
  }

  /**
   * Sets the random number seed.
   *
   * @param value 	the new random number seed.
   */
  public void setRandomSeed(int value) {
    m_RandomSeed = value;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String percentageTipText() {
    return "The percentage of SMOTE instances to create.";
  }

  /**
   * Sets the percentage of SMOTE instances to create.
   * 
   * @param value	the percentage to use
   */
  public void setPercentage(double value) {
    if (value >= 0)
      m_Percentage = value;
    else
      System.err.println("Percentage must be >= 0!");
  }

  /**
   * Gets the percentage of SMOTE instances to create.
   * 
   * @return 		the percentage of SMOTE instances to create
   */
  public double getPercentage() {
    return m_Percentage;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String nearestNeighborsTipText() {
    return "The number of nearest neighbors to use.";
  }

  /**
   * Sets the number of nearest neighbors to use.
   * 
   * @param value	the number of nearest neighbors to use
   */
  public void setNearestNeighbors(int value) {
    if (value >= 1)
      m_NearestNeighbors = value;
    else
      System.err.println("At least 1 neighbor necessary!");
  }

  /**
   * Gets the number of nearest neighbors to use.
   * 
   * @return 		the number of nearest neighbors to use
   */
  public int getNearestNeighbors() {
    return m_NearestNeighbors;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String classValueTipText() {
    return "The index of the class value to which SMOTE should be applied. " +
    "Use a value of 0 to auto-detect the non-empty minority class.";
  }

  /**
   * Sets the index of the class value to which SMOTE should be applied.
   * 
   * @param value	the class value index
   */
  public void setClassValue(String value) {
    m_ClassValueIndex = value;
    if (m_ClassValueIndex.equals("0")) {
      m_DetectMinorityClass = true;
    } else {
      m_DetectMinorityClass = false;
    }
  }

  /**
   * Gets the index of the class value to which SMOTE should be applied.
   * 
   * @return 		the index of the clas value to which SMOTE should be applied
   */
  public String getClassValue() {
    return m_ClassValueIndex;
  }

  /**
   * Sets the format of the input instances.
   *
   * @param instanceInfo 	an Instances object containing the input 
   * 				instance structure (any instances contained in 
   * 				the object are ignored - only the structure is required).
   * @return 			true if the outputFormat may be collected immediately
   * @throws Exception 		if the input format can't be set successfully
   */
  public boolean setInputFormat(Instances instanceInfo) throws Exception {
    super.setInputFormat(instanceInfo);
    super.setOutputFormat(instanceInfo);
    return true;
  }

  /**
   * Input an instance for filtering. Filter requires all
   * training instances be read before producing output.
   *
   * @param instance 		the input instance
   * @return 			true if the filtered instance may now be
   * 				collected with output().
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
   * @return 		true if there are instances pending output
   * @throws IllegalStateException if no input structure has been defined
   * @throws Exception 	if provided options cannot be executed 
   * 			on input instances
   */
  public boolean batchFinished() throws Exception {
    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }

    if (!m_FirstBatchDone) {
      // Do SMOTE, and clear the input instances.
      doSMOTE();
    }
    flushInput();

    m_NewBatch = true;
    m_FirstBatchDone = true;
    return (numPendingOutput() != 0);
  }

  /**
   * The procedure implementing the SMOTE algorithm. The output
   * instances are pushed onto the output queue for collection.
   * 
   * @throws Exception 	if provided options cannot be executed 
   * 			on input instances
   */
  protected void doSMOTE() throws Exception {
    int minIndex = 0;
    int min = Integer.MAX_VALUE;
    if (m_DetectMinorityClass) {
      // find minority class
      int[] classCounts = getInputFormat().attributeStats(getInputFormat().classIndex()).nominalCounts;
      for (int i = 0; i < classCounts.length; i++) {
	if (classCounts[i] != 0 && classCounts[i] < min) {
	  min = classCounts[i];
	  minIndex = i;
	}
      }
    } else {
      String classVal = getClassValue();
      if (classVal.equalsIgnoreCase("first")) {
	minIndex = 1;
      } else if (classVal.equalsIgnoreCase("last")) {
	minIndex = getInputFormat().numClasses();
      } else {
	minIndex = Integer.parseInt(classVal);
      }
      if (minIndex > getInputFormat().numClasses()) {
	throw new Exception("value index must be <= the number of classes");
      }
      minIndex--; // make it an index
    }

    int nearestNeighbors;
    if (min <= getNearestNeighbors()) {
      nearestNeighbors = min - 1;
    } else {
      nearestNeighbors = getNearestNeighbors();
    }
    if (nearestNeighbors < 1)
      throw new Exception("Cannot use 0 neighbors!");

    // compose minority class dataset
    // also push all dataset instances
    Instances sample = getInputFormat().stringFreeStructure();
    Enumeration instanceEnum = getInputFormat().enumerateInstances();
    while(instanceEnum.hasMoreElements()) {
      Instance instance = (Instance) instanceEnum.nextElement();
      push((Instance) instance.copy());
      if ((int) instance.classValue() == minIndex) {
	sample.add(instance);
      }
    }

    // compute Value Distance Metric matrices for nominal features
    Map vdmMap = new HashMap();
    Enumeration attrEnum = getInputFormat().enumerateAttributes();
    while(attrEnum.hasMoreElements()) {
      Attribute attr = (Attribute) attrEnum.nextElement();
      if (!attr.equals(getInputFormat().classAttribute())) {
	if (attr.isNominal() || attr.isString()) {
	  double[][] vdm = new double[attr.numValues()][attr.numValues()];
	  vdmMap.put(attr, vdm);
	  int[] featureValueCounts = new int[attr.numValues()];
	  int[][] featureValueCountsByClass = new int[getInputFormat().classAttribute().numValues()][attr.numValues()];
	  instanceEnum = getInputFormat().enumerateInstances();
	  while(instanceEnum.hasMoreElements()) {
	    Instance instance = (Instance) instanceEnum.nextElement();
	    int value = (int) instance.value(attr);
	    int classValue = (int) instance.classValue();
	    featureValueCounts[value]++;
	    featureValueCountsByClass[classValue][value]++;
	  }
	  for (int valueIndex1 = 0; valueIndex1 < attr.numValues(); valueIndex1++) {
	    for (int valueIndex2 = 0; valueIndex2 < attr.numValues(); valueIndex2++) {
	      double sum = 0;
	      for (int classValueIndex = 0; classValueIndex < getInputFormat().numClasses(); classValueIndex++) {
		double c1i = (double) featureValueCountsByClass[classValueIndex][valueIndex1];
		double c2i = (double) featureValueCountsByClass[classValueIndex][valueIndex2];
		double c1 = (double) featureValueCounts[valueIndex1];
		double c2 = (double) featureValueCounts[valueIndex2];
		double term1 = c1i / c1;
		double term2 = c2i / c2;
		sum += Math.abs(term1 - term2);
	      }
	      vdm[valueIndex1][valueIndex2] = sum;
	    }
	  }
	}
      }
    }

    // use this random source for all required randomness
    Random rand = new Random(getRandomSeed());

    // find the set of extra indices to use if the percentage is not evenly divisible by 100
    List extraIndices = new LinkedList();
    double percentageRemainder = (getPercentage() / 100) - Math.floor(getPercentage() / 100.0);
    int extraIndicesCount = (int) (percentageRemainder * sample.numInstances());
    if (extraIndicesCount >= 1) {
      for (int i = 0; i < sample.numInstances(); i++) {
	extraIndices.add(i);
      }
    }
    Collections.shuffle(extraIndices, rand);
    extraIndices = extraIndices.subList(0, extraIndicesCount);
    Set extraIndexSet = new HashSet(extraIndices);

    // the main loop to handle computing nearest neighbors and generating SMOTE
    // examples from each instance in the original minority class data
    Instance[] nnArray = new Instance[nearestNeighbors];
    for (int i = 0; i < sample.numInstances(); i++) {
      Instance instanceI = sample.instance(i);
      // find k nearest neighbors for each instance
      List distanceToInstance = new LinkedList();
      for (int j = 0; j < sample.numInstances(); j++) {
	Instance instanceJ = sample.instance(j);
	if (i != j) {
	  double distance = 0;
	  attrEnum = getInputFormat().enumerateAttributes();
	  while(attrEnum.hasMoreElements()) {
	    Attribute attr = (Attribute) attrEnum.nextElement();
	    if (!attr.equals(getInputFormat().classAttribute())) {
	      double iVal = instanceI.value(attr);
	      double jVal = instanceJ.value(attr);
	      if (attr.isNumeric()) {
		distance += Math.pow(iVal - jVal, 2);
	      } else {
		distance += ((double[][]) vdmMap.get(attr))[(int) iVal][(int) jVal];
	      }
	    }
	  }
	  distance = Math.pow(distance, .5);
	  distanceToInstance.add(new Object[] {distance, instanceJ});
	}
      }

      // sort the neighbors according to distance
      Collections.sort(distanceToInstance, new Comparator() {
	public int compare(Object o1, Object o2) {
	  double distance1 = (Double) ((Object[]) o1)[0];
	  double distance2 = (Double) ((Object[]) o2)[0];
	  return (int) Math.ceil(distance1 - distance2);
	}
      });

      // populate the actual nearest neighbor instance array
      Iterator entryIterator = distanceToInstance.iterator();
      int j = 0;
      while(entryIterator.hasNext() && j < nearestNeighbors) {
	nnArray[j] = (Instance) ((Object[])entryIterator.next())[1];
	j++;
      }

      // create synthetic examples
      int n = (int) Math.floor(getPercentage() / 100);
      while(n > 0 || extraIndexSet.remove(i)) {
	double[] values = new double[sample.numAttributes()];
	int nn = rand.nextInt(nearestNeighbors);
	attrEnum = getInputFormat().enumerateAttributes();
	while(attrEnum.hasMoreElements()) {
	  Attribute attr = (Attribute) attrEnum.nextElement();
	  if (!attr.equals(getInputFormat().classAttribute())) {
	    if (attr.isNumeric()) {
	      double dif = nnArray[nn].value(attr) - instanceI.value(attr);
	      double gap = rand.nextDouble();
	      values[attr.index()] = (double) (instanceI.value(attr) + gap * dif);
	    } else if (attr.isDate()) {
	      double dif = nnArray[nn].value(attr) - instanceI.value(attr);
	      double gap = rand.nextDouble();
	      values[attr.index()] = (long) (instanceI.value(attr) + gap * dif);
	    } else {
	      int[] valueCounts = new int[attr.numValues()];
	      int iVal = (int) instanceI.value(attr);
	      valueCounts[iVal]++;
	      for (int nnEx = 0; nnEx < nearestNeighbors; nnEx++) {
		int val = (int) nnArray[nnEx].value(attr);
		valueCounts[val]++;
	      }
	      int maxIndex = 0;
	      int max = Integer.MIN_VALUE;
	      for (int index = 0; index < attr.numValues(); index++) {
		if (valueCounts[index] > max) {
		  max = valueCounts[index];
		  maxIndex = index;
		}
	      }
	      values[attr.index()] = maxIndex;
	    }
	  }
	}
	values[sample.classIndex()] = minIndex;
	Instance synthetic = new Instance(1.0, values);
	push(synthetic);
	n--;
      }
    }
  }

  /**
   * Main method for running this filter.
   *
   * @param args 	should contain arguments to the filter: 
   * 			use -h for help
   */
  public static void main(String[] args) {
    runFilter(new SMOTE(), args);
  }
}
