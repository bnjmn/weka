/*
 * STANDARD ML OF NEW JERSEY COPYRIGHT NOTICE, LICENSE AND DISCLAIMER.
 * 
 * Copyright (c) 1989-1998 by Lucent Technologies
 * 
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose and without fee is hereby granted, provided
 * that the above copyright notice appear in all copies and that both the
 * copyright notice and this permission notice and warranty disclaimer appear
 * in supporting documentation, and that the name of Lucent Technologies, Bell
 * Labs or any Lucent entity not be used in advertising or publicity pertaining
 * to distribution of the software without specific, written prior permission.
 *
 * Lucent disclaims all warranties with regard to this software, including all
 * implied warranties of merchantability and fitness. In no event shall Lucent
 * be liable for any special, indirect or consequential damages or any damages
 * whatsoever resulting from loss of use, data or profits, whether in an action
 * of contract, negligence or other tortious action, arising out of or in
 * connection with the use or performance of this software. 
 *
 * Taken from this URL:
 * http://www.smlnj.org/license.html
 * 
 * This license is compatible with the GNU GPL (see section "Standard ML of New
 * Jersey Copyright License"):
 * http://www.gnu.org/licenses/license-list.html#StandardMLofNJ
 */

/*
 * Copyright 1996-1999 by Scott Hudson, Frank Flannery, C. Scott Ananian
 */

package weka.core.parser.java_cup;

/** 
 * This class represents a part of a production which contains an
 * action.  These are eventually eliminated from productions and converted
 * to trailing actions by factoring out with a production that derives the
 * empty string (and ends with this action).
 *
 * @see weka.core.parser.java_cup.production
 * @version last update: 11/25/95
 * @author Scott Hudson
 */

public class action_part extends production_part {

  /*-----------------------------------------------------------*/
  /*--- Constructors ------------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Simple constructor. 
   * @param code_str string containing the actual user code.
   */
  public action_part(String code_str)
    {
      super(/* never have a label on code */null);
      _code_string = code_str;
    }

  /*-----------------------------------------------------------*/
  /*--- (Access to) Instance Variables ------------------------*/
  /*-----------------------------------------------------------*/

  /** String containing code for the action in question. */
  protected String _code_string;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** String containing code for the action in question. */
  public String code_string() {return _code_string;}

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Set the code string. */
  public void set_code_string(String new_str) {_code_string = new_str;}

  /*-----------------------------------------------------------*/
  /*--- General Methods ---------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Override to report this object as an action. */
  public boolean is_action() { return true; }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Equality comparison for properly typed object. */
  public boolean equals(action_part other)
    {
      /* compare the strings */
      return other != null && super.equals(other) && 
	     other.code_string().equals(code_string());
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Generic equality comparison. */
  public boolean equals(Object other)
    {
      if (!(other instanceof action_part)) 
	return false;
      else
	return equals((action_part)other);
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Produce a hash code. */
  public int hashCode()
    {
      return super.hashCode() ^ 
	     (code_string()==null ? 0 : code_string().hashCode());
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Convert to a string.  */
  public String toString()
    {
      return super.toString() + "{" + code_string() + "}";
    }

  /*-----------------------------------------------------------*/
}
