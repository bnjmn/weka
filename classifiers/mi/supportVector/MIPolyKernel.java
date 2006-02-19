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
 * MIPolyKernel.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.mi.supportVector;

import weka.classifiers.functions.supportVector.CachedKernel;
import weka.core.Instance;
import weka.core.Instances;

/**
 * The polynomial kernel : K(x, y) = &lt;x, y&gt;^p or K(x, y) = ( &lt;x,
 * y&gt;+1)^p <p/>
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Shane Legg (shane@intelligenesis.net) (sparse vector code)
 * @author Stuart Inglis (stuart@reeltwo.com) (sparse vector code)
 * @author Lin Dong (ld21@cs.waikato.ac.nz) (MIkernel)
 * @version $Revision: 1.1 $ 
 */

public class MIPolyKernel 
  extends CachedKernel {

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
  public MIPolyKernel(Instances dataset, int cacheSize, double exponent,
      boolean lowerOrder) {

    super(dataset, cacheSize);

    m_exponent = exponent;
    m_lowerOrder = lowerOrder;
  }

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
        if (m_lowerOrder) {
          result += 1.0; 
        }
        if (m_exponent != 1.0) {
          result = Math.pow(result, m_exponent);
        }

        res += result;
      }
    }

    return res;
  }
}




