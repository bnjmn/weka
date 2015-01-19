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
 *    MacroDeclarations.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.expressionlanguage.core;

/**
 * Interface to expose macros to a program.</p>
 * 
 * It is deliberately kept very simple to give as little constraints
 * as possible to implementations.</p>
 * 
 * There is an implied invariant here:</br>
 * <code>{@link #hasMacro(String)} == true ->
 * {@link #getMacro(String)} != null</code></p>
 * 
 * {@link #hasMacro(String)} should be pure i.e. have no side effects.</br>
 * Whereas {@link #getMacro(String)} may have side effects.</br>
 * (This is useful for creating macros on the fly in {@link #getMacro(String)})
 * 
 * @author Benjamin Weber ( benweber at student dot ethz dot ch )
 * @version $Revision: 1000 $
 */
public interface MacroDeclarations {
  
  /**
   * Whether the macro is declared
   * 
   * @param name name of the macro being queried
   * @return whether the macro is declared
   */
  public boolean hasMacro(String name);
  
  /**
   * Tries to fetch the macro</p>
   * 
   * Before a macro is fetched it shold be checked whether it is declared
   * through {@link #hasMacro(String)}
   * @param name
   * @return
   */
  public Macro getMacro(String name);
}
