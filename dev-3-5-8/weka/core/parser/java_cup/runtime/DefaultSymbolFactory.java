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
 * Default Implementation for SymbolFactory, creates
 * plain old Symbols
 *
 * @version last updated 27-03-2006
 * @author Michael Petter
 */

/* *************************************************
  class DefaultSymbolFactory

  interface for creating new symbols  
 ***************************************************/
public class DefaultSymbolFactory implements SymbolFactory{
    // Factory methods
    /**
     * DefaultSymbolFactory for CUP.
     * Users are strongly encoraged to use ComplexSymbolFactory instead, since
     * it offers more detailed information about Symbols in source code.
     * Yet since migrating has always been a critical process, You have the
     * chance of still using the oldstyle Symbols.
     *
     * @deprecated as of CUP v11a
     * replaced by the new java_cup.runtime.ComplexSymbolFactory
     */
    //@deprecated 
    public DefaultSymbolFactory(){
    }
    public Symbol newSymbol(String name ,int id, Symbol left, Symbol right, Object value){
        return new Symbol(id,left,right,value);
    }
    public Symbol newSymbol(String name, int id, Symbol left, Symbol right){
        return new Symbol(id,left,right);
    }
    public Symbol newSymbol(String name, int id, int left, int right, Object value){
        return new Symbol(id,left,right,value);
    }
    public Symbol newSymbol(String name, int id, int left, int right){
        return new Symbol(id,left,right);
    }
    public Symbol startSymbol(String name, int id, int state){
        return new Symbol(id,state);
    }
    public Symbol newSymbol(String name, int id){
        return new Symbol(id);
    }
    public Symbol newSymbol(String name, int id, Object value){
        return new Symbol(id,value);
    }
}
