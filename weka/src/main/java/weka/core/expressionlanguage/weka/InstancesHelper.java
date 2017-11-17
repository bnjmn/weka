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
 *    InstancesHelper.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.expressionlanguage.weka;

import java.util.regex.Pattern;

import weka.core.Utils;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.expressionlanguage.core.Macro;
import weka.core.expressionlanguage.core.MacroDeclarations;
import weka.core.expressionlanguage.core.Node;
import weka.core.expressionlanguage.core.SemanticException;
import weka.core.expressionlanguage.core.VariableDeclarations;
import weka.core.expressionlanguage.common.Primitives.BooleanExpression;
import weka.core.expressionlanguage.common.Primitives.DoubleExpression;
import weka.core.expressionlanguage.common.Primitives.StringExpression;

/**
 * A helper class to expose instance values and macros for instance values to a
 * program</p>
 * 
 * Exposes instance values of the current instance (see
 * {@link #setInstance(Instance)} and {@link #setInstance(int)} methods) as a1,
 * A1 or ATT1 etc where the number is the attribute index (starting with
 * 1).</br> Furthermore exposes the class value as CLASS.</p>
 * 
 * Exposes the '<code>ismissing</code>' macro which can only be applied to
 * instance values and returns whether the value is set as missing in the
 * current instance.</p>
 * 
 * @author Benjamin Weber ( benweber at student dot ethz dot ch )
 * @version $Revision: 1000 $
 */
