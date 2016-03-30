/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * TextSaver.java
 *
 * Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 */

package weka.knowledgeflow.steps;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

import weka.core.Defaults;
import weka.core.OptionMetadata;
import weka.core.Settings;
import weka.core.WekaException;
import weka.gui.FilePropertyMetadata;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

/**
 * Step for saving textual data to a file.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "TextSaver", category = "DataSinks",
  toolTipText = "Save text output to a file",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "DefaultText.gif")
public class TextSaver extends BaseStep {

  private static final long serialVersionUID = -1434752243260858338L;

  /** The file to save to */
  protected File m_file = new File("");

  /** Whether to append to the file or not */
  protected boolean m_append = true;

  /** Whether to write the title string for each textual result too */
  protected boolean m_writeTitleString;

  /** Default location to write to, in case a file has not been explicitly set */
  protected String m_defaultFile = "";

  /**
   * Set the file to save to
   *
   * @param f the file to save to
   */
  @OptionMetadata(displayName = "File to save to",
    description = "The file to save textual results to", displayOrder = 1)
  @FilePropertyMetadata(fileChooserDialogType = KFGUIConsts.OPEN_DIALOG,
    directoriesOnly = false)
  public void setFile(File f) {
    m_file = f;
  }

  /**
   * Get the file to save to
   *
   * @return the file to save to
   */
  public File getFile() {
    return m_file;
  }

  /**
   * Set whether the file should be appended to rather than overwritten
   *
   * @param append true to append
   */
  @OptionMetadata(displayName = "Append to file",
    description = "Append to file, rather than re-create for each incoming "
      + "texual result", displayOrder = 2)
  public void setAppend(boolean append) {
    m_append = append;
  }

  /**
   * get whether the file should be appended to rather than overwritten
   *
   * @return true if the file will be appended to
   */
  public boolean getAppend() {
    return m_append;
  }

  /**
   * Set whether the title string will be written to the file
   *
   * @param w true to write the title string
   */
  @OptionMetadata(displayName = "Write title string",
    description = "Whether to output the title string associated "
      + "with each textual result", displayOrder = 3)
  public void setWriteTitleString(boolean w) {
    m_writeTitleString = w;
  }

  /**
   * Get whether the title string will be written to the file
   *
   * @return true if the title string will be written
   */
  public boolean getWriteTitleString() {
    return m_writeTitleString;
  }

  /**
   * Initialize the step
   *
   * @throws WekaException if a problem occurs
   */
  @Override
  public void stepInit() throws WekaException {
    m_defaultFile = getFile().toString();
    if (m_defaultFile == null || m_defaultFile.length() == 0) {
      File defaultF =
        getStepManager().getSettings().getSetting(TextSaverDefaults.ID,
          TextSaverDefaults.DEFAULT_FILE_KEY, TextSaverDefaults.DEFAULT_FILE,
          getStepManager().getExecutionEnvironment().getEnvironmentVariables());
      m_defaultFile = defaultF.toString();
    }
  }

  /**
   * Get a list of incoming connection types that this step can accept. Ideally
   * (and if appropriate), this should take into account the state of the step
   * and any existing incoming connections. E.g. a step might be able to accept
   * one (and only one) incoming batch data connection.
   *
   * @return a list of incoming connections that this step can accept given its
   *         current state
   */
  @Override
  public List<String> getIncomingConnectionTypes() {
    return Arrays.asList(StepManager.CON_TEXT);
  }

  /**
   * Get a list of outgoing connection types that this step can produce. Ideally
   * (and if appropriate), this should take into account the state of the step
   * and the incoming connections. E.g. depending on what incoming connection is
   * present, a step might be able to produce a trainingSet output, a testSet
   * output or neither, but not both.
   *
   * @return a list of outgoing connections that this step can produce
   */
  @Override
  public List<String> getOutgoingConnectionTypes() {
    return null;
  }

  /**
   * Process an incoming data payload (if the step accepts incoming connections)
   *
   * @param data the data to process
   * @throws WekaException if a problem occurs
   */
  @Override
  public synchronized void processIncoming(Data data) throws WekaException {
    getStepManager().processing();
    String content = data.getPrimaryPayload();
    String title = data.getPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE);
    String fileName = getFile().toString();
    if (fileName == null || fileName.length() == 0) {
      fileName = m_defaultFile;
    }
    fileName = environmentSubstitute(fileName);

    if (title != null && title.length() > 0) {
      title = environmentSubstitute(title);
    } else {
      title = null;
    }

    if (!(new File(fileName)).isDirectory()) {
      if (!fileName.toLowerCase().endsWith(".txt")) {
        fileName += ".txt";
      }
      File file = new File(fileName);

      getStepManager().logDetailed(
        "Writing " + (title != null ? title : "file to " + file.toString()));
      Writer writer = null;
      try {
        writer =
          new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file,
            m_append), "utf-8"));
        if (title != null && getWriteTitleString()) {
          writer.write(title + "\n\n");
        }
        writer.write(content);
      } catch (IOException e) {
        throw new WekaException(e);
      } finally {
        if (writer != null) {
          try {
            writer.flush();
            writer.close();
          } catch (IOException e) {
            throw new WekaException(e);
          }
        }
      }
    } else {
      getStepManager().logWarning(
        "Supplied file is a directory! Unable to write.");
    }

    if (!isStopRequested()) {
      getStepManager().finished();
    } else {
      getStepManager().interrupted();
    }
  }

  /**
   * Get default settings for the step (if any). Returning null indicates that
   * the step has no user-editable defaults.
   *
   * @return the default settings
   */
  @Override
  public Defaults getDefaultSettings() {
    return new TextSaverDefaults();
  }

  /**
   * Defaults for the {@TextSaver} step
   */
  public static final class TextSaverDefaults extends Defaults {

    public static final String ID = "weka.knowledgeflow.steps.textsaver";

    public static final Settings.SettingKey DEFAULT_FILE_KEY =
      new Settings.SettingKey(ID + ".defaultFile", "Default file to save to",
        "Save to this file if the user has "
          + "not explicitly set one in the step");
    public static final File DEFAULT_FILE = new File("${user.dir}/textout.txt");

    private static final long serialVersionUID = -2739579935119189195L;

    /**
     * Constructor
     */
    public TextSaverDefaults() {
      super(ID);
      m_defaults.put(DEFAULT_FILE_KEY, DEFAULT_FILE);
    }
  }
}
