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

import weka.classifiers.functions.supportVector.CachedKernel;
import weka.core.Instance;
import weka.core.Instances;

/**
 * The RBF kernel. K(x, y) = e^-(gamma * &lt;x-y, x-y&gt;^2) <p/>
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Shane Legg (shane@intelligenesis.net) (sparse vector code)
 * @author Stuart Inglis (stuart@reeltwo.com) (sparse vector code)
 * @author J. Lindgren (jtlindgr{at}cs.helsinki.fi) (RBF kernel)
 * @author Lin Dong (ld21@cs.waikato.ac.nz) (MIkernel)
 * @version $Revision: 1.1 $ 
 */

public class MIRBFKernel 
  extends CachedKernel {

  /** The precalculated dotproducts of <inst_i,inst_i> */
  private double m_kernelPrecalc[][];

  /** Gamma for the RBF kernel. */
  private double m_gamma = 0.01;

  /**
   * Constructor. 
   */
  public MIRBFKernel(Instances data, int cacheSize, double gamma)
    throws Exception {

    super(data, cacheSize);
    m_gamma = gamma;
    m_kernelPrecalc = new double[data.numInstances()][];

    for (int i = 0; i < data.numInstances(); i++){
      Instances insts=new Instances(data.instance(i).relationalValue(1));
      m_kernelPrecalc[i] = new double [insts.numInstances()];
      for (int j=0; j<insts.numInstances(); j++)
        m_kernelPrecalc[i][j]=dotProd(insts.instance(j), insts.instance(j));
    }
  }

  protected double evaluate(int id1, int id2, Instance inst1)
    throws Exception {

    double result =0 ;
    Instances insts1, insts2;
    if (id1==-1)
      insts1 = new Instances(inst1.relationalValue(1));
    else
      insts1 = new Instances(m_data.instance(id1).relationalValue(1));
    insts2 = new Instances (m_data.instance(id2).relationalValue(1));

    double precalc1=0;
    for(int i=0; i<insts1.numInstances();i++){
      for (int j=0; j<insts2.numInstances(); j++){
        if (id1==-1)
          precalc1 = dotProd(insts1.instance(i), insts1.instance(i));
        else 
          precalc1 =  m_kernelPrecalc[id1][i];

        double res = Math.exp(m_gamma*(2. * dotProd(insts1.instance(i), insts2.instance(j)) -precalc1 -  m_kernelPrecalc[id2][j] ) );

        result += res;
      }
    }

    return result;
  }   
}
























