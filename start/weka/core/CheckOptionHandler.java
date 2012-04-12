/*
 *    CheckOptionHandler.java
 *    Copyright (C) 1999 Len Trigg
 *
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

package weka.core;

import java.util.*;

/**
 * Simple command line checking of classes that implement OptionHandler.<p>
 *
 * Usage: <br>
 * <code> CheckOptionHandler -W optionHandlerClassName -- test options </code>
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version 1.0 - 26 Mar 1999 - Initial version (Len)
 */
public class CheckOptionHandler {
  
  public static void printOptions(String [] options) {
    if (options == null) {
      System.out.println("<null>");
    } else {
      for (int i = 0; i < options.length; i++) {
	if (options[i].indexOf(' ') != -1) {
	  System.out.print(" \"" + options[i] + '"');
	} else {
	  System.out.print(" " + options[i]);
	}
      }
      System.out.println("");
    }
  }

  public static void compareOptions(String [] options1, String [] options2) 
    throws Exception {

    if (options1 == null) {
      throw new Exception("first set of options is null!");
    }
    if (options2 == null) {
      throw new Exception("second set of options is null!");
    }
    if (options1.length != options2.length) {
      throw new Exception("options differ in length");
    }
    for (int i = 0; i < options1.length; i++) {
      if (!options1[i].equals(options2[i])) {
	throw new Exception(options1[i] + " != " + options2[i]);
      }
    }
  }

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

      System.out.println("OptionHandler: " + className);
      System.out.println("ListOptions:");
      Enumeration enum = o.listOptions();
      while (enum.hasMoreElements()) {
	Option option = (Option) enum.nextElement();
	System.out.println(option.synopsis());
	System.out.println(option.description());
      }

      // Get the default options and check that after
      // setting them the same gets returned
      String [] defaultOptions = o.getOptions();
      System.out.print("Default options:");
      CheckOptionHandler.printOptions(defaultOptions);

      // Set some options, get them back, set them, and check 
      // the returned ones are the same as returned initially
      System.out.print("User options:");
      CheckOptionHandler.printOptions(options);
      System.out.println("Setting user options...");
      o.setOptions(options);
      System.out.print("Remaining options:");
      CheckOptionHandler.printOptions(options);
      System.out.print("Getting canonical user options:");
      String [] userOptions = o.getOptions();
      CheckOptionHandler.printOptions(userOptions);
      System.out.println("Setting canonical user options...");
      o.setOptions((String [])userOptions.clone());
      System.out.print("Checking canonical user options...");
      String [] userOptionsCheck = o.getOptions();
      CheckOptionHandler.compareOptions(userOptions, userOptionsCheck);
      System.out.println("OK");

      System.out.println("Resetting to default options...");
      o.setOptions(new String[0]);
      System.out.print("Checking default options match previous default...");
      String [] defaultOptionsCheck = o.getOptions();
      CheckOptionHandler.compareOptions(defaultOptions, defaultOptionsCheck);
      System.out.println("OK");
    } catch (Exception ex) {
      System.err.println(ex.getMessage());
    }
  }
}
