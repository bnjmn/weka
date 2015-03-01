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
 *    ArffHeaderSparkJob
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.awt.image.BufferedImage;
import java.beans.EventSetDescriptor;
import java.util.ArrayList;
import java.util.List;

import com.sun.jdi.event.EventSet;
import weka.core.Instances;

/**
 * KnowledgeFlow step for the ARFF header Spark job
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
@KFStep(category = "Spark", toolTipText = "Makes a unified ARFF header for a data set")
public class ArffHeaderSparkJob extends AbstractSparkJob {

  /** For serialization */
  private static final long serialVersionUID = 6745648560336043639L;

  /** Downstream listeners for data set output */
  protected List<DataSourceListener> m_dsListeners =
    new ArrayList<DataSourceListener>();

  /** Downstream listeners for image events */
  protected List<ImageListener> m_imageListeners =
    new ArrayList<ImageListener>();

  public ArffHeaderSparkJob() {
    super();

    m_job = new weka.distributed.spark.ArffHeaderSparkJob();
    m_visual.setText("ArffHeaderSparkJob");
  }

  @Override
  public void useDefaultVisual() {
    m_visual.loadIcons(BeanVisual.ICON_PATH + "ARFFHeaderSparkJob.gif",
            BeanVisual.ICON_PATH + "ARFFHeaderSparkJob.gif");
  }

  /**
   * Help info for this KF step
   * 
   * @return help info
   */
  public String globalInfo() {
    return "Creates a unified ARFF header for a data set by "
      + "determining column types (if not supplied by "
      + "user) and all nominal values";
  }

  @Override
  protected void notifyJobOutputListeners() {
    Instances finalHeader =
      ((weka.distributed.spark.ArffHeaderSparkJob) m_runningJob).getHeader();
    if (finalHeader != null) {
      DataSetEvent de = new DataSetEvent(this, finalHeader);
      for (DataSourceListener d : m_dsListeners) {
        d.acceptDataSet(de);
      }
    }

    List<BufferedImage> charts = ((weka.distributed.spark.ArffHeaderSparkJob)
      m_runningJob).getSummaryCharts();
    if (charts != null && charts.size() > 0) {
      for (BufferedImage i : charts) {
        ImageEvent ie = new ImageEvent(this, i);
        for (ImageListener l : m_imageListeners) {
          l.acceptImage(ie);
        }
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
        }
      }
    }
  }

  @Override
  public boolean connectionAllowed(EventSetDescriptor esd) {
    return false;
  }

  @Override
  public boolean connectionAllowed(String eventName) {
    return false;
  }

  /**
   * Add a data source listener
   * 
   * @param dsl a data source listener
   */
  public synchronized void addDataSourceListener(DataSourceListener dsl) {
    m_dsListeners.add(dsl);
  }

  /**
   * Remove a data source listener
   * 
   * @param dsl a data source listener
   */
  public synchronized void removeDataSourceListener(DataSourceListener dsl) {
    m_dsListeners.remove(dsl);
  }

  /**
   * Add an image listener
   * 
   * @param l the image listener to add
   */
  public synchronized void addImageListener(ImageListener l) {
    m_imageListeners.add(l);
  }

  /**
   * Remove an image listener
   * 
   * @param l an image listener
   */
  public synchronized void removeImageListener(ImageListener l) {
    m_imageListeners.remove(l);
  }
}
