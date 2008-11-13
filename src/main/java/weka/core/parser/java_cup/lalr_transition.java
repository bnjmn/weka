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

/** This class represents a transition in an LALR viable prefix recognition 
 *  machine.  Transitions can be under terminals for non-terminals.  They are
 *  internally linked together into singly linked lists containing all the 
 *  transitions out of a single state via the _next field.
 *
 * @see     weka.core.parser.java_cup.lalr_state
 * @version last updated: 11/25/95
 * @author  Scott Hudson
 *
 */
public class lalr_transition {

  /*-----------------------------------------------------------*/
  /*--- Constructor(s) ----------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Full constructor.
   * @param on_sym  symbol we are transitioning on.
   * @param to_st   state we transition to.
   * @param nxt     next transition in linked list.
   */
  public lalr_transition(symbol on_sym, lalr_state to_st, lalr_transition nxt)
    throws internal_error
    {
      /* sanity checks */
      if (on_sym == null)
	throw new internal_error("Attempt to create transition on null symbol");
      if (to_st == null)
	throw new internal_error("Attempt to create transition to null state");

      /* initialize */
      _on_symbol = on_sym;
      _to_state  = to_st;
      _next      = nxt;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Constructor with null next. 
   * @param on_sym  symbol we are transitioning on.
   * @param to_st   state we transition to.
   */
  public lalr_transition(symbol on_sym, lalr_state to_st) throws internal_error
    {
      this(on_sym, to_st, null);
    }

  /*-----------------------------------------------------------*/
  /*--- (Access to) Instance Variables ------------------------*/
  /*-----------------------------------------------------------*/

  /** The symbol we make the transition on. */
  protected symbol _on_symbol;

  /** The symbol we make the transition on. */
  public symbol on_symbol() {return _on_symbol;}

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** The state we transition to. */
  protected lalr_state _to_state;

  /** The state we transition to. */
  public lalr_state to_state() {return _to_state;}

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Next transition in linked list of transitions out of a state */
  protected lalr_transition _next;

  /** Next transition in linked list of transitions out of a state */
  public lalr_transition next() {return _next;}

  /*-----------------------------------------------------------*/
  /*--- General Methods ---------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Convert to a string. */
  public String toString()
    {
      String result;

      result = "transition on " + on_symbol().name() + " to state [";
      result += _to_state.index();
      result += "]";

      return result;
    }

  /*-----------------------------------------------------------*/
}
