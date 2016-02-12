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
 *    PerspectiveManager.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui;

import weka.core.Copyright;
import weka.core.Defaults;
import weka.core.Environment;
import weka.core.Settings;
import weka.core.logging.Logger;
import weka.core.PluginManager;
import weka.gui.knowledgeflow.MainKFPerspectiveToolBar;

import javax.swing.*;
import java.awt.*;
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

  /** Interface name of perspectives */
  public static final String PERSPECTIVE_INTERFACE = Perspective.class
    .getCanonicalName();

  /** Settings key for visible perspectives in an application */
  public static final Settings.SettingKey VISIBLE_PERSPECTIVES_KEY =
    new Settings.SettingKey("perspective_manager.visible_perspectives",
      "Visible perspectives", "");

  private static final long serialVersionUID = -6099806469970666208L;

  /** The actual perspectives toolbar */
  protected JToolBar m_perspectiveToolBar = new JToolBar(JToolBar.HORIZONTAL);

  /** For grouping the perspectives buttons in the toolbar */
  protected ButtonGroup m_perspectiveGroup = new ButtonGroup();

  /** The main application that owns this perspective manager */
  protected GUIApplication m_mainApp;

  /** Cache of perspectives */
  protected Map<String, Perspective> m_perspectiveCache =
    new LinkedHashMap<String, Perspective>();

  /** Name lookup for perspectives */
  protected Map<String, String> m_perspectiveNameLookup =
    new HashMap<String, String>();

  /** The perspectives that have a button in the toolbar */
  protected List<Perspective> m_perspectives = new ArrayList<Perspective>();

  /**
   * Names of visible perspectives (i.e. those that the user has opted to be
   * available via a button in the toolbar
   */
  protected LinkedHashSet<String> m_visiblePerspectives =
    new LinkedHashSet<String>();

  /** Allow these perspectives in the toolbar (empty list means allow all) */
  protected List<String> m_allowedPerspectiveClassPrefixes =
    new ArrayList<String>();

  /**
   * Disallow these perspectives (non-empty list overrides meaning of empty
   * allowed list)
   */
  protected List<String> m_disallowedPerspectiveClassPrefixes =
    new ArrayList<String>();

  /**
   * The main perspective for the application owning this perspective manager
   */
  protected Perspective m_mainPerspective;

  /** Whether the toolbar is visible or hidden */
  protected boolean m_configAndPerspectivesVisible;

  /** Holds the config/settings button and toolbar */
  protected JPanel m_configAndPerspectivesToolBar;

  /** Main application menu bar */
  protected JMenuBar m_appMenuBar = new JMenuBar();

  /** Program menu */
  protected JMenu m_programMenu;

  /** Menu item for toggling whether the toolbar is visible or not */
  protected JMenuItem m_togglePerspectivesToolBar;

  /** The panel for log and status messages */
  protected LogPanel m_LogPanel = new LogPanel(new WekaTaskMonitor());

  /** Whether the log is visible in the current perspective */
  protected boolean m_logVisible;

  /**
   * Constructor
   * 
   * @param mainApp the application that owns this perspective manager
   * @param perspectivePrefixesToAllow a list of perspective class name prefixes
   *          that are to be allowed in this perspective manager. Any
   *          perspectives not covered by this list are ignored. An empty list
   *          means allow all.
   */
  public PerspectiveManager(GUIApplication mainApp,
    String... perspectivePrefixesToAllow) {
    this(mainApp, perspectivePrefixesToAllow, new String[0]);
  }

  /**
   * Constructor
   * 
   * @param mainApp the application that owns this perspective manager
   * @param perspectivePrefixesToAllow a list of perspective class name prefixes
   *          that are to be allowed in this perspective manager. Any
   *          perspectives not covered by this list are ignored. An empty list
   *          means allow all.
   * @param perspectivePrefixesToDisallow a list of perspective class name
   *          prefixes that are disallowed in this perspective manager. Any
   *          matches in this list are prevented from appearing in this
   *          perspective manager. Overrides a successful match in the allowed
   *          list. This enables fine-grained exclusion of perspectives (e.g.
   *          allowed might specify all perspectives in weka.gui.funky, while
   *          disallowed vetoes just weka.gui.funky.NonFunkyPerspective.)
   */
  public PerspectiveManager(GUIApplication mainApp,
    String[] perspectivePrefixesToAllow, String[] perspectivePrefixesToDisallow) {

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

    initLogPanel(settings);
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

  /**
   * Apply settings to the log panel
   *
   * @param settings settings to apply
   */
  protected void setLogSettings(Settings settings) {
    int fontSize =
      settings.getSetting(m_mainApp.getApplicationID(),
        new Settings.SettingKey(m_mainApp.getApplicationID()
          + ".logMessageFontSize", "", ""), -1);
    m_LogPanel.setLoggingFontSize(fontSize);
  }

  /**
   * Initialize the log panel
   */
  protected void initLogPanel(Settings settings) {
    setLogSettings(settings);
    String date =
      (new SimpleDateFormat("EEEE, d MMMM yyyy")).format(new Date());
    m_LogPanel.logMessage("Weka " + m_mainApp.getApplicationName());
    m_LogPanel.logMessage("(c) " + Copyright.getFromYear() + "-"
      + Copyright.getToYear() + " " + Copyright.getOwner() + ", "
      + Copyright.getAddress());
    m_LogPanel.logMessage("web: " + Copyright.getURL());
    m_LogPanel.logMessage("Started on " + date);
    m_LogPanel.statusMessage("Welcome to the Weka "
      + m_mainApp.getApplicationName());
  }

  /**
   * Set the main application on all perspectives managed by this manager
   */
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

  /**
   * Notify all perspectives of a change to the application settings
   */
  protected void notifySettingsChanged() {
    m_mainApp.settingsChanged();
    m_mainPerspective.settingsChanged();
    for (Map.Entry<String, Perspective> e : m_perspectiveCache.entrySet()) {
      e.getValue().settingsChanged();
    }
    setLogSettings(m_mainApp.getApplicationSettings());
  }

  /**
   * Initialize the program menu
   *
   * @return the JMenu item for the program menu
   */
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

  /**
   * Set whether the perspectives toolbar should always be hidden. This allows
   * just menu-based access to the perspectives and their settings
   *
   * @param settings the settings object to set this property on
   */
  public void setPerspectiveToolbarAlwaysHidden(Settings settings) {
    SelectedPerspectivePreferences userVisiblePerspectives =
      settings.getSetting(m_mainApp.getApplicationID(),
        VISIBLE_PERSPECTIVES_KEY, new SelectedPerspectivePreferences(),
        Environment.getSystemWide());
    userVisiblePerspectives.setPerspectivesToolbarAlwaysHidden(true);
    setPerspectiveToolBarIsVisible(false);

    m_programMenu.remove(m_togglePerspectivesToolBar);
  }

  /**
   * Applications can call this to allow access to the settings editor from the
   * program menu (in addition to the toolbar widget that pops up the settings
   * editor)
   *
   * @param settings the settings object for the application
   */
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

    JButton configB =
      new JButton(new ImageIcon(loadIcon(
        MainKFPerspectiveToolBar.ICON_PATH + "cog.png").getImage()));
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

  /**
   * Popup the settings editor dialog
   *
   * @param settings the settings to edit
   */
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

  /**
   * Creates a button on the toolbar for each visible perspective
   */
  protected void setupUserPerspectives() {
    // first clear the toolbar
    for (int i = m_perspectiveToolBar.getComponentCount() - 1; i > 0; i--) {
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

  /**
   * Set the active perspective
   *
   * @param theIndex the index of the perspective to make the active one
   */
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
    if (mainMenus != null) {
      for (JMenu m : mainMenus) {
        m_appMenuBar.add(m);
      }
    }

    m_mainApp.revalidate();
  }

  /**
   * Set the active perspective
   *
   * @param perspectiveID the ID of the perspective to make the active one
   */
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
        if (!impl.equals(m_mainPerspective.getClass().getCanonicalName())) {
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
              settings.applyDefaults(((Perspective) perspective)
                .getDefaultSettings());
            }
          } catch (Exception e) {
            e.printStackTrace();
            m_mainApp.showErrorDialog(e);
          }
        }
      }
    }
  }

  /**
   * Initializes the visible perspectives. Makes sure that default settings for
   * each perspective get added to the application-wide settings object.
   *
   * @param settings the settings object for the owner application
   */
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

      SelectedPerspectivePreferences userVisiblePerspectives =
        settings.getSetting(m_mainApp.getApplicationID(),
          VISIBLE_PERSPECTIVES_KEY, new SelectedPerspectivePreferences(),
          Environment.getSystemWide());

      if (userVisiblePerspectives == defaultEmpty) {
        // no stored settings for this yet. We should start with
        // all perspectives visible
        for (Map.Entry<String, Perspective> e : m_perspectiveCache.entrySet()) {
          userVisiblePerspectives.getUserVisiblePerspectives().add(
            e.getValue().getPerspectiveTitle());
        }
        userVisiblePerspectives.setPerspectivesToolbarVisibleOnStartup(true);
      }

      for (String userVisPer : userVisiblePerspectives
        .getUserVisiblePerspectives()) {
        m_visiblePerspectives.add(userVisPer);
      }

    }
  }

  /**
   * Get the panel that contains the perspectives toolbar
   * 
   * @return the panel that contains the perspecitves toolbar
   */
  public JPanel getPerspectiveToolBar() {
    return m_configAndPerspectivesToolBar;
  }

  /**
   * Disable the tab/button for each visible perspective
   */
  public void disableAllPerspectiveTabs() {
    for (int i = 0; i < m_perspectiveToolBar.getComponentCount(); i++) {
      m_perspectiveToolBar.getComponent(i).setEnabled(false);
    }
  }

  /**
   * Enable the tab/button for each visible perspective
   */
  public void enableAllPerspectiveTabs() {
    for (int i = 0; i < m_perspectiveToolBar.getComponentCount(); i++) {
      m_perspectiveToolBar.getComponent(i).setEnabled(true);
    }
  }

  /**
   * Enable/disable the tab/button for each perspective in the supplied list of
   * perspective IDs
   * 
   * @param perspectiveIDs the list of perspective IDs
   * @param enabled true or false to enable or disable the perspective buttons
   */
  public void setEnablePerspectiveTabs(List<String> perspectiveIDs,
    boolean enabled) {
    for (int i = 0; i < m_perspectives.size(); i++) {
      Perspective p = m_perspectives.get(i);
      if (perspectiveIDs.contains(p.getPerspectiveID())) {
        m_perspectiveToolBar.getComponent(i).setEnabled(enabled);
      }
    }
  }

  /**
   * Enable/disable a perspective's button/tab
   * 
   * @param perspectiveID the ID of the perspective to enable/disable
   * @param enabled true or false to enable or disable
   */
  public void setEnablePerspectiveTab(String perspectiveID, boolean enabled) {
    for (int i = 0; i < m_perspectives.size(); i++) {
      Perspective p = m_perspectives.get(i);
      if (p.getPerspectiveID().equals(perspectiveID) && p.okToBeActive()) {
        m_perspectiveToolBar.getComponent(i).setEnabled(enabled);
      }
    }
  }

  /**
   * Returns true if the perspective toolbar is visible
   * 
   * @return true if the perspective toolbar is visible
   */
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

  /**
   * Get the perspective with the given ID
   * 
   * @param ID the ID of the perspective to get
   * @return the perspective, or null if there is no perspective with the
   *         supplied ID
   */
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

  /**
   * Tell the perspective manager to show the menu bar
   * 
   * @param topLevelAncestor the owning application's Frame
   */
  public void showMenuBar(JFrame topLevelAncestor) {
    topLevelAncestor.setJMenuBar(m_appMenuBar);
  }

  /**
   * Returns true if the user has requested that the perspective toolbar is
   * visible when the application starts up
   *
   * @param settings the settings object for the application
   * @return true if the user has specified that the perspective toolbar should
   *         be visible when the application first starts up
   */
  public boolean userRequestedPerspectiveToolbarVisibleOnStartup(
    Settings settings) {
    SelectedPerspectivePreferences perspectivePreferences =
      settings.getSetting(m_mainApp.getApplicationID(),
        VISIBLE_PERSPECTIVES_KEY, new SelectedPerspectivePreferences(),
        Environment.getSystemWide());
    return perspectivePreferences.getPerspectivesToolbarVisibleOnStartup();
  }

  /**
   * Class to manage user preferences with respect to visible perspectives and
   * whether the perspectives toolbar is always hidden or is visible on
   * application startup
   */
  public static class SelectedPerspectivePreferences implements
    java.io.Serializable {
    private static final long serialVersionUID = -2665480123235382483L;

    /** List of user selected perspectives to show */
    protected LinkedList<String> m_userVisiblePerspectives =
      new LinkedList<String>();

    /** Whether the toolbar should be visible (or hidden) at startup */
    protected boolean m_perspectivesToolbarVisibleOnStartup;

    /**
     * Whether the toolbar should always be hidden (and not able to be toggled
     * from the menu or widget.
     */
    protected boolean m_perspectivesToolbarAlwaysHidden;

    /**
     * Set a list of perspectives that should be visible
     *
     * @param userVisiblePerspectives
     */
    public void setUserVisiblePerspectives(
      LinkedList<String> userVisiblePerspectives) {
      m_userVisiblePerspectives = userVisiblePerspectives;
    }

    /**
     * Get the list of perspectives that the user has specified should be
     * visible in the application
     * 
     * @return the list of visible perspectives
     */
    public LinkedList<String> getUserVisiblePerspectives() {
      return m_userVisiblePerspectives;
    }

    /**
     * Set whether the perspectives toolbar should be visible in the GUI at
     * application startup
     *
     * @param v true if the perspectives toolbar should be visible at
     *          application startup
     */
    public void setPerspectivesToolbarVisibleOnStartup(boolean v) {
      m_perspectivesToolbarVisibleOnStartup = v;
    }

    /**
     * Get whether the perspectives toolbar should be visible in the GUI at
     * application startup
     *
     * @return true if the perspectives toolbar should be visible at application
     *         startup
     */
    public boolean getPerspectivesToolbarVisibleOnStartup() {
      return m_perspectivesToolbarVisibleOnStartup;
    }

    /**
     * Set whether the perspectives toolbar should always be hidden
     *
     * @param h true if the perspectives toolbar should always be hidden
     */
    public void setPerspectivesToolbarAlwaysHidden(boolean h) {
      m_perspectivesToolbarAlwaysHidden = h;
    }

    /**
     * Get whether the perspectives toolbar should always be hidden
     *
     * @return true if the perspectives toolbar should always be hidden
     */
    public boolean getPerspectivesToolbarAlwaysHidden() {
      return m_perspectivesToolbarAlwaysHidden;
    }
  }
}
