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
 * Copyright (C) 2008-2014 University of Waikato 
 */

package weka.filters.unsupervised.instance;

import java.io.ByteArrayInputStream;
import java.util.HashMap;

import java_cup.runtime.DefaultSymbolFactory;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import weka.core.Attribute;
import weka.core.Instances;
import weka.filters.AbstractFilterTest;
import weka.filters.Filter;
import weka.filters.unsupervised.instance.subsetbyexpression.Parser;
import weka.filters.unsupervised.instance.subsetbyexpression.Scanner;

// TODO: documentation for tests
/**
 * Tests SubsetByExpression. Run from the command line with: <p/>
 * java weka.filters.unsupervised.instance.SubsetByExpressionTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class SubsetByExpressionTest
  extends AbstractFilterTest {
  
  /**
   * Setup the test.
   * 
   * @param name	the name of the test
   */
  public SubsetByExpressionTest(String name) {
    super(name);
  }

  /**
   * Called by JUnit before each test method. 
   * <p/>
   * Removes all the string attributes and sets the first attribute as class 
   * attribute.
   *
   * @throws Exception 	if an error occurs reading the example instances.
   */
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    
    m_Instances.deleteAttributeType(Attribute.STRING);
    m_Instances.setClassIndex(0);
  }

  /**
   * Creates a default SubsetByExpression filter.
   * 
   * @return		the filter
   */
  @Override
  public Filter getFilter() {
    return new SubsetByExpression();
  }

  /**
   * Creates a SubsetByExpression filter with the given expression.
   * 
   * @param expr	the expression to use
   * @return		the filter
   */
  public Filter getFilter(String expr) {
    SubsetByExpression result = new SubsetByExpression();
    result.setExpression(expr);
    return result;
  }

  public void testBasic() throws Exception {
    // basic tests, taken from MathematicalExpressionTest
    assertTrue(evaluate("4.0 = ATT1-ATT2+ATT3", 4.0, 2.0, 2.0));
    assertTrue(evaluate("0.0 = ATT1-ATT2*ATT3", 4.0, 2.0, 2.0));
    assertTrue(evaluate("4.0 = (ATT1-ATT2)*ATT3", 4.0, 2.0, 2.0));
    assertTrue(evaluate("-10.0 = ATT1-ATT2*(ATT3+5)", 4.0, 2.0, 2.0));
    assertTrue(evaluate("12.0 = pow(ATT1,ATT2*1)-ATT3*2", 4.0, 2.0, 2.0));
  }
  
  public void testParsing() throws Exception {
    
    // parser respects brackets:
    assertTrue(evaluate("4.0 = 8.0/(4.0/2.0)"));
    assertTrue(evaluate("6.0 = 8.0 - (4.0 - 2.0)"));
    
    // parser can handle brackets
    evaluate("((((((((((((true))))))))))))");
    evaluate("((((((((((((true)))))))))))) and ((((((((((((true))))))))))))");
    evaluate("((((((((((((true) and true) and true) and true) and true) and true) and true) and true) and true) and true) and true) and true) and true");

    evaluate("not not not not not not true");

    // float parsing
    try {
      assertTrue(evaluate("1 = 1"));
    } catch (Exception e) {
      fail("Parsing failure!");
    }
    try {
      assertTrue(evaluate("1 = 1.0"));
    } catch (Exception e) {
      fail("Parsing failure!");
    }
    try {
      assertTrue(evaluate("1 = 1.0000000000000000000000000000000"));
    } catch (Exception e) {
      fail("Parsing failure!");
    }
    try {
      assertTrue(evaluate("0 = 0"));
    } catch (Exception e) {
      fail("Parsing failure!");
    }
    try {
      assertTrue(evaluate("0 = 0.0"));
    } catch (Exception e) {
      fail("Parsing failure!");
    }
    try {
      assertTrue(evaluate("0 = 0.00000000000000000000000000000000"));
    } catch (Exception e) {
      fail("Parsing failure!");
    }
    try {
      assertTrue(evaluate("-1 = -1"));
    } catch (Exception e) {
      fail("Parsing failure!");
    }
    try {
      assertTrue(evaluate("-1 = -1.0"));
    } catch (Exception e) {
      fail("Parsing failure!");
    }
    try {
      assertTrue(evaluate("-1 = -1.00000000000000000000000000000000"));
    } catch (Exception e) {
      fail("Parsing failure!");
    }
    try {
      // parser error
      evaluate("1.0 & 5");
      fail("Failed to find syntax error!");
    } catch (Exception e) {
      // ok
    }
    try {
      // scanner error
      evaluate("1.000a0");
      fail("Failed to find syntax error!");
    } catch (Exception e) {
      // ok
    }
  }
  
  public void testParsingBug1() throws Exception {
    evaluate("1 = --------1");
    evaluate("1 = 1--------1");
    evaluate("1 = ++++++++1");
    evaluate("1 = 1++++++++1");
    evaluate("1 = -+-+-+-+1");
    evaluate("1 = 1-+-+-+-+1");
    
  }
  
  public void testParsingBug2() throws Exception {
    // bug in subsetbyexpression
    evaluate("(4.0-2.0) = 0.0");
  }
  
  public void testVariables() throws Exception {
    
    double[] values = {0.0, 1.0, 2.0, -1.0, -0.0};
    
    // retains value
    for (double value : values) {
      assertTrue(evaluate(String.format("ATT1 = %.20f", value), value));
    }
    
    // can deal with several variables
    for (double first : values) {
      for (double second : values) {
        for (double third : values) {
          assertTrue(
              evaluate(String.format("ATT1 + ATT2 + ATT3 = %.20f", first + second + third)
                  , first, second, third)
              );
          assertTrue(
              evaluate(String.format("ATT1 * ATT2 * ATT3 = %.20f", first*second*third)
                  , first, second, third)
              );
        }
      }
    }
    
  }
 
  public void testMathFunctions() throws Exception {
    
    for (double v : new double[]{1.0, 2.0, 100.0, -1.0, -2.0, -100.0, 0.0}) {
      assertTrue(evaluate(String.format("abs(ATT1) = %.20f", Math.abs(v)), v));
      if (v > 0)
        assertTrue(evaluate(String.format("log(ATT1) = %.20f", Math.log(v)), v));
      if (!Double.isInfinite(Math.exp(v)) && -100 != v)
        assertTrue(evaluate(String.format("exp(ATT1) = %.20f", Math.exp(v)), v));
      assertTrue(evaluate(String.format("sin(ATT1) = %.20f", Math.sin(v)), v));
      assertTrue(evaluate(String.format("cos(ATT1) = %.20f", Math.cos(v)), v));
      assertTrue(evaluate(String.format("tan(ATT1) = %.20f", Math.tan(v)), v));
      if (v > 0)
        assertTrue(evaluate(String.format("sqrt(ATT1) = %.20f", Math.sqrt(v)), v));
      assertTrue(evaluate(String.format("floor(ATT1) = %.20f", Math.floor(v)), v));
      assertTrue(evaluate(String.format("ceil(ATT1) = %.20f", Math.ceil(v)), v));
      assertTrue(evaluate(String.format("rint(ATT1) = %.20f", Math.rint(v)), v));
      if (!Double.isInfinite(Math.pow(v, v)) && -100 != v)
        assertTrue(evaluate(String.format("pow(ATT1, ATT1) = %.20f", Math.pow(v, v)), v));
    }
  }
 
  public void testPlusOperator() throws Exception {
    
    double[] values = {0.0,  1.0,  2.0,  3.0,  100.0,  1000.0,  1e8,  1e-8,  0.1,
                       -0.0, -1.0, -2.0, -3.0, -100.0, -1000.0, -1e8, -1e-8, -0.1};
    
    for (double left : values) {
      
      // neutral element:
      assertTrue(evaluate(String.format("%.20f + 0.0 = %.20f", left, left)));
      assertTrue(evaluate(String.format("0.0 + %.20f = %.20f", left, left)));

      for (double right: values) {
        // basics
        assertTrue(evaluate(String.format("%.20f + %.20f = %.20f", left, right, left + right)));
        
        // commutativity
        assertTrue(evaluate(String.format("%.20f + %.20f = %.20f + %.20f", left, right, right, left)));
      }
    }

    double[] safe_values = {0.0, 1.0, 2.0, -0.0, -1.0, -2.0};
    for (double first : safe_values) {
      for (double second : safe_values) {
        for (double third : safe_values) {
          // due to commutativity the operator is both left and right associative
          // so we test both

          // left associativity
          assertTrue(
              evaluate(String.format(
                  "%.20f + %.20f + %.20f = (%.20f + %.20f) + %.20f",
                  first, second, third, first, second, third))
              );

          // right associativity
          assertTrue(
              evaluate(String.format(
                  "%.20f + %.20f + %.20f = %.20f + (%.20f + %.20f)",
                  first, second, third, first, second, third))
              );
        }
      }
    }

    // basic double semantics
    // NaN + v == NaN
    assertFalse(evaluate("(sqrt(-1) + 1.0) <= 1/0"));
    // NaN + NaN == NaN
    assertFalse(evaluate("(sqrt(-1) + sqrt(-1.0)) <= 1/0"));
    // overflow -> +inf
    assertTrue(evaluate(String.format(
        "(%.20f + %.20f) = 1/0", Double.MAX_VALUE, Double.MAX_VALUE)));
    // inf + (-inf) == NaN
    assertFalse(evaluate("(1/0 + -1/0) <= 1/0"));
    // inf + inf == inf 
    assertTrue(evaluate("(1/0 + 1/0) = 1/0"));
    
  }
  
  public void testMinusOperator() throws Exception {
    
    double[] values = {0.0,  1.0,  2.0,  3.0,  100.0,  1000.0,  1e8,  1e-8,  0.1,
                      -0.0, -1.0, -2.0, -3.0, -100.0, -1000.0, -1e8, -1e-8, -0.1};
    
    for (double left : values) {
      
      // neutral element:
      assertTrue(evaluate(String.format("%.20f - 0.0 = %.20f", left, left)));

      for (double right: values) {
        // basics
        assertTrue(evaluate(String.format("%.20f - %.20f = %.20f", left, right, left - right)));
      }
    }

    double[] safe_values = {0.0, 1.0, 2.0, -0.0, -1.0, -2.0};
    for (double first : safe_values) {
      for (double second : safe_values) {
        for (double third : safe_values) {
          // left associativity
          assertTrue(
              evaluate(String.format(
                  "%.20f - %.20f - %.20f = (%.20f - %.20f) - %.20f",
                  first, second, third, first, second, third))
              );
        }
      }
    }

    // basic double semantics
    // NaN - v == NaN
    assertFalse(evaluate("(sqrt(-1) - 1.0) <= 1/0"));
    // NaN - NaN == NaN
    assertFalse(evaluate("(sqrt(-1) - sqrt(-1.0)) <= 1/0"));
    // underflow -> -inf
    assertTrue(evaluate(String.format(
        "(%.20f - %.20f) = -1/0", -Double.MAX_VALUE, Double.MAX_VALUE)));
    // inf - inf == NaN
    assertFalse(evaluate("(1/0 - 1/0) <= 1/0"));
    
  }
 
  public void testTimesOperator() throws Exception {
    
    double[] values = {0.0,  1.0,  2.0,  3.0,  100.0,  1000.0,  1e8,  1e-8,  0.1,
                       -0.0, -1.0, -2.0, -3.0, -100.0, -1000.0, -1e8, -1e-8, -0.1};
    
    for (double left : values) {
      
      // neutral element:
      assertTrue(evaluate(String.format("(%.20f * 1.0 = %.20f)", left, left)));
      assertTrue(evaluate(String.format("(1.0 * %.20f) = %.20f", left, left)));

      for (double right: values) {
        // basics
        assertTrue(evaluate(String.format("%.20f * %.20f = %.50f",
            left, right, left*right)));
        
        // commutativity
        assertTrue(evaluate(String.format("%.20f * %.20f = %.20f * %.20f",
            left, right, right, left)));
      }
    }

    double[] safe_values = {0.0, 1.0, 2.0, -0.0, -1.0, -2.0};
    for (double first : safe_values) {
      for (double second : safe_values) {
        for (double third : safe_values) {
          // due to commutativity the operator is both left and right associative
          // so we test both

          // left associativity
          assertTrue(
              evaluate(String.format(
                  "%.20f * %.20f * %.20f = (%.20f * %.20f) * %.20f",
                  first, second, third, first, second, third))
              );

          // right associativity
          assertTrue(
              evaluate(String.format(
                  "%.20f * %.20f * %.20f = %20f * (%.20f * %.20f)",
                  first, second, third, first, second, third))
              );
        }
      }
    }

    // basic double semantics
    // NaN * v == NaN
    assertFalse(evaluate("(sqrt(-1) * 1.0) <= 1/0"));
    // NaN * NaN == NaN
    assertFalse(evaluate("(sqrt(-1) * sqrt(-1.0)) <= 1/0"));
    // overflow -> +inf
    assertTrue(evaluate(String.format(
        "(%.20f * %.20f) = 1/0", Double.MAX_VALUE, Double.MAX_VALUE)));
    // inf + (-inf) == -inf 
    assertTrue(evaluate("((1/0) * (-1/0)) = -1/0"));
    // inf * inf == inf 
    assertTrue(evaluate("(1/0) * (1/0) = 1/0"));
    
  }
  
  public void testDivisionOperator() throws Exception {
    
    double[] values = {0.0,  1.0,  2.0,  3.0,  100.0,  1000.0,  1e8,  1e-8,  0.1,
                       -0.0, -1.0, -2.0, -3.0, -100.0, -1000.0, -1e8, -1e-8, -0.1};
    
    for (double left : values) {
      
      // neutral element:
      assertTrue(evaluate(String.format("%.20f / 1.0 = %.20f", left, left)));

      for (double right: values) {
        // basics
        if (left == 0.0 && right == 0.0)
          continue; // NaN == NaN always evaluates to false
        if (right == 0.0)
          continue; // parser can't deal with inf
        assertTrue(evaluate(String.format("%.20f / %.20f = %.50f",
            left, right, left/right)));
      }
    }

    double[] safe_values = {0.0, 1.0, 2.0, -0.0, -1.0, -2.0};
    for (double first : safe_values) {
      for (double second : safe_values) {
        for (double third : safe_values) {
          if (first == 0.0 && (second == 0.0 || third == 0.0))
            continue; // NaN == NaN always evaluates to false
          // left associativity
          assertTrue(
              evaluate(String.format(
                  "%.20f / %.20f / %.20f = (%.20f / %.20f) / %.20f",
                  first, second, third, first, second, third))
              );
        }
      }
    }

    // basic double semantics
    // NaN / v == NaN
    assertFalse(evaluate("sqrt(-1) / 1.0 <= 1/0"));
    // NaN / NaN == NaN
    assertFalse(evaluate("sqrt(-1) / sqrt(-1.0) <= 1/0"));
    // overflow -> +inf
    assertTrue(evaluate(String.format(
        "%.20f + %.20f = 1/0", Double.MAX_VALUE, Double.MAX_VALUE)));
    // inf / inf == NaN 
    assertFalse(evaluate("(1/0) / (1/0) <= 1/0"));
    
  }
  
  public void testAndOperator() throws Exception {
    
    boolean[] values = {true, false};
    for (boolean left : values) {
      for (boolean right : values) {
        // basics
        assertEquals(
            left && right,
            evaluate(left + " and " + right)
            );
        
        // commutativity
        assertEquals(
            evaluate(right + " and " + left),
            evaluate(left + " and " + right)
            );
        
        for (boolean third : values) {
          // due to commutativity the operator is both left and right associative
          // so we test both
          
          // left associativity
          assertEquals(
              left && right && third,
              evaluate("(" + left + " and " + right + ") and " + third)
              );

          // right associativity
          assertEquals(
              left && right && third,
              evaluate(left + " and (" + right + " and " + third + ")")
              );

        }
      }
    }
  }
   
  public void testOrOperator() throws Exception {
    
    boolean[] values = {true, false};
    for (boolean left : values) {
      for (boolean right : values) {
        // basics
        assertEquals(
            left || right,
            evaluate(left + " or " + right)
            );
        
        // commutativity
        assertEquals(
            evaluate(right + " or " + left),
            evaluate(left + " or " + right)
            );
        
        for (boolean third : values) {
          // due to commutativity the operator is both left and right associative
          // so we test both
          
          // left associativity
          assertEquals(
              left || right || third,
              evaluate("(" + left + " or " + right + ") or " + third)
              );

          // right associativity
          assertEquals(
              left || right || third,
              evaluate(left + " or (" + right + " or " + third + ")")
              );

        }
      }
    }
  }
  
  public void testEqualsOperator() throws Exception {
    
    double[] values = {0.0,  1.0,  2.0,  3.0,  100.0,  1000.0,  1e8,  1e-8,  0.1,
                       -0.0, -1.0, -2.0, -3.0, -100.0, -1000.0, -1e8, -1e-8, -0.1};
    

    for (double left : values) {
      
      // reflexivity
      assertTrue(evaluate(String.format("%.20f = %.20f", left, left)));
      
      for (double right: values) {
        // basics
        assertEquals(
            left == right,
            evaluate(String.format(
                "%.20f = %.20f", left, right))
            );
        
        // commutativity
        assertEquals(
            evaluate(String.format(
                "%.20f = %.20f", left, right)),
            evaluate(String.format(
                "%.20f = %.20f", right, left))
            );
      }
    }

    // non-associativity
    try {
      evaluate("1.0 == 1.0 == 1.0");
      fail("non-associativity!");
    } catch (Exception e) {
      // ok
    }

    // basic double semantics
    // inf = inf -> true
    assertTrue(evaluate("1/0 = 1/0"));
    // -inf = inf -> false
    assertFalse(evaluate("-1/0 = 1/0"));
    // NaN = NaN -> false
    assertFalse(evaluate("sqrt(-1) = sqrt(-1)"));
    // 0 = NaN -> false
    assertFalse(evaluate("0.0 = sqrt(-1)"));
    // inf = NaN -> false
    assertFalse(evaluate("1/0 = sqrt(-1)"));
    
  }

  public void testLtLeGtGeOperator() throws Exception {
  
    double[] values = {0.0,  1.0,  2.0,  3.0,  100.0,  1000.0,  1e8,  1e-8,  0.1,
                       -0.0, -1.0, -2.0, -3.0, -100.0, -1000.0, -1e8, -1e-8, -0.1};
    
    for (double left : values) {
      
      assertFalse(evaluate(String.format("%.20f < %.20f", left, left)));
      assertTrue(evaluate(String.format("%.20f <= %.20f", left, left)));
      assertFalse(evaluate(String.format("%.20f > %.20f", left, left)));
      assertTrue(evaluate(String.format("%.20f >= %.20f", left, left)));
      
      for (double right: values) {
        // basics
        assertEquals(
            left < right,
            evaluate(String.format("%.20f < %.20f", left, right))
            );
        assertEquals(
            left <= right,
            evaluate(String.format("%.20f <= %.20f", left, right))
            );
        assertEquals(
            left > right,
            evaluate(String.format("%.20f > %.20f", left, right))
            );
        assertEquals(
            left >= right,
            evaluate(String.format("%.20f >= %.20f", left, right))
            );
      }
    }

    // non-associativity
    try {
      evaluate("1.0 < 1.0 < 1.0");
      fail("non-associativity!");
    } catch (Exception e) {
      // ok
    }
    try {
      evaluate("1.0 <= 1.0 <= 1.0");
      fail("non-associativity!");
    } catch (Exception e) {
      // ok
    }
    try {
      evaluate("1.0 > 1.0 > 1.0");
      fail("non-associativity!");
    } catch (Exception e) {
      // ok
    }
    try {
      evaluate("1.0 >= 1.0 >= 1.0");
      fail("non-associativity!");
    } catch (Exception e) {
      // ok
    }

    // basic double semantics
    // inf < inf -> false
    assertFalse(evaluate("1/0 < 1/0"));
    // inf <= inf -> true
    assertTrue(evaluate("1/0 <= 1/0"));
    // inf > inf -> false
    assertFalse(evaluate("1/0 > 1/0"));
    // inf >= inf -> true
    assertTrue(evaluate("1/0 >= 1/0"));

    // -inf < inf -> true
    assertTrue(evaluate("-1/0 < 1/0"));
    // -inf <= inf -> true
    assertTrue(evaluate("-1/0 <= 1/0"));
    // -inf > inf -> false
    assertFalse(evaluate("-1/0 > 1/0"));
    // -inf >= inf -> false
    assertFalse(evaluate("-1/0 >= 1/0"));

    // NaN < NaN -> false
    assertFalse(evaluate("sqrt(-1) < sqrt(-1)"));
    // NaN <= NaN -> false
    assertFalse(evaluate("sqrt(-1) <= sqrt(-1)"));
    // NaN > NaN -> false
    assertFalse(evaluate("sqrt(-1) > sqrt(-1)"));
    // NaN >= NaN -> false
    assertFalse(evaluate("sqrt(-1) >= sqrt(-1)"));

    // 0 < NaN -> false
    assertFalse(evaluate("0.0 < sqrt(-1)"));
    // 0 <= NaN -> false
    assertFalse(evaluate("0.0 <= sqrt(-1)"));
    // 0 > NaN -> false
    assertFalse(evaluate("0.0 > sqrt(-1)"));
    // 0 >= NaN -> false
    assertFalse(evaluate("0.0 >= sqrt(-1)"));

    // inf < NaN -> false
    assertFalse(evaluate("1/0 < sqrt(-1)"));
    // inf <= NaN -> false
    assertFalse(evaluate("1/0 <= sqrt(-1)"));
    // inf > NaN -> false
    assertFalse(evaluate("1/0 > sqrt(-1)"));
    // inf >= NaN -> false
    assertFalse(evaluate("1/0 >= sqrt(-1)"));
    
  }
  
  public void testPrecedence() throws Exception {
    
    // && has higher precedence than ||
    assertTrue(evaluate("false and false or true"));
    assertTrue(evaluate("false and true or true"));
    // ! has higher precedence than &&
    assertFalse(evaluate("not true and false"));
    assertFalse(evaluate("not false and false"));
    // ! has higher precedence than ||
    assertTrue(evaluate("not true or true"));
    assertTrue(evaluate("not false or true"));
    
    // *, / have higher precedence than +, -
    assertTrue(evaluate("10.0 = 2.0*3.0 + 4.0"));
    assertTrue(evaluate("2.0 = 2.0*3.0 - 4.0"));
    assertTrue(evaluate("-0.5 = 2.0/4.0 - 1.0"));
    assertTrue(evaluate("1.5 = 2.0/4.0 + 1.0"));
    // unary - has higher precedence than +, -
    assertTrue(evaluate("1.0 = -1.0 + 2.0"));
    assertTrue(evaluate("-3.0 = -1.0 - 2.0"));
    // due to the distributive law we don't have to test for unary - over *, /
    
    // +, -, /, * have higher precedence than =, <=, <, >, >=
    // if not, then the program will fail due to a type error
    assertTrue(evaluate("1.0 + 2.0 >= 2.5"));
    assertFalse(evaluate("-2.0 - 2.0 > -2.5"));
    assertFalse(evaluate("2.0 * 2.0 <= 2.5"));
    assertFalse(evaluate("1.0 / 0.5 < 1.0"));
    assertTrue(evaluate("1.0 + 2.0 = 3.0"));

    // =, <=, <, >, >= have higher precedence than &&, ||
    // if not, then the program will fail due to a type error
    assertTrue(evaluate("1.0 < 2.0 and true"));
    assertTrue(evaluate("1.0 <= 2.0 or false"));
    assertFalse(evaluate("1.0 > 2.0 and true"));
    assertFalse(evaluate("1.0 >= 2.0 or false"));
    assertFalse(evaluate("1.0 = 2.0 and true"));

  }

  public void testCompositions() throws Exception {

    String[] binaryDoubleOperators = {"+", "-", "*", "/"};
    String[] comparisonOperators = {"=", "<", ">="};
    String[] booleanBinaryOperators = {"and", "or"};
    
    for (String booleanBinaryOperator : booleanBinaryOperators) {
      for (String comparisonOperator : comparisonOperators) {
        for (String binaryDoubleOperator : binaryDoubleOperators) {
          evaluate(
              String.format("exp(sin(ATT1) %s ATT2) %s ATT3 %s true",
                  binaryDoubleOperator, comparisonOperator, booleanBinaryOperator),
              11.0, 43.0, 89.0
              );
        }
      }
    }
    
  }

  /**
   * Tests the "ismissing" functionality.
   */
  public void testIsmissing() {
    m_Filter = getFilter("ismissing(ATT3)");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(3, result.numInstances());
  }
  
  /**
   * Tests the "not ismissing" functionality.
   */
  public void testNotIsmissing() {
    m_Filter = getFilter("not ismissing(ATT3)");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances() - 3, result.numInstances());
  }
  
  /**
   * Tests the "CLASS" shortcut with 'is'.
   */
  public void testClassIs() {
    m_Filter = getFilter("CLASS is 'g'");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(3, result.numInstances());
  }
  
  /**
   * Tests the "CLASS" shortcut with 'regexp'.
   */
  public void testClassRegexp() {
    m_Filter = getFilter("CLASS regexp '(r|g)'");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(15, result.numInstances());
  }
  
  /**
   * Tests the "CLASS" shortcut with 'is' over all class labels, using ' or '.
   */
  public void testClassIs2() {
    m_Filter = getFilter("(CLASS is 'r') or (CLASS is 'g') or (CLASS is 'b')");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(), result.numInstances());
  }
  
  /**
   * Tests the "ATT1" placeholder with 'is'.
   */
  public void testAttIs() {
    m_Filter = getFilter("ATT1 is 'r'");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(12, result.numInstances());
  }
  
  /**
   * Tests the "ATT1" placeholder with 'regexp'.
   */
  public void testAttRegexp() {
    m_Filter = getFilter("ATT1 regexp '(r|g)'");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(15, result.numInstances());
  }
  
  /**
   * Tests the "&gt;" functionality.
   */
  public void testGreater() {
    m_Filter = getFilter("ATT2 > 4");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(13, result.numInstances());
  }
  
  /**
   * Tests the "&lt;" functionality.
   */
  public void testLess() {
    m_Filter = getFilter("ATT2 < 4");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(7, result.numInstances());
  }
  
  /**
   * Tests the "&gt;=" functionality.
   */
  public void testGreaterOrEqual() {
    m_Filter = getFilter("ATT2 >= 4");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(14, result.numInstances());
  }
  
  /**
   * Tests the "&lt;=" functionality.
   */
  public void testLessOrEqual() {
    m_Filter = getFilter("ATT2 <= 4");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(8, result.numInstances());
  }
  
  /**
   * Tests the "=" functionality.
   */
  public void testEqual() {
    m_Filter = getFilter("ATT2 = 4");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(1, result.numInstances());
  }
  
  /**
   * Tests the "ATT1" shortcut with 'is' and restricting it via ' and '.
   */
  public void testAnd() {
    m_Filter = getFilter("(ATT1 is 'r') and (ATT2 <= 5)");
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(6, result.numInstances());
  }

  private static boolean evaluate(String expr, double... values) throws Exception {
    HashMap<String, Object> variables = new HashMap<String, Object>();
    for (int i = 0; i < values.length; i++) {
      variables.put("ATT" + (i + 1), values[i]);
    }
    
    DefaultSymbolFactory sf = new DefaultSymbolFactory();
    Parser parser = new Parser(new Scanner(new ByteArrayInputStream(expr.getBytes()), sf), sf);
    parser.setSymbols(variables);
    parser.parse();
    return parser.getResult();
  }

  /**
   * Returns a test suite.
   * 
   * @return		test suite
   */
  public static Test suite() {
    return new TestSuite(SubsetByExpressionTest.class);
  }

  /**
   * Runs the test from command-line.
   * 
   * @param args	ignored
   */
  public static void main(String[] args){
    TestRunner.run(suite());
  }
}
