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

/** This class serves as the base class for entries in a parse action table.  
 *  Full entries will either be SHIFT(state_num), REDUCE(production), NONASSOC,
 *  or ERROR. Objects of this base class will default to ERROR, while
 *  the other three types will be represented by subclasses. 
 * 
 * @see     weka.core.parser.java_cup.reduce_action
 * @see     weka.core.parser.java_cup.shift_action
 * @version last updated: 7/2/96
 * @author  Frank Flannery
 */

public class parse_action {

  /*-----------------------------------------------------------*/
  /*--- Constructor(s) ----------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Simple constructor. */
  public parse_action()
    {
      /* nothing to do in the base class */
    }

 
  /*-----------------------------------------------------------*/
  /*--- (Access to) Static (Class) Variables ------------------*/
  /*-----------------------------------------------------------*/

  /** Constant for action type -- error action. */
  public static final int ERROR = 0;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Constant for action type -- shift action. */
  public static final int SHIFT = 1;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Constants for action type -- reduce action. */
  public static final int REDUCE = 2;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Constants for action type -- reduce action. */
  public static final int NONASSOC = 3;

  /*-----------------------------------------------------------*/
  /*--- General Methods ---------------------------------------*/
  /*-----------------------------------------------------------*/
	 
  /** Quick access to the type -- base class defaults to error. */
  public int kind() {return ERROR;}

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Equality test. */
  public boolean equals(parse_action other)
    {
      /* we match all error actions */
      return other != null && other.kind() == ERROR;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Generic equality test. */
  public boolean equals(Object other)
    {
      if (other instanceof parse_action)
	return equals((parse_action)other);
      else
	return false;
    }
  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Compute a hash code. */
  public int hashCode()
    {
      /* all objects of this class hash together */
      return 0xCafe123;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Convert to string. */
  public String toString() {return "ERROR";}

  /*-----------------------------------------------------------*/
}
    
