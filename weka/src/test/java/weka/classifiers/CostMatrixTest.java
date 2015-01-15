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
 * Copyright (C) 2015 University of Waikato 
 */

package weka.classifiers;

import java.io.StringReader;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instances;

/**
 * Some simple tests for 2 class cost matrices with and without expressions
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class CostMatrixTest extends TestCase {

  protected static final String DATA = "@relation test\n"
    + "@attribute one numeric\n"
    + "@attribute two numeric\n"
    + "@attribute three {c1,c2}\n"
    + "@data\n"
    + "-1, 5, c1\n"
    + "6, 8, c2\n";

  /**
   * Called by JUnit before each test method.
   * 
   * @throws Exception if an error occurs
   */
  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  /**
   * Called by JUnit after each test method
   * 
   * @throws Exception if an error occurs
   */
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Get a little dataset to use
   * 
   * @return an Instances object
   * @throws Exception if a problem occurs
   */
  protected Instances getData() throws Exception {
    Instances insts = new Instances(new StringReader(DATA));
    insts.setClassIndex(insts.numAttributes() - 1);

    return insts;
  }

  /**
   * Get a 2 class cost matrix with the supplied off diagonal costs set
   * 
   * @param cost1 first cost value
   * @param cost2 second cost value
   * @return
   */
  protected CostMatrix get2ClassCostMatrixNoExpressions(double cost1,
    double cost2) {
    CostMatrix matrix = new CostMatrix(2);
    matrix.setCell(0, 1, cost1);
    matrix.setCell(1, 0, cost2);

    return matrix;
  }

  /**
   * Tests creating a 3x3 cost matrix and applying it to 2 class data. An
   * execption is expected in this case
   * 
   * @throws Exception if a problem occurs
   */
  public void testIncorrectSize() throws Exception {
    CostMatrix matrix = new CostMatrix(3);
    Instances data = getData();

    try {
      matrix.applyCostMatrix(data, null);
      fail("Was expecting an exception as the cost matrix represents more classes than "
        + "are present in the data");
    } catch (Exception ex) {
      // expected
    }
  }

  /**
   * Tests a simple 2 class cost matrix with fixed costs
   * 
   * @throws Exception if a problem occurs
   */
  public void test2ClassCostMatrixNoExpressions() throws Exception {
    CostMatrix matrix = get2ClassCostMatrixNoExpressions(2, 6);
    Instances data = getData();

    Instances weighted = matrix.applyCostMatrix(data, null);

    assertEquals(0.5, weighted.instance(0).weight());
    assertEquals(1.5, weighted.instance(1).weight());
  }

  /**
   * Tests a 2 class cost matrix with one simple expression
   * 
   * @throws Exception if a problem occurs
   */
  public void test2ClassCostMatrixOneSimpleExpression() throws Exception {
    CostMatrix matrix = get2ClassCostMatrixNoExpressions(2, 6);
    matrix.setCell(0, 1, "a2");

    Instances data = getData();
    Instances weighted = matrix.applyCostMatrix(data, null);

    assertEquals(5.0, weighted.instance(0).weight());
    assertEquals(6.0, weighted.instance(1).weight());
  }

  /**
   * Tests a 2 class cost matrix with one more complex expression
   * 
   * @throws Exception if a problem occurs
   */
  public void test2ClassCostMatrixOneExpression() throws Exception {
    CostMatrix matrix = get2ClassCostMatrixNoExpressions(2, 6);
    matrix.setCell(0, 1, "log(a2^2)*a1-1");

    Instances data = getData();
    Instances weighted = matrix.applyCostMatrix(data, null);
    assertEquals(-4.218876, weighted.instance(0).weight(), 1e-6);
    assertEquals(6.0, weighted.instance(1).weight());
  }

  /**
   * Tests a 2 class cost matrix with two expressions
   * 
   * @throws Exception if a problem occurs
   */
  public void test2ClassCostMatrixTwoExpressions() throws Exception {
    CostMatrix matrix = get2ClassCostMatrixNoExpressions(2, 6);
    matrix.setCell(0, 1, "log(a2^2)*a1-1");
    matrix.setCell(1, 0, "exp(a1*cos(a2))/sqrt(a1/2)");

    Instances data = getData();
    Instances weighted = matrix.applyCostMatrix(data, null);
    assertEquals(-4.218876, weighted.instance(0).weight(), 1e-6);
    assertEquals(0.241157, weighted.instance(1).weight(), 1e-6);
  }

  public static Test suite() {
    return new TestSuite(CostMatrixTest.class);
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
