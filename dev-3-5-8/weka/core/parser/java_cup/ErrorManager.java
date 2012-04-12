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

import weka.core.parser.java_cup.runtime.Symbol;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
public class ErrorManager{
    private static ErrorManager errorManager;
    private int errors = 0;
    private int warnings = 0;
    private int fatals = 0;
    public int getFatalCount() { return fatals; }
    public int getErrorCount() { return errors; }
    public int getWarningCount() { return warnings; }
    static {
        errorManager = new ErrorManager();
    }
    public static ErrorManager getManager() { return errorManager; }
    private ErrorManager(){
    }

    //TODO: migrate to java.util.logging
    /**
     * Error message format: 
     * ERRORLEVEL at (LINE/COLUMN)@SYMBOL: MESSAGE
     * ERRORLEVEL : MESSAGE
     **/
    public void emit_fatal(String message){
        System.err.println("Fatal : "+message);
        fatals++;
    }
    public void emit_fatal(String message, Symbol sym){
        //System.err.println("Fatal at ("+sym.left+"/"+sym.right+")@"+convSymbol(sym)+" : "+message);
        System.err.println("Fatal: "+message+" @ "+sym);
        fatals++;
    }
    public void emit_warning(String message){
        System.err.println("Warning : " + message);
        warnings++;	
    }
    public void emit_warning(String message, Symbol sym){
//        System.err.println("Warning at ("+sym.left+"/"+sym.right+")@"+convSymbol(sym)+" : "+message);
        System.err.println("Fatal: "+message+" @ "+sym);
        warnings++;
    }
    public void emit_error(String message){
        System.err.println("Error : " + message);
        errors++;
    }
    public void emit_error(String message, Symbol sym){
//        System.err.println("Error at ("+sym.left+"/"+sym.right+")@"+convSymbol(sym)+" : "+message);
        System.err.println("Error: "+message+" @ "+sym);
        errors++;
    }
    private static String convSymbol(Symbol symbol){
        String result = (symbol.value == null)? "" : " (\""+symbol.value.toString()+"\")";
        Field [] fields = sym.class.getFields();
        for (int i = 0; i < fields.length ; i++){
            if (!Modifier.isPublic(fields[i].getModifiers())) continue;
            try {
                if (fields[i].getInt(null) == symbol.sym) return fields[i].getName()+result;
            }catch (Exception ex) {
            }
        }
        return symbol.toString()+result;
    }
    
}
