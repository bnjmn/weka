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
 *    OptionsToCode.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *
 */

package wekaexamples.core;

import weka.core.OptionHandler;
import weka.core.Utils;

/**
 * Generates code based on the provided arguments, which consist of
 * classname of a scheme and its options (outputs it to stdout). 
 * The generated code instantiates the scheme and sets its options.
 * The classname of the generated code is <code>OptionsTest</code>.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class OptionsToCode {

  /**
   * Generates the code and outputs it on stdout. E.g.:<p/>
   * <code>java OptionsToCode weka.classifiers.functions.SMO -K "weka.classifiers.functions.supportVector.RBFKernel" &gt; OptionsTest.java</code>
   */
  public static void main(String[] args) throws Exception {
    // output usage
    if (args.length == 0) {
      System.err.println("\nUsage: java OptionsToCode <classname> [options] > OptionsTest.java\n");
      System.exit(1);
    }

    // instantiate scheme
    String classname = args[0];
    args[0] = "";
    Object scheme = Class.forName(classname).newInstance();
    if (scheme instanceof OptionHandler)
      ((OptionHandler) scheme).setOptions(args);

    // generate Java code
    StringBuffer buf = new StringBuffer();
    buf.append("public class OptionsTest {\n");
    buf.append("\n");
    buf.append("  public static void main(String[] args) throws Exception {\n");
    buf.append("    // create new instance of scheme\n");
    buf.append("    " + classname + " scheme = new " + classname + "();\n");
    if (scheme instanceof OptionHandler) {
      OptionHandler handler = (OptionHandler) scheme;
      buf.append("    \n");
      buf.append("    // set options\n");
      buf.append("    scheme.setOptions(weka.core.Utils.splitOptions(\"" + Utils.backQuoteChars(Utils.joinOptions(handler.getOptions())) + "\"));\n");
      buf.append("  }\n");
    }
    buf.append("}\n");

    // output Java code
    System.out.println(buf.toString());
  }
}
