package weka.core;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.Map;
import java.util.Vector;

/** 
 * Class to build (and evaluate) the tree of the grammar:<br/>
 * 
 * <pre>
 * Exp  -&gt; Term '+' Exp   
 *         | Term '-' Exp 
 *         | Term         
 * Term -&gt; Atom '*' Term  
 *         | Atom '/' Term 
 *         | Atom 
 * Atom -&gt; &lt;number&gt; 
 *         | '('Exp')' 
 *         | function '(' Params ')' 
 *         | '-' Atom | VARIABLE 
 *         | Disjunction 
 * Params -&gt; E 
 *           | E,Params 
 * Disjunction -&gt; Conjunction 
 *                | Conjunction '|' Disjunction 
 * Conjunction -&gt; NumTest 
 *                | NumTest '&amp;' Conjunction 
 * NumTest -&gt; Exp '&lt;' Exp | Exp '&gt;' Exp | Exp '=' Exp | '[' Disjunction ']' | '!' '[' Disjunction ']'
 * </pre>
 *
 * Code example 1:
 * <pre>
 * String expr = "pow(BASE,EXPONENT)*MULT";
 * HashMap symbols = new HashMap();
 * symbols.put("BASE", new Double(2));
 * symbols.put("EXPONENT", new Double(9));
 * symbols.put("MULT", new Double(0.1));
 * double result = MathematicalExpression.evaluate(expr, symbols);
 * System.out.println(expr + " and " + symbols + " = " + result);
 * </pre>
 * 
 * Code Example 2 (utilizing a parse tree several times):
 * <pre>
 * String expr = "pow(BASE,EXPONENT)";
 * MathematicalExpression.TreeNode tree = MathematicalExpression.parse(expr);
 * for (int i = -10; i <= 10; i++) {
 *   HashMap symbols = new HashMap();
 *   symbols.put("BASE", new Double(2));
 *   symbols.put("EXPONENT", new Double(i));
 *   double result = MathematicalExpression.evaluate(tree, symbols);
 *   System.out.println(expr + " and " + symbols + " = " + result);
 * }
 * </pre>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz) 
 * @author Prados Julien (julien.prados@cui.unige.ch) 
 * @version $Revision: 1.1 $
 */
public class MathematicalExpression {
  
  /**
   * A tokenizer for MathematicalExpression
   *
   * @author Eibe Frank (eibe@cs.waikato.ac.nz) 
   * @author Prados Julien (julien.prados@cui.unige.ch) 
   * @version $Revision: 1.1 $
   */
  static public class Tokenizer 
    extends StreamTokenizer {
    
    /** token for a variable */
    final static int  TT_VAR = -5;
    
    /** token for a function */
    final static int  TT_FUN = -6;
    
    /** token for if-else */
    final static int  TT_IFELSE = -7;
    
    /**
     * Constructor
     * 
     * @param r		the reader to use
     */
    public Tokenizer(Reader r) {
      super(r);
      resetSyntax();
      parseNumbers();
      whitespaceChars(' ',' ');
      wordChars('a','z');
      wordChars('A','Z');
      ordinaryChar('-');
    }
    
    /**
     * returns the next token
     * 
     * @return			the next token
     * @throws IOException	if something goes wrong
     */
    public int nextToken() throws IOException {
      super.nextToken();
      if (ttype == TT_WORD) {
	if (sval.equals("ifelse")) {
	  ttype = TT_IFELSE;
	} else if (Character.isUpperCase(sval.charAt(0))) {
	  ttype = TT_VAR;
	} else {
	  ttype = TT_FUN;
	}
      }
      return ttype;
    }
  }
  
