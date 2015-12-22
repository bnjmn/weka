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
 *    StepVisual.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow;

import weka.core.WekaException;
import weka.knowledgeflow.StepManagerImpl;
import weka.knowledgeflow.steps.KFStep;
import weka.knowledgeflow.steps.Note;
import weka.knowledgeflow.steps.Step;
import weka.knowledgeflow.steps.WekaAlgorithmWrapper;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.beans.Beans;
import java.util.Map;
import java.util.Set;

/**
 * Class for managing the appearance of a step in the GUI Knowledge Flow
 * environment.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class StepVisual extends JPanel {

  public static final String BASE_ICON_PATH = KFGUIConsts.BASE_ICON_PATH;

  /**
   * For serialization
   */
  private static final long serialVersionUID = 4156046438296843760L;

  /** The x coordinate of the step on the graphical layout */
  protected int m_x;

  /** The y coordinate of the step on the graphical layout */
  protected int m_y;

  /** The icon for the step this visual represents */
  protected ImageIcon m_icon;

  /** Whether to display connector dots */
  protected boolean m_displayConnectors;

  protected Color m_connectorColor = Color.blue;

  /**
   * The step manager for the step this visual represents
   */
  protected StepManagerImpl m_stepManager;

  /**
   * Private constructor. Use factory methods.
   */
  private StepVisual(ImageIcon icon) {
    m_icon = icon;

    if (icon != null) {
      setLayout(new BorderLayout());
      setOpaque(false);
      JLabel visual = new JLabel(m_icon);
      add(visual, BorderLayout.CENTER);
      Dimension d = visual.getPreferredSize();
      Dimension d2 =
        new Dimension((int) d.getWidth() + 10, (int) d.getHeight() + 10);
      setMinimumSize(d2);
      setPreferredSize(d2);
      setMaximumSize(d2);
    }
  }

  protected StepVisual() {
    this(null);
  }

  public static StepVisual createVisual(StepManagerImpl stepManager) {

    if (stepManager.getManagedStep() instanceof Note) {
      NoteVisual wrapper = new NoteVisual();
      wrapper.setStepManager(stepManager);
      return wrapper;
    } else {
      ImageIcon icon = iconForStep(stepManager.getManagedStep());
      return createVisual(stepManager, icon);
    }
  }

  public static StepVisual createVisual(StepManagerImpl stepManager,
    ImageIcon icon) {

    StepVisual wrapper = new StepVisual(icon);
    wrapper.setStepManager(stepManager);

    return wrapper;
  }

  public static ImageIcon iconForStep(Step step) {
    KFStep stepAnnotation = step.getClass().getAnnotation(KFStep.class);
    if (stepAnnotation != null && stepAnnotation.iconPath() != null
      && stepAnnotation.iconPath().length() > 0) {
      return loadIcon(stepAnnotation.iconPath());
    }

    if (step instanceof WekaAlgorithmWrapper) {
      ImageIcon icon = loadIcon(((WekaAlgorithmWrapper) step).getIconPath());
      if (icon == null) {
        // try package default for this class of wrapped algorithm
        icon =
          loadIcon(((WekaAlgorithmWrapper) step)
            .getDefaultPackageLevelIconPath());
      }

      if (icon == null) {
        // try default for this class of wrapped algorithm
        icon = loadIcon(((WekaAlgorithmWrapper) step).getDefaultIconPath());
      }

      return icon;
    }

    // TODO default icon for non-wrapped steps
    return null;
  }

  public static ImageIcon loadIcon(String iconPath) {
    java.net.URL imageURL =
      StepVisual.class.getClassLoader().getResource(iconPath);

    if (imageURL != null) {
      Image pic = Toolkit.getDefaultToolkit().getImage(imageURL);
      return new ImageIcon(pic);
    }

    return null;
  }

  public static ImageIcon scaleIcon(ImageIcon icon, double factor) {
    Image pic = icon.getImage();
    double width = icon.getIconWidth();
    double height = icon.getIconHeight();

    width *= factor;
    height *= factor;

    pic = pic.getScaledInstance((int) width, (int) height, Image.SCALE_SMOOTH);

    return new ImageIcon(pic);
  }

  public Image getIcon(double scale) {
    if (scale == 1) {
      return m_icon.getImage();
    }

    Image pic = m_icon.getImage();
    double width = m_icon.getIconWidth();
    double height = m_icon.getIconHeight();

    width *= scale;
    height *= scale;

    pic = pic.getScaledInstance((int) width, (int) height, Image.SCALE_SMOOTH);

    return pic;
  }

  public String getStepName() {
    return m_stepManager.getManagedStep().getName();
  }

  public void setStepName(String name) {
    m_stepManager.getManagedStep().setName(name);
  }

  @Override
  public int getX() {
    return m_x;
  }

  public void setX(int x) {
    m_x = x;
  }

  @Override
  public int getY() {
    return m_y;
  }

  public void setY(int y) {
    m_y = y;
  }

  public StepManagerImpl getStepManager() {
    return m_stepManager;
  }

  public void setStepManager(StepManagerImpl manager) {
    m_stepManager = manager;
  }

  public String getCustomEditorForStep() {
    return m_stepManager.getManagedStep().getCustomEditorForStep();
  }

  public Set<String> getStepInteractiveViewActionNames() {
    Map<String, String> viewComps =
      m_stepManager.getManagedStep().getInteractiveViewers();
    if (viewComps == null) {
      return null;
    }

    return viewComps.keySet();
  }

  public JComponent getStepInteractiveViewComponent(String viewActioName)
    throws WekaException {

    if (m_stepManager.getManagedStep().getInteractiveViewers() == null) {
      throw new WekaException("Steo '"
        + m_stepManager.getManagedStep().getName() + "' "
        + "does not have any interactive view components");
    }

    String clazz =
      m_stepManager.getManagedStep().getInteractiveViewers().get(viewActioName);
    if (clazz == null) {
      throw new WekaException("Step '"
        + m_stepManager.getManagedStep().getName() + "' "
        + "does not have an interactive view component called '"
        + viewActioName + "'");
    }

    Object comp = null;
    try {
      comp = Beans.instantiate(this.getClass().getClassLoader(), clazz);
    } catch (Exception ex) {
      throw new WekaException(ex);
    }

    if (!(comp instanceof JComponent)) {
      throw new WekaException("Interactive view component '" + clazz
        + "' does not " + "extend JComponent");
    }

    return (JComponent) comp;
  }

  /**
   * Returns the coordinates of the closest "connector" point to the supplied
   * point. Coordinates are in the parent containers coordinate space.
   *
   * @param pt the reference point
   * @return the closest connector point
   */
  public Point getClosestConnectorPoint(Point pt) {
    int sourceX = getX();
    int sourceY = getY();
    int sourceWidth = getWidth();
    int sourceHeight = getHeight();
    int sourceMidX = sourceX + (sourceWidth / 2);
    int sourceMidY = sourceY + (sourceHeight / 2);
    int x = (int) pt.getX();
    int y = (int) pt.getY();

    Point closest = new Point();
    int cx =
      (Math.abs(x - sourceMidX) < Math.abs(y - sourceMidY)) ? sourceMidX
        : ((x < sourceMidX) ? sourceX : sourceX + sourceWidth);
    int cy =
      (Math.abs(y - sourceMidY) < Math.abs(x - sourceMidX)) ? sourceMidY
        : ((y < sourceMidY) ? sourceY : sourceY + sourceHeight);
    closest.setLocation(cx, cy);
    return closest;
  }

  /**
   * Turn on/off the connector points
   *
   * @param dc a <code>boolean</code> value
   */
  public void setDisplayConnectors(boolean dc) {
    // m_visualHolder.setDisplayConnectors(dc);
    m_displayConnectors = dc;
    m_connectorColor = Color.blue;
    repaint();
  }

  /**
   * Turn on/off the connector points
   *
   * @param dc a <code>boolean</code> value
   * @param c the Color to use
   */
  public void setDisplayConnectors(boolean dc, Color c) {
    setDisplayConnectors(dc);
    m_connectorColor = c;
  }

  /**
   * Returns true if the step label is to be displayed. Subclasses can override
   * to change this.
   *
   * @return true (default) if the step label is to be displayed
   */
  public boolean getDisplayStepLabel() {
    return true;
  }

  @Override
  public void paintComponent(Graphics gx) {
    ((Graphics2D) gx).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON);

    super.paintComponent(gx);
    if (m_displayConnectors) {
      gx.setColor(m_connectorColor);

      int midx = (int) (this.getWidth() / 2.0);
      int midy = (int) (this.getHeight() / 2.0);
      gx.fillOval(midx - 2, 0, 5, 5);
      gx.fillOval(midx - 2, this.getHeight() - 5, 5, 5);
      gx.fillOval(0, midy - 2, 5, 5);
      gx.fillOval(this.getWidth() - 5, midy - 2, 5, 5);
    }
  }

}
