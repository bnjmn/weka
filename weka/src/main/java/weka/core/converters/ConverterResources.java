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

/**
 * ConverterResources.java
 * Copyright (C) 2017 University of Waikato, Hamilton, NZ
 */

package weka.core.converters;

import weka.core.InheritanceUtils;
import weka.core.PluginManager;
import weka.core.WekaPackageClassLoaderManager;
import weka.gui.GenericPropertiesCreator;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

/**
 * Helper class for dealing with Converter resources.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class ConverterResources {

  /**
   * the core loaders - hardcoded list necessary for RMI/Remote Experiments
   * (comma-separated list).
   */
  public final static String CORE_FILE_LOADERS = weka.core.converters.ArffLoader.class
    .getName()
    + ","
    // + weka.core.converters.C45Loader.class.getName() + ","
    + weka.core.converters.CSVLoader.class.getName()
    + ","
    + weka.core.converters.DatabaseConverter.class.getName()
    + ","
    // + weka.core.converters.LibSVMLoader.class.getName() + ","
    // + weka.core.converters.MatlabLoader.class.getName() + ","
    // + weka.core.converters.SVMLightLoader.class.getName() + ","
    + weka.core.converters.SerializedInstancesLoader.class.getName()
    + ","
    + weka.core.converters.TextDirectoryLoader.class.getName()
    + ","
    + weka.core.converters.XRFFLoader.class.getName();

  /**
   * the core savers - hardcoded list necessary for RMI/Remote Experiments
   * (comma-separated list).
   */
  public final static String CORE_FILE_SAVERS = weka.core.converters.ArffSaver.class
    .getName()
    + ","
    // + weka.core.converters.C45Saver.class.getName() + ","
    + weka.core.converters.CSVSaver.class.getName()
    + ","
    + weka.core.converters.DatabaseConverter.class.getName()
    + ","
    // + weka.core.converters.LibSVMSaver.class.getName() + ","
    // + weka.core.converters.MatlabSaver.class.getName() + ","
    // + weka.core.converters.SVMLightSaver.class.getName() + ","
    + weka.core.converters.SerializedInstancesSaver.class.getName()
    + ","
    + weka.core.converters.XRFFSaver.class.getName();

  /** all available loaders (extension &lt;-&gt; classname). */
  protected static Hashtable<String, String> m_FileLoaders;

  /** all available URL loaders (extension &lt;-&gt; classname). */
  protected static Hashtable<String, String> m_URLFileLoaders;

  /** all available savers (extension &lt;-&gt; classname). */
  protected static Hashtable<String, String> m_FileSavers;

  /**
   * checks whether the given class is one of the hardcoded core file loaders.
   *
   * @param classname the class to check
   * @return true if the class is one of the core loaders
   * @see ConverterResources#CORE_FILE_LOADERS
   */
  public static boolean isCoreFileLoader(String classname) {
    boolean result;
    String[] classnames;

    classnames = CORE_FILE_LOADERS.split(",");
    result = (Arrays.binarySearch(classnames, classname) >= 0);

    return result;
  }

  /**
   * checks whether the given class is one of the hardcoded core file savers.
   *
   * @param classname the class to check
   * @return true if the class is one of the core savers
   * @see ConverterResources#CORE_FILE_SAVERS
   */
  public static boolean isCoreFileSaver(String classname) {
    boolean result;
    String[] classnames;

    classnames = CORE_FILE_SAVERS.split(",");
    result = (Arrays.binarySearch(classnames, classname) >= 0);

    return result;
  }

  /**
   * Returns the file loaders.
   *
   * @return		the file loaders
   */
  public static Hashtable<String,String> getFileLoaders() {
    return m_FileLoaders;
  }

  /**
   * Returns the URL file loaders.
   *
   * @return		the URL file loaders
   */
  public static Hashtable<String,String> getURLFileLoaders() {
    return m_URLFileLoaders;
  }

  /**
   * Returns the file savers.
   *
   * @return		the file savers
   */
  public static Hashtable<String,String> getFileSavers() {
    return m_FileSavers;
  }

  public static void initialize() {
    List<String> classnames;

    try {
      // init
      m_FileLoaders = new Hashtable<String, String>();
      m_URLFileLoaders = new Hashtable<String, String>();
      m_FileSavers = new Hashtable<String, String>();

      // generate properties
      // Note: does NOT work with RMI, hence m_FileLoadersCore/m_FileSaversCore

      Properties props = GenericPropertiesCreator.getGlobalOutputProperties();
      if (props == null) {
        GenericPropertiesCreator creator = new GenericPropertiesCreator();

        creator.execute(false);
        props = creator.getOutputProperties();
      }

      // loaders
      m_FileLoaders = getFileConverters(
        props.getProperty(Loader.class.getName(), ConverterResources.CORE_FILE_LOADERS),
        new String[] { FileSourcedConverter.class.getName() });

      // URL loaders
      m_URLFileLoaders = getFileConverters(
        props.getProperty(Loader.class.getName(), ConverterResources.CORE_FILE_LOADERS),
        new String[] { FileSourcedConverter.class.getName(),
          URLSourcedLoader.class.getName() });

      // savers
      m_FileSavers = getFileConverters(
        props.getProperty(Saver.class.getName(), ConverterResources.CORE_FILE_SAVERS),
        new String[] { FileSourcedConverter.class.getName() });
    } catch (Exception e) {
      e.printStackTrace();
      // ignore
    } finally {
      // loaders
      if (m_FileLoaders.size() == 0) {
        classnames = PluginManager.getPluginNamesOfTypeList(AbstractFileLoader.class
          .getName());
        if (classnames.size() > 0) {
          m_FileLoaders = getFileConverters(classnames,
            new String[] { FileSourcedConverter.class.getName() });
        } else {
          m_FileLoaders = getFileConverters(ConverterResources.CORE_FILE_LOADERS,
            new String[] { FileSourcedConverter.class.getName() });
        }
      }

      // URL loaders
      if (m_URLFileLoaders.size() == 0) {
        classnames = PluginManager.getPluginNamesOfTypeList(AbstractFileLoader.class
          .getName());
        if (classnames.size() > 0) {
          m_URLFileLoaders = getFileConverters(classnames,
            new String[] { FileSourcedConverter.class.getName(),
              URLSourcedLoader.class.getName() });
        } else {
          m_URLFileLoaders = getFileConverters(ConverterResources.CORE_FILE_LOADERS,
            new String[] { FileSourcedConverter.class.getName(),
              URLSourcedLoader.class.getName() });
        }
      }

      // savers
      if (m_FileSavers.size() == 0) {
        classnames = PluginManager.getPluginNamesOfTypeList(AbstractFileSaver.class
          .getName());
        if (classnames.size() > 0) {
          m_FileSavers = getFileConverters(classnames,
            new String[] { FileSourcedConverter.class.getName() });
        } else {
          m_FileSavers = getFileConverters(ConverterResources.CORE_FILE_SAVERS,
            new String[] { FileSourcedConverter.class.getName() });
        }
      }
    }
  }

  /**
   * returns a hashtable with the association
   * "file extension &lt;-&gt; converter classname" for the comma-separated list
   * of converter classnames.
   *
   * @param classnames comma-separated list of converter classnames
   * @param intf interfaces the converters have to implement
   * @return hashtable with ExtensionFileFilters
   */
  protected static Hashtable<String, String> getFileConverters(
    String classnames, String[] intf) {
    Vector<String> list;
    String[] names;
    int i;

    list = new Vector<String>();
    names = classnames.split(",");
    for (i = 0; i < names.length; i++) {
      list.add(names[i]);
    }

    return getFileConverters(list, intf);
  }

  /**
   * returns a hashtable with the association
   * "file extension &lt;-&gt; converter classname" for the list of converter
   * classnames.
   *
   * @param classnames list of converter classnames
   * @param intf interfaces the converters have to implement
   * @return hashtable with ExtensionFileFilters
   */
  protected static Hashtable<String, String> getFileConverters(
    List<String> classnames, String[] intf) {
    Hashtable<String, String> result;
    String classname;
    Class<?> cls;
    String[] ext;
    FileSourcedConverter converter;
    int i;
    int n;

    result = new Hashtable<String, String>();

    for (i = 0; i < classnames.size(); i++) {
      classname = classnames.get(i);

      // all necessary interfaces implemented?
      for (n = 0; n < intf.length; n++) {
        if (!InheritanceUtils.hasInterface(intf[n], classname)) {
          continue;
        }
      }

      // get data from converter
      try {
        cls = WekaPackageClassLoaderManager.forName(classname);
        converter = (FileSourcedConverter) cls.newInstance();
        ext = converter.getFileExtensions();
      } catch (Exception e) {
        cls = null;
        converter = null;
        ext = new String[0];
      }

      if (converter == null) {
        continue;
      }

      for (n = 0; n < ext.length; n++) {
        result.put(ext[n], classname);
      }
    }

    return result;
  }
}
