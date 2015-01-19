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
 *    MacroDeclarationsCompositor.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.expressionlanguage.common;

import weka.core.expressionlanguage.core.Macro;
import weka.core.expressionlanguage.core.MacroDeclarations;

/**
 * A helper class that allows to combine several macro declarations together.</p>
 * 
 * It can be though of as layering several scopes over one another.</p>
 * 
 * It will delegate the {@link #hasMacro(String)} and {@link #getMacro(String)}
 * methods to other macro declarations.</p>
 * 
 * Each macro declaration combined is checked in sequential order.</p>
 * 
 * No checks for conflicts are done. Thus shadowing is possible.</p>
 * 
 * @author Benjamin Weber ( benweber at student dot ethz dot ch )
 * @version $Revision: 1000 $
 */
public class MacroDeclarationsCompositor implements MacroDeclarations {

  /** the declarations being combined */
  private final MacroDeclarations[] declarations;
  
  /**
   * Constructs a {@link MacroDeclarationsCompositor} containing the provided
   * declarations</p>
   * 
   * The order of the declarations will determine the order of checking the
   * declarations for macros.</p>
   * 
   * @param declarations the declarations being combined
   */
  public MacroDeclarationsCompositor(MacroDeclarations... declarations) {
    this.declarations = declarations;
  }

  /**
   * Whether the macro is contained in one of the combined declarations.
   * 
   * @param name name of the macro
   * @return whether the macro is contained in one of the combined declarations
   */
  @Override
  public boolean hasMacro(String name) {
    for (MacroDeclarations declaration : declarations)
      if (declaration.hasMacro(name))
        return true;
    return false;
  }

  /**
   * Tries to fetch a macro from one of the combined declarations.</p>
   * 
   * The same invariant of {@link MacroDeclarations} applies here too.
   * 
   * @param name the name of the macro to be fetched
   * @return a macro
   */
  @Override
  public Macro getMacro(String name) {
    for (MacroDeclarations declaration : declarations)
      if (declaration.hasMacro(name))
        return declaration.getMacro(name);
    throw new RuntimeException("Macro '" + name + "' doesn't exist!");
  }

}
