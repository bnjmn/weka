/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * MIPolyKernel.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.mi.supportVector;

import weka.classifiers.functions.supportVector.PolyKernel;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.MultiInstanceCapabilitiesHandler;
import weka.core.RevisionUtils;
import weka.core.Capabilities.Capability;

/**
 <!-- globalinfo-start -->
 * The polynomial kernel : K(x, y) = &lt;x, y&gt;^p or K(x, y) = (&lt;x, y&gt;+1)^p
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -D
 *  Enables debugging output (if available) to be printed.
 *  (default: off)</pre>
 *
 * <pre> -C &lt;num&gt;
 *  The size of the cache (a prime number), 0 for full cache and 
 *  -1 to turn it off.
 *  (default: 250007)</pre>
 * 
 * <pre> -E &lt;num&gt;
 *  The Exponent to use.
 *  (default: 1.0)</pre>
 * 
 * <pre> -L
 *  Use lower-order terms.
 *  (default: no)</pre>
 * 
 <!-- options-end -->
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Shane Legg (shane@intelligenesis.net) (sparse vector code)
 * @author Stuart Inglis (stuart@reeltwo.com) (sparse vector code)
 * @author Lin Dong (ld21@cs.waikato.ac.nz) (MIkernel)
 * @version $Revision$ 
 */
public class MIPolyKernel 
  extends PolyKernel 
  implements MultiInstanceCapabilitiesHandler {

  /** for serialiation */
  private static final long serialVersionUID = 7926421479341051777L;

  /**
   * default constructor - does nothing.
   */
  public MIPolyKernel() {
    super();
  }

  /**
   * Creates a new <code>MIPolyKernel</code> instance.
   * 
   * @param data	the training dataset used.
   * @param cacheSize	the size of the cache (a prime number)
   * @param exponent	the exponent to use
   * @param lowerOrder	whether to use lower-order terms
   * @throws Exception	if something goes wrong
   */
  public MIPolyKernel(Instances data, int cacheSize, double exponent,
      boolean lowerOrder) throws Exception {

    super(data, cacheSize, exponent, lowerOrder);
  }

  /**
   * 
   * @param id1   	the index of instance 1
   * @param id2		the index of instance 2
   * @param inst1	the instance 1 object
   * @return 		the dot product
   * @throws Exception 	if something goes wrong
   */
  protected double evaluate(int id1, int id2, Instance inst1)
    throws Exception {

    double result, res;
    Instances data1= new Instances(inst1.relationalValue(1));
    Instances data2;
    if(id1==id2)
      data2= new Instances(data1);
    else
      data2 = new Instances (m_data.instance(id2).relationalValue(1));

    res=0;
    for(int i=0; i<data1.numInstances();i++){
      for (int j=0; j<data2.numInstances(); j++){
        result = dotProd(data1.instance(i), data2.instance(j));

        // Use lower order terms?
        if (getUseLowerOrder()) {
          result += 1.0; 
        }
        if (getExponent() != 1.0) {
          result = Math.pow(result, getExponent());
        }

        res += result;
      }
    }

    return res;
  }

  /** 
   * Returns the Capabilities of this kernel.
   *
   * @return            the capabilities of this object
   * @see               Capabilities
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.RELATIONAL_ATTRIBUTES);
    result.disable(Capability.MISSING_VALUES);

    // class
    result.enableAllClasses();

    // other
    result.enable(Capability.ONLY_MULTIINSTANCE);
    
    return result;
  }

  /**
   * Returns the capabilities of this multi-instance kernel for the
   * relational data.
   *
   * @return            the capabilities of this object
   * @see               Capabilities
   */
  public Capabilities getMultiInstanceCapabilities() {
    Capabilities result = super.getCapabilities();
    
    // class
    result.disableAllClasses();
    result.enable(Capability.NO_CLASS);
    
    return result;
  }
  
  /**
   * Frees the cache used by the kernel.
   */
  public void clean() {
    m_storage = null;
    m_keys = null;
    m_kernelMatrix = null;
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

