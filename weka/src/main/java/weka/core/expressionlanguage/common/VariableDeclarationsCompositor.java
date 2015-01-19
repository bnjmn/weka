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
 *    VariableDeclarationsCompositor.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.expressionlanguage.common;

import weka.core.expressionlanguage.core.Node;
import weka.core.expressionlanguage.core.VariableDeclarations;

/**
 * A helper class that allows to combine several variable declarations together.</p>
 * 
 * It can be thought of as layering several scopes over one another.</p>
 * 
 * It will delegate the {@link #hasVariable(String)} and {@link #getVariable(String)}
 * methods to other variable declarations.</p>
 * 
 * Each variable declaration combined is checked in sequential order.</p>
 * 
 * No checks for conflicts are done. Thus shadowing is possible.</p>
 * 
 * @author Benjamin Weber ( benweber at student dot ethz dot ch )
 * @version $Revision: 1000 $
 */
public class VariableDeclarationsCompositor implements VariableDeclarations {
  
  /** the declarations being combined */
  private VariableDeclarations[] declarations;
  
  /**
   * Constructs a {@link VariableDeclarationsCompositor} containing the provided
   * declarations</p>
   * 
   * The order of the declarations will determine the order of checking the
   * declarations for variables.</p>
   * 
   * @param declarations the declarations being combined
   */
  public VariableDeclarationsCompositor(VariableDeclarations... declarations) {
    this.declarations = declarations;
  }

  /**
   * Whether the variable is contained in one of the combined declarations.
   * 
   * @param name name of the variable
   * @return whether the variable is contained in one of the combined declarations
   */
  @Override
  public boolean hasVariable(String name) {
    for (VariableDeclarations declaration : declarations)
      if (declaration.hasVariable(name))
        return true;
    return false;
  }

  /**
   * Tries to fetch a variable from one of the combined declarations.</p>
   * 
   * The same invariant of {@link VariableDeclarations} applies here too.
   * 
   * @param name the name of the variable to be fetched
   * @return an AST (abstract syntax tree) node representing the variable
   */
  @Override
  public Node getVariable(String name) {
    for (VariableDeclarations declaration : declarations)
      if (declaration.hasVariable(name))
        return declaration.getVariable(name);
    throw new RuntimeException("Variable '" + name + "' doesn't exist!");
  }

}
