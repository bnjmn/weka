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

import weka.core.*;

/**
 * The normalized polynomial kernel. 
 * K(x,y) = <x,y>/sqrt(<x,x><y,y>) where <x,y> = PolyKernel(x,y)
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.1.2.2 $
 */
public class NormalizedPolyKernel extends PolyKernel {


  /**
   * Creates a new <code>NormalizedPolyKernel</code> instance.
   *
   * @param dataset the training dataset used.
   * @param cacheSize the size of the cache (a prime number)
   */
  public NormalizedPolyKernel(Instances dataset, int cacheSize, double exponent, boolean lowerOrder){
	
    super(dataset, cacheSize, exponent, lowerOrder);
  }
 
   
  /**
   * Redefines the eval function of PolyKernel.
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
}
