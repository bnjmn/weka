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
 * MainKFPerspectiveToolBar.java
 * Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 */

package weka.gui.knowledgeflow;

import weka.core.Utils;
import weka.core.WekaException;
import weka.knowledgeflow.Flow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static weka.gui.knowledgeflow.StepVisual.loadIcon;

/**
 * Class that provides the main editing widget toolbar and menu items
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class MainKFPerspectiveToolBar extends JPanel {

  private static final long serialVersionUID = -157986423490835642L;

  /** Path to the icons for the toolbar */
  public static String ICON_PATH = "weka/gui/knowledgeflow/icons/";

  /** Reference to the main knowledge flow perspective */
  protected MainKFPerspective m_mainPerspective;

  /** holds a map of widgets, keyed by widget name */
  protected Map<String, JComponent> m_widgetMap =
    new HashMap<String, JComponent>();

  /** Holds a map of top level menus */
  protected Map<String, JMenu> m_menuMap = new LinkedHashMap<String, JMenu>();

  /** holds a map of menu items (for widgets that have corresponding menu items) */
  protected Map<String, JMenuItem> m_menuItemMap =
    new HashMap<String, JMenuItem>();

  /** True if menu items corresponding to the toolbar widgets should be shown */
  protected boolean m_showMenus;

  /**
   * Constructor
   *
   * @param mainKFPerspective the main knowledge flow perspective
   */
  public MainKFPerspectiveToolBar(MainKFPerspective mainKFPerspective) {
    super();

    JMenu fileMenu = new JMenu();
    fileMenu.setText("File");
    m_menuMap.put("File", fileMenu);
    JMenu editMenu = new JMenu();
    editMenu.setText("Edit");
    m_menuMap.put("Edit", editMenu);
    JMenu insertMenu = new JMenu();
    insertMenu.setText("Insert");
    m_menuMap.put("Insert", insertMenu);
    JMenu viewMenu = new JMenu();
    viewMenu.setText("View");
    m_menuMap.put("View", viewMenu);

    m_mainPerspective = mainKFPerspective;
    setLayout(new BorderLayout());

    // set up an action for closing the current tab
    final Action closeAction = new AbstractAction("Close") {

      private static final long serialVersionUID = 4762166880144590384L;

      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_mainPerspective.getCurrentTabIndex() >= 0) {
          m_mainPerspective.removeTab(m_mainPerspective.getCurrentTabIndex());
        }
      }
    };
    KeyStroke closeKey =
      KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK);
    m_mainPerspective.getActionMap().put("Close", closeAction);
    m_mainPerspective.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
      closeKey, "Close");

    setupLeftSideToolBar();
    setupRightSideToolBar();
  }

  private void setupLeftSideToolBar() {
    JToolBar fixedTools2 = new JToolBar();
    fixedTools2.setOrientation(JToolBar.HORIZONTAL);
    fixedTools2.setFloatable(false);

    JButton playB =
      new JButton(new ImageIcon(loadIcon(ICON_PATH + "resultset_next.png")
        .getImage()));
    playB.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
    playB
      .setToolTipText("Run this flow (all start points launched in parallel)");
    final Action playParallelAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_mainPerspective.getCurrentLayout() != null) {
          boolean proceed = true;
          if (m_mainPerspective.isMemoryLow()) {
            proceed = m_mainPerspective.showMemoryIsLow();
          }
          if (proceed) {
            try {
              m_mainPerspective.getCurrentLayout().executeFlow(false);
            } catch (WekaException e1) {
              m_mainPerspective.showErrorDialog(e1);
            }
          }
        }
      }
    };
    playB.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        playParallelAction.actionPerformed(e);
      }
    });

    JButton playBB =
      new JButton(new ImageIcon(loadIcon(ICON_PATH + "resultset_last.png")
        .getImage()));
    playBB.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
    playBB.setToolTipText("Run this flow (start points launched sequentially)");
    final Action playSequentialAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_mainPerspective.getCurrentLayout() != null) {
          if (!Utils
            .getDontShowDialog("weka.gui.knowledgeflow.SequentialRunInfo")) {
            JCheckBox dontShow =
              new JCheckBox("Do not show this message again");
            Object[] stuff = new Object[2];
            stuff[0] =
              "The order that data sources are launched in can be\n"
                + "specified by setting a custom name for each data source that\n"
                + "that includes a number. E.g. \"1:MyArffLoader\". To set a name,\n"
                + "right-click over a data source and select \"Set name\"\n\n"
                + "If the prefix is not specified, then the order of execution\n"
                + "will correspond to the order that the components were added\n"
                + "to the layout. Note that it is also possible to prevent a data\n"
                + "source from executing by prefixing its name with a \"!\". E.g\n"
                + "\"!:MyArffLoader\"";
            stuff[1] = dontShow;

            JOptionPane.showMessageDialog(m_mainPerspective, stuff,
              "Sequential execution information", JOptionPane.OK_OPTION);

            if (dontShow.isSelected()) {
              try {
                Utils
                  .setDontShowDialog("weka.gui.knowledgeFlow.SequentialRunInfo");
              } catch (Exception e1) {
              }
            }
          }

          boolean proceed = true;
          if (m_mainPerspective.isMemoryLow()) {
            proceed = m_mainPerspective.showMemoryIsLow();
          }
          if (proceed) {
            try {
              m_mainPerspective.getCurrentLayout().executeFlow(true);
            } catch (WekaException e1) {
              m_mainPerspective.showErrorDialog(e1);
            }
          }
        }
      }
    };
    playBB.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        playSequentialAction.actionPerformed(e);
      }
    });

    JButton stopB =
      new JButton(new ImageIcon(loadIcon(ICON_PATH + "shape_square.png")
        .getImage()));
    stopB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    stopB.setToolTipText("Stop all execution");
    final Action stopAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_mainPerspective.getCurrentLayout() != null) {
          m_mainPerspective
            .getCurrentLayout()
            .getLogPanel()
            .statusMessage(
              "@!@[KnowledgeFlow]|Attempting to stop all components...");
          m_mainPerspective.getCurrentLayout().stopFlow();
          m_mainPerspective.getCurrentLayout().getLogPanel()
            .statusMessage("@!@[KnowledgeFlow]|OK.");
        }
      }
    };

    stopB.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        stopAction.actionPerformed(e);
      }
    });

    JButton pointerB =
      new JButton(new ImageIcon(loadIcon(ICON_PATH + "cursor.png").getImage()));
    pointerB.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
    pointerB.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        m_mainPerspective.setPalleteSelectedStep(null);
        m_mainPerspective.setCursor(Cursor
          .getPredefinedCursor(Cursor.DEFAULT_CURSOR));

        m_mainPerspective.clearDesignPaletteSelection();

        if (m_mainPerspective.getCurrentLayout() != null) {
          m_mainPerspective.getCurrentLayout().setFlowLayoutOperation(
            VisibleLayout.LayoutOperation.NONE);
        }
      }
    });

    addWidgetToToolBar(fixedTools2, Widgets.POINTER_BUTTON.toString(), pointerB);
    addWidgetToToolBar(fixedTools2, Widgets.PLAY_PARALLEL_BUTTON.toString(),
      playB);
    addWidgetToToolBar(fixedTools2, Widgets.PLAY_SEQUENTIAL_BUTTON.toString(),
      playBB);
    addWidgetToToolBar(fixedTools2, Widgets.STOP_BUTTON.toString(), stopB);
    Dimension d = playB.getPreferredSize();
    Dimension d2 = fixedTools2.getMinimumSize();
    Dimension d3 = new Dimension(d2.width, d.height + 4);
    fixedTools2.setPreferredSize(d3);
    fixedTools2.setMaximumSize(d3);
    add(fixedTools2, BorderLayout.WEST);
  }

  private void setupRightSideToolBar() {
    JToolBar fixedTools = new JToolBar();
    fixedTools.setOrientation(JToolBar.HORIZONTAL);

    JButton cutB =
      new JButton(new ImageIcon(loadIcon(ICON_PATH + "cut.png").getImage()));
    cutB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    cutB.setToolTipText("Cut selected (Ctrl+X)");
    JButton copyB =
      new JButton(new ImageIcon(loadIcon(ICON_PATH + "page_copy.png")
        .getImage()));
    copyB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    copyB.setToolTipText("Copy selected (Ctrl+C)");
    JButton pasteB =
      new JButton(new ImageIcon(loadIcon(ICON_PATH + "paste_plain.png")
        .getImage()));
    pasteB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    pasteB.setToolTipText("Paste from clipboard (Ctrl+V)");
    JButton deleteB =
      new JButton(new ImageIcon(loadIcon(ICON_PATH + "delete.png").getImage()));
    deleteB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    deleteB.setToolTipText("Delete selected (DEL)");
    final JToggleButton snapToGridB =
      new JToggleButton(new ImageIcon(loadIcon(ICON_PATH + "shape_handles.png")
        .getImage()));
    // m_snapToGridB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    snapToGridB.setToolTipText("Snap to grid (Ctrl+G)");
    JButton saveB =
      new JButton(new ImageIcon(loadIcon(ICON_PATH + "disk.png").getImage()));
    saveB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    saveB.setToolTipText("Save layout (Ctrl+S)");
    JButton saveBB =
      new JButton(new ImageIcon(loadIcon(ICON_PATH + "disk_multiple.png")
        .getImage()));
    saveBB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    saveBB.setToolTipText("Save layout with new name");
    JButton loadB =
      new JButton(new ImageIcon(loadIcon(ICON_PATH + "folder_add.png")
        .getImage()));
    loadB.setToolTipText("Open (Ctrl+O)");
    loadB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    JButton newB =
      new JButton(
        new ImageIcon(loadIcon(ICON_PATH + "page_add.png").getImage()));
    newB.setToolTipText("New layout (Ctrl+N)");
    newB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    newB.setEnabled(m_mainPerspective.getAllowMultipleTabs());
    final JButton helpB =
      new JButton(new ImageIcon(loadIcon(ICON_PATH + "help.png").getImage()));
    helpB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    helpB.setToolTipText("Display help (Ctrl+H)");
    JButton togglePerspectivesB =
      new JButton(new ImageIcon(loadIcon(ICON_PATH + "cog_go.png").getImage()));
    togglePerspectivesB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    togglePerspectivesB
      .setToolTipText("Show/hide perspectives toolbar (Ctrl+P)");
    final JButton templatesB =
      new JButton(new ImageIcon(loadIcon(
        ICON_PATH + "application_view_tile.png").getImage()));
    templatesB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    templatesB.setToolTipText("Load a template layout");
    JButton noteB =
      new JButton(
        new ImageIcon(loadIcon(ICON_PATH + "note_add.png").getImage()));
    noteB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    noteB.setToolTipText("Add a note to the layout (Ctrl+I)");
    JButton selectAllB =
      new JButton(new ImageIcon(loadIcon(ICON_PATH + "shape_group.png")
        .getImage()));
    selectAllB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    selectAllB.setToolTipText("Select all (Ctrl+A)");
    final JButton zoomInB =
      new JButton(new ImageIcon(loadIcon(ICON_PATH + "zoom_in.png").getImage()));
    zoomInB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    zoomInB.setToolTipText("Zoom in (Ctrl++)");
    final JButton zoomOutB =
      new JButton(
        new ImageIcon(loadIcon(ICON_PATH + "zoom_out.png").getImage()));
    zoomOutB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    zoomOutB.setToolTipText("Zoom out (Ctrl+-)");
    JButton undoB =
      new JButton(new ImageIcon(loadIcon(ICON_PATH + "arrow_undo.png")
        .getImage()));
    undoB.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
    undoB.setToolTipText("Undo (Ctrl+U)");

    // actions
    final Action saveAction = new AbstractAction("Save") {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_mainPerspective.getCurrentTabIndex() >= 0) {
          m_mainPerspective.saveLayout(m_mainPerspective.getCurrentTabIndex(),
            false);
        }
      }
    };
    KeyStroke saveKey =
      KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK);
    m_mainPerspective.getActionMap().put("Save", saveAction);
    m_mainPerspective.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
      saveKey, "Save");
    saveB.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        saveAction.actionPerformed(e);
      }
    });

    KeyStroke saveAsKey =
      KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK);
    final Action saveAsAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        m_mainPerspective.saveLayout(m_mainPerspective.getCurrentTabIndex(),
          true);
      }
    };
    m_mainPerspective.getActionMap().put("SaveAS", saveAsAction);
    m_mainPerspective.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
      saveAsKey, "SaveAS");

    saveBB.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        saveAsAction.actionPerformed(e);
      }
    });

    final Action openAction = new AbstractAction("Open") {
      @Override
      public void actionPerformed(ActionEvent e) {
        m_mainPerspective.loadLayout();
      }
    };
    KeyStroke openKey =
      KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK);
    m_mainPerspective.getActionMap().put("Open", openAction);
    m_mainPerspective.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
      openKey, "Open");
    loadB.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        openAction.actionPerformed(e);
      }
    });

    final Action newAction = new AbstractAction("New") {
      @Override
      public void actionPerformed(ActionEvent e) {
        m_mainPerspective.addUntitledTab();
      }
    };
    KeyStroke newKey =
      KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK);
    m_mainPerspective.getActionMap().put("New", newAction);
    m_mainPerspective.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
      newKey, "New");
    newB.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent ae) {
        newAction.actionPerformed(ae);
      }
    });

    final Action selectAllAction = new AbstractAction("SelectAll") {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_mainPerspective.getCurrentLayout() != null
          && m_mainPerspective.getCurrentLayout().numSteps() > 0) {
          List<StepVisual> newSelected =
            newSelected = new ArrayList<StepVisual>();
          newSelected.addAll(m_mainPerspective.getCurrentLayout()
            .getRenderGraph());

          // toggle
          if (newSelected.size() == m_mainPerspective.getCurrentLayout()
            .getSelectedSteps().size()) {
            // unselect all
            m_mainPerspective.getCurrentLayout().setSelectedSteps(
              new ArrayList<StepVisual>());
          } else {
            // select all
            m_mainPerspective.getCurrentLayout().setSelectedSteps(newSelected);
          }
          m_mainPerspective.revalidate();
          m_mainPerspective.repaint();
          m_mainPerspective.notifyIsDirty();
        }
      }
    };
    KeyStroke selectAllKey =
      KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK);
    m_mainPerspective.getActionMap().put("SelectAll", selectAllAction);
    m_mainPerspective.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
      selectAllKey, "SelectAll");
    selectAllB.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        selectAllAction.actionPerformed(e);
      }
    });

    final Action zoomInAction = new AbstractAction("ZoomIn") {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_mainPerspective.getCurrentLayout() != null) {
          int z = m_mainPerspective.getCurrentLayout().getZoomSetting();
          z += 25;
          zoomOutB.setEnabled(true);
          if (z >= 200) {
            z = 200;
            zoomInB.setEnabled(false);
          }
          m_mainPerspective.getCurrentLayout().setZoomSetting(z);
          m_mainPerspective.revalidate();
          m_mainPerspective.repaint();
          m_mainPerspective.notifyIsDirty();
        }
      }
    };
    KeyStroke zoomInKey =
      KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK);
    m_mainPerspective.getActionMap().put("ZoomIn", zoomInAction);
    m_mainPerspective.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
      zoomInKey, "ZoomIn");
    zoomInB.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        zoomInAction.actionPerformed(e);
      }
    });

    final Action zoomOutAction = new AbstractAction("ZoomOut") {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_mainPerspective.getCurrentLayout() != null) {
          int z = m_mainPerspective.getCurrentLayout().getZoomSetting();
          z -= 25;
          zoomInB.setEnabled(true);
          if (z <= 50) {
            z = 50;
            zoomOutB.setEnabled(false);
          }
          m_mainPerspective.getCurrentLayout().setZoomSetting(z);
          m_mainPerspective.revalidate();
          m_mainPerspective.repaint();
          m_mainPerspective.notifyIsDirty();
        }
      }
    };
    KeyStroke zoomOutKey =
      KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK);
    m_mainPerspective.getActionMap().put("ZoomOut", zoomOutAction);
    m_mainPerspective.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
      zoomOutKey, "ZoomOut");
    zoomOutB.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        zoomOutAction.actionPerformed(e);
      }
    });

    final Action cutAction = new AbstractAction("Cut") {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_mainPerspective.getCurrentLayout() != null
          && m_mainPerspective.getCurrentLayout().getSelectedSteps().size() > 0) {
          try {
            m_mainPerspective.getCurrentLayout().copySelectedStepsToClipboard();
            m_mainPerspective.getCurrentLayout().removeSelectedSteps();
          } catch (WekaException e1) {
            m_mainPerspective.showErrorDialog(e1);
          }
        }
      }
    };
    KeyStroke cutKey =
      KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK);
    m_mainPerspective.getActionMap().put("Cut", cutAction);
    m_mainPerspective.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
      cutKey, "Cut");
    cutB.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        cutAction.actionPerformed(e);
      }
    });

    final Action deleteAction = new AbstractAction("Delete") {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_mainPerspective.getCurrentLayout() != null) {
          try {
            m_mainPerspective.getCurrentLayout().removeSelectedSteps();
          } catch (WekaException e1) {
            m_mainPerspective.showErrorDialog(e1);
          }
        }
      }
    };
    KeyStroke deleteKey = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
    m_mainPerspective.getActionMap().put("Delete", deleteAction);
    m_mainPerspective.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
      deleteKey, "Delete");
    deleteB.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        deleteAction.actionPerformed(e);
      }
    });

    final Action copyAction = new AbstractAction("Copy") {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_mainPerspective.getCurrentLayout() != null
          && m_mainPerspective.getCurrentLayout().getSelectedSteps().size() > 0) {
          try {
            m_mainPerspective.getCurrentLayout().copySelectedStepsToClipboard();
            m_mainPerspective.getCurrentLayout().setSelectedSteps(
              new ArrayList<StepVisual>());
          } catch (WekaException e1) {
            m_mainPerspective.showErrorDialog(e1);
          }
        }
      }
    };
    KeyStroke copyKey =
      KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK);
    m_mainPerspective.getActionMap().put("Copy", copyAction);
    m_mainPerspective.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
      copyKey, "Copy");
    copyB.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        copyAction.actionPerformed(e);
      }
    });

    final Action pasteAction = new AbstractAction("Paste") {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_mainPerspective.getCurrentLayout() != null
          && m_mainPerspective.getPasteBuffer().length() > 0) {
          m_mainPerspective.setCursor(Cursor
            .getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
          m_mainPerspective.getCurrentLayout().setFlowLayoutOperation(
            VisibleLayout.LayoutOperation.PASTING);
        }
      }
    };
    KeyStroke pasteKey =
      KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK);
    m_mainPerspective.getActionMap().put("Paste", pasteAction);
    m_mainPerspective.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
      pasteKey, "Paste");
    pasteB.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        pasteAction.actionPerformed(e);
      }
    });

    final Action snapAction = new AbstractAction("Snap") {
      @Override
      public void actionPerformed(ActionEvent e) {
        // toggle first
        // snapToGridB.setSelected(!snapToGridB.isSelected());
        if (snapToGridB.isSelected()) {
          if (m_mainPerspective.getCurrentLayout() != null) {
            m_mainPerspective.getCurrentLayout().snapSelectedToGrid();
          }
        }
      }
    };
    KeyStroke snapKey =
      KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK);
    m_mainPerspective.getActionMap().put("Snap", snapAction);
    m_mainPerspective.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
      snapKey, "Snap");
    snapToGridB.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (snapToGridB.isSelected()) {
          snapAction.actionPerformed(e);
        }
      }
    });

    final Action noteAction = new AbstractAction("Note") {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_mainPerspective.getCurrentLayout() != null) {
          m_mainPerspective.getCurrentLayout().initiateAddNote();
        }
      }
    };
    KeyStroke noteKey =
      KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK);
    m_mainPerspective.getActionMap().put("Note", noteAction);
    m_mainPerspective.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
      noteKey, "Note");
    noteB.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        noteAction.actionPerformed(e);
      }
    });

    final Action undoAction = new AbstractAction("Undo") {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_mainPerspective.getCurrentLayout() != null) {
          m_mainPerspective.getCurrentLayout().popAndLoadUndo();
        }
      }
    };
    KeyStroke undoKey =
      KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_DOWN_MASK);
    m_mainPerspective.getActionMap().put("Undo", undoAction);
    m_mainPerspective.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
      undoKey, "Undo");
    undoB.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        undoAction.actionPerformed(e);
      }
    });

    final Action helpAction = new AbstractAction("Help") {
      @Override
      public void actionPerformed(ActionEvent e) {
        popupHelp(helpB);
      }
    };
    KeyStroke helpKey =
      KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK);
    m_mainPerspective.getActionMap().put("Help", helpAction);
    m_mainPerspective.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
      helpKey, "Help");
    helpB.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent ae) {
        helpAction.actionPerformed(ae);
      }
    });

    templatesB.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        // createTemplateMenuPopup();
        PopupMenu popupMenu = new PopupMenu();
        List<String> builtinTemplates =
          m_mainPerspective.getTemplateManager()
            .getBuiltinTemplateDescriptions();
        List<String> pluginTemplates =
          m_mainPerspective.getTemplateManager()
            .getPluginTemplateDescriptions();

        for (final String desc : builtinTemplates) {
          MenuItem menuItem = new MenuItem(desc);
          menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              try {
                Flow templateFlow =
                  m_mainPerspective.getTemplateManager().getTemplateFlow(desc);
                m_mainPerspective.addTab(desc);
                m_mainPerspective.getCurrentLayout().setFlow(templateFlow);
              } catch (WekaException ex) {
                m_mainPerspective.showErrorDialog(ex);
              }
            }
          });
          popupMenu.add(menuItem);
        }
        if (builtinTemplates.size() > 0 && pluginTemplates.size() > 0) {
          popupMenu.addSeparator();
        }
        for (final String desc : pluginTemplates) {
          MenuItem menuItem = new MenuItem(desc);
          menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              try {
                Flow templateFlow =
                  m_mainPerspective.getTemplateManager().getTemplateFlow(desc);
                m_mainPerspective.addTab(desc);
                m_mainPerspective.getCurrentLayout().setFlow(templateFlow);
              } catch (WekaException ex) {
                m_mainPerspective.showErrorDialog(ex);
              }
            }
          });
          popupMenu.add(menuItem);
        }
        templatesB.add(popupMenu);
        popupMenu.show(templatesB, 0, 0);
      }
    });
    templatesB
      .setEnabled(m_mainPerspective.getTemplateManager().numTemplates() > 0);

    final Action togglePerspectivesAction =
      new AbstractAction("Toggle perspectives") {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (!Utils
            .getDontShowDialog("weka.gui.knowledgeflow.PerspectiveInfo")) {
            JCheckBox dontShow =
              new JCheckBox("Do not show this message again");
            Object[] stuff = new Object[2];
            stuff[0] =
              "Perspectives are environments that take over the\n"
                + "Knowledge Flow UI and provide major additional functionality.\n"
                + "Many perspectives will operate on a set of instances. Instances\n"
                + "Can be sent to a perspective by placing a DataSource on the\n"
                + "layout canvas, configuring it and then selecting \"Send to perspective\"\n"
                + "from the contextual popup menu that appears when you right-click on\n"
                + "it. Several perspectives are built in to the Knowledge Flow, others\n"
                + "can be installed via the package manager.\n";
            stuff[1] = dontShow;

            JOptionPane.showMessageDialog(m_mainPerspective, stuff,
              "Perspective information", JOptionPane.OK_OPTION);

            if (dontShow.isSelected()) {
              try {
                Utils
                  .setDontShowDialog("weka.gui.Knowledgeflow.PerspectiveInfo");
              } catch (Exception ex) {
                // quietly ignore
              }
            }
          }
          if (m_mainPerspective.getMainApplication()
            .isPerspectivesToolBarVisible()) {
            m_mainPerspective.getMainApplication().hidePerspectivesToolBar();
          } else {
            m_mainPerspective.getMainApplication().showPerspectivesToolBar();
          }
          m_mainPerspective.revalidate();
          m_mainPerspective.notifyIsDirty();
        }
      };
    KeyStroke togglePerspectivesKey =
      KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK);
    m_mainPerspective.getActionMap().put("Toggle perspectives",
      togglePerspectivesAction);
    m_mainPerspective.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
      togglePerspectivesKey, "Toggle perspectives");
    togglePerspectivesB.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        togglePerspectivesAction.actionPerformed(e);
      }
    });

    addWidgetToToolBar(fixedTools, Widgets.ZOOM_IN_BUTTON.toString(), zoomInB);
    addMenuItemToMenu("View", Widgets.ZOOM_IN_BUTTON.toString(), zoomInAction,
      zoomInKey);
    addWidgetToToolBar(fixedTools, Widgets.ZOOM_OUT_BUTTON.toString(), zoomOutB);
    addMenuItemToMenu("View", Widgets.ZOOM_OUT_BUTTON.toString(),
      zoomOutAction, zoomOutKey);
    fixedTools.addSeparator();
    addWidgetToToolBar(fixedTools, Widgets.SELECT_ALL_BUTTON.toString(),
      selectAllB);
    addWidgetToToolBar(fixedTools, Widgets.CUT_BUTTON.toString(), cutB);
    addMenuItemToMenu("Edit", Widgets.CUT_BUTTON.toString(), cutAction, cutKey);
    addWidgetToToolBar(fixedTools, Widgets.COPY_BUTTON.toString(), copyB);
    addMenuItemToMenu("Edit", Widgets.COPY_BUTTON.toString(), copyAction,
      copyKey);
    addMenuItemToMenu("Edit", Widgets.PASTE_BUTTON.toString(), pasteAction,
      pasteKey);
    addWidgetToToolBar(fixedTools, Widgets.DELETE_BUTTON.toString(), deleteB);
    addMenuItemToMenu("Edit", Widgets.DELETE_BUTTON.toString(), deleteAction,
      deleteKey);
    addWidgetToToolBar(fixedTools, Widgets.PASTE_BUTTON.toString(), pasteB);
    addWidgetToToolBar(fixedTools, Widgets.UNDO_BUTTON.toString(), undoB);
    addMenuItemToMenu("Edit", Widgets.UNDO_BUTTON.toString(), undoAction,
      undoKey);
    addWidgetToToolBar(fixedTools, Widgets.NOTE_BUTTON.toString(), noteB);
    addMenuItemToMenu("Insert", Widgets.NOTE_BUTTON.toString(), noteAction,
      noteKey);
    fixedTools.addSeparator();
    addWidgetToToolBar(fixedTools, Widgets.SNAP_TO_GRID_BUTTON.toString(),
      snapToGridB);
    fixedTools.addSeparator();
    addWidgetToToolBar(fixedTools, Widgets.NEW_FLOW_BUTTON.toString(), newB);
    addMenuItemToMenu("File", Widgets.NEW_FLOW_BUTTON.toString(), newAction,
      newKey);
    addWidgetToToolBar(fixedTools, Widgets.SAVE_FLOW_BUTTON.toString(), saveB);
    addMenuItemToMenu("File", Widgets.LOAD_FLOW_BUTTON.toString(), openAction,
      openKey);
    addMenuItemToMenu("File", Widgets.SAVE_FLOW_BUTTON.toString(), saveAction,
      saveKey);
    addWidgetToToolBar(fixedTools, Widgets.SAVE_FLOW_AS_BUTTON.toString(),
      saveBB);
    addMenuItemToMenu("File", Widgets.SAVE_FLOW_AS_BUTTON.toString(),
      saveAction, saveAsKey);
    addWidgetToToolBar(fixedTools, Widgets.LOAD_FLOW_BUTTON.toString(), loadB);
    addWidgetToToolBar(fixedTools, Widgets.TEMPLATES_BUTTON.toString(),
      templatesB);
    fixedTools.addSeparator();
    addWidgetToToolBar(fixedTools,
      Widgets.TOGGLE_PERSPECTIVES_BUTTON.toString(), togglePerspectivesB);
    addWidgetToToolBar(fixedTools, Widgets.HELP_BUTTON.toString(), helpB);
    Dimension d = undoB.getPreferredSize();
    Dimension d2 = fixedTools.getMinimumSize();
    Dimension d3 = new Dimension(d2.width, d.height + 4);
    fixedTools.setPreferredSize(d3);
    fixedTools.setMaximumSize(d3);
    fixedTools.setFloatable(false);
    add(fixedTools, BorderLayout.EAST);
  }

  /**
   * Add a widget to a toolbar
   *
   * @param toolBar the toolbar to add to
   * @param widgetName the name of the widget
   * @param widget the widget component itself
   */
  protected void addWidgetToToolBar(JToolBar toolBar, String widgetName,
    JComponent widget) {
    toolBar.add(widget);
    m_widgetMap.put(widgetName, widget);
  }

  /**
   * Add a menu item to a named menu
   *
   * @param topMenu the name of the menu to add to
   * @param menuItem the entry text for the item itself
   * @param action the action associated with the menu item
   * @param accelerator the keystroke accelerator to associate with the item
   */
  protected void addMenuItemToMenu(String topMenu, String menuItem,
    final Action action, KeyStroke accelerator) {
    JMenuItem newItem = m_menuItemMap.get(menuItem);
    if (newItem != null) {
      throw new IllegalArgumentException("The menu item '" + menuItem
        + "' already exists!");
    }
    newItem = new JMenuItem(menuItem);
    if (accelerator != null) {
      newItem.setAccelerator(accelerator);
    }
    newItem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        action.actionPerformed(e);
      }
    });

    JMenu topJ = m_menuMap.get(topMenu);
    if (topJ == null) {
      topJ = new JMenu();
      topJ.setText(topMenu);
      m_menuMap.put(topMenu, topJ);
    }

    topJ.add(newItem);
    m_menuItemMap.put(menuItem, newItem);
  }

  /**
   * Enable/disable a named widget
   * 
   * @param widgetName the name of the widget to enable/disable
   * @param enable true if the widget should be enabled; false otherwise
   * @see {@code Widget} enum
   */
  public void enableWidget(String widgetName, boolean enable) {
    JComponent widget = m_widgetMap.get(widgetName);
    if (widget != null) {
      widget.setEnabled(enable);
    }
    JMenuItem mI = m_menuItemMap.get(widgetName);
    if (mI != null) {
      mI.setEnabled(enable);
    }
  }

  public void enableWidgets(String... widgetNames) {
    for (String s : widgetNames) {
      enableWidget(s, true);
    }
  }

  public void disableWidgets(String... widgetNames) {
    for (String s : widgetNames) {
      enableWidget(s, false);
    }
  }

  private void popupHelp(final JButton helpB) {
    try {
      helpB.setEnabled(false);

      InputStream inR =
        this.getClass().getClassLoader()
          .getResourceAsStream("weka/gui/knowledgeflow/README_KnowledgeFlow");

      StringBuilder helpHolder = new StringBuilder();
      LineNumberReader lnr = new LineNumberReader(new InputStreamReader(inR));

      String line;

      while ((line = lnr.readLine()) != null) {
        helpHolder.append(line + "\n");
      }

      lnr.close();
      final javax.swing.JFrame jf = new javax.swing.JFrame();
      jf.getContentPane().setLayout(new java.awt.BorderLayout());
      final JTextArea ta = new JTextArea(helpHolder.toString());
      ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
      ta.setEditable(false);
      final JScrollPane sp = new JScrollPane(ta);
      jf.getContentPane().add(sp, java.awt.BorderLayout.CENTER);
      jf.addWindowListener(new java.awt.event.WindowAdapter() {
        @Override
        public void windowClosing(java.awt.event.WindowEvent e) {
          helpB.setEnabled(true);
          jf.dispose();
        }
      });
      jf.setSize(600, 600);
      jf.setVisible(true);

    } catch (Exception ex) {
      ex.printStackTrace();
      helpB.setEnabled(true);
    }
  }

  public JComponent getWidget(String widgetName) {
    return m_widgetMap.get(widgetName);
  }

  public JMenuItem getMenuItem(String menuItemName) {
    return m_menuItemMap.get(menuItemName);
  }

  /**
   * Enum containing all the widgets provided by the toolbar. The toString()
   * method provides the corresponding menu entry text for each widget.
   */
  public enum Widgets {
    ZOOM_IN_BUTTON("Zoom In"), ZOOM_OUT_BUTTON("Zoom Out"), SELECT_ALL_BUTTON(
      "Select All"), CUT_BUTTON("Cut"), COPY_BUTTON("Copy"), DELETE_BUTTON(
      "Delete"), PASTE_BUTTON("Paste"), UNDO_BUTTON("Undo"), NOTE_BUTTON(
      "New Note"), SNAP_TO_GRID_BUTTON("Snap to Grid"), NEW_FLOW_BUTTON(
      "New Layout"), SAVE_FLOW_BUTTON("Save"),
    SAVE_FLOW_AS_BUTTON("Save As..."), LOAD_FLOW_BUTTON("Open..."),
    TEMPLATES_BUTTON("Template"), TOGGLE_PERSPECTIVES_BUTTON(
      "Toggle Perspectives"), HELP_BUTTON("help..."),
    POINTER_BUTTON("Pointer"), PLAY_PARALLEL_BUTTON("Launch"),
    PLAY_SEQUENTIAL_BUTTON("Launch Squential"), STOP_BUTTON("Stop");

    String m_name;

    Widgets(String name) {
      m_name = name;
    }

    @Override
    public String toString() {
      return m_name;
    }
  }

  /**
   * Get the list of menus
   *
   * @return a list of {@code JMenu}s
   */
  public List<JMenu> getMenus() {
    List<JMenu> menuList = new ArrayList<JMenu>();
    for (Map.Entry<String, JMenu> e : m_menuMap.entrySet()) {
      menuList.add(e.getValue());
    }
    return menuList;
  }
}
