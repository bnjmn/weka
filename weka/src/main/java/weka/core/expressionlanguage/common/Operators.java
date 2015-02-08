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
 *    Operators.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.expressionlanguage.common;

import java.io.Serializable;
import java.util.regex.Pattern;

import weka.core.expressionlanguage.common.Primitives.BooleanExpression;
import weka.core.expressionlanguage.common.Primitives.DoubleExpression;
import weka.core.expressionlanguage.common.Primitives.StringConstant;
import weka.core.expressionlanguage.common.Primitives.StringExpression;
import weka.core.expressionlanguage.core.Node;
import weka.core.expressionlanguage.core.SemanticException;

/**
 * A class to specify the semantics of operators in the expressionlanguage</p>
 * 
 * To move the operator semantics outside the parser they are specified
 * here.</br> The parser can then call these methods so that operators can be
 * resolved to AST (abstract syntax tree) nodes.
 * 
 * @author Benjamin Weber ( benweber at student dot ethz dot ch )
 * @version $Revision: 1000 $
 */
public class Operators {

  /**
   * '<code>+</code>' plus operator
   */
  public static Node plus(Node left, Node right) throws SemanticException {
    if (left instanceof DoubleExpression && right instanceof DoubleExpression)
      return new Addition((DoubleExpression) left, (DoubleExpression) right);
    if (left instanceof StringExpression && right instanceof StringExpression)
      return new Concatenation((StringExpression) left,
        (StringExpression) right);

    throw new SemanticException("Plus is only applicable to doubles & Strings!");
  }

  /**
   * '<code>-</code>' minus operator
   */
  public static Node minus(Node left, Node right) throws SemanticException {
    if (left instanceof DoubleExpression && right instanceof DoubleExpression)
      return new Subtraction((DoubleExpression) left, (DoubleExpression) right);
    throw new SemanticException("Minus is only applicable to doubles!");
  }

  /**
   * '<code>*</code>' times operator
   */
  public static Node times(Node left, Node right) throws SemanticException {
    if (left instanceof DoubleExpression && right instanceof DoubleExpression)
      return new Multiplication((DoubleExpression) left,
        (DoubleExpression) right);
    throw new SemanticException("Multiplication is only applicable to doubles!");
  }

  /**
   * '<code>/</code>' division operator
   */
  public static Node division(Node left, Node right) throws SemanticException {
    if (left instanceof DoubleExpression && right instanceof DoubleExpression)
      return new Division((DoubleExpression) left, (DoubleExpression) right);
    throw new SemanticException("division is only applicable to doubles!");
  }

  /**
   * '<code>+</code>' unary plus operator
   */
  public static Node uplus(Node expr) throws SemanticException {
    if (expr instanceof DoubleExpression)
      return expr;
    throw new SemanticException("unary minus is only applicable to doubles!");
  }

  /**
   * '<code>-</code>' unary minus operator
   */
  public static Node uminus(Node expr) throws SemanticException {
    if (expr instanceof DoubleExpression)
      return new UMinus((DoubleExpression) expr);
    throw new SemanticException("unary minus is only applicable to doubles!");
  }

  /**
   * '<code>^</code>' power operator
   */
  public static Node pow(Node left, Node right) throws SemanticException {
    if (left instanceof DoubleExpression && right instanceof DoubleExpression)
      return new Pow((DoubleExpression) left, (DoubleExpression) right);
    throw new SemanticException("Power is only applicable to doubles!");
  }

  /**
   * '<code>&lt;</code>' less than operator
   */
  public static BooleanExpression lessThan(Node left, Node right)
    throws SemanticException {
    if (left instanceof DoubleExpression && right instanceof DoubleExpression)
      return new LessThan((DoubleExpression) left, (DoubleExpression) right);
    throw new SemanticException("less than is only applicable to doubles!");
  }

  /**
   * '<code>&lt=</code>' less equal operator
   */
  public static BooleanExpression lessEqual(Node left, Node right)
    throws SemanticException {
    if (left instanceof DoubleExpression && right instanceof DoubleExpression)
      return new LessEqual((DoubleExpression) left, (DoubleExpression) right);
    throw new SemanticException("less equal is only applicable to doubles!");
  }

  /**
   * '<code>&gt;</code>' greater than operator
   */
  public static BooleanExpression greaterThan(Node left, Node right)
    throws SemanticException {
    if (left instanceof DoubleExpression && right instanceof DoubleExpression)
      return new GreaterThan((DoubleExpression) left, (DoubleExpression) right);
    throw new SemanticException("greater than is only applicable to doubles!");
  }

