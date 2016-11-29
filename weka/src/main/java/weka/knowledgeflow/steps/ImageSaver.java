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
 * ImageSaver.java
 *
 * Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 */

package weka.knowledgeflow.steps;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import weka.core.Defaults;
import weka.core.OptionMetadata;
import weka.core.Settings;
import weka.core.WekaException;
import weka.gui.FilePropertyMetadata;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

/**
 * Step for saving static images as either png or gif.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "ImageSaver", category = "DataSinks",
  toolTipText = "Save static images to a file",
  iconPath = KFGUIConsts.BASE_ICON_PATH + "SerializedModelSaver.gif")
public class ImageSaver extends BaseStep {

  private static final long serialVersionUID = -8766164679635957891L;

  protected static enum ImageFormat {
    DEFAULT, PNG, GIF
  };

  /** The file to save to */
  protected File m_file = new File("");

  /** Default location to write to, in case a file has not been explicitly set */
  protected String m_defaultFile = "";

  /**
   * Format to save to. If set to DEFAULT, then the default format the user has
   * set in the settings for this step is used.
   */
  protected ImageFormat m_format = ImageFormat.DEFAULT;

  /**
   * Default format to use - read from the settings for this step, and used in
   * the case when the user has selected/left DEFAULT as the format type in the
   * step's options. Must not be set to the type DEFAULT of course :-)
   */
  protected ImageFormat m_defaultFormat;

  /**
   * Gets incremented by 1 for each image received during execution. Can be used
   * (via the image_count variable) to ensure that each image gets saved to a
   * different file when there are multiple images expected during execution.
   */
  protected int m_imageCounter;

  /**
   * Set the file to save to
   *
   * @param f the file to save to
   */
  @OptionMetadata(
    displayName = "File to save to",
    description = "<html>The file to save an image to<br>The variable 'image_count' may be "
      + "used as<br>part of the filename/path in order to differentiate<br>"
      + "multiple images.</html>", displayOrder = 1)
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
   * Set the format of the image to save
   *
   * @param format
   */
  @OptionMetadata(displayName = "Format to save image as",
    description = "Format to save to", displayOrder = 2)
  public void setFormat(ImageFormat format) {
    m_format = format;
  }

  /**
   * Get the format of the image to save
   *
   * @return the format of the image to save
   */
  public ImageFormat getFormat() {
    return m_format;
  }

  /**
   * Initialize the step.
   *
   * @throws WekaException if a problem occurs during initialization
   */
  @Override
  public void stepInit() throws WekaException {
    m_imageCounter = 1;
    m_defaultFile = getFile().toString();
    if (m_defaultFile == null || m_defaultFile.length() == 0) {
      File defaultF =
        getStepManager().getSettings().getSetting(ImageSaverDefaults.ID,
          ImageSaverDefaults.DEFAULT_FILE_KEY, ImageSaverDefaults.DEFAULT_FILE,
          getStepManager().getExecutionEnvironment().getEnvironmentVariables());
      m_defaultFile = defaultF.toString();
    }

    if (m_format == ImageFormat.DEFAULT) {
      m_defaultFormat =
        getStepManager().getSettings().getSetting(ImageSaverDefaults.ID,
          ImageSaverDefaults.DEFAULT_FORMAT_KEY,
          ImageSaverDefaults.DEFAULT_FORMAT,
          getStepManager().getExecutionEnvironment().getEnvironmentVariables());

      if (m_defaultFormat == ImageFormat.DEFAULT) {
        throw new WekaException("The default format to use must be something "
          + "other than 'DEFAULT'");
      }
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
    return Arrays.asList(StepManager.CON_IMAGE);
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
   * @param data the payload to process
   * @throws WekaException if a problem occurs
   */
  @Override
  public synchronized void processIncoming(Data data) throws WekaException {
    getStepManager().processing();
    ImageFormat formatToUse =
      m_format == ImageFormat.DEFAULT ? m_defaultFormat : m_format;
    BufferedImage content = data.getPrimaryPayload();
    getStepManager().getExecutionEnvironment().getEnvironmentVariables()
      .addVariable("image_count", "" + m_imageCounter++);
    String fileName = getFile().toString();
    if (fileName == null || fileName.length() == 0) {
      fileName = m_defaultFile;
    }
    fileName = environmentSubstitute(fileName);
    if (!(new File(fileName)).isDirectory()) {
      if (!fileName.toLowerCase()
        .endsWith(formatToUse.toString().toLowerCase())) {
        fileName += "." + formatToUse.toString().toLowerCase();
      }
      File file = new File(fileName);
      getStepManager().logDetailed("Writing image to " + fileName);
      try {
        ImageIO.write(content, formatToUse.toString().toLowerCase(), file);
      } catch (IOException ex) {
        throw new WekaException(ex);
      }
    } else {
      getStepManager().logWarning(
        "Unable to write image because '" + fileName + "' is a directory!");
    }
    if (!isStopRequested()) {
      getStepManager().finished();
    } else {
      getStepManager().interrupted();
    }
  }

  /**
   * Get default settings for the step.
   *
   * @return the default settings
   */
  @Override
  public Defaults getDefaultSettings() {
    return new ImageSaverDefaults();
  }

  public static final class ImageSaverDefaults extends Defaults {

    public static final String ID = "weka.knowledgeflow.steps.imagesaver";

    public static final Settings.SettingKey DEFAULT_FILE_KEY =
      new Settings.SettingKey(ID + ".defaultFile", "Default file to save to",
        "Save to this file if the user has "
          + "not explicitly set one in the step");
    public static final File DEFAULT_FILE = new File("${user.dir}/image");

    public static final Settings.SettingKey DEFAULT_FORMAT_KEY =
      new Settings.SettingKey(ID + ".defaultFormat", "Default image format to "
        + "write", "Default image format to write in the case that the user "
        + "has explicitly set 'DEFAULT' in the step's options");
    public static final ImageFormat DEFAULT_FORMAT = ImageFormat.PNG;

    private static final long serialVersionUID = -2739579935119189195L;

    public ImageSaverDefaults() {
      super(ID);
      m_defaults.put(DEFAULT_FILE_KEY, DEFAULT_FILE);
      m_defaults.put(DEFAULT_FORMAT_KEY, DEFAULT_FORMAT);
    }
  }
}