  /**
   * Tree Node of MathematicalExpression
   *
   * @author Eibe Frank (eibe@cs.waikato.ac.nz) 
   * @author Prados Julien (julien.prados@cui.unige.ch) 
   * @version $Revision: 1.1 $
   */
  static public class TreeNode 
    implements Serializable {
    
    /** for serialization */
    static final long serialVersionUID = -654720966350007711L;
    
    /** The known functions */
    static public String[] funs = {"abs", "sqrt", "log", "exp","sin","cos","tan","rint","floor","pow", "ceil"};
    
    /** The arity of the known functions */
    static public int[] arity   = {    1,      1,     1,     1,    1,    1,    1,     1,      1,    2,      1};
    
    /** Node type */
    int type;
    
    /** Constant value */
    double nval;
    
    /** Var name */
    String sval = null;
    
    /** table of operands */
    MathematicalExpression.TreeNode operands[] = null;
    
    /**
     * Construct a constant node
     * 
     * @param v the value of the constant
     */
    public TreeNode(double v) {
      type = Tokenizer.TT_NUMBER;
      nval = v;
    }
    
    /**
     * Construct a constant node
     * 
     * @param n the value of the constant
     */
    public TreeNode(MathematicalExpression.TreeNode n) {
      type = '!';
      operands = new MathematicalExpression.TreeNode[1];
      operands[0] = n;
    }
    
    /**
     * Construct a variable node
     * 
     * @param v the name of the variable
     */
    public TreeNode(String v) {
      type = Tokenizer.TT_VAR;
      sval = v;
    }
    
    /**
     * Construct an ifelse node
     * 
     * @param p parameters of the ifelse
     */
    public TreeNode(Vector p) {
      type = Tokenizer.TT_IFELSE;
      operands = new MathematicalExpression.TreeNode[p.size()];
      for(int i=0;i<operands.length;i++) {
	operands[i] = (MathematicalExpression.TreeNode) p.elementAt(i);
      }
    }
    
    /**
     * Construct a function node
     * 
     * @param f the name of the function
     * @param ops the operands of the function
     * @throws Exception if function is unknown or wrong arity
     */        
    public TreeNode(String f,Vector ops) throws Exception {
      int i = 0;
      while(i < funs.length && !funs[i].equals(f))
	i++;
      if (i >= funs.length) {
	throw new Exception("Unknow function " + f);
      }
      if (arity[i] != ops.size()) {
	throw new Exception("Wrong Number of argument in " + f);
      }
      type = Tokenizer.TT_FUN;
      nval = i;
      operands = new MathematicalExpression.TreeNode[ops.size()];
      for(i=0;i<operands.length;i++) {
	operands[i] = (MathematicalExpression.TreeNode) ops.elementAt(i);
      }
    }
    
    /**
     * Construct an operator node
     * 
     * @param t the operator '+','-','*','/'
     * @param ops the operands of the operator
     * @throws Exception is something goes wrong
     */        
    public TreeNode(int t,Vector ops) throws Exception {
      type = t;
      operands = new MathematicalExpression.TreeNode[ops.size()];
      for(int i=0;i<operands.length;i++) {
	operands[i] = (MathematicalExpression.TreeNode) ops.elementAt(i);
      }
    }
    
    /**
     * Evaluate the tree with for specific values of the variables
     * 
     * @param symbols a map associating a Double value to each variable name
     * @return the evaluation
     * @throws Exception if a symbol, function or node type is unknown
     */  
    public double eval(Map symbols) throws Exception {
      switch (type) {
	case Tokenizer.TT_NUMBER: return nval;
	case Tokenizer.TT_VAR: 
	  if (!symbols.containsKey(sval)) {
	    throw new Exception("Unknow symbol " + sval);
	  }
	  return ((Double) symbols.get(sval)).doubleValue();
	case '+': return operands[0].eval(symbols) + operands[1].eval(symbols);
	case '-': 
	  if (operands.length > 1) {
	    return operands[0].eval(symbols) - operands[1].eval(symbols);
	  } else {
	    return -operands[0].eval(symbols);
	  }
	case '*': return operands[0].eval(symbols) * operands[1].eval(symbols);
	case '/': return operands[0].eval(symbols) / operands[1].eval(symbols);
	case '>': return (operands[0].eval(symbols) > operands[1].eval(symbols))?1.0:0.0;
	case '<': return (operands[0].eval(symbols) < operands[1].eval(symbols))?1.0:0.0;
	case '=': return (operands[0].eval(symbols) == operands[1].eval(symbols))?1.0:0.0;
	case '&': return (operands[0].eval(symbols)==1.0) && (operands[1].eval(symbols)==1.0)?1.0:0.0;
	case '|': return (operands[0].eval(symbols)==1.0) || (operands[1].eval(symbols)==1.0)?1.0:0.0;
	case '!': return (operands[0].eval(symbols)==1.0)?0.0:1.0;
	case Tokenizer.TT_FUN:
	  switch ((int) nval) {
	    case 0: return Math.abs(operands[0].eval(symbols));
	    case 1: return Math.sqrt(operands[0].eval(symbols));
	    case 2: return Math.log(operands[0].eval(symbols));
	    case 3: return Math.exp(operands[0].eval(symbols));
	    case 4: return Math.sin(operands[0].eval(symbols));
	    case 5: return Math.cos(operands[0].eval(symbols));
	    case 6: return Math.tan(operands[0].eval(symbols));
	    case 7: return Math.rint(operands[0].eval(symbols));
	    case 8: return Math.floor(operands[0].eval(symbols));
	    case 9: return Math.pow(operands[0].eval(symbols),
		operands[1].eval(symbols));
	    case 10:return Math.ceil(operands[0].eval(symbols));
	    default: throw new Exception("Unknow Function");
	  }
	case Tokenizer.TT_IFELSE: return (operands[0].eval(symbols)==1.0)?operands[1].eval(symbols):operands[2].eval(symbols);
	
	default:throw new Exception("Unknow Tree Node Type.");
      }
    }
  }
  
