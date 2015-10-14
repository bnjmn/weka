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
 *    ExpressionLanguageTest.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.expressionlanguage;

import weka.core.expressionlanguage.common.IfElseMacro;
import weka.core.expressionlanguage.common.JavaMacro;
import weka.core.expressionlanguage.common.MacroDeclarationsCompositor;
import weka.core.expressionlanguage.common.MathFunctions;
import weka.core.expressionlanguage.common.Primitives.BooleanExpression;
import weka.core.expressionlanguage.common.Primitives.DoubleExpression;
import weka.core.expressionlanguage.common.Primitives.StringExpression;
import weka.core.expressionlanguage.common.SimpleVariableDeclarations;
import weka.core.expressionlanguage.core.Node;
import weka.core.expressionlanguage.parser.Parser;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Tests the weka.core.expressionlanguage package.</p>
 * 
 * Run from command line with:</br>
 * java weka.core.expressionlanguage.ExpressionLanguageTest
 * 
 * @author Benjamin Weber ( benweber at student dot ethz dot ch )
 * @version $Revision: 1000 $
 */
public class ExpressionLanguageTest extends TestCase {
  
  /**
   * Constructs the {@link ExpressionLanguageTest}
   * 
   * @param name the name of the test
   */
  public ExpressionLanguageTest(String name) {
    super(name);
  }
  
  private static Node compile(String expression, Object[] values) throws Exception {
    
    SimpleVariableDeclarations variables = new SimpleVariableDeclarations();
    
    for (int i = 0; i < values.length; i++) {
      String name = "" + (char) ('A' + i);
      if (values[i] instanceof Boolean)
        variables.addBoolean(name);
      else if (values[i] instanceof Double)
        variables.addDouble(name);
      else if (values[i] instanceof String)
        variables.addString(name);
      else
        throw new Exception("Unsupported variable type!");
    }
    
    Node node = Parser.parse(
        // expression
        expression,
        // variables
        variables,
        // marcos
        new MacroDeclarationsCompositor(
            new MathFunctions(),
            new IfElseMacro(),
            new JavaMacro()
            )
        );

    for (int i = 0; i < values.length; i++) {
      String name = "" + (char) ('A' + i);
      if (!variables.getInitializer().hasVariable(name))
        continue;
      if (values[i] instanceof Boolean)
        variables.getInitializer().setBoolean(name, (Boolean) values[i]);
      else if (values[i] instanceof Double)
        variables.getInitializer().setDouble(name, (Double) values[i]);
      else if (values[i] instanceof String)
        variables.getInitializer().setString(name, (String) values[i]);
      else
        throw new Exception("Unsupported variable type!");
    }   
    
    return node;

  }

  private static boolean evaluateBoolean(String expr, Object... values) throws Exception {
  
    Node node = compile(expr, values);
    if (!(node instanceof BooleanExpression))
      throw new Exception("Type error in expression!");
  
    return ((BooleanExpression) node).evaluate();
  }

  private static double evaluateDouble(String expr, Object... values) throws Exception {
  
    Node node = compile(expr, values);
    
    if (!(node instanceof DoubleExpression))
      throw new Exception("Type error in expression!");
  
    return ((DoubleExpression) node).evaluate();
  }

  private static String evaluateString(String expr, Object... values) throws Exception {
  
    Node node = compile(expr, values);

    if (!(node instanceof StringExpression))
      throw new Exception("Type error in expression!");
  
    return ((StringExpression) node).evaluate();
  
  }

