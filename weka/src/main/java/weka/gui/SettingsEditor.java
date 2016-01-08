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
 *    SettingsEditor.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui;

import weka.classifiers.Classifier;
import weka.clusterers.Clusterer;
import weka.core.Environment;
import weka.core.Settings;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides a panel for editing application and perspective settings
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class SettingsEditor extends JPanel {

  private static final long serialVersionUID = 1453121012707399758L;

  protected GUIApplication m_ownerApp;
  protected Settings m_settings;
  protected JTabbedPane m_settingsTabs = new JTabbedPane();

  protected PerspectiveSelector m_perspectiveSelector;
  List<SingleSettingsEditor> m_perspectiveEditors =
    new ArrayList<SingleSettingsEditor>();

  public SettingsEditor(Settings settings, GUIApplication ownerApp) {
    setLayout(new BorderLayout());
    m_settings = settings;
    m_ownerApp = ownerApp;
    GenericObjectEditor.registerEditors();
    PropertyEditorManager.registerEditor(Color.class, ColorEditor.class);

    if (m_ownerApp.getPerspectiveManager().getLoadedPerspectives().size() > 0) {
      setupPerspectiveSelector();
    }
    setupPerspectiveSettings();
    add(m_settingsTabs, BorderLayout.CENTER);
  }

  public void applyToSettings() {
    if (m_perspectiveSelector != null) {
      m_perspectiveSelector.applyToSettings();
    }

    for (SingleSettingsEditor editor : m_perspectiveEditors) {
      editor.applyToSettings();
    }
  }

  protected void setupPerspectiveSelector() {
    m_perspectiveSelector = new PerspectiveSelector();
    m_settingsTabs.addTab("Perspectives", m_perspectiveSelector);
  }

  protected void setupPerspectiveSettings() {
    // any general settings? >1 because app settings have visible perspectives
    // settings
    if (m_settings.getSettings(m_ownerApp.getApplicationID()) != null
      && m_settings.getSettings(m_ownerApp.getApplicationID()).size() > 1) {
      Map<Settings.SettingKey, Object> appSettings =
        m_settings.getSettings(m_ownerApp.getApplicationID());
      SingleSettingsEditor appEditor = new SingleSettingsEditor(appSettings);
      m_settingsTabs.addTab("General", appEditor);
      m_perspectiveEditors.add(appEditor);
    }

    // main perspective
    Perspective mainPers = m_ownerApp.getMainPerspective();
    String mainTitle = mainPers.getPerspectiveTitle();
    String mainID = mainPers.getPerspectiveID();
    SingleSettingsEditor mainEditor =
      new SingleSettingsEditor(m_settings.getSettings(mainID));
    m_settingsTabs.addTab(mainTitle, mainEditor);
    m_perspectiveEditors.add(mainEditor);

    List<Perspective> availablePerspectives =
      m_ownerApp.getPerspectiveManager().getLoadedPerspectives();
    List<String> availablePerspectivesIDs = new ArrayList<String>();
    List<String> availablePerspectiveTitles = new ArrayList<String>();
    for (int i = 0; i < availablePerspectives.size(); i++) {
      Perspective p = availablePerspectives.get(i);
      availablePerspectivesIDs.add(p.getPerspectiveID());
      availablePerspectiveTitles.add(p.getPerspectiveTitle());
    }

    Set<String> settingsIDs = m_settings.getSettingsIDs();
    for (String settingID : settingsIDs) {
      if (availablePerspectivesIDs.contains(settingID)) {
        int indexOfP = availablePerspectivesIDs.indexOf(settingID);

        // make a tab for this one
        Map<Settings.SettingKey, Object> settingsForID =
          m_settings.getSettings(settingID);
        if (settingsForID != null && settingsForID.size() > 0) {
          SingleSettingsEditor perpEditor =
            new SingleSettingsEditor(settingsForID);
          m_settingsTabs.addTab(availablePerspectiveTitles.get(indexOfP),
            perpEditor);
          m_perspectiveEditors.add(perpEditor);
        }
      }
    }
  }

  /**
   * Creates a single stand-alone settings editor panel
   *
   * @param settingsToEdit the settings to edit
   * @return a single settings editor panel for the supplied settings
   */
  public static SingleSettingsEditor createSingleSettingsEditor(
    Map<Settings.SettingKey, Object> settingsToEdit) {
    GenericObjectEditor.registerEditors();
    PropertyEditorManager.registerEditor(Color.class, ColorEditor.class);
    return new SingleSettingsEditor(settingsToEdit);
  }

  /**
   * Popup a single panel settings editor dialog for one group of related
   * settings
   *
   * @param settings the settings object containing (potentially) many groups of
   *          settings
   * @param settingsID the ID of the particular set of settings to edit
   * @param settingsName the name of this related set of settings
   * @param parent the parent window
   * @return the result chosen by the user (JOptionPane.OK_OPTION or
   *         JOptionPanel.CANCEL_OPTION)
   * @throws IOException if saving altered settings fails
   */
  public static int showSingleSettingsEditor(Settings settings,
    String settingsID, String settingsName, JComponent parent)
      throws IOException {
    return showSingleSettingsEditor(settings, settingsID, settingsName, parent,
      600, 300);
  }

  /**
   * Popup a single panel settings editor dialog for one group of related
   * settings
   * 
   * @param settings the settings object containing (potentially) many groups of
   *          settings
   * @param settingsID the ID of the particular set of settings to edit
   * @param settingsName the name of this related set of settings
   * @param parent the parent window
   * @param width the width for the dialog
   * @param height the height for the dialog
   * @return the result chosen by the user (JOptionPane.OK_OPTION or
   *         JOptionPanel.CANCEL_OPTION)
   * @throws IOException if saving altered settings fails
   */
  public static int showSingleSettingsEditor(Settings settings,
    String settingsID, String settingsName, JComponent parent, int width,
    int height) throws IOException {
    final SingleSettingsEditor sse =
      createSingleSettingsEditor(settings.getSettings(settingsID));
    sse.setPreferredSize(new Dimension(width, height));

    final JOptionPane pane = new JOptionPane(sse, JOptionPane.PLAIN_MESSAGE,
      JOptionPane.OK_CANCEL_OPTION);

    // There appears to be a bug in Java > 1.6 under Linux that, more often than
    // not, causes a sun.awt.X11.XException to occur when the following code
    // to make the dialog resizable is used. A workaround is to set the
    // suppressSwingDropSupport property to true (but this has to be done
    // at JVM startup, and setting it programatically, no matter how early,
    // does not seem to work). The hacky workaround here is to check for
    // a nix OS and disable the resizing, unless the user has specifically
    // used the -DsuppressSwingDropSupport=true JVM flag.
    //
    // See: http://bugs.java.com/view_bug.do?bug_id=7027598
    // and: http://mipav.cit.nih.gov/pubwiki/index.php/FAQ:_Why_do_I_get_an_exception_when_running_MIPAV_via_X11_forwarding_on_Linux%3F
    String os = System.getProperty("os.name").toLowerCase();
    String suppressSwingDropSupport =
      System.getProperty("suppressSwingDropSupport", "false");
    boolean nix = os.contains("nix") || os.contains("nux") || os.contains("aix");
    if (!nix || suppressSwingDropSupport.equalsIgnoreCase("true")) {
      pane.addHierarchyListener(new HierarchyListener() {
        @Override public void hierarchyChanged(HierarchyEvent e) {
          Window window = SwingUtilities.getWindowAncestor(pane);
          if (window instanceof Dialog) {
            Dialog dialog = (Dialog) window;
            if (!dialog.isResizable()) {
              dialog.setResizable(true);
            }
          }
        }
      });
    }
    JDialog dialog =
      pane.createDialog((JComponent) parent, settingsName + " Settings");
    dialog.show();
    Object resultO = pane.getValue();

    /*
     * int result = JOptionPane.showConfirmDialog(parent, sse, settingsName +
     * " Settings", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
     * wekaIcon);
     */

    int result = -1;
    if (resultO == null) {
      result = JOptionPane.CLOSED_OPTION;
    } else if (resultO instanceof Integer) {
      result = (Integer) resultO;
    }

    if (result == JOptionPane.OK_OPTION) {
      sse.applyToSettings();
      settings.saveSettings();
    }

    if (result == JOptionPane.OK_OPTION) {
      sse.applyToSettings();
      settings.saveSettings();
    }

    return result;
  }

  /**
   * Popup a settings editor for an application
   *
   * @param settings the settings object for the application
   * @param application the application itself
   * @return the result chosen by the user (JOptionPane.OK_OPTION or
   *         JOptionPanel.CANCEL)
   * @throws IOException if saving altered settings fails
   */
  public static int showApplicationSettingsEditor(Settings settings,
    GUIApplication application) throws IOException {

    final SettingsEditor settingsEditor =
      new SettingsEditor(settings, application);
    settingsEditor.setPreferredSize(new Dimension(800, 350));

    final JOptionPane pane = new JOptionPane(settingsEditor,
      JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);

    // There appears to be a bug in Java > 1.6 under Linux that, more often than
    // not, causes a sun.awt.X11.XException to occur when the following code
    // to make the dialog resizable is used. A workaround is to set the
    // suppressSwingDropSupport property to true (but this has to be done
    // at JVM startup, and setting it programatically, no matter how early,
    // does not seem to work). The hacky workaround here is to check for
    // a nix OS and disable the resizing, unless the user has specifically
    // used the -DsuppressSwingDropSupport=true JVM flag.
    //
    // See: http://bugs.java.com/view_bug.do?bug_id=7027598
    // and: http://mipav.cit.nih.gov/pubwiki/index.php/FAQ:_Why_do_I_get_an_exception_when_running_MIPAV_via_X11_forwarding_on_Linux%3F
    String os = System.getProperty("os.name").toLowerCase();
    String suppressSwingDropSupport =
      System.getProperty("suppressSwingDropSupport", "false");
    boolean nix = os.contains("nix") || os.contains("nux") || os.contains("aix");
    if (!nix || suppressSwingDropSupport.equalsIgnoreCase("true")) {
      pane.addHierarchyListener(new HierarchyListener() {
        @Override public void hierarchyChanged(HierarchyEvent e) {
          Window window = SwingUtilities.getWindowAncestor(pane);
          if (window instanceof Dialog) {
            Dialog dialog = (Dialog) window;
            if (!dialog.isResizable()) {
              dialog.setResizable(true);
            }
          }
        }
      });
    }
    JDialog dialog = pane.createDialog((JComponent) application,
      application.getApplicationName() + " Settings");
    dialog.show();
    Object resultO = pane.getValue();
    int result = -1;
    if (resultO == null) {
      result = JOptionPane.CLOSED_OPTION;
    } else if (resultO instanceof Integer) {
      result = (Integer) resultO;
    }

    if (result == JOptionPane.OK_OPTION) {
      settingsEditor.applyToSettings();
      settings.saveSettings();
    }

    return result;
  }

  public static class SingleSettingsEditor extends JPanel
    implements PropertyChangeListener {

    private static final long serialVersionUID = 8896265984902770239L;
    protected Map<Settings.SettingKey, Object> m_perspSettings;
    protected Map<Settings.SettingKey, PropertyEditor> m_editorMap =
      new LinkedHashMap<Settings.SettingKey, PropertyEditor>();

    public SingleSettingsEditor(Map<Settings.SettingKey, Object> pSettings) {
      m_perspSettings = pSettings;
      setLayout(new BorderLayout());

      JPanel scrollablePanel = new JPanel();
      JScrollPane scrollPane = new JScrollPane(scrollablePanel);
      scrollPane.setBorder(BorderFactory.createEmptyBorder());
      add(scrollPane, BorderLayout.CENTER);

      GridBagLayout gbLayout = new GridBagLayout();

      scrollablePanel.setLayout(gbLayout);
      setVisible(false);

      int i = 0;
      for (Map.Entry<Settings.SettingKey, Object> prop : pSettings.entrySet()) {
        Settings.SettingKey settingName = prop.getKey();
        if (settingName.getKey()
          .equals(PerspectiveManager.VISIBLE_PERSPECTIVES_KEY.getKey())) {
          // skip this as we've got a dedicated panel for this one
          continue;
        }
        Object settingValue = prop.getValue();
        List<String> pickList = prop.getKey().getPickList();

        PropertyEditor editor = null;
        if (settingValue instanceof String && pickList != null
          && pickList.size() > 0) {
          // special case - list of legal values to choose from
          PickList pEditor = new PickList(pickList);
          pEditor.setValue(prop.getValue());
          editor = pEditor;
        }

        Class<?> settingClass = settingValue.getClass();

        if (editor == null) {
          editor = PropertyEditorManager.findEditor(settingClass);
        }
        // hardcoded check for File here because we don't register File as the
        // FileEnvironmentField does not play nicely with the standard GOE
        // practice
        // of listening/updating for every key stroke on text fields
        if (settingValue instanceof java.io.File) {

          String dialogType = settingName.getMetadataElement(
            "java.io.File.dialogType", "" + JFileChooser.OPEN_DIALOG);
          String fileType =
            settingName.getMetadataElement("java.io.File.fileSelectionMode",
              "" + JFileChooser.FILES_AND_DIRECTORIES);

          int dType = Integer.parseInt(dialogType);
          int fType = Integer.parseInt(fileType);

          editor = new FileEnvironmentField("", Environment.getSystemWide(),
            dType, fType == JFileChooser.DIRECTORIES_ONLY);
        }

        if (editor == null) {
          // TODO check for primitive classes (need to do this it seems to be
          // backward
          // compatible with Java 1.6 - PropertyEditorManager only has built in
          // editors
          // for primitive int, float etc.)
          if (settingValue instanceof Integer) {
            settingClass = Integer.TYPE;
          } else if (settingValue instanceof Float) {
            settingClass = Float.TYPE;
          } else if (settingValue instanceof Double) {
            settingClass = Double.TYPE;
          } else if (settingValue instanceof Boolean) {
            settingClass = Boolean.TYPE;
          } else if (settingValue instanceof Long) {
            settingClass = Long.TYPE;
          } else if (settingValue instanceof Short) {
            settingClass = Short.TYPE;
          } else if (settingValue instanceof Byte) {
            settingClass = Byte.TYPE;
          }

          // TODO need an editor for enums under Java 1.6

          // try again
          editor = PropertyEditorManager.findEditor(settingClass);
        }

        // Check a few special cases - where the following
        // interfaces followed by superclass approach might get
        // fooled (i.e. classifiers that implement multiple interfaces
        // that themselves have entries in GUIEditors.props)
        if (editor == null) {
          if (settingValue instanceof Classifier) {
            editor = PropertyEditorManager.findEditor(Classifier.class);
            settingClass = Classifier.class;
          } else if (settingValue instanceof Clusterer) {
            editor = PropertyEditorManager.findEditor(Clusterer.class);
            settingClass = Clusterer.class;
          }
        }

        if (editor == null) {
          while (settingClass != null && editor == null) {
            // try interfaces first
            Class<?>[] interfaces = settingClass.getInterfaces();
            if (interfaces != null) {
              for (Class intf : interfaces) {
                editor = PropertyEditorManager.findEditor(intf);
                if (editor != null) {
                  settingClass = intf;
                  break;
                }
              }
            }
            if (editor == null) {
              settingClass = settingClass.getSuperclass();
              if (settingClass != null) {
                editor = PropertyEditorManager.findEditor(settingClass);
              }
            }
          }
        }

        if (editor != null) {
          if (editor instanceof GenericObjectEditor) {
            ((GenericObjectEditor) editor).setClassType(settingClass);
          }
          editor.addPropertyChangeListener(this);
          editor.setValue(settingValue);
          JComponent view = null;

          if (editor.isPaintable() && editor.supportsCustomEditor()) {
            view = new PropertyPanel(editor);
          } else if (editor.supportsCustomEditor()
            && (editor.getCustomEditor() instanceof JComponent)) {
            view = (JComponent) editor.getCustomEditor();
          } else if (editor.getTags() != null) {
            view = new PropertyValueSelector(editor);
          } else if (editor.getAsText() != null) {
            view = new PropertyText(editor);
          } else {
            System.err.println("Warning: Property \"" + settingName
              + "\" has non-displayabale editor.  Skipping.");
            continue;
          }

          m_editorMap.put(settingName, editor);

          JLabel propLabel =
            new JLabel(settingName.getDescription(), SwingConstants.RIGHT);
          if (prop.getKey().getToolTip().length() > 0) {
            propLabel.setToolTipText(prop.getKey().getToolTip());
            view.setToolTipText(prop.getKey().getToolTip());
          }
          propLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 5));
          GridBagConstraints gbConstraints = new GridBagConstraints();
          gbConstraints.anchor = GridBagConstraints.EAST;
          gbConstraints.fill = GridBagConstraints.HORIZONTAL;
          gbConstraints.gridy = i;
          gbConstraints.gridx = 0;
          gbLayout.setConstraints(propLabel, gbConstraints);
          scrollablePanel.add(propLabel);

          JPanel newPanel = new JPanel();
          newPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 0, 10));
          newPanel.setLayout(new BorderLayout());
          newPanel.add(view, BorderLayout.CENTER);
          gbConstraints = new GridBagConstraints();
          gbConstraints.anchor = GridBagConstraints.WEST;
          gbConstraints.fill = GridBagConstraints.BOTH;
          gbConstraints.gridy = i;
          gbConstraints.gridx = 1;
          gbConstraints.weightx = 100;
          gbLayout.setConstraints(newPanel, gbConstraints);
          scrollablePanel.add(newPanel);

          i++;
        } else {
          System.err.println("SettingsEditor can't find an editor for: "
            + settingClass.toString());
        }
      }

      Dimension dim = scrollablePanel.getPreferredSize();
      dim.height += 20;
      dim.width += 20;
      scrollPane.setPreferredSize(dim);
      validate();

      setVisible(true);
    }

    public void applyToSettings() {
      for (Map.Entry<Settings.SettingKey, Object> e : m_perspSettings
        .entrySet()) {
        Settings.SettingKey settingKey = e.getKey();
        if (settingKey.getKey()
          .equals(PerspectiveManager.VISIBLE_PERSPECTIVES_KEY.getKey())) {
          continue;
        }
        PropertyEditor editor = m_editorMap.get(settingKey);
        if (editor != null) {
          Object newSettingValue = editor.getValue();
          m_perspSettings.put(settingKey, newSettingValue);
        }
      }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      repaint();
      revalidate();
    }
  }

  protected class PerspectiveSelector extends JPanel {

    private static final long serialVersionUID = -4765015948030757897L;
    protected List<JCheckBox> m_perspectiveChecks = new ArrayList<JCheckBox>();
    protected JCheckBox m_toolBarVisibleOnStartup =
      new JCheckBox("Perspective toolbar visible on start up");

    public PerspectiveSelector() {
      setLayout(new BorderLayout());

      List<Perspective> availablePerspectives =
        m_ownerApp.getPerspectiveManager().getLoadedPerspectives();
      if (availablePerspectives.size() > 0) {
        PerspectiveManager.SelectedPerspectivePreferences userSelected =
          new PerspectiveManager.SelectedPerspectivePreferences();
        userSelected = m_settings.getSetting(m_ownerApp.getApplicationID(),
          PerspectiveManager.VISIBLE_PERSPECTIVES_KEY, userSelected,
          Environment.getSystemWide());

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        if (!userSelected.getPerspectivesToolbarAlwaysHidden()) {
          p.add(m_toolBarVisibleOnStartup);
          m_toolBarVisibleOnStartup
            .setSelected(userSelected.getPerspectivesToolbarVisibleOnStartup());
        }

        for (Perspective perspective : availablePerspectives) {
          String pName = perspective.getPerspectiveTitle();
          JCheckBox jb = new JCheckBox(pName);
          jb.setSelected(
            userSelected.getUserVisiblePerspectives().contains(pName));
          m_perspectiveChecks.add(jb);
          p.add(jb);
        }

        add(p, BorderLayout.CENTER);
      }
    }

    public void applyToSettings() {
      LinkedList<String> selectedPerspectives = new LinkedList<String>();
      for (JCheckBox c : m_perspectiveChecks) {
        if (c.isSelected()) {
          selectedPerspectives.add(c.getText());
        }
      }
      PerspectiveManager.SelectedPerspectivePreferences newPrefs =
        new PerspectiveManager.SelectedPerspectivePreferences();
      newPrefs.setUserVisiblePerspectives(selectedPerspectives);
      newPrefs.setPerspectivesToolbarVisibleOnStartup(
        m_toolBarVisibleOnStartup.isSelected());
      m_settings.setSetting(m_ownerApp.getApplicationID(),
        PerspectiveManager.VISIBLE_PERSPECTIVES_KEY, newPrefs);
    }
  }

  /**
   * Simple property editor for pick lists
   */
  protected static class PickList extends JPanel implements PropertyEditor {

    private static final long serialVersionUID = 3505647427533464230L;
    protected JComboBox<String> m_list = new JComboBox<String>();

    public PickList(List<String> list) {
      for (String item : list) {
        m_list.addItem(item);
      }

      setLayout(new BorderLayout());
      add(m_list, BorderLayout.CENTER);
    }

    @Override
    public void setValue(Object value) {
      m_list.setSelectedItem(value.toString());
    }

    @Override
    public Object getValue() {
      return m_list.getSelectedItem();
    }

    @Override
    public boolean isPaintable() {
      return false;
    }

    @Override
    public void paintValue(Graphics gfx, Rectangle box) {

    }

    @Override
    public String getJavaInitializationString() {
      return null;
    }

    @Override
    public String getAsText() {
      return null;
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {

    }

    @Override
    public String[] getTags() {
      return null;
    }

    @Override
    public Component getCustomEditor() {
      return this;
    }

    @Override
    public boolean supportsCustomEditor() {
      return true;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {

    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {

    }
  }
}
