/*
 *    SpreadSubsampleFilter.java
 *    Copyright (C) 2000 Intelligenesis Corp.
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

package weka.filters;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.Option;
import weka.core.Utils;

import java.util.Random;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;

/** 
 * Produces a random subsample of a dataset. The original dataset must
 * fit entirely in memory. This filter allows you to specify the maximum
 * "spread" between the rarest and most common class. For example, you may
 * specify that there be at most a 2:1 difference in class frequencies.
 * When used in batch mode, subsequent batches are
 * <b>not</b> resampled.
 *
 * Valid options are:<p>
 *
 * -S num <br>
 * Specify the random number seed (default 1).<p>
 *
 * -M num <br>
 *  The maximum class distribution spread. <br>
 *  0 = no maximum spread, 1 = uniform distribution, 10 = allow at most a
 *  10:1 ratio between the classes (default 0)
 *  <p>
 *
 * -X num <br>
 *  The maximum count for any class value. <br>
 *  (default 0 = unlimited)
 *  <p>
 *
 * @author Stuart Inglis (stuart@intelligenesis.net)
 * @version $Revision: 1.2 $ 
 **/
public class SpreadSubsampleFilter extends Filter implements OptionHandler {

  /** The random number generator seed */
  private int m_RandomSeed = 1;

  /** The maximum count of any class */
  private int m_MaxCount;
  
  /** True if the first batch has been done */
  private boolean m_FirstBatchDone = false;

  /** True if the first batch has been done */
  private double m_DistributionSpread = 0;

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(1);

    newVector.addElement(new Option(
              "\tSpecify the random number seed (default 1)",
              "S", 1, "-S <num>"));
    newVector.addElement(new Option(
              "\tThe maximum class distribution spread.\n"
              +"\t0 = no maximum spread, 1 = uniform distribution, 10 = allow at most\n"
	      +"\ta 10:1 ratio between the classes (default 0)",
              "M", 1, "-M <num>"));
    newVector.addElement(new Option(
	      "\tThe maximum count for any class value (default 0 = unlimited).\n",
              "X", 0, "-X <num>"));

