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
 *    PolyKernel.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */

package weka.classifiers.functions.supportVector;

import weka.core.*;

/**
 * The polynomial kernel : K(x, y) = <x, y>^p or K(x, y) = ( <x, y>+1)^p
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Shane Legg (shane@intelligenesis.net) (sparse vector code)
 * @author Stuart Inglis (stuart@reeltwo.com) (sparse vector code)
 * @version $Revision: 1.3 $
 */
public class PolyKernel extends CachedKernel {

  /** Use lower-order terms? */
  private boolean m_lowerOrder = false;

  /** The exponent for the polynomial kernel. */
  private double m_exponent = 1.0;

  /**
   * Creates a new <code>PolyKernel</code> instance.
   * 
   * @param dataset
   *            the training dataset used.
   * @param cacheSize
   *            the size of the cache (a prime number)
   */
  public PolyKernel(Instances dataset, int cacheSize, double exponent,
		    boolean lowerOrder) {
		
    super(dataset, cacheSize);
		
    m_exponent = exponent;
    m_lowerOrder = lowerOrder;
    m_data = dataset;
  }

  protected double evaluate(int id1, int id2, Instance inst1)
    throws Exception {
		
    double result;
    if (id1 == id2) {
      result = dotProd(inst1, inst1);
    } else {
      result = dotProd(inst1, m_data.instance(id2));
    }
    // Use lower order terms?
    if (m_lowerOrder) {
      result += 1.0;
    }
    if (m_exponent != 1.0) {
      result = Math.pow(result, m_exponent);
    }
    return result;
  }

}
