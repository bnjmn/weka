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
 *    LayoutPanel.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow;

import weka.core.*;
import weka.core.converters.FileSourcedConverter;
import weka.gui.Perspective;
import weka.gui.knowledgeflow.VisibleLayout.LayoutOperation;
import weka.gui.visualize.PrintablePanel;
import weka.knowledgeflow.KFDefaults;
import weka.knowledgeflow.StepManager;
import weka.knowledgeflow.StepManagerImpl;
import weka.knowledgeflow.steps.Loader;
import weka.knowledgeflow.steps.Note;

import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Provides a panel just for laying out a Knowledge Flow graph. Also listens for
 * mouse events for editing the flow.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class LayoutPanel extends PrintablePanel {

  /** For serialization */
  private static final long serialVersionUID = 4988098224376217099L;

  /** Grid spacing */
  protected int m_gridSpacing;

  /** The flow contained in this LayoutPanel as a visible (graphical) flow */
  protected VisibleLayout m_visLayout;

  protected int m_currentX;
  protected int m_currentY;
  protected int m_oldX;
  protected int m_oldY;

  /** Thread for loading data for perspectives */
  protected Thread m_perspectiveDataLoadThread;

  /**
   * Constructor
   *
   * @param vis the {@code VisibleLayout} to display
   */
  public LayoutPanel(VisibleLayout vis) {
    super();
    m_visLayout = vis;
    setLayout(null);

    setupMouseListener();
    setupMouseMotionListener();

    m_gridSpacing =
      m_visLayout.getMainPerspective().getSetting(KFDefaults.GRID_SPACING_KEY,
        KFDefaults.GRID_SPACING);
  }

  /**
   * Configure mouse listener
   */
  protected void setupMouseListener() {
    addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent me) {
        LayoutPanel.this.requestFocusInWindow();
        double z = m_visLayout.getZoomSetting() / 100.0;
        double px = me.getX();
        double py = me.getY();
        py /= z;
        px /= z;

        if (m_visLayout.getMainPerspective().getPalleteSelectedStep() == null) {
          if (((me.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK)
            && m_visLayout.getFlowLayoutOperation() == VisibleLayout.LayoutOperation.NONE) {
            StepVisual step =
              m_visLayout.findStep(new Point((int) px, (int) py));
            if (step != null) {
              m_visLayout.setEditStep(step);
              m_oldX = (int) px;
              m_oldY = (int) py;
              m_visLayout.setFlowLayoutOperation(LayoutOperation.MOVING);
            }
            if (m_visLayout.getFlowLayoutOperation() != LayoutOperation.MOVING) {
              m_visLayout.setFlowLayoutOperation(LayoutOperation.SELECTING);
              m_oldX = (int) px;
              m_oldY = (int) py;

              m_currentX = m_oldX;
              m_currentY = m_oldY;
              Graphics2D gx = (Graphics2D) LayoutPanel.this.getGraphics();
              gx.setXORMode(java.awt.Color.white);
              gx.dispose();
            }
          }
        }
      }

      @Override
      public void mouseReleased(MouseEvent me) {
        LayoutPanel.this.requestFocusInWindow();

        if (m_visLayout.getEditStep() != null
          && m_visLayout.getFlowLayoutOperation() == LayoutOperation.MOVING) {
          if (m_visLayout.getMainPerspective().getSnapToGrid()) {
            int x = snapToGrid(m_visLayout.getEditStep().getX());
            int y = snapToGrid(m_visLayout.getEditStep().getY());
            m_visLayout.getEditStep().setX(x);
            m_visLayout.getEditStep().setY(y);
            snapSelectedToGrid();
          }

          m_visLayout.setEditStep(null);
          revalidate();
          repaint();
          m_visLayout.setFlowLayoutOperation(LayoutOperation.NONE);
        }

        if (m_visLayout.getFlowLayoutOperation() == LayoutOperation.SELECTING) {
          revalidate();
          repaint();
          m_visLayout.setFlowLayoutOperation(LayoutOperation.NONE);
          double z = m_visLayout.getZoomSetting() / 100.0;
          double px = me.getX();
          double py = me.getY();
          py /= z;
          px /= z;

          highlightSubFlow(m_currentX, m_currentY, (int) px, (int) py);
        }
      }

      @Override
      public void mouseClicked(MouseEvent me) {
        LayoutPanel.this.requestFocusInWindow();
        Point p = me.getPoint();
        Point np = new Point();
        double z = m_visLayout.getZoomSetting() / 100.0;
        double px = me.getX();
        double py = me.getY();
        px /= z;
        py /= z;

        np.setLocation(p.getX() / z, p.getY() / z);
        StepVisual step = m_visLayout.findStep(np);
        if (m_visLayout.getFlowLayoutOperation() == LayoutOperation.ADDING
          || m_visLayout.getFlowLayoutOperation() == LayoutOperation.NONE) {
          // try and popup a context sensitive menu if we've been
          // clicked over a step
          if (step != null) {
            if (me.getClickCount() == 2) {
              if (!step.getStepManager().isStepBusy()
                && !m_visLayout.isExecuting()) {
                popupStepEditorDialog(step);
              }
            } else if ((me.getModifiers() & InputEvent.BUTTON1_MASK) != InputEvent.BUTTON1_MASK
              || me.isAltDown()) {
              stepContextualMenu(step, (int) (p.getX() / z),
                (int) (p.getY() / z));
              return;
            } else {
              // just select this step
              List<StepVisual> v = m_visLayout.getSelectedSteps();
              if (!me.isShiftDown()) {
                v = new ArrayList<StepVisual>();
              }
              v.add(step);
              m_visLayout.setSelectedSteps(v);
              return;
            }
          } else {
            if ((me.getModifiers() & InputEvent.BUTTON1_MASK) != InputEvent.BUTTON1_MASK
              || me.isAltDown()) {

              if (!m_visLayout.isExecuting()) {
                canvasContextualMenu((int) px, (int) py);

                revalidate();
                repaint();
                m_visLayout.getMainPerspective().notifyIsDirty();
              }
              return;
            } else if (m_visLayout.getMainPerspective()
              .getPalleteSelectedStep() != null) {
              // if there is a user-selected step from the design palette then
              // add the step

              // snap to grid
              double x = px;
              double y = py;

              if (m_visLayout.getMainPerspective().getSnapToGrid()) {
                x = snapToGrid((int) x);
                y = snapToGrid((int) y);
              }

              m_visLayout.addUndoPoint();
              m_visLayout.addStep(m_visLayout.getMainPerspective()
                .getPalleteSelectedStep(), (int) x, (int) y);

              m_visLayout.getMainPerspective().clearDesignPaletteSelection();
              m_visLayout.getMainPerspective().setPalleteSelectedStep(null);
              m_visLayout.setFlowLayoutOperation(LayoutOperation.NONE);
              m_visLayout.setEdited(true);
            }
          }
          revalidate();
          repaint();
          m_visLayout.getMainPerspective().notifyIsDirty();
        }

        if (m_visLayout.getFlowLayoutOperation() == LayoutOperation.PASTING
          && m_visLayout.getMainPerspective().getPasteBuffer().length() > 0) {

          try {
            m_visLayout.pasteFromClipboard((int) px, (int) py);
          } catch (WekaException e) {
            m_visLayout.getMainPerspective().showErrorDialog(e);
          }

          m_visLayout.setFlowLayoutOperation(LayoutOperation.NONE);
          m_visLayout.getMainPerspective().setCursor(
            Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
          return;
        }

        if (m_visLayout.getFlowLayoutOperation() == LayoutOperation.CONNECTING) {
          // turn off connector points and remove connecting line
          repaint();
          for (StepVisual v : m_visLayout.getRenderGraph()) {
            v.setDisplayConnectors(false);
          }

          if (step != null
            && step.getStepManager() != m_visLayout.getEditStep()
              .getStepManager()) {
            // connection is valid because only valid connections will
            // have appeared in the contextual popup

            m_visLayout.addUndoPoint();

            m_visLayout.connectSteps(
              m_visLayout.getEditStep().getStepManager(),
              step.getStepManager(), m_visLayout.getEditConnection());

            m_visLayout.setEdited(true);
            repaint();
          }
          m_visLayout.setFlowLayoutOperation(LayoutOperation.NONE);
          m_visLayout.setEditStep(null);
          m_visLayout.setEditConnection(null);
        }

        if (m_visLayout.getSelectedSteps().size() > 0) {
          m_visLayout.setSelectedSteps(new ArrayList<StepVisual>());
        }
      }
    });
  }

  /**
   * Configure mouse motion listener
   */
  protected void setupMouseMotionListener() {
    addMouseMotionListener(new MouseMotionAdapter() {

      @Override
      public void mouseDragged(MouseEvent me) {
        double z = m_visLayout.getZoomSetting() / 100.0;
        double px = me.getX();
        double py = me.getY();
        px /= z;
        py /= z;

        if (m_visLayout.getEditStep() != null
          && m_visLayout.getFlowLayoutOperation() == LayoutOperation.MOVING) {
          int deltaX = (int) px - m_oldX;
          int deltaY = (int) py - m_oldY;

          m_visLayout.getEditStep().setX(
            m_visLayout.getEditStep().getX() + deltaX);
          m_visLayout.getEditStep().setY(
            m_visLayout.getEditStep().getY() + deltaY);
          for (StepVisual v : m_visLayout.getSelectedSteps()) {
            if (v != m_visLayout.getEditStep()) {
              v.setX(v.getX() + deltaX);
              v.setY(v.getY() + deltaY);
            }
          }
          repaint();
          m_oldX = (int) px;
          m_oldY = (int) py;
          m_visLayout.setEdited(true);
        }

        if (m_visLayout.getFlowLayoutOperation() == LayoutOperation.SELECTING) {
          repaint();
          m_oldX = (int) px;
          m_oldY = (int) py;
        }
      }

      @Override
      public void mouseMoved(MouseEvent me) {
        double z = m_visLayout.getZoomSetting() / 100.0;
        double px = me.getX();
        double py = me.getY();
        px /= z;
        py /= z;

        if (m_visLayout.getFlowLayoutOperation() == LayoutOperation.CONNECTING) {
          repaint();
          m_oldX = (int) px;
          m_oldY = (int) py;
        }
      }
    });
  }

  @Override
  public void paintComponent(Graphics gx) {
    Color backG =
      m_visLayout.getMainPerspective().getSetting(KFDefaults.LAYOUT_COLOR_KEY,
        KFDefaults.LAYOUT_COLOR);
    if (!backG.equals(getBackground())) {
      setBackground(backG);
    }

    double lz = m_visLayout.getZoomSetting() / 100.0;
    ((Graphics2D) gx).scale(lz, lz);
    if (m_visLayout.getZoomSetting() < 100) {
      ((Graphics2D) gx).setStroke(new BasicStroke(2));
    }
    super.paintComponent(gx);

    ((Graphics2D) gx).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON);

    ((Graphics2D) gx).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
      RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);

    paintStepLabels(gx);
    paintConnections(gx);

    if (m_visLayout.getFlowLayoutOperation() == LayoutOperation.CONNECTING) {
      gx.drawLine(m_currentX, m_currentY, m_oldX, m_oldY);
    } else if (m_visLayout.getFlowLayoutOperation() == LayoutOperation.SELECTING) {
      gx.drawRect(m_currentX < m_oldX ? m_currentX : m_oldX,
        m_currentY < m_oldY ? m_currentY : m_oldY,
        Math.abs(m_oldX - m_currentX), Math.abs(m_oldY - m_currentY));
    }

    if (m_visLayout.getMainPerspective().getSetting(KFDefaults.SHOW_GRID_KEY,
      KFDefaults.SHOW_GRID)) {
      Color gridColor =
        m_visLayout.getMainPerspective().getSetting(KFDefaults.GRID_COLOR_KEY,
          KFDefaults.GRID_COLOR);
      gx.setColor(gridColor);
      int gridSpacing =
        m_visLayout.getMainPerspective().getSetting(
          KFDefaults.GRID_SPACING_KEY, KFDefaults.GRID_SPACING);
      int layoutWidth =
        m_visLayout.getMainPerspective().getSetting(
          KFDefaults.LAYOUT_WIDTH_KEY, KFDefaults.LAYOUT_WIDTH);
      int layoutHeight =
        m_visLayout.getMainPerspective().getSetting(
          KFDefaults.LAYOUT_HEIGHT_KEY, KFDefaults.LAYOUT_HEIGHT);
      Stroke original = ((Graphics2D) gx).getStroke();
      Stroke dashed =
        new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0,
          new float[] { 3 }, 0);
      ((Graphics2D) gx).setStroke(dashed);

      for (int i = gridSpacing; i < layoutWidth / lz; i += gridSpacing) {
        gx.drawLine(i, 0, i, (int) (layoutHeight / lz));
      }
      for (int i = gridSpacing; i < layoutHeight / lz; i += gridSpacing) {
        gx.drawLine(0, i, (int) (layoutWidth / lz), i);
      }
      ((Graphics2D) gx).setStroke(original);
    }

  }

  @Override
  public void doLayout() {
    super.doLayout();

    for (StepVisual v : m_visLayout.getRenderGraph()) {
      Dimension d = v.getPreferredSize();
      v.setBounds(v.getX(), v.getY(), d.width, d.height);
      v.revalidate();
    }
  }

  /**
   * Render the labels for each step in the layout
   *
   * @param gx the graphics context to use
   */
  protected void paintStepLabels(Graphics gx) {
    gx.setFont(new Font(null, Font.PLAIN, m_visLayout.getMainPerspective()
      .getSetting(KFDefaults.STEP_LABEL_FONT_SIZE_KEY,
        KFDefaults.STEP_LABEL_FONT_SIZE)));
    FontMetrics fm = gx.getFontMetrics();
    int hf = fm.getAscent();

    for (StepVisual v : m_visLayout.getRenderGraph()) {
      if (!v.getDisplayStepLabel()) {
        continue;
      }

      int cx = v.getX();
      int cy = v.getY();
      int width = v.getWidth();
      int height = v.getHeight();
      String label = v.getStepName();
      int labelwidth = fm.stringWidth(label);
      if (labelwidth < width) {
        gx.drawString(label, (cx + (width / 2)) - (labelwidth / 2), cy + height
          + hf + 2);
      } else {
        // split label

        // find mid point
        int mid = label.length() / 2;
        // look for split point closest to the mid
        int closest = label.length();
        int closestI = -1;
        for (int z = 0; z < label.length(); z++) {
          if (label.charAt(z) < 'a') {
            if (Math.abs(mid - z) < closest) {
              closest = Math.abs(mid - z);
              closestI = z;
            }
          }
        }
        if (closestI != -1) {
          String left = label.substring(0, closestI);
          String right = label.substring(closestI, label.length());
          if (left.length() > 1 && right.length() > 1) {
            gx.drawString(left,
              (cx + (width / 2)) - (fm.stringWidth(left) / 2), cy + height
                + (hf * 1) + 2);
            gx.drawString(right, (cx + (width / 2))
              - (fm.stringWidth(right) / 2), cy + height + (hf * 2) + 2);
          } else {
            gx.drawString(label, (cx + (width / 2))
              - (fm.stringWidth(label) / 2), cy + height + (hf * 1) + 2);
          }
        } else {
          gx.drawString(label,
            (cx + (width / 2)) - (fm.stringWidth(label) / 2), cy + height
              + (hf * 1) + 2);
        }
      }
    }
  }

  /**
   * Render the connections between steps
   * 
   * @param gx the graphics object to use
   */
  protected void paintConnections(Graphics gx) {
    for (StepVisual stepVis : m_visLayout.getRenderGraph()) {
      Map<String, List<StepManager>> outConns =
        stepVis.getStepManager().getOutgoingConnections();

      if (outConns.size() > 0) {
        List<String> generatableOutputConnections =
          stepVis.getStepManager().getStepOutgoingConnectionTypes();

        // iterate over the outgoing connections and paint
        // with color according to what is present in
        // generatableOutputConnections
        int count = 0;
        for (Entry<String, List<StepManager>> e : outConns.entrySet()) {
          String connName = e.getKey();
          List<StepManager> connectedSteps = e.getValue();
          if (connectedSteps.size() > 0) {
            int sX = stepVis.getX();
            int sY = stepVis.getY();
            int sWidth = stepVis.getWidth();
            int sHeight = stepVis.getHeight();

            for (StepManager target : connectedSteps) {
              StepManagerImpl targetI = (StepManagerImpl) target;
              int tX = targetI.getStepVisual().getX();
              int tY = targetI.getStepVisual().getY();
              int tWidth = targetI.getStepVisual().getWidth();
              int tHeight = targetI.getStepVisual().getHeight();

              Point bestSourcePoint =
                stepVis.getClosestConnectorPoint(new Point(tX + (tWidth / 2),
                  tY + (tHeight / 2)));
              Point bestTargetPoint =
                targetI.getStepVisual().getClosestConnectorPoint(
                  new Point(sX + (sWidth / 2), sY + (sHeight / 2)));

              gx.setColor(Color.red);
              boolean active =
                generatableOutputConnections == null
                  || !generatableOutputConnections.contains(connName) ? false
                  : true;
              if (!active) {
                gx.setColor(Color.gray);
              }

              gx.drawLine((int) bestSourcePoint.getX(),
                (int) bestSourcePoint.getY(), (int) bestTargetPoint.getX(),
                (int) bestTargetPoint.getY());

              // paint an arrow head
              double angle;
              try {
                double a =
                  (bestSourcePoint.getY() - bestTargetPoint.getY())
                    / (bestSourcePoint.getX() - bestTargetPoint.getX());
                angle = Math.atan(a);
              } catch (Exception ex) {
                angle = Math.PI / 2;
              }
              Point arrowstart =
                new Point(bestTargetPoint.x, bestTargetPoint.y);
              Point arrowoffset =
                new Point((int) (7 * Math.cos(angle)),
                  (int) (7 * Math.sin(angle)));
              Point arrowend;
              if (bestSourcePoint.getX() >= bestTargetPoint.getX()) {

                arrowend =
                  new Point(arrowstart.x + arrowoffset.x, arrowstart.y
                    + arrowoffset.y);
              } else {
                arrowend =
                  new Point(arrowstart.x - arrowoffset.x, arrowstart.y
                    - arrowoffset.y);
              }
              int xs[] =
                { arrowstart.x,
                  arrowend.x + (int) (7 * Math.cos(angle + (Math.PI / 2))),
                  arrowend.x + (int) (7 * Math.cos(angle - (Math.PI / 2))) };
              int ys[] =
                { arrowstart.y,
                  arrowend.y + (int) (7 * Math.sin(angle + (Math.PI / 2))),
                  arrowend.y + (int) (7 * Math.sin(angle - (Math.PI / 2))) };
              gx.fillPolygon(xs, ys, 3);

              // paint the connection name
              int midx = (int) bestSourcePoint.getX();
              midx +=
                (int) ((bestTargetPoint.getX() - bestSourcePoint.getX()) / 2);
              int midy = (int) bestSourcePoint.getY();
              midy +=
                (int) ((bestTargetPoint.getY() - bestSourcePoint.getY()) / 2) - 2;
              gx.setColor((active) ? Color.blue : Color.gray);
              // check to see if there is more than one connection
              // between the source and target
              if (m_visLayout.previousConn(outConns, targetI, count)) {
                midy -= 15;
              }
              gx.drawString(connName, midx, midy);

            }
          }
          count++;
        }
      }
    }
  }

  /**
   * Popup a contextual menu on the canvas that provides options for cutting,
   * pasting, deleting selected steps etc.
   *
   * @param x the x coordinate to pop up at
   * @param y the y coordinate to pop up at
   */
  protected void canvasContextualMenu(final int x, final int y) {
    Map<String, List<StepManagerImpl[]>> closestConnections =
      m_visLayout.findClosestConnections(new Point(x, y), 10);

    PopupMenu contextualMenu = new PopupMenu();
    int menuItemCount = 0;
    if (m_visLayout.getSelectedSteps().size() > 0) {
      MenuItem snapItem = new MenuItem("Snap selected to grid");
      snapItem.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          snapSelectedToGrid();
        }
      });
      contextualMenu.add(snapItem);
      menuItemCount++;

      MenuItem copyItem = new MenuItem("Copy selected");
      copyItem.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          try {
            m_visLayout.copySelectedStepsToClipboard();
            m_visLayout.setSelectedSteps(new ArrayList<StepVisual>());
          } catch (WekaException ex) {
            m_visLayout.getMainPerspective().showErrorDialog(ex);
          }
        }
      });
      contextualMenu.add(copyItem);
      menuItemCount++;

      MenuItem cutItem = new MenuItem("Cut selected");
      cutItem.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          try {
            m_visLayout.copySelectedStepsToClipboard();
            m_visLayout.removeSelectedSteps();
          } catch (WekaException ex) {
            m_visLayout.getMainPerspective().showErrorDialog(ex);
          }
        }
      });
      contextualMenu.add(cutItem);
      menuItemCount++;

      MenuItem deleteSelected = new MenuItem("Delete selected");
      deleteSelected.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          try {
            m_visLayout.removeSelectedSteps();
          } catch (WekaException ex) {
            m_visLayout.getMainPerspective().showErrorDialog(ex);
          }
        }
      });
      contextualMenu.add(deleteSelected);
      menuItemCount++;
    }

    if (m_visLayout.getMainPerspective().getPasteBuffer() != null
      && m_visLayout.getMainPerspective().getPasteBuffer().length() > 0) {
      contextualMenu.addSeparator();
      menuItemCount++;

      MenuItem pasteItem = new MenuItem("Paste");
      pasteItem.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          try {
            m_visLayout.pasteFromClipboard(x, y);
          } catch (WekaException ex) {
            m_visLayout.getMainPerspective().showErrorDialog(ex);
          }
        }
      });
      contextualMenu.add(pasteItem);
      menuItemCount++;
    }

    if (closestConnections.size() > 0) {
      contextualMenu.addSeparator();
      menuItemCount++;

      MenuItem deleteConnection = new MenuItem("Delete connection:");
      deleteConnection.setEnabled(false);
      contextualMenu.insert(deleteConnection, menuItemCount++);

      for (Map.Entry<String, List<StepManagerImpl[]>> e : closestConnections
        .entrySet()) {
        final String connName = e.getKey();
        for (StepManagerImpl[] cons : e.getValue()) {
          final StepManagerImpl source = cons[0];
          final StepManagerImpl target = cons[1];

          MenuItem deleteItem =
            new MenuItem(connName + "-->" + target.getName());
          deleteItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              m_visLayout.addUndoPoint();

              source.disconnectStepWithConnection(target.getManagedStep(),
                connName);
              target.disconnectStepWithConnection(source.getManagedStep(),
                connName);
              if (m_visLayout.getSelectedSteps().size() > 0) {
                m_visLayout.setSelectedSteps(new ArrayList<StepVisual>());
              }
              m_visLayout.setEdited(true);
              revalidate();
              repaint();
              m_visLayout.getMainPerspective().notifyIsDirty();
            }
          });
          contextualMenu.add(deleteItem);
          menuItemCount++;
        }
      }
    }

    if (menuItemCount > 0) {
      contextualMenu.addSeparator();
      menuItemCount++;
    }

    MenuItem noteItem = new MenuItem("New note");
    noteItem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        initiateAddNote();
      }
    });
    contextualMenu.add(noteItem);

    this.add(contextualMenu);

    // make sure that popup location takes current scaling into account
    double z = m_visLayout.getZoomSetting() / 100.0;
    double px = x * z;
    double py = y * z;
    contextualMenu.show(this, (int) px, (int) py);
  }

  /**
   * Initiate the process of adding a note to the canvas
   */
  protected void initiateAddNote() {
    Note n = new Note();
    StepManagerImpl noteManager = new StepManagerImpl(n);
    StepVisual noteVisual = StepVisual.createVisual(noteManager);
    m_visLayout.getMainPerspective().setPalleteSelectedStep(
      noteVisual.getStepManager());
    m_visLayout.setFlowLayoutOperation(LayoutOperation.ADDING);
    m_visLayout.getMainPerspective().setCursor(
      Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
  }

  /**
   * Popup the contextual menu for when a step is right clicked on
   *
   * @param step the step that was clicked on
   * @param x the x coordiante to pop up at
   * @param y the y coordinate to pop up at
   */
  protected void stepContextualMenu(final StepVisual step, final int x,
    final int y) {
    PopupMenu stepContextMenu = new PopupMenu();
    boolean executing = m_visLayout.isExecuting();

    int menuItemCount = 0;
    MenuItem edit = new MenuItem("Edit:");
    edit.setEnabled(false);
    stepContextMenu.insert(edit, menuItemCount);
    menuItemCount++;

    if (m_visLayout.getSelectedSteps().size() > 0) {
      MenuItem copyItem = new MenuItem("Copy");
      copyItem.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          try {
            m_visLayout.copySelectedStepsToClipboard();
          } catch (WekaException ex) {
            m_visLayout.getMainPerspective().showErrorDialog(ex);
          }
          m_visLayout.setSelectedSteps(new ArrayList<StepVisual>());
        }
      });
      stepContextMenu.add(copyItem);
      copyItem.setEnabled(!executing);
      menuItemCount++;
    }

    MenuItem deleteItem = new MenuItem("Delete");
    deleteItem.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        m_visLayout.addUndoPoint();

        try {
          m_visLayout.removeStep(step);
        } catch (WekaException ex) {
          m_visLayout.getMainPerspective().showErrorDialog(ex);
        }
        // step.getStepManager().clearAllConnections();
        // LayoutPanel.this.remove(step);
        String key =
          step.getStepName() + "$"
            + step.getStepManager().getManagedStep().hashCode();
        m_visLayout.getLogPanel().statusMessage(key + "|remove");

        LayoutPanel.this.revalidate();
        LayoutPanel.this.repaint();

        m_visLayout.setEdited(true);
        m_visLayout.getMainPerspective().notifyIsDirty();
        m_visLayout
          .getMainPerspective()
          .getMainToolBar()
          .enableWidget(
            MainKFPerspectiveToolBar.Widgets.SELECT_ALL_BUTTON.toString(),
            m_visLayout.getSelectedSteps().size() > 0);
      }
    });
    deleteItem.setEnabled(!executing);
    stepContextMenu.add(deleteItem);
    menuItemCount++;

    MenuItem nameItem = new MenuItem("Set name...");
    nameItem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        String oldName = step.getStepName();
        String name =
          JOptionPane.showInputDialog(m_visLayout.getMainPerspective(),
            "Enter a name for this step", oldName);
        if (name != null) {
          m_visLayout.renameStep(oldName, name);
          m_visLayout.setEdited(true);
          revalidate();
          repaint();
        }
      }
    });
    nameItem.setEnabled(!executing);
    stepContextMenu.add(nameItem);
    menuItemCount++;

    MenuItem configItem = new MenuItem("Configure...");
    configItem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        popupStepEditorDialog(step);

        m_visLayout.getMainPerspective().notifyIsDirty();
      }
    });

    configItem.setEnabled(!executing);
    stepContextMenu.add(configItem);
    menuItemCount++;

    // connections
    List<String> outgoingConnTypes =
      step.getStepManager().getManagedStep().getOutgoingConnectionTypes();
    if (outgoingConnTypes != null && outgoingConnTypes.size() > 0) {
      MenuItem connections = new MenuItem("Connections:");
      connections.setEnabled(false);
      stepContextMenu.insert(connections, menuItemCount);
      menuItemCount++;

      for (final String connType : outgoingConnTypes) {
        MenuItem conItem = new MenuItem(connType);
        conItem.setEnabled(!executing);
        conItem.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            initiateConnection(connType, step, x, y);
            m_visLayout.getMainPerspective().notifyIsDirty();
          }
        });
        stepContextMenu.add(conItem);
        menuItemCount++;
      }
    }

    // Any interactive views?
    Map<String, String> interactiveViews =
      step.getStepManager().getManagedStep().getInteractiveViewers();
    Map<String, StepInteractiveViewer> interactiveViewsImpls =
      step.getStepManager().getManagedStep().getInteractiveViewersImpls();
    if (interactiveViews != null && interactiveViews.size() > 0) {
      MenuItem actions = new MenuItem("Actions:");
      actions.setEnabled(false);
      stepContextMenu.insert(actions, menuItemCount++);

      for (Map.Entry<String, String> e : interactiveViews.entrySet()) {
        String command = e.getKey();
        String viewerClassName = e.getValue();
        addInteractiveViewMenuItem(step, e.getKey(), !executing, e.getValue(),
          null, stepContextMenu);
        menuItemCount++;
      }
    } else if (interactiveViewsImpls != null
      && interactiveViewsImpls.size() > 0) {
      MenuItem actions = new MenuItem("Actions:");
      actions.setEnabled(false);
      stepContextMenu.insert(actions, menuItemCount++);

      for (Map.Entry<String, StepInteractiveViewer> e : interactiveViewsImpls
        .entrySet()) {
        String command = e.getKey();
        StepInteractiveViewer impl = e.getValue();
        addInteractiveViewMenuItem(step, e.getKey(), !executing, null, impl,
          stepContextMenu);
        menuItemCount++;
      }
    }

    // perspective stuff...
    final List<Perspective> perspectives =
      m_visLayout.getMainPerspective().getMainApplication()
        .getPerspectiveManager().getVisiblePerspectives();
    if (perspectives.size() > 0
      && step.getStepManager().getManagedStep() instanceof Loader) {
      final weka.core.converters.Loader theLoader =
        ((Loader) step.getStepManager().getManagedStep()).getLoader();

      boolean ok = true;
      if (theLoader instanceof FileSourcedConverter) {
        String fileName =
          ((FileSourcedConverter) theLoader).retrieveFile().getPath();
        fileName = m_visLayout.environmentSubstitute(fileName);
        File tempF = new File(fileName);
        String fileNameFixedPathSep = fileName.replace(File.separatorChar, '/');
        if (!tempF.isFile()
          && this.getClass().getClassLoader().getResource(fileNameFixedPathSep) == null) {
          ok = false;
        }
      }
      if (ok) {
        stepContextMenu.addSeparator();
        menuItemCount++;

        if (perspectives.size() > 1) {
          MenuItem sendToAllPerspectives =
            new MenuItem("Send to all perspectives");
          menuItemCount++;
          sendToAllPerspectives.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              loadDataAndSendToPerspective(theLoader, perspectives, -1);
            }
          });
          stepContextMenu.add(sendToAllPerspectives);
        }
        Menu sendToPerspective = new Menu("Send to perspective...");
        stepContextMenu.add(sendToPerspective);
        menuItemCount++;
        for (int i = 0; i < perspectives.size(); i++) {
          final int pIndex = i;

          if (perspectives.get(i).acceptsInstances()
            && !perspectives.get(i).getPerspectiveID()
              .equalsIgnoreCase(KFDefaults.MAIN_PERSPECTIVE_ID)) {
            String pName = perspectives.get(i).getPerspectiveTitle();
            final MenuItem pI = new MenuItem(pName);
            pI.addActionListener(new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                loadDataAndSendToPerspective(theLoader, perspectives, pIndex);
              }
            });
            sendToPerspective.add(pI);
          }
        }
      }
    }

    if (menuItemCount > 0) {
      // make sure that popup location takes current scaling into account
      double z = m_visLayout.getZoomSetting() / 100.0;
      double px = x * z;
      double py = y * z;

      this.add(stepContextMenu);
      stepContextMenu.show(this, (int) px, (int) py);
    }
  }

  private synchronized void loadDataAndSendToPerspective(
    final weka.core.converters.Loader loader,
    final List<Perspective> perspectives, final int perspectiveIndex) {
    if (m_perspectiveDataLoadThread == null) {
      m_perspectiveDataLoadThread = new Thread() {
        @Override
        public void run() {
          try {
            if (loader instanceof EnvironmentHandler) {
              ((EnvironmentHandler) loader).setEnvironment(m_visLayout
                .getEnvironment());
              loader.reset();
            }
            m_visLayout.getLogPanel().statusMessage(
              "@!@[KnowledgeFlow]|Sending data to perspective(s)...");
            Instances data = loader.getDataSet();
            if (data != null) {
              m_visLayout.getMainPerspective().getMainApplication()
                .showPerspectivesToolBar();
              // need to disable all the perspective buttons on the toolbar
              /*
               * m_visLayout.getMainPerspective().getMainApplication()
               * .getPerspectiveManager().disableAllPerspectiveTabs();
               */
              if (perspectiveIndex < 0) {
                // send to all
                for (Perspective p : perspectives) {
                  if (p.acceptsInstances()) {
                    p.setInstances(data);
                    m_visLayout.getMainPerspective().getMainApplication()
                      .getPerspectiveManager()
                      .setEnablePerspectiveTab(p.getPerspectiveID(), true);
                  }
                }
              } else {
                perspectives.get(perspectiveIndex).setInstances(data);
                m_visLayout
                  .getMainPerspective()
                  .getMainApplication()
                  .getPerspectiveManager()
                  .setActivePerspective(
                    perspectives.get(perspectiveIndex).getPerspectiveID());
                m_visLayout
                  .getMainPerspective()
                  .getMainApplication()
                  .getPerspectiveManager()
                  .setEnablePerspectiveTab(
                    perspectives.get(perspectiveIndex).getPerspectiveID(), true);
              }
            }
          } catch (Exception ex) {
            m_visLayout.getMainPerspective().showErrorDialog(ex);
          } finally {
            // re-enable the perspective buttons
            /*
             * m_visLayout.getMainPerspective().getMainApplication()
             * .getPerspectiveManager().enableAllPerspectiveTabs();
             */
            m_perspectiveDataLoadThread = null;
            m_visLayout.getLogPanel().statusMessage("@!@[KnowledgeFlow]|OK");
          }
        }
      };
      m_perspectiveDataLoadThread.setPriority(Thread.MIN_PRIORITY);
      m_perspectiveDataLoadThread.start();
    }
  }

  /**
   * Initiate the process of connecting two steps.
   *
   * @param connType the type of the connection to create
   * @param source the source step of the connection
   * @param x the x coordinate at which the connection starts
   * @param y the y coordinate at which the connection starts
   */
  protected void initiateConnection(String connType, StepVisual source, int x,
    int y) {
    // unselect any selected steps on the layaout
    if (m_visLayout.getSelectedSteps().size() > 0) {
      m_visLayout.setSelectedSteps(new ArrayList<StepVisual>());
    }

    List<StepManagerImpl> connectableForConnType =
      m_visLayout.findStepsThatCanAcceptConnection(connType);

    for (StepManagerImpl sm : connectableForConnType) {
      sm.getStepVisual().setDisplayConnectors(true);
    }

    if (connectableForConnType.size() > 0) {
      source.setDisplayConnectors(true);
      m_visLayout.setEditStep(source);
      m_visLayout.setEditConnection(connType);
      Point closest = source.getClosestConnectorPoint(new Point(x, y));
      m_currentX = (int) closest.getX();
      m_currentY = (int) closest.getY();
      m_oldX = m_currentX;
      m_oldY = m_currentY;
      Graphics2D gx = (Graphics2D) this.getGraphics();
      gx.setXORMode(java.awt.Color.white);
      gx.drawLine(m_currentX, m_currentY, m_currentX, m_currentY);
      gx.dispose();
      m_visLayout.setFlowLayoutOperation(LayoutOperation.CONNECTING);
    }

    revalidate();
    repaint();
    m_visLayout.getMainPerspective().notifyIsDirty();
  }

  /**
   * Add a menu item to a contextual menu for accessing any interactive viewers
   * that a step might provide
   *
   * @param step the step in question
   * @param entryText the text of the menu entry
   * @param enabled true if the menu entry is to be enabled
   * @param viewerClassName the fully qualified name of the viewer that is
   *          associated with the menu entry
   * @param viewerImpl an instance of the viewer itself. If null, then the fully
   *          qualified viewer class name will be used to instantiate an
   *          instance
   * @param stepContextMenu the menu to add the item to
   */
  protected void addInteractiveViewMenuItem(final StepVisual step,
    String entryText, boolean enabled, final String viewerClassName,
    final StepInteractiveViewer viewerImpl, PopupMenu stepContextMenu) {

    MenuItem viewItem = new MenuItem(entryText);
    viewItem.setEnabled(enabled);
    viewItem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        popupStepInteractiveViewer(step, viewerClassName, viewerImpl);
      }
    });
    stepContextMenu.add(viewItem);
  }

  /**
   * Popup a dialog containing a particular interactive step viewer
   *
   * @param step the step that provides the viewer
   * @param viewerClassName the fully qualified name of the viewer
   * @param viewerImpl the actual instance of the viewer
   */
  protected void popupStepInteractiveViewer(StepVisual step,
    String viewerClassName, StepInteractiveViewer viewerImpl) {
    try {
      Object viewer =
        viewerClassName != null ? WekaPackageClassLoaderManager
          .objectForName(viewerClassName) : viewerImpl;
      // viewerClassName != null ? Beans.instantiate(this.getClass()
      // .getClassLoader(), viewerClassName) : viewerImpl;
      if (!(viewer instanceof StepInteractiveViewer)) {
        throw new WekaException("Interactive step viewer component "
          + viewerClassName + " must implement StepInteractiveViewer");
      }

      if (!(viewer instanceof JComponent)) {
        throw new WekaException("Interactive step viewer component "
          + viewerClassName + " must be a JComponent");
      }

      String viewerName = ((StepInteractiveViewer) viewer).getViewerName();
      ((StepInteractiveViewer) viewer).setStep(step.getStepManager()
        .getManagedStep());
      ((StepInteractiveViewer) viewer).setMainKFPerspective(m_visLayout
        .getMainPerspective());
      JFrame jf = Utils.getWekaJFrame(viewerName, this);
      ((StepInteractiveViewer) viewer).setParentWindow(jf);
      ((StepInteractiveViewer) viewer).init();
      jf.setLayout(new BorderLayout());
      jf.add((JComponent) viewer, BorderLayout.CENTER);
      jf.pack();
      jf.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
      jf.setVisible(true);
      ((StepInteractiveViewer) viewer).nowVisible();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (Exception e) {
      m_visLayout.getMainPerspective().showErrorDialog(e);
    }
  }

  /**
   * Popup an editor dialog for a given step
   *
   * @param step the step to popup the editor dialog for
   */
  protected void popupStepEditorDialog(StepVisual step) {
    String custEditor =
      step.getStepManager().getManagedStep().getCustomEditorForStep();

    StepEditorDialog toPopup = null;
    if (custEditor != null && custEditor.length() > 0) {
      try {
        Object custPanel = WekaPackageClassLoaderManager.objectForName(custEditor);
          // Beans.instantiate(getClass().getClassLoader(), custEditor);

        if (!(custPanel instanceof StepEditorDialog)) {
          throw new WekaException(
            "Custom editor must be a subclass of StepEditorDialog");
        }

        toPopup = ((StepEditorDialog) custPanel);
      } catch (Exception ex) {
        m_visLayout.getMainPerspective().showErrorDialog(ex);
        ex.printStackTrace();
      }
    } else {
      // create an editor based on the GenericObjectEditor
      toPopup = new GOEStepEditorDialog();
      toPopup.setMainPerspective(m_visLayout.getMainPerspective());
    }

    final JDialog d =
      new JDialog((java.awt.Frame) getTopLevelAncestor(),
        ModalityType.DOCUMENT_MODAL);
    d.setLayout(new BorderLayout());
    d.getContentPane().add(toPopup, BorderLayout.CENTER);
    final StepEditorDialog toPopupC = toPopup;
    d.addWindowListener(new java.awt.event.WindowAdapter() {
      @Override
      public void windowClosing(java.awt.event.WindowEvent e) {
        d.dispose();
      }
    });
    toPopup.setParentWindow(d);
    toPopup.setEnvironment(m_visLayout.getEnvironment());
    toPopup.setMainPerspective(m_visLayout.getMainPerspective());
    toPopup.setStepToEdit(step.getStepManager().getManagedStep());

    toPopup.setClosingListener(new StepEditorDialog.ClosingListener() {
      @Override
      public void closing() {
        if (toPopupC.isEdited()) {
          m_visLayout.setEdited(true);
        }
      }
    });
    d.pack();
    d.setLocationRelativeTo(m_visLayout.getMainPerspective());
    d.setVisible(true);
  }

  private int snapToGrid(int val) {
    int r = val % m_gridSpacing;
    val /= m_gridSpacing;
    if (r > (m_gridSpacing / 2)) {
      val++;
    }
    val *= m_gridSpacing;

    return val;
  }

  /**
   * Snaps selected steps to the grid
   */
  protected void snapSelectedToGrid() {
    List<StepVisual> selected = m_visLayout.getSelectedSteps();
    for (StepVisual s : selected) {
      int x = s.getX();
      int y = s.getY();
      s.setX(snapToGrid(x));
      s.setY(snapToGrid(y));
    }
    revalidate();
    repaint();
    m_visLayout.setEdited(true);
    m_visLayout.getMainPerspective().notifyIsDirty();
  }

  private void highlightSubFlow(int startX, int startY, int endX, int endY) {
    java.awt.Rectangle r =
      new java.awt.Rectangle((startX < endX) ? startX : endX,
        (startY < endY) ? startY : endY, Math.abs(startX - endX),
        Math.abs(startY - endY));

    List<StepVisual> selected = m_visLayout.findSteps(r);
    m_visLayout.setSelectedSteps(selected);
  }
}