    return newVector.elements();
  }


  /**
   * Parses a list of options for this object. Valid options are:<p>
   *
   * -S num <br>
   * Specify the random number seed (default 1).<p>
   *
   * -M num <br>
   *  The maximum class distribution spread. <br>
   *  0 = no maximum spread, 1 = uniform distribution, 10 = allow at most a
   *  10:1 ratio between the classes (default 0)
   *  <p>
   *
   * -X num <br>
   *  The maximum count for any class value. <br>
   *  (default 0 = unlimited)
   *  <p>
   *
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    String seedString = Utils.getOption('S', options);
    if (seedString.length() != 0) {
      setRandomSeed(Integer.parseInt(seedString));
    } else {
      setRandomSeed(1);
    }

    String maxString = Utils.getOption('M', options);
    if (maxString.length() != 0) {
      setDistributionSpread(Double.valueOf(maxString).doubleValue());
    } else {
      setDistributionSpread(0);
    }

    String maxCount = Utils.getOption('X', options);
    if (maxCount.length() != 0) {
      setMaxCount(Double.valueOf(maxCount).doubleValue());
    } else {
      setMaxCount(0);
    }

    if (m_InputFormat != null) {
      inputFormat(m_InputFormat);
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

    options[current++] = "-M"; 
    options[current++] = "" + getDistributionSpread();

    options[current++] = "-X"; 
    options[current++] = "" + getMaxCount();

    options[current++] = "-S"; 
    options[current++] = "" + getRandomSeed();

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }
  
  
  /**
   * Sets the value for the distribution spread
   *
   * @param spread the new distribution spread
   */
  public void setDistributionSpread(double spread) {

    m_DistributionSpread = spread;
  }

  /**
   * Gets the value for the distribution spread
   *
   * @return the distribution spread
   */    
  public double getDistributionSpread() {

    return m_DistributionSpread;
  }
  
  /**
   * Sets the value for the max count
   *
   * @param spread the new max count
   */
  public void setMaxCount(double maxcount) {

    m_MaxCount = (int)maxcount;
  }

  /**
   * Gets the value for the max count
   *
   * @return the max count
   */    
  public double getMaxCount() {

    return m_MaxCount;
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
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input 
   * instance structure (any instances contained in the object are 
   * ignored - only the structure is required).
   * @return true if the outputFormat may be collected immediately
   * @exception Exception if the input format can't be set 
   * successfully
   */
  public boolean inputFormat(Instances instanceInfo) 
       throws Exception {

    m_InputFormat = new Instances(instanceInfo, 0);
    setOutputFormat(m_InputFormat);
    m_NewBatch = true;
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
   * @exception Exception if the input instance was not of the 
   * correct format or if there was a problem with the filtering.
   */
  public boolean input(Instance instance) throws Exception {

    if (m_InputFormat == null) {
      throw new Exception("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }
    if (m_FirstBatchDone) {
      push(instance);
      return true;
    } else {
      m_InputFormat.add(instance);
      return false;
    }
  }

  /**
   * Signify that this batch of input to the filter is finished. 
   * If the filter requires all instances prior to filtering,
   * output() may now be called to retrieve the filtered instances.
   *
   * @return true if there are instances pending output
   * @exception Exception if no input structure has been defined
   */
  public boolean batchFinished() throws Exception {

    Instance current;

    if (m_InputFormat == null) {
      throw new Exception("No input instance format defined");
    }

    // Do the subsample, and clear the input instances.
    createSubsample();
    m_InputFormat = new Instances(m_InputFormat, 0);

    m_NewBatch = true;
    m_FirstBatchDone = true;
    return (numPendingOutput() != 0);
  }


  /**
   * Creates a subsample of the current set of input instances. The output
   * instances are pushed onto the output queue for collection.
   */
  private void createSubsample() throws Exception {

    int classI = m_InputFormat.classIndex();
    if (classI == -1) {
      classI = m_InputFormat.numAttributes()-1;
    }

    m_InputFormat.setClassIndex(classI);

    if (m_InputFormat.classAttribute().isNominal() == false) {
      throw new Exception ("The class attribute must be nominal.");
    }

    // Sort according to class attribute.
    m_InputFormat.sort(classI);

    // Create an index of where each class value starts
    int [] classIndices = new int [m_InputFormat.numClasses() + 1];
    int currentClass = 0;
    classIndices[currentClass] = 0;
    for (int i = 0; i < m_InputFormat.numInstances(); i++) {
      Instance current = m_InputFormat.instance(i);
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
    if (currentClass <= m_InputFormat.numClasses()) {
      for (int j = currentClass + 1; j < classIndices.length; j++) {
        classIndices[j] = m_InputFormat.numInstances();
      }
    }

    Random random = new Random(m_RandomSeed);

    // Create the new distribution

    int [] counts = new int [m_InputFormat.numClasses()];
    int [] new_counts = new int [m_InputFormat.numClasses()];
    int min = -1;
	
    for (int i = 0; i < m_InputFormat.numInstances(); i++) {
      Instance current = m_InputFormat.instance(i);
      if (current.classIsMissing() == false) {
        counts[(int)current.classValue()]++;
      }
    }

    // find the class with the minimum number of instances
    for (int i = 0; i < counts.length; i++) {
      if ( (min < 0) && (counts[i] > 0) ) {
        min = counts[i];
      } else if ((counts[i] < min) && (counts[i] > 0)) {
        min = counts[i];
      }
    }

    if (min < 0) { 
	System.err.println("SpreadSubsampleFilter: *warning* none of the classes have any values in them.");
	return;
    }

    // determine the new distribution 
    for (int i = 0; i < counts.length; i++) {
      new_counts[i] = (int)Math.abs(Math.min(counts[i],
                                             min * m_DistributionSpread));
      if (m_DistributionSpread == 0) {
        new_counts[i] = counts[i];
      }

      if (m_MaxCount > 0) {
	  new_counts[i] = Math.min(new_counts[i], m_MaxCount);
      }
    }

    // Sample without replacement
    Hashtable t = new Hashtable();
    for(int j = 0; j < new_counts.length; j++) {
      for(int k = 0; k < new_counts[j]; k++) {
        boolean ok = false;
        do{
	  int index = classIndices[j] + (Math.abs(random.nextInt()) 
					   % (classIndices[j + 1] - classIndices[j])) ;
	  // Have we used this instance before?
          if (t.get("" + index) == null) {
            // if not, add it to the hashtable and use it
            t.put("" + index, "");
            ok = true;
	    if(index>=0)
		push((Instance)m_InputFormat.instance(index).copy());
          }
        } while (!ok);
      }
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
 	Filter.batchFilterFile(new SpreadSubsampleFilter(), argv);
      } else {
	Filter.filterFile(new SpreadSubsampleFilter(), argv);
      }
    } catch (Exception ex) {
		ex.printStackTrace();
      System.out.println(ex.getMessage());
    }
  }
}








