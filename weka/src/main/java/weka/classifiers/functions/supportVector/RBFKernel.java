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
 *    RBFKernel.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */

package weka.classifiers.functions.supportVector;

import weka.core.*;

/**
 * The RBF kernel. K(x, y) = e^-(gamma * <x-y, x-y>^2)
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Shane Legg (shane@intelligenesis.net) (sparse vector code)
 * @author Stuart Inglis (stuart@reeltwo.com) (sparse vector code)
 * @author J. Lindgren (jtlindgr{at}cs.helsinki.fi) (RBF kernel)
 * @version $Revision: 1.1.2.2 $
 */
public class RBFKernel extends CachedKernel {

  /** The precalculated dotproducts of <inst_i,inst_i> */
  private double m_kernelPrecalc[];

  /** Gamma for the RBF kernel. */
  private double m_gamma = 0.01;

  /**
   * Constructor. Initializes m_kernelPrecalc[].
   */
  public RBFKernel(Instances data, int cacheSize, double gamma)
    throws Exception {

    super(data, cacheSize);
    m_gamma = gamma;
    m_kernelPrecalc = new double[data.numInstances()];
    for (int i = 0; i < data.numInstances(); i++)
      m_kernelPrecalc[i] = dotProd(data.instance(i), data.instance(i));
  }

  protected double evaluate(int id1, int id2, Instance inst1)
    throws Exception {

    double precalc1;
    if (id1 == -1)
      precalc1 = dotProd(inst1, inst1);
    else
      precalc1 = m_kernelPrecalc[id1];
    Instance inst2 = m_data.instance(id2);
    double result = Math.exp(m_gamma
			     * (2. * dotProd(inst1, inst2) - precalc1 - m_kernelPrecalc[id2]));
    return result;
  }
    
}
