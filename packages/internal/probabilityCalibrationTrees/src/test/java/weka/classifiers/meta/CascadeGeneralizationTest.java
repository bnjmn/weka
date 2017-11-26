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

package weka.classifiers.meta;

import weka.classifiers.AbstractClassifierTest;
import weka.classifiers.Classifier;

import junit.framework.Test;
import junit.framework.TestSuite;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.rules.ZeroR;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.PCT;

/**
 * Tests CascadeGeneralization. Run from the command line with:<p/>
 * java weka.classifiers.trees.CascadeGeneralizationTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 8109 $
 */
public class CascadeGeneralizationTest 
  extends AbstractClassifierTest {

  public CascadeGeneralizationTest(String name) { 
    super(name);  
  }

  /** Creates a default CascadeGeneralization */
  public Classifier getClassifier() {
    CascadeGeneralization cs = new CascadeGeneralization();
    PCT pct = new PCT();
    pct.setUseRootMeanSquaredError(true);
    cs.setMetaClassifier(pct);
    Classifier[] cls = new Classifier[1];
    cls[0] = new J48();
    cs.setClassifiers(cls);
    cs.setKeepOriginal(true);
    cs.setUseLogOdds(true);
    return cs;
  }

  public static Test suite() {
    return new TestSuite(CascadeGeneralizationTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
