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
 * Copyright (C) 2009 Amri Napolitano 
 */

package weka.filters.unsupervised.attribute;

import weka.core.Attribute;
import weka.core.AttributeStats;
import weka.core.Instances;
import weka.core.TestInstances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToNominal;
import weka.filters.unsupervised.attribute.NominalToBinary;
import weka.filters.unsupervised.attribute.Remove;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests EMImputation. Run from the command line with:<p>
 * java weka.filters.unsupervised.attribute.EMImputationTest
 *
 * @author Amri Napolitano
 * @version $Revision$
 */
public class EMImputationTest extends AbstractFilterTest {
  
  public EMImputationTest(String name) { super(name);  }

  /** Creates a default EMImputation */
  public Filter getFilter() {
    return new EMImputation();
  }

  protected void setUp() throws Exception {
    super.setUp();

    Instances temp = new Instances(m_Instances);
    for (int j = 0; j < 2; j++) {
      for (int i = 0; i < temp.numInstances(); i++) {
        m_Instances.add(temp.instance(i));
      }
    }

    // now just filter the instances to convert String attributes
    // and binarize nominal attributes
    StringToNominal stn = new StringToNominal();
    stn.setAttributeRange("first-last");
    stn.setInputFormat(m_Instances);
    m_Instances = Filter.useFilter(m_Instances, stn);
    NominalToBinary ntb = new NominalToBinary();
    ntb.setInputFormat(m_Instances);
    m_Instances = Filter.useFilter(m_Instances, ntb);

    // remove the last column (date attribute)
    Remove r = new Remove();
    r.setAttributeIndices("last");
    r.setInputFormat(m_Instances);
    m_Instances = Filter.useFilter(m_Instances, r);
  }

  protected Instances getFilteredClassifierData() throws Exception {
    TestInstances test;
    Instances result;
    
    test = TestInstances.forCapabilities(getFilter().getCapabilities());
    test.setClassIndex(TestInstances.CLASS_IS_LAST);
    test.setNumNumeric(3);

    result = test.generate();

    return result;
  }

  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    // Number of instances may change (if an instance has all missing values)
    // assertEquals(m_Instances.numInstances(), result.numInstances());
    for (int j = 0; j < result.numAttributes(); j++) {
      if (j == m_Instances.classIndex() && m_Instances.attribute(j).isNumeric() == false) {
        continue;
      }
      AttributeStats currentStats = m_Instances.attributeStats(j);
      if (currentStats.distinctCount < 2) {
        continue;
      }
      assertTrue("All missing values except for those in nonnumeric class " +
                  "attributes should be replaced.", 
                  result.attributeStats(j).missingCount == 0);
    }
  }

  public static Test suite() {
    return new TestSuite(EMImputationTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
