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
 *    NoVariables.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.expressionlanguage.common;

import weka.core.expressionlanguage.core.Node;
import weka.core.expressionlanguage.core.VariableDeclarations;

/**
 * A variable declarations that contains no variables
 * 
 * @author Benjamin Weber ( benweber at student dot ethz dot ch )
 * @version $Revision: 1000 $
 */
public class NoVariables implements VariableDeclarations {

  /**
   * Whether the variable is declared. Will always return <code>false</code>
   * 
   * @param name name of the variable
   * @return whether the variable is declared. Always <code>false</code>.
   */
  @Override
  public boolean hasVariable(String name) {
    return false;
  }

  /**
   * Tries to fetch the variable. Will always fail.</p>
   * 
   * The same invariant of {@link VariableDeclarations} applies here too.
   * 
   * @param name name of the variable
   * @return nothing
   */
  @Override
  public Node getVariable(String name) {
    throw new RuntimeException("Variable '" + name + "' doesn't exist!");
  }

}
