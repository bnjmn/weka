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
 * Generates a setup file for <a href="http://www.jrsoftware.org/isdl.php"
 * target="_blank">Inno Setup</a> and copies also all the necessary files to
 * the output.
 *
 * @author    FracPete (fracpete at waikato dot ac dot nz)
 * @version   $Revision: 1.1 $
 */
public class GenerateSetup {

  /** the templates directory */
  public final static String TEMPLATES = "templates";

  /** the batch file for JRE */
  public final static String JRE_BATCH = "RunJREInstaller.bat";

  /** the version */
  protected String mVersion = "";

  /** the input directory */
  protected String mInputDir = "";

  /** the output directory */
  protected String mOutputDir = "";

  /** the directory for Weka */
  protected String mDir = "";

  /** the link in the Start Program */
  protected String mLink = "";

  /** the jre file */
  protected String mJRE = "";

  /** whether to clear the output dir before generating */
  protected boolean mClear = false;

  /** whether to generate the scripts only */
  protected boolean mScriptOnly = false;

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
   * sets the name of the link
   *
   * @param value       the name
   */
  public void setLink(String value) {
    mLink = value;
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
   * sets whether to clear the output directory first
   *
   * @param value       true if output directory is cleared first
   */
  public void setClear(boolean value) {
    mClear = value;
  }
  
  /**
   * sets whether to generate the inno script only
   *
   * @param value       if true, only the inno script will be generated
   */
  public void setScriptOnly(boolean value) {
    mScriptOnly = value;
  }

  /**
   * Copies src file to dst file.
   * If the dst file does not exist, it is created.
   * Taken from here: http://javaalmanac.com/egs/java.io/CopyFile.html
   *
   * @param src         the source file
   * @param dst         the destination file
   * @throws Exception  if something goes wrong
   */
  protected void copy(File src, File dst) throws IOException {
    InputStream in = new FileInputStream(src);
    OutputStream out = new FileOutputStream(dst);

    // Transfer bytes from in to out
    byte[] buf = new byte[1024];
    int len;
    while ((len = in.read(buf)) > 0) {
      out.write(buf, 0, len);
    }
    in.close();
    out.close();
  }

  /**
   * lists the files in a directory as array.
   * Taken from here: http://www.bigbold.com/snippets/posts/show/1875
   *
   * @param directory     the directory to search
   * @param filter        an optional filter
   * @param recurse       if true then we recurse into sub-dirs
   * @return              the files
   */
  protected static File[] listFilesAsArray(File directory, 
      FilenameFilter filter, boolean recurse) {

    Collection<File> files = listFiles(directory, filter, recurse);

    File[] arr = new File[files.size()];
    return files.toArray(arr);
  }

  /**
   * lists the files in a directory.
   * Taken from here: http://www.bigbold.com/snippets/posts/show/1875
   *
   * @param directory     the directory to search
   * @param filter        an optional filter
   * @param recurse       if true then we recurse into sub-dirs
   * @return              the files
   */
  protected static Collection<File> listFiles(File directory, 
      FilenameFilter filter, boolean recurse) {

    // List of files / directories
    Vector<File> files = new Vector<File>();

    // Get files / directories in the directory
    File[] entries = directory.listFiles();

    // Go over entries
    for (File entry : entries) {
      // If there is no filter or the filter accepts the 
      // file / directory, add it to the list
      if (filter == null || filter.accept(directory, entry.getName())) {
        files.add(entry);
      }

      // If the file is a directory and the recurse flag
      // is set, recurse into the directory
      if (recurse && entry.isDirectory()) {
        files.addAll(listFiles(entry, filter, recurse));
      }
    }

    // Return collection of files
    return files;		
  }

  /**
   * returns only directories from the collection
   *
   * @param list        the list with files and dirs
   * @return            contains only dirs
   */
  protected static Collection<File> getDirectories(Collection<File> list) {
    Vector<File>    result;

    result = new Vector<File>();

    for (File file : list) {
      if (file.isDirectory())
        result.add(file);
    }

    return result;
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
   * removes the input directory from the given filename
   *
   * @param filename  the file to work on
   * @return          the filename without the input dir
   */
  protected String removeInputDir(String filename) {
    File    inputDir;
    File    inFile;

    inputDir  = new File(mInputDir);
    inFile    = new File(filename);

    return inFile.getPath().substring(inputDir.getPath().length());
  }

  /**
   * moves the given file from the input dir to the output dir.
   * 
   * @param filename  the filename in the input dir to process
   * @return          the file as it should appear in the output dir
   */
  protected String inputToOutput(String filename) {
    File    outputDir;
    File    outFile;

    outputDir = new File(mOutputDir);
    outFile   = new File(outputDir.getPath() + "\\" + removeInputDir(filename));

    return outFile.getPath();
  }

  /**
   * checks whether the directory contains any files at all
   *
   * @return        true if at least one file is found
   */
  protected boolean containsFiles(File dir) {
    boolean       result;
    File[]        list;
    int           i;

    result = false;

    list   = dir.listFiles();
    for (i = 0; i < list.length; i++) {
      if (!list[i].isDirectory()) {
        result = true;
        break;
      }
    }

    return result;
  }

  /**
   * removes the files in the output directory, if necessary
   *
   * @return          true if the removal was successful or not necessary
   */
  protected boolean clear() {
    boolean           result;
    File              output;
    Collection<File>  files;

    result = true;

    output = new File(mOutputDir);
    if (output.exists() && output.isDirectory()) {
      // delete files 
      files = listFiles(output, null, true);
      for (File file : files) {
        if (file.isDirectory())
          continue;
        else
          file.delete();
      }
      
      // delete dirs
      files = listFiles(output, null, true);
      for (File file : files)
        file.delete();
    }

    return result;
  }

  /**
   * copies all the files
   *
   * @return        true if copying was successful
   */
  protected boolean copyFiles() {
    Collection<File>    files;
    boolean             result;
    File                outFile;
    String              prefix;

    result = true;

    try {
      prefix = mOutputDir + "/" + new File(mOutputDir).getName();
      files  = listFiles(new File(mInputDir), null, true);
      for (File file : files) {
        outFile = new File(prefix + "/" + removeInputDir(file.getPath()));

        if (file.isDirectory()) {
          if (!outFile.exists())
            outFile.mkdirs();
        }
        else {
          if (!outFile.getParentFile().exists())
            outFile.getParentFile().mkdirs();
          copy(file, outFile);
        }
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      result = false;
    }

    return result;
  }

  /**
   * copies the JRE
   *
   * @return        true if copying was successful
   */
  protected boolean copyJRE() {
    File      file;
    boolean   result;

    result = true;

    try {
      file = new File(mJRE);
      copy(file, new File(mOutputDir + "/" + file.getName()));
    }
    catch (Exception e) {
      e.printStackTrace();
      result = false;
    }

    return result;
  }

  /**
   * copies all the templates
   *
   * @return        true if copying was successful
   */
  protected boolean copyTemplates() {
    Collection<File>    files;
    boolean             result;

    result = true;

    try {
      files  = listFiles(new File(TEMPLATES), null, false);
      for (File file : files)
        copy(file, new File(mOutputDir + "/" + file.getName()));
    }
    catch (Exception e) {
      e.printStackTrace();
      result = false;
    }

    return result;
  }

  /**
   * generates the batch file for installing the JRE
   *
   * @return        true if generation was successful
   */
  protected boolean generateJreFile() {
    Vector      setup;

    setup = new Vector();

    setup.add(new File(mJRE).getName());
    setup.add("del " + new File(mJRE).getName());

    // write file
    return writeToFile(setup, mOutputDir + "/" + JRE_BATCH);
  }

  /**
   * generates the setup file
   *
   * @return        true if generation was successful
   */
  protected boolean generateSetupFile() {
    Vector            setup;
    Collection<File>  files;

    // generate file
    setup = new Vector();
    setup.add("; -- Setup for Weka " + mVersion + " --");

    // Setup
    setup.add("");
    setup.add("[Setup]");
    setup.add("AppName=WEKA");
    setup.add("AppVerName=WEKA " + mVersion);
    setup.add("DefaultDirName={pf}\\" + mDir);
    setup.add("DefaultGroupName=WEKA");
    setup.add("ChangesAssociations=yes");
    setup.add("LicenseFile=" + new File(mOutputDir).getName() + "\\COPYING");
    if (mJRE.length() != 0)
      setup.add("MessagesFile=DEFAULT.JRE.ISL");
    else
      setup.add("MessagesFile=DEFAULT.ISL");

    // Dirs
    setup.add("");
    setup.add("[Dirs]");
    files = listFiles(new File(mInputDir), null, true);
    for (File file : files) {
      if (!file.isDirectory())
        continue;
      setup.add("Name: \"{app}" + removeInputDir(file.getPath()) + "\"");
    }

    // Files
    setup.add("");
    setup.add("[Files]");
    setup.add(
        "Source: \"" + new File(mOutputDir).getName() + "\\*\"; "
        + "DestDir: \"{app}\"");
    files = listFiles(new File(mInputDir), null, true);
    for (File file : files) {
      if (!file.isDirectory() || !containsFiles(file))
        continue;
      setup.add(
          "Source: \"" + new File(mOutputDir).getName() 
          + removeInputDir(file.getPath()) + "\\*\"; "
          + "DestDir: \"{app}" + removeInputDir(file.getPath()) + "\"");
    }
    if (mJRE.length() != 0) {
      setup.add("Source: \"" + new File(mJRE).getName() + "\"; DestDir: \"{app}\"");
      setup.add("Source: \"RunJREInstaller.bat\"; DestDir: \"{app}\"");
    }
    setup.add("Source: \"RunWeka.bat\"; DestDir: \"{app}\"");
    setup.add("Source: \"weka.ico\"; DestDir: \"{app}\"");

    // Icons
    setup.add("");
    setup.add("[Icons]");
    setup.add("Name: \"{group}\\" + mLink + "\"; Filename: \"{app}\\RunWeka.bat\"; WorkingDir: \"{app}\"; IconFilename: \"{app}\\weka.ico\"");
    setup.add("Name: \"{group}\\Explorer Guide\"; Filename: \"{app}\\ExplorerGuide.pdf\"");
    setup.add("Name: \"{group}\\Package Documentation\"; Filename: \"{app}\\doc\\index.html\"");
    setup.add("Name: \"{group}\\Uninstall\"; Filename: \"{app}\\unins000.exe\"");

    // Run
    if (mJRE.length() != 0) {
      setup.add("");
      setup.add("[Run]");
      setup.add("FileName: \"{app}\\RunJREInstaller.bat\"; WorkingDir: \"{app}\"");
    }

    // Registry
    setup.add("");
    setup.add("[Registry]");
    setup.add("Root: HKCR; Subkey: \".arff\"; ValueType: string; ValueName: \"\"; ValueData: \"ARFFDataFile\"; Flags: uninsdeletevalue");
    setup.add("Root: HKCR; Subkey: \"ARFFDataFile\"; ValueType: string; ValueName: \"\"; ValueData: \"ARFF Data File\"; Flags: uninsdeletekey");
    setup.add("Root: HKCR; Subkey: \"ARFFDataFile\\DefaultIcon\"; ValueType: string; ValueName: \"\"; ValueData: \"{app}\\weka.ico\"");
    setup.add("Root: HKCR; Subkey: \"ARFFDataFile\\shell\\open\\command\"; ValueType: string; ValueName: \"\"; ValueData: \"\"\"java.exe\"\" \"\"-cp\"\" \"\"{app}\\weka.jar\"\" \"\"weka.gui.explorer.Explorer\"\" \"\"%1\"\"\"");

    // write file
    if (mJRE.length() != 0)
      return writeToFile(setup, mOutputDir + "/" + mDir + "jre.iss");
    else
      return writeToFile(setup, mOutputDir + "/" + mDir + ".iss");
  }

  /**
   * generates the output
   *
   * @return      true if generation was successful
   */
  public boolean execute() {
    boolean     result;

    result = true;

    if (!mScriptOnly) {
      if (mClear)
        result = clear();

      if (result)
        result = copyFiles();

      if (result)
        result = copyTemplates();

      if (result && (mJRE.length() != 0)) {
        result = copyJRE();
        if (result)
          result = generateJreFile();
      }
    }

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
    generator.setLink(Utils.getOption("link", args));
    generator.setJRE(Utils.getOption("jre", args));
    generator.setClear(Utils.getFlag("clear", args));
    generator.setScriptOnly(Utils.getFlag("scriptonly", args));
    System.out.println("Result = " + generator.execute());
  }
}
