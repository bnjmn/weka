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
 *    JavaMacro.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.expressionlanguage.common;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import weka.core.expressionlanguage.common.Primitives.BooleanExpression;
import weka.core.expressionlanguage.common.Primitives.DoubleExpression;
import weka.core.expressionlanguage.common.Primitives.StringConstant;
import weka.core.expressionlanguage.common.Primitives.StringExpression;
import weka.core.expressionlanguage.core.Macro;
import weka.core.expressionlanguage.core.MacroDeclarations;
import weka.core.expressionlanguage.core.Node;
import weka.core.expressionlanguage.core.SemanticException;

/**
 * A macro declarations that exposes the java macro to a program.</p>
 * 
 * The java macro can be used as follows</br>
 * </code>java(class, signature, arguments)</code></p>
 * 
 * Where class is the name of class, signature is the signature of the function
 * and arguments is a list of arguments passed to the java function.</p>
 * 
 * The signature follows the pattern:</br>
 * <code>type name '(' [ type [ ',' type ]* ] ')'</code></br>
 * where type is one of 'boolean', 'double' or 'String' and name must be a valid
 * java identifier.</p>
 * 
 * Examples:</br>
 * <ul>
 * <li><code>java('java.lang.Math', 'double sqrt(double)', 4.0)</code></li>
 * <li><code>java('java.lang.String', 'String valueOf(double)', 2^30)</code></li>
 * </ul>
 * 
 * @author Benjamin Weber ( benweber at student dot ethz dot ch )
 * @version $Revision: 1000 $
 */
public class JavaMacro implements MacroDeclarations, Macro {
  
  private static final String JAVA_MACRO = "java";
  private static final String BOOLEAN = "boolean";
  private static final String DOUBLE = "double";
  private static final String STRING = "String";

  /**
   * Evaluates the java macro on the given arguments
   * 
   * @param nodes the arguments to the java macro
   * @retun an AST (abstract syntax tree) node
   */
  @Override
  public Node evaluate(Node... nodes) throws SemanticException {
    if (nodes.length < 2)
      throw new SemanticException("The " + JAVA_MACRO + " macro takes at least 2 arguments!");
    if (!(nodes[0] instanceof StringConstant && nodes[1] instanceof StringConstant))
      throw new SemanticException(JAVA_MACRO + "'s first and second argument must be String constants!");

    Node[] parameterNodes = Arrays.copyOfRange(nodes, 2, nodes.length);
    
    String className = ((StringConstant) nodes[0]).evaluate();
    String signature = ((StringConstant) nodes[1]).evaluate();
    
    // get types
    List<Class<?>> parameterTypes = new ArrayList<Class<?>>();
    String name = parseSignature(signature, parameterTypes);
    Class<?> returnType = parameterTypes.remove(0);
    
    // get method
    Method m;
    try {
      m = Class.forName(className).getMethod(name, parameterTypes.toArray(new Class<?>[0]));
    } catch (Exception e) {
      throw new SemanticException("Failed to load method '"
        + className + "." + name + "' in " + JAVA_MACRO + " macro!", e);
    }
    
    // check parameters
    if (parameterTypes.size() != parameterNodes.length)
      throw new SemanticException("Wrong amount of parameters given in " + JAVA_MACRO + " macro!");
    for (int i = 0; i < parameterTypes.size() && i < parameterNodes.length; i++) {
      if (parameterTypes.get(i).equals(Boolean.TYPE) && parameterNodes[i] instanceof BooleanExpression)
        continue;
      if (parameterTypes.get(i).equals(Double.TYPE) && parameterNodes[i] instanceof DoubleExpression)
        continue;
      if (parameterTypes.get(i).equals(String.class) && parameterNodes[i] instanceof StringExpression)
        continue;
      throw new SemanticException("Type error in " + JAVA_MACRO + " macro!");
    }
    
    if (returnType.equals(Boolean.TYPE))
      return new BooleanJavaMethod(m, parameterNodes);
    if (returnType.equals(Double.TYPE))
      return new DoubleJavaMethod(m, parameterNodes);
    if (returnType.equals(String.class))
      return new StringJavaMethod(m, parameterNodes);
    
    assert false;
    throw new SemanticException("Internal error in " + JAVA_MACRO + " macro!");
  }
  
  /**
   * Parses a signature
   * 
   * @param signature signature to be parsed
   * @param types List into which the types get safed
   * @return name of the function in the signature
   * @throws InvalidSignature if the signature doesn't follow the specified pattern
   */
  private String parseSignature(String signature, List<Class<?>> types) throws InvalidSignature {
    
    // tokens now should be <returntype> <name> '(' [ <type> [ ',' <type> ]* ] ')'
    List<String> tokens = tokenize(signature);
    
    if (tokens.size() < 4)
      throw new InvalidSignature("Not enough tokens in '" + signature + "'");

    // get return type
    types.add(getType(tokens.get(0)));
    
    // check name
    if (!isJavaIdentifier(tokens.get(1)))
      throw new InvalidSignature("Invalid function name '" + tokens.get(1) + "'");
    String name = tokens.get(1);
    
    // check opening bracket
    if (!"(".equals(tokens.get(2)))
      throw new InvalidSignature("Missing opening bracket, got '" + tokens.get(2) + "' instead");
    
    boolean first = true;
    int i = 3;
    for (; i < tokens.size() && !")".equals(tokens.get(i)); i++) {

      // check comma
      if (!first && !",".equals(tokens.get(i)))
        throw new InvalidSignature("Missing comma between parameters, got '" + tokens.get(i) + "' instead");
      
      if (!first)
        ++i;

      // get parameter
      if (i >= tokens.size())
        throw new InvalidSignature("No parameter after comma!");
      types.add(getType(tokens.get(i)));
      
      first = false;
    }
    
    // check closing bracket
    if (i < tokens.size() && !")".equals(tokens.get(i))) {
      System.out.println(i);
      System.out.println(tokens);
      throw new InvalidSignature("Missing closing bracket, got '" + tokens.get(i) + "' instead");
    }
    
    // check end
    if (i != tokens.size() - 1)
      throw new InvalidSignature("Failed parsing signature at token '" + tokens.get(i) + "'");
      
    return name;
  }
  
