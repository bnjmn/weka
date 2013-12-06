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
 * Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.attribute;

import junit.framework.Test;
import junit.framework.TestSuite;
import weka.core.Attribute;
import weka.core.SelectedTag;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;

/**
 * Tests MLPAutoencoder. Run from the command line with:
 * <p/>
 * java weka.filters.unsupervised.instance.MLPAutoencoderTest
 * 
 * @author Eibe Frank
 * @version $Revision: 8108 $
 */
public class MLPAutoencoderTest extends AbstractFilterTest {

  public MLPAutoencoderTest(String name) {
    super(name);
  }

  /** Set class index and remove attributes */
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    m_Instances.setClassIndex(1);

    RemoveType rt = new RemoveType();
    rt.setAttributeType(new SelectedTag(Attribute.NUMERIC,
      RemoveType.TAGS_ATTRIBUTETYPE));
    rt.setInvertSelection(true);
    rt.setInputFormat(m_Instances);

    m_Instances = RemoveType.useFilter(m_Instances, rt);

    ReplaceMissingValues rmv = new ReplaceMissingValues();
    rmv.setInputFormat(m_Instances);

    m_Instances = RemoveType.useFilter(m_Instances, rmv);

    m_FilteredClassifier = null; // Too much hassle...
  }

  /** Creates a default MLPAutoencoder */
  @Override
  public Filter getFilter() {
    MLPAutoencoder f = new MLPAutoencoder();
    f.setWeightsFile(new java.io.File("."));
    return f;
  }

  public static Test suite() {
    return new TestSuite(MLPAutoencoderTest.class);
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
