/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    PMMLClassifierScoringBeanInfo.java
 *    Copyright (C) 2008 Pentaho Corporation
 *
 */

package weka.gui.beans;

import java.beans.*;

/**
 * BeanInfo class for the PMMLClassifierScoring component.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com
 * @version $Revision 1.0 $
 */
public class PMMLClassifierScoringBeanInfo extends SimpleBeanInfo {

  public EventSetDescriptor [] getEventSetDescriptors() {
    try {
      EventSetDescriptor [] esds = {
          new EventSetDescriptor(PMMLClassifierScoring.class,
              "incrementalClassifier",
              IncrementalClassifierListener.class,
              "acceptClassifier"),
              new EventSetDescriptor(PMMLClassifierScoring.class,
                  "batchClassifier",
                  BatchClassifierListener.class,
                  "acceptClassifier")
      };
      return esds;
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }

  /**                                                                                           
   * Get the bean descriptor for this bean                                                      
   *                                                                                            
   * @return a <code>BeanDescriptor</code> value                                                
   */
  public BeanDescriptor getBeanDescriptor() {
    return new BeanDescriptor(weka.gui.beans.PMMLClassifierScoring.class,
                              PMMLClassifierScoringCustomizer.class);
  }
}
