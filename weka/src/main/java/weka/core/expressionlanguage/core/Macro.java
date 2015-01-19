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
 *    Macro.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.expressionlanguage.core;

/**
 * Interface for compile time macros to enable meta programming</p>
 * 
 * Because macros have direct access to the AST (abstract syntax tree) they can
 * rewrite it as they please.
 * 
 * @author Benjamin Weber ( benweber at student dot ethz dot ch )
 * @version $Revision: 1000 $
 */
public interface Macro {
  /**
   * Applies a macro to a set of parameter nodes.
   * 
   * @param nodes the nodes this macro should be applied to
   * @return an AST node returned by the macro
   * @throws SemanticException
   */
  Node evaluate(Node... nodes) throws SemanticException;
}