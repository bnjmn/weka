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
 * Copyright (C) 2002 University of Waikato 
 */

package weka.filters.unsupervised.attribute;

import junit.framework.Test;
import junit.framework.TestSuite;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

/**
 * Tests RenameNominalValues. Run from the command line with:
 * <p>
 * java weka.filters.unsupervised.attribute.RemoveTest
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class RenameNominalValuesTest extends AbstractFilterTest {

  public RenameNominalValuesTest(String name) {
    super(name);
  }

  /** Creates a default RenameNominalValues */
  @Override
  public Filter getFilter() {
    return getFilter("2,5", "b:bob");
  }

  /** Creates a specialized Remove */
  public Filter getFilter(String rangelist, String renameSpec) {

    RenameNominalValues af = new RenameNominalValues();

    if (rangelist.length() > 0) {
      af.setSelectedAttributes(rangelist);
    }

    if (renameSpec.length() > 0) {
      af.setValueReplacements(renameSpec);
    }

    return af;
  }

  public void testNoSelectedAttsNoReplaceSpec() {
    m_Filter = getFilter();
    ((RenameNominalValues) m_Filter).setSelectedAttributes("");
    ((RenameNominalValues) m_Filter).setValueReplacements("");

    Instances result = useFilter();
    assertEquals(m_Instances.numInstances(), result.numInstances());
    assertEquals(m_Instances.numAttributes(), result.numAttributes());

    // all instances should be unchanged
    for (int i = 0; i < result.numInstances(); i++) {
      Instance orig = m_Instances.instance(i);
      Instance filtered = result.instance(i);

      for (int j = 0; j < orig.numAttributes(); j++) {
        assertEquals(orig.value(j), filtered.value(j));
      }
    }
  }

  public void testTypical() {
    m_Filter = getFilter();
    Instances result = useFilter();

    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());

    // shouldn't be any 'b' values in the header - they should now
    // be 'bob'
    Attribute first = result.attribute(1);
    Attribute second = result.attribute(4);

    assertEquals(first.value(2), "bob");
    assertEquals(second.value(1), "bob");

    // check an instance
    Instance inst = result.instance(1);
    assertEquals(inst.stringValue(1), "bob");
    assertEquals(inst.stringValue(4), "bob");
  }

  public void testTypical2() {
    m_Filter = getFilter("2", "b:bob");
    Instances result = useFilter();

    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());

    // shouldn't be any 'b' values in the header for attribute 2
    // - they should now be 'bob'
    Attribute first = result.attribute(1);
    Attribute second = result.attribute(4);

    assertEquals(first.value(2), "bob");

    // check that the other nominal attribute is unchanged
    assertEquals(second.value(1), "b");

    // check an instance
    Instance inst = result.instance(1);
    assertEquals(inst.stringValue(1), "bob");
    assertEquals(inst.stringValue(4), "b");
  }

  public void testInverted1() {
    m_Filter = getFilter("", "b:bob");
    ((RenameNominalValues) m_Filter).setInvertSelection(true);

    Instances result = useFilter();

    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());

    // shouldn't be any 'b' values in the header - they should now
    // be 'bob'
    Attribute first = result.attribute(1);
    Attribute second = result.attribute(4);

    assertEquals(first.value(2), "bob");
    assertEquals(second.value(1), "bob");

    // check an instance
    Instance inst = result.instance(1);
    assertEquals(inst.stringValue(1), "bob");
    assertEquals(inst.stringValue(4), "bob");
  }

  /**
   * tests the filter in conjunction with the FilteredClassifier
   */
  @Override
  public void testFilteredClassifier() {
    try {
      Instances data = getFilteredClassifierData();

      for (int i = 0; i < data.numAttributes(); i++) {
        if (data.classIndex() == i)
          continue;
        if (data.attribute(i).isNominal()) {
          ((RenameNominalValues) m_FilteredClassifier.getFilter())
              .setSelectedAttributes("" + (i + 1));
          break;
        }
      }
    } catch (Exception e) {
      fail("Problem setting up test for FilteredClassifier: " + e.toString());
    }

    super.testFilteredClassifier();
  }

  public static Test suite() {
    return new TestSuite(RenameNominalValuesTest.class);
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }

}
