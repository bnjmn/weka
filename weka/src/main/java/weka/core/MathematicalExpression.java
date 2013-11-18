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
 *    MathematicalExpression.java
 *    Copyright (C) 2008-2012 University of Waikato, Hamilton, New Zealand
 */

package weka.core;

import java.io.ByteArrayInputStream;
import java.util.HashMap;

import java_cup.runtime.DefaultSymbolFactory;
import java_cup.runtime.SymbolFactory;
import weka.core.mathematicalexpression.Parser;
import weka.core.mathematicalexpression.Scanner;

/**
 * Class for evaluating a string adhering the following grammar:<br/>
 * 
 * <pre>
 * expr_list ::= expr_list expr_part | expr_part ;
 * expr_part ::= expr ;
 * expr      ::=   NUMBER
 *               | ( expr )
 *               | opexpr
 *               | varexpr
 *               | funcexpr
 *               ;
 * 
 * opexpr    ::=   expr + expr
 *               | expr - expr
 *               | expr * expr
 *               | expr / expr
 *               ;
 * 
 * varexpr  ::=  VARIABLE ;
 * 
 * funcexpr ::=    abs ( expr )
 *               | sqrt ( expr )
 *               | log ( expr )
 *               | exp ( expr )
 *               | sin ( expr )
 *               | cos ( expr )
 *               | tan ( expr )
 *               | rint ( expr )
 *               | floor ( expr )
 *               | pow ( expr , expr )
 *               | ceil ( expr )
 *               | ifelse ( boolexpr , expr (if true) , expr (if false) )
 *               ;
 * 
 * boolexpr ::=    BOOLEAN
 *               | true
 *               | false
 *               | expr &lt; expr
 *               | expr &lt;= expr
 *               | expr &gt; expr
 *               | expr &gt;= expr
 *               | expr = expr
 *               | ( boolexpr )
 *               | ! boolexpr
 *               | boolexpr & boolexpr
 *               | boolexpr | boolexpr
 *               ;
 * </pre>
 * 
 * Code example 1:
 * 
 * <pre>
 * String expr = &quot;pow(BASE,EXPONENT)*MULT&quot;;
 * HashMap symbols = new HashMap();
 * symbols.put(&quot;BASE&quot;, new Double(2));
 * symbols.put(&quot;EXPONENT&quot;, new Double(9));
 * symbols.put(&quot;MULT&quot;, new Double(0.1));
 * double result = MathematicalExpression.evaluate(expr, symbols);
 * System.out.println(expr + &quot; and &quot; + symbols + &quot; = &quot; + result);
 * </pre>
 * 
 * Code Example 2 (uses the "ifelse" construct):
 * 
 * <pre>
 * String expr = &quot;ifelse(I&lt;0,pow(BASE,I*0.5),pow(BASE,I))&quot;;
 * MathematicalExpression.TreeNode tree = MathematicalExpression.parse(expr);
 * HashMap symbols = new HashMap();
 * symbols.put(&quot;BASE&quot;, new Double(2));
 * for (int i = -10; i &lt;= 10; i++) {
 *   symbols.put(&quot;I&quot;, new Double(i));
 *   double result = MathematicalExpression.evaluate(expr, symbols);
 *   System.out.println(expr + &quot; and &quot; + symbols + &quot; = &quot; + result);
 * }
 * </pre>
 * 
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class MathematicalExpression implements RevisionHandler {

  /**
   * Parses and evaluates the given expression. Returns the result of the
   * mathematical expression, based on the given values of the symbols.
   * 
   * @param expr the expression to evaluate
   * @param symbols the symbol/value mapping
   * @return the evaluated result
   * @throws Exception if something goes wrong
   */
  @SuppressWarnings("deprecation")
  public static double evaluate(String expr, HashMap<String, Double> symbols)
    throws Exception {
    SymbolFactory sf;
    ByteArrayInputStream parserInput;
    Parser parser;

    sf = new DefaultSymbolFactory();
    parserInput = new ByteArrayInputStream(expr.getBytes());
    parser = new Parser(new Scanner(parserInput, sf), sf);
    parser.setSymbols(symbols);
    parser.parse();

    return parser.getResult();
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }
}
