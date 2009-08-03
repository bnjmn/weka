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
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 */

package weka.attributeSelection;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Tests ClassifierAttributeEval. Run from the command line with:<p/>
 * java weka.attributeSelection.ClassifierAttributeEvalTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class ClassifierAttributeEvalTest 
  extends AbstractEvaluatorTest {

  /**
   * Initializes the test.
   * 
   * @param name	the name of the test
   */
  public ClassifierAttributeEvalTest(String name) { 
    super(name);  
  }

  /**
   * Creates a default Ranker.
   * 
   * @return		the search algorithm
   */
  public ASSearch getSearch() {
    return new Ranker();
  }

  /** 
   * Creates a default ClassifierAttributeEval.
   * 
   * @return		the evaluator
   */
  public ASEvaluation getEvaluator() {
    return new ClassifierAttributeEval();
  }

  /**
   * Returns a test suite.
   * 
   * @return		the suite
   */
  public static Test suite() {
    return new TestSuite(ClassifierAttributeEvalTest.class);
  }

  /**
   * Runs the test from commandline.
   * 
   * @param args	ignored
   */
  public static void main(String[] args){
    TestRunner.run(suite());
  }
}
