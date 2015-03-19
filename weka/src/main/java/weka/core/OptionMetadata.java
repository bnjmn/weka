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
 *    OptionMetadata.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import java.lang.annotation.*;

/**
 * Method annotation that can be used with scheme parameters to provide a nice
 * display-ready name for the parameter, help information and, if applicable,
 * command line option details
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OptionMetadata {

  /**
   * The nice GUI displayable name for this parameter
   *
   * @return a nice displayable name
   */
  String displayName();

  /**
   * Description of this parameter. Displayed as a tool tip and help in the GUI,
   * and on the command line.
   *
   * @return the description text of this parameter
   */
  String description();

  /**
   * The order (low to high), relative to other parameters, that this property
   * should be displayed in the GUI and, if applicable, on the command line help
   *
   * @return the order (default 100)
   */
  int displayOrder() default 100;

  /**
   * The name of the command line version of this parameter (without leading -).
   * If this parameter is not a command line one, then just leave at the default
   * empty string.
   *
   * @return the name of the command line version of this parameter
   */
  String commandLineParamName() default "";

  /**
   * True if the command line version of this parameter is a flag (i.e. binary
   * parameter).
   *
   * @return true if the command line version of this parameter is a flag
   */
  boolean commandLineParamIsFlag() default false;

  /**
   * The synopsis to display on in the command line help for this parameter
   * (e.g. -Z <integer>). If this parameter is not a command line one, then just
   * leave at the default empty string.
   *
   * @return the command line synopsis for this parameter
   */
  String commandLineParamSynopsis() default "";
}
