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
 *    VisibleLayout.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow;

import weka.core.Copyright;
import weka.core.Environment;
import weka.core.PluginManager;
import weka.core.Settings;
import weka.core.WekaException;
import weka.gui.beans.LogPanel;
import weka.knowledgeflow.BaseExecutionEnvironment;
import weka.knowledgeflow.ExecutionFinishedCallback;
import weka.knowledgeflow.Flow;
import weka.knowledgeflow.FlowExecutor;
import weka.knowledgeflow.FlowRunner;
import weka.knowledgeflow.JSONFlowUtils;
import weka.knowledgeflow.KFDefaults;
import weka.knowledgeflow.LogManager;
import weka.knowledgeflow.StepManager;
import weka.knowledgeflow.StepManagerImpl;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

/**
 * Panel that wraps a flow and makes it visible in the KnowledgeFlow, along with
 * it's associated log panel
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class VisibleLayout extends JPanel {

  /** the flow layout width */
  protected static final int LAYOUT_WIDTH = 2560;

  /** the flow layout height */
  protected static final int LAYOUT_HEIGHT = 1440;

  /** The scrollbar increment of the layout scrollpane */
  protected static final int SCROLLBAR_INCREMENT = 50;

  private static final long serialVersionUID = -3644458365810712479L;

  protected Flow m_flow;

  /** The log panel to use for this layout */
  protected KFLogPanel m_logPanel = new KFLogPanel();

  /** Current zoom setting for this layout */
  protected int m_zoomSetting = 100;

  /** Current path on disk for this flow */
  protected File m_filePath;

  /** Keeps track of any highlighted steps on the canvas */
  protected List<StepVisual> m_selectedSteps = new ArrayList<StepVisual>();

  /** Keeps track of the undo buffer for this flow */
  protected Stack<File> m_undoBuffer = new Stack<File>();

  /** True if the flow has been edited, but not yet saved */
  protected boolean m_hasBeenEdited;

  /** The flow executor used to execute the flow */
  protected FlowExecutor m_flowExecutor;

  /** Environment variables to use */
  protected Environment m_env = new Environment();

  /** True if this flow is executing */
  protected boolean m_isExecuting;

  /** A reference to the main perspective */
  protected MainKFPerspective m_mainPerspective;

  /** The steps to be rendered on this layout */
  protected List<StepVisual> m_renderGraph = new ArrayList<StepVisual>();

  /** The current layout operation */
  protected LayoutOperation m_userOpp = LayoutOperation.NONE;

  /** The panel used to render the flow */
  protected LayoutPanel m_layout;

  /** Reference to the step being edited (if any) */
  protected StepVisual m_editStep;

  /**
   * The name of the user-selected connection if the user has initiated a
   * connection from a source step (stored in m_editStep)
   */
  protected String m_editConnection;

  /**
   * Constructor
   *
   * @param mainPerspective the main Knowledge Flow perspective
   */
  public VisibleLayout(MainKFPerspective mainPerspective) {
    super();
    setLayout(new BorderLayout());

    m_flow = new Flow();
    m_mainPerspective = mainPerspective;

    m_layout = new LayoutPanel(this);
    JPanel p1 = new JPanel();
    p1.setLayout(new BorderLayout());
    JScrollPane js = new JScrollPane(m_layout);
    p1.add(js, BorderLayout.CENTER);
    js.getVerticalScrollBar().setUnitIncrement(
      KFDefaults.SCROLL_BAR_INCREMENT_LAYOUT);
    js.getHorizontalScrollBar().setUnitIncrement(
      KFDefaults.SCROLL_BAR_INCREMENT_LAYOUT);

    m_layout.setSize(m_mainPerspective.getSetting(KFDefaults.LAYOUT_WIDTH_KEY,
      KFDefaults.LAYOUT_WIDTH), m_mainPerspective.getSetting(
      KFDefaults.LAYOUT_HEIGHT_KEY, KFDefaults.LAYOUT_HEIGHT));
    Dimension d = m_layout.getPreferredSize();
    m_layout.setMinimumSize(d);
    m_layout.setPreferredSize(d);

    m_logPanel = new KFLogPanel();
    setUpLogPanel(m_logPanel);
    Dimension d2 = new Dimension(100, 170);
    m_logPanel.setPreferredSize(d2);
    m_logPanel.setMinimumSize(d2);
    m_filePath = new File("-NONE-");

    JSplitPane p2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, p1, m_logPanel);
    p2.setOneTouchExpandable(true);
    p2.setDividerLocation(0.7);
    // p2.setDividerLocation(500);
    p2.setResizeWeight(1.0);

    add(p2, BorderLayout.CENTER);
  }

  /**
   * Get a list of the steps (wrapped in {@code StepVisual} instances) that make
   * up the flow rendered/edited by this panel
   *
   * @return a list of steps
   */
  protected List<StepVisual> getRenderGraph() {
    return m_renderGraph;
  }

  /**
   * Get the main perspective that owns this layout
   *
   * @return the main perspective
   */
  protected MainKFPerspective getMainPerspective() {
    return m_mainPerspective;
  }

  /**
   * Get the step currently being "edited". Returns null if no step is being
   * edited
   *
   * @return the step being edited
   */
  protected StepVisual getEditStep() {
    return m_editStep;
  }

  /**
   * Set the step to "edit"
   *
   * @param step the step to edit
   */
  protected void setEditStep(StepVisual step) {
    m_editStep = step;
  }

  /**
   * If the user has initiated the connection process, then this method returns
   * the name of the connection involved
   *
   * @return the name of the connection if the user has initiated the connection
   *         process
   */
  protected String getEditConnection() {
    return m_editConnection;
  }

  /**
   * Set the name of the connection involved in a connection operation
   *
   * @param connName the name of the connection
   */
  protected void setEditConnection(String connName) {
    m_editConnection = connName;
  }

  /**
   * Get the list of selected steps. The list will be empty if there are no
   * selected (highlighted) steps on the canvas.
   *
   * @return the list of selected steps
   */
  protected List<StepVisual> getSelectedSteps() {
    return m_selectedSteps;
  }

  /**
   * Set a list of steps to be considered "selected". Setting an empty list
   * clears any current selection.
   *
   * @param selected a list of steps to be considered as selected
   */
  protected void setSelectedSteps(List<StepVisual> selected) {
    // turn off any set ones
    for (StepVisual s : m_selectedSteps) {
      s.setDisplayConnectors(false);
    }

    m_selectedSteps = selected;

    // highlight any new ones
    for (StepVisual s : m_selectedSteps) {
      s.setDisplayConnectors(true);
    }

    if (m_selectedSteps.size() > 0) {
      m_mainPerspective.getMainToolBar().enableWidgets(
        MainKFPerspectiveToolBar.Widgets.CUT_BUTTON.toString(),
        MainKFPerspectiveToolBar.Widgets.COPY_BUTTON.toString(),
        MainKFPerspectiveToolBar.Widgets.DELETE_BUTTON.toString());
    } else {
      m_mainPerspective.getMainToolBar().disableWidgets(
        MainKFPerspectiveToolBar.Widgets.CUT_BUTTON.toString(),
        MainKFPerspectiveToolBar.Widgets.COPY_BUTTON.toString(),
        MainKFPerspectiveToolBar.Widgets.DELETE_BUTTON.toString());
    }
  }

  /**
   * Removes the currently selected list of steps from the layout
   *
   * @throws WekaException if a problem occurs
   */
  protected void removeSelectedSteps() throws WekaException {
    addUndoPoint();
    for (StepVisual v : m_selectedSteps) {
      m_flow.removeStep(v.getStepManager());
      m_renderGraph.remove(v);
      m_layout.remove(v);

      String key =
        v.getStepName() + "$" + v.getStepManager().getManagedStep().hashCode();
      m_logPanel.statusMessage(key + "|remove");
    }

    setSelectedSteps(new ArrayList<StepVisual>());
    m_mainPerspective.getMainToolBar().disableWidgets(
      MainKFPerspectiveToolBar.Widgets.DELETE_BUTTON.toString());
    m_mainPerspective.getMainToolBar().enableWidget(
      MainKFPerspectiveToolBar.Widgets.SELECT_ALL_BUTTON.toString(),
      m_flow.size() > 0);
    m_layout.repaint();
  }

  /**
   * Copies the currently selected list of steps to the global clipboard
   *
   * @throws WekaException if a problem occurs
   */
  protected void copySelectedStepsToClipboard() throws WekaException {
    copyStepsToClipboard(getSelectedSteps());
  }

  /**
   * Copies the supplied list of steps to the global clipboard
   *
   * @param steps the steps to copy to the clipboard
   * @throws WekaException if a problem occurs
   */
  protected void copyStepsToClipboard(List<StepVisual> steps)
    throws WekaException {
    m_mainPerspective.copyStepsToClipboard(steps);
  }

  /**
   * Pastes the contents (if any) of the global clipboard to this layout
   *
   * @param x the x coordinate to paste at
   * @param y the y cooridinate to paste at
   * @throws WekaException if a problem occurs
   */
  protected void pasteFromClipboard(int x, int y) throws WekaException {

    addUndoPoint();

    Flow fromPaste = Flow.JSONToFlow(m_mainPerspective.getPasteBuffer(), true);
    List<StepVisual> added = addAll(fromPaste.getSteps(), false);

    // adjust x,y coords of pasted steps. Look for the smallest
    // x and the smallest y (top left corner of bounding box)
    int minX = Integer.MAX_VALUE;
    int minY = Integer.MAX_VALUE;
    for (StepVisual v : added) {
      if (v.getX() < minX) {
        minX = v.getX();
      }
      if (v.getY() < minY) {
        minY = v.getY();
      }
    }
    int deltaX = x - minX;
    int deltaY = y - minY;
    for (StepVisual v : added) {
      v.setX(v.getX() + deltaX);
      v.setY(v.getY() + deltaY);
    }

    m_layout.revalidate();
    m_layout.repaint();

    setSelectedSteps(added);
  }

  /**
   * Add an undo point
   */
  protected void addUndoPoint() {
    try {
      File tempFile =
        File.createTempFile("knowledgeflow",
          MainKFPerspective.FILE_EXTENSION_JSON);
      tempFile.deleteOnExit();
      JSONFlowUtils.writeFlow(m_flow, tempFile);
      m_undoBuffer.push(tempFile);

      if (m_undoBuffer.size() > m_mainPerspective.getSetting(
        KFDefaults.MAX_UNDO_POINTS_KEY, KFDefaults.MAX_UNDO_POINTS)) {
        m_undoBuffer.remove(0);
      }
      m_mainPerspective.getMainToolBar().enableWidgets(
        MainKFPerspectiveToolBar.Widgets.UNDO_BUTTON.toString());
    } catch (Exception ex) {
      m_logPanel
        .logMessage("[KnowledgeFlow] a problem occurred while trying to "
          + "create an undo point : " + ex.getMessage());
    }
  }

  /**
   * Get the number of entries in the undo stack
   *
   * @return the number of entries in the undo stack
   */
  protected int getUndoBufferSize() {
    return m_undoBuffer.size();
  }

  /**
   * Snap the selected steps (if any) to the grid
   */
  protected void snapSelectedToGrid() {
    if (m_selectedSteps.size() > 0) {
      m_layout.snapSelectedToGrid();
    }
  }

  /**
   * Initiate the process of adding a note to the layout
   */
  protected void initiateAddNote() {
    m_layout.initiateAddNote();
  }

  /**
   * Get the flow being edited by this layout
   *
   * @return the flow being edited by this layout
   */
  public Flow getFlow() {
    return m_flow;
  }

  /**
   * Set the flow to edit in this layout
   *
   * @param flow the flow to edit in this layout
   */
  public void setFlow(Flow flow) {
    m_flow = flow;

    m_renderGraph.clear();
    Iterator<StepManagerImpl> iter = m_flow.iterator();
    m_layout.removeAll();

    while (iter.hasNext()) {
      StepManagerImpl manager = iter.next();
      StepVisual visual = StepVisual.createVisual(manager);
      manager.setStepVisual(visual);
      m_renderGraph.add(visual);
      m_layout.add(visual);
    }

    m_mainPerspective.getMainToolBar().enableWidget(
      MainKFPerspectiveToolBar.Widgets.SELECT_ALL_BUTTON.toString(),
      m_flow.size() > 0);

    m_layout.revalidate();
    m_layout.repaint();
  }

  /**
   * Add all the supplied steps to the flow managed by this layout
   *
   * @param steps the steps to add
   * @return a list of all the added steps, where each has been wrapped in an
   *         appropriate {@code StepVisual} instance.
   */
  protected List<StepVisual> addAll(List<StepManagerImpl> steps) {
    return addAll(steps, true);
  }

  /**
   * Add all the supplied steps to the flow managed by this layout
   *
   * @param steps the steps to add
   * @param revalidate true if the GUI should be repainted
   * @return a list of all the added steps, where each has been wrapped in an
   *         appropriate {@code StepVisual} instance.
   */
  protected List<StepVisual> addAll(List<StepManagerImpl> steps,
    boolean revalidate) {
    List<StepVisual> added = new ArrayList<StepVisual>();
    m_flow.addAll(steps);
    for (StepManagerImpl s : steps) {
      StepVisual visual = StepVisual.createVisual(s);
      s.setStepVisual(visual);
      added.add(visual);
      m_renderGraph.add(visual);
      m_layout.add(visual);
    }
    if (revalidate) {
      m_layout.repaint();
    }

    return added;
  }

  /**
   * Add the supplied step to the flow managed by this layout
   * 
   * @param manager the {@code StepManager} instance managing the step to be
   *          added
   * @param x the x coordinate to add at
   * @param y the y coordinate to add at
   */
  protected void addStep(StepManagerImpl manager, int x, int y) {
    m_flow.addStep(manager);
    StepVisual visual = StepVisual.createVisual(manager);

    Dimension d = visual.getPreferredSize();
    int dx = (int) (d.getWidth() / 2);
    int dy = (int) (d.getHeight() / 2);
    x -= dx;
    y -= dy;

    if (x >= 0 && y >= 0) {
      visual.setX(x);
      visual.setY(y);
    }

    // manager will overwrite x and y if this step has been sourced
    // from JSON and has valid x, y settings
    manager.setStepVisual(visual);
    m_renderGraph.add(visual);
    m_layout.add(visual);
    visual.setLocation(x, y);

    m_mainPerspective.setCursor(Cursor
      .getPredefinedCursor(Cursor.DEFAULT_CURSOR));

    m_mainPerspective.getMainToolBar().enableWidget(
      MainKFPerspectiveToolBar.Widgets.SELECT_ALL_BUTTON.toString(),
      m_flow.size() > 0);
  }

  /**
   * Connect the supplied source step to the supplied target step using the
   * specified connection type
   *
   * @param source the {@code StepManager} instance managing the source step
   * @param target the {@code StepManager} instance managing the target step
   * @param connectionType the connection type to use
   */
  public void connectSteps(StepManagerImpl source, StepManagerImpl target,
    String connectionType) {
    if (m_mainPerspective.getDebug()) {
      System.err.println("[KF] connecting steps: " + source.getName() + " to "
        + target.getName());
    }
    boolean success = m_flow.connectSteps(source, target, connectionType);
    if (m_mainPerspective.getDebug()) {
      if (success) {
        System.err.println("[KF] connection successful");
      } else {
        System.err.println("[KF] connection failed");
      }
    }
    m_layout.repaint();
  }

  /**
   * Rename a step
   * 
   * @param oldName the old name of the step to rename
   * @param newName the new name to give the step
   */
  protected void renameStep(String oldName, String newName) {
    try {
      m_flow.renameStep(oldName, newName);
    } catch (WekaException ex) {
      m_mainPerspective.showErrorDialog(ex);
    }
  }

  /**
   * Remove a step from the flow displayed/edited by this layout
   * 
   * @param step the {@code StepVisual} instance wrapping the step to be removed
   * @throws WekaException if a problem occurs
   */
  protected void removeStep(StepVisual step) throws WekaException {
    m_flow.removeStep(step.getStepManager());
    m_renderGraph.remove(step);
    m_layout.remove(step);

    m_layout.repaint();
  }

  /**
   * Get the number of steps in the layout
   *
   * @return the number of steps in the layout
   */
  protected int numSteps() {
    return m_renderGraph.size();
  }

  /**
   * Get the environment variables being used by this layout
   * 
   * @return the environment variables being used by this layout
   */
  public Environment getEnvironment() {
    return m_env;
  }

  /**
   * Set the environment variables to use with this layout
   * 
   * @param env the environment variables to use
   */
  public void setEnvironment(Environment env) {
    m_env = env;
  }

  public String environmentSubstitute(String source) {
    Environment env = m_env != null ? m_env : Environment.getSystemWide();
    try {
      source = env.substitute(source);
    } catch (Exception ex) {
    }
    return source;
  }

  /**
   * Get the {@code FlowExecutor} being used for execution of this flow
   * 
   * @return the {@code FlowExecutor} in use by this layout
   */
  public FlowExecutor getFlowExecutor() {
    return m_flowExecutor;
  }

  /**
   * Set the {@code FlowExcecutor} to use for executing the flow
   * 
   * @param executor the {@code FlowExecutor} to use for executing the flow in
   *          this layout
   */
  public void setFlowExecutor(FlowExecutor executor) {
    m_flowExecutor = executor;
  }

  /**
   * Get the current path (if any) of the flow being edited in this layout
   * 
   * @return the current path on disk of the flow
   */
  public File getFilePath() {
    return m_filePath;
  }

  /**
   * Set the file path for the flow being edited by this layout
   *
   * @param path the path on disk for the flow being edited
   */
  public void setFilePath(File path) {
    m_filePath = path != null ? path : new File("-NONE-");

    if (path != null) {
      File absolute = new File(path.getAbsolutePath());
      getEnvironment().addVariable(KFGUIConsts.FLOW_DIRECTORY_KEY,
        absolute.getParent());
    }
  }

  /**
   * Get the log panel in use by this layout
   * 
   * @return the log panel
   */
  public KFLogPanel getLogPanel() {
    return m_logPanel;
  }

  /**
   * Get the current zoom setting for this layout
   * 
   * @return the current zoom setting
   */
  public int getZoomSetting() {
    return m_zoomSetting;
  }

  /**
   * Set the current zoom setting for this layout
   *
   * @param zoom the current zoom setting
   */
  public void setZoomSetting(int zoom) {
    m_zoomSetting = zoom;
  }

  /**
   * Get whether this flow has been altered since the last save operation
   *
   * @return true if the flow has been altered
   */
  public boolean getEdited() {
    return m_hasBeenEdited;
  }

  /**
   * Set the edited status of this flow
   *
   * @param edited true if the flow has been altered
   */
  public void setEdited(boolean edited) {
    m_hasBeenEdited = edited;

    // pass on the edited status so that the tab title can be made
    // bold if necessary
    m_mainPerspective.setCurrentTabTitleEditedStatus(edited);
  }

  /**
   * Returns true if the flow managed by this layout is currently executing
   *
   * @return true if the flow is executing
   */
  public boolean isExecuting() {
    return m_isExecuting;
  }

  /**
   * Get the current flow edit operation
   * 
   * @return the current flow edit operation
   */
  protected LayoutOperation getFlowLayoutOperation() {
    return m_userOpp;
  }

  /**
   * Set the current flow edit operation
   * 
   * @param mode the current flow edit operation
   */
  protected void setFlowLayoutOperation(LayoutOperation mode) {
    m_userOpp = mode;
  }

  /**
   * Execute the flow managed by this layout
   * 
   * @param sequential true if the flow's start points are to be launched
   *          sequentially rather than in parallel
   * @throws WekaException if a problem occurs
   */
  public synchronized void executeFlow(boolean sequential) throws WekaException {
    if (isExecuting()) {
      throw new WekaException("The flow is already executing!");
    }
    Settings appSettings =
      m_mainPerspective.getMainApplication().getApplicationSettings();
    if (m_flowExecutor == null) {
      String execName =
        appSettings.getSetting(KFDefaults.APP_ID,
          KnowledgeFlowApp.KnowledgeFlowGeneralDefaults.EXECUTION_ENV_KEY,
          KnowledgeFlowApp.KnowledgeFlowGeneralDefaults.EXECUTION_ENV);
      BaseExecutionEnvironment execE = null;
      try {
        execE =
          (BaseExecutionEnvironment) PluginManager.getPluginInstance(
            BaseExecutionEnvironment.class.getCanonicalName(), execName);
      } catch (Exception ex) {
        // drop through
      }
      if (execE == null) {
        execE = new BaseExecutionEnvironment();
      }
      m_flowExecutor = execE.getDefaultFlowExecutor();

      /*
       * m_flowExecutor = new FlowRunner(m_mainPerspective.getMainApplication()
       * .getApplicationSettings());
       */
      m_flowExecutor.setLogger(m_logPanel);
      m_flowExecutor
        .addExecutionFinishedCallback(new ExecutionFinishedCallback() {
          @Override
          public void executionFinished() {
            m_isExecuting = false;
            m_logPanel.statusMessage("@!@[KnowledgeFlow]|OK.");
            if (m_flowExecutor.wasStopped()) {
              m_logPanel.setMessageOnAll(false, "Stopped.");
            }
            m_mainPerspective.getMainToolBar().enableWidgets(
              MainKFPerspectiveToolBar.Widgets.PLAY_PARALLEL_BUTTON.toString(),
              MainKFPerspectiveToolBar.Widgets.PLAY_SEQUENTIAL_BUTTON
                .toString());
            m_mainPerspective.getMainToolBar().disableWidgets(
              MainKFPerspectiveToolBar.Widgets.STOP_BUTTON.toString());
          }
        });
    }
    m_flowExecutor.setSettings(appSettings);
    m_mainPerspective.getMainToolBar().disableWidgets(
      MainKFPerspectiveToolBar.Widgets.PLAY_PARALLEL_BUTTON.toString(),
      MainKFPerspectiveToolBar.Widgets.PLAY_SEQUENTIAL_BUTTON.toString());
    m_mainPerspective.getMainToolBar().enableWidgets(
      MainKFPerspectiveToolBar.Widgets.STOP_BUTTON.toString());

    m_flowExecutor.getExecutionEnvironment().setEnvironmentVariables(m_env);
    m_flowExecutor.getExecutionEnvironment().setHeadless(false);
    m_flowExecutor.getExecutionEnvironment()
      .setGraphicalEnvironmentCommandHandler(
        new KFGraphicalEnvironmentCommandHandler(m_mainPerspective));
    m_isExecuting = true;

    // Flow toRun = m_flow.copyFlow();
    m_flowExecutor.setFlow(m_flow);
    m_logPanel.clearStatus();
    m_logPanel.statusMessage("@!@[KnowledgeFlow]|Executing...");
    if (sequential) {
      m_flowExecutor.runSequentially();
    } else {
      m_flowExecutor.runParallel();
    }
  }

  /**
   * Stop the flow from executing
   */
  public void stopFlow() {
    if (isExecuting()) {
      m_flowExecutor.stopProcessing();
    }
  }

  /**
   * Find the first step whose bounds enclose the supplied point
   * 
   * @param p the point
   * @return the first step in the flow whose visible bounds enclose the
   *         supplied point, or null if no such step exists
   */
  protected StepVisual findStep(Point p) {

    Rectangle tempBounds = new Rectangle();
    for (StepVisual v : m_renderGraph) {
      tempBounds = v.getBounds();
      if (tempBounds.contains(p)) {
        return v;
      }
    }

    return null;
  }

  /**
   * Find a list of steps that exist within the supplied bounding box
   * 
   * @param boundingBox the bounding box to check
   * @return a list of steps that fall within the bounding box
   */
  protected List<StepVisual> findSteps(Rectangle boundingBox) {
    List<StepVisual> steps = new ArrayList<StepVisual>();

    for (StepVisual v : m_renderGraph) {
      int centerX = v.getX() + (v.getWidth() / 2);
      int centerY = v.getY() + (v.getHeight() / 2);
      if (boundingBox.contains(centerX, centerY)) {
        steps.add(v);
      }
    }

    return steps;
  }

  /**
   * Find a list of steps in the flow that can accept the supplied connection
   * type
   * 
   * @param connectionName the type of connection to check for
   * @return a list of steps that can accept the supplied connection type
   */
  protected List<StepManagerImpl> findStepsThatCanAcceptConnection(
    String connectionName) {

    List<StepManagerImpl> result = new ArrayList<StepManagerImpl>();
    for (StepManagerImpl step : m_flow.getSteps()) {
      List<String> incomingConnNames =
        step.getManagedStep().getIncomingConnectionTypes();
      if (incomingConnNames != null
        && incomingConnNames.contains(connectionName)) {
        result.add(step);
      }
    }

    return result;
  }

  /**
   * Find all connections within delta of the supplied point
   *
   * @param point the point on the canvas to find steps at
   * @param delta the radius around point for detecting connections
   * @return a map keyed by connection type name of connected steps. The value
   *         is a list of 2 element arrays, where each array contains source and
   *         target steps for the connection in question
   */
  protected Map<String, List<StepManagerImpl[]>> findClosestConnections(
    Point point, int delta) {

    Map<String, List<StepManagerImpl[]>> closestConnections =
      new HashMap<String, List<StepManagerImpl[]>>();

    for (StepManagerImpl sourceManager : m_flow.getSteps()) {
      for (Map.Entry<String, List<StepManager>> outCons : sourceManager
        .getOutgoingConnections().entrySet()) {
        List<StepManager> targetsOfConnType = outCons.getValue();
        for (StepManager target : targetsOfConnType) {
          StepManagerImpl targetManager = (StepManagerImpl) target;
          String connName = outCons.getKey();
          StepVisual sourceVisual = sourceManager.getStepVisual();
          StepVisual targetVisual = targetManager.getStepVisual();
          Point bestSourcePt =
            sourceVisual.getClosestConnectorPoint(new Point(targetVisual.getX()
              + targetVisual.getWidth() / 2, targetVisual.getY()
              + targetVisual.getHeight() / 2));
          Point bestTargetPt =
            targetVisual.getClosestConnectorPoint(new Point(sourceVisual.getX()
              + sourceVisual.getWidth() / 2, sourceVisual.getY()
              + sourceVisual.getHeight() / 2));

          int minx = (int) Math.min(bestSourcePt.getX(), bestTargetPt.getX());
          int maxx = (int) Math.max(bestSourcePt.getX(), bestTargetPt.getX());
          int miny = (int) Math.min(bestSourcePt.getY(), bestTargetPt.getY());
          int maxy = (int) Math.max(bestSourcePt.getY(), bestTargetPt.getY());

          // check to see if supplied pt is inside bounding box
          if (point.getX() >= minx - delta && point.getX() <= maxx + delta
            && point.getY() >= miny - delta && point.getY() <= maxy + delta) {
            // now see if the point is within delta of the line
            // formulate ax + by + c = 0
            double a = bestSourcePt.getY() - bestTargetPt.getY();
            double b = bestTargetPt.getX() - bestSourcePt.getX();
            double c =
              (bestSourcePt.getX() * bestTargetPt.getY())
                - (bestTargetPt.getX() * bestSourcePt.getY());

            double distance =
              Math.abs((a * point.getX()) + (b * point.getY()) + c);
            distance /= Math.abs(Math.sqrt((a * a) + (b * b)));

            if (distance <= delta) {
              List<StepManagerImpl[]> conList =
                closestConnections.get(connName);
              if (conList == null) {
                conList = new ArrayList<StepManagerImpl[]>();
                closestConnections.put(connName, conList);
              }
              StepManagerImpl[] conn = { sourceManager, targetManager };

              conList.add(conn);
            }
          }
        }
      }
    }

    return closestConnections;
  }

  /**
   * Returns true if there is a previous (prior to index) connection to the
   * supplied target step in the supplied map of connections.
   *
   * 
   * @param outConns the map of connections to check
   * @param target the target step to check for
   * @param index connections to the target prior to this index in the map count
   * @return true if a previous connection is found
   */
  protected boolean previousConn(Map<String, List<StepManager>> outConns,
    StepManagerImpl target, int index) {
    boolean result = false;

    int count = 0;
    for (Entry<String, List<StepManager>> e : outConns.entrySet()) {
      List<StepManager> connectedSteps = e.getValue();
      for (StepManager c : connectedSteps) {
        StepManagerImpl cI = (StepManagerImpl) c;
        if (target.getManagedStep() == cI.getManagedStep() && count < index) {
          result = true;
          break;
        }
      }

      if (result) {
        break;
      }
      count++;
    }

    return result;
  }

  private void setUpLogPanel(final LogPanel logPanel) {
    String date =
      (new SimpleDateFormat("EEEE, d MMMM yyyy")).format(new Date());
    logPanel.logMessage("Weka Knowledge Flow was written by Mark Hall");
    logPanel.logMessage("Weka Knowledge Flow");
    logPanel.logMessage("(c) 2002-" + Copyright.getToYear() + " "
      + Copyright.getOwner() + ", " + Copyright.getAddress());
    logPanel.logMessage("web: " + Copyright.getURL());
    logPanel.logMessage(date);
    logPanel
      .statusMessage("@!@[KnowledgeFlow]|Welcome to the Weka Knowledge Flow");
    logPanel.getStatusTable().addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (logPanel.getStatusTable().rowAtPoint(e.getPoint()) == 0) {
          if (((e.getModifiers() & InputEvent.BUTTON1_MASK) != InputEvent.BUTTON1_MASK)
            || e.isAltDown()) {
            System.gc();
            Runtime currR = Runtime.getRuntime();
            long freeM = currR.freeMemory();
            long totalM = currR.totalMemory();
            long maxM = currR.maxMemory();
            logPanel
              .logMessage("[KnowledgeFlow] Memory (free/total/max.) in bytes: "
                + String.format("%,d", freeM) + " / "
                + String.format("%,d", totalM) + " / "
                + String.format("%,d", maxM));
            logPanel
              .statusMessage("@!@[KnowledgeFlow]|Memory (free/total/max.) in bytes: "
                + String.format("%,d", freeM)
                + " / "
                + String.format("%,d", totalM)
                + " / "
                + String.format("%,d", maxM));
          }
        }
      }
    });
  }

  /**
   * Save the flow managed by this layout
   *
   * @param showDialog true if a file dialog should be displayed
   */
  protected void saveLayout(boolean showDialog) {
    boolean shownDialog = false;
    int returnVal = JFileChooser.APPROVE_OPTION;
    File sFile = getFilePath();
    if (showDialog || sFile.getName().equals("-NONE-")) {
      returnVal = m_mainPerspective.m_saveFileChooser.showSaveDialog(this);
      shownDialog = true;
    }

    if (returnVal == JFileChooser.APPROVE_OPTION) {
      if (shownDialog) {
        sFile = m_mainPerspective.m_saveFileChooser.getSelectedFile();
      }

      // add extension if necessary
      if (!sFile.getName().toLowerCase().endsWith(".kf")) {
        sFile = new File(sFile.getParent(), sFile.getName() + ".kf");
      }

      try {
        String fName = sFile.getName();
        if (fName.indexOf(".") > 0) {
          fName = fName.substring(0, fName.lastIndexOf('.'));
        }
        m_flow.setFlowName(fName.replace(".kf", ""));
        m_flow.saveFlow(sFile);
        setFilePath(sFile);
        setEdited(false);
        m_mainPerspective.setCurrentTabTitle(fName);
      } catch (WekaException e) {
        m_mainPerspective.showErrorDialog(e);
      }
    }
  }

  /**
   * Pop an undo point and load the flow it encapsulates
   */
  protected void popAndLoadUndo() {
    if (m_undoBuffer.size() > 0) {
      File undo = m_undoBuffer.pop();
      if (m_undoBuffer.size() == 0) {
        m_mainPerspective.getMainToolBar().disableWidgets(
          MainKFPerspectiveToolBar.Widgets.UNDO_BUTTON.toString());
      }
      loadLayout(undo, true);
    }
  }

  /**
   * Load a flow into this layout
   *
   * @param fFile the file containing the flow
   * @param isUndo true if this is an "undo" layout
   */
  protected void loadLayout(File fFile, boolean isUndo) {
    stopFlow();

    m_mainPerspective.getMainToolBar().disableWidgets(
      MainKFPerspectiveToolBar.Widgets.PLAY_PARALLEL_BUTTON.toString(),
      MainKFPerspectiveToolBar.Widgets.PLAY_SEQUENTIAL_BUTTON.toString(),
      MainKFPerspectiveToolBar.Widgets.SAVE_FLOW_BUTTON.toString(),
      MainKFPerspectiveToolBar.Widgets.SAVE_FLOW_AS_BUTTON.toString());

    if (!isUndo) {
      File absolute = new File(fFile.getAbsolutePath());
      getEnvironment().addVariable("Internal.knowledgeflow.directory",
        absolute.getParent());
    }

    try {
      Flow flow = Flow.loadFlow(fFile, m_logPanel);
      setFlow(flow);
      if (!isUndo) {
        setFilePath(fFile);
      }

      if (!getFlow().getFlowName().equals("Untitled")) {
        m_mainPerspective.setCurrentTabTitle(getFlow().getFlowName());
      }

    } catch (WekaException e) {
      m_logPanel
        .statusMessage("@!@[KnowledgeFlow]|Unable to load flow (see log).");
      m_logPanel.logMessage("[KnowledgeFlow] Unable to load flow\n"
        + LogManager.stackTraceToString(e));
      m_mainPerspective.showErrorDialog(e);
    }

    m_mainPerspective.getMainToolBar().enableWidgets(
      MainKFPerspectiveToolBar.Widgets.PLAY_PARALLEL_BUTTON.toString(),
      MainKFPerspectiveToolBar.Widgets.PLAY_SEQUENTIAL_BUTTON.toString(),
      MainKFPerspectiveToolBar.Widgets.SAVE_FLOW_BUTTON.toString(),
      MainKFPerspectiveToolBar.Widgets.SAVE_FLOW_AS_BUTTON.toString());

    m_mainPerspective.getMainToolBar().enableWidget(
      MainKFPerspectiveToolBar.Widgets.SELECT_ALL_BUTTON.toString(),
      m_flow.size() > 0);
  }

  /**
   * Editing operations on the layout
   */
  protected static enum LayoutOperation {
    NONE, MOVING, CONNECTING, ADDING, SELECTING, PASTING;
  }

  /**
   * Small subclass of LogPanel for use by layouts
   */
  protected class KFLogPanel extends LogPanel {

    /** For serialization */
    private static final long serialVersionUID = -2224509243343105276L;

    public synchronized void
      setMessageOnAll(boolean mainKFLine, String message) {
      for (String key : m_tableIndexes.keySet()) {
        if (!mainKFLine && key.equals("[KnowledgeFlow]")) {
          continue;
        }

        String tm = key + "|" + message;
        statusMessage(tm);
      }
    }
  }

  /**
   * Utility method to serialize a list of steps (encapsulated in StepVisuals)
   * to a JSON flow.
   *
   * @param steps the steps to serialize
   * @param name the name to set in the encapsulating Flow before serializing
   * @return the serialized Flow
   * @throws WekaException if a problem occurs
   */
  public static String
    serializeStepsToJSON(List<StepVisual> steps, String name)
      throws WekaException {
    if (steps.size() > 0) {
      List<StepManagerImpl> toCopy = new ArrayList<StepManagerImpl>();
      for (StepVisual s : steps) {
        toCopy.add(s.getStepManager());
      }
      Flow temp = new Flow();
      temp.setFlowName("Clipboard copy");
      temp.addAll(toCopy);

      return JSONFlowUtils.flowToJSON(temp);
    }

    throw new WekaException("No steps to serialize!");
  }
}
