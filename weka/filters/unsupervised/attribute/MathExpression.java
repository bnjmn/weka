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
 *    MathExpression.java
 *    Copyright (C) 2004 Prados Julien
 *    Copyright (C) 2002 Eibe Frank
 */

package weka.filters.unsupervised.attribute;

import weka.filters.*;
import java.io.*;
import java.util.*;
import weka.core.*;

/** 
 * Modify numeric attributes according to a given expression. <p/>
 *
 * Valid options are: <p/>
 * 
 * -E expression <br/>
 *  Specify the expression to apply. Eg. pow(A,6)/(MEAN+MAX) <br/>
 *  Supported operators are +, -, *, /, pow, log,
 *  abs, cos, exp, sqrt, tan, sin, ceil, floor, rint, (, ), 
 *  MEAN, MAX, MIN, SD, COUNT, SUM, SUMSQUARED, ifelse <p/>
 *
 * -R range <br/>
 *  Specify list of columns to ignore. First and last are valid
 *  indexes. (default none) <p/>
 *
 * -V <br/>
 *  Invert matching sense (i.e. only modify specified columns) <p/>
 *  
 * @author Eibe Frank (eibe@cs.waikato.ac.nz) 
 * @author Prados Julien (julien.prados@cui.unige.ch) 
 * @version $Revision: 1.3 $
 */
public class MathExpression extends PotentialClassIgnorer implements UnsupervisedFilter, OptionHandler {
  /** Stores which columns to select as a funky range */
  protected Range m_SelectCols = new Range();
    
  /** The default modification expression */
  public static final String m_defaultExpression = "(A-MIN)/(MAX-MIN)";

  /** The modification expression */
  private String m_expression = m_defaultExpression;
  
  /** The expression tree*/
  private Parser.TreeNode m_expTree = null;
  