  /**
   * Construct the Expression tree for a given String
   * 
   * @param exp the expression used to build the tree
   * @return the generated node
   * @throws Exception if EOF is not reached
   */
  public static MathematicalExpression.TreeNode parse(String exp) throws Exception {
    MathematicalExpression.Tokenizer tokenizer = new Tokenizer(new StringReader(exp));
    tokenizer.nextToken();
    MathematicalExpression.TreeNode res = parseExp(tokenizer);
    if (tokenizer.ttype != Tokenizer.TT_EOF) {
      throw new Exception("Syntax Error: end of file expected.");
    }
    return res;
  }
  
  /**
   * Parse the exp rule
   * 
   * @param tokenizer the tokenizer from which the token are extracted.
   * @return the tree of the corresponding expression
   * @throws Exception if something goes wrong
   */
  protected static MathematicalExpression.TreeNode parseExp(MathematicalExpression.Tokenizer tokenizer) throws Exception {
    Vector operands = new Vector();
    operands.add(parseTerm(tokenizer));
    switch (tokenizer.ttype) {
      case '+':
	tokenizer.nextToken();
	operands.add(parseExp(tokenizer));
	return new TreeNode('+',operands);
      case '-':
	tokenizer.nextToken();
	operands.add(parseExp(tokenizer));
	return new TreeNode('-',operands);
      default:
	return (MathematicalExpression.TreeNode) operands.get(0);
    }
  }    
  
  /**
   * Parse the term rule
   * 
   * @param tokenizer the tokenizer from which the token are extracted.
   * @return the tree of the corresponding term
   * @throws Exception if something goes wrong
   */    
  protected static MathematicalExpression.TreeNode parseTerm(MathematicalExpression.Tokenizer tokenizer) throws Exception {
    Vector operands = new Vector();
    operands.add(parseAtom(tokenizer));
    switch (tokenizer.ttype) {
      case '*':
	tokenizer.nextToken();
	operands.add(parseTerm(tokenizer));
	return new TreeNode('*',operands);
      case '/':
	tokenizer.nextToken();
	operands.add(parseTerm(tokenizer));
	return new TreeNode('/',operands);
      default:
	return (MathematicalExpression.TreeNode) operands.get(0);
    }
  }    
  
  /**
   * Parse the atom rule
   * 
   * @param tokenizer the tokenizer from which the token are extracted.
   * @return the tree of the corresponding atom
   * @throws Exception if a syntax error occurs
   */    
  protected static MathematicalExpression.TreeNode parseAtom(MathematicalExpression.Tokenizer tokenizer) throws Exception {
    switch (tokenizer.ttype) {
      case Tokenizer.TT_NUMBER:
	MathematicalExpression.TreeNode res = new TreeNode(tokenizer.nval);
	tokenizer.nextToken();
	return res;
      case '(':
	tokenizer.nextToken();
	MathematicalExpression.TreeNode e = parseExp(tokenizer);
	if (tokenizer.ttype != ')') {
	  throw new Exception("Syntax Error: ')' expected.");
	}
	tokenizer.nextToken();
	return e;
      case '-':
	tokenizer.nextToken();
	Vector operands = new Vector(1);
	operands.add(parseAtom(tokenizer));
	return new TreeNode('-',operands);
      case Tokenizer.TT_VAR:
	MathematicalExpression.TreeNode t = new TreeNode(tokenizer.sval);
	tokenizer.nextToken();
	return t;
      case Tokenizer.TT_IFELSE:
	tokenizer.nextToken();
	if (tokenizer.ttype != '(') {
	  throw new Exception("Syntax Error: '(' expected.");
	}
	tokenizer.nextToken();
	Vector par = new Vector(3);
	par.add(parseDisjunction(tokenizer));
	if (tokenizer.ttype != ',') throw new Exception("Syntax Error: ',' expected.");
	tokenizer.nextToken();
	par.add(parseExp(tokenizer));
	if (tokenizer.ttype != ',') throw new Exception("Syntax Error: ',' expected.");
	tokenizer.nextToken();
	par.add(parseExp(tokenizer));
	if (tokenizer.ttype != ')') throw new Exception("Syntax Error: ',' expected.");
	tokenizer.nextToken();
	return new TreeNode(par);            
      case Tokenizer.TT_FUN:
	String fn = tokenizer.sval;
	tokenizer.nextToken();
	if (tokenizer.ttype != '(') throw new Exception("Syntax Error: '(' expected.");
	tokenizer.nextToken();
	Vector params = new Vector(1);
	params.add(parseExp(tokenizer));
	while (tokenizer.ttype == ',') {
	  tokenizer.nextToken();
	  params.add(parseExp(tokenizer));
	}
	if (tokenizer.ttype != ')') throw new Exception("Syntax Error: ')' expected.");
	tokenizer.nextToken();
	return new TreeNode(fn,params);
      default:
	throw new Exception("Syntax Error: Unexpected token");
    }
  }
  
