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
 * Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 */

package weka.core;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

// TODO: documentation for tests
/**
 * Tests AttributeExpression. Run from the command line with:
 * <p/>
 * java weka.core.AttributeExpressionTest
 * 
 * java weka.core.MathematicalTest
 * @author Benjamin Weber ( benweber at student dot ethz dot ch )
 * @version $Revision 1000 $
 */
public class AttributeExpressionTest extends TestCase {
 
  /**
   * Constructs the <code>AttributeExpressionTest</code>.
   * 
   * @param name the name of the test class
   */
  public AttributeExpressionTest(String name) {
    super(name);
  }

  public static Test suite() {
    return new TestSuite(AttributeExpressionTest.class);
  } 

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }

  public void testBasic() throws Exception {
    // basic tests, taken from MathematicalExpressionTest
    assertEquals(4.0, evaluate("a1-a2+a3", 4, 2, 2));
    assertEquals(0.0, evaluate("a1-a2*a3", 4, 2, 2));
    assertEquals(4.0, evaluate("(a1-a2)*a3", 4, 2, 2));
    assertEquals(-10.0, evaluate("a1-a2*(a3+5)", 4, 2, 2));
    assertEquals(12.0, evaluate("a1^a2*1-a3*2", 4, 2, 2));
  }
  
  public void testParsing() throws Exception {
    
    // parser respects brackets:
    assertEquals(4.0,
        evaluate("8.0/(4.0/2.0)"));
    assertEquals(6.0,
        evaluate("8.0 - (4.0 - 2.0)"));
    
    // bug in MathematicalExpression
    evaluate("(4.0-2.0)");

    // unary operators
    // bug in MathExpression
    evaluate("--------1");
    evaluate("1--------1");
    evaluate("1-+-+-+-+1");
    evaluate("1++++++++1");

    // parser can handle brackets
    evaluate("((((((((((((1))))))))))))");
    evaluate("((((((((((((1)))))))))))) + ((((((((((((1))))))))))))");
    evaluate("((((((((((((1) + 1) + 1) + 1) + 1) + 1) + 1) + 1) + 1) + 1) + 1) + 1) + 1");
    
    // parser can handle many operators in a row
    evaluate("1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1");
    evaluate("1 - 1 - 1 - 1 - 1 - 1 - 1 - 1 - 1 - 1 - 1 - 1 - 1");
    evaluate("1 * 1 * 1 * 1 * 1 * 1 * 1 * 1 * 1 * 1 * 1 * 1 * 1");
    evaluate("1 / 1 / 1 / 1 / 1 / 1 / 1 / 1 / 1 / 1 / 1 / 1 / 1");
    evaluate("1 ^ 1 ^ 1 ^ 1 ^ 1 ^ 1 ^ 1 ^ 1 ^ 1 ^ 1 ^ 1 ^ 1 ^ 1");

    // float parsing
    try {
      assertEquals(1.0, evaluate("1"));
    } catch (Exception e) {
      fail("Parsing failure!");
    }
    try {
      assertEquals(1.0, evaluate("1.0"));
    } catch (Exception e) {
      fail("Parsing failure!");
    }
    try {
      assertEquals(1.0, evaluate("1.0000000000000000000000000000000"));
    } catch (Exception e) {
      fail("Parsing failure!");
    }
    try {
      assertEquals(0.0, evaluate("0"));
    } catch (Exception e) {
      fail("Parsing failure!");
    }
    try {
      assertEquals(0.0, evaluate("0.0"));
    } catch (Exception e) {
      fail("Parsing failure!");
    }
    try {
      assertEquals(0.0, evaluate("0.00000000000000000000000000000000"));
    } catch (Exception e) {
      fail("Parsing failure!");
    }
    try {
      assertEquals(-1.0, evaluate("-1"));
    } catch (Exception e) {
      fail("Parsing failure!");
    }
    try {
      assertEquals(-1.0, evaluate("-1.0"));
    } catch (Exception e) {
      fail("Parsing failure!");
    }
    try {
      assertEquals(-1.0, evaluate("-1.00000000000000000000000000000000"));
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
    evaluate("-+-+-+-+1");
  }

  public void testParsingBug2() throws Exception {
    evaluate("++++++++1");
  }
  
  public void testParsingBug3() throws Exception {
    evaluate("0.0 + (-0.0 + 0.0)");
  }
  
  public void testParsingBug4() {
    try {
      evaluate("f log oor(2.0)");
      fail("Accepted unreasonable syntax!");
    } catch (Exception e) {
      // ok
    }
  }
  
  public void testVariables() throws Exception {
    
    double[] values = {0.0, 1.0, 2.0, -1.0, -0.0};
    
    // retains value
    for (double value : values) {
      assertEquals(value, evaluate("a1", value));
    }
    
    // can deal with several variables
    for (double first : values) {
      for (double second : values) {
        for (double third : values) {
          assertEquals(
              first + second + third,
              evaluate("a1 + a2 + a3", first, second, third)
              );
          assertEquals(
              first * second * third,
              evaluate("a1 * a2 * a3", first, second, third)
              );
        }
      }
    }
    
  }
  
  public void testMathFunctions() throws Exception {
    
    for (double v : new double[]{1.0, 2.0, 100.0, -1.0, -2.0, -100.0, 0.0}) {
      assertEquals(Math.abs(v), evaluate("abs(a1)", v));
      if (!Double.isInfinite(Math.log(v)))
        assertEquals(Math.log(v), evaluate("log(a1)", v));
      assertEquals(Math.exp(v), evaluate("exp(a1)", v));
      assertEquals(Math.sin(v), evaluate("sin(a1)", v));
      assertEquals(Math.cos(v), evaluate("cos(a1)", v));
      assertEquals(Math.tan(v), evaluate("tan(a1)", v));
      assertEquals(Math.sqrt(v), evaluate("sqrt(a1)", v));
      assertEquals(Math.floor(v), evaluate("floor(a1)", v));
      assertEquals(Math.ceil(v), evaluate("ceil(a1)", v));
      assertEquals(Math.rint(v), evaluate("rint(a1)", v));

      assertEquals(Math.abs(v), evaluate("abs a1", v));
      if (!Double.isInfinite(Math.log(v)))
        assertEquals(Math.log(v), evaluate("log a1", v));
      assertEquals(Math.exp(v), evaluate("exp a1", v));
      assertEquals(Math.sin(v), evaluate("sin a1", v));
      assertEquals(Math.cos(v), evaluate("cos a1", v));
      assertEquals(Math.tan(v), evaluate("tan a1", v));
      assertEquals(Math.sqrt(v), evaluate("sqrt a1", v));
      assertEquals(Math.floor(v), evaluate("floor a1", v));
      assertEquals(Math.ceil(v), evaluate("ceil a1", v));
      assertEquals(Math.rint(v), evaluate("rint a1", v));

      assertEquals(Math.abs(v), evaluate("b a1", v));
      if (!Double.isInfinite(Math.log(v)))
        assertEquals(Math.log(v), evaluate("l a1", v));
      assertEquals(Math.exp(v), evaluate("e a1", v));
      assertEquals(Math.sin(v), evaluate("n a1", v));
      assertEquals(Math.cos(v), evaluate("c a1", v));
      assertEquals(Math.tan(v), evaluate("t a1", v));
      assertEquals(Math.sqrt(v), evaluate("s a1", v));
      assertEquals(Math.floor(v), evaluate("f a1", v));
      assertEquals(Math.ceil(v), evaluate("h a1", v));
      assertEquals(Math.rint(v), evaluate("r a1", v));

      assertEquals(Math.abs(v), evaluate("b(a1)", v));
      if (!Double.isInfinite(Math.log(v)))
        assertEquals(Math.log(v), evaluate("l(a1)", v));
      assertEquals(Math.exp(v), evaluate("e(a1)", v));
      assertEquals(Math.sin(v), evaluate("n(a1)", v));
      assertEquals(Math.cos(v), evaluate("c(a1)", v));
      assertEquals(Math.tan(v), evaluate("t(a1)", v));
      assertEquals(Math.sqrt(v), evaluate("s(a1)", v));
      assertEquals(Math.floor(v), evaluate("f(a1)", v));
      assertEquals(Math.ceil(v), evaluate("h(a1)", v));
      assertEquals(Math.rint(v), evaluate("r(a1)", v));
    }
  }
  
  public void testPlusOperator() throws Exception {
    
    double[] values = {0.0,  1.0,  2.0,  3.0,  100.0,  1000.0,  1e8,  1e-8,  0.1,
                      -0.0, -1.0, -2.0, -3.0, -100.0, -1000.0, -1e8, -1e-8, -0.1};
    
    // Note: assertEquals(0.0, -0.0) fails, thus we're using asserTrue(... == ...)

    for (double left : values) {
      
      // neutral element:
      assertTrue(left == evaluate(String.format("%.20f + 0.0", left)));
      assertTrue(left == evaluate(String.format("0.0 + %.20f", left)));

      for (double right: values) {
        // basics
        assertTrue(left + right == evaluate(String.format("%.20f + %.20f", left, right)));
        
        // commutativity
        assertTrue(evaluate(String.format("%.20f + %.20f", left, right)) == 
                   evaluate(String.format("%.20f + %.20f", right, left)));
      }
    }

    double[] safe_values = {0.0, 1.0, 2.0};//, -0.0, -1.0, -2.0};
    for (double first : safe_values) {
      for (double second : safe_values) {
        for (double third : safe_values) {
          // due to commutativity the operator is both left and right associative
          // so we test both

          // left associativity
          assertTrue(
              evaluate(String.format(
                  "%.20f + %.20f + %.20f", first, second, third))
              == 
              evaluate(String.format(
                  "(%.20f + %.20f) + %.20f", first, second, third))
              );

          // right associativity
          assertTrue(
              evaluate(String.format(
                  "%.20f + %.20f + %.20f", first, second, third))
              == 
              evaluate(String.format(
                  "%.20f + (%.20f + %.20f)", first, second, third))
              );
        }
      }
    }

    // basic double semantics
    // NaN + v == NaN
    assertTrue(Double.isNaN(evaluate("sqrt(-1) + 1.0")));
    // NaN + NaN == NaN
    assertTrue(Double.isNaN(evaluate("sqrt(-1) + sqrt(1 - 2)")));
    // overflow -> +inf // AttributeExpression converts inf & NaN to Utils.missingValue()
    assertTrue(Double.isNaN(evaluate(String.format(
        "%.20f + %.20f", Double.MAX_VALUE, Double.MAX_VALUE))));
    // inf + (-inf) == NaN
    assertTrue(Double.isNaN(evaluate("1/0 + -1/0")));
    // inf + inf == inf // AttributeExpression converts inf & NaN to Utils.missingValue()
    assertTrue(Double.isNaN(evaluate("1/0 + 1/0")));
    
  }
  
  public void testMinusOperator() throws Exception {
    
    double[] values = {0.0,  1.0,  2.0,  3.0,  100.0,  1000.0,  1e8,  1e-8,  0.1,
                      -0.0, -1.0, -2.0, -3.0, -100.0, -1000.0, -1e8, -1e-8, -0.1};
    
    // Note: assertEquals(0.0, -0.0) fails, thus we're using asserTrue(... == ...)

    for (double left : values) {
      
      // neutral element:
      assertTrue(left == evaluate(String.format("%.20f - 0.0", left)));

      for (double right: values) {
        // basics
        assertTrue(left - right == evaluate(String.format("%.20f - %.20f", left, right)));
      }
    }

    double[] safe_values = {0.0, 1.0, 2.0, -0.0, -1.0, -2.0};
    for (double first : safe_values) {
      for (double second : safe_values) {
        for (double third : safe_values) {
          // left associativity
          assertTrue(
              evaluate(String.format(
                  "%.20f - %.20f - %.20f", first, second, third))
              == 
              evaluate(String.format(
                  "(%.20f - %.20f) - %.20f", first, second, third))
              );
        }
      }
    }

    // basic double semantics
    // NaN - v == NaN
    assertTrue(Double.isNaN(evaluate("sqrt(-1) - 1.0")));
    // NaN - NaN == NaN
    assertTrue(Double.isNaN(evaluate("sqrt(-1) - sqrt(1 - 2)")));
    // underflow -> -inf // AttributeExpression converts inf & NaN to Utils.missingValue()
    assertTrue(Double.isNaN(evaluate(String.format(
        "%.20f - %.20f", -Double.MAX_VALUE, Double.MAX_VALUE))));
    // inf - inf == NaN
    assertTrue(Double.isNaN(evaluate("1/0 - 1/0")));
    
  }
 
  public void testTimesOperator() throws Exception {
    
    double[] values = {0.0,  1.0,  2.0,  3.0,  100.0,  1000.0,  1e8,  1e-8,  0.1,
                       -0.0, -1.0, -2.0, -3.0, -100.0, -1000.0, -1e8, -1e-8, -0.1};
    
    // Note: assertEquals(0.0, -0.0) fails, thus we're using asserTrue(... == ...)

    for (double left : values) {
      
      // neutral element:
      assertTrue(left == evaluate(String.format("%.20f * 1.0", left)));
      assertTrue(left == evaluate(String.format("1.0 * %.20f", left)));

      for (double right: values) {
        // basics
        assertTrue(left * right == evaluate(String.format("%.20f * %.20f", left, right)));
        
        // commutativity
        assertTrue(evaluate(String.format("%.20f * %.20f", left, right)) == 
                   evaluate(String.format("%.20f * %.20f", right, left)));
      }
    }

    double[] safe_values = {0.0, 1.0, 2.0};//, -0.0, -1.0, -2.0};
    for (double first : safe_values) {
      for (double second : safe_values) {
        for (double third : safe_values) {
          // due to commutativity the operator is both left and right associative
          // so we test both

          // left associativity
          assertTrue(
              evaluate(String.format(
                  "%.20f * %.20f * %.20f", first, second, third))
              == 
              evaluate(String.format(
                  "(%.20f * %.20f) * %.20f", first, second, third))
              );

          // right associativity
          assertTrue(
              evaluate(String.format(
                  "%.20f * %.20f * %.20f", first, second, third))
              == 
              evaluate(String.format(
                  "%.20f * (%.20f * %.20f)", first, second, third))
              );
        }
      }
    }

    // basic double semantics
    // NaN * v == NaN
    assertTrue(Double.isNaN(evaluate("sqrt(-1) * 1.0")));
    // NaN * NaN == NaN
    assertTrue(Double.isNaN(evaluate("sqrt(-1) * sqrt(1 - 2)")));
    // overflow -> +inf // AttributeExpression converts inf & NaN to Utils.missingValue()
    assertTrue(Double.isNaN(evaluate(String.format(
        "%.20f * %.20f", Double.MAX_VALUE, Double.MAX_VALUE))));
    // inf + (-inf) == -inf  // AttributeExpression converts inf & NaN to Utils.missingValue()
    assertTrue(Double.isNaN(evaluate("(1/0) * ((1-2)/0)")));
    // inf * inf == inf  // AttributeExpression converts inf & NaN to Utils.missingValue()
    assertTrue(Double.isNaN(evaluate("(1/0) * (1/0)")));
    
  }
  
  public void testDivisionOperator() throws Exception {
    
    double[] values = {0.0,  1.0,  2.0,  3.0,  100.0,  1000.0,  1e8,  1e-8,  0.1,
                       -0.0, -1.0, -2.0, -3.0, -100.0, -1000.0, -1e8, -1e-8, -0.1};
    
    // Note: assertEquals(0.0, -0.0) fails, thus we're using asserTrue(... == ...)

    for (double left : values) {
      
      // neutral element:
      assertTrue(left == evaluate(String.format("%.20f / 1.0", left)));

      for (double right: values) {
        // basics
        if (left == 0.0 && right == 0.0)
          continue; // NaN == NaN always evaluates to false
        if (Double.isInfinite(left/right))
          continue; // AttributeExpression converts inf & NaN to Utils.missingValue()
        assertTrue(left / right == evaluate(String.format("%.20f / (0 + %.20f)", left, right)));
      }
    }

    double[] safe_values = {0.0, 1.0, 2.0, -0.0, -1.0, -2.0};
    for (double first : safe_values) {
      for (double second : safe_values) {
        for (double third : safe_values) {
          if (first == 0.0 && (second == 0.0 || third == 0.0))
            continue; // NaN == NaN always evaluates to false
          if (second == 0.0 || third == 0.0)
            continue; // AttributeExpression converts inf & NaN to Utils.missingValue()
          // left associativity
          assertTrue(
              evaluate(String.format(
                  "%.20f / %.20f / %.20f", first, second, third))
              == 
              evaluate(String.format(
                  "(%.20f / %.20f) / %.20f", first, second, third))
              );
        }
      }
    }

    // basic double semantics
    // NaN / v == NaN
    assertTrue(Double.isNaN(evaluate("sqrt(-1) / 1.0")));
    // NaN / NaN == NaN
    assertTrue(Double.isNaN(evaluate("sqrt(-1) / sqrt(1 - 2)")));
    // overflow -> +inf // AttributeExpression converts inf & NaN to Utils.missingValue();
    assertTrue(Double.isNaN(evaluate(String.format(
        "%.20f + %.20f", Double.MAX_VALUE, Double.MAX_VALUE))));
    // inf / inf == NaN 
    assertTrue(Double.isNaN(evaluate("(1/0) / (1/0)")));
    
  }
 
  public void testPrecedence() throws Exception {
    
    // *, / have higher precedence than +, -
    assertEquals(10.0,
        evaluate("2.0*3.0 + 4.0"));
    assertEquals(2.0,
        evaluate("2.0*3.0 - 4.0"));
    assertEquals(-0.5,
        evaluate("2.0/4.0 - 1.0"));
    assertEquals(1.5,
        evaluate("2.0/4.0 + 1.0"));
    // unary - has higher precedence than +, -
    assertEquals(1.0,
        evaluate("-1.0 + 2.0"));
    assertEquals(-3.0,
        evaluate("-1.0 - 2.0"));
    // due to the distributive law we don't have to test for unary - over *, /
    
  }
  
  public void testPrecedenceBug() throws Exception {
    // ^ has higher precedence than *, /
    // bug in AttributeExpression
    assertEquals(32.0, evaluate("8*2^2"));
    assertEquals(2.0, evaluate("8/2^2"));
  }

  public void testCompositions() throws Exception {

    String[] binaryDoubleOperators = {"+", "-", "*", "/", "^"};
    
    for (String binaryDoubleOperator : binaryDoubleOperators) {
      evaluate(
          String.format("exp(a1) %s log(rint(a2) - a3^sin(a2)) %s tan(a1)",
              binaryDoubleOperator, binaryDoubleOperator),
              11.0, 43.0, 89.0
          );
    }
    
  }
  
  private static double evaluate(String expr, double... values) throws Exception {
    
    AttributeExpression e = new AttributeExpression();
    e.convertInfixToPostfix(expr);
    double[] results = new double[values.length + 1];
    System.arraycopy(values, 0, results, 0, values.length);
    e.evaluateExpression(results);
    return results[results.length - 1];
  }
  
}
