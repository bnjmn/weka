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
 *    MainKFPerspective.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow;

import weka.core.Defaults;
import weka.core.Environment;
import weka.core.Instances;
import weka.core.Memory;
import weka.core.Settings;
import weka.core.WekaException;
import weka.gui.AbstractPerspective;
import weka.gui.CloseableTabTitle;
import weka.gui.ExtensionFileFilter;
import weka.gui.GUIApplication;
import weka.gui.PerspectiveInfo;
import weka.gui.WorkbenchDefaults;
import weka.gui.explorer.PreprocessPanel;
import weka.knowledgeflow.Flow;
import weka.knowledgeflow.JSONFlowLoader;
import weka.knowledgeflow.JSONFlowUtils;
import weka.knowledgeflow.KFDefaults;
import weka.knowledgeflow.LoggingLevel;
import weka.knowledgeflow.StepManagerImpl;
import weka.knowledgeflow.steps.MemoryBasedDataSource;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Main perspective for the Knowledge flow application
 *
 * @author Mark Hall
 */
@PerspectiveInfo(ID = "knowledgeflow.main", title = "Data mining processes",
  toolTipText = "Data mining processes",
  iconPath = "weka/gui/weka_icon_new_small.png")
public class MainKFPerspective extends AbstractPerspective {

  /**
   * Key for the environment variable that holds the parent directory of a
   * loaded flow
   */
  public static final String FLOW_PARENT_DIRECTORY_VARIABLE_KEY =
    "Internal.knowledgeflow.directory";

  /** File extension for undo point files */
  public static final String FILE_EXTENSION_JSON = ".kf";

  /** For serialization */
  private static final long serialVersionUID = 3986047323839299447L;

  /** For monitoring Memory consumption */
  private static Memory m_memory = new Memory(true);

  /** Count for new "untitled" tabs */
  protected int m_untitledCount = 1;

  /** Whether to allow multiple tabs */
  protected boolean m_allowMultipleTabs = true;

  /** The current step selected from the design pallete */
  protected StepManagerImpl m_palleteSelectedStep;

  /** Holds the tabs of the perspective */
  protected JTabbedPane m_flowTabs = new JTabbedPane();

  /** List of layouts - one for each tab */
  protected List<VisibleLayout> m_flowGraphs = new ArrayList<VisibleLayout>();

  /** The jtree holding steps */
  protected StepTree m_stepTree;

  /** The paste buffer */
  protected String m_pasteBuffer;

  /** The file chooser for loading layout files */
  protected JFileChooser m_FileChooser = new JFileChooser(new File(
    System.getProperty("user.dir")));

  /**
   * The file chooser for saving layout files (only supports saving as json .kf
   * files
   */
  protected JFileChooser m_saveFileChooser = new JFileChooser(new File(
    System.getProperty("user.dir")));

  /** Manages template flows */
  protected TemplateManager m_templateManager = new TemplateManager();

  /** Main toolbar */
  protected MainKFPerspectiveToolBar m_mainToolBar;

  /**
   * Construct a new MainKFPerspective
   */
  public MainKFPerspective() {
    m_isLoaded = true;
    m_isActive = true;
    setLayout(new BorderLayout());
    m_stepTree = new StepTree(this);
    DesignPanel designPanel = new DesignPanel(m_stepTree);

    // spit pane for design panel and flow tabs
    JSplitPane pane =
      new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, designPanel, m_flowTabs);
    pane.setOneTouchExpandable(true);
    add(pane, BorderLayout.CENTER);
    Dimension d = designPanel.getPreferredSize();
    d = new Dimension((int) (d.getWidth() * 1.5), (int) d.getHeight());
    designPanel.setPreferredSize(d);
    designPanel.setMinimumSize(d);

