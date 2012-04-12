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

import weka.core.parser.java_cup.runtime.ComplexSymbolFactory;
import weka.core.parser.java_cup.runtime.ComplexSymbolFactory.Location;
import weka.core.parser.java_cup.runtime.Symbol;
import java.lang.Error;
import java.io.InputStreamReader;

%%

%class Lexer
%implements sym
%public
%unicode
%line
%column
%cup
%{
    public Lexer(ComplexSymbolFactory sf){
	this(new InputStreamReader(System.in));
        symbolFactory = sf;
    }
    private StringBuffer sb;
    private ComplexSymbolFactory symbolFactory;
    private int csline,cscolumn;
    public Symbol symbol(String name, int code){
//	System.out.println("code:"+code+" "+yytext());
	return symbolFactory.newSymbol(name, code,new Location(yyline+1,yycolumn+1-yylength()),new Location(yyline+1,yycolumn+1));
    }
    public Symbol symbol(String name, int code, String lexem){
//	System.out.println("code:"+code+", lexem :"+lexem);
	return symbolFactory.newSymbol(name, code, new Location(yyline+1, yycolumn +1), new Location(yyline+1,yycolumn+yylength()), lexem);
    }
    protected void emit_warning(String message){
	ErrorManager.getManager().emit_warning("Scanner at " + (yyline+1) + "(" + (yycolumn+1) + "): " + message);
    }
    protected void emit_error(String message){
	ErrorManager.getManager().emit_error("Scanner at " + (yyline+1) + "(" + (yycolumn+1) +  "): " + message);
    }
%}

Newline = \r | \n | \r\n
Whitespace = [ \t\f] | {Newline}

/* comments */
Comment = {TraditionalComment} | {EndOfLineComment}
TraditionalComment = "/*" {CommentContent} \*+ "/"
EndOfLineComment = "//" [^\r\n]* {Newline}
CommentContent = ( [^*] | \*+[^*/] )*

ident = ([:jletter:] | "_" ) ([:jletterdigit:] | [:jletter:] | "_" )*


%eofval{
    return symbolFactory.newSymbol("EOF",sym.EOF);
%eofval}

%state CODESEG

%%  

<YYINITIAL> {

  {Whitespace}  {                                              }
  "?"           { return symbol("QESTION",QUESTION);           }
  ";"           { return symbol("SEMI",SEMI);                  }
  ","           { return symbol("COMMA",COMMA);                }
  "*"           { return symbol("STAR",STAR);                  }
  "."           { return symbol("DOT",DOT);                    }
  "|"           { return symbol("BAR",BAR);                    }
  "["           { return symbol("LBRACK",LBRACK);              }
  "]"           { return symbol("RBRACK",RBRACK);              }
  ":"           { return symbol("COLON",COLON);                }
  "::="         { return symbol("COLON_COLON_EQUALS",COLON_COLON_EQUALS);   }
  "%prec"       { return symbol("PERCENT_PREC",PERCENT_PREC);  }
  ">"           { return symbol("GT",GT);                      }
  "<"           { return symbol("LT",LT);                      }
  {Comment}     {                                              }
  "{:"          { sb = new StringBuffer(); csline=yyline+1; cscolumn=yycolumn+1; yybegin(CODESEG);    }
  "package"     { return symbol("PACKAGE",PACKAGE);            } 
  "import"      { return symbol("IMPORT",IMPORT);	       }
  "code"        { return symbol("CODE",CODE);		       }
  "action"      { return symbol("ACTION",ACTION);	       }
  "parser"      { return symbol("PARSER",PARSER);	       }
  "terminal"    { return symbol("PARSER",TERMINAL);	       }
  "non"         { return symbol("NON",NON);		       }
  "nonterminal" { return symbol("NONTERMINAL",NONTERMINAL);    }
  "init"        { return symbol("INIT",INIT);		       }
  "scan"        { return symbol("SCAN",SCAN);		       }
  "with"        { return symbol("WITH",WITH);		       }
  "start"       { return symbol("START",START);		       }
  "precedence"  { return symbol("PRECEDENCE",PRECEDENCE);      }
  "left"        { return symbol("LEFT",LEFT);		       }
  "right"       { return symbol("RIGHT",RIGHT);		       }
  "nonassoc"    { return symbol("NONASSOC",NONASSOC);          }
  "extends"     { return symbol("EXTENDS",EXTENDS);            }
  "super"       { return symbol("SUPER",SUPER);                }
  {ident}       { return symbol("ID",ID,yytext());             }
  
}

<CODESEG> {
  ":}"         { yybegin(YYINITIAL); return symbolFactory.newSymbol("CODE_STRING",CODE_STRING, new Location(csline, cscolumn),new Location(yyline+1,yycolumn+1+yylength()), sb.toString()); }
  .|\n            { sb.append(yytext()); }
}

// error fallback
.|\n          { emit_warning("Unrecognized character '" +yytext()+"' -- ignored"); }
