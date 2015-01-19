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
 *    StatsHelper.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.expressionlanguage.weka;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import weka.core.expressionlanguage.common.Primitives.DoubleExpression;
import weka.core.expressionlanguage.core.Node;
import weka.core.expressionlanguage.core.VariableDeclarations;
import weka.experiment.Stats;

/**
 * A helper class to expose a Stats object to a program</p>
 * 
 * Exposes the following variables that correspond to Stats' fields:</br>
 * <ul>
 * <li>MAX</li>
 * <li>MIN</li>
 * <li>MEAN</li>
 * <li>SD</li>
 * <li>COUNT</li>
 * <li>SUM</li>
 * <li>SUMSQUARED</li>
 * </ul>
 * 
 * @author Benjamin Weber ( benweber at student dot ethz dot ch )
 * @version $Revision: 1000 $
 */
public class StatsHelper implements VariableDeclarations {
  
  private final Map<String, Node> variables = new HashMap<String, Node>();
  private Set<String> usedVariables = new HashSet<String>();
  private Stats stats;
  
  /**
   * Construct a {@link StatsHelper}
   */
  public StatsHelper() {
    variables.put("MAX", new Max());
    variables.put("MIN", new Min());
    variables.put("MEAN", new Mean());
    variables.put("SD", new StdDev());
    variables.put("COUNT", new Count());
    variables.put("SUM", new Sum());
    variables.put("SUMSQUARED", new SumSq());
  }
  
  /**
   * Sets the corresponding Stats object
   * 
   * @param stats the Stats object whose fields should be exposed
   */
  public void setStats(Stats stats) {
    this.stats = stats;
  }
  
  /**
   * Whether Stats fields are accessed in the program</p>
   * 
   * This is only meaningful after compilation.
   * 
   * @return Whether Stats fields are accessed in the program
   */
  public boolean used() {
    return usedVariables.isEmpty();
  }
  
  /**
   * Returns whether the Stats field is used in the program</p>
   * 
   * Must be one of the exposed variables.</p>
   * 
   * This is only meaningful after compilation.
   * 
   * @param name The name of the variable of the Stats field
   * @return whether the Stats field is used in the program
   */
  public boolean used(String name) {
    return usedVariables.contains(name);
  }
  
  /**
   * Returns whether the variable is declared
   * 
   * @param name name of the variable
   * @return whether the variable has been declared
   */
  @Override
  public boolean hasVariable(String name) {
    return variables.containsKey(name);
  }
  
  /**
   * Tries to fetch a Stats field</p>
   * 
   * The same invariant of {@link VariableDeclaration} applies here too.
   * 
   * @param name name of the variable
   * @return node representing the Stats field
   */
  @Override
  public Node getVariable(String name) {
    if (variables.containsKey(name)) {
      usedVariables.add(name);
      return variables.get(name);
    }
    throw new RuntimeException("Variable '" + name + "' undefined!");
  }

  private class Max implements DoubleExpression {
    @Override
    public double evaluate() {
      return stats.max;
    }
  }

  private class Min implements DoubleExpression {
    @Override
    public double evaluate() {
      return stats.min;
    }
  }

  private class Mean implements DoubleExpression {
    @Override
    public double evaluate() {
      return stats.mean;
    }
  }

  private class StdDev implements DoubleExpression {
    @Override
    public double evaluate() {
      return stats.stdDev;
    }
  }

  private class Count implements DoubleExpression {
    @Override
    public double evaluate() {
      return stats.count;
    }
  }

  private class Sum implements DoubleExpression {
    @Override
    public double evaluate() {
      return stats.sum;
    }
  }

  private class SumSq implements DoubleExpression {
    @Override
    public double evaluate() {
      return stats.sumSq;
    }
  }

}
