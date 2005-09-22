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
 *    Kernel.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */

package weka.classifiers.functions.supportVector;

import weka.core.*;
import java.io.*;

/**
 * Abstract kernel. 
 * Kernels implementing this class must respect Mercer's condition in order 
 * to ensure a correct behaviour of SMOreg.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.1.2.2 $
 */
public  abstract class Kernel implements Serializable {

  /** The dataset */
  protected Instances m_data;
    
  /**
   * Computes the result of the kernel function for two instances.
   * If id1 == -1, eval use inst1 instead of an instance in the dataset.
   *
   * @param id1 the index of the first instance in the dataset
   * @param id2 the index of the second instance in the dataset
   * @param inst the instance corresponding to id1 (used if id1 == -1)
   * @return the result of the kernel function
   */
  public abstract double eval(int id1, int id2, Instance inst1) 
    throws Exception;

    
  /**
   * Frees the memory used by the kernel.
   * (Usefull with kernels which use cache.)
   * This function is called when the training is done.
   * i.e. after that, eval will be called with id1 == -1.
   */
  public abstract void clean();


  /**
   * Returns the number of kernel evaluation performed.
   *
   * @return the number of kernel evaluation performed.
   */
  public abstract int numEvals();

  /**
   * Returns the number of dot product cache hits.
   *
   * @return the number of dot product cache hits, or -1 if not supported by this kernel.
   */
  public abstract int numCacheHits();
    
}
