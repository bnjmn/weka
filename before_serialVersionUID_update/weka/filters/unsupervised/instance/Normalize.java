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
 *    Normalize.java
 *    Copyright (C) 2003 Prados Julien
 *
 */


package weka.filters.unsupervised.instance;

import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.filters.Filter;
import weka.filters.UnsupervisedFilter;

import java.util.Enumeration;
import java.util.Vector;

/** 
 <!-- globalinfo-start -->
 * An instance filter that normalize instances considering only numeric attributes and ignoring class index.
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -N &lt;num&gt;
 *  Specify the norm that each instance must have (default 1.0)</pre>
 * 
 * <pre> -L &lt;num&gt;
 *  Specify L-norm to use (default 2.0)</pre>
 * 
 <!-- options-end -->
 *
 * @author Julien Prados
 * @version $Revision: 1.5 $
 */
public class Normalize 
  extends Filter 
  implements UnsupervisedFilter, OptionHandler {
  
  /** for serialization */
  static final long serialVersionUID = -7947971807522917395L;

  /** The norm that each instance must have at the end */
  protected double m_Norm = 1.0;

  /** The L-norm to use */
  protected double m_LNorm = 2.0;

  
  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "An instance filter that normalize instances considering only numeric "+
           "attributes and ignoring class index.";
  }  
  
  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String LNormTipText() { 
    return "The LNorm to use.";
  }
  
  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String normTipText() { 
    return "The norm of the instances after normalization.";
  }
  
  
  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector newVector = new Vector(2);
    newVector.addElement(new Option(
              "\tSpecify the norm that each instance must have (default 1.0)",
              "N", 1, "-N <num>"));
    newVector.addElement(new Option(
              "\tSpecify L-norm to use (default 2.0)",
              "L", 1, "-L <num>"));
    return newVector.elements();
  }


  /**
   * Parses a given list of options. <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -N &lt;num&gt;
   *  Specify the norm that each instance must have (default 1.0)</pre>
   * 
   * <pre> -L &lt;num&gt;
   *  Specify L-norm to use (default 2.0)</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    String normString = Utils.getOption('N', options);
    if (normString.length() != 0) {
      setNorm(Double.parseDouble(normString));
    } else {
      setNorm(1.0);
    }

    String lNormString = Utils.getOption('L', options);
    if (lNormString.length() != 0) {
      setLNorm(Double.parseDouble(lNormString));
    } else {
      setLNorm(2.0);
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
    String [] options = new String [4];
    int current = 0;

    options[current++] = "-N"; 
    options[current++] = "" + getNorm();

    options[current++] = "-L"; 
    options[current++] = "" + getLNorm();
    
    return options;
  }

  
  /**
   * Get the instance's Norm.
   *
   * @return the Norm
   */
  public double getNorm() {
    return m_Norm;
  }
  
  /**
   * Set the norm of the instances
   *
   * @param newNorm the norm to wich the instances must be set
   */
  public void setNorm(double newNorm) {
    m_Norm = newNorm;
  }
  
  /**
   * Get the L Norm used.
   *
   * @return the L-norm used
   */
  public double getLNorm() {
    return m_LNorm;
  }
  
  /**
   * Set the L-norm to used
   *
   * @param newLNorm the L-norm
   */
  public void setLNorm(double newLNorm) {
    m_LNorm = newLNorm;
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
    result.enableAllClasses();
    result.enable(Capability.MISSING_CLASS_VALUES);
    result.enable(Capability.NO_CLASS);
    
    return result;
  }
  
  /**
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input instance
   * structure (any instances contained in the object are ignored - only the
   * structure is required).
   * @return true if the outputFormat may be collected immediately
   * @throws Exception if format cannot be processed
   */
  public boolean setInputFormat(Instances instanceInfo) throws Exception {
    /* CHECK REMOVE, BECAUSE THE FILTER IS APPLIED ONLY TO NUMERIC ATTRIBUTE
     * IN input()
    //check if all attributes are numeric
    for(int i=0;i<instanceInfo.numAttributes();i++){
        if (!instanceInfo.attribute(i).isNumeric() 
            && instanceInfo.classIndex() != i){
            throw Exception("All the attributes must be numeric");
        }
    }
    */
      
    super.setInputFormat(instanceInfo);
    setOutputFormat(instanceInfo);
    return true;
  }

  
  /**
   * Input an instance for filtering. Ordinarily the instance is processed
   * and made available for output immediately. Some filters require all
   * instances be read before producing output.
   *
   * @param instance the input instance
   * @return true if the filtered instance may now be
   * collected with output().
   * @throws IllegalStateException if no input format has been defined.
   */
  public boolean input(Instance instance) throws Exception {
    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }
    Instance inst = (Instance) instance.copy();

    //compute norm of inst
    double iNorm = 0;
    for(int i=0;i<getInputFormat().numAttributes();i++){
        if (getInputFormat().classIndex() == i) continue;
        if (!getInputFormat().attribute(i).isNumeric()) continue;
        iNorm += Math.pow(Math.abs(inst.value(i)),getLNorm());
    }
    iNorm = Math.pow(iNorm,1.0/getLNorm());
    
    //normalize inst
    for(int i=0;i<getInputFormat().numAttributes();i++){
        if (getInputFormat().classIndex() == i) continue;
        if (!getInputFormat().attribute(i).isNumeric()) continue;
        inst.setValue(i,inst.value(i)/iNorm*getNorm());
    }
    
    push(inst);
    return true;
  }  
  



  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: use -h for help
   */
  public static void main(String [] argv) {
    runFilter(new Normalize(), argv);
  }
}
