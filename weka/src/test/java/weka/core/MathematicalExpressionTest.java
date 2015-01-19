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
 * Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
 */

package weka.core;

import java.util.HashMap;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

// TODO: documentation for tests
/**
 * Tests MathematicalExpression. Run from the command line with:
 * <p/>
 * java weka.core.MathematicalExpressionTest
 * 
 * @author mhall (mhall{[at]}pentaho{[dot]}org)
 * @version $Revision$
 */
public class MathematicalExpressionTest extends TestCase {

  /**
   * Constructs the <code>MathematicalExpresionTest</code>.
   * 
   * @param name the name of the test class
   */
  public MathematicalExpressionTest(String name) {
    super(name);
  }

  public static Test suite() {
    return new TestSuite(MathematicalExpressionTest.class);
  }
  
  public void testBasic() throws Exception {
    // basic tests, taken from MathematicalExpressionTest
    assertEquals(4.0, evaluate("A-B+C", 4, 2, 2));
    assertEquals(0.0, evaluate("A-B*C", 4, 2, 2));
    assertEquals(4.0, evaluate("(A-B)*C", 4, 2, 2));
    assertEquals(-10.0, evaluate("A-B*(C+5)", 4, 2, 2));
    assertEquals(12.0, evaluate("pow(A,B*1)-C*2", 4, 2, 2));
    assertEquals(6.0, evaluate("ifelse((C<1000|C>5000),(A+B),C+C)", 4, 2, 2));
  }
  
  public void testParsing() throws Exception {
    
    // parser respects brackets:
    assertEquals(4.0,
        evaluate("8.0/(4.0/2.0)"));
    assertEquals(6.0,
        evaluate("8.0 - (4.0 - 2.0)"));
    
    // parser can handle brackets
    evaluate("((((((((((((1))))))))))))");
    evaluate("((((((((((((1)))))))))))) + ((((((((((((1))))))))))))");
    evaluate("((((((((((((1) + 1) + 1) + 1) + 1) + 1) + 1) + 1) + 1) + 1) + 1) + 1) + 1");

    evaluate("ifelse(! ! ! ! ! true, 1.0, 0.0)");
    evaluate("ifelse(!!!!!true, 1.0, 0.0)");
   
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
  }
  

  public void testParsingBug1() {
    try {
      // scanner error
      evaluate("1.000a0");
      fail("Failed to find syntax error!");
    } catch (Exception e) {
      // ok
    }
  }
  
  public void testParsingBug2() throws Exception {
    // bug in MathematicalExpression
    evaluate("(4.0-2.0)");
  }
  
  public void testParsingBug3() throws Exception {
    // bug in MathExpression
    evaluate("--------1");
    evaluate("1--------1");
    evaluate("++++++++1");
    evaluate("1++++++++1");
    evaluate("-+-+-+-+1");
    evaluate("1-+-+-+-+1");
  }

  public void testVariables() throws Exception {
    
    double[] values = {0.0, 1.0, 2.0, -1.0, -0.0};
    
    // retains value
    for (double value : values) {
      assertEquals(value, evaluate("A", value));
    }
    
    // can deal with several variables
    for (double first : values) {
      for (double second : values) {
        for (double third : values) {
          assertEquals(
              first + second + third,
              evaluate("A + B + C", first, second, third)
              );
          assertEquals(
              first * second * third,
              evaluate("A * B * C", first, second, third)
              );
        }
      }
    }
    
  }
  