public class InstancesHelper implements VariableDeclarations, Macro,
  MacroDeclarations {

  // from MathExpression filter and AttributeExpression
  private static final Pattern ATTRIBUTE1 = Pattern.compile("[aA][0-9]+");

  // from subsetbyexpression
  private static final Pattern ATTRIBUTE2 = Pattern.compile("ATT[0-9]+");

  // from subsetbyexpression
  private static final String CLASS = "CLASS";

  // from subsetbyexpression
  private static final String IS_MISSING = "ismissing";

  private static final long serialVersionUID = -4398876812339967703L;

  /** the dataset whose instance values should be exposed */
  private final Instances dataset;

  /** the current instance whose values should be exposed */
  private Instance instance;

  /** whether a missing value has been evaluated during computation */
  private boolean missingAccessed = false;

  /**
   * True if this instance retains the full dataset rather than just the
   * attribute information
   */
  private final boolean dataRetained;

  /**
   * Constructs an {@link InstancesHelper} for the given dataset. Only attribute
   * information is retained in memory.
   * 
   * @param dataset the dataset whose attribute values should be exposed
   */
  public InstancesHelper(Instances dataset) {
    this(dataset, false);
  }

  /**
   * Constructs an {@link InstancesHelper} for the given dataset.
   * 
   * @param dataset the dataset
   * @param retainData true if the full dataset (rather than just the attribute
   *          information) is to be retained
   */
  public InstancesHelper(Instances dataset, boolean retainData) {
    assert dataset != null;
    if (retainData) {
      this.dataset = dataset;
    } else {
      this.dataset = new Instances(dataset, 0);
    }
    dataRetained = retainData;
  }

  /**
   * Whether the given macro is declared
   * 
   * @param name name of the macro
   * @return whether the given macro is declared
   */
  @Override
  public boolean hasMacro(String name) {
    return IS_MISSING.equals(name);
  }

  /**
   * Tries to fetch a macro</p>
   * 
   * The same invariant of {@link MacroDeclarations} applies here too.
   * 
   * @param name of the macro
   * @return the macro
   */
  @Override
  public Macro getMacro(String name) {
    if (hasMacro(name))
      return this;
    throw new RuntimeException("Macro '" + name + "' undefined!");
  }

  /**
   * Evaluates the 'ismissing' macro
   * 
   * @param params the arguments for the macro
   * @return an AST node representing the ismissing function
   */
  public Node evaluate(Node... params) throws SemanticException {
    if (params.length != 1)
      throw new SemanticException("Macro " + IS_MISSING
        + " takes exactly one argument!");
    if (params[0] instanceof Value)
      return new isMissing((Value) params[0]);
    throw new SemanticException(IS_MISSING
      + " is only applicable to a dataset value!");
  }

  private static class isMissing implements BooleanExpression {

    private static final long serialVersionUID = -3805035561340865906L;

    private final Value value;

    public isMissing(Value value) {
      this.value = value;
    }

    @Override
    public boolean evaluate() {
      return value.isMissing();
    }
  }

  /**
   * Sets the instance at index i of the supplied dataset to be the current
   * instance
   * 
   * @param i the index of the instance to be set
   * @throws UnsupportedOperationException if the full dataset has not been
   *           retained in memory
   */
  public void setInstance(int i) {
    if (!dataRetained) {
      throw new UnsupportedOperationException(
        "Unable to set the instance based "
          + "on index because the dataset has not been retained in memory");
    }
    setInstance(dataset.get(i));
  }

  /**
   * Sets the current instance to be the supplied instance
   * 
   * @param instance instance to be set as the current instance
   */
  public void setInstance(Instance instance) {
    assert dataset.equalHeaders(instance.dataset());
    this.instance = instance;
    missingAccessed = false;
  }

  /**
   * Whether a missing value has been evaluated during computation.</p>
   * 
   * Will be reset when the {@link #setInstance(int)} or
   * {@link #setInstance(Instance)} method is called.
   * 
   * @return whether a missing value has been evaluated during computation
   */
  public boolean missingAccessed() {
    return missingAccessed;
  }

  private int getIndex(String attribute) {

    if (ATTRIBUTE1.matcher(attribute).matches())
      return Integer.parseInt(attribute.substring(1)) - 1;
    if (ATTRIBUTE2.matcher(attribute).matches())
      return Integer.parseInt(attribute.substring(3)) - 1;
    if (CLASS.equals(attribute))
      return dataset.classIndex();

    return -1;
  }

  /**
   * Returns whether the variable is declared
   * 
   * @param name name of the variable
   * @return whether it has been declared
   */
  @Override
  public boolean hasVariable(String name) {
    int index = getIndex(name);

    if (0 <= index && index < dataset.numAttributes())
      return true;
    return false;
  }

  /**
   * Tries to fetch a variable of an instance value</p>
   * 
   * The same invariant of {@link VariableDeclarations} applies here too.
   * 
   * @param name name of the variable
   * @return node representing the instance value
   */
  @Override
  public Node getVariable(String name) {
    int index = getIndex(name);

    if (index < 0 || index >= dataset.numAttributes())
      throw new RuntimeException("Variable '" + name + "' undefined!");

    if (dataset.attribute(index).isNumeric())
      return new DoubleValue(index);
    if (dataset.attribute(index).isString()
      || dataset.attribute(index).isNominal())
      return new StringValue(index);
    throw new RuntimeException("Attributes of type '"
      + dataset.attribute(index).toString() + "' not supported!");
  }

  private abstract class Value implements Node {

    private static final long serialVersionUID = 5839070716097467627L;

    private final int index;

    public Value(int index) {
      this.index = index;
    }

    public int getIndex() {
      return index;
    }

    public boolean isMissing() {
      return instance.isMissing(getIndex());
    }
  }

  private class DoubleValue extends Value implements DoubleExpression {

    private static final long serialVersionUID = -1001674545929082424L;

    public DoubleValue(int index) {
      super(index);
      assert dataset.attribute(getIndex()).isNumeric();
    }

    @Override
    public double evaluate() {
      if (isMissing()) {
        missingAccessed = true;
        return Utils.missingValue();
      }
      return instance.value(getIndex());
    }
  }

  private class StringValue extends Value implements StringExpression {

    private static final long serialVersionUID = -249974216283801876L;

    public StringValue(int index) {
      super(index);
      assert dataset.attribute(index).isString()
        || dataset.attribute(index).isNominal();
    }

    @Override
    public String evaluate() {
      if (isMissing()) {
        missingAccessed = true;
        return "";
      }
      return instance.stringValue(getIndex());
    }
  }
}
