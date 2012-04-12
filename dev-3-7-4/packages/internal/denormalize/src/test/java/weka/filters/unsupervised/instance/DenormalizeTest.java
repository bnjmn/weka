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
 * Copyright (C) 2010 University of Waikato 
 */

package weka.filters.unsupervised.instance;

import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instances;
import weka.core.TestInstances;
import weka.core.SparseInstance;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;
import weka.filters.unsupervised.instance.Denormalize;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests Denormalize. Run from the command line with:<p>
 * java weka.filters.unsupervised.instance.DenormalizeTest
 *
 * @author Mark Hall
 * @version $Revision$
 */
public class DenormalizeTest extends AbstractFilterTest {
  
  public DenormalizeTest(String name) { super(name);  }
  
  public void setUp() throws Exception {
    super.setUp();
    
    m_Instances.setClassIndex(1);
    m_FilteredClassifier = null;
  }
  
  /** Creates an example Denormalize */
  public Filter getFilter() {
    Denormalize f = new Denormalize();
    f.setGroupingAttribute("2");
    return f;
  }

  public static Test suite() {
    return new TestSuite(DenormalizeTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
