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

package weka.core.parser.java_cup.runtime;

/**
 * Defines the Symbol class, which is used to represent all terminals
 * and nonterminals while parsing.  The lexer should pass CUP Symbols 
 * and CUP returns a Symbol.
 *
 * @version last updated: 7/3/96
 * @author  Frank Flannery
 */

/* ****************************************************************
  Class Symbol
  what the parser expects to receive from the lexer. 
  the token is identified as follows:
  sym:    the symbol type
  parse_state: the parse state.
  value:  is the lexical value of type Object
  left :  is the left position in the original input file
  right:  is the right position in the original input file
  xleft:  is the left position Object in the original input file
  xright:  is the left position Object in the original input file
******************************************************************/

public class Symbol {

//  TUM 20060327: Added new Constructor to provide more flexible way
//   for location handling
/*******************************
 *******************************/
    public Symbol(int id, Symbol left, Symbol right, Object o){
        this(id,left.left,right.right,o);
    }
    public Symbol(int id, Symbol left, Symbol right){
        this(id,left.left,right.right);
    }
/*******************************
  Constructor for l,r values
 *******************************/

  public Symbol(int id, int l, int r, Object o) {
    this(id);
    left = l;
    right = r;
    value = o;
  }

/*******************************
  Constructor for no l,r values
********************************/

  public Symbol(int id, Object o) {
    this(id, -1, -1, o);
  }

/*****************************
  Constructor for no value
  ***************************/

  public Symbol(int id, int l, int r) {
    this(id, l, r, null);
  }

/***********************************
  Constructor for no value or l,r
***********************************/

  public Symbol(int sym_num) {
    this(sym_num, -1);
    left = -1;
    right = -1;
  }

/***********************************
  Constructor to give a start state
***********************************/
  Symbol(int sym_num, int state)
    {
      sym = sym_num;
      parse_state = state;
    }

/*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** The symbol number of the terminal or non terminal being represented */
  public int sym;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** The parse state to be recorded on the parse stack with this symbol.
   *  This field is for the convenience of the parser and shouldn't be 
   *  modified except by the parser. 
   */
  public int parse_state;
  /** This allows us to catch some errors caused by scanners recycling
   *  symbols.  For the use of the parser only. [CSA, 23-Jul-1999] */
  boolean used_by_parser = false;

/*******************************
  The data passed to parser
 *******************************/

  public int left, right;
  public Object value;

  /*****************************
    Printing this token out. (Override for pretty-print).
    ****************************/
  public String toString() { return "#"+sym; }
}