  /**
   * Tests the parser against a set of extreme situations, simple cases and
   * known bugs of previous languages.
   * 
   * @throws Exception if something goes wrong (mostly compilation errors)
   */
  public void testParsing() throws Exception {
    
    // bug in AttributeExpression
    evaluateDouble("0.0 + (-0.0 + 0.0)");

    // bug in MathematicalExpression
    evaluateDouble("(4.0-2.0)");
    
    // basic strings
    assertEquals("asdf", evaluateString("'asdf'"));
    assertEquals("asdf", evaluateString("\"asdf\""));
    
    // escape sequences
    assertEquals("\b\f\n\r\t\\\"\'", evaluateString("'\\b\\f\\n\\r\\t\\\\\\\"\\''"));
    assertEquals("\b\f\n\r\t\\\"\'", evaluateString("\"\\b\\f\\n\\r\\t\\\\\\\"\\'\""));
    
    // incorrect escaping
    try {
      evaluateString("'\\a\\b'");
      fail("Didn't detect improper string escaping");
    } catch (Exception e) {
      //ok
    }
    try {
      evaluateString("\"\\a\\b\"");
      fail("Didn't detect improper string escaping");
    } catch (Exception e) {
      //ok
    }

    // parser respects brackets:
    assertEquals(4.0,
        evaluateDouble("8.0/(4.0/2.0)"));
    assertEquals(6.0,
        evaluateDouble("8.0 - (4.0 - 2.0)"));
    
    // parser can handle brackets
    evaluateDouble("((((((((((((1))))))))))))");
    evaluateDouble("((((((((((((1)))))))))))) + ((((((((((((1))))))))))))");
    evaluateDouble("((((((((((((1) + 1) + 1) + 1) + 1) + 1) + 1) + 1) + 1) + 1) + 1) + 1) + 1");

    // parser can handle many operators in a row
    evaluateDouble("1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1");
    evaluateDouble("1 - 1 - 1 - 1 - 1 - 1 - 1 - 1 - 1 - 1 - 1 - 1 - 1");
    evaluateDouble("1 * 1 * 1 * 1 * 1 * 1 * 1 * 1 * 1 * 1 * 1 * 1 * 1");
    evaluateDouble("1 / 1 / 1 / 1 / 1 / 1 / 1 / 1 / 1 / 1 / 1 / 1 / 1");
    evaluateDouble("1 ^ 1 ^ 1 ^ 1 ^ 1 ^ 1 ^ 1 ^ 1 ^ 1 ^ 1 ^ 1 ^ 1 ^ 1");
     
    // unary operators
    evaluateDouble("--------1");
    evaluateDouble("1--------1");
    evaluateDouble("++++++++1");
    evaluateDouble("1++++++++1");
    evaluateDouble("-+-+-+-+1");
    evaluateDouble("1-+-+-+-+1");
    evaluateBoolean("not not not not not not true");
    evaluateBoolean("! ! ! ! ! true");
    evaluateBoolean("!!!!!true");
    evaluateBoolean("not ! not ! not true");
    evaluateBoolean("not!not!not true");

    // float parsing
    try {
      assertEquals(1.0, evaluateDouble("1"));
    } catch (Exception e) {
      fail("Parsing failure!");
    }
    try {
      assertEquals(1.0, evaluateDouble("1.0"));
    } catch (Exception e) {
      fail("Parsing failure!");
    }
    try {
      assertEquals(1.0, evaluateDouble("1.0000000000000000000000000000000"));
    } catch (Exception e) {
      fail("Parsing failure!");
    }
    try {
      assertEquals(0.0, evaluateDouble("0"));
    } catch (Exception e) {
      fail("Parsing failure!");
    }
    try {
      assertEquals(0.0, evaluateDouble("0.0"));
    } catch (Exception e) {
      fail("Parsing failure!");
    }
    try {
      assertEquals(0.0, evaluateDouble("0.00000000000000000000000000000000"));
    } catch (Exception e) {
      fail("Parsing failure!");
    }
    try {
      assertEquals(-1.0, evaluateDouble("-1"));
    } catch (Exception e) {
      fail("Parsing failure!");
    }
    try {
      assertEquals(-1.0, evaluateDouble("-1.0"));
    } catch (Exception e) {
      fail("Parsing failure!");
    }
    try {
      assertEquals(-1.0, evaluateDouble("-1.00000000000000000000000000000000"));
    } catch (Exception e) {
      fail("Parsing failure!");
    }
    try {
      // parser error
      evaluateDouble("1.0 & 5");
      fail("Failed to find syntax error!");
    } catch (Exception e) {
      // ok
    }
    try {
      // scanner error
      evaluateDouble("1.000a0");
      fail("Failed to find syntax error!");
    } catch (Exception e) {
      // ok
    }
 
  }
 
  /**
   * Tests variable for special names, different values, different types and 
   * usages in expressions
   * 
   * @throws Exception if something goes wrong (mostly compilation errors)
   */
  public void testVariables() throws Exception {

    SimpleVariableDeclarations vars = new SimpleVariableDeclarations();
    vars.addBoolean("someBool");
    vars.addDouble("someDouble");
    vars.addString("someString");
    vars.addString("__weird_NAME_0123456789__");
  
    DoubleExpression root = (DoubleExpression) Parser.parse(
        "someDouble + ifelse(someBool, 0.0, ifelse(someString + __weird_NAME_0123456789__ regexp 'asdf', 1.0, 2.0))",
        vars,
        new MacroDeclarationsCompositor(
            new IfElseMacro(), new MathFunctions()
            )
        );
  
    double[] values = { 0.0, 1.0, Double.NaN, Double.POSITIVE_INFINITY};
    
    vars.getInitializer().setBoolean("someBool", true);
    for (double value : values) {
      vars.getInitializer().setDouble("someDouble", value);
      assertEquals(value + 0.0, root.evaluate());
    }
    
    vars.getInitializer().setBoolean("someBool", false);
    for (double value : values) {
      vars.getInitializer().setDouble("someDouble", value);
  
      vars.getInitializer().setString("someString", "as");
      vars.getInitializer().setString("__weird_NAME_0123456789__", "df");
      assertEquals(value + 1.0, root.evaluate());
  
      vars.getInitializer().setString("someString", "clearly not matchin!");
      vars.getInitializer().setString("__weird_NAME_0123456789__", "and neither is this one");
      assertEquals(value + 2.0, root.evaluate());
    }
 
  }

