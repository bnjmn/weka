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
 *    NormalizedPolyKernel.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */

package weka.classifiers.functions.supportVector;

import weka.core.Instance;
import weka.core.Instances;

/**
 <!-- globalinfo-start -->
 <!-- globalinfo-end -->
 * 
 <!-- options-start -->
 <!-- options-end -->
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.4 $
 */
public class NormalizedPolyKernel 
  extends PolyKernel {

  /** for serialization */
  static final long serialVersionUID = 1248574185532130851L;

  /**
   * default constructor - does nothing
   */
  public NormalizedPolyKernel() {
    super();

    setExponent(2.0);
  }
  
  /**
   * Creates a new <code>NormalizedPolyKernel</code> instance.
   *
   * @param dataset	the training dataset used.
   * @param cacheSize	the size of the cache (a prime number)
   * @param exponent	the exponent to use
   * @param lowerOrder	whether to use lower-order terms
   * @throws Exception	if something goes wrong
   */
  public NormalizedPolyKernel(Instances dataset, int cacheSize, 
      double exponent, boolean lowerOrder) throws Exception {
	
    super(dataset, cacheSize, exponent, lowerOrder);
  }
  
  /**
   * Returns a string describing the kernel
   * 
   * @return a description suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return 
        "The normalized polynomial kernel.\n"
      + "K(x,y) = <x,y>/sqrt(<x,x><y,y>) where <x,y> = PolyKernel(x,y)";
  }
   
  /**
   * Computes the result of the kernel function for two instances.
   * If id1 == -1, eval use inst1 instead of an instance in the dataset.
   * Redefines the eval function of PolyKernel.
   *
   * @param id1 the index of the first instance in the dataset
   * @param id2 the index of the second instance in the dataset
   * @param inst1 the instance corresponding to id1 (used if id1 == -1)
   * @return the result of the kernel function
   * @throws Exception if something goes wrong
   */
  public double eval(int id1, int id2, Instance inst1) 
    throws Exception {

    double div = Math.sqrt(super.eval(id1, id1, inst1) * ((m_keys != null)
                           ? super.eval(id2, id2, m_data.instance(id2))
                           : super.eval(-1, -1, m_data.instance(id2))));

    if(div != 0){      
      return super.eval(id1, id2, inst1) / div;
    } else {
      return 0;
    }
  }    
  
  /**
   * Sets the exponent value (must be different from 1.0).
   * 
   * @param value	the exponent value
   */
  public void setExponent(double value) {
    if (value != 1.0)
      super.setExponent(value);
    else
      System.out.println("A linear kernel, i.e., Exponent=1, is not possible!");
  }
  
  /**
   * returns a string representation for the Kernel
   * 
   * @return 		a string representaiton of the kernel
   */
  public String toString() {
    String	result;
    
    if (getUseLowerOrder())
      result = "Normalized Poly Kernel with lower order: K(x,y) = (<x,y>+1)^" + getExponent() + "/" + 
      	       "((<x,x>+1)^" + getExponent() + "*" + "(<y,y>+1)^" + getExponent() + ")^(1/2)";
    else
      result = "Normalized Poly Kernel: K(x,y) = <x,y>^" + getExponent() + "/" + "(<x,x>^" + 
               getExponent() + "*" + "<y,y>^" + getExponent() + ")^(1/2)";
    
    return result;
  }
}
