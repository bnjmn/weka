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
 * Copyright (C) 2002 University of Waikato 
 */

package weka.filters.unsupervised.instance;

import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests Resample. Run from the command line with:<p>
 * java weka.filters.unsupervised.instance.ResampleTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 1.3 $
 */
public class ResampleTest extends AbstractFilterTest {
  
  public ResampleTest(String name) { super(name);  }

  /** Creates a default Resample */
  public Filter getFilter() {
    Resample f = new Resample();
    f.setSampleSizePercent(50);
    return f;
  }

  public void testSampleSizePercent() {
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals("Expecting output to be 50% of input",
                 m_Instances.numInstances() / 2,  result.numInstances());

    ((Resample)m_Filter).setSampleSizePercent(200);
    result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals("Expecting output to be 200% of input",
                 m_Instances.numInstances() * 2,  result.numInstances());
  }

  public void testSampleSizePercentNoReplacement() {
    ((Resample) m_Filter).setSampleSizePercent(20);
    ((Resample) m_Filter).setNoReplacement(true);
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals("Expecting output to be 20% of input",
                 (int) (m_Instances.numInstances() * 20.0 / 100),  result.numInstances());
  }

  public void testSampleSizePercentNoReplacementInverted() {
    ((Resample) m_Filter).setSampleSizePercent(20);
    ((Resample) m_Filter).setNoReplacement(true);
    ((Resample) m_Filter).setInvertSelection(true);
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals("Expecting output to be 80% of input (20% inverted)",
                 m_Instances.numInstances() 
                 - (int) (m_Instances.numInstances() * 20.0 / 100),  result.numInstances());
  }

  public static Test suite() {
    return new TestSuite(ResampleTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