  public void testMathFunctions() throws Exception {
    
    for (double v : new double[]{1.0, 2.0, 100.0, -1.0, -2.0, -100.0, 0.0}) {
      assertEquals(Math.abs(v), evaluate("abs(A)", v));
      assertEquals(Math.log(v), evaluate("log(A)", v));
      assertEquals(Math.exp(v), evaluate("exp(A)", v));
      assertEquals(Math.sin(v), evaluate("sin(A)", v));
      assertEquals(Math.cos(v), evaluate("cos(A)", v));
      assertEquals(Math.tan(v), evaluate("tan(A)", v));
      assertEquals(Math.sqrt(v), evaluate("sqrt(A)", v));
      assertEquals(Math.floor(v), evaluate("floor(A)", v));
      assertEquals(Math.ceil(v), evaluate("ceil(A)", v));
      assertEquals(Math.rint(v), evaluate("rint(A)", v));
      assertEquals(Math.pow(v, v), evaluate("pow(A, A)", v));
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

    double[] safe_values = {0.0, 1.0, 2.0, -0.0, -1.0, -2.0};
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
    assertTrue(Double.isNaN(evaluate("sqrt(-1) + sqrt(-1.0)")));
    // overflow -> +inf
    assertTrue(Double.isInfinite(evaluate(String.format(
        "%.20f + %.20f", Double.MAX_VALUE, Double.MAX_VALUE))));
    // inf + (-inf) == NaN
    assertTrue(Double.isNaN(evaluate("1/0 + -1/0")));
    // inf + inf == inf 
    assertTrue(Double.isInfinite(evaluate("1/0 + 1/0")));
    
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
    assertTrue(Double.isNaN(evaluate("sqrt(-1) - sqrt(-1.0)")));
    // underflow -> -inf
    assertTrue(Double.isInfinite(evaluate(String.format(
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

    double[] safe_values = {0.0, 1.0, 2.0, -0.0, -1.0, -2.0};
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
    assertTrue(Double.isNaN(evaluate("sqrt(-1) * sqrt(-1.0)")));
    // overflow -> +inf
    assertTrue(Double.isInfinite(evaluate(String.format(
        "%.20f * %.20f", Double.MAX_VALUE, Double.MAX_VALUE))));
    // inf + (-inf) == -inf 
    assertTrue(Double.isInfinite(evaluate("(1/0) * (-1/0)")));
    // inf * inf == inf 
    assertTrue(Double.isInfinite(evaluate("(1/0) * (1/0)")));
    
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
        assertTrue(left / right == evaluate(String.format("%.20f / %.20f", left, right)));
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
    assertTrue(Double.isNaN(evaluate("sqrt(-1) / sqrt(-1.0)")));
    // overflow -> +inf
    assertTrue(Double.isInfinite(evaluate(String.format(
        "%.20f + %.20f", Double.MAX_VALUE, Double.MAX_VALUE))));
    // inf / inf == NaN 
    assertTrue(Double.isNaN(evaluate("(1/0) / (1/0)")));
    
  }
  
  public void testAndOperator() throws Exception {
    
    boolean[] values = {true, false};
    for (boolean left : values) {
      for (boolean right : values) {
        // basics
        assertEquals(
            (left && right ? 1.0 : 0.0),
            evaluate("ifelse(" + left + " & " + right + ", 1.0, 0.0)")
            );
        
        // commutativity
        assertEquals(
            evaluate("ifelse(" + right + " & " + left + ", 1.0, 0.0)"),
            evaluate("ifelse(" + left + " & " + right + ", 1.0, 0.0)")
            );
        
        for (boolean third : values) {
          // due to commutativity the operator is both left and right associative
          // so we test both
          
          // left associativity
          assertEquals(
              left && right && third ? 1.0 : 0.0,
              evaluate("ifelse((" + left + " & " + right + ") & " + third + ", 1.0, 0.0)")
              );

          // right associativity
          assertEquals(
              left && right && third ? 1.0 : 0.0,
              evaluate("ifelse(" + left + " & (" + right + " & " + third + "), 1.0, 0.0)")
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
            (left || right ? 1.0 : 0.0),
            evaluate("ifelse(" + left + " | " + right + ", 1.0, 0.0)")
            );
        
        // commutativity
        assertEquals(
            evaluate("ifelse(" + right + " | " + left + ", 1.0, 0.0)"),
            evaluate("ifelse(" + left + " | " + right + ", 1.0, 0.0)")
            );
        
        for (boolean third : values) {
          // due to commutativity the operator is both left and right associative
          // so we test both
          
          // left associativity
          assertEquals(
              left || right || third ? 1.0 : 0.0,
              evaluate("ifelse((" + left + " | " + right + ") | " + third + ", 1.0, 0.0)")
              );

          // right associativity
          assertEquals(
              left || right || third ? 1.0 : 0.0,
              evaluate("ifelse(" + left + " | (" + right + " | " + third + "), 1.0, 0.0)")
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
      assertEquals(1.0,
          evaluate(String.format(
              "ifelse(%.20f = %.20f, 1.0, 0.0)", left, left)));
      
      for (double right: values) {
        // basics
        assertEquals(
            left == right ? 1.0 : 0.0,
            evaluate(String.format(
                "ifelse(%.20f = %.20f, 1.0, 0.0)", left, right))
            );
        
        // commutativity
        assertEquals(
            evaluate(String.format(
                "ifelse(%.20f = %.20f, 1.0, 0.0)", left, right)),
            evaluate(String.format(
                "ifelse(%.20f = %.20f, 1.0, 0.0)", right, left))
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
    assertEquals(1.0,
        evaluate("ifelse(1/0 = 1/0, 1.0, 0.0)"));
    // -inf = inf -> false
    assertEquals(0.0,
        evaluate("ifelse(-1/0 = 1/0, 1.0, 0.0)"));
    // NaN = NaN -> false
    assertEquals(0.0,
        evaluate("ifelse(sqrt(-1) = sqrt(-1), 1.0, 0.0)"));
    // 0 = NaN -> false
    assertEquals(0.0,
        evaluate("ifelse(0.0 = sqrt(-1), 1.0, 0.0)"));
    // inf = NaN -> false
    assertEquals(0.0,
        evaluate("ifelse(1/0 = sqrt(-1), 1.0, 0.0)"));
    
  }

  public void testLtLeGtGeOperator() throws Exception {
  
    double[] values = {0.0,  1.0,  2.0,  3.0,  100.0,  1000.0,  1e8,  1e-8,  0.1,
                       -0.0, -1.0, -2.0, -3.0, -100.0, -1000.0, -1e8, -1e-8, -0.1};
    
    for (double left : values) {
      
      assertEquals(0.0,
          evaluate(String.format(
              "ifelse(%.20f < %.20f, 1.0, 0.0)", left, left)));
      assertEquals(1.0,
          evaluate(String.format(
              "ifelse(%.20f <= %.20f, 1.0, 0.0)", left, left)));
      assertEquals(0.0,
          evaluate(String.format(
              "ifelse(%.20f > %.20f, 1.0, 0.0)", left, left)));
      assertEquals(1.0,
          evaluate(String.format(
              "ifelse(%.20f >= %.20f, 1.0, 0.0)", left, left)));
      
      for (double right: values) {
        // basics
        assertEquals(
            left < right ? 1.0 : 0.0,
            evaluate(String.format(
                "ifelse(%.20f < %.20f, 1.0, 0.0)", left, right))
            );
        assertEquals(
            left <= right ? 1.0 : 0.0,
            evaluate(String.format(
                "ifelse(%.20f <= %.20f, 1.0, 0.0)", left, right))
            );
        assertEquals(
            left > right ? 1.0 : 0.0,
            evaluate(String.format(
                "ifelse(%.20f > %.20f, 1.0, 0.0)", left, right))
            );
        assertEquals(
            left >= right ? 1.0 : 0.0,
            evaluate(String.format(
                "ifelse(%.20f >= %.20f, 1.0, 0.0)", left, right))
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
    assertEquals(0.0,
        evaluate("ifelse(1/0 < 1/0, 1.0, 0.0)"));
    // inf <= inf -> true
    assertEquals(1.0,
        evaluate("ifelse(1/0 <= 1/0, 1.0, 0.0)"));
    // inf > inf -> false
    assertEquals(0.0,
        evaluate("ifelse(1/0 > 1/0, 1.0, 0.0)"));
    // inf >= inf -> true
    assertEquals(1.0,
        evaluate("ifelse(1/0 >= 1/0, 1.0, 0.0)"));

    // -inf < inf -> true
    assertEquals(1.0,
        evaluate("ifelse(-1/0 < 1/0, 1.0, 0.0)"));
    // -inf <= inf -> true
    assertEquals(1.0,
        evaluate("ifelse(-1/0 <= 1/0, 1.0, 0.0)"));
    // -inf > inf -> false
    assertEquals(0.0,
        evaluate("ifelse(-1/0 > 1/0, 1.0, 0.0)"));
    // -inf >= inf -> false
    assertEquals(0.0,
        evaluate("ifelse(-1/0 >= 1/0, 1.0, 0.0)"));

    // NaN < NaN -> false
    assertEquals(0.0,
        evaluate("ifelse(sqrt(-1) < sqrt(-1), 1.0, 0.0)"));
    // NaN <= NaN -> false
    assertEquals(0.0,
        evaluate("ifelse(sqrt(-1) <= sqrt(-1), 1.0, 0.0)"));
    // NaN > NaN -> false
    assertEquals(0.0,
        evaluate("ifelse(sqrt(-1) > sqrt(-1), 1.0, 0.0)"));
    // NaN >= NaN -> false
    assertEquals(0.0,
        evaluate("ifelse(sqrt(-1) >= sqrt(-1), 1.0, 0.0)"));

    // 0 < NaN -> false
    assertEquals(0.0,
        evaluate("ifelse(0.0 < sqrt(-1), 1.0, 0.0)"));
    // 0 <= NaN -> false
    assertEquals(0.0,
        evaluate("ifelse(0.0 <= sqrt(-1), 1.0, 0.0)"));
    // 0 > NaN -> false
    assertEquals(0.0,
        evaluate("ifelse(0.0 > sqrt(-1), 1.0, 0.0)"));
    // 0 >= NaN -> false
    assertEquals(0.0,
        evaluate("ifelse(0.0 >= sqrt(-1), 1.0, 0.0)"));

    // inf < NaN -> false
    assertEquals(0.0,
        evaluate("ifelse(1/0 < sqrt(-1), 1.0, 0.0)"));
    // inf <= NaN -> false
    assertEquals(0.0,
        evaluate("ifelse(1/0 <= sqrt(-1), 1.0, 0.0)"));
    // inf > NaN -> false
    assertEquals(0.0,
        evaluate("ifelse(1/0 > sqrt(-1), 1.0, 0.0)"));
    // inf >= NaN -> false
    assertEquals(0.0,
        evaluate("ifelse(1/0 >= sqrt(-1), 1.0, 0.0)"));
    
  }
  
  public void testPrecedence() throws Exception {
    
    // && has higher precedence than ||
    assertEquals(1.0,
        evaluate("ifelse(false & false | true, 1.0, 0.0)"));
    assertEquals(1.0,
        evaluate("ifelse(false & true | true, 1.0, 0.0)"));
    // ! has higher precedence than &&
    assertEquals(0.0,
        evaluate("ifelse(!true & false, 1.0, 0.0)"));
    assertEquals(0.0,
        evaluate("ifelse(!false & false, 1.0, 0.0)"));
    // ! has higher precedence than ||
    assertEquals(1.0,
        evaluate("ifelse(!true | true, 1.0, 0.0)"));
    assertEquals(1.0,
        evaluate("ifelse(!false | true, 1.0, 0.0)"));
    
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
    
    // +, -, /, * have higher precedence than =, <=, <, >, >=
    // if not, then the program will fail due to a type error
    assertEquals(1.0,
        evaluate("ifelse(1.0 + 2.0 >= 2.5, 1.0, 0.0)")
        );
    assertEquals(0.0,
        evaluate("ifelse(-2.0 - 2.0 > -2.5, 1.0, 0.0)")
        );
    assertEquals(0.0,
        evaluate("ifelse(2.0 * 2.0 <= 2.5, 1.0, 0.0)")
        );
    assertEquals(0.0,
        evaluate("ifelse(1.0 / 0.5 < 1.0, 1.0, 0.0)")
        );
    assertEquals(1.0,
        evaluate("ifelse(1.0 + 2.0 = 3.0, 1.0, 0.0)")
        );

    // =, <=, <, >, >= have higher precedence than &&, ||
    // if not, then the program will fail due to a type error
    assertEquals(1.0,
        evaluate("ifelse(1.0 < 2.0 & true, 1.0, 0.0)"));
    assertEquals(1.0,
        evaluate("ifelse(1.0 <= 2.0 | false, 1.0, 0.0)"));
    assertEquals(0.0,
        evaluate("ifelse(1.0 > 2.0 & true, 1.0, 0.0)"));
    assertEquals(0.0,
        evaluate("ifelse(1.0 >= 2.0 | false, 1.0, 0.0)"));
    assertEquals(0.0,
        evaluate("ifelse(1.0 = 2.0 & true, 1.0, 0.0)"));
  }

  public void testCompositions() throws Exception {

    String[] binaryDoubleOperators = {"+", "-", "*", "/"};
    String[] comparisonOperators = {"=", "<", ">="};
    String[] booleanBinaryOperators = {"&", "|"};
    
    for (String booleanBinaryOperator : booleanBinaryOperators) {
      for (String comparisonOperator : comparisonOperators) {
        for (String binaryDoubleOperator : binaryDoubleOperators) {
          evaluate(
              String.format("ifelse(exp(sin(A) %s B) %s C %s true, tan(A), rint(C) + 3.0)",
                  binaryDoubleOperator, comparisonOperator, booleanBinaryOperator),
              11.0, 43.0, 89.0
              );
        }
      }
    }
    
  }
  
  private static double evaluate(String expr, double... values) throws Exception {
    HashMap<String, Double> variables = new HashMap<String, Double>();
    
    for (int i = 0; i < values.length; i++)
      variables.put("" + (char) ('A' + i), values[i]);
    return MathematicalExpression.evaluate(expr,  variables);
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