  /**
   * Tokenizes a signature
   * 
   * @param signature to be tokenized
   * @return list of tokens
   */
  private List<String> tokenize(String signature) {
     // split on white spaces
    String[] whiteSpaceTokens = signature.split("\\s+");
    
    // split on ,() but also return ,()
    List<String> tokens = new ArrayList<String>();
    for (String token : whiteSpaceTokens) {
      StringTokenizer tokenizer = new StringTokenizer(token, ",()", true);
      while (tokenizer.hasMoreElements())
        tokens.add(tokenizer.nextToken());
    }
    
    return tokens;
  }
  
  /**
   * Tries to fetch a {@link Class<?>} for the given type
   * 
   * @param type name of the type
   * @return {@link Class<?>} for the type
   * @throws InvalidSignature if the type is invalid
   */
  private Class<?> getType(String type) throws InvalidSignature {
    if (type.equals(BOOLEAN)) {
      return Boolean.TYPE;
    } else if (type.equals(DOUBLE)) {
      return Double.TYPE;
    } else if (type.equals(STRING)) {
      return String.class;
    } else {
      throw new InvalidSignature("Expected type, got '" + type + "' instead");
    }
  }
  
  /**
   * Whether identifier is a valid java identifier
   * 
   * @param identifier the identifier to be tested
   * @return whether the identifier is a valid java identifier
   */
  private boolean isJavaIdentifier(String identifier) {
    if (identifier.length() == 0)
      return false;
    if (!Character.isJavaIdentifierStart(identifier.charAt(0)))
      return false;
    for (int i = 1; i < identifier.length(); i++)
      if (!Character.isJavaIdentifierPart(identifier.charAt(i)))
        return false;
    return true;
  }
  
  private static class InvalidSignature extends SemanticException {
    /** for serialization */
    private static final long serialVersionUID = -4198745015342335018L;

    public InvalidSignature(String reason) {
      super("Invalid function signature in " + JAVA_MACRO + " macro (" + reason + ")");
    }
  }

  /**
   * Whether the macro declarations contains the macro
   * 
   * @param name name of the macro
   * @return whether the macro is declared
   */
  @Override
  public boolean hasMacro(String name) {
    return JAVA_MACRO.equals(name);
  }

  @Override
  public Macro getMacro(String name) {
    if (hasMacro(name))
      return this;
    throw new RuntimeException("Undefined macro '" + name + "'!");
  }
  
  private static abstract class JavaMethod implements Node {
    
    protected final Method method;
    protected final Node[] params;
    protected final Object[] args;

    /**
     * Constructs a JavaMethod encapsulating the given method with the given
     * arguments.</p>
     * 
     * Requires that the given arguments match the parameters of the method.
     * 
     * @param method method to be encapsulated
     * @param params the arguments for the method
     */
    public JavaMethod(Method method, Node... params) {
      assert Modifier.isStatic(method.getModifiers());

      this.method = method;
      this.params = params;
      args = new Object[params.length];
    }

    protected void evaluateArgs() {

      for (int i = 0; i < params.length; ++i) {
        if (params[i] instanceof BooleanExpression) {
          args[i] = new Boolean(((BooleanExpression) params[i]).evaluate());
        } else if (params[i] instanceof DoubleExpression) {
          args[i] = new Double(((DoubleExpression) params[i]).evaluate());
        } else if (params[i] instanceof StringExpression) {
          args[i] = ((StringExpression) params[i]).evaluate();
        } // else shouldn't happen!
      }

    }

  }

  private static class BooleanJavaMethod extends JavaMethod implements BooleanExpression {

    public BooleanJavaMethod(Method method, Node... params) {
      super(method, params);
      assert Boolean.TYPE.equals(method.getReturnType());
    }

    @Override
    public boolean evaluate() {
      try {
        evaluateArgs();
        return (Boolean) method.invoke(null, args);
      } catch (Exception e) {
        throw new RuntimeException(
            "Failed to execute java function '" + method.getName() + "'!", e);
      }
    }
  }

  private static class DoubleJavaMethod extends JavaMethod implements DoubleExpression {

    public DoubleJavaMethod(Method method, Node... params) {
      super(method, params);
      assert Double.TYPE.equals(method.getReturnType());
    }

    @Override
    public double evaluate() {
      try {
        evaluateArgs();
        return (Double) method.invoke(null, args);
      } catch (Exception e) {
        throw new RuntimeException(
            "Failed to execute java function '" + method.getName() + "'!", e);
      }
    }
  }

  private static class StringJavaMethod extends JavaMethod implements StringExpression {

    public StringJavaMethod(Method method, Node... params) {
      super(method, params);
      assert String.class.equals(method.getReturnType());
    }

    @Override
    public String evaluate() {
      try {
        evaluateArgs();
        return (String) method.invoke(null, args);
      } catch (Exception e) {
        throw new RuntimeException(
            "Failed to execute java function '" + method.getName() + "'!", e);
      }
    }
  }

}