  /**
   * Tests the plus operator
   * 
   * @throws Exception if something goes wrong (mostly compilation errors)
   */
  public void testPlusOperator() throws Exception {
    
    double[] values = {0.0,  1.0,  2.0,  3.0,  100.0,  1000.0,  1e8,  1e-8,  0.1,
                       -0.0, -1.0, -2.0, -3.0, -100.0, -1000.0, -1e8, -1e-8, -0.1};
    
    // Note: assertEquals(0.0, -0.0) fails, thus we're using asserTrue(... == ...)

    for (double left : values) {
      
      // neutral element:
      assertTrue(left == evaluateDouble(String.format("%.20f + 0.0", left)));
      assertTrue(left == evaluateDouble(String.format("0.0 + %.20f", left)));

      for (double right: values) {
        // basics
        assertTrue(left + right == evaluateDouble(String.format("%.20f + %.20f", left, right)));
        
        // commutativity
        assertTrue(evaluateDouble(String.format("%.20f + %.20f", left, right)) == 
                   evaluateDouble(String.format("%.20f + %.20f", right, left)));
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
              evaluateDouble(String.format(
                  "%.20f + %.20f + %.20f", first, second, third))
              == 
              evaluateDouble(String.format(
                  "(%.20f + %.20f) + %.20f", first, second, third))
              );

          // right associativity
          assertTrue(
              evaluateDouble(String.format(
                  "%.20f + %.20f + %.20f", first, second, third))
              == 
              evaluateDouble(String.format(
                  "%.20f + (%.20f + %.20f)", first, second, third))
              );
        }
      }
    }

    // basic double semantics
    // NaN + v == NaN
    assertTrue(Double.isNaN(evaluateDouble("sqrt(-1) + 1.0")));
    // NaN + NaN == NaN
    assertTrue(Double.isNaN(evaluateDouble("sqrt(-1) + sqrt(-1.0)")));
    // overflow -> +inf
    assertTrue(Double.isInfinite(evaluateDouble(String.format(
        "%.20f + %.20f", Double.MAX_VALUE, Double.MAX_VALUE))));
    // inf + (-inf) == NaN
    assertTrue(Double.isNaN(evaluateDouble("1/0 + -1/0")));
    // inf + inf == inf 
    assertTrue(Double.isInfinite(evaluateDouble("1/0 + 1/0")));
    
  }

  /**
   * Tests string concatenation (special case of plus operator)
   * 
   * @throws Exception if something goes wrong (mostly compilation errors)
   */
  public void testStringConcatenation() throws Exception {
    
    assertEquals("asdf", evaluateString("'asdf'"));
    assertEquals("asdf", evaluateString("\"asdf\""));
    assertEquals("asdf", evaluateString("'as' + 'df'"));
    assertEquals("asdf", evaluateString("\"as\" + \"df\""));
    assertEquals("asdf", evaluateString("'as' + \"df\""));
    assertEquals("asdf", evaluateString("\"as\" + 'df'"));
    assertEquals("Hello World from WEKA :)",
        evaluateString("'Hello' + \" W\" + \"o\" + ('r' + (\"l\" + (('d'))) + \" from\")+' WEKA :)'"));
    
  }

  /**
   * Tests the minus operator
   * 
   * @throws Exception if something goes wrong (mostly compilation errors)
   */
  public void testMinusOperator() throws Exception {
    
    double[] values = {0.0,  1.0,  2.0,  3.0,  100.0,  1000.0,  1e8,  1e-8,  0.1,
                      -0.0, -1.0, -2.0, -3.0, -100.0, -1000.0, -1e8, -1e-8, -0.1};
    
    // Note: assertEquals(0.0, -0.0) fails, thus we're using asserTrue(... == ...)

    for (double left : values) {
      
      // neutral element:
      assertTrue(left == evaluateDouble(String.format("%.20f - 0.0", left)));

      for (double right: values) {
        // basics
        assertTrue(left - right == evaluateDouble(String.format("%.20f - %.20f", left, right)));
      }
    }

    double[] safe_values = {0.0, 1.0, 2.0, -0.0, -1.0, -2.0};
    for (double first : safe_values) {
      for (double second : safe_values) {
        for (double third : safe_values) {
          // left associativity
          assertTrue(
              evaluateDouble(String.format(
                  "%.20f - %.20f - %.20f", first, second, third))
              == 
              evaluateDouble(String.format(
                  "(%.20f - %.20f) - %.20f", first, second, third))
              );
        }
      }
    }

    // basic double semantics
    // NaN - v == NaN
    assertTrue(Double.isNaN(evaluateDouble("sqrt(-1) - 1.0")));
    // NaN - NaN == NaN
    assertTrue(Double.isNaN(evaluateDouble("sqrt(-1) - sqrt(-1.0)")));
    // underflow -> -inf
    assertTrue(Double.isInfinite(evaluateDouble(String.format(
        "%.20f - %.20f", -Double.MAX_VALUE, Double.MAX_VALUE))));
    // inf - inf == NaN
    assertTrue(Double.isNaN(evaluateDouble("1/0 - 1/0")));
    
  }

  /**
   * Tests the times operator
   * 
   * @throws Exception if something goes wrong (mostly compilation errors)
   */
  public void testTimesOperator() throws Exception {
    
    double[] values = {0.0,  1.0,  2.0,  3.0,  100.0,  1000.0,  1e8,  1e-8,  0.1,
                       -0.0, -1.0, -2.0, -3.0, -100.0, -1000.0, -1e8, -1e-8, -0.1};
    
    // Note: assertEquals(0.0, -0.0) fails, thus we're using asserTrue(... == ...)

    for (double left : values) {
      
      // neutral element:
      assertTrue(left == evaluateDouble(String.format("%.20f * 1.0", left)));
      assertTrue(left == evaluateDouble(String.format("1.0 * %.20f", left)));

      for (double right: values) {
        // basics
        assertTrue(left * right == evaluateDouble(String.format("%.20f * %.20f", left, right)));
        
        // commutativity
        assertTrue(evaluateDouble(String.format("%.20f * %.20f", left, right)) == 
                   evaluateDouble(String.format("%.20f * %.20f", right, left)));
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
              evaluateDouble(String.format(
                  "%.20f * %.20f * %.20f", first, second, third))
              == 
              evaluateDouble(String.format(
                  "(%.20f * %.20f) * %.20f", first, second, third))
              );

          // right associativity
          assertTrue(
              evaluateDouble(String.format(
                  "%.20f * %.20f * %.20f", first, second, third))
              == 
              evaluateDouble(String.format(
                  "%.20f * (%.20f * %.20f)", first, second, third))
              );
        }
      }
    }

    // basic double semantics
    // NaN * v == NaN
    assertTrue(Double.isNaN(evaluateDouble("sqrt(-1) * 1.0")));
    // NaN * NaN == NaN
    assertTrue(Double.isNaN(evaluateDouble("sqrt(-1) * sqrt(-1.0)")));
    // overflow -> +inf
    assertTrue(Double.isInfinite(evaluateDouble(String.format(
        "%.20f * %.20f", Double.MAX_VALUE, Double.MAX_VALUE))));
    // inf + (-inf) == -inf 
    assertTrue(Double.isInfinite(evaluateDouble("(1/0) * (-1/0)")));
    // inf * inf == inf 
    assertTrue(Double.isInfinite(evaluateDouble("(1/0) * (1/0)")));
    
  }

  /**
   * Tests the division operator
   * 
   * @throws Exception if something goes wrong (mostly compilation errors)
   */
  public void testDivisionOperator() throws Exception {
    
    double[] values = {0.0,  1.0,  2.0,  3.0,  100.0,  1000.0,  1e8,  1e-8,  0.1,
                       -0.0, -1.0, -2.0, -3.0, -100.0, -1000.0, -1e8, -1e-8, -0.1};
    
    // Note: assertEquals(0.0, -0.0) fails, thus we're using asserTrue(... == ...)

    for (double left : values) {
      
      // neutral element:
      assertTrue(left == evaluateDouble(String.format("%.20f / 1.0", left)));

      for (double right: values) {
        // basics
        if (left == 0.0 && right == 0.0)
          continue; // NaN == NaN always evaluates to false
        assertTrue(left / right == evaluateDouble(String.format("%.20f / %.20f", left, right)));
      }
    }

    double[] safe_values = {0.0, 1.0, 2.0, -0.0, -1.0, -2.0};
    for (double first : safe_values) {
      for (double second : safe_values) {
        for (double third : safe_values) {
          if (first == 0.0 && (second == 0.0 || third == 0.0))
            continue; // NaN == NaN always evaluates to false
          // left associativity
          try {
          assertTrue(
              evaluateDouble(String.format(
                  "%.20f / %.20f / %.20f", first, second, third))
              == 
              evaluateDouble(String.format(
                  "(%.20f / %.20f) / %.20f", first, second, third))
              );
          } catch (AssertionFailedError e) {
            System.out.println("failed");
          assertTrue(
              evaluateDouble(String.format(
                  "%.20f / %.20f / %.20f", first, second, third))
              == 
              evaluateDouble(String.format(
                  "(%.20f / %.20f) / %.20f", first, second, third))
              );
          }
        }
      }
    }

    // basic double semantics
    // NaN / v == NaN
    assertTrue(Double.isNaN(evaluateDouble("sqrt(-1) / 1.0")));
    // NaN / NaN == NaN
    assertTrue(Double.isNaN(evaluateDouble("sqrt(-1) / sqrt(-1.0)")));
    // overflow -> +inf
    assertTrue(Double.isInfinite(evaluateDouble(String.format(
        "%.20f + %.20f", Double.MAX_VALUE, Double.MAX_VALUE))));
    // inf / inf == NaN 
    assertTrue(Double.isNaN(evaluateDouble("(1/0) / (1/0)")));
    
  }
  
  /**
   * Tests the power operator
   * 
   * @throws Exception if something goes wrong (mostly compilation errors)
   */
  public void testPowOperator() throws Exception {
    
    double[] values = {0.0,  1.0,  2.0,  3.0,  100.0,  1000.0,  1e8,  1e-8,  0.1,
                       -0.0, -1.0, -2.0, -3.0, -100.0, -1000.0, -1e8, -1e-8, -0.1};
    
    // Note: assertEquals(0.0, -0.0) fails, thus we're using asserTrue(... == ...)

    for (double left : values) {
      
      // neutral element:
      assertTrue(left == evaluateDouble(String.format("%.20f ^ 1.0", left)));
      assertTrue(1.0 == evaluateDouble(String.format("1.0 ^ %.20f", left)));

      for (double right: values) {
        // basics
        if (!Double.isNaN(Math.pow(left, right)))
          assertTrue(Math.pow(left, right) == evaluateDouble(String.format("(%.20f) ^ (%.20f)", left, right)));
      }
    }

    double[] safe_values = {0.0, 1.0, 2.0, -0.0, -1.0, -2.0};
    for (double first : safe_values) {
      for (double second : safe_values) {
        for (double third : safe_values) {
      
          // right associativity
          if (!Double.isNaN(Math.pow(first, Math.pow(second, third))))
            assertTrue(
                evaluateDouble(String.format(
                    "(%.20f) ^ (%.20f) ^ (%.20f)", first, second, third))
                    == 
                    evaluateDouble(String.format(
                        "(%.20f) ^ ((%.20f) ^ (%.20f))", first, second, third))
                );
        }
      }
    }

    // basic double semantics
    // NaN ^ v == NaN
    assertTrue(Double.isNaN(evaluateDouble("sqrt(-1) ^ 1.0")));
    // v ^ NaN == NaN
    assertTrue(Double.isNaN(evaluateDouble("1.0 ^ sqrt(-1)")));
    // NaN ^ NaN == NaN
    assertTrue(Double.isNaN(evaluateDouble("sqrt(-1) ^ sqrt(-1.0)")));
    // overflow -> ^inf
    assertTrue(Double.isInfinite(evaluateDouble(String.format(
        "%.20f ^ 2", Double.MAX_VALUE))));
    // inf ^ (-inf) == NaN
    assertTrue(Double.isNaN(evaluateDouble("1/0 ^ -1/0")));
    // inf ^ inf == inf 
    assertTrue(Double.isInfinite(evaluateDouble("1/0 + 1/0")));
    

  }
   
  
  /**
   * Tests the logical and operator
   * 
   * @throws Exception if something goes wrong (mostly compilation errors)
   */
  public void testAndOperator() throws Exception {
    
    boolean[] values = {true, false};
    for (boolean left : values) {
      for (boolean right : values) {
        // basics
        assertEquals(
            left && right,
            evaluateBoolean(left + " and " + right)
            );
        assertEquals(
            left && right,
            evaluateBoolean(left + " & " + right)
            );
        
        // commutativity
        assertEquals(
            evaluateBoolean(right + " and " + left),
            evaluateBoolean(left + " and " + right)
            );
        assertEquals(
            evaluateBoolean(right + " & " + left),
            evaluateBoolean(left + " & " + right)
            );
        
        for (boolean third : values) {
          // due to commutativity the operator is both left and right associative
          // so we test both
          
          // left associativity
          assertEquals(
              left && right && third,
              evaluateBoolean("(" + left + " and " + right + ") and " + third)
              );
          assertEquals(
              left && right && third,
              evaluateBoolean("(" + left + " & " + right + ") & " + third)
              );
  
          // right associativity
          assertEquals(
              left && right && third,
              evaluateBoolean(left + " and (" + right + " and " + third + ")")
              );
          assertEquals(
              left && right && third,
              evaluateBoolean(left + " & (" + right + " & " + third + ")")
              );
          
          // interchangeability
          assertEquals(
              left && right && third,
              evaluateBoolean(left + " & " + right + " and " + third)
              );
  
        }
      }
    }
  }
  
  /**
   * Tests the logical or operator
   * 
   * @throws Exception if something goes wrong (mostly compilation errors)
   */
  public void testOrOperator() throws Exception {
    
    boolean[] values = {true, false};
    for (boolean left : values) {
      for (boolean right : values) {
        // basics
        assertEquals(
            left || right,
            evaluateBoolean(left + " or " + right)
            );
        assertEquals(
            left || right,
            evaluateBoolean(left + " | " + right)
            );
        
        // commutativity
        assertEquals(
            evaluateBoolean(right + " or " + left),
            evaluateBoolean(left + " or " + right)
            );
        assertEquals(
            evaluateBoolean(right + " | " + left),
            evaluateBoolean(left + " | " + right)
            );
        
        for (boolean third : values) {
          // due to commutativity the operator is both left and right associative
          // so we test both
          
          // left associativity
          assertEquals(
              left || right || third,
              evaluateBoolean("(" + left + " or " + right + ") or " + third)
              );
          assertEquals(
              left || right || third,
              evaluateBoolean("(" + left + " | " + right + ") | " + third)
              );
  
          // right associativity
          assertEquals(
              left || right || third,
              evaluateBoolean(left + " or (" + right + " or " + third + ")")
              );
          assertEquals(
              left || right || third,
              evaluateBoolean(left + " | (" + right + " | " + third + ")")
              );
          
          // interchangeability
          assertEquals(
              left || right || third,
              evaluateBoolean(left + " or " + right + " | " + third)
              );
  
        }
      }
    }
  }

  /**
   * Tests the comparison equality operator
   * 
   * @throws Exception if something goes wrong (mostly compilation errors)
   */
  public void testEqualsOperator() throws Exception {
    
    double[] values = {0.0,  1.0,  2.0,  3.0,  100.0,  1000.0,  1e8,  1e-8,  0.1,
                       -0.0, -1.0, -2.0, -3.0, -100.0, -1000.0, -1e8, -1e-8, -0.1};
    
  
    for (double left : values) {
      
      // reflexivity
      assertTrue(evaluateBoolean(String.format("%.20f = %.20f", left, left)));
      
      for (double right: values) {
        // basics
        assertEquals(
            left == right,
            evaluateBoolean(String.format(
                "%.20f = %.20f", left, right))
            );
        
        // commutativity
        assertEquals(
            evaluateBoolean(String.format(
                "%.20f = %.20f", left, right)),
            evaluateBoolean(String.format(
                "%.20f = %.20f", right, left))
            );
      }
    }
  
    // non-associativity
    try {
      evaluateBoolean("1.0 == 1.0 == 1.0");
      fail("non-associativity!");
    } catch (Exception e) {
      // ok
    }
  
    // basic double semantics
    // inf = inf -> true
    assertTrue(evaluateBoolean("1/0 = 1/0"));
    // -inf = inf -> false
    assertFalse(evaluateBoolean("-1/0 = 1/0"));
    // NaN = NaN -> false
    assertFalse(evaluateBoolean("sqrt(-1) = sqrt(-1)"));
    // 0 = NaN -> false
    assertFalse(evaluateBoolean("0.0 = sqrt(-1)"));
    // inf = NaN -> false
    assertFalse(evaluateBoolean("1/0 = sqrt(-1)"));
    
  }

  /**
   * Tests the comparison less than, less equals, greater than & greater equals
   * operators
   * 
   * @throws Exception if something goes wrong (mostly compilation errors)
   */
  public void testLtLeGtGeOperator() throws Exception {
  
    double[] values = {0.0,  1.0,  2.0,  3.0,  100.0,  1000.0,  1e8,  1e-8,  0.1,
                       -0.0, -1.0, -2.0, -3.0, -100.0, -1000.0, -1e8, -1e-8, -0.1};
    
    for (double left : values) {
      
      assertFalse(evaluateBoolean(String.format("%.20f < %.20f", left, left)));
      assertTrue(evaluateBoolean(String.format("%.20f <= %.20f", left, left)));
      assertFalse(evaluateBoolean(String.format("%.20f > %.20f", left, left)));
      assertTrue(evaluateBoolean(String.format("%.20f >= %.20f", left, left)));
      
      for (double right: values) {
        // basics
        assertEquals(
            left < right,
            evaluateBoolean(String.format("%.20f < %.20f", left, right))
            );
        assertEquals(
            left <= right,
            evaluateBoolean(String.format("%.20f <= %.20f", left, right))
            );
        assertEquals(
            left > right,
            evaluateBoolean(String.format("%.20f > %.20f", left, right))
            );
        assertEquals(
            left >= right,
            evaluateBoolean(String.format("%.20f >= %.20f", left, right))
            );
      }
    }
  
    // non-associativity
    try {
      evaluateBoolean("1.0 < 1.0 < 1.0");
      fail("non-associativity!");
    } catch (Exception e) {
      // ok
    }
    try {
      evaluateBoolean("1.0 <= 1.0 <= 1.0");
      fail("non-associativity!");
    } catch (Exception e) {
      // ok
    }
    try {
      evaluateBoolean("1.0 > 1.0 > 1.0");
      fail("non-associativity!");
    } catch (Exception e) {
      // ok
    }
    try {
      evaluateBoolean("1.0 >= 1.0 >= 1.0");
      fail("non-associativity!");
    } catch (Exception e) {
      // ok
    }
  
    // basic double semantics
    // inf < inf -> false
    assertFalse(evaluateBoolean("1/0 < 1/0"));
    // inf <= inf -> true
    assertTrue(evaluateBoolean("1/0 <= 1/0"));
    // inf > inf -> false
    assertFalse(evaluateBoolean("1/0 > 1/0"));
    // inf >= inf -> true
    assertTrue(evaluateBoolean("1/0 >= 1/0"));
  
    // -inf < inf -> true
    assertTrue(evaluateBoolean("-1/0 < 1/0"));
    // -inf <= inf -> true
    assertTrue(evaluateBoolean("-1/0 <= 1/0"));
    // -inf > inf -> false
    assertFalse(evaluateBoolean("-1/0 > 1/0"));
    // -inf >= inf -> false
    assertFalse(evaluateBoolean("-1/0 >= 1/0"));
  
    // NaN < NaN -> false
    assertFalse(evaluateBoolean("sqrt(-1) < sqrt(-1)"));
    // NaN <= NaN -> false
    assertFalse(evaluateBoolean("sqrt(-1) <= sqrt(-1)"));
    // NaN > NaN -> false
    assertFalse(evaluateBoolean("sqrt(-1) > sqrt(-1)"));
    // NaN >= NaN -> false
    assertFalse(evaluateBoolean("sqrt(-1) >= sqrt(-1)"));
  
    // 0 < NaN -> false
    assertFalse(evaluateBoolean("0.0 < sqrt(-1)"));
    // 0 <= NaN -> false
    assertFalse(evaluateBoolean("0.0 <= sqrt(-1)"));
    // 0 > NaN -> false
    assertFalse(evaluateBoolean("0.0 > sqrt(-1)"));
    // 0 >= NaN -> false
    assertFalse(evaluateBoolean("0.0 >= sqrt(-1)"));
  
    // inf < NaN -> false
    assertFalse(evaluateBoolean("1/0 < sqrt(-1)"));
    // inf <= NaN -> false
    assertFalse(evaluateBoolean("1/0 <= sqrt(-1)"));
    // inf > NaN -> false
    assertFalse(evaluateBoolean("1/0 > sqrt(-1)"));
    // inf >= NaN -> false
    assertFalse(evaluateBoolean("1/0 >= sqrt(-1)"));
    
  }

  /**
   * Tests the precedence of different operators over other operators
   * 
   * @throws Exception if something goes wrong (mostly compilation errors)
   */
  public void testPrecedence() throws Exception {

    // ^ has higher precedence than *, /
    // bug in AttributeExpression
    assertEquals(32.0, evaluateDouble("8*2^2"));
    assertEquals(2.0, evaluateDouble("8/2^2"));
    
    // ^ has higher precedence than unary -
    assertEquals(-1.0, evaluateDouble("-1^2"));

    // *, / have higher precedence than +, -
    assertEquals(10.0,evaluateDouble("2.0*3.0 + 4.0"));
    assertEquals(2.0,evaluateDouble("2.0*3.0 - 4.0"));
    assertEquals(-0.5,evaluateDouble("2.0/4.0 - 1.0"));
    assertEquals(1.5,evaluateDouble("2.0/4.0 + 1.0"));

    // unary - has higher precedence than +, -
    assertEquals(1.0,evaluateDouble("-1.0 + 2.0"));
    assertEquals(-3.0,evaluateDouble("-1.0 - 2.0"));
    // due to the associativity of *, / we don't have to test for unary - over *, /
    
    // +, -, /, * have higher precedence than =, <=, <, >, >=
    // if not, then the program will fail due to a type error
    assertTrue(evaluateBoolean("1.0 + 2.0 >= 2.5"));
    assertFalse(evaluateBoolean("-2.0 - 2.0 > -2.5"));
    assertFalse(evaluateBoolean("2.0 * 2.0 <= 2.5"));
    assertFalse(evaluateBoolean("1.0 / 0.5 < 1.0"));
    assertTrue(evaluateBoolean("1.0 + 2.0 = 3.0"));
  
    // =, <=, <, >, >= have higher precedence than &&, ||
    // if not, then the program will fail due to a type error
    assertTrue(evaluateBoolean("1.0 < 2.0 and true"));
    assertTrue(evaluateBoolean("1.0 <= 2.0 or false"));
    assertFalse(evaluateBoolean("1.0 > 2.0 and true"));
    assertFalse(evaluateBoolean("1.0 >= 2.0 or false"));
    assertFalse(evaluateBoolean("1.0 = 2.0 and true"));
    assertTrue(evaluateBoolean("1.0 < 2.0 & true"));
    assertTrue(evaluateBoolean("1.0 <= 2.0 | false"));
    assertFalse(evaluateBoolean("1.0 > 2.0 & true"));
    assertFalse(evaluateBoolean("1.0 >= 2.0 | false"));
    assertFalse(evaluateBoolean("1.0 = 2.0 & true"));
    
    // ! has higher precedence than &&
    assertFalse(evaluateBoolean("not true and false"));
    assertFalse(evaluateBoolean("not false and false"));
    assertFalse(evaluateBoolean("! true & false"));
    assertFalse(evaluateBoolean("! false & false"));

    // ! has higher precedence than ||
    assertTrue(evaluateBoolean("not true or true"));
    assertTrue(evaluateBoolean("not false or true"));
    assertTrue(evaluateBoolean("! true | true"));
    assertTrue(evaluateBoolean("! false | true"));

    // && has higher precedence than ||
    assertTrue(evaluateBoolean("false and false or true"));
    assertTrue(evaluateBoolean("false and true or true"));
    assertTrue(evaluateBoolean("false & false | true"));
    assertTrue(evaluateBoolean("false & true | true"));

  }

  /**
   * Tests the composition of different language features
   * 
   * @throws Exception if something goes wrong (mostly compilation errors)
   */
  public void testCompositions() throws Exception {

    String[] binaryDoubleOperators = {"+", "-", "*", "/"};
    String[] comparisonOperators = {"=", "<", ">="};
    String[] booleanBinaryOperators = {"&", "|", "and", "or"};
    
    for (String booleanBinaryOperator : booleanBinaryOperators) {
      for (String comparisonOperator : comparisonOperators) {
        for (String binaryDoubleOperator : binaryDoubleOperators) {
          evaluateDouble(
              String.format("ifelse(exp(sin(A) %s B) %s C %s true, tan(A), rint(C) + 3.0)"
                  + " + java('java.lang.Math', 'double nextUp(double)', C^3)",
                  binaryDoubleOperator, comparisonOperator, booleanBinaryOperator),
              11.0, 43.0, 89.0
              );
        }
      }
    }
    
  }
 
  /**
   * Tests the mathematical functions exposed by the MathFunction
   * MacroDeclarations
   * 
   * @throws Exception if something goes wrong (mostly compilation errors)
   */
  public void testMathFunctions() throws Exception {
    
    for (double v : new double[]{1.0, 2.0, 100.0, -1.0, -2.0, -100.0, 0.0}) {
      assertEquals(Math.abs(v), evaluateDouble("abs(A)", v));
      assertEquals(Math.log(v), evaluateDouble("log(A)", v));
      assertEquals(Math.exp(v), evaluateDouble("exp(A)", v));
      assertEquals(Math.sin(v), evaluateDouble("sin(A)", v));
      assertEquals(Math.cos(v), evaluateDouble("cos(A)", v));
      assertEquals(Math.tan(v), evaluateDouble("tan(A)", v));
      assertEquals(Math.sqrt(v), evaluateDouble("sqrt(A)", v));
      assertEquals(Math.floor(v), evaluateDouble("floor(A)", v));
      assertEquals(Math.ceil(v), evaluateDouble("ceil(A)", v));
      assertEquals(Math.rint(v), evaluateDouble("rint(A)", v));
      assertEquals(Math.pow(v, v), evaluateDouble("pow(A, A)", v));
    }
  }

  /**
   * Tests calling java code through the java macro
   * 
   * @throws Exception if something goes wrong (mostly compilation errors)
   */
  public void testCallingJavaFunctions() throws Exception {
    
    double[] values = {0.0, -1.0, 1.0, Double.NaN, Double.POSITIVE_INFINITY,
        100.0};

    for  (double value : values) {
      assertEquals(
          Math.sqrt(value),
          evaluateDouble("java('java.lang.Math', 'double sqrt(double)', A)", value)
          );
      assertEquals(
          String.valueOf(value),
          evaluateString("java('java.lang.String', 'String valueOf(double)', A)", value)
          );
      assertEquals(
          badRandom(value, value*value, 1/value),
          evaluateDouble("java('weka.core.expressionlanguage.ExpressionLanguageTest', 'double badRandom(double, double, double)', A, A^2, 1/A)", value)
          );
    }

    assertEquals(
        Thread.interrupted(),
        evaluateBoolean("java('java.lang.Thread', 'boolean interrupted()')")
        );
  }
  
  /**
   * Creates a (deterministic) pseudo random number through the seed of three
   * different random numbers.</p>
   * 
   * This is used to test calling java code in the
   * testCallingJavaFunctions() test.</p>
   * 
   * Warning: This function should not be used other than for highly unimportant
   * functionality.
   * 
   * @param seed1 first seed value
   * @param seed2 second seed value
   * @param seed3 third seed value
   * @return deterministic pseudo random number
   */
  public static double badRandom(double seed1, double seed2, double seed3) {
    return new Double(Double.longBitsToDouble(
        Double.doubleToLongBits(seed1) ^ Double.doubleToLongBits(seed2) ^ Double.doubleToLongBits(seed3)
        )).hashCode();
  }
  
  /**
   * Returns the test suite.
   * 
   * @return the test suite
   */
  public static Test suite() {
    return new TestSuite(ExpressionLanguageTest.class);
  }
  
  /**
   * Executes the test from command-line.
   * 
   * @param args ignored
   */
  public static void main(String[] args) {
    TestRunner.run(suite());
  }
}
