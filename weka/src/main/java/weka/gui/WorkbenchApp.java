package weka.gui;

import weka.core.Capabilities;
import weka.core.Environment;
import weka.core.Memory;
import weka.core.Settings;
import weka.core.converters.AbstractFileLoader;
import weka.core.converters.ConverterUtils;
import weka.gui.explorer.Explorer;
import weka.gui.explorer.PreprocessPanel;

import javax.swing.JFrame;
import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class WorkbenchApp extends AbstractGUIApplication {

  private static final long serialVersionUID = -2357486011273897728L;

  /** for monitoring the Memory consumption */
  protected static Memory m_Memory = new Memory(true);

  /**
   * variable for the Workbench class which would be set to null by the memory
   * monitoring thread to free up some memory if we running out of memory
   */
  protected static WorkbenchApp m_workbench;

  protected Settings m_workbenchSettings;

  public WorkbenchApp() {
    super(true, new String[0],
      new String[] {
        weka.gui.knowledgeflow.AttributeSummaryPerspective.class
          .getCanonicalName(),
        weka.gui.knowledgeflow.ScatterPlotMatrixPerspective.class
          .getCanonicalName(),
        weka.gui.knowledgeflow.SQLViewerPerspective.class.getCanonicalName() });
    m_perspectiveManager
      .addSettingsMenuItemToProgramMenu(getApplicationSettings());
    showPerspectivesToolBar();

    List<Perspective> perspectives =
      m_perspectiveManager.getLoadedPerspectives();
    for (Perspective p : perspectives) {
      m_perspectiveManager.setEnablePerspectiveTab(p.getPerspectiveID(),
        p.okToBeActive());
    }
  }

  @Override
  public String getApplicationName() {
    return WorkbenchDefaults.APP_NAME;
  }

  @Override
  public String getApplicationID() {
    return WorkbenchDefaults.APP_ID;
  }

  @Override
  public Perspective getMainPerspective() {
    return new PreprocessPanel();
  }

  @Override
  public Settings getApplicationSettings() {

    if (m_workbenchSettings == null) {
      m_workbenchSettings = new Settings("weka", WorkbenchDefaults.APP_ID);
      m_workbenchSettings.applyDefaults(new WorkbenchDefaults());
    }

    return m_workbenchSettings;
  }

  @Override
  public void settingsChanged() {
    GenericObjectEditor.setShowGlobalInfoToolTips(
      getApplicationSettings().getSetting(WorkbenchDefaults.APP_ID,
        WorkbenchDefaults.SHOW_JTREE_TIP_TEXT_KEY,
        WorkbenchDefaults.SHOW_JTREE_GLOBAL_INFO_TIPS,
        Environment.getSystemWide()));
  }

  public void notifyCapabilitiesFilterListeners(Capabilities filter) {
    for (Perspective p : getPerspectiveManager().getVisiblePerspectives()) {
      if (p instanceof Explorer.CapabilitiesFilterChangeListener) {
        ((Explorer.CapabilitiesFilterChangeListener) p)
          .capabilitiesFilterChanged(
            new Explorer.CapabilitiesFilterChangeEvent(this, filter));
      }
    }
  }

  public static void main(String[] args) {
    try {
      LookAndFeel.setLookAndFeel(WorkbenchDefaults.APP_ID,
        WorkbenchDefaults.APP_ID + ".lookAndFeel");
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    weka.gui.GenericObjectEditor.determineClasses();

    try {
      if (System.getProperty("os.name").contains("Mac")) {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
      }
      m_workbench = new WorkbenchApp();
      final javax.swing.JFrame jf =
        new javax.swing.JFrame("Weka " + m_workbench.getApplicationName());
      jf.getContentPane().setLayout(new java.awt.BorderLayout());

      Image icon = Toolkit.getDefaultToolkit().getImage(WorkbenchApp.class
        .getClassLoader().getResource("weka/gui/weka_icon_new_48.png"));
      jf.setIconImage(icon);

      jf.getContentPane().add(m_workbench, BorderLayout.CENTER);
      jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      jf.pack();
      m_workbench.showMenuBar(jf);
      jf.setSize(1024, 768);
      jf.setVisible(true);

      if (args.length == 1) {
        System.err.println("Loading instances from " + args[0]);
        AbstractFileLoader loader = ConverterUtils.getLoaderForFile(args[0]);
        loader.setFile(new File(args[0]));
        m_workbench.getPerspectiveManager().getMainPerspective()
          .setInstances(loader.getDataSet());
      }

      Thread memMonitor = new Thread() {
        @Override
        public void run() {
          while (true) {
            // try {
            // System.out.println("Before sleeping.");
            // Thread.sleep(10);

            if (m_Memory.isOutOfMemory()) {
              // clean up
              jf.dispose();
              m_workbench = null;
              System.gc();

              // display error
              System.err.println("\ndisplayed message:");
              m_Memory.showOutOfMemory();
              System.err.println("\nexiting");
              System.exit(-1);
            }
          }
        }
      };

      memMonitor.setPriority(Thread.MAX_PRIORITY);
      memMonitor.start();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
