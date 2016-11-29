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
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.meta.nestedDichotomies;

import weka.classifiers.AbstractClassifierTest;
import weka.classifiers.Classifier;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests FurthestCentroidND. Run from the command line with:<p/>
 * java weka.classifiers.meta.nestedDichotomies.FurthestCentroidNDTest
 *
 * @author eibe
 * @version $Revision: 8109 $
 */
public class FurthestCentroidNDTest
  extends AbstractClassifierTest {

  public FurthestCentroidNDTest(String name) {
    super(name);  
  }

  /** Creates a default Dagging */
  public Classifier getClassifier() {
    return new FurthestCentroidND();
  }

  public static Test suite() {
    return new TestSuite(FurthestCentroidNDTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
