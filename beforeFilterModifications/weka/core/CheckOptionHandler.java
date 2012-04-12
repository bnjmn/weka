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

import java.util.*;

/**
 * Simple command line checking of classes that implement OptionHandler.<p>
 *
 * Usage: <p>
 * <code>
 *     CheckOptionHandler -W optionHandlerClassName -- test options
 * </code> <p>
 *
 * Valid options are: <p>
 *
 * -W classname <br>
 * The name of a class implementing an OptionHandler. <p>
 *
 * Options after -- are used as user options in testing the
 * OptionHandler <p>
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.8 $
 */
public class CheckOptionHandler {
  
  /**
   * Prints the given options to a string.
   *
   * @param options the options to be joined
   */
  public static String printOptions(String [] options) {
    
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
   * @exception Exception if the two sets of options differ
   */
  public static void compareOptions(String [] options1, String [] options2) 
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
   * Runs some diagnostic tests on an optionhandler object. Output is
   * printed to System.out.
   *
   * @param oh the OptionHandler of interest
   * @param options an array of strings containing some test command
   * line options
   * @exception Exception if the option handler fails any of the tests.
   */
  public static void checkOptionHandler(OptionHandler oh, String []options)
    throws Exception {
    
    System.out.println("OptionHandler: " + oh.getClass().getName());
    System.out.println("ListOptions:");
    Enumeration enu = oh.listOptions();
    while (enu.hasMoreElements()) {
      Option option = (Option) enu.nextElement();
      System.out.println(option.synopsis());
      System.out.println(option.description());
    }

    // Get the default options and check that after
    // setting them the same gets returned
    String [] defaultOptions = oh.getOptions();
    System.out.print("Default options:");
    System.out.println(printOptions(defaultOptions));

    // Set some options, get them back, set them, and check 
    // the returned ones are the same as returned initially
    System.out.print("User options:");
    System.out.println(printOptions(options));
    System.out.println("Setting user options...");
    oh.setOptions(options);
    System.out.print("Remaining options:");
    System.out.println(CheckOptionHandler.printOptions(options));
    System.out.print("Getting canonical user options:");
    String [] userOptions = oh.getOptions();
    System.out.println(CheckOptionHandler.printOptions(userOptions));
    System.out.println("Setting canonical user options...");
    oh.setOptions((String [])userOptions.clone());
    System.out.print("Checking canonical user options...");
    String [] userOptionsCheck = oh.getOptions();
    CheckOptionHandler.compareOptions(userOptions, userOptionsCheck);
    System.out.println("OK");

    System.out.println("Resetting to default options...");
    oh.setOptions((String [])defaultOptions.clone());
    System.out.print("Checking default options match previous default...");
    String [] defaultOptionsCheck = oh.getOptions();
    CheckOptionHandler.compareOptions(defaultOptions, defaultOptionsCheck);
    System.out.println("OK");
  }
  
  /** 
   * Main method for using the CheckOptionHandler.<p>
   *
   * Valid options are: <p>
   *
   * -W classname <br>
   * The name of the class implementing an OptionHandler. <p>
   *
   * Options after -- are used as user options in testing the
   * OptionHandler <p>
   *
   * @param the options to the CheckOptionHandler
   */
  public static void main(String [] args) {

    try {
      String className = Utils.getOption('W', args);
      if (className.length() == 0) {
	throw new Exception("Please give a class name with -W option");
      }
      OptionHandler o;
      try {
	o = (OptionHandler)Class.forName(className).newInstance();
      } catch (Exception ex) {
	throw new Exception("Couldn't find OptionHandler with name " 
			    + className);
      }
      String [] options = Utils.partitionOptions(args);
      Utils.checkForRemainingOptions(args);

      CheckOptionHandler.checkOptionHandler(o, options);
    } catch (Exception ex) {
      System.err.println(ex.getMessage());
    }
  }
}
