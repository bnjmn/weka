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
 *    IfElseMacro.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.expressionlanguage.common;

import weka.core.expressionlanguage.core.Node;
import weka.core.expressionlanguage.core.Macro;
import weka.core.expressionlanguage.core.MacroDeclarations;
import weka.core.expressionlanguage.core.SemanticException;
import weka.core.expressionlanguage.common.Primitives.BooleanExpression;
import weka.core.expressionlanguage.common.Primitives.DoubleExpression;
import weka.core.expressionlanguage.common.Primitives.StringExpression;

/**
 * A macro declaration exposing the <code>ifelse</code> function.</p>
 * 
 * The ifelse macro can be used as follows:</br>
 * <code>ifelse(condition, ifpart, elsepart)</code></p>
 * 
 * Whith the following constraints:</br>
 * <ul>
 * <li>condition must be of boolean type</li>
 * <li>ifpart and elsepart must be of the same type</li>
 * <li>ifpart must be either of boolean, double or string type</li>
 * </ul>
 * 
 * Examples:</br>
 * <ul>
 * <li><code>ifelse(A &lt; B, true, A = B)</code></li>
 * <li><code>ifelse(A = B, 4.0^2, 1/5)</code></li>
 * <li><code>ifelse(A &gt; B, 'bigger', 'smaller')</code></li>
 * </ul>
 * 
 * @author Benjamin Weber ( benweber at student dot ethz dot ch )
 * @version $Revision: 1000 $
 */
public class IfElseMacro implements MacroDeclarations, Macro {
  
  private final static String IF_ELSE = "ifelse";

  /**
   * Whether the macro is declared
   * 
   * @param name name of the macro
   * @return whether the macro is declared
   */
  @Override
  public boolean hasMacro(String name) {
    return IF_ELSE.equals(name);
  }

  /**
   * Tries to fetch the macro</p>
   * 
   * The same invariant of {@link MacroDeclarations} applies here too.
   * 
   * @param name name of the macro
   * @return a macro
   */
  @Override
  public Macro getMacro(String name) {
    if (IF_ELSE.equals(name))
      return this;
    throw new RuntimeException("Undefined Macro '" + name + "'!");
  }
  
  /**
   * Evaluates the ifelse macro
   * 
   * @param params the parameters
   * @return an AST (abstract syntax tree) node evaluating the ifelse function
   */
  @Override
  public Node evaluate(Node... params) throws SemanticException {
    if (params.length != 3)
      throw new SemanticException("ifelse takes exactly 3 arguments!");

    if (!(params[0] instanceof BooleanExpression))
      throw new SemanticException("ifelse's first parameter must be boolean!");

    if (params[1] instanceof BooleanExpression
        && params[2] instanceof BooleanExpression) {

      return new BooleanIfElse(
          (BooleanExpression) params[0],
          (BooleanExpression) params[1],
          (BooleanExpression) params[2]
          );
    } else if (params[1] instanceof DoubleExpression 
        && params[2] instanceof DoubleExpression) {

      return new DoubleIfElse(
          (BooleanExpression) params[0],
          (DoubleExpression) params[1],
          (DoubleExpression) params[2]
          );
    } else if (params[1] instanceof StringExpression 
        && params[2] instanceof StringExpression) {

      return new StringIfElse(
          (BooleanExpression) params[0],
          (StringExpression) params[1],
          (StringExpression) params[2]
          );
    }
    
    throw new SemanticException("ifelse's second and third parameter must be doubles, booleans or Strings!");
  }

  private static class DoubleIfElse implements DoubleExpression {

    private final BooleanExpression condition;
    private final DoubleExpression ifPart;
    private final DoubleExpression elsePart;
    
    public DoubleIfElse(BooleanExpression condition,
        DoubleExpression ifPart,
        DoubleExpression elsePart) {
      
      this.condition = condition;
      this.ifPart = ifPart;
      this.elsePart = elsePart;
    }
    
    @Override
    public double evaluate() {
      if (condition.evaluate())
        return ifPart.evaluate();
      return elsePart.evaluate();
    }
  }
 
  private static class BooleanIfElse implements BooleanExpression {

    private final BooleanExpression condition;
    private final BooleanExpression ifPart;
    private final BooleanExpression elsePart;
    
    public BooleanIfElse(BooleanExpression condition,
        BooleanExpression ifPart,
        BooleanExpression elsePart) {
      
      this.condition = condition;
      this.ifPart = ifPart;
      this.elsePart = elsePart;
    }
    
    @Override
    public boolean evaluate() {
      if (condition.evaluate())
        return ifPart.evaluate();
      return elsePart.evaluate();
    }
  }

  private static class StringIfElse implements StringExpression {

    private final BooleanExpression condition;
    private final StringExpression ifPart;
    private final StringExpression elsePart;
    
    public StringIfElse(BooleanExpression condition,
        StringExpression ifPart,
        StringExpression elsePart) {
      
      this.condition = condition;
      this.ifPart = ifPart;
      this.elsePart = elsePart;
    }
    
    @Override
    public String evaluate() {
      if (condition.evaluate())
        return ifPart.evaluate();
      return elsePart.evaluate();
    }
  }

}
