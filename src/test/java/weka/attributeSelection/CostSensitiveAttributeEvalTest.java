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
 * Copyright (C) 2008 Pentaho Corporation
 */

package weka.attributeSelection;

import weka.classifiers.CostMatrix;

import java.io.InputStreamReader;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests CostSensitiveAttributeEval. Run from the command line with:<p/>
 * java weka.attributeSelection.CostSensitiveAttributeEvalTest
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com
 * @version $Revision: 1.1 $
 */
public class CostSensitiveAttributeEvalTest 
  extends AbstractEvaluatorTest {

  public CostSensitiveAttributeEvalTest(String name) { 
    super(name);  
  }

  /**
   * Called by JUnit before each test method. This implementation creates
   * the default evaluator to test and loads a test set of Instances.
   *
   * @exception Exception if an error occurs reading the example instances.
   */
  protected void setUp() throws Exception {
    super.setUp();

    // can handle only as much classes as there are in the CostMatrix!
    m_NClasses = ((CostSensitiveAttributeEval) getEvaluator()).getCostMatrix().numRows();
  }

  /** Creates a default Ranker */
  public ASSearch getSearch() {
    return new Ranker();
  }

  /** Creates a default CostSensitiveAttributeEval */
  public ASEvaluation getEvaluator() {
    CostSensitiveAttributeEval csse = new CostSensitiveAttributeEval();

    // load cost matrix
    try {
      csse.setCostMatrix(
         new CostMatrix(
            new InputStreamReader(ClassLoader.getSystemResourceAsStream(
                  "weka/classifiers/data/ClassifierTest.cost"))));
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    return csse;
  }

  public static Test suite() {
    return new TestSuite(CostSensitiveAttributeEvalTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