  /**
   * '<code>&gt;=</code>' greater equal operator
   */
  public static BooleanExpression greaterEqual(Node left, Node right)
    throws SemanticException {
    if (left instanceof DoubleExpression && right instanceof DoubleExpression)
      return new GreaterEqual((DoubleExpression) left, (DoubleExpression) right);
    throw new SemanticException("greater equal is only applicable to doubles!");
  }

  /**
   * '<code>=</code>' equal operator
   */
  public static BooleanExpression equal(Node left, Node right)
    throws SemanticException {
    if (left instanceof DoubleExpression && right instanceof DoubleExpression)
      return new Equal((DoubleExpression) left, (DoubleExpression) right);
    throw new SemanticException("equal is only applicable to doubles!");
  }

  /**
   * '<code>!</code>' or '<code>not</code>' logical not operator
   */
  public static BooleanExpression not(Node expr) throws SemanticException {
    if (expr instanceof BooleanExpression)
      return new Not((BooleanExpression) expr);
    throw new SemanticException("Logical not is only applicable to booleans!");
  }

  /**
   * '<code>&</code>' or '<code>and</code>' logical and operator
   */
  public static BooleanExpression and(Node left, Node right)
    throws SemanticException {
    if (left instanceof BooleanExpression && right instanceof BooleanExpression)
      return new And((BooleanExpression) left, (BooleanExpression) right);
    throw new SemanticException("Logical and is only applicable to booleans!");
  }

  /**
   * '<code>|</code>' or '<code>or</code>' logical or operator
   */
  public static BooleanExpression or(Node left, Node right)
    throws SemanticException {
    if (left instanceof BooleanExpression && right instanceof BooleanExpression)
      return new Or((BooleanExpression) left, (BooleanExpression) right);
    throw new SemanticException("Logical or is only applicable to booleans!");
  }

  /**
   * '<code>is</code>' is operator (to check for string equality)
   */
  public static BooleanExpression is(Node left, Node right)
    throws SemanticException {
    if (left instanceof StringExpression && right instanceof StringExpression)
      return new Is((StringExpression) left, (StringExpression) right);
    throw new SemanticException("Is operator is only applicable to strings!");
  }

  /**
   * '<code>regexp</code>' regexp operator (to check for string matching a given
   * regular expression)
   */
  public static BooleanExpression regexp(Node left, Node right)
    throws SemanticException {
    if (left instanceof StringExpression) {
      if (right instanceof StringConstant)
        return new CompiledRegexp((StringExpression) left,
          ((StringConstant) right).evaluate());
      if (right instanceof StringExpression)
        return new Regexp((StringExpression) left, (StringExpression) right);
    }
    throw new SemanticException("Is operator is only applicable to strings!");
  }

  private static abstract class DoubleBinaryExpression implements
    DoubleExpression, Serializable {

    private static final long serialVersionUID = -5632795030311662604L;

    final DoubleExpression left;
    final DoubleExpression right;

    public DoubleBinaryExpression(DoubleExpression left, DoubleExpression right) {
      this.left = left;
      this.right = right;
    }

  }

  private static class Addition extends DoubleBinaryExpression implements
    Serializable {

    private static final long serialVersionUID = 4742624413216069408L;

    public Addition(DoubleExpression left, DoubleExpression right) {
      super(left, right);
    }

    @Override
    public double evaluate() {
      return left.evaluate() + right.evaluate();
    }
  }

  private static class Subtraction extends DoubleBinaryExpression implements
    Serializable {

    private static final long serialVersionUID = 2136831100085494486L;

    public Subtraction(DoubleExpression left, DoubleExpression right) {
      super(left, right);
    }

    @Override
    public double evaluate() {
      return left.evaluate() - right.evaluate();
    }
  }

  private static class Multiplication extends DoubleBinaryExpression implements
    Serializable {

    private static final long serialVersionUID = 3119913759352807383L;

    public Multiplication(DoubleExpression left, DoubleExpression right) {
      super(left, right);
    }

    @Override
    public double evaluate() {
      return left.evaluate() * right.evaluate();
    }

  }

  private static class UMinus implements DoubleExpression, Serializable {

    private static final long serialVersionUID = 8950381197456945108L;

    private final DoubleExpression expr;

    public UMinus(DoubleExpression expr) {
      this.expr = expr;
    }

    @Override
    public double evaluate() {
      return -(expr.evaluate());
    }
  }

  private static class Division extends DoubleBinaryExpression implements
    Serializable {

    private static final long serialVersionUID = -8438478400061106753L;

    public Division(DoubleExpression left, DoubleExpression right) {
      super(left, right);
    }

    @Override
    public double evaluate() {
      return left.evaluate() / right.evaluate();
    }

  }

  private static class Pow extends DoubleBinaryExpression implements
    Serializable {

    private static final long serialVersionUID = -5103792715762588751L;

    public Pow(DoubleExpression left, DoubleExpression right) {
      super(left, right);
    }

    @Override
    public double evaluate() {
      return Math.pow(left.evaluate(), right.evaluate());
    }

  }

