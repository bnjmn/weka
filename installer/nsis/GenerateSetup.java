import weka.core.Utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Vector;

/**
 * Generates a setup file for <a href="http://nsis.sourceforget.net"
 * target="_blank">NSIS</a>.
 *
 * @author    FracPete (fracpete at waikato dot ac dot nz)
 * @version   $Revision: 1.2 $
 */
public class GenerateSetup {

  /** the templates directory */
  public final static String TEMPLATES = "templates";

  /** the images directory */
  public final static String IMAGES = "images";

  /** the batch file for JRE */
  public final static String JRE_BATCH = "RunJREInstaller.bat";

  /** the version */
  protected String mVersion = "";

  /** the input directory */
  protected String mInputDir = "";

  /** the output directory */
  protected String mOutputDir = "";

  /** the directory for Weka (in "Program Files") */
  protected String mDir = "";

  /** the prefix for links */
  protected String mLinkPrefix = "";

  /** the jre file */
  protected String mJRE = "";

  /**
   * initializes the setup generator
   */
  public GenerateSetup() {
    super();
  }
  
  /**
   * sets the version number
   *
   * @param value       the version number
   */
  public void setVersion(String value) {
    mVersion = value;
  }
  
  /**
   * sets the input directory
   *
   * @param value       the dir
   */
  public void setInputDir(String value) {
    mInputDir = value;
  }
  
  /**
   * sets the output directory
   *
   * @param value       the dir
   */
  public void setOutputDir(String value) {
    mOutputDir = value;
  }
  
  /**
   * sets the directory for Weka used in the setup
   *
   * @param value       the dir
   */
  public void setDir(String value) {
    mDir = value;
  }
  
  /**
   * sets the link prefix
   *
   * @param value       the prefix
   */
  public void setLinkPrefix(String value) {
    mLinkPrefix = value;
  }
  
  /**
   * sets the JRE filename
   *
   * @param value       the filename
   */
  public void setJRE(String value) {
    mJRE = value;
  }

  /**
   * writes the given vector to the specified file
   *
   * @param content     the content to write
   * @param filename    the file to write to
   * @return            if writing was successful
   */
  protected static boolean writeToFile(Vector content, String filename) {
    StringBuffer    contentStr;
    int             i;

    contentStr = new StringBuffer();
    for (i = 0; i < content.size(); i++)
      contentStr.append(content.get(i).toString() + "\n");

    return writeToFile(contentStr.toString(), filename);
  }

  /**
   * writes the given content to the specified file
   *
   * @param content     the content to write
   * @param filename    the file to write to
   * @return            if writing was successful
   */
  protected static boolean writeToFile(String content, String filename) {
    BufferedWriter  writer;
    boolean         result;
    File            file;

    result = true;

    try {
      // do we need to create dir?
      file = new File(filename);
      if (!file.getParentFile().exists())
        file.getParentFile().mkdirs();
      
      // content
      writer = new BufferedWriter(new FileWriter(file));
      writer.write(content);
      writer.flush();
      writer.close();
    }
    catch (Exception e) {
      e.printStackTrace();
      result = false;
    }

    return result;
  }

  /**
   * indents the given string by count spaces and returns it
   *
   * @param line      the string to indent
   * @param count     the number of spaces
   * @return          the indented string
   */
  protected String indent(String line, int count) {
    String      result;
    int         i;

    result = line;
    for (i = 0; i < count; i++)
      result = " " + result;

    return result;
  }

  /**
   * replaces code blocks, surrounded by "# Start: identifier" and 
   * "# End: identifier" with the given content and returns the new
   * list
   *
   * @param lines       the current text
   * @param identifier  the identifier to look for
   * @param content     the new content between the start/end comment
   * @return            the new text
   */
  protected Vector replaceBlock(Vector lines, String identifier, String content) {
    Vector      result;
    String      start;
    String      end;
    int         i;
    boolean     skip;

    result = new Vector();
    start  = "# Start: " + identifier;
    end    = "# End: " + identifier;
    skip   = false;

    for (i = 0; i < lines.size(); i++) {
      if (lines.get(i).toString().indexOf(start) > -1) {
        result.add(lines.get(i).toString());
        if (content.length() == 0)
          result.add(
              indent("# removed", lines.get(i).toString().indexOf(start)));
        else
          result.add(content);
        skip = true;
        continue;
      }
      else if (lines.get(i).toString().indexOf(end) > -1) {
        result.add(lines.get(i).toString());
        skip = false;
        continue;
      }
      else {
        if (!skip)
          result.add(lines.get(i).toString());
      }
    }

    return result;
  }
  
