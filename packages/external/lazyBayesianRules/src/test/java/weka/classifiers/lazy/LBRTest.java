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

package weka.classifiers.lazy;

import weka.classifiers.AbstractClassifierTest;
import weka.classifiers.Classifier;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests LBR. Run from the command line with:<p/>
 * java weka.classifiers.lazy.LBRTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class LBRTest 
  extends AbstractClassifierTest {

  public LBRTest(String name) { 
    super(name);  
  }

  /** Creates a default LBR */
  public Classifier getClassifier() {
    return new LBR();
  }

  public static Test suite() {
    return new TestSuite(LBRTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