  /**
   * Disjunctive boolean test
   * 
   * @param tokenizer the tokenizer from which the token are extracted.
   * @return the tree of the corresponding Dijunction
   * @throws Exception if something goes wrong
   */    
  protected static MathematicalExpression.TreeNode parseDisjunction(MathematicalExpression.Tokenizer tokenizer) throws Exception {
    Vector params = new Vector(2);
    params.add(parseConjunction(tokenizer));
    if (tokenizer.ttype != '|') {
      return (MathematicalExpression.TreeNode) params.get(0);
    }
    tokenizer.nextToken();
    params.add(parseDisjunction(tokenizer));
    return new TreeNode('|',params);
  }
  
  /**
   * Conjunction boolean test
   * 
   * @param tokenizer the tokenizer from which the token are extracted.
   * @return the tree of the corresponding Conjunction
   * @throws Exception if something goes wrong
   */        
  protected static MathematicalExpression.TreeNode parseConjunction(MathematicalExpression.Tokenizer tokenizer) throws Exception {
    Vector params = new Vector(2);
    params.add(parseNumTest(tokenizer));
    if (tokenizer.ttype != '&') {
      return (MathematicalExpression.TreeNode) params.get(0);
    }
    tokenizer.nextToken();
    params.add(parseConjunction(tokenizer));
    return new TreeNode('&',params);
  }
  
  /**
   * Parse a numeric test
   * 
   * @param tokenizer the tokenizer from which the token are extracted.
   * @return the tree of the corresponding test
   * @throws Exception if brackets don't match or test unknown
   */            
  protected static MathematicalExpression.TreeNode parseNumTest(MathematicalExpression.Tokenizer tokenizer) throws Exception {
    MathematicalExpression.TreeNode n;
    switch (tokenizer.ttype) {
      case '[':
	tokenizer.nextToken();
	n = parseDisjunction(tokenizer);
	if (tokenizer.ttype != ']') {
	  throw new Exception("']' expected");
	}
	tokenizer.nextToken();
	return n;
      case '!':
	tokenizer.nextToken();
	if (tokenizer.ttype != '[') {
	  throw new Exception("'[' expected after '!'");
	}
	tokenizer.nextToken();
	n = parseDisjunction(tokenizer);
	if (tokenizer.ttype != ']') {
	  throw new Exception("']' expected");
	}
	tokenizer.nextToken();
	return new TreeNode(n);                
      default:
	Vector params = new Vector(2);
      params.add(parseExp(tokenizer));
      int op = tokenizer.ttype;
      if (op != '<' && op != '>' && op != '=') {
	throw new Exception("Unknow test " + (char) tokenizer.ttype);
      }
      tokenizer.nextToken();
      params.add(parseExp(tokenizer));
      return new TreeNode(op,params);
    }
  }
  
  /**
   * generates a new parse tree from the given expression and evalutes and
   * returns the result of a mathematical expression, based on the given 
   * values of the symbols.
   * 
   * @param expr	the expression to evaluate
   * @param symbols	the symbol/value mapping
   * @return		the evaluated result
   * @throws Exception	if something goes wrong
   */
  public static double evaluate(String expr, Map symbols) throws Exception {
    return evaluate(parse(expr), symbols);
  }
  
  /**
   * evalutes and returns the result of a mathematical expression, based on 
   * the given expression tree and values of the symbols.
   * 
   * @param parseTree	fully generated expression tree
   * @param symbols	the symbol/value mapping
   * @return		the evaluated result
   * @throws Exception	if something goes wrong
   */
  public static double evaluate(MathematicalExpression.TreeNode parseTree, Map symbols) throws Exception {
    return parseTree.eval(symbols);
  }
}
