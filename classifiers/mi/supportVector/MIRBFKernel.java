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
 * MIRBFKernel.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.mi.supportVector;

import weka.classifiers.functions.supportVector.RBFKernel;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.MultiInstanceCapabilitiesHandler;
import weka.core.RevisionUtils;
import weka.core.Capabilities.Capability;

/**
 <!-- globalinfo-start -->
 * The RBF kernel. K(x, y) = e^-(gamma * &lt;x-y, x-y&gt;^2)
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
 * <pre> -no-checks
 *  Turns off all checks - use with caution!
 *  (default: checks on)</pre>
 * 
 * <pre> -C &lt;num&gt;
 *  The size of the cache (a prime number).
 *  (default: 250007)</pre>
 * 
 * <pre> -G &lt;num&gt;
 *  The Gamma parameter.
 *  (default: 0.01)</pre>
 * 
 <!-- options-end -->
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Shane Legg (shane@intelligenesis.net) (sparse vector code)
 * @author Stuart Inglis (stuart@reeltwo.com) (sparse vector code)
 * @author J. Lindgren (jtlindgr{at}cs.helsinki.fi) (RBF kernel)
 * @author Lin Dong (ld21@cs.waikato.ac.nz) (MIkernel)
 * @version $Revision: 1.3 $ 
 */
public class MIRBFKernel 
  extends RBFKernel
  implements MultiInstanceCapabilitiesHandler {

  /** for serialiation */
  private static final long serialVersionUID = -8711882393708956962L;
  
  /** The precalculated dotproducts of &lt;inst_i,inst_i&gt; */
  protected double m_kernelPrecalc[][];

  /**
   * default constructor - does nothing.
   */
  public MIRBFKernel() {
    super();
  }

  /**
   * Constructor. 
   * 
   * @param data	the data to use
   * @param cacheSize	the size of the cache
   * @param gamma	the bandwidth
   * @throws Exception	if something goes wrong
   */
  public MIRBFKernel(Instances data, int cacheSize, double gamma)
    throws Exception {

    super(data, cacheSize, gamma);
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

    double result = 0;
    Instances insts1, insts2;
    if (id1 == -1)
      insts1 = new Instances(inst1.relationalValue(1));
    else
      insts1 = new Instances(m_data.instance(id1).relationalValue(1));
    insts2 = new Instances (m_data.instance(id2).relationalValue(1));

    double precalc1=0;
    for(int i = 0; i < insts1.numInstances(); i++){
      for (int j = 0; j < insts2.numInstances(); j++){
        if (id1 == -1)
          precalc1 = dotProd(insts1.instance(i), insts1.instance(i));
        else 
          precalc1 =  m_kernelPrecalc[id1][i];

        double res = Math.exp(m_gamma*(2. * dotProd(insts1.instance(i), insts2.instance(j)) -precalc1 -  m_kernelPrecalc[id2][j] ) );

        result += res;
      }
    }

    return result;
  }   

  /**
   * initializes variables etc.
   * 
   * @param data	the data to use
   */
  protected void initVars(Instances data) {
    super.initVars(data);
    
    m_kernelPrecalc = new double[data.numInstances()][];
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
    result.enable(Capability.MISSING_VALUES);

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
   * builds the kernel with the given data. Initializes the kernel cache. 
   * The actual size of the cache in bytes is (64 * cacheSize).
   * 
   * @param data	the data to base the kernel on
   * @throws Exception	if something goes wrong
   */
  public void buildKernel(Instances data) throws Exception {
    // does kernel handle the data?
    if (!getChecksTurnedOff())
      getCapabilities().testWithFail(data);
    
    initVars(data);

    for (int i = 0; i < data.numInstances(); i++){
      Instances insts = new Instances(data.instance(i).relationalValue(1));
      m_kernelPrecalc[i] = new double [insts.numInstances()];
      for (int j = 0; j < insts.numInstances(); j++)
        m_kernelPrecalc[i][j] = dotProd(insts.instance(j), insts.instance(j));
    }
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 1.3 $");
  }
}
