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
 * Copyright (C) 2006-2017 University of Waikato, Hamilton, New Zealand
 */

package weka.filters;

import junit.framework.Test;
import junit.framework.TestSuite;
import weka.core.Instances;

/**
 * Tests RenameRelation. Run from the command line with:<p>
 * java weka.filters.RenameRelationTest</p>
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class RenameRelationTest extends AbstractFilterTest {

  public RenameRelationTest(String name) {
    super(name);
  }

  @Override
  public Filter getFilter() {
    return new RenameRelation();
  }

  public void testTypical() {
    String originalRelationName = m_Instances.relationName();
    ((RenameRelation) m_Filter).setModificationText("GoofyGoober");
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());

    assertEquals(m_Instances.relationName(), originalRelationName);
    assertEquals(result.relationName(), "GoofyGoober");
  }

  public void testPrepend() {
    String originalRelationName = m_Instances.relationName();
    m_Filter = getFilter();
    ((RenameRelation) m_Filter).setModificationText("GoofyGoober");
    ((RenameRelation) m_Filter).setModType(RenameRelation.ModType.PREPEND);
    Instances result = useFilter();

    assertEquals(result.relationName(), "GoofyGoober" + originalRelationName);
  }

  public void testAppend() {
    String originalRelationName = m_Instances.relationName();
    m_Filter = getFilter();
    ((RenameRelation) m_Filter).setModificationText("GoofyGoober");
    ((RenameRelation) m_Filter).setModType(RenameRelation.ModType.APPEND);
    Instances result = useFilter();

    assertEquals(result.relationName(), originalRelationName + "GoofyGoober");
  }

  public void testRegex() {
    m_Filter = getFilter();
    ((RenameRelation) m_Filter).setModificationText("GoofyGoober");
    ((RenameRelation) m_Filter).setModType(RenameRelation.ModType.REGEX);
    ((RenameRelation) m_Filter).setRegexMatch("Test");
    Instances result = useFilter();

    assertEquals(result.relationName(), "FilterGoofyGoober");
  }

  public static Test suite() {
    return new TestSuite(RenameRelationTest.class);
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
