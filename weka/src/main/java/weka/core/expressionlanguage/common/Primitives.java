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
 *    Primitives.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.expressionlanguage.common;

import weka.core.expressionlanguage.core.Node;

import java.io.Serializable;

/**
 * A class providing AST (abstract syntax tree) nodes to support primitive types.
 * 
 * @author Benjamin Weber ( benweber at student dot ethz dot ch )
 * @version $Revision: 1000 $
 */
public class Primitives implements Serializable {

  private static final long serialVersionUID = -6356635298310530223L;
  
  /*
   * Currently the different expressions of different primitive types are all
   * evaluated through an 'evaluate()' method. This has significant impact on the
   * typing system.
   * 
   * Due to java's inheritance a node can't implement two primitive types at the
   * same time. Thus each node can only ever be of exactly at most one primitive
   * type.
   * 
   * It would be possible to refactor the method names to allow implementing
   * different primitive expression.
   * 
   * However this will make things much more complicated.
   * 
   * For example the ifelse macro will check what primitive expression is 
   * implemented in its parameters. Knowing that each parameter will only ever
   * implement one primitive type it's easy to determine whether the ifelse
   * macro can be evaluated.
   * If parameters could implement two types simultaneously it wouldn't be clear
   * which one should be chosen/preferred.
   */
 
  /**
   * An AST node for an expression of boolean type
   * 
   * @author Benjamin Weber ( benweber at student dot ethz dot ch )
   * @version $Revision: 1000 $
   */
  public static interface BooleanExpression extends Node {
    public boolean evaluate();
  }

  /**
   * An AST node for an expression of double type
   * 
   * @author Benjamin Weber ( benweber at student dot ethz dot ch )
   * @version $Revision: 1000 $
   */
  public static interface DoubleExpression extends Node {
    public double evaluate();
  }
  
  /**
   * An AST node for an expression of String type
   * 
   * @author Benjamin Weber ( benweber at student dot ethz dot ch )
   * @version $Revision: 1000 $
   */
  public static interface StringExpression extends Node {
    public String evaluate();
  }

  /**
   * An AST node representing a boolean constant
   * 
   * @author Benjamin Weber ( benweber at student dot ethz dot ch )
   * @version $Revision: 1000 $
   */
  public static class BooleanConstant implements BooleanExpression, Serializable {

    private static final long serialVersionUID = -7104666336890622673L;

    private final boolean value;
    
    public BooleanConstant(boolean value) {
      this.value = value;
    }
    
    @Override
    public boolean evaluate() {
      return value;
    }
  }

  /**
   * An AST node representing a double constant
   * 
   * @author Benjamin Weber ( benweber at student dot ethz dot ch )
   * @version $Revision: 1000 $
   */
  public static class DoubleConstant implements DoubleExpression, Serializable {

    private static final long serialVersionUID = 6876724986473710563L;

    private final double value;

    public DoubleConstant(double value) {
      this.value = value;
    }

    @Override
    public double evaluate() {
      return value;
    }

  }
 
  /**
   * An AST node representing a string constant
   * 
   * @author Benjamin Weber ( benweber at student dot ethz dot ch )
   * @version $Revision: 1000 $
   */
  public static class StringConstant implements StringExpression, Serializable {

    private static final long serialVersionUID = 491766938196527684L;

    private final String value;
    
    public StringConstant(String value) {
      this.value = value;
    }

    @Override
    public String evaluate() {
      return value;
    }
  }

  /**
   * An AST node representing a boolean variable
   * 
   * @author Benjamin Weber ( benweber at student dot ethz dot ch )
   * @version $Revision: 1000 $
   */
  public static class BooleanVariable implements BooleanExpression, Serializable {

    private static final long serialVersionUID = 6041670101306161521L;

    private boolean value;
    private final String name;
    
    public BooleanVariable(String name) {
      this.name = name;
    }
    
    @Override
    public boolean evaluate() {
      return value;
    }
    
    public String getName() {
      return name;
    }
    
    public boolean getValue() {
      return value;
    }
    
    public void setValue(boolean value) {
      this.value = value;
    }
  }

  /**
   * An AST node representing a double variable
   * 
   * @author Benjamin Weber ( benweber at student dot ethz dot ch )
   * @version $Revision: 1000 $
   */
  public static class DoubleVariable implements DoubleExpression, Serializable {

    private static final long serialVersionUID = -6059066803856814750L;

    private double value;
    private final String name;
    
    public DoubleVariable(String name) {
      this.name = name;
    }
    
    @Override
    public double evaluate() {
      return this.value;
    }
    
    public String getName() {
      return name;
    }

    public double getValue() {
      return value;
    }

    public void setValue(double value) {
      this.value = value;
    }
  }
  
  /**
   * An AST node representing a string variable
   * 
   * @author Benjamin Weber ( benweber at student dot ethz dot ch )
   * @version $Revision: 1000 $
   */
  public static class StringVariable implements StringExpression, Serializable {

    private static final long serialVersionUID = -7332982581964981085L;

    private final String name;
    private String value;

    public StringVariable(String name) {
      this.name = name;
    }

    @Override
    public String evaluate() {
      return value;
    }

    public String getName() {
      return name;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

  }

}