    m_flowTabs.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        int selected = m_flowTabs.getSelectedIndex();
        setActiveTab(selected);
      }
    });

    m_mainToolBar = new MainKFPerspectiveToolBar(this);
    add(m_mainToolBar, BorderLayout.NORTH);

    FileFilter nativeF = null;
    for (FileFilter f : Flow.FLOW_FILE_EXTENSIONS) {
      m_FileChooser.addChoosableFileFilter(f);
      if (((ExtensionFileFilter) f).getExtensions()[0].equals("."
        + JSONFlowLoader.EXTENSION)) {
        nativeF = f;
        m_saveFileChooser.addChoosableFileFilter(nativeF);
      }
    }

    if (nativeF != null) {
      m_FileChooser.setFileFilter(nativeF);
      m_saveFileChooser.setFileFilter(nativeF);
    }
  }

  /**
   * Return the currently selected step in the design palette
   *
   * @return the step selected in the design palette, or null if no step is
   *         selected
   */
  public StepManagerImpl getPalleteSelectedStep() {
    return m_palleteSelectedStep;
  }

  /**
   * Set a reference to the currently selected step in the design palette
   *
   * @param stepvisual the currently selected step
   */
  protected void setPalleteSelectedStep(StepManagerImpl stepvisual) {
    m_palleteSelectedStep = stepvisual;
  }

  /**
   * Popup an error dialog
   *
   * @param cause the exception associated with the error
   */
  public void showErrorDialog(Exception cause) {
    m_mainApplication.showErrorDialog(cause);
  }

  /**
   * Popup an information dialog
   *
   * @param information the information to display
   * @param title the title for the dialog
   * @param isWarning true if the dialog should be a warning dialog
   */
  public void
    showInfoDialog(Object information, String title, boolean isWarning) {
    m_mainApplication.showInfoDialog(information, title, isWarning);
  }

  /**
   * Set the current flow layout operation
   *
   * @param opp the operation to use
   */
  public void setFlowLayoutOperation(VisibleLayout.LayoutOperation opp) {
    // pass this through to the current visible tab. Main purpose
    // is to allow the Design palette to cancel any current operation
    // when the user clicks on a step in the jtree.
    if (getCurrentTabIndex() < getNumTabs() && getCurrentTabIndex() >= 0) {
      m_flowGraphs.get(getCurrentTabIndex()).setFlowLayoutOperation(opp);
    }
  }

  /**
   * Return true if the snap-to-grid button is selected
   *
   * @return true if snap-to-grid is turned on
   */
  public boolean getSnapToGrid() {
    return ((JToggleButton) m_mainToolBar
      .getWidget(MainKFPerspectiveToolBar.Widgets.SNAP_TO_GRID_BUTTON
        .toString())).isSelected();
  }

  /**
   * Clear the current selection in the design palette
   */
  public void clearDesignPaletteSelection() {
    m_stepTree.clearSelection();
  }

  /**
   * Get the contents of the paste buffer
   * 
   * @return the contents of the paste buffer (in JSON)
   */
  public String getPasteBuffer() {
    return m_pasteBuffer;
  }

  /**
   * Set the contents of the paste buffer
   *
   * @param serializedFlow (sub-)flow in JSON
   */
  protected void setPasteBuffer(String serializedFlow) {
    m_pasteBuffer = serializedFlow;
  }

  /**
   * Copy the supplied steps to the clipboard
   *
   * @param steps a list of steps to copy
   * @throws WekaException if a problem occurs
   */
  public void copyStepsToClipboard(List<StepVisual> steps) throws WekaException {
    if (steps.size() > 0) {
      m_pasteBuffer =
        VisibleLayout.serializeStepsToJSON(steps, "Clipboard copy");
      getMainToolBar().enableWidgets(
        MainKFPerspectiveToolBar.Widgets.PASTE_BUTTON.toString());
    }
  }

  public void copyFlowToClipboard(Flow flow) throws WekaException {
    m_pasteBuffer = JSONFlowUtils.flowToJSON(flow);
    getMainToolBar().enableWidgets(
      MainKFPerspectiveToolBar.Widgets.PASTE_BUTTON.toString());
  }

  /**
   * Get the template manager
   *
   * @return the template manager
   */
  public TemplateManager getTemplateManager() {
    return m_templateManager;
  }

  /**
   * Add a new untitled tab to the UI
   */
  public synchronized void addUntitledTab() {
    if (getNumTabs() == 0 || getAllowMultipleTabs()) {
      addTab("Untitled" + m_untitledCount++);
    } else {
      m_flowGraphs.get(getCurrentTabIndex()).stopFlow();
      m_flowGraphs.get(getCurrentTabIndex()).setFlow(new Flow());
    }
  }

  /**
   * Add a new titled tab to the UI
   *
   * @param tabTitle the title for the tab
   */
  public synchronized void addTab(String tabTitle) {
    VisibleLayout newLayout = new VisibleLayout(this);
    m_flowGraphs.add(newLayout);

    m_flowTabs.add(tabTitle, newLayout);
    m_flowTabs.setTabComponentAt(getNumTabs() - 1, new CloseableTabTitle(
      m_flowTabs, "(Ctrl+W)", new CloseableTabTitle.ClosingCallback() {
        @Override
        public void tabClosing(int tabIndex) {
          if (getAllowMultipleTabs()) {
            removeTab(tabIndex);
          }
        }
      }));

    setActiveTab(getNumTabs() - 1);
  }

  /**
   * Set the edited status for the current (visible) tab
   * 
   * @param edited true if the flow in the tab has been edited (but not saved)
   */
  public void setCurrentTabTitleEditedStatus(boolean edited) {
    CloseableTabTitle current =
      (CloseableTabTitle) m_flowTabs.getTabComponentAt(getCurrentTabIndex());
    current.setBold(edited);
  }

  /**
   * Get the index of the current (visible) tab
   *
   * @return the index of the visible tab
   */
  public int getCurrentTabIndex() {
    return m_flowTabs.getSelectedIndex();
  }

  /**
   * Get the number of open tabs
   *
   * @return the number of open tabs
   */
  public synchronized int getNumTabs() {
    return m_flowTabs.getTabCount();
  }

  /**
   * Get the title of the tab at the supplied index
   *
   * @param index the index of the tab to get the title for
   * @return the title of the tab
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public synchronized String getTabTitle(int index) {
    if (index < getNumTabs() && index >= 0) {
      return m_flowTabs.getTitleAt(index);
    }

    throw new IndexOutOfBoundsException("Tab index " + index
      + " is out of range!");
  }

  /**
   * Set the active (visible) tab
   *
   * @param tabIndex the index of the tab to make active
   */
  public synchronized void setActiveTab(int tabIndex) {
    if (tabIndex < getNumTabs() && tabIndex >= 0) {
      m_flowTabs.setSelectedIndex(tabIndex);

      VisibleLayout current = getCurrentLayout();
      m_mainToolBar.enableWidget(
        MainKFPerspectiveToolBar.Widgets.SAVE_FLOW_BUTTON.toString(),
        !current.isExecuting());
      m_mainToolBar.enableWidget(
        MainKFPerspectiveToolBar.Widgets.SAVE_FLOW_AS_BUTTON.toString(),
        !current.isExecuting());
      m_mainToolBar.enableWidget(
        MainKFPerspectiveToolBar.Widgets.PLAY_PARALLEL_BUTTON.toString(),
        !current.isExecuting());
      m_mainToolBar.enableWidget(
        MainKFPerspectiveToolBar.Widgets.PLAY_SEQUENTIAL_BUTTON.toString(),
        !current.isExecuting());
      m_mainToolBar.enableWidget(
        MainKFPerspectiveToolBar.Widgets.ZOOM_OUT_BUTTON.toString(),
        !current.isExecuting());
      m_mainToolBar.enableWidget(
        MainKFPerspectiveToolBar.Widgets.ZOOM_IN_BUTTON.toString(),
        !current.isExecuting());

      if (current.getZoomSetting() == 50) {
        m_mainToolBar.enableWidget(
          MainKFPerspectiveToolBar.Widgets.ZOOM_OUT_BUTTON.toString(), false);
      }
      if (current.getZoomSetting() == 200) {
        m_mainToolBar.enableWidget(
          MainKFPerspectiveToolBar.Widgets.ZOOM_IN_BUTTON.toString(), false);
      }

      m_mainToolBar.enableWidget(
        MainKFPerspectiveToolBar.Widgets.CUT_BUTTON.toString(), current
          .getSelectedSteps().size() > 0 && !current.isExecuting());
      m_mainToolBar.enableWidget(
        MainKFPerspectiveToolBar.Widgets.COPY_BUTTON.toString(), current
          .getSelectedSteps().size() > 0 && !current.isExecuting());
      m_mainToolBar.enableWidget(
        MainKFPerspectiveToolBar.Widgets.DELETE_BUTTON.toString(), current
          .getSelectedSteps().size() > 0 && !current.isExecuting());
      m_mainToolBar.enableWidget(
        MainKFPerspectiveToolBar.Widgets.SELECT_ALL_BUTTON.toString(),
        current.numSteps() > 0 && !current.isExecuting());
      m_mainToolBar.enableWidget(
        MainKFPerspectiveToolBar.Widgets.PASTE_BUTTON.toString(),
        getPasteBuffer() != null && getPasteBuffer().length() > 0
          && !current.isExecuting());
      m_mainToolBar.enableWidget(
        MainKFPerspectiveToolBar.Widgets.STOP_BUTTON.toString(),
        current.isExecuting());
      m_mainToolBar.enableWidget(
        MainKFPerspectiveToolBar.Widgets.UNDO_BUTTON.toString(),
        !current.isExecuting() && current.getUndoBufferSize() > 0);

      m_mainToolBar.enableWidget(
        MainKFPerspectiveToolBar.Widgets.NEW_FLOW_BUTTON.toString(),
        !current.isExecuting() && getAllowMultipleTabs());
    }
  }

  /**
   * Close all the open tabs
   */
  public void closeAllTabs() {
    for (int i = 0; i < getNumTabs(); i++) {
      removeTab(i);
    }
  }

  /**
   * Remove/close a tab
   *
   * @param tabIndex the index of the tab to close
   */
  public synchronized void removeTab(int tabIndex) {
    if (tabIndex < 0 || tabIndex >= getNumTabs()) {
      return;
    }

    if (m_flowGraphs.get(tabIndex).getEdited()) {
      String tabTitle = m_flowTabs.getTitleAt(tabIndex);
      String message =
        "\"" + tabTitle + "\" has been modified. Save changes "
          + "before closing?";
      int result =
        JOptionPane.showConfirmDialog(this, message, "Save changes",
          JOptionPane.YES_NO_CANCEL_OPTION);

      if (result == JOptionPane.YES_OPTION) {
        saveLayout(tabIndex, false);
      } else if (result == JOptionPane.CANCEL_OPTION) {
        return;
      }
    }
    m_flowTabs.remove(tabIndex);
    m_flowGraphs.remove(tabIndex);
    if (getCurrentTabIndex() < 0) {
      m_mainToolBar.disableWidgets(
        MainKFPerspectiveToolBar.Widgets.SAVE_FLOW_BUTTON.toString(),
        MainKFPerspectiveToolBar.Widgets.SAVE_FLOW_AS_BUTTON.toString());
    }
  }

  /**
   * Get the flow layout for the current (visible) tab
   *
   * @return the current flow layout
   */
  public VisibleLayout getCurrentLayout() {
    if (getCurrentTabIndex() >= 0) {
      return m_flowGraphs.get(getCurrentTabIndex());
    }

    return null;
  }

  /**
   * Get the flow layout at the supplied index
   *
   * @param index the index of the flow layout to get
   * @return the flow layout at the index
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public VisibleLayout getLayoutAt(int index) {
    if (index >= 0 && index < m_flowGraphs.size()) {
      return m_flowGraphs.get(index);
    }

    throw new IndexOutOfBoundsException("Flow index " + index
      + " is out of range!");
  }

  /**
   * Returns true if the perspective is allowing multiple tabs to be open
   *
   * @return true if multiple tabs are allowed
   */
  public boolean getAllowMultipleTabs() {
    return m_allowMultipleTabs;
  }

  /**
   * Set whether multiple tabs are allowed
   *
   * @param allow true if multiple tabs are allowed
   */
  public void setAllowMultipleTabs(boolean allow) {
    m_allowMultipleTabs = allow;
  }

  /**
   * Get the value of a setting for the main perspective
   *
   * @param propKey the key of the setting to get
   * @param defaultVal the default value to use if nothing is set yet
   * @param <T> the type of the setting
   * @return the value (or default value) for the setting
   */
  public <T> T getSetting(Settings.SettingKey propKey, T defaultVal) {
    return m_mainApplication.getApplicationSettings().getSetting(
      getPerspectiveID(), propKey, defaultVal, null);
  }

  public void notifyIsDirty() {
    firePropertyChange("PROP_DIRTY", null, null);
  }

  /**
   * Save a particular flow layout
   *
   * @param tabIndex the index of the tab containing the flow to asave
   * @param showDialog true if a file dialog should be popped up
   */
  protected void saveLayout(int tabIndex, boolean showDialog) {
    m_flowGraphs.get(tabIndex).saveLayout(showDialog);
  }

  /**
   * Load a flow layout. Prompts via a filechooser
   */
  public void loadLayout() {
    File lFile = null;
    int returnVal = m_FileChooser.showOpenDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      lFile = m_FileChooser.getSelectedFile();
      loadLayout(lFile, true);
    }
  }

  /**
   * Load a flow layout.
   *
   * @param fFile the file to load
   * @param newTab true if the flow should be loaded into a new tab
   */
  public void loadLayout(File fFile, boolean newTab) {
    File absoluteF = fFile.getAbsoluteFile();
    if (!newTab) {
      m_flowGraphs.get(getCurrentTabIndex()).loadLayout(fFile, false);
      m_flowGraphs.get(getCurrentTabIndex()).getEnvironment()
        .addVariable(FLOW_PARENT_DIRECTORY_VARIABLE_KEY, absoluteF.getParent());
    } else {
      String tabTitle = fFile.toString();
      tabTitle =
        tabTitle.substring(tabTitle.lastIndexOf(File.separator) + 1,
          tabTitle.length());
      if (tabTitle.lastIndexOf('.') > 0) {
        tabTitle = tabTitle.substring(0, tabTitle.lastIndexOf('.'));
      }
      addTab(tabTitle);
      VisibleLayout current = getCurrentLayout();
      current.loadLayout(fFile, false);
      current.getEnvironment().addVariable(FLOW_PARENT_DIRECTORY_VARIABLE_KEY,
        absoluteF.getParent());

      // set the enabled status of the toolbar widgets
      setActiveTab(getCurrentTabIndex());
    }
  }

  /**
   * Set the title of the current (visible) tab
   *
   * @param title the tab title to use
   */
  protected void setCurrentTabTitle(String title) {
    m_flowTabs.setTitleAt(getCurrentTabIndex(), title);
  }

  /**
   * Return true if memory is running low
   *
   * @return true if memory is running low
   */
  public boolean isMemoryLow() {
    return m_memory.memoryIsLow();
  }

  /**
   * Print a warning if memory is low (and show a GUI dialog if running in a
   * graphical environment)
   *
   * @return true if user opts to continue (in the GUI case)
   */
  public boolean showMemoryIsLow() {
    return m_memory.showMemoryIsLow();
  }

  public boolean getDebug() {
    return getMainApplication()
      .getApplicationSettings()
      .getSetting(getPerspectiveID(), KFDefaults.LOGGING_LEVEL_KEY,
        LoggingLevel.BASIC, Environment.getSystemWide()).ordinal() == LoggingLevel.DEBUGGING
      .ordinal();
  }

  /**
   * Get the main toolbar
   *
   * @return the main toolbar
   */
  public MainKFPerspectiveToolBar getMainToolBar() {
    return m_mainToolBar;
  }

  /**
   * Set active status of this perspective. True indicates that this perspective
   * is the visible active perspective in the application
   *
   * @param active true if this perspective is the active one
   */
  @Override
  public void setActive(boolean active) {
    if (active) {
      // are we operating as a non-primary perspective somewhere other
      // than in the Knowledge Flow?
      if (!getMainApplication().getApplicationID().equalsIgnoreCase(
        KFDefaults.APP_ID)) {
        // check number of tabs and create a new one if none are open
        if (getNumTabs() == 0) {
          addUntitledTab();
        }
      }
    }
  }

  /**
   * Get an ordered list of menus to appear in the main menu bar. Return null
   * for no menus
   *
   * @return a list of menus to appear in the main menu bar or null for no menus
   */
  @Override
  public List<JMenu> getMenus() {
    return m_mainToolBar.getMenus();
  }

  /**
   * Get the default settings for this perspective (or null if there are none)
   *
   * @return the default settings for this perspective, or null if the
   *         perspective does not have any settings
   */
  @Override
  public Defaults getDefaultSettings() {
    return new KFDefaults();
  }

  /**
   * Returns true if this perspective is OK with being an active perspective -
   * i.e. the user can click on this perspective at this time in the perspective
   * toolbar. For example, a Perspective might return false from this method if
   * it needs a set of instances to operate but none have been supplied yet.
   *
   * @return true if this perspective can be active at the current time
   */
  @Override
  public boolean okToBeActive() {
    return true;
  }

  /**
   * Called when the user alters settings. The settings altered by the user are
   * not necessarily ones related to this perspective
   */
  @Override
  public void settingsChanged() {
    Settings settings = getMainApplication().getApplicationSettings();
    int fontSize =
      settings.getSetting(KFDefaults.MAIN_PERSPECTIVE_ID,
        new Settings.SettingKey(KFDefaults.MAIN_PERSPECTIVE_ID
          + ".logMessageFontSize", "", ""), -1);
    for (VisibleLayout v : m_flowGraphs) {
      v.getLogPanel().setLoggingFontSize(fontSize);
    }
  }

  /**
   * Returns true if this perspective can do something meaningful with a set of
   * instances
   *
   * @return true if this perspective accepts instances
   */
  @Override
  public boolean acceptsInstances() {
    // allow instances only if running in the Workbench and the user has
    // opted to manually send loaded instances to perspectives. This is because
    // we create a new flow containing a MemoryDataSource when receiving
    // a set of instances. The MemoryDataSource keeps a reference to the data.
    GUIApplication mainApp = getMainApplication();
    if (mainApp.getApplicationID().equals(WorkbenchDefaults.APP_ID)) {
      boolean sendToAll =
        mainApp.getApplicationSettings().getSetting(
          PreprocessPanel.PreprocessDefaults.ID,
          PreprocessPanel.PreprocessDefaults.ALWAYS_SEND_INSTANCES_TO_ALL_KEY,
          PreprocessPanel.PreprocessDefaults.ALWAYS_SEND_INSTANCES_TO_ALL,
          Environment.getSystemWide());

      return !sendToAll;
    }
    return false;
  }

  /**
   * Set instances (if this perspective can use them)
   *
   * @param instances the instances
   */
  @Override
  public void setInstances(Instances instances) {
    addUntitledTab();

    VisibleLayout newL = m_flowGraphs.get(m_flowGraphs.size() - 1);
    MemoryBasedDataSource memoryBasedDataSource = new MemoryBasedDataSource();
    memoryBasedDataSource.setInstances(instances);

    StepManagerImpl newS = new StepManagerImpl(memoryBasedDataSource);
    newL.addStep(newS, 30, 30);
  }
}
