/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 * Scanner.java
 * Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.instance.subsetbyexpression;

import weka.core.parser.java_cup.runtime.SymbolFactory;
import java.io.*;

/**
 * A scanner for evaluating whether an Instance is to be included in a subset
 * or not.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.1 $
 */
%%
%cup
%public
%class Scanner
%{
  // Author: FracPete (fracpete at waikato dot ac dot nz)
  // Version: $Revision: 1.1 $
  protected SymbolFactory sf;

  public Scanner(InputStream r, SymbolFactory sf){
    this(r);
    this.sf = sf;
  }
%}
%eofval{
    return sf.newSymbol("EOF",sym.EOF);
%eofval}

%%
// operands
"-" { return sf.newSymbol("Minus", sym.MINUS); }
"+" { return sf.newSymbol("Plus", sym.PLUS); }
"*" { return sf.newSymbol("Times", sym.TIMES); }
"/" { return sf.newSymbol("Division", sym.DIVISION); }

// boolean stuff
"<" { return sf.newSymbol("Less than", sym.LT); }
"<=" { return sf.newSymbol("Less or equal than", sym.LE); }
">" { return sf.newSymbol("Greater than", sym.GT); }
">=" { return sf.newSymbol("Greater or equal than", sym.GE); }
"=" { return sf.newSymbol("Equals", sym.EQ); }
"is" { return sf.newSymbol("Is", sym.IS); }
"not" { return sf.newSymbol("Not", sym.NOT); }
"and" { return sf.newSymbol("And", sym.AND); }
"or" { return sf.newSymbol("Or", sym.OR); }
"true" { return sf.newSymbol("True", sym.TRUE); }
"false" { return sf.newSymbol("False", sym.FALSE); }

// functions
"abs" { return sf.newSymbol("Abs", sym.ABS); }
"sqrt" { return sf.newSymbol("Sqrt", sym.SQRT); }
"log" { return sf.newSymbol("Log", sym.LOG); }
"exp" { return sf.newSymbol("Exp", sym.EXP); }
"sin" { return sf.newSymbol("Sin", sym.SIN); }
"cos" { return sf.newSymbol("Cos", sym.COS); }
"tan" { return sf.newSymbol("Tan", sym.TAN); }
"rint" { return sf.newSymbol("Rint", sym.RINT); }
"floor" { return sf.newSymbol("Floor", sym.FLOOR); }
"pow" { return sf.newSymbol("Pow", sym.POW); }
"ceil" { return sf.newSymbol("Ceil", sym.CEIL); }

// numbers and variables
[0-9][0-9]*\.?[0-9]* { return sf.newSymbol("Number", sym.NUMBER, new Double(yytext())); }
-[0-9][0-9]*\.?[0-9]* { return sf.newSymbol("Number", sym.NUMBER, new Double(yytext())); }
[A][T][T][0-9][0-9]* { return sf.newSymbol("Attribute", sym.ATTRIBUTE, new String(yytext())); }
"CLASS" { return sf.newSymbol("Class", sym.ATTRIBUTE, new String(yytext())); }
\'[^\'.]*\' { return sf.newSymbol("String", sym.STRING, new String(yytext().substring(1, yytext().length() - 1))); }

// whitespaces
[ \r\n\t\f] { /* ignore white space. */ }

// various
"," { return sf.newSymbol("Comma", sym.COMMA); }
"(" { return sf.newSymbol("Left Bracket", sym.LPAREN); }
")" { return sf.newSymbol("Right Bracket", sym.RPAREN); }
"ismissing" { return sf.newSymbol("Missing", sym.ISMISSING); }

// catch all
. { System.err.println("Illegal character: "+yytext()); }
