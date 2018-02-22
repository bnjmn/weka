/* -*-Antlr-*-
********************************************************************************
*
* File:         arff.g
* RCS:          $Header: $
* Description:  ARFF format antlr v3 parser/lexer specification as given
*               on 
*      http://weka.sourceforge.net/wekadoc/index.php/en:ARFF_%283.5.1%29
*               The clean parser grammar is:
*
*               file     : header data;
*               header   : '@relation' string adecls;
*               adecls   : adecl (adecl)*;
*               adecl    : '@attribute' string datatype;
*               datatype : 'numeric'|'integer'|'real'|'string'|
*                          'relational' adecls '@end' string | date |
*                          '{' values '}';
*               date     : 'date' (string)?;
*               data     : '@data' ( (pairs)+ | (values)+ );
*               pairs    : '{' pair (',' pair)* '}';
*               pair     : INT value;
*               values   : value (',' value)*;
*               value    : '?' | FLOAT | INT | string;
*               string   : QSTRING | STRING | keyword;
*               keyword  : 'numeric' | 'integer' | 'real' | 'string' | 
*                          'relational' | 'date';
*               No semantic checking of values is done. 
*               Also, keywords are not
*               truly case insensitive (key, Key, and KEY are supported).
*               Example of use after generating python code:
*
* __all__ = ['arffstream']
*
* from antlr3 import CommonTokenStream as CTS, ANTLRInputStream as AIS
* from arffLexer import arffLexer
* from arffParser import arffParser
*
* def arffstream(fstream):
*     parser = arffParser(CTS(arffLexer(AIS(fstream))))
*     parser.file()
*     return (parser.rname, parser.sparse, parser.alist, parser.m)
* 
* if __name__ == "__main__":
*     from urllib import urlopen
*     from pprint import pprint
*     import sys
*     f = urlopen(sys.argv[1])
*     pprint(arffstream(f))
* 
* Author:       Staal Vinterbo
* Created:      Mon Jan  7 16:43:28 2008
* Modified:     Mon Jan  7 16:44:30 2008 (Staal Vinterbo) staal@peep
* Language:     Antlr/Python
* Package:      arff
* Status:       Experimental
*
* (c) Copyright 2008, Staal Vinterbo
*
* Licensed under the Apache License, Version 2.0 (the "License"); 
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at 
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, 
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
* See the License for the specific language governing permissions and 
* limitations under the License. 
*
*
********************************************************************************
*/


/***
The command:

$ awk '/^\/\// {print} /^INT/,/^WS/ {print}' arff.g | sed 's/^\/\/ //' 

will output the "clean" grammar.
*/

grammar arff;


options {
  language=Python;
}


@init {
  self.m = [];         # data 'matrix' (LoL)
  self.alist = [];     # list of (attribute_name, type_code, range/format/sub)
  self.rname = 'None'; # relation name
  self.sparse = False; # sparse arff format? (self.m contains the tuples)
  self.anyErrors = False;  # use this to check if we had any errors            
}

@members {
    def reportError(self, e):  # modified to set self.anyErrors to True
        if self.errorRecovery:
            return
        self.errorRecovery = True
        self.displayRecognitionError(self.tokenNames, e)
        self.anyErrors = True
}

// grammar arff;
// file: header data;
file
    :
        header
        data
    ;

// header: '@relation' string adecls;
header
    :
        ('@relation' | '@RELATION' | '@Relation')
        string
        {
            self.rname = $string.s;
        }
        adecls
        {
            self.alist = $adecls.atups;
        }
    ;


// adecls: adecl (adecl)*;
adecls
returns [atups]
@init {
    atups = [];
}
    :
        a1=adecl
        {
            atups.append($a1.atup);
        }
        (
            a2=adecl
            {
                atups.append($a2.atup);
            }
        )*
    ;

// adecl: '@attribute' string datatype;
adecl
returns [atup]
    :
        ('@attribute' | '@Attribute' | '@ATTRIBUTE')
        string
        datatype
        {
            $atup = ($string.s, $datatype.typecode, $datatype.nlist);
        }
    ;

