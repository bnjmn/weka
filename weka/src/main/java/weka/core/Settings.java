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
 *    Settings
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import weka.core.metastore.MetaStore;
import weka.core.metastore.XMLFileBasedMetaStore;
import weka.knowledgeflow.LoggingLevel;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maintains a collection of settings. Settings are key value pairs which can be
 * grouped together under a given name. All settings managed by an instance
 * of this class are persisted in the central metastore under a given store
 * name. For example, the store name could be the name/ID of an application/
 * system, and settings could be grouped according to applications, components,
 * panels etc. Default settings (managed by {@code Defaults}
 * objects) can be applied to provide initial defaults (or to allow new settings
 * that have yet to be persisted to be added in the future).
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class Settings implements Serializable {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -4005372566372478008L;

  /**
   * outer map keyed by perspective ID. Inner map - settings for a perspective.
   */
  protected Map<String, Map<SettingKey, Object>> m_settings =
    new LinkedHashMap<String, Map<SettingKey, Object>>();

  /**
   * The name of the store that these settings should be saved/loaded to/from
   */
  protected String m_storeName = "";

  /**
   * The name of the entry within the store to save to
   */
  protected String m_ID = "";

  /**
   * Load the settings with ID m_ID from store m_storeName.
   *
   * @throws IOException if a problem occurs
   */
  @SuppressWarnings("unchecked")
  public void loadSettings() throws IOException {
    MetaStore store = new XMLFileBasedMetaStore();

    Map<String, Map<SettingKey, Object>> loaded =
      (Map<String, Map<SettingKey, Object>>) store.getEntry(m_storeName, m_ID,
        Map.class);

    if (loaded != null) {
      m_settings = loaded;
    }

    // unwrap EnumHelper/FontHelper/FileHelper instances
    for (Map<SettingKey, Object> s : m_settings.values()) {
      for (Map.Entry<SettingKey, Object> e : s.entrySet()) {
        if (e.getValue() instanceof EnumHelper) {
          SettingKey key = e.getKey();
          EnumHelper eHelper = (EnumHelper) e.getValue();
          try {
            // get the actual enumerated value
            Object actualValue =
              EnumHelper.valueFromString(eHelper.getEnumClass(),
                eHelper.getSelectedEnumValue());
            s.put(key, actualValue);
          } catch (Exception ex) {
            throw new IOException(ex);
          }
        } else if (e.getValue() instanceof FontHelper) {
          SettingKey key = e.getKey();
          FontHelper fHelper = (FontHelper) e.getValue();
          Font f = fHelper.getFont();
          s.put(key, f);
        } else if (e.getValue() instanceof FileHelper) {
          SettingKey key = e.getKey();
          FileHelper fileHelper = (FileHelper) e.getValue();
          File f = fileHelper.getFile();
          s.put(key, f);
        }
      }
    }
  }

  /**
   * Construct a new Settings object to be stored in the supplied store under
   * the given ID/name
   *
   * @param storeName the name of the store to load/save to in the metastore
   * @param ID the ID/name to use
   */
  public Settings(String storeName, String ID) {
    m_storeName = storeName;
    m_ID = ID;
    try {
      loadSettings();
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  /**
   * Get the ID used for these settings
   *
   * @return the ID used
   */
  public String getID() {
    return m_ID;
  }

  /**
   * Get the store name for these settings
   *
   * @return the store name
   */
  public String getStoreName() {
    return m_storeName;
  }

  /**
   * Applies a set of default settings. If the ID of default settings is not
   * known then a new entry is made for it. Otherwise we examine all settings in
   * the default set supplied and add any that we don't have a value for.
   *
   * @param defaults the defaults to apply
   */
  public void applyDefaults(Defaults defaults) {
    if (defaults == null) {
      return;
    }
    Map<SettingKey, Object> settingsForID = m_settings.get(defaults.getID());
    if (settingsForID == null) {
      settingsForID = new LinkedHashMap<SettingKey, Object>();
      m_settings.put(defaults.getID(), settingsForID);
    }

    // add all settings that don't exist in the set we loaded for this ID
    for (Map.Entry<SettingKey, Object> e : defaults.getDefaults().entrySet()) {
      if (!settingsForID.containsKey(e.getKey())) {
        settingsForID.put(e.getKey(), e.getValue());
      }
    }
  }

  /**
   * Get the settings for a given ID
   *
   * @param settingsID the ID to get settings for
   * @return a map of settings, or null if the given ID is unknown
   */
  public Map<SettingKey, Object> getSettings(String settingsID) {
    return m_settings.get(settingsID);
  }

  /**
   * Get a list of settings IDs
   *
   * @return a list of the settings IDs managed by this settings instance
   */
  public Set<String> getSettingsIDs() {
    return m_settings.keySet();
  }

  public <T> T
    getSetting(String ID, String key, T defaultValue, Environment env) {
    SettingKey tempKey = new SettingKey(key, "", "");
    return getSetting(ID, tempKey, defaultValue, env);
  }

  /**
   * Get the value of a setting
   * 
   * @param ID the ID for settings map to lookup (typically the ID of a
   *          perspective)
   * @param key the name of the setting value to lookup
   * @param defaultValue the default value to use if the setting is not known
   * @param <T> the type of the vaue
   * @return the setting value, or the default value if not found
   */
  public <T> T getSetting(String ID, SettingKey key, T defaultValue) {
    return getSetting(ID, key, defaultValue, Environment.getSystemWide());
  }

  /**
   * Get the value of a setting
   *
   * @param ID the ID for settings map to lookup (typically the ID of a
   *          perspective)
   * @param key the name of the setting value to lookup
   * @param defaultValue the default value to use if the setting is not known
   * @param env environment variables to use. String setting values will have
   *          environment variables replaced automatically
   * @param <T> the type of the vaue
   * @return the setting value, or the default value if not found
   */
  // TOOD new...
  @SuppressWarnings("unchecked")
  public <T> T getSetting(String ID, SettingKey key, T defaultValue,
    Environment env) {
    Map<SettingKey, Object> settingsForID = m_settings.get(ID);
    T value = null;
    if (settingsForID != null && settingsForID.size() > 0) {
      value = (T) settingsForID.get(key);
      if (value instanceof String) {
        try {
          value = (T) env.substitute((String) value);
        } catch (Exception ex) {
          // ignore
        }
      }
    }

    if (value == null) {
      // Next is environment
      if (env != null) {
        String val = env.getVariableValue(key.getKey());
        if (val != null) {
          value = stringToT(val, defaultValue);
        }
      }
    }

    // Try system props
    if (value == null) {
      String val = System.getProperty(key.getKey());
      if (val != null) {
        value = stringToT(val, defaultValue);
      }
    }

    return value != null ? value : defaultValue;
  }

  /**
   * Set a value for a setting.
   *
   * @param ID the for the settings map to store the setting in (typically the
   *          ID of a perspective or application)
   * @param propName the name of the setting to store
   * @param value the value of the setting to store
   */
  public void setSetting(String ID, SettingKey propName, Object value) {
    Map<SettingKey, Object> settingsForID = m_settings.get(ID);
    if (settingsForID == null) {
      settingsForID = new LinkedHashMap<SettingKey, Object>();
      m_settings.put(ID, settingsForID);
    }
    settingsForID.put(propName, value);
  }

  /**
   * Returns true if there are settings available for a given ID
   *
   * @param settingsID the ID to check
   * @return true if there are settings available for that ID
   */
  public boolean hasSettings(String settingsID) {
    return m_settings.containsKey(settingsID);
  }

  /**
   * Returns true if a given setting has a value
   *
   * @param settingsID the ID of the settings group
   * @param propName the actual setting to check for
   * @return true if the setting has a value
   */
  public boolean hasSetting(String settingsID, String propName) {
    if (!hasSettings(settingsID)) {
      return false;
    }

    return m_settings.get(settingsID).containsKey(propName);
  }

  /**
   * Save the settings to the metastore
   *
   * @throws IOException if a problem occurs
   */
  public void saveSettings() throws IOException {
    // shallow copy settings so that we can wrap any enums in EnumHelper
    // objects before persisting
    Map<String, Map<SettingKey, Object>> settingsCopy =
      new LinkedHashMap<String, Map<SettingKey, Object>>();
    for (Map.Entry<String, Map<SettingKey, Object>> e : m_settings.entrySet()) {
      Map<SettingKey, Object> s = new LinkedHashMap<SettingKey, Object>();
      settingsCopy.put(e.getKey(), s);

      for (Map.Entry<SettingKey, Object> ee : e.getValue().entrySet()) {
        if (ee.getValue() instanceof Enum) {
          EnumHelper wrapper = new EnumHelper((Enum) ee.getValue());
          s.put(ee.getKey(), wrapper);
        } else if (ee.getValue() instanceof Font) {
          FontHelper wrapper = new FontHelper((Font) ee.getValue());
          s.put(ee.getKey(), wrapper);
        } else if (ee.getValue() instanceof File) {
          FileHelper wrapper = new FileHelper((File) ee.getValue());
          s.put(ee.getKey(), wrapper);
        } else {
          s.put(ee.getKey(), ee.getValue());
        }
      }
    }

    XMLFileBasedMetaStore store = new XMLFileBasedMetaStore();

    if (m_settings.size() > 0) {
      store.storeEntry(m_storeName, m_ID, settingsCopy);
    }
  }

  /**
   * Convert a setting value stored in a string to type T
   *
   * @param propVal the value of the setting as a string
   * @param defaultVal the default value for the setting (of type T)
   * @param <T> the type of the setting
   * @return the setting as an instance of type T
   */
  protected static <T> T stringToT(String propVal, T defaultVal) {
    if (defaultVal instanceof String) {
      return (T) propVal;
    }

    if (defaultVal instanceof Boolean) {
      return (T) (Boolean.valueOf(propVal));
    }

    if (defaultVal instanceof Double) {
      return (T) (Double.valueOf(propVal));
    }

    if (defaultVal instanceof Integer) {
      return (T) (Integer.valueOf(propVal));
    }

    if (defaultVal instanceof Long) {
      return (T) (Long.valueOf(propVal));
    }

    if (defaultVal instanceof LoggingLevel) {
      return (T) (LoggingLevel.stringToLevel(propVal));
    }

    return null;
  }

  /**
   * Class implementing a key for a setting. Has a unique key, description, tool
   * tip text and map of arbitrary other metadata
   */
  public static class SettingKey implements java.io.Serializable {

    /** Key for this setting */
    protected String m_key;

    /** Description/display name of the seting */
    protected String m_description;

    /** Tool tip for the setting */
    protected String m_toolTip;

    /** Pick list (can be null if not applicable) for a string-based setting */
    protected List<String> m_pickList;

    /**
     * Metadata for this setting - e.g. file property could specify whether it
     * is files only, directories only or both
     */
    protected Map<String, String> m_meta;

    /**
     * Construct a new empty setting key
     */
    public SettingKey() {
      this("", "", "");
    }

    /**
     * Construct a new SettingKey with the given key, description and tool tip
     * text
     *
     * @param key the key of this SettingKey
     * @param description the description (display name of the setting)
     * @param toolTip the tool tip text for the setting
     */
    public SettingKey(String key, String description, String toolTip) {
      this(key, description, toolTip, null);
    }

    /**
     * Construct a new SettingKey with the given key, description, tool tip and
     * pick list
     *
     * @param key the key of this SettingKey
     * @param description the description (display name of the setting)
     * @param toolTip the tool tip for the setting
     * @param pickList an optional list of legal values for a string setting
     */
    public SettingKey(String key, String description, String toolTip,
      List<String> pickList) {
      m_key = key;
      m_description = description;
      m_toolTip = toolTip;
      m_pickList = pickList;
    }

    /**
     * set the key of this setting
     *
     * @param key the key to use
     */
    public void setKey(String key) {
      m_key = key;
    }

    /**
     * Get the key of this setting
     *
     * @return the key of this setting
     */
    public String getKey() {
      return m_key;
    }

    /**
     * Set the description (display name) of this setting
     *
     * @param description the description of this setting
     */
    public void setDescription(String description) {
      m_description = description;
    }

    /**
     * Get the description (display name) of this setting
     *
     * @return the description of this setting
     */
    public String getDescription() {
      return m_description;
    }

    /**
     * Set the tool tip text for this setting
     *
     * @param toolTip the tool tip text to use
     */
    public void setToolTip(String toolTip) {
      m_toolTip = toolTip;
    }

    /**
     * Get the tool tip text for this setting
     *
     * @return the tool tip text to use
     */
    public String getToolTip() {
      return m_toolTip;
    }

    /**
     * Set the value of a piece of metadata for this setting
     *
     * @param key the key for the metadata
     * @param value the value of the metadata
     */
    public void setMetadataElement(String key, String value) {
      if (m_meta == null) {
        m_meta = new HashMap<String, String>();
      }

      m_meta.put(key, value);
    }

    /**
     * Get a piece of metadata for this setting
     *
     * @param key the key of the metadata
     * @return the corresponding value, or null if the key is not known
     */
    public String getMetadataElement(String key) {
      if (m_meta == null) {
        return null;
      }

      return m_meta.get(key);
    }

    /**
     * Get a peice of metadata for this setting
     *
     * @param key the key of the metadata
     * @param defaultValue the default value for the metadata
     * @return the corresponding value, or the default value if the key is not
     *         known
     */
    public String getMetadataElement(String key, String defaultValue) {
      String result = getMetadataElement(key);
      return result == null ? defaultValue : result;
    }

    /**
     * Set the metadata for this setting
     *
     * @param metadata the metadata for this setting
     */
    public void setMetadata(Map<String, String> metadata) {
      m_meta = metadata;
    }

    /**
     * Get the metadata for this setting
     *
     * @return the metadata for this setting
     */
    public Map<String, String> getMetadata() {
      return m_meta;
    }

    /**
     * Get the optional pick list for the setting
     * 
     * @return the optional pick list for the setting (can be null if not
     *         applicable)
     */
    public List<String> getPickList() {
      return m_pickList;
    }

    /**
     * Set the optional pick list for the setting
     *
     * @param pickList the optional pick list for the setting (can be null if
     *          not applicable)
     */
    public void setPickList(List<String> pickList) {
      m_pickList = pickList;
    }

    /**
     * Hashcode based on the key
     *
     * @return the hashcode of this setting
     */
    @Override
    public int hashCode() {
      return m_key.hashCode();
    }

    /**
     * Compares two setting keys for equality
     *
     * @param other the other setting key to compare to
     * @return true if this setting key is equal to the supplied one
     */
    @Override
    public boolean equals(Object other) {
      if (!(other instanceof SettingKey)) {
        return false;
      }
      return m_key.equals(((SettingKey) other).getKey());
    }

    /**
     * Return the description (display name) of this setting
     *
     * @return the description of this setting
     */
    @Override
    public String toString() {
      return m_description;
    }
  }
}
