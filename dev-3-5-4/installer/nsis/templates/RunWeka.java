import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

/**
 * A simple class for starting Weka under Windows with parameters from
 * the props file "RunWeka.ini". <p/>
 *
 * Valid options: <p/>
 *
 * -h <br/>
 *  prints all available commands in the ini file and exits. <p/>
 *
 * -c cmd <br/>
 *  the command to use (are prefixed with "cmd_" in the inifile), 
 *  either "console" or "default". "default" starts without a command
 *  prompt, which looks nicer for a Windows application but can miss
 *  out on some exceptions that are printed to the console. "console" 
 *  on the other starts Weka from a console. <p/>
 *
 * -i inifile <br/>
 *  specifies the inifile to use, otherwise the one in the current
 *  directory is used. <p/>
 *
 * -w jar-location <br/>
 *  specifies which weka.jar to use. <p/>
 *
 * -d <br/>
 *  only for debugging purposes. <p/>
 *
 * Additional parameters will be appended to the generated java call.
 *
 * @author  FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.2 $
 */
public class RunWeka {

  /** the props file to read */
  public static final String PROPERTIES_FILE = "RunWeka.ini";

  /** the wekajar placeholder */
  public static final String PLACEHOLDER_WEKAJAR = "wekajar";

  /**
   * returns the value of the given option from the option array, empty
   * string if not found.
   *
   * @param option    the option to look for
   * @param options   the options array
   * @return          the value of the option, if found
   */
  protected static String getOption(String option, String[] options) {
    return getOption(option, options, "");
  }

  /**
   * returns the value of the given option from the option array, the default
   * string if not found.
   *
   * @param option    the option to look for
   * @param options   the options array
   * @param defValue  the default value if not found
   * @return          the value of the option, if found
   */
  protected static String getOption(
    String option, String[] options, String defValue) {

    String      result;
    int         i;

    result = defValue;

    for (i = 0; i < options.length; i++) {
      if (options[i].equals(option)) {
        if (i < options.length - 1) {
          result         = options[i + 1];
          options[i]     = "";
          options[i + 1] = "";
        }
        break;
      }
    }

    return result;
  }

  /**
   * returns true if the flag was found in the options.
   *
   * @param option    the flag to look for
   * @param options   the options array
   * @return          true if the flag was found
   */
  protected static boolean getFlag(String option, String[] options) {
    boolean     result;
    int         i;

    result = false;

    for (i = 0; i < options.length; i++) {
      if (options[i].equals(option)) {
        result     = true;
        options[i] = "";
        break;
      }
    }

    return result;
  }

  /**
   * runs Weka, "-h" displays all the available commands in RunWeka.ini
   *
   * @param args        the command line parameters
   * @throws Exception  if something goes wrong
   */
  public static void main(String[] args) throws Exception {
    boolean debug = getFlag("-d", args);
    boolean help = getFlag("-h", args);

    // other setup than default?
    String command = getOption("-c", args, "default");
    if (debug)
      System.out.println("command: " + command);

    // other weka.jar?
    String wekajar = getOption("-w", args, "weka.jar");
    wekajar = wekajar.replaceAll("\\\\", "\\\\\\\\");
    if (debug)
      System.out.println("weka.jar: " + wekajar);

    // read parameters
    String inifile = getOption(
                        "-i", args, 
                        new File(".").getAbsolutePath() + "/" + PROPERTIES_FILE);
    if (debug)
      System.out.println("inifile: " + inifile);
    Properties props = new Properties();;
    props.load(new FileInputStream(inifile));
    props.setProperty(PLACEHOLDER_WEKAJAR, wekajar);
    
    // help? -> output all commands
    if (help) {
      System.out.println("\nAvailable commands:");
      Vector commands = new Vector();
      Enumeration enm = props.propertyNames();
      while (enm.hasMoreElements()) {
        String cmd = enm.nextElement().toString();
        if (cmd.startsWith("cmd_"))
          commands.add(cmd.substring(4));
      }
      Collections.sort(commands);
      for (int i = 0; i < commands.size(); i++)
        System.out.println("  " + commands.get(i));
      System.out.println();
      System.exit(0);
    }

    // does command exist?
    String cmd = props.getProperty("cmd_" + command, "");
    if (cmd.length() == 0) {
      System.err.println(
          "Command '" + command + "' is not valid or empty, using 'default'!");
      cmd = props.getProperty("cmd_default");
    }
    if (debug)
      System.out.println("command being used: " + cmd);

    // read all placeholders
    Enumeration enm = props.propertyNames();
    Vector placeholders = new Vector();
    String name;
    while (enm.hasMoreElements()) {
      name = enm.nextElement().toString();
      // skip commands
      if (name.startsWith("cmd_"))
        continue;
      placeholders.add(name);
    }

    // build command
    String key;
    String value;
    for (int i = 0; i < placeholders.size(); i++) {
      key   = (String) placeholders.get(i);
      value = props.getProperty(key, "");
      cmd   = cmd.replaceAll("#" + key + "#", value);
    }
    for (int i = 0; i < args.length; i++) {
      if (args[i].length() != 0)
        cmd += " \"" + args[i] + "\"";
    }
    if (debug)
      System.out.println("processed command: " + cmd);

    // start Weka
    Runtime rt = Runtime.getRuntime();
    rt.exec(cmd);
  }
}