  /** Attributes statistics*/
  private AttributeStats[] m_attStats;
  
  
  /**Constructor*/
  public MathExpression() {
      setInvertSelection(false);
  }  
  
  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "Modify numeric attributes according to a given expression ";
  }
  
  /**
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input 
   * instance structure (any instances contained in the object are 
   * ignored - only the structure is required).
   * @return true if the outputFormat may be collected immediately
   * @exception Exception if the input format can't be set 
   * successfully
   */
  public boolean setInputFormat(Instances instanceInfo) 
       throws Exception {
    m_SelectCols.setUpper(instanceInfo.numAttributes() - 1);
    super.setInputFormat(instanceInfo);
    setOutputFormat(instanceInfo);
    m_attStats = null;
    m_expTree = null;
    return true;
  }

  /**
   * Input an instance for filtering. Filter requires all
   * training instances be read before producing output.
   *
   * @param instance the input instance
   * @return true if the filtered instance may now be
   * collected with output().
   * @exception IllegalStateException if no input format has been set.
   */
  public boolean input(Instance instance) throws Exception {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }
    if (m_attStats == null) {
      bufferInput(instance);
      return false;
    } else {
      convertInstance(instance);
      return true;
    }
  }

  /**
   * Signify that this batch of input to the filter is finished. 
   * If the filter requires all instances prior to filtering,
   * output() may now be called to retrieve the filtered instances.
   *
   * @return true if there are instances pending output
   * @exception IllegalStateException if no input structure has been defined
   * @exception IllegalStateException if no input structure has been defined
   */
  public boolean batchFinished() throws Exception {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_attStats == null) {
      Instances input = getInputFormat();

      m_expTree = Parser.parse(getExpression());
      m_attStats = new AttributeStats [input.numAttributes()];
      
      for (int i = 0; i < input.numAttributes(); i++) {
	if (input.attribute(i).isNumeric() &&
	    (input.classIndex() != i)) {
	  m_attStats[i] = input.attributeStats(i);
	}
      }

      // Convert pending input instances
      for(int i = 0; i < input.numInstances(); i++) {
	convertInstance(input.instance(i));
      }
    } 
    // Free memory
    flushInput();

    m_NewBatch = true;
    return (numPendingOutput() != 0);
  }
  
  /**
   * Convert a single instance over. The converted instance is 
   * added to the end of the output queue.
   *
   * @param instance the instance to convert
   */
  private void convertInstance(Instance instance) throws Exception {
  
    Instance inst = null;
    HashMap symbols = new HashMap(5);
    if (instance instanceof SparseInstance) {
      double[] newVals = new double[instance.numAttributes()];
      int[] newIndices = new int[instance.numAttributes()];
      double[] vals = instance.toDoubleArray();
      int ind = 0;
      for (int j = 0; j < instance.numAttributes(); j++) {
        if (m_SelectCols.isInRange(j)) {          
	  double value;
	  if (instance.attribute(j).isNumeric() &&
	    (!Instance.isMissingValue(vals[j])) &&
	    (getInputFormat().classIndex() != j)) {
              symbols.put("A", new Double(vals[j]));  
              symbols.put("MAX", new Double(m_attStats[j].numericStats.max));
              symbols.put("MIN", new Double(m_attStats[j].numericStats.min));
              symbols.put("MEAN", new Double(m_attStats[j].numericStats.mean));
              symbols.put("SD", new Double(m_attStats[j].numericStats.stdDev));
              symbols.put("COUNT", new Double(m_attStats[j].numericStats.count));
              symbols.put("SUM", new Double(m_attStats[j].numericStats.sum));
              symbols.put("SUMSQUARED", new Double(m_attStats[j].numericStats.sumSq));
              value = m_expTree.eval(symbols);
              if (Double.isNaN(value) || Double.isInfinite(value)) {
                  System.err.println("WARNING:Error in evaluating the expression: missing value set");
                  value = Instance.missingValue();
              }
	      if (value != 0.0) {
	        newVals[ind] = value;
	        newIndices[ind] = j;
	        ind++;
	      }
	  } else {
	      value = vals[j];
	      if (value != 0.0) {
	        newVals[ind] = value;
	        newIndices[ind] = j;
	        ind++;
	      }
	  }
        }
      }	
      double[] tempVals = new double[ind];
      int[] tempInd = new int[ind];
      System.arraycopy(newVals, 0, tempVals, 0, ind);
      System.arraycopy(newIndices, 0, tempInd, 0, ind);
      inst = new SparseInstance(instance.weight(), tempVals, tempInd,
                                instance.numAttributes());
    } else {
      double[] vals = instance.toDoubleArray();
      for (int j = 0; j < getInputFormat().numAttributes(); j++) {
        if (m_SelectCols.isInRange(j)) {
	  if (instance.attribute(j).isNumeric() &&
	      (!Instance.isMissingValue(vals[j])) &&
	      (getInputFormat().classIndex() != j)) {
              symbols.put("A", new Double(vals[j]));  
              symbols.put("MAX", new Double(m_attStats[j].numericStats.max));
              symbols.put("MIN", new Double(m_attStats[j].numericStats.min));
              symbols.put("MEAN", new Double(m_attStats[j].numericStats.mean));
              symbols.put("SD", new Double(m_attStats[j].numericStats.stdDev));
              symbols.put("COUNT", new Double(m_attStats[j].numericStats.count));
              symbols.put("SUM", new Double(m_attStats[j].numericStats.sum));
              symbols.put("SUMSQUARED", new Double(m_attStats[j].numericStats.sumSq));
              vals[j] = m_expTree.eval(symbols);
              if (Double.isNaN(vals[j]) || Double.isInfinite(vals[j])) {
                  System.err.println("WARNING:Error in Evaluation the Expression: missing value set");
                  vals[j] = Instance.missingValue();
              }
	  }
        }
      }
      inst = new Instance(instance.weight(), vals);
    }
    inst.setDataset(instance.dataset());
    push(inst);
  }

    /**
   * Parses a list of options for this object. 
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    String expString = Utils.getOption('E', options);
    if (expString.length() != 0) {
      setExpression(expString);
    } else {
      setExpression(m_defaultExpression);
    }
    
    String ignoreList = Utils.getOption('R', options);
    if (ignoreList.length() != 0) {
      setIgnoreRange(ignoreList);
    }

    setInvertSelection(Utils.getFlag('V', options));
  }
  
  /**
   * Gets the current settings of the filter.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [5];
    int current = 0;
    
    options[current++] = "-E"; options[current++] = getExpression();
    if (getInvertSelection()) {
      options[current++] = "-V";
    }
    if (!getIgnoreRange().equals("")) {
      options[current++] = "-R"; options[current++] = getIgnoreRange();
    }
    while(current < options.length) {
      options[current++] = "";
    }
    return options;
  }
  
  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector newVector = new Vector(3);
    newVector.addElement(new Option(
	     "\tSpecify the expression to apply. Eg. pow(A,6)/(MEAN+MAX)"
	     +"\n\tSupported operators are +, -, *, /, pow, log,"
	     +"\n\tabs, cos, exp, sqrt, tan, sin, ceil, floor, rint, (, ), "
             +"\n\tMEAN, MAX, MIN, SD, COUNT, SUM, SUMSQUARED, ifelse",
	     "E",1,"-E <expression>"));
    newVector.addElement(new Option(
              "\tSpecify list of columns to ignore. First and last are valid\n"
	      +"\tindexes. (default none)",
              "R", 1, "-R <index1,index2-index4,...>"));    
    newVector.addElement(new Option(
	      "\tInvert matching sense (i.e. only modify specified columns)",
              "V", 0, "-V"));
    return newVector.elements();
  }
  
  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String expressionTipText() {
    return "Specify the expression to apply. The 'A' letter"
             + "refers to the attribute value. MIN,MAX,MEAN,SD"
             + "refer respectively to minimum, maximum, mean and"
             + "standard deviation of the attribute."
	     +"\n\tSupported operators are +, -, *, /, pow, log,"
             +"abs, cos, exp, sqrt, tan, sin, ceil, floor, rint, (, ),"
             +"A,MEAN, MAX, MIN, SD, COUNT, SUM, SUMSQUARED, ifelse"
             +"\n\tEg. pow(A,6)/(MEAN+MAX)*ifelse(A<0,0,sqrt(A))+ifelse(![A>9 && A<15])";
  }
  
  /**
   * Set the expression to apply
   * @param expr a mathematical expression to apply
   */
  public void setExpression(String expr) {
    m_expression = expr;
  }

  /**
   * Get the expression
   * @return the expression
   */
  public String getExpression() {
    return m_expression;
  }
  
    /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String invertSelectionTipText() {

    return "Determines whether action is to select or unselect."
      + " If set to true, only the specified attributes will be modified;"
      + " If set to false, specified attributes will not be modified.";
  }

  /**
   * Get whether the supplied columns are to be select or unselect
   *
   * @return true if the supplied columns will be kept
   */
  public boolean getInvertSelection() {

    return !m_SelectCols.getInvert();
  }

  /**
   * Set whether selected columns should be select or unselect. If true the 
   * selected columns are modified. If false the selected columns are not
   * modified.
   *
   * @param invert the new invert setting
   */
  public void setInvertSelection(boolean invert) {

    m_SelectCols.setInvert(!invert);
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String ignoreRangeTipText() {

    return "Specify range of attributes to act on."
      + " This is a comma separated list of attribute indices, with"
      + " \"first\" and \"last\" valid values. Specify an inclusive"
      + " range with \"-\". E.g: \"first-3,5,6-10,last\".";
  }

  /**
   * Get the current range selection.
   *
   * @return a string containing a comma separated list of ranges
   */
  public String getIgnoreRange() {

    return m_SelectCols.getRanges();
  }

  /**
   * Set which attributes are to be ignored
   *
   * @param rangeList a string representing the list of attributes.  Since
   * the string will typically come from a user, attributes are indexed from
   * 1. <br/>
   * eg: first-3,5,6-last
   */
  public void setIgnoreRange(String rangeList) {

    m_SelectCols.setRanges(rangeList);
  }
  
  
  
  /** Class to built the tree of the grammar:<br/>
   * Exp  -&gt; Term '+' Exp   <br/>
   *         | Term '-' Exp <br/>
   *         | Term         <br/>
   * Term -&gt; Atom '*' Term  <br/>
   *         | Atom '/' Term <br/>
   *         | Atom <br/>
   * Atom -&gt; &lt;number&gt; <br/>
   *         | '('Exp')' <br/>
   *         | function '(' Params ')' <br/>
   *         | '-' Atom | VARIABLE <br/>
   *         | Disjunction <br/>
   * Params -&gt; E <br/>
   *           | E,Params <br/>
   * Disjunction -&gt; Conjunction <br/>
   *                | Conjunction '|' Disjunction <br/>
   * Conjonction -&gt; NumTest <br/>
   *                | NumTest '&amp;' Conjonction <br/>
   * NumTest -&gt; Exp '&lt;' Exp | Exp '&gt;' Exp | Exp '=' Exp | '[' Disjunction ']' | '!' '[' Disjunction ']'<br/>
   */
  static public class Parser {
      
      /**A tokenizer for Math Expression*/
      static public class Tokenizer extends StreamTokenizer {
        final static int  TT_VAR = -5;
        final static int  TT_FUN = -6;
        final static int  TT_IFELSE = -7;
        
        /**Constructor*/
        public Tokenizer(Reader r) {
            super(r);
            resetSyntax();
            parseNumbers();
            whitespaceChars(' ',' ');
            wordChars('a','z');
            wordChars('A','Z');
            ordinaryChar('-');
        }
        
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
      
      /**Tree Node of Math Expression*/
      static public class TreeNode implements Serializable {
        /*The known functions and their arity*/
        static public String[] funs = {"abs", "sqrt", "log", "exp","sin","cos","tan","rint","floor","pow", "ceil"};
        static public int[] arity   = {    1,      1,     1,     1,    1,    1,    1,     1,      1,    2,      1};
        /*Node type*/
        int type;
        /*Constant value*/
        double nval;
        /*Var name*/
        String sval = null;
        /*table of operands*/
        TreeNode operands[] = null;
        
        /**Construct a constant node
         *@param v the value of the constant
         */
        TreeNode(double v) {
          type = Tokenizer.TT_NUMBER;
          nval = v;
        }

        /**Construct a constant node
         *@param v the value of the constant
         */
        TreeNode(TreeNode n) {
          type = '!';
          operands = new TreeNode[1];
          operands[0] = n;
        }
        
        /**Construct a variable node
         *@param v the name of the variable
         */
        TreeNode(String v) {
          type = Tokenizer.TT_VAR;
          sval = v;
        }

        /**Construct an ifelse node
         *@param p parameters of the ifelse
         */
        TreeNode(Vector p) {
          type = Tokenizer.TT_IFELSE;
          operands = new TreeNode[p.size()];
          for(int i=0;i<operands.length;i++) {
              operands[i] = (TreeNode) p.elementAt(i);
          }
        }
        
        /**Construct a function node
         *@param v the name of the function
         *@param ops the operands of the function
         */        
        TreeNode(String f,Vector ops) throws Exception {
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
          operands = new TreeNode[ops.size()];
          for(i=0;i<operands.length;i++) {
              operands[i] = (TreeNode) ops.elementAt(i);
          }
        }
        
        /**Construct an operator node
         *@param t the operator '+','-','*','/'
         *@param ops the operands of the operator
         */        
        TreeNode(int t,Vector ops) throws Exception {
          type = t;
          operands = new TreeNode[ops.size()];
          for(int i=0;i<operands.length;i++) {
              operands[i] = (TreeNode) ops.elementAt(i);
          }
        }
          
        /**Evaluate the tree with for specific values of the variables
         * @param symbols a map associating a Double value to each variable name
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

    /**Construct the Expression tree for a given String
     *@param exp the expression used to build the tree
     */
    public static TreeNode parse(String exp) throws Exception {
        Tokenizer tokenizer = new Tokenizer(new StringReader(exp));
        tokenizer.nextToken();
        TreeNode res = parseExp(tokenizer);
        if (tokenizer.ttype != Tokenizer.TT_EOF) {
            throw new Exception("Syntax Error: end of file expected.");
        }
        return res;
    }
    
    /**Parse the exp rule
     *@param streamTokenizer the tokenizer from which the token are extracted.
     *@return the tree of the corresponding expression
     */
    public static TreeNode parseExp(Tokenizer tokenizer) throws Exception {
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
                return (TreeNode) operands.get(0);
        }
    }    
    
    /**Parse the term rule
     *@param streamTokenizer the tokenizer from which the token are extracted.
     *@return the tree of the corresponding term
     */    
    public static TreeNode parseTerm(Tokenizer tokenizer) throws Exception {
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
                return (TreeNode) operands.get(0);
        }
    }    

    /**Parse the atom rule
     *@param streamTokenizer the tokenizer from which the token are extracted.
     *@return the tree of the corresponding atom
     */    
    public static TreeNode parseAtom(Tokenizer tokenizer) throws Exception {
        switch (tokenizer.ttype) {
            case Tokenizer.TT_NUMBER:
                TreeNode res = new TreeNode(tokenizer.nval);
                tokenizer.nextToken();
                return res;
            case '(':
                tokenizer.nextToken();
                TreeNode e = parseExp(tokenizer);
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
                TreeNode t = new TreeNode(tokenizer.sval);
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

    /**Disjunctive boolean test
     *@param streamTokenizer the tokenizer from which the token are extracted.
     *@return the tree of the corresponding Dijunction
     */    
    public static TreeNode parseDisjunction(Tokenizer tokenizer) throws Exception {
        Vector params = new Vector(2);
        params.add(parseConjunction(tokenizer));
        if (tokenizer.ttype != '|') {
            return (TreeNode) params.get(0);
        }
        tokenizer.nextToken();
        params.add(parseDisjunction(tokenizer));
        return new TreeNode('|',params);
    }

    /**Conjunction boolean test
     *@param streamTokenizer the tokenizer from which the token are extracted.
     *@return the tree of the corresponding Conjunction
     */        
    public static TreeNode parseConjunction(Tokenizer tokenizer) throws Exception {
        Vector params = new Vector(2);
        params.add(parseNumTest(tokenizer));
        if (tokenizer.ttype != '&') {
            return (TreeNode) params.get(0);
        }
        tokenizer.nextToken();
        params.add(parseConjunction(tokenizer));
        return new TreeNode('&',params);
    }

    /**Parse a numeric test
     *@param streamTokenizer the tokenizer from which the token are extracted.
     *@return the tree of the corresponding test
     */            
    public static TreeNode parseNumTest(Tokenizer tokenizer) throws Exception {
        TreeNode n;
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
  }
  
  
  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: 
   * use -h for help
   */
  public static void main(String [] argv) {

    try {
      if (Utils.getFlag('b', argv)) {
 	Filter.batchFilterFile(new MathExpression(), argv);
      } else {
	Filter.filterFile(new MathExpression(), argv);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      System.out.println(ex.getMessage());
    }
  }
}

