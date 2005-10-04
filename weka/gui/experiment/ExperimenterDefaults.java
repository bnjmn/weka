/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 * ExperimenterDefaults.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.experiment;

import weka.core.Utils;

import java.io.File;
import java.io.Serializable;
import java.util.Properties;

/**
 * This class offers get methods for the default Experimenter settings in 
 * the props file <code>weka.gui.experiment.Experimenter.props</code>.
 *
 * @author  FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.1.2.3 $
 * @see #PROPERTY_FILE
 */
public class ExperimenterDefaults
  implements Serializable {
  
  /** The name of the properties file */
  public final static String PROPERTY_FILE = "weka/gui/experiment/Experimenter.props";

  /** Properties associated with the experimenter options */
  protected static Properties PROPERTIES;
  static {
    try {
      PROPERTIES = Utils.readProperties(PROPERTY_FILE);
    }
    catch (Exception e) {
      System.err.println("Problem reading properties. Fix before continuing.");
      e.printStackTrace();
      PROPERTIES = new Properties();
    }
  }

  /**
   * returns the value for the specified property, if non-existent then the
   * default value.
   *
   * @param property      the property to retrieve the value for
   * @param defaultValue  the default value for the property
   */
  public static String get(String property, String defaultValue) {
    return PROPERTIES.getProperty(property, defaultValue);
  }

  /**
   * returns the associated properties file
   */
  public final static Properties getProperties() {
    return PROPERTIES;
  }

  /**
   * returns the default experiment extension
   */
  public final static String getExtension() {
    return get("Extension", ".exp");
  }

  /**
   * returns the default destination
   */
  public final static String getDestination() {
    return get("Destination", "ARFF file");
  }

  /**
   * returns the default experiment type
   */
  public final static String getExperimentType() {
    return get("ExperimentType", "Cross-validation");
  }

  /**
   * whether classification or regression is used
   */
  public final static boolean getUseClassification() {
    return Boolean.valueOf(get("UseClassification", "true")).booleanValue();
  }

  /**
   * returns the number of folds used for cross-validation
   */
  public final static int getFolds() {
    return Integer.parseInt(get("Folds", "10"));
  }

  /**
   * returns the training percentage in case of splits
   */
  public final static double getTrainPercentage() {
    return Integer.parseInt(get("TrainPercentage", "66"));
  }

  /**
   * returns the number of repetitions to use
   */
  public final static int getRepetitions() {
    return Integer.parseInt(get("Repetitions", "10"));
  }

  /**
   * whether datasets or algorithms are iterated first
   */
  public final static boolean getDatasetsFirst() {
    return Boolean.valueOf(get("DatasetsFirst", "true")).booleanValue();
  }

  /**
   * returns the initial directory for the datasets (if empty, it returns
   * the user's home directory)
   */
  public final static File getInitialDatasetsDirectory() {
    String    dir;
    
    dir = get("InitialDatasetsDirectory", "");
    if (dir.equals(""))
      dir = System.getProperty("user.dir");

    return new File(dir);
  }

  /**
   * whether relative paths are used by default
   */
  public final static boolean getUseRelativePaths() {
    return Boolean.valueOf(get("UseRelativePaths", "false")).booleanValue();
  }

  /**
   * the comma-separated list of attribute names that identify a row
   */
  public final static String getRow() {
    return get("Row", "Key_Dataset");
  }

  /**
   * the comma-separated list of attribute names that identify a column
   */
  public final static String getColumn() {
    return get("Column", "Key_Scheme,Key_Scheme_options,Key_Scheme_version_ID");
  }

  /**
   * returns the name of the field used for comparison
   */
  public final static String getComparisonField() {
    return get("ComparisonField", "percent_correct");
  }

  /**
   * returns the default significance
   */
  public final static double getSignificance() {
    return Double.parseDouble(get("Significance", "0.05"));
  }

  /**
   * returns whether StdDevs are shown by default
   */
  public final static boolean getShowStdDevs() {
    return Boolean.valueOf(get("ShowStdDev", "false")).booleanValue();
  }

  /**
   * returns the default precision for the mean
   */
  public final static int getMeanPrecision() {
    return Integer.parseInt(get("MeanPrecision", "2"));
  }

  /**
   * returns the default precision for the stddevs
   */
  public final static int getStdDevPrecision() {
    return Integer.parseInt(get("StdDevPrecision", "2"));
  }
}

