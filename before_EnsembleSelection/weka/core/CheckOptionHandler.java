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
 *    CheckOptionHandler.java
 *    Copyright (C) 1999 Len Trigg
 *
 */

package weka.core;

import java.util.Enumeration;
import java.util.Vector;

/**
 * Simple command line checking of classes that implement OptionHandler.<p/>
 *
 * Usage: <p/>
 * <code>
 *     CheckOptionHandler -W optionHandlerClassName -- test options
 * </code> <p/>
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -D
 *  Turn on debugging output.</pre>
 * 
 * <pre> -S
 *  Silent mode - prints nothing to stdout.</pre>
 * 
 * <pre> -W
 *  Full name of the OptionHandler analysed.
 *  eg: weka.classifiers.rules.ZeroR</pre>
 * 
 <!-- options-end -->
 *
 * Options after -- are used as user options in testing the
 * OptionHandler
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.9 $
 */
public class CheckOptionHandler
  implements OptionHandler {

  /** additional debug information can be printed */
  protected boolean m_Debug = false;

  /** silences the testing, i.e., not output to stdout */
  protected boolean m_Silent = false;

  /** the optionhandler to test */
  protected OptionHandler m_OptionHandler = new weka.classifiers.rules.ZeroR();

  /** the user-supplied options */
  protected String[] m_UserOptions = new String[0];
  
  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector result = new Vector();
    
    result.addElement(new Option(
        "\tTurn on debugging output.",
        "D", 0, "-D"));
    
    result.addElement(new Option(
        "\tSilent mode - prints nothing to stdout.",
        "S", 0, "-S"));
    
    result.addElement(new Option(
        "\tFull name of the OptionHandler analysed.\n"
        +"\teg: weka.classifiers.rules.ZeroR",
        "W", 1, "-W"));
    
    if (m_OptionHandler != null) {
      result.addElement(new Option(
	  "", "", 0, 
	  "\nOptions specific to option handler " 
	  + m_OptionHandler.getClass().getName() + ":"));
      
      Enumeration enm = m_OptionHandler.listOptions();
      while (enm.hasMoreElements())
        result.addElement(enm.nextElement());
    }
    
    return result.elements();
  }
  
  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -D
   *  Turn on debugging output.</pre>
   * 
   * <pre> -S
   *  Silent mode - prints nothing to stdout.</pre>
   * 
   * <pre> -W
   *  Full name of the OptionHandler analysed.
   *  eg: weka.classifiers.rules.ZeroR</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    String      tmpStr;
    
    setDebug(Utils.getFlag('D', options));
    
    setSilent(Utils.getFlag('S', options));
    
    tmpStr = Utils.getOption('W', options);
    if (tmpStr.length() == 0)
      setUserOptions(new String[]{weka.classifiers.rules.ZeroR.class.getName()});
    else
      setUserOptions(Utils.partitionOptions(options));
    setOptionHandler(
	(OptionHandler) Utils.forName(
	    OptionHandler.class, tmpStr, null));
  }
  
  /**
   * Gets the current settings of the CheckClassifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    Vector        result;
    String[]      options;
    int           i;
    
    result = new Vector();
    
    if (getDebug())
      result.add("-D");
    
    if (getSilent())
      result.add("-S");
    
    if (getOptionHandler() != null) {
      result.add("-W");
      result.add(getOptionHandler().getClass().getName());
    }
    
    if (m_OptionHandler != null) {
      options = m_OptionHandler.getOptions();
      result.add("--");
      for (i = 0; i < options.length; i++)
        result.add(options[i]);
    }
    
    return (String[]) result.toArray(new String[result.size()]);
  }
  
  /**
   * Set debugging mode
   *
   * @param debug true if debug output should be printed
   */
  public void setDebug(boolean debug) {
    m_Debug = debug;
    // disable silent mode, if necessary
    if (getDebug())
      setSilent(false);
  }
  
  /**
   * Get whether debugging is turned on
   *
   * @return true if debugging output is on
   */
  public boolean getDebug() {
    return m_Debug;
  }
  
  /**
   * Set slient mode, i.e., no output at all to stdout
   *
   * @param value whether silent mode is active or not
   */
  public void setSilent(boolean value) {
    m_Silent = value;
  }
  
  /**
   * Get whether silent mode is turned on
   *
   * @return true if silent mode is on
   */
  public boolean getSilent() {
    return m_Silent;
  }
  
  /**
   * Set the OptionHandler to work on.. 
   *
   * @param value the OptionHandler to use.
   */
  public void setOptionHandler(OptionHandler value) {
    m_OptionHandler = value;
  }
  
  /**
   * Get the OptionHandler used in the tests.
   *
   * @return the OptionHandler used in the tests.
   */
  public OptionHandler getOptionHandler() {
    return m_OptionHandler;
  }

  /**
   * Sets the user-supplied options (creates a copy)
   * 
   * @param value	the user-supplied options to use
   */
  public void setUserOptions(String[] value) {
    m_UserOptions = getCopy(value);
  }
  
  /**
   * Gets the current user-supplied options (creates a copy)
   * 
   * @return		the user-supplied options
   */
  public String[] getUserOptions() {
    return getCopy(m_UserOptions);
  }
  
  /**
   * prints the given object to stdout
   * 
   * @param o		the object to print
   */
  protected void print(Object o) {
    if (!getSilent())
      System.out.print(o);
  }

  /**
   * prints the given object to stdout, plus linefeed
   * 
   * @param o		the object to print
   */
  protected void println(Object o) {
    if (!getSilent())
      System.out.println(o);
  }
  
  /**
   * Prints the given options to a string.
   *
   * @param options the options to be joined
   * @return the options as one long string
   */
  protected String printOptions(String[] options) {
    if (options == null) {
      return("<null>");
    } else {
      return Utils.joinOptions(options);
    }
  }

  /**
   * Compares the two given sets of options.
   *
   * @param options1 the first set of options
   * @param options2 the second set of options
   * @throws Exception if the two sets of options differ
   */
  protected void compareOptions(String[] options1, String[] options2) 
    throws Exception {

    if (options1 == null) {
      throw new Exception("first set of options is null!");
    }
    if (options2 == null) {
      throw new Exception("second set of options is null!");
    }
    if (options1.length != options2.length) {
      throw new Exception("problem found!\n"
			    + "First set: " + printOptions(options1) + '\n'
			    + "Second set: " + printOptions(options2) + '\n'
			    + "options differ in length");
    }
    for (int i = 0; i < options1.length; i++) {
      if (!options1[i].equals(options2[i])) {
	
	throw new Exception("problem found!\n"
			    + "\tFirst set: " + printOptions(options1) + '\n'
			    + "\tSecond set: " + printOptions(options2) + '\n'
			    + '\t' + options1[i] + " != " + options2[i]);
      }
    }
  }

  /**
   * creates a copy of the given options
   * 
   * @param options	the options to copy
   * @return		the copy
   */
  protected String[] getCopy(String[] options) {
    String[]	result;
    
    result = new String[options.length];
    System.arraycopy(options, 0, result, 0, options.length);
    
    return result;
  }
  
  /**
   * returns a new instance of the OptionHandler's class
   * 
   * @return		a new instance
   */
  protected OptionHandler getDefaultHandler() {
    OptionHandler	result;
    
    try {
      result = (OptionHandler) m_OptionHandler.getClass().newInstance();
    }
    catch (Exception e) {
      e.printStackTrace();
      result = null;
    }
    
    return result;
  }

  /**
   * returns the default options the default OptionHandler will return
   * 
   * @return		the default options
   */
  protected String[] getDefaultOptions() {
    String[]		result;
    OptionHandler	o;
    
    o = getDefaultHandler();
    if (o == null) {
      println("WARNING: couldn't create default handler, cannot use default options!");
      result = new String[0];
    }
    else {
      result = o.getOptions();
    }
    
    return result;
  }
  
  /**
   * checks whether the listOptions method works
   * 
   * @return index 0 is true if the test was passed, index 1 is always false
   */
  public boolean checkListOptions() {
    boolean	result;
    
    print("ListOptions...");
    
    try {
      Enumeration enu = getOptionHandler().listOptions();
      if (getDebug() && enu.hasMoreElements())
	println("");
      while (enu.hasMoreElements()) {
	Option option = (Option) enu.nextElement();
	if (getDebug()) {
	  println(option.synopsis());
	  println(option.description());
	}
      }

      println("yes");
      result = true;
    }
    catch (Exception e) {
      println("no");
      result = false;

      if (getDebug())
	println(e);
    }
    
    return result;
  }
  
  /**
   * checks whether the user-supplied options can be processed at all
   * 
   * @return index 0 is true if the test was passed, index 1 is always false
   */
  public boolean checkSetOptions() {
    boolean	result;
    
    print("SetOptions...");
    
    try {
      getDefaultHandler().setOptions(getUserOptions());
      println("yes");
      result = true;
    }
    catch (Exception e) {
      println("no");
      result = false;

      if (getDebug())
	println(e);
    }
    
    return result;
  }
  
  /**
   * checks whether the user-supplied options can be processed completely
   * or some "left-over" options remain
   * 
   * @return index 0 is true if the test was passed, index 1 is always false
   */
  public boolean checkRemainingOptions() {
    boolean	result;
    String[]	options;
    
    print("Remaining options...");

    options = getUserOptions();
    
    try {
      getDefaultHandler().setOptions(options);
      if (getDebug())
	println("\n  remaining: " + printOptions(options));
      println("yes");
      result = true;
    }
    catch (Exception e) {
      println("no");
      result = false;

      if (getDebug())
	println(e);
    }
    
    return result;
  }
  
  /**
   * checks whether the user-supplied options stay the same after settting,
   * getting and re-setting again
   * 
   * @return index 0 is true if the test was passed, index 1 is always false
   */
  public boolean checkCanonicalUserOptions() {
    boolean		result;
    OptionHandler	handler;
    String[] 		userOptions;
    String[] 		userOptionsCheck;
    
    print("Canonical user options...");

    try {
      handler = getDefaultHandler();
      handler.setOptions(getUserOptions());
      if (getDebug())
	print("\n  Getting canonical user options: ");
      userOptions = handler.getOptions();
      if (getDebug())
	println(printOptions(userOptions));
      if (getDebug())
	println("  Setting canonical user options");
      handler.setOptions((String[])userOptions.clone());
      if (getDebug())
	println("  Checking canonical user options");
      userOptionsCheck = handler.getOptions();
      compareOptions(userOptions, userOptionsCheck);

      println("yes");
      result = true;
    }
    catch (Exception e) {
      println("no");
      result = false;

      if (getDebug())
	println(e);
    }
    
    return result;
  }

  /**
   * checks whether the optionhandler can be re-setted again to default
   * options after the user-supplied options have been set.
   * 
   * @return index 0 is true if the test was passed, index 1 is always false
   */
  public boolean checkResettingOptions() {
    boolean		result;
    String[]		defaultOptions;
    String[] 		defaultOptionsCheck;
    OptionHandler	handler;

    print("Resetting options...");
    
    try {
      if (getDebug())
	println("\n  Setting user options");
      handler = getDefaultHandler();
      handler.setOptions(getUserOptions());
      defaultOptions = getDefaultOptions();
      if (getDebug())
	println("  Resetting to default options");
      handler.setOptions(getCopy(defaultOptions));
      if (getDebug())
	println("  Checking default options match previous default");
      defaultOptionsCheck = handler.getOptions();
      compareOptions(defaultOptions, defaultOptionsCheck);
      
      println("yes");
      result = true;
    }
    catch (Exception e) {
      println("no");
      result = false;

      if (getDebug())
	println(e);
    }
    
    return result;
  }
  
  /**
   * Runs some diagnostic tests on an optionhandler object. Output is
   * printed to System.out (if not silent).
   * 
   * @return	true if all tests passed
   */
  public boolean doTests() {
    boolean	result;    
    
    println("OptionHandler: " + m_OptionHandler.getClass().getName() + "\n");

    if (getDebug()) {
      println("--> Info");
      print("Default options: ");
      println(printOptions(getDefaultOptions()));
      print("User options: ");
      println(printOptions(getUserOptions()));
    }

    println("--> Tests");
    result = checkListOptions();

    if (result)
      result = checkSetOptions();
    
    if (result)
      result = checkRemainingOptions();

    if (result)
      result = checkCanonicalUserOptions();

    if (result)
      result = checkResettingOptions();
    
    return result;
  }
  
  /** 
   * Main method for using the CheckOptionHandler.
   *
   * @param args the options to the CheckOptionHandler
   */
  public static void main(String[] args) {
    try {
      CheckOptionHandler check = new CheckOptionHandler();
      
      try {
        check.setOptions(args);
      } 
      catch (Exception ex) {
        String result = ex.getMessage() + "\n\n" + check.getClass().getName().replaceAll(".*\\.", "") + " Options:\n\n";
        Enumeration enu = check.listOptions();
        while (enu.hasMoreElements()) {
          Option option = (Option) enu.nextElement();
          result += option.synopsis() + "\n" + option.description() + "\n";
        }
        throw new Exception(result);
      }
      
      check.doTests();
    } 
    catch (Exception ex) {
      System.err.println(ex.getMessage());
    }
  }
}
