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
 * The RBF kernel.
 * K(x, y) = e^-(gamma * <x-y, x-y>^2)
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Shane Legg (shane@intelligenesis.net) (sparse vector code)
 * @author Stuart Inglis (stuart@reeltwo.com) (sparse vector code)
 * @author J. Lindgren (jtlindgr{at}cs.helsinki.fi) (RBF kernel)
 * @version $$ */
public class RBFKernel extends Kernel {
    
  /** The precalculated dotproducts of <inst_i,inst_i> */
  private double m_kernelPrecalc[];

  /** Counts the number of kernel evaluations. */
  private int m_kernelEvals = 0;

  /** The size of the cache (a prime number) */
  private int m_cacheSize;

  /** Kernel cache */
  private double[] m_storage;
  private long[] m_keys;

  /** Gamma for the RBF kernel. */
  private double m_gamma = 0.01;
  
  /** The number of instance in the dataset */
  private int m_numInsts;


  /**
   * Constructor. Initializes m_kernelPrecalc[].
   */
  public RBFKernel(Instances data, int cacheSize, double gamma) throws Exception {
        
    m_gamma = gamma;
    m_data = data;
    m_numInsts = m_data.numInstances();
    m_cacheSize = cacheSize;
    m_storage = new double[m_cacheSize];
    m_keys = new long[m_cacheSize];

    m_kernelPrecalc=new double[data.numInstances()];

    for(int i=0;i<data.numInstances();i++)
      m_kernelPrecalc[i]=dotProd(data.instance(i),data.instance(i));
  }


  /**
   * Implements the abstract function of Kernel.
   */
  public double eval(int id1, int id2, Instance inst1) 
    throws Exception {
  
    double result = 0;
    long key = -1;
    int location = -1;
      
    // we can only cache if we know the indexes
    if (id1 >= 0) {
      if (id1 > id2) {
	key = (long)id1 * m_numInsts + id2;
      } else {
	key = (long)id2 * m_numInsts + id1;
      }
      if (key < 0) {
	throw new Exception("Cache overflow detected!");
      }
      location = (int)(key % m_keys.length);
      if (m_keys[location] == (key + 1)) {
	return m_storage[location];
      }
    }
	
    Instance inst2 = m_data.instance(id2);

    double precalc1;

    if(id1 == -1)
      precalc1 = dotProd(inst1,inst1);
    else
      precalc1 = m_kernelPrecalc[id1];
	
    result = Math.exp(m_gamma * (2. * dotProd(inst1, inst2) -
				 precalc1 - m_kernelPrecalc[id2]));
  
    m_kernelEvals++;
    
    // store result in cache 	
    if (key != -1){
      m_storage[location] = result;
      m_keys[location] = (key + 1);
    }

    return result;
  }

  /**
   * Calculates a dot product between two instances
   *
   * @param inst1 the first instance
   * @param inst2 the second instance
   * @return the dot product of the two instances.
   * @exception Exception if an error occurs
   */
  private double dotProd(Instance inst1, Instance inst2) 
    throws Exception {
	
    double result=0;
	
    // we can do a fast dot product
    int n1 = inst1.numValues(); int n2 = inst2.numValues();
    int classIndex = m_data.classIndex();
    for (int p1 = 0, p2 = 0; p1 < n1 && p2 < n2;) {
      int ind1 = inst1.index(p1); 
      int ind2 = inst2.index(p2);
      if (ind1 == ind2) {
	if (ind1 != classIndex) {
	  result += inst1.valueSparse(p1) * inst2.valueSparse(p2);
	}
	p1++; p2++;
      } else if (ind1 > ind2) {
	p2++;
      } else {
	p1++;
      }
    }
    return(result);
  }


  /**
   * Frees the cache used by the kernel.
   */
  public void clean(){
    m_storage = null; 
    m_keys = null;
  }


  /**
   * Returns the number of time Eval has been called.
   *
   * @return the number of kernel evaluation.
   */
  public int numEvals(){
    return m_kernelEvals;
  }
    
}
