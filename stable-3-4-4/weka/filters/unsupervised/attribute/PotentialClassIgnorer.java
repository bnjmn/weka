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
 *    PotentialClassIgnorer.java
 *    Copyright (C) 2003 University of Waikato
 *
 */

package weka.filters.unsupervised.attribute;

import weka.filters.Filter;
import weka.core.Instances;

/**
 * This filter should be extended by other unsupervised attribute
 * filters to allow processing of the class attribute if that's
 * required. It the class is to be ignored it is essential that the
 * extending filter does not change the position (i.e. index) of the
 * attribute that is originally the class attribute !
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz), Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $ 
 */
public abstract class PotentialClassIgnorer extends Filter {

  /** True if the class is to be unset */
  protected boolean m_IgnoreClass = false;

  /** Storing the class index */
  protected int m_ClassIndex = -1;

  /**
   * Sets the format of the input instances. If the filter is able to
   * determine the output format before seeing any input instances, it
   * does so here. This default implementation clears the output format
   * and output queue, and the new batch flag is set. Overriders should
   * call <code>super.setInputFormat(Instances)</code>
   *
   * @param instanceInfo an Instances object containing the input instance
   * structure (any instances contained in the object are ignored - only the
   * structure is required).
   * @return true if the outputFormat may be collected immediately
   * @exception Exception if the inputFormat can't be set successfully 
   */
  public boolean setInputFormat(Instances instanceInfo) throws Exception {

    boolean result = super.setInputFormat(instanceInfo);
    if (m_IgnoreClass) {
      m_ClassIndex = inputFormatPeek().classIndex();
      inputFormatPeek().setClassIndex(-1);
    }      
    return result;
  }

  /**
   * Gets the format of the output instances. This should only be called
   * after input() or batchFinished() has returned true. The relation
   * name of the output instances should be changed to reflect the
   * action of the filter (eg: add the filter name and options).
   *
   * @return an Instances object containing the output instance
   * structure only.
   * @exception NullPointerException if no input structure has been
   * defined (or the output format hasn't been determined yet) 
   */
  public final Instances getOutputFormat() {

    if (m_IgnoreClass) {
      outputFormatPeek().setClassIndex(m_ClassIndex);
    }
    return super.getOutputFormat();
  }

  /**
   * Set the IgnoreClass value. Set this to true if the
   * class index is to be unset before the filter is applied.
   * @param newIgnoreClass The new IgnoreClass value.
   */
  public void setIgnoreClass(boolean newIgnoreClass) {

    m_IgnoreClass = newIgnoreClass;
  }
}
  
   
