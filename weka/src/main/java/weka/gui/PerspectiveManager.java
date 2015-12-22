package weka.gui;

import weka.core.Copyright;
import weka.core.Defaults;
import weka.core.Environment;
import weka.core.Settings;
import weka.core.logging.Logger;
import weka.gui.beans.PluginManager;
import weka.gui.knowledgeflow.MainKFPerspectiveToolBar;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static weka.gui.knowledgeflow.StepVisual.loadIcon;

/**
 * Manages perspectives and the main menu bar (if visible), holds the currently
 * selected perspective, and implements the perspective button bar.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class PerspectiveManager extends JPanel {

  public static final String PERSPECTIVE_INTERFACE =
    Perspective.class.getCanonicalName();

  public static final Settings.SettingKey VISIBLE_PERSPECTIVES_KEY =
    new Settings.SettingKey("perspective_manager.visible_perspectives",
      "Visible perspectives", "");

  private static final long serialVersionUID = -6099806469970666208L;

  protected JToolBar m_perspectiveToolBar = new JToolBar(JToolBar.HORIZONTAL);

  protected ButtonGroup m_perspectiveGroup = new ButtonGroup();
  protected GUIApplication m_mainApp;

  protected Map<String, Perspective> m_perspectiveCache =
    new LinkedHashMap<String, Perspective>();
  protected Map<String, String> m_perspectiveNameLookup =
    new HashMap<String, String>();
  protected List<Perspective> m_perspectives = new ArrayList<Perspective>();
  protected LinkedHashSet<String> m_visiblePerspectives =
    new LinkedHashSet<String>();

  protected List<String> m_allowedPerspectiveClassPrefixes =
    new ArrayList<String>();

  protected List<String> m_disallowedPerspectiveClassPrefixes =
    new ArrayList<String>();

  protected Perspective m_mainPerspective;

  protected boolean m_configAndPerspectivesVisible;

  protected JPanel m_configAndPerspectivesToolBar;

  protected AttributeSelectionPanel m_perspectiveConfigurer;

  /** Main application menu bar */
  protected JMenuBar m_appMenuBar = new JMenuBar();

  protected JMenu m_programMenu;
  protected JMenuItem m_togglePerspectivesToolBar;

  /** The panel for log and status messages */
  protected LogPanel m_LogPanel = new LogPanel(new WekaTaskMonitor());
  protected boolean m_logVisible;

  public PerspectiveManager(GUIApplication mainApp,
    String... perspectivePrefixesToAllow) {
    this(mainApp, perspectivePrefixesToAllow, new String[0]);
  }

  public PerspectiveManager(GUIApplication mainApp,
    String[] perspectivePrefixesToAllow,
    String[] perspectivePrefixesToDisallow) {

    if (perspectivePrefixesToAllow != null) {
      for (String prefix : perspectivePrefixesToAllow) {
        m_allowedPerspectiveClassPrefixes.add(prefix);
      }
    }

    if (perspectivePrefixesToDisallow != null) {
      for (String prefix : perspectivePrefixesToDisallow) {
        m_disallowedPerspectiveClassPrefixes.add(prefix);
      }
    }
    m_mainApp = mainApp;
    final Settings settings = m_mainApp.getApplicationSettings();

    m_mainPerspective = m_mainApp.getMainPerspective();
    // apply defaults for the main perspective
    settings.applyDefaults(m_mainPerspective.getDefaultSettings());

    setLayout(new BorderLayout());
    m_configAndPerspectivesToolBar = new JPanel();
    m_configAndPerspectivesToolBar.setLayout(new BorderLayout());
    m_perspectiveToolBar.setLayout(new WrapLayout(FlowLayout.LEFT, 0, 0));
    /*
     * JScrollPane toolbarScroller = new JScrollPane(m_perspectiveToolBar);
     * toolbarScroller
     * .setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)
     * ; toolbarScroller
     * .setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER); int
     * scrollbarSize =
     * toolbarScroller.getHorizontalScrollBar().getVisibleAmount();
     * m_perspectiveToolBar.setBorder(
     * BorderFactory.createEmptyBorder(scrollbarSize, 0, scrollbarSize, 0));
     */

    m_configAndPerspectivesToolBar.add(m_perspectiveToolBar,
      BorderLayout.CENTER);

    m_mainPerspective.setMainApplication(m_mainApp);
    m_perspectives.add(m_mainPerspective);
    initPerspectivesCache(settings);
    initVisiblePerspectives(settings);

    add((JComponent) m_mainPerspective, BorderLayout.CENTER);

    // The program menu
    m_programMenu = initProgramMenu();

    // add the main perspective's toggle button to the toolbar
    String titleM = m_mainPerspective.getPerspectiveTitle();
    Icon icon = m_mainPerspective.getPerspectiveIcon();
    JToggleButton tBut = new JToggleButton(titleM, icon, true);
    tBut.setToolTipText(m_mainPerspective.getPerspectiveTipText());
    tBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Component[] comps = getComponents();
        Perspective current = null;
        int pIndex = 0;
        for (int i = 0; i < comps.length; i++) {
          if (comps[i] instanceof Perspective) {
            pIndex = i;
            current = (Perspective) comps[i];
            break;
          }
        }
        if (current == m_mainPerspective) {
          return;
        }

        current.setActive(false);
        remove(pIndex);
        add((JComponent) m_mainPerspective, BorderLayout.CENTER);
        m_perspectives.get(0).setActive(true);

        m_appMenuBar.removeAll();
        m_appMenuBar.add(m_programMenu);
        List<JMenu> mainMenus = m_perspectives.get(0).getMenus();
        for (JMenu m : mainMenus) {
          m_appMenuBar.add(m);
        }

        m_mainApp.revalidate();
      }
    });
    m_perspectiveToolBar.add(tBut);
    m_perspectiveGroup.add(tBut);

    setupUserPerspectives();

    initLogPanel();
    if (m_mainPerspective.requiresLog()) {
      m_mainPerspective.setLog(m_LogPanel);
      add(m_LogPanel, BorderLayout.SOUTH);
      m_logVisible = true;
    }
    // tell the main perspective that it is active (i.e. main app is now
    // available and it can access settings).
    m_mainPerspective.setActive(true);

    // make sure any initial general settings are applied
    m_mainApp.settingsChanged();
  }

  protected void initLogPanel() {
    String date =
      (new SimpleDateFormat("EEEE, d MMMM yyyy")).format(new Date());
    m_LogPanel.logMessage("Weka " + m_mainApp.getApplicationName());
    m_LogPanel
      .logMessage("(c) " + Copyright.getFromYear() + "-" + Copyright.getToYear()
        + " " + Copyright.getOwner() + ", " + Copyright.getAddress());
    m_LogPanel.logMessage("web: " + Copyright.getURL());
    m_LogPanel.logMessage("Started on " + date);
    m_LogPanel
      .statusMessage("Welcome to the Weka " + m_mainApp.getApplicationName());
  }

  public void setMainApplicationForAllPerspectives() {
    // set main application on all cached perspectives and then tell
    // each that instantiation is complete
    for (Map.Entry<String, Perspective> e : m_perspectiveCache.entrySet()) {
      e.getValue().setMainApplication(m_mainApp);
    }
    for (Map.Entry<String, Perspective> e : m_perspectiveCache.entrySet()) {
      // instantiation is complete at this point - the perspective now
      // has access to the main application and the PerspectiveManager
      e.getValue().instantiationComplete();

      if (e.getValue().requiresLog()) {
        e.getValue().setLog(m_LogPanel);
      }
    }

    m_mainPerspective.setMainApplication(m_mainApp);
    m_mainPerspective.instantiationComplete();
  }

  protected void notifySettingsChanged() {
    m_mainApp.settingsChanged();
    m_mainPerspective.settingsChanged();
    for (Map.Entry<String, Perspective> e : m_perspectiveCache.entrySet()) {
      e.getValue().settingsChanged();
    }
  }

  protected JMenu initProgramMenu() {
    JMenu programMenu = new JMenu();
    m_togglePerspectivesToolBar = new JMenuItem("Toggle perspectives toolbar");
    KeyStroke hideKey =
      KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.ALT_DOWN_MASK);
    m_togglePerspectivesToolBar.setAccelerator(hideKey);
    m_togglePerspectivesToolBar.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_mainApp.isPerspectivesToolBarVisible()) {
          m_mainApp.hidePerspectivesToolBar();
        } else {
          m_mainApp.showPerspectivesToolBar();
        }
        m_mainApp.revalidate();
      }
    });
    programMenu.add(m_togglePerspectivesToolBar);

    programMenu.setText("Program");
    JMenuItem exitItem = new JMenuItem("Exit");
    KeyStroke exitKey =
      KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK);
    exitItem.setAccelerator(exitKey);
    exitItem.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        ((Frame) ((JComponent) m_mainApp).getTopLevelAncestor()).dispose();
        System.exit(0);
      }
    });
    programMenu.add(exitItem);

    m_appMenuBar.add(programMenu);
    List<JMenu> mainMenus = m_mainPerspective.getMenus();
    for (JMenu m : mainMenus) {
      m_appMenuBar.add(m);
    }

    return programMenu;
  }

  public void setPerspectiveToolbarAlwaysHidden(Settings settings) {
    SelectedPerspectivePreferences userVisiblePerspectives = settings
      .getSetting(m_mainApp.getApplicationID(), VISIBLE_PERSPECTIVES_KEY,
        new SelectedPerspectivePreferences(), Environment.getSystemWide());
    userVisiblePerspectives.setPerspectivesToolbarAlwaysHidden(true);
    setPerspectiveToolBarIsVisible(false);

    m_programMenu.remove(m_togglePerspectivesToolBar);
  }

  public void addSettingsMenuItemToProgramMenu(final Settings settings) {

    if (settings != null) {
      JMenuItem settingsM = new JMenuItem("Settings...");
      settingsM.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          popupSettingsDialog(settings);
        }
      });
      m_programMenu.insert(settingsM, 0);
    }

    JButton configB = new JButton(new ImageIcon(
      loadIcon(MainKFPerspectiveToolBar.ICON_PATH + "cog.png").getImage()));
    configB.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 1));
    configB.setToolTipText("Settings");
    m_configAndPerspectivesToolBar.add(configB, BorderLayout.WEST);
    configB.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        popupSettingsDialog(settings);
      }
    });

  }

  protected void popupSettingsDialog(final Settings settings) {
    final SettingsEditor settingsEditor =
      new SettingsEditor(settings, m_mainApp);

    try {
      int result =
        SettingsEditor.showApplicationSettingsEditor(settings, m_mainApp);
      if (result == JOptionPane.OK_OPTION) {
        initVisiblePerspectives(settings);
        setupUserPerspectives();
        notifySettingsChanged();
      }
    } catch (IOException ex) {
      m_mainApp.showErrorDialog(ex);
    }
  }

  protected void setupUserPerspectives() {
    // first clear the toolbar
    // boolean atLeastOneEnabled = false;
    for (int i = m_perspectiveToolBar.getComponentCount() - 1; i > 0; i--) {
      /*
       * atLeastOneEnabled = atLeastOneEnabled ||
       * m_perspectiveToolBar.getComponent(i).isEnabled();
       */
      m_perspectiveToolBar.remove(i);
      m_perspectives.remove(i);
    }

    int index = 1;
    for (String c : m_visiblePerspectives) {
      String impl = m_perspectiveNameLookup.get(c);
      Perspective toAdd = m_perspectiveCache.get(impl);
      if (toAdd instanceof JComponent) {
        toAdd.setLoaded(true);
        m_perspectives.add(toAdd);
        String titleM = toAdd.getPerspectiveTitle();
        Icon icon = toAdd.getPerspectiveIcon();
        JToggleButton tBut = null;
        if (icon != null) {
          tBut = new JToggleButton(titleM, icon, false);
        } else {
          tBut = new JToggleButton(titleM, false);
        }
        tBut.setToolTipText(toAdd.getPerspectiveTipText());
        final int theIndex = index;
        tBut.setEnabled(toAdd.okToBeActive());
        tBut.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            setActivePerspective(theIndex);
          }
        });
        m_perspectiveToolBar.add(tBut);
        m_perspectiveGroup.add(tBut);
        index++;
      }
    }

    Component[] comps = getComponents();
    Perspective current = null;
    int pIndex = 0;
    for (int i = 0; i < comps.length; i++) {
      if (comps[i] instanceof Perspective) {
        pIndex = i;
        current = (Perspective) comps[i];
        break;
      }
    }
    if (current != m_mainPerspective) {
      setActivePerspective(0);
    }

    m_mainApp.revalidate();
  }

  public void setActivePerspective(int theIndex) {
    if (theIndex < 0 || theIndex > m_perspectives.size() - 1) {
      return;
    }

    Component[] comps = getComponents();
    Perspective current = null;
    int pIndex = 0;
    for (int i = 0; i < comps.length; i++) {
      if (comps[i] instanceof Perspective) {
        pIndex = i;
        current = (Perspective) comps[i];
        break;
      }
    }
    current.setActive(false);
    remove(pIndex);
    add((JComponent) m_perspectives.get(theIndex), BorderLayout.CENTER);
    m_perspectives.get(theIndex).setActive(true);
    ((JToggleButton) m_perspectiveToolBar.getComponent(theIndex))
      .setSelected(true);

    if (((Perspective) m_perspectives.get(theIndex)).requiresLog()
      && !m_logVisible) {
      add(m_LogPanel, BorderLayout.SOUTH);
      m_logVisible = true;
    } else if (!((Perspective) m_perspectives.get(theIndex)).requiresLog()
      && m_logVisible) {
      remove(m_LogPanel);
      m_logVisible = false;
    }

    JMenu programMenu = m_appMenuBar.getMenu(0);
    m_appMenuBar.removeAll();
    m_appMenuBar.add(programMenu);
    List<JMenu> mainMenus = m_perspectives.get(theIndex).getMenus();
    for (JMenu m : mainMenus) {
      m_appMenuBar.add(m);
    }

    m_mainApp.revalidate();
  }

  public void setActivePerspective(String perspectiveID) {
    int index = -1;
    for (int i = 0; i < m_perspectives.size(); i++) {
      if (m_perspectives.get(i).getPerspectiveID().equals(perspectiveID)) {
        index = i;
        break;
      }
    }

    if (index >= 0) {
      setActivePerspective(index);
    }
  }

  /**
   * Get a list of all loaded perspectives. I.e. all perspectives that this
   * manager knows about. Note that this list does not include the main
   * application perspective - use getMainPerspective() to retrieve this.
   *
   * @return a list of all loaded (but not necessary visible) perspectives
   */
  public List<Perspective> getLoadedPerspectives() {
    List<Perspective> available = new ArrayList<Perspective>();
    for (Map.Entry<String, Perspective> e : m_perspectiveCache.entrySet()) {
      available.add(e.getValue());
    }

    return available;
  }

  /**
   * Get a list of visible perspectives. I.e. those that are available to be
   * selected in the perspective toolbar
   *
   * @return a list of visible perspectives
   */
  public List<Perspective> getVisiblePerspectives() {
    List<Perspective> visible = new ArrayList<Perspective>();

    for (String pName : m_visiblePerspectives) {
      String impl = m_perspectiveNameLookup.get(pName);
      Perspective p = m_perspectiveCache.get(impl);
      if (p != null) {
        visible.add(p);
      }
    }

    return visible;
  }

  /**
   * Loads perspectives and initializes the cache.
   *
   * @param settings the settings object in which to store default settings from
   *          each loaded perspective
   */
  protected void initPerspectivesCache(Settings settings) {
    Set<String> pluginPerspectiveImpls =
      PluginManager.getPluginNamesOfType(PERSPECTIVE_INTERFACE);

    if (pluginPerspectiveImpls != null) {
      for (String impl : pluginPerspectiveImpls) {
        try {
          Object perspective =
            PluginManager.getPluginInstance(PERSPECTIVE_INTERFACE, impl);

          if (!(perspective instanceof Perspective)) {
            weka.core.logging.Logger.log(Logger.Level.WARNING,
              "[PerspectiveManager] " + impl + " is not an instance"
                + PERSPECTIVE_INTERFACE + ". Skipping...");
          }

          boolean ok = true;
          if (m_allowedPerspectiveClassPrefixes.size() > 0) {
            ok = false;
            for (String prefix : m_allowedPerspectiveClassPrefixes) {
              if (impl.startsWith(prefix)) {
                ok = true;
                break;
              }
            }
          }

          if (m_disallowedPerspectiveClassPrefixes.size() > 0) {
            for (String prefix : m_disallowedPerspectiveClassPrefixes) {
              if (impl.startsWith(prefix)) {
                ok = false;
                break;
              }
            }
          }

          if (impl.equals(m_mainPerspective.getClass().getCanonicalName())) {
            // main perspective is always part of the application and we dont
            // want it to appear as an option in the list of perspectives to
            // choose from in the preferences dialog
            ok = false;
          }

          if (ok) {
            m_perspectiveCache.put(impl, (Perspective) perspective);
            String perspectiveTitle =
              ((Perspective) perspective).getPerspectiveTitle();
            m_perspectiveNameLookup.put(perspectiveTitle, impl);
            settings
              .applyDefaults(((Perspective) perspective).getDefaultSettings());
          }
        } catch (Exception e) {
          e.printStackTrace();
          m_mainApp.showErrorDialog(e);
        }
      }
    }
  }

  protected void initVisiblePerspectives(Settings settings) {
    m_visiblePerspectives.clear();
    if (m_perspectiveCache.size() > 0) {
      Map<Settings.SettingKey, Object> defaults =
        new LinkedHashMap<Settings.SettingKey, Object>();

      SelectedPerspectivePreferences defaultEmpty =
        new SelectedPerspectivePreferences();
      defaults.put(VISIBLE_PERSPECTIVES_KEY, defaultEmpty);

      Defaults mainAppPerspectiveDefaults =
        new Defaults(m_mainApp.getApplicationID(), defaults);

      settings.applyDefaults(mainAppPerspectiveDefaults);

      SelectedPerspectivePreferences userVisiblePerspectives = settings
        .getSetting(m_mainApp.getApplicationID(), VISIBLE_PERSPECTIVES_KEY,
          new SelectedPerspectivePreferences(), Environment.getSystemWide());

      if (userVisiblePerspectives == defaultEmpty) {
        // no stored settings for this yet. We should start with
        // all perspectives visible
        for (Map.Entry<String, Perspective> e : m_perspectiveCache.entrySet()) {
          userVisiblePerspectives.getUserVisiblePerspectives()
            .add(e.getValue().getPerspectiveTitle());
        }
        userVisiblePerspectives.setPerspectivesToolbarVisibleOnStartup(true);
      }

      for (String userVisPer : userVisiblePerspectives
        .getUserVisiblePerspectives()) {
        m_visiblePerspectives.add(userVisPer);
      }

    }
  }

  public JPanel getPerspectiveToolBar() {
    return m_configAndPerspectivesToolBar;
  }

  public void disableAllPerspectiveTabs() {
    for (int i = 0; i < m_perspectiveToolBar.getComponentCount(); i++) {
      m_perspectiveToolBar.getComponent(i).setEnabled(false);
    }
  }

  public void enableAllPerspectiveTabs() {
    for (int i = 0; i < m_perspectiveToolBar.getComponentCount(); i++) {
      m_perspectiveToolBar.getComponent(i).setEnabled(true);
    }
  }

  public void setEnablePerspectiveTabs(List<String> perspectiveIDs,
    boolean enabled) {
    for (int i = 0; i < m_perspectives.size(); i++) {
      Perspective p = m_perspectives.get(i);
      if (perspectiveIDs.contains(p.getPerspectiveID())) {
        m_perspectiveToolBar.getComponent(i).setEnabled(enabled);
      }
    }
  }

  public void setEnablePerspectiveTab(String perspectiveID, boolean enabled) {
    for (int i = 0; i < m_perspectives.size(); i++) {
      Perspective p = m_perspectives.get(i);
      if (p.getPerspectiveID().equals(perspectiveID) && p.okToBeActive()) {
        m_perspectiveToolBar.getComponent(i).setEnabled(enabled);
      }
    }
  }

  public boolean perspectiveToolBarIsVisible() {
    return m_configAndPerspectivesVisible;
  }

  public void setPerspectiveToolBarIsVisible(boolean v) {
    m_configAndPerspectivesVisible = v;
  }

  /**
   * Get the main application perspective. This is the perspective that is
   * visible on startup of the application and is usually the entry point for
   * the application.
   *
   * @return the main perspective
   */
  public Perspective getMainPerspective() {
    return m_mainPerspective;
  }

  public Perspective getPerspective(String ID) {
    Perspective perspective = null;
    for (Perspective p : m_perspectives) {
      if (p.getPerspectiveID().equals(ID)) {
        perspective = p;
        break;
      }
    }
    return perspective;
  }

  public void showMenuBar(JFrame topLevelAncestor) {
    topLevelAncestor.setJMenuBar(m_appMenuBar);
  }

  public boolean
    userRequestedPerspectiveToolbarVisibleOnStartup(Settings settings) {
    SelectedPerspectivePreferences perspectivePreferences = settings.getSetting(
      m_mainApp.getApplicationID(), VISIBLE_PERSPECTIVES_KEY,
      new SelectedPerspectivePreferences(), Environment.getSystemWide());
    return perspectivePreferences.getPerspectivesToolbarVisibleOnStartup();
  }

  public static class SelectedPerspectivePreferences
    implements java.io.Serializable {
    private static final long serialVersionUID = -2665480123235382483L;

    protected LinkedList<String> m_userVisiblePerspectives =
      new LinkedList<String>();

    /** Whether the toolbar should be visible (or hidden) at startup */
    protected boolean m_perspectivesToolbarVisibleOnStartup;

    /**
     * Whether the toolbar should always be hidden (and not able to be toggled
     * from the menu or widget.
     */
    protected boolean m_perspectivesToolbarAlwaysHidden;

    public void
      setUserVisiblePerspectives(LinkedList<String> userVisiblePerspectives) {
      m_userVisiblePerspectives = userVisiblePerspectives;
    }

    public LinkedList<String> getUserVisiblePerspectives() {
      return m_userVisiblePerspectives;
    }

    public void setPerspectivesToolbarVisibleOnStartup(boolean v) {
      m_perspectivesToolbarVisibleOnStartup = v;
    }

    public boolean getPerspectivesToolbarVisibleOnStartup() {
      return m_perspectivesToolbarVisibleOnStartup;
    }

    public void setPerspectivesToolbarAlwaysHidden(boolean h) {
      m_perspectivesToolbarAlwaysHidden = h;
    }

    public boolean getPerspectivesToolbarAlwaysHidden() {
      return m_perspectivesToolbarAlwaysHidden;
    }
  }
}
