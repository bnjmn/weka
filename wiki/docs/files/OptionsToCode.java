import weka.core.*;

/**
 * Generates code based on the provided arguments, which consist of
 * classname of a scheme and its options (outputs it to stdout). 
 * The generated code instantiates the scheme and sets its options.
 * The classname of the generated code is <code>OptionsTest</code>.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
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
