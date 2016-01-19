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
 *    ImageViewer.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.knowledgeflow.steps;

import weka.core.WekaException;
import weka.gui.knowledgeflow.KFGUIConsts;
import weka.knowledgeflow.Data;
import weka.knowledgeflow.StepManager;

import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A step for collecting and viewing image data
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
@KFStep(name = "ImageViewer", category = "Visualization",
  toolTipText = "View images", iconPath = KFGUIConsts.BASE_ICON_PATH
    + "StripChart.gif")
public class ImageViewer extends BaseStep {

  private static final long serialVersionUID = -4055716444227948343L;

  /** Holds the received images */
  protected transient Map<String, BufferedImage> m_images =
    new LinkedHashMap<String, BufferedImage>();

  /**
   * Initialize the step. Nothing to do in the case of this step
   */
  @Override
  public void stepInit() {
    // nothing to do
  }

  /**
   * Get a list of acceptable incoming connections - only StepManager.CON_IMAGE
   * in this case
   * 
   * @return a list of acceptable incoming connections
   */
  @Override
  public List<String> getIncomingConnectionTypes() {
    return Arrays.asList(StepManager.CON_IMAGE);
  }

  /**
   * Get a list of outgoing connections that can be generated given the current
   * state of the step - will produce StepManager.CON_IMAGE data if we have at
   * least one incoming image connection connection
   * 
   * @return a list of outgoing connections
   */
  @Override
  public List<String> getOutgoingConnectionTypes() {
    return getStepManager().numIncomingConnectionsOfType(StepManager.CON_IMAGE) > 0 ? Arrays
      .asList(StepManager.CON_IMAGE) : new ArrayList<String>();
  }

  /**
   * Process incoming image data
   *
   * @param data the data to process
   * @throws WekaException if a problem occurs
   */
  @Override
  public synchronized void processIncoming(Data data) throws WekaException {
    getStepManager().processing();
    String imageTitle =
      data.getPayloadElement(StepManager.CON_AUX_DATA_TEXT_TITLE);
    BufferedImage image = data.getPrimaryPayload();

    if (image == null) {
      throw new WekaException("Data does not seem to contain an image!");
    }

    String date =
      (new SimpleDateFormat("HH:mm:ss.SSS - ")).format(new Date());
    if (imageTitle != null) {
      imageTitle = date + imageTitle;
    } else {
      imageTitle = date + "Untitled";
    }

    m_images.put(imageTitle, image);

    getStepManager().logDetailed("Storing image: " + imageTitle);

    // pass on downstream
    getStepManager().outputData(data);
    getStepManager().finished();
  }

  public Map<String, BufferedImage> getImages() {
    return m_images;
  }

  /**
   * Gets a list of classes of viewers that can be popped up in the GUI
   * Knowledge Flow from this step, given that we have received and stored
   * some image data.
   *
   * @return a list of viewer classes
   */
  @Override
  public Map<String, String> getInteractiveViewers() {
    Map<String, String> views = new LinkedHashMap<String, String>();

    if (m_images.size() > 0) {
      views.put("Show images",
        "weka.gui.knowledgeflow.steps.ImageViewerInteractiveView");
    }

    return views;
  }
}
