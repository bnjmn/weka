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
 *    AddExpression.java
 *    Copyright (C) 2000 Mark Hall
 *
 */

package weka.filters.unsupervised.attribute;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.SparseInstance;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.StreamableFilter;
import weka.filters.UnsupervisedFilter;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * An instance filter that creates a new attribute by applying a mathematical expression to existing attributes. The expression can contain attribute references and numeric constants. Supported opperators are :  +, -, *, /, ^, log, abs, cos, exp, sqrt, floor, ceil, rint, tan, sin, (, ). Attributes are specified by prefixing with 'a', eg. a7 is attribute number 7 (starting from 1). Example expression : a1^2*a5/log(a7*4.0).
 * <p/>
 <!-- globalinfo-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -E &lt;expression&gt;
 *  Specify the expression to apply. Eg a1^2*a5/log(a7*4.0).
 *  Supported opperators: ,+, -, *, /, ^, log, abs, cos, 
 *  exp, sqrt, floor, ceil, rint, tan, sin, (, )</pre>
 * 
 * <pre> -N &lt;name&gt;
 *  Specify the name for the new attribute. (default is the expression provided with -E)</pre>
 * 
 * <pre> -D
 *  Debug. Names attribute with the postfix parse of the expression.</pre>
 * 
 <!-- options-end -->
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.4 $
 */