// datatype: 'numeric'|'integer'|'real'|'string'|'relational' adecls '@end' string | date | '{' values '}';
/* typecode: 1 -- number, 0 - string, 2 - date, 3 - tuple (relational) */
datatype 
returns [typecode, nlist] 
@init {
	$typecode=0; 
	$nlist=[];
}
	:
        ('numeric'|'Numeric'|'NUMERIC') {$typecode=1}
    |   ('integer'|'Integer'|'INTEGER') {$typecode=1}
    |   ('real'|'Real'|'REAL')          {$typecode=1}
    |   ('string'|'String'|'STRING')  
    |   ('relational'|'Relational'|'RELATIONAL')
        adecls
        ('@end' | '@End' | '@END' )
        string
        {
            $typecode = 3;
            $nlist = $adecls.atups;
        }
    |   date
        {
            $typecode=2;
            $nlist = [$date.format];
        }
    |   '{' values '}'
        {
            $nlist = $values.v;
        }
    ;

// date: 'date' (string)?;
date
returns [format]
@init {
    $format = 'yyyy-MM-ddTHH:mm:ss';
}
    :
        ('date' | 'DATE' | 'Date')
        (
            string
            {
                $format = $string.s;
            }
        )?
    ;


// data: '@data' ( (pairs)+ | (values)+ );
data
    :
        ('@data' | '@Data' |'@DATA')
        (
            (
                pairs
                {
                    self.m.append($pairs.v);
                }
                
            )+
            {
                self.sparse = True;
            }
        |
            (
                values
                {
                    self.m.append($values.v);
                }
            )+
        )
    ;

// pairs: '{' pair (',' pair)* '}';
pairs
returns [v]
@init {
    v = [];
}
    :
        '{'
        p1=pair
        {
            v.append($p1.t);
        }
        (
            ','
            p2=pair
            {
                v.append($p2.t);
            }
        )*
        '}'
    ;

// pair: INT value;
pair
returns [t]
    :
        INT
        value
        {
            $t = (int($INT.text), $value.val);
        }
    ;

// values: value (',' value)*;
values
returns [v]
@init {
    v = [];
}
    :
        v1=value
        {
            v.append($v1.val);
        }
        (
            ','
            v2=value
            {
                v.append($v2.val);
            }
        )*
    ;

// value: '?' | FLOAT | INT | string;
value
returns [val]
@init {
    val = None;
}
    :
        '?'
        {
            $val = '?';
        } 
    |
        FLOAT
        {
            $val = float($FLOAT.text);
        } 
    |
        INT
        {
            $val = int($INT.text);
        } 
    |
        string
        {
            $val = $string.s;
        } 
    ;

// keyword: 'numeric' | 'integer' | 'real' | 'string' | 'relational' | 'date';
keyword
    :   ('numeric'|'Numeric'|'NUMERIC') 
    |   ('integer'|'Integer'|'INTEGER') 
    |   ('real'|'Real'|'REAL')          
    |   ('string'|'String'|'STRING')  
    |   ('relational'|'Relational'|'RELATIONAL')
    |   ('date' | 'DATE' | 'Date')    
    ;

// string: QSTRING | STRING | keyword;
string
returns [s]
    :
        QSTRING
        {
            $s = $QSTRING.text[1:-1];
        }
    |
        STRING
        {
            $s = $STRING.text;
        }
    |   keyword
        {
            $s = $keyword.text;
        }
    ;


/** LEXER */

INT
    : ('+'|'-')?  ('0'..'9')+
    ;


FLOAT
    : ('+'|'-')?
        (
            (('0'..'9')+)? '.' ('0'..'9')+ EXPONENT?
        |
            ('0'..'9')+ EXPONENT
        )
    ;

fragment
EXPONENT
    : ('e'|'E') ('+'|'-')? ('0'..'9')+
    ;


QSTRING
	:	('"'  (~('"'))* '"')   
	|	('\'' (~('\''))* '\'') 
    ;


/** bare string with escaped stuff in it  */
STRING
    :
        (
            ~(' '|'\r'|'\t'|'\u000C'|'\n'|','|'{'|'}'|'\''|'\"')
        |
            '\\' (','|'{'|'}'|'\''|'\"')
        )+
    ;


/** Discard whitespace and comments */
LINE_COMMENT    : '%' ~('\n'|'\r')* '\r'? '\n' {$channel=HIDDEN;}    ;
WS  :  (' '|'\r'|'\t'|'\u000C'|'\n') {$channel=HIDDEN;}; 	

