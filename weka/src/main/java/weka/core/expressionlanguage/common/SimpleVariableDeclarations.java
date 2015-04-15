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
 *    SimpleVariableDeclarations.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.expressionlanguage.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import weka.core.expressionlanguage.core.Node;
import weka.core.expressionlanguage.core.VariableDeclarations;
import weka.core.expressionlanguage.common.Primitives.BooleanVariable;
import weka.core.expressionlanguage.common.Primitives.DoubleVariable;
import weka.core.expressionlanguage.common.Primitives.StringVariable;

/**
 * A set of customizable variable declarations for primitive types.</p>
 * 
 * @author Benjamin Weber ( benweber at student dot ethz dot ch )
 * @version $Revision: 1000 $
 */
public class SimpleVariableDeclarations implements VariableDeclarations {
  
  /** the variable declarations */
  private Map<String, Node> variables = new HashMap<String, Node>();

  /** the initializer to initialize the variables with values */
  private VariableInitializer initializer = new VariableInitializer();

  /**
   * Whether the variable is declared
   * 
   * @param name name of the variable being queried
   * @return whether the variable is declared
   */
  @Override
  public boolean hasVariable(String name) {
    return variables.containsKey(name);
  }

  /**
   * Tries to fetch a declared variable
   * 
   * @param name name of the variable to be fetched
   * @return an AST (abstrac syntax tree) node representing the variable
   */
  @Override
  public Node getVariable(String name) {
    if (!variables.containsKey(name))
      throw new RuntimeException("Variable '" + name + "' doesn't exist!");
    
    initializer.add(variables.get(name));
    return variables.get(name);
  }
  
  /**
   * Adds a variable declaration for a boolean variable</p>
   * 
   * Requires that the variable hasn't been declared before.
   * 
   * @param name the name of the variable
   */
  public void addBoolean(String name) {
    if (variables.containsKey(name))
      throw new RuntimeException("Variable '" + name + "' already exists!");
    variables.put(name, new BooleanVariable(name));
  }
  
  /**
   * Adds a variable declaration for a double variable</p>
   * 
   * Requires that the variable hasn't been declared before.
   * 
   * @param name the name of the variable
   */
  public void addDouble(String name) {
    if (variables.containsKey(name))
      throw new RuntimeException("Variable '" + name + "' already exists!");
    variables.put(name, new DoubleVariable(name));
  }
  
  /**
   * Adds a variable declaration for a string variable</p>
   * 
   * Requires that the variable hasn't been declared before.
   * 
   * @param name the name of the variable
   */
  public void addString(String name) {
    if (variables.containsKey(name))
      throw new RuntimeException("Variable '" + name + "' already exists!");
    variables.put(name, new StringVariable(name));
  }
  
  /**
   * Returns an object to initialize the declared variables
   * 
   * @return an object to initialize the declared variables
   */
  public VariableInitializer getInitializer() {
    return initializer;
  }
  
  /**
   * A class to initialize variables that have been declared by a
   * {@link SimpleVariableDeclarations} class and used inside a program</p>
   * 
   * Note that not all variables declared can be initialized!</br>
   * Particularly variables that have been declared but are never used inside a 
   * program can't be initialized.</p>
   * 
   * Then variable values that are expensive to compute don't have to be computed
   * if the values are never used inside a program.
   * 
   * @author Benjamin Weber ( benweber at student dot ethz dot ch )
   * @version $Revision: 1000 $
   */
  public static class VariableInitializer implements Serializable {
    
    /** The variables that have been declared and used in a program */
    private Map<String, Node> variables = new HashMap<String, Node>();
    
    /**
     * Adds a variable so that it can be initialized.</p>
     * 
     * Requires that the var parameter is one of
     * <ul>
     * <li>{@link Primitives.BooleanVariable}</li>
     * <li>{@link Primitives.DoubleVariable}</li>
     * <li>{@link Primitives.StringVariable}</li>
     * </ul>
     * 
     * @param var an AST node representing the variable
     */
    private void add(Node var) {
      assert var instanceof BooleanVariable || var instanceof DoubleVariable ||
        var instanceof StringVariable;
      
      if (var instanceof BooleanVariable)
        variables.put(((BooleanVariable) var).getName(), var);
      else if (var instanceof DoubleVariable)
        variables.put(((DoubleVariable) var).getName(), var);
      else if (var instanceof StringVariable)
        variables.put(((StringVariable) var).getName(), var);
    }

    /**
     * Returns the set of variable names that can be initialized
     * 
     * @return the set of variable names that can be initialized
     */
    public Set<String> getVariables() {
      return variables.keySet();
    }
    
    /**
     * Returns whether the {@link VariableInitializer} contains the variable</p>
     * 
     * @param variable name of the variable
     * @return whether the variable exists
     */
    public boolean hasVariable(String variable) {
      return variables.containsKey(variable);
    }
    
    /**
     * Sets the value of a boolean variable</p>
     * 
     * Requires that the variable exists and is of boolean type.
     * 
     * @param name name of the boolean variable
     * @param value the value the boolean variable will be set to
     */
    public void setBoolean(String name, boolean value) {
      if (!variables.containsKey(name))
        throw new RuntimeException("Variable '" + name + "' doesn't exist!");
      if (!(variables.get(name) instanceof BooleanVariable))
        throw new RuntimeException("Variable '" + name + "' is not of boolean type!");
      
      ((BooleanVariable) variables.get(name)).setValue(value);
    }
    
    /**
     * Sets the value of a double variable</p>
     * 
     * Requires that the variable exists and is of double type.
     * 
     * @param name the name of the double variable
     * @param value the value the double variable will be set to
     */
    public void setDouble(String name, double value) {
      if (!variables.containsKey(name))
        throw new RuntimeException("Variable '" + name + "' doesn't exist!");
      if (!(variables.get(name) instanceof DoubleVariable))
        throw new RuntimeException("Variable '" + name + "' is not of double type!");
      
      ((DoubleVariable) variables.get(name)).setValue(value);
    }
    
    /**
     * Sets the value of a string variable</p>
     * 
     * Requires that the variable exists and is of string type.
     * 
     * @param name the name of the string variable
     * @param value the value the string variable will be set to
     */
    public void setString(String name, String value) {
      if (!variables.containsKey(name))
        throw new RuntimeException("Variable '" + name + "' doesn't exist!");
      if (!(variables.get(name) instanceof StringVariable))
        throw new RuntimeException("Variable '" + name + "' is not of String type!");
      
      ((StringVariable) variables.get(name)).setValue(value);
    }
    
  }
  
}