public class AddExpression 
  extends Filter 
  implements UnsupervisedFilter, StreamableFilter, OptionHandler {

  /** for serialization */
  static final long serialVersionUID = 402130384261736245L;
  
  /**
   * Inner class handling an attribute index as an operand
   */
  private class AttributeOperand 
    implements Serializable {
    
    /** for serialization */
    static final long serialVersionUID = -7674280127286031105L;

    /** the index of the attribute */
    protected int m_attributeIndex;

    /** true if the value of the attribute are to be multiplied by -1 */
    protected boolean m_negative;

    /**
     * Constructor
     * 
     * @param operand
     * @param sign
     * @throws Exception
     */
    public AttributeOperand(String operand, boolean sign) throws Exception {
      // strip the leading 'a' and set the index
      m_attributeIndex = (Integer.parseInt(operand.substring(1)))-1;
      m_negative = sign;
    }

    /**
     * Return a string describing this object
     * @return a string descibing the attribute operand
     */
    public String toString() {
      String result = "";
      if (m_negative) {
	result += '-';
      }
      return result+"a"+(m_attributeIndex+1);
    }
  }

  /**
   * Inner class for storing numeric constant opperands
   */
  private class NumericOperand 
    implements Serializable {
    
    /** for serialization */
    static final long serialVersionUID = 9037007836243662859L;

    /** numeric constant */
    protected double m_numericConst;

    /**
     * Constructor
     * 
     * @param operand
     * @param sign
     * @throws Exception
     */
    public NumericOperand(String operand, boolean sign) throws Exception {
      m_numericConst = Double.valueOf(operand).doubleValue();
      if (sign) {
	m_numericConst *= -1.0;
      }
    }
    
    /**
     * Return a string describing this object
     * @return a string descibing the numeric operand
     */
    public String toString() {
      return ""+m_numericConst;
    }
  }

  /**
   * Inner class for storing operators
   */
  private class Operator 
    implements Serializable {
    
    /** for serialization */
    static final long serialVersionUID = -2760353522666004638L;

    /** the operator */
    protected char m_operator;

    /**
     * Constructor
     * 
     * @param opp the operator
     */
    public Operator(char opp) {
      if (!isOperator(opp)) {
	throw new IllegalArgumentException("Unrecognized operator:" + opp);
      }
      m_operator = opp;
    }

    /**
     * Apply this operator to the supplied arguments
     * @param first the first argument
     * @param second the second argument
     * @return the result
     */
    protected double applyOperator(double first, double second) {
      switch (m_operator) {
	case '+' :
	  return (first+second);
	case '-' :
	  return (first-second);
	case '*' :
	  return (first*second);
	case '/' :
	  return (first/second);
	case '^' :
	  return Math.pow(first,second);
      }
      return Double.NaN;
    }

    /**
     * Apply this operator (function) to the supplied argument
     * @param value the argument
     * @return the result
     */
    protected double applyFunction(double value) {
      switch (m_operator) {
	case 'l' :
	  return Math.log(value);
	case 'b' :
	  return Math.abs(value);
	case 'c' :
	  return Math.cos(value);
	case 'e' :
	  return Math.exp(value);
	case 's' :
	  return Math.sqrt(value);
	case 'f' :
	  return Math.floor(value);
	case 'h' :
	  return Math.ceil(value);
	case 'r' :
	  return Math.rint(value);
	case 't' :
	  return Math.tan(value);
	case 'n' :
	  return Math.sin(value);
      }
      return Double.NaN;
    }

    /**
     * Return a string describing this object
     * @return a string descibing the operator
     */
    public String toString() {
      return ""+m_operator;
    }
  }

  /** The infix expression */
  private String m_infixExpression = "a1^2";

  /** Operator stack */
  private Stack m_operatorStack = new Stack();

  /** Supported operators. l = log, b = abs, c = cos, e = exp, s = sqrt, 
      f = floor, h = ceil, r = rint, t = tan, n = sin */
  private static final String OPERATORS = "+-*/()^lbcesfhrtn";
  /** Unary functions. l = log, b = abs, c = cos, e = exp, s = sqrt, 
      f = floor, h = ceil, r = rint, t = tan, n = sin */
  private static final String UNARY_FUNCTIONS = "lbcesfhrtn";

  /** Holds the expression in postfix form */
  private Vector m_postFixExpVector;

  /** True if the next numeric constant or attribute index is negative */
  private boolean m_signMod = false;

  /** Holds the previous token */
  private String m_previousTok = "";

  /** Name of the new attribute. "expression"  length string will use the 
      provided expression as the new attribute name */
  private String m_attributeName="expression";

  /** If true, makes the attribute name equal to the postfix parse of the
      expression */
  private boolean m_Debug = false;

  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "An instance filter that creates a new attribute by applying a "
      +"mathematical expression to existing attributes. The expression "
      +"can contain attribute references and numeric constants. Supported "
      +"opperators are :  +, -, *, /, ^, log, abs, cos, exp, sqrt, "
      +"floor, ceil, rint, tan, sin, (, ). Attributes are specified "
      +"by prefixing with 'a', eg. a7 is attribute number 7 (starting from 1)."
      +" Example expression : a1^2*a5/log(a7*4.0)."; 
  }

  /**
   * Handles the processing of an infix operand to postfix
   * @param tok the infix operand
   * @throws Exception if there is difficulty parsing the operand
   */
  private void handleOperand(String tok) throws Exception {
    // if it contains an 'a' then its an attribute index
    if (tok.indexOf('a') != -1) {
      m_postFixExpVector.addElement(new AttributeOperand(tok,m_signMod));
    } else {
      try {
	// should be a numeric constant
	m_postFixExpVector.addElement(new NumericOperand(tok, m_signMod));
      } catch (NumberFormatException ne) {
	throw new Exception("Trouble parsing numeric constant");
      }
    }
    m_signMod = false;
  }

  /**
   * Handles the processing of an infix operator to postfix
   * @param tok the infix operator
   * @throws Exception if there is difficulty parsing the operator
   */
  private void handleOperator(String tok) throws Exception {
    boolean push = true;

    char tokchar = tok.charAt(0);
    if (tokchar == ')') {
      String popop = " ";
      do {
	popop = (String)(m_operatorStack.pop());
	if (popop.charAt(0) != '(') {
	  m_postFixExpVector.addElement(new Operator(popop.charAt(0)));
	}
      } while (popop.charAt(0) != '(');
    } else {
      int infixToc = infixPriority(tok.charAt(0));
      while (!m_operatorStack.empty() && 
	     stackPriority(((String)(m_operatorStack.peek())).charAt(0)) 
	     >= infixToc) {
	
	// try an catch double operators and see if the current one can
	// be interpreted as the sign of an upcoming number
	if (m_previousTok.length() == 1 && 
	    isOperator(m_previousTok.charAt(0)) &&
	    m_previousTok.charAt(0) != ')') {
	  if (tok.charAt(0) == '-') {
	    m_signMod = true;
	  } else {
	    m_signMod = false;
	  }
	  push = false;
	  break;
	} else {
	  String popop = (String)(m_operatorStack.pop());
	  m_postFixExpVector.addElement(new Operator(popop.charAt(0)));
	}
      }
      if (m_postFixExpVector.size() == 0) {
	if (tok.charAt(0) == '-') {
	  m_signMod = true;
	  push = false;
	}
      }

      if (push) {
	m_operatorStack.push(tok);
      }
    }
  }

  /**
   * Converts a string containing a mathematical expression in infix form
   * to postfix form. The result is stored in the vector m_postfixExpVector
   * @param infixExp the infix expression to convert
   * @throws Exception if something goes wrong during the conversion
   */
  private void convertInfixToPostfix(String infixExp) throws Exception {
    infixExp = Utils.removeSubstring(infixExp, " ");
    infixExp = Utils.replaceSubstring(infixExp,"log","l");
    infixExp = Utils.replaceSubstring(infixExp,"abs","b");
    infixExp = Utils.replaceSubstring(infixExp,"cos","c");
    infixExp = Utils.replaceSubstring(infixExp,"exp","e");
    infixExp = Utils.replaceSubstring(infixExp,"sqrt","s");
    infixExp = Utils.replaceSubstring(infixExp,"floor","f");
    infixExp = Utils.replaceSubstring(infixExp,"ceil","h");
    infixExp = Utils.replaceSubstring(infixExp,"rint","r");
    infixExp = Utils.replaceSubstring(infixExp,"tan","t");
    infixExp = Utils.replaceSubstring(infixExp,"sin","n");

    StringTokenizer tokenizer = new StringTokenizer(infixExp, OPERATORS, true);
    m_postFixExpVector = new Vector();

    while (tokenizer.hasMoreTokens()) {
      String tok = tokenizer.nextToken();
      
      if (tok.length() > 1) {
	handleOperand(tok);
      } else {
	// probably an operator, but could be a single char operand
	if (isOperator(tok.charAt(0))) {
	  handleOperator(tok);
	} else {
	  // should be a numeric constant
	  handleOperand(tok);
	}
      }
      m_previousTok = tok;
    }
    while (!m_operatorStack.empty()) {
      String popop = (String)(m_operatorStack.pop());
      if (popop.charAt(0) == '(' || popop.charAt(0) == ')') {
	throw new Exception("Mis-matched parenthesis!");
      }
      m_postFixExpVector.addElement(new Operator(popop.charAt(0)));
    }
  }

  /**
   * Evaluate the expression using the supplied array of attribute values.
   * The result is stored in the last element of the array. Assumes that
   * the infix expression has been converted to postfix and stored in
   * m_postFixExpVector
   * @param vals the values to apply the expression to
   * @throws Exception if something goes wrong
   */
  private void evaluateExpression(double [] vals) throws Exception {
    Stack operands = new Stack();

    for (int i=0;i<m_postFixExpVector.size();i++) {
      Object nextob = m_postFixExpVector.elementAt(i);
      if (nextob instanceof NumericOperand) {
	operands.push(new Double(((NumericOperand)nextob).m_numericConst));
      } else if (nextob instanceof AttributeOperand) {
	double value = vals[((AttributeOperand)nextob).m_attributeIndex];
	if (value == Instance.missingValue()) {
	  vals[vals.length-1] = Instance.missingValue();
	  break;
	}
	if (((AttributeOperand)nextob).m_negative) {
	  value = -value;
	}
	operands.push(new Double(value));
      } else if (nextob instanceof Operator) {
	char op = ((Operator)nextob).m_operator;
	if (isUnaryFunction(op)) {
	  double operand = ((Double)operands.pop()).doubleValue();
	  double result = ((Operator)nextob).applyFunction(operand);
	  operands.push(new Double(result));
	} else {
	  double second = ((Double)operands.pop()).doubleValue();
	  double first = ((Double)operands.pop()).doubleValue();
	  double result = ((Operator)nextob).applyOperator(first,second);
	  operands.push(new Double(result));
	}
      } else {
	throw new Exception("Unknown object in postfix vector!");
      }
    }

    if (operands.size() != 1) {
      throw new Exception("Problem applying function");
    }

    Double result = ((Double)operands.pop());
    if (result.isNaN() || result.isInfinite()) {
      vals[vals.length-1] = Instance.missingValue();
    } else {
      vals[vals.length-1] = result.doubleValue();
    }
  }

  /**
   * Returns true if a token is an operator
   * @param tok the token to check
   * @return true if the supplied token is an operator
   */
  private boolean isOperator(char tok) {
    if (OPERATORS.indexOf(tok) == -1) {
      return false;
    }

    return true;
  }

  /**
   * Returns true if a token is a unary function
   * @param tok the token to check
   * @return true if the supplied token is a unary function
   */
  private boolean isUnaryFunction(char tok) {
    if (UNARY_FUNCTIONS.indexOf(tok) == -1) {
      return false;
    }

    return true;
  }

  /**
   * Return the infix priority of an operator
   * @param opp the operator
   * @return the infix priority
   */
  private int infixPriority(char opp) {
    switch (opp) {
      case 'l' : 
      case 'b' :
      case 'c' :
      case 'e' :
      case 's' :
      case 'f' :
      case 'h' :
      case 'r' :
      case 't' :
      case 'n' :
	return 3;
      case '^' :
	return 2;
      case '*' : 
	return 2;
      case '/' : 
	return 2;
      case '+' :
	return 1;
      case '-' :
	return 1;
      case '(' :
	return 4;
      case ')' :
	return 0;
      default :
	throw new IllegalArgumentException("Unrecognized operator:" + opp);
    }
  }

  /**
   * Return the stack priority of an operator
   * @param opp the operator
   * @return the stack priority
   */
  private int stackPriority(char opp) {
     switch (opp) {
       case 'l' :
       case 'b' :
       case 'c' :
       case 'e' :
       case 's' :
       case 'f' :
       case 'h' :
       case 'r' :
       case 't' :
       case 'n' :
	 return 3;
       case '^' :
	 return 2;
       case '*' : 
	 return 2;
       case '/' : 
	 return 2;
       case '+' :
	 return 1;
       case '-' :
	 return 1;
       case '(' :
	 return 0;
       case ')' :
	 return -1;
       default :
	 throw new IllegalArgumentException("Unrecognized operator:" + opp);
    }
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(3); 

    newVector.addElement(new Option(
	     "\tSpecify the expression to apply. Eg a1^2*a5/log(a7*4.0)."
	     +"\n\tSupported opperators: ,+, -, *, /, ^, log, abs, cos, "
	     +"\n\texp, sqrt, floor, ceil, rint, tan, sin, (, )",
	     "E",1,"-E <expression>"));

    newVector.addElement(new Option(
	     "\tSpecify the name for the new attribute. (default is the "
	     +"expression provided with -E)",
	     "N",1,"-N <name>"));

    newVector.addElement(new Option(
	     "\tDebug. Names attribute with the postfix parse of the "
	     +"expression.","D",0,"-D"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options. <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -E &lt;expression&gt;
   *  Specify the expression to apply. Eg a1^2*a5/log(a7*4.0).
   *  Supported opperators: ,+, -, *, /, ^, log, abs, cos, 
   *  exp, sqrt, floor, ceil, rint, tan, sin, (, )</pre>
   * 
   * <pre> -N &lt;name&gt;
   *  Specify the name for the new attribute. (default is the expression provided with -E)</pre>
   * 
   * <pre> -D
   *  Debug. Names attribute with the postfix parse of the expression.</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    String expString = Utils.getOption('E', options);
    if (expString.length() != 0) {
      setExpression(expString);
    } else {
      throw new Exception("Must specify an expression with the -E option");
    }

    String name = Utils.getOption('N',options);
    if (name.length() != 0) {
      setName(name);
    }

    setDebug(Utils.getFlag('D', options));
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
    options[current++] = "-N"; options[current++] = getName();

    if (getDebug()) {
      options[current++] = "-D";
    }
    
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String nameTipText() {
    return "Set the name of the new attribute.";
  }

  /**
   * Set the name for the new attribute. The string "expression" can
   * be used to make the name of the new attribute equal to the expression
   * provided.
   * @param name the name of the new attribute
   */
  public void setName(String name) {
    m_attributeName = name;
  }

  /**
   * Returns the name of the new attribute
   * @return the name of the new attribute
   */
  public String getName() {
    return m_attributeName;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String debugTipText() {
    return "Set debug mode. If true then the new attribute will be named with "
      +"the postfix parse of the supplied expression.";
  }
  
  /**
   * Set debug mode. Causes the new attribute to be named with the postfix
   * parse of the expression
   * @param d true if debug mode is to be used
   */
  public void setDebug(boolean d) {
    m_Debug = d;
  }

  /**
   * Gets whether debug is set
   * @return true if debug is set
   */
  public boolean getDebug() {
    return m_Debug;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String expressionTipText() {
    return "Set the math expression to apply. Eg. a1^2*a5/log(a7*4.0)";
  }

  /**
   * Set the expression to apply
   * @param expr a mathematical expression to apply
   */
  public void setExpression(String expr) {
    m_infixExpression = expr;
  }

  /**
   * Get the expression
   * @return the expression
   */
  public String getExpression() {
    return m_infixExpression;
  }

  /**
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input instance
   * structure (any instances contained in the object are ignored - only the
   * structure is required).
   * @return true if the outputFormat may be collected immediately
   * @throws Exception if the format couldn't be set successfully
   */
  public boolean setInputFormat(Instances instanceInfo) throws Exception {

    convertInfixToPostfix(new String(m_infixExpression));

    super.setInputFormat(instanceInfo);

    Instances outputFormat = new Instances(instanceInfo, 0);
    Attribute newAttribute;
    if (m_Debug) {
      newAttribute = new Attribute(m_postFixExpVector.toString());
    } else if (m_attributeName.compareTo("expression") != 0) {
      newAttribute = new Attribute(m_attributeName);
    } else {
      newAttribute = new Attribute(m_infixExpression);
    }
    outputFormat.insertAttributeAt(newAttribute, 
				   instanceInfo.numAttributes());
    setOutputFormat(outputFormat);
    return true;
  }

  /**
   * Input an instance for filtering. Ordinarily the instance is processed
   * and made available for output immediately. Some filters require all
   * instances be read before producing output.
   *
   * @param instance the input instance
   * @return true if the filtered instance may now be
   * collected with output().
   * @throws IllegalStateException if no input format has been defined.
   * @throws Exception if there was a problem during the filtering.
   */
  public boolean input(Instance instance) throws Exception {

    if (getInputFormat() == null) {
      throw new IllegalStateException("No input instance format defined");
    }
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }

    double[] vals = new double[instance.numAttributes()+1];
    for(int i = 0; i < instance.numAttributes(); i++) {
      if (instance.isMissing(i)) {
	vals[i] = Instance.missingValue();
      } else {
	vals[i] = instance.value(i);
      }
    }

    evaluateExpression(vals);

    Instance inst = null;
    if (instance instanceof SparseInstance) {
      inst = new SparseInstance(instance.weight(), vals);
    } else {
      inst = new Instance(instance.weight(), vals);
    }

    inst.setDataset(getOutputFormat());
    copyValues(inst, false, instance.dataset(), getOutputFormat());
    inst.setDataset(getOutputFormat());
    push(inst);
    return true;
  }
  
  /**
   * Main method for testing this class.
   *
   * @param args should contain arguments to the filter: use -h for help
   */
  public static void main(String [] args) {
    try {
      if (Utils.getFlag('b', args)) {
	Filter.batchFilterFile(new AddExpression(), args);
      } else {
	Filter.filterFile(new AddExpression(), args);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}