  /**
   * loads the file and returns the lines in a Vector
   * 
   * @param filename  the file to load
   * @return          the content of the file
   */
  public Vector loadFile(String filename) {
    Vector            result;
    BufferedReader    reader;
    String            line;
    
    result = new Vector();
    
    try {
      reader = new BufferedReader(new FileReader(filename));
      while ((line = reader.readLine()) != null)
        result.add(line);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    
    return result;
  }

  /**
   * generates the setup file
   *
   * @return        true if generation was successful
   */
  protected boolean generateSetupFile() {
    Vector            setup;
    String            block;

    // load file
    setup = loadFile(TEMPLATES + "/" + "setup.nsi");

    // Weka
    block = "";
    block += "!define WEKA_WEKA \"Weka\"\n";
    block += "!define WEKA_VERSION \"" + mVersion + "\"\n";
    block += "!define WEKA_FILES \"" + new File(mInputDir).getAbsolutePath() + "\"\n";
    block += "!define WEKA_TEMPLATES \"" + new File(TEMPLATES).getAbsolutePath() + "\"\n";
    block += "!define WEKA_LINK_PREFIX \"" + mLinkPrefix + "\"\n";
    block += "!define WEKA_DIR \"" + mDir + "\"\n";
    block += "!define WEKA_URL \"http://www.cs.waikato.ac.nz/~ml/weka/\"\n";
    block += "!define WEKA_MLGROUP \"Machine Learning Group, University of Waikato, Hamilton, NZ\"\n";
    block += "!define WEKA_HEADERIMAGE \"" + new File(IMAGES + "/weka_new.bmp").getAbsolutePath() + "\"\n";
    block += "!define WEKA_JRE \"" + new File(mJRE).getAbsolutePath() + "\"\n";
    block += "!define WEKA_JRE_TEMP \"jre_setup.exe\"\n";
    block += "!define WEKA_JRE_INSTALL \"RunJREInstaller.bat\"\n";
    block += "!define WEKA_JRE_INSTALL_DONE \"RunJREInstaller.done\"\n";
    if (mJRE.length() != 0)
      block += "!define WEKA_JRE_SUFFIX \"jre\"";
    else
      block += "!define WEKA_JRE_SUFFIX \"\"";
    setup = replaceBlock(setup, "Weka", block);

    // no JRE?
    if (mJRE.length() == 0)
      setup = replaceBlock(setup, "JRE", "");

    // write file
    if (mJRE.length() != 0)
      return writeToFile(setup, mOutputDir + "/Weka-" + mVersion + "jre.nsi");
    else
      return writeToFile(setup, mOutputDir + "/Weka-" + mVersion + ".nsi");
  }

  /**
   * generates the output
   *
   * @return      true if generation was successful
   */
  public boolean execute() {
    boolean     result;

    result = true;

    if (result)
      result = generateSetupFile();

    return result;
  }

  /**
   * runs the generator with the necessary parameters.
   *
   * @param args        the commandline parameters
   * @throws Exception  if something goes wrong
   */
  public static void main(String[] args) throws Exception {
    GenerateSetup generator;

    generator = new GenerateSetup();
    generator.setVersion(Utils.getOption("version", args));
    generator.setInputDir(Utils.getOption("input-dir", args));
    generator.setOutputDir(Utils.getOption("output-dir", args));
    generator.setDir(Utils.getOption("dir", args));
    generator.setLinkPrefix(Utils.getOption("link-prefix", args));
    generator.setJRE(Utils.getOption("jre", args));
    System.out.println("Result = " + generator.execute());
  }
}