  private static abstract class BooleanBinaryExpression<T> implements
    BooleanExpression, Serializable {

    private static final long serialVersionUID = -5375209267408472403L;

    final T left;
    final T right;

    public BooleanBinaryExpression(T left, T right) {
      this.left = left;
      this.right = right;
    }
  }

  private static class LessThan extends
    BooleanBinaryExpression<DoubleExpression> implements Serializable {

    private static final long serialVersionUID = -4323355926531143842L;

    public LessThan(DoubleExpression left, DoubleExpression right) {
      super(left, right);
    }

    @Override
    public boolean evaluate() {
      return left.evaluate() < right.evaluate();
    }

  }

  private static class LessEqual extends
    BooleanBinaryExpression<DoubleExpression> implements Serializable {

    private static final long serialVersionUID = -1949681957973467756L;

    public LessEqual(DoubleExpression left, DoubleExpression right) {
      super(left, right);
    }

    @Override
    public boolean evaluate() {
      return left.evaluate() <= right.evaluate();
    }

  }

  private static class GreaterThan extends
    BooleanBinaryExpression<DoubleExpression> implements Serializable {

    private static final long serialVersionUID = 4541137398510802289L;

    public GreaterThan(DoubleExpression left, DoubleExpression right) {
      super(left, right);
    }

    @Override
    public boolean evaluate() {
      return left.evaluate() > right.evaluate();
    }

  }

  private static class GreaterEqual extends
    BooleanBinaryExpression<DoubleExpression> implements Serializable {

    private static final long serialVersionUID = 3425719763247073382L;

    public GreaterEqual(DoubleExpression left, DoubleExpression right) {
      super(left, right);
    }

    @Override
    public boolean evaluate() {
      return left.evaluate() >= right.evaluate();
    }

  }

  private static class Equal extends BooleanBinaryExpression<DoubleExpression>
    implements Serializable {

    private static final long serialVersionUID = 4154699553290213656L;

    public Equal(DoubleExpression left, DoubleExpression right) {
      super(left, right);
    }

    @Override
    public boolean evaluate() {
      return left.evaluate() == right.evaluate();
    }

  }

  private static class And extends BooleanBinaryExpression<BooleanExpression>
    implements Serializable {

    private static final long serialVersionUID = 6786891291372905824L;

    public And(BooleanExpression left, BooleanExpression right) {
      super(left, right);
    }

    @Override
    public boolean evaluate() {
      return left.evaluate() && right.evaluate();
    }

  }

  private static class Or extends BooleanBinaryExpression<BooleanExpression>
    implements Serializable {

    private static final long serialVersionUID = -5943051466425242059L;

    public Or(BooleanExpression left, BooleanExpression right) {
      super(left, right);
    }

    @Override
    public boolean evaluate() {
      return left.evaluate() || right.evaluate();
    }

  }

  private static class Not implements BooleanExpression, Serializable {

    private static final long serialVersionUID = -6235716110409152192L;

    private final BooleanExpression expr;

    public Not(BooleanExpression expr) {
      this.expr = expr;
    }

    @Override
    public boolean evaluate() {
      return !expr.evaluate();
    }
  }

  private static class Is extends BooleanBinaryExpression<StringExpression>
    implements Serializable {

    private static final long serialVersionUID = -7519297057279624722L;

    public Is(StringExpression left, StringExpression right) {
      super(left, right);
    }

    @Override
    public boolean evaluate() {
      return left.evaluate().equals(right.evaluate());
    }
  }

  private static class Regexp extends BooleanBinaryExpression<StringExpression>
    implements Serializable {

    private static final long serialVersionUID = -3987002284718527200L;

    public Regexp(StringExpression left, StringExpression right) {
      super(left, right);
    }

    @Override
    public boolean evaluate() {
      return left.evaluate().matches(right.evaluate());
    }
  }

  private static class CompiledRegexp implements BooleanExpression,
    Serializable {

    private static final long serialVersionUID = -224974827347001236L;

    private final StringExpression expr;
    private final Pattern pattern;

    public CompiledRegexp(StringExpression expr, String pattern) {
      this.expr = expr;
      this.pattern = Pattern.compile(pattern);
    }

    @Override
    public boolean evaluate() {
      return pattern.matcher(expr.evaluate()).matches();
    }
  }

  private static class Concatenation implements StringExpression, Serializable {

    private static final long serialVersionUID = 2413200029613562555L;

    private final StringExpression left;
    private final StringExpression right;

    public Concatenation(StringExpression left, StringExpression right) {
      this.left = left;
      this.right = right;
    }

    @Override
    public String evaluate() {
      return left.evaluate() + right.evaluate();
    }
  }
}
