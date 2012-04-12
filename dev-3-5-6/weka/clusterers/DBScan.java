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
 *    Copyright (C) 2004
 *    & Matthias Schubert (schubert@dbs.ifi.lmu.de)
 *    & Zhanna Melnikova-Albrecht (melnikov@cip.ifi.lmu.de)
 *    & Rainer Holzmann (holzmann@cip.ifi.lmu.de)
 */

package weka.clusterers;

import weka.clusterers.forOPTICSAndDBScan.DataObjects.DataObject;
import weka.clusterers.forOPTICSAndDBScan.Databases.Database;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * Martin Ester, Hans-Peter Kriegel, Joerg Sander, Xiaowei Xu: A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases with Noise. In: Second International Conference on Knowledge Discovery and Data Mining, 226-231, 1996.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;inproceedings{Ester1996,
 *    author = {Martin Ester and Hans-Peter Kriegel and Joerg Sander and Xiaowei Xu},
 *    booktitle = {Second International Conference on Knowledge Discovery and Data Mining},
 *    editor = {Evangelos Simoudis and Jiawei Han and Usama M. Fayyad},
 *    pages = {226-231},
 *    publisher = {AAAI Press},
 *    title = {A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases with Noise},
 *    year = {1996}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -E &lt;double&gt;
 *  epsilon (default = 0.9)</pre>
 * 
 * <pre> -M &lt;int&gt;
 *  minPoints (default = 6)</pre>
 * 
 * <pre> -I &lt;String&gt;
 *  index (database) used for DBScan (default = weka.clusterers.forOPTICSAndDBScan.Databases.SequentialDatabase)</pre>
 * 
 * <pre> -D &lt;String&gt;
 *  distance-type (default = weka.clusterers.forOPTICSAndDBScan.DataObjects.EuclidianDataObject)</pre>
 * 
 <!-- options-end -->
 *
 * @author Matthias Schubert (schubert@dbs.ifi.lmu.de)
 * @author Zhanna Melnikova-Albrecht (melnikov@cip.ifi.lmu.de)
 * @author Rainer Holzmann (holzmann@cip.ifi.lmu.de)
 * @version $Revision: 1.7 $
 */
public class DBScan 
    extends Clusterer 
    implements OptionHandler, TechnicalInformationHandler {

    /** for serialization */
    static final long serialVersionUID = -1666498248451219728L;
  
    /**
     * Specifies the radius for a range-query
     */
    private double epsilon = 0.9;

    /**
     * Specifies the density (the range-query must contain at least minPoints DataObjects)
     */
    private int minPoints = 6;

    /**
     * Replace missing values in training instances
     */
    private ReplaceMissingValues replaceMissingValues_Filter;

    /**
     * Holds the number of clusters generated
     */
    private int numberOfGeneratedClusters;

    /**
     * Holds the distance-type that is used
     * (default = weka.clusterers.forOPTICSAndDBScan.DataObjects.EuclidianDataObject)
     */
    private String database_distanceType = "weka.clusterers.forOPTICSAndDBScan.DataObjects.EuclidianDataObject";

    /**
     * Holds the type of the used database
     * (default = weka.clusterers.forOPTICSAndDBScan.Databases.SequentialDatabase)
     */
    private String database_Type = "weka.clusterers.forOPTICSAndDBScan.Databases.SequentialDatabase";

    /**
     * The database that is used for DBScan
     */
    private Database database;

    /**
     * Holds the current clusterID
     */
    private int clusterID;

    /**
     * Counter for the processed instances
     */
    private int processed_InstanceID;

    /**
     * Holds the time-value (seconds) for the duration of the clustering-process
     */
    private double elapsedTime;

    /**
     * Returns default capabilities of the clusterer.
     *
     * @return      the capabilities of this clusterer
     */
    public Capabilities getCapabilities() {
      Capabilities result = super.getCapabilities();

      // attributes
      result.enable(Capability.NOMINAL_ATTRIBUTES);
      result.enable(Capability.NUMERIC_ATTRIBUTES);
      result.enable(Capability.DATE_ATTRIBUTES);
      result.enable(Capability.MISSING_VALUES);

      return result;
    }

    // *****************************************************************************************************************
    // constructors
    // *****************************************************************************************************************

    // *****************************************************************************************************************
    // methods
    // *****************************************************************************************************************

    /**
     * Generate Clustering via DBScan
     * @param instances The instances that need to be clustered
     * @throws java.lang.Exception If clustering was not successful
     */
    public void buildClusterer(Instances instances) throws Exception {
        // can clusterer handle the data?
        getCapabilities().testWithFail(instances);

        long time_1 = System.currentTimeMillis();

        processed_InstanceID = 0;
        numberOfGeneratedClusters = 0;
        clusterID = 0;

        replaceMissingValues_Filter = new ReplaceMissingValues();
        replaceMissingValues_Filter.setInputFormat(instances);
        Instances filteredInstances = Filter.useFilter(instances, replaceMissingValues_Filter);

        database = databaseForName(getDatabase_Type(), filteredInstances);
        for (int i = 0; i < database.getInstances().numInstances(); i++) {
            DataObject dataObject = dataObjectForName(getDatabase_distanceType(),
                    database.getInstances().instance(i),
                    Integer.toString(i),
                    database);
            database.insert(dataObject);
        }
        database.setMinMaxValues();

        Iterator iterator = database.dataObjectIterator();
        while (iterator.hasNext()) {
            DataObject dataObject = (DataObject) iterator.next();
            if (dataObject.getClusterLabel() == DataObject.UNCLASSIFIED) {
                if (expandCluster(dataObject)) {
                    clusterID++;
                    numberOfGeneratedClusters++;
                }
            }
        }

        long time_2 = System.currentTimeMillis();
        elapsedTime = (double) (time_2 - time_1) / 1000.0;
    }

    /**
     * Assigns this dataObject to a cluster or remains it as NOISE
     * @param dataObject The DataObject that needs to be assigned
     * @return true, if the DataObject could be assigned, else false
     */
    private boolean expandCluster(DataObject dataObject) {
        List seedList = database.epsilonRangeQuery(getEpsilon(), dataObject);
        /** dataObject is NO coreObject */
        if (seedList.size() < getMinPoints()) {
            dataObject.setClusterLabel(DataObject.NOISE);
            return false;
        }

        /** dataObject is coreObject */
        for (int i = 0; i < seedList.size(); i++) {
            DataObject seedListDataObject = (DataObject) seedList.get(i);
            /** label this seedListDataObject with the current clusterID, because it is in epsilon-range */
            seedListDataObject.setClusterLabel(clusterID);
            if (seedListDataObject.equals(dataObject)) {
                seedList.remove(i);
                i--;
            }
        }

        /** Iterate the seedList of the startDataObject */
        for (int j = 0; j < seedList.size(); j++) {
            DataObject seedListDataObject = (DataObject) seedList.get(j);
            List seedListDataObject_Neighbourhood = database.epsilonRangeQuery(getEpsilon(), seedListDataObject);

            /** seedListDataObject is coreObject */
            if (seedListDataObject_Neighbourhood.size() >= getMinPoints()) {
                for (int i = 0; i < seedListDataObject_Neighbourhood.size(); i++) {
                    DataObject p = (DataObject) seedListDataObject_Neighbourhood.get(i);
                    if (p.getClusterLabel() == DataObject.UNCLASSIFIED || p.getClusterLabel() == DataObject.NOISE) {
                        if (p.getClusterLabel() == DataObject.UNCLASSIFIED) {
                            seedList.add(p);
                        }
                        p.setClusterLabel(clusterID);
                    }
                }
            }
            seedList.remove(j);
            j--;
        }

        return true;
    }

    /**
     * Classifies a given instance.
     *
     * @param instance The instance to be assigned to a cluster
     * @return int The number of the assigned cluster as an integer
     * @throws java.lang.Exception If instance could not be clustered
     * successfully
     */
    public int clusterInstance(Instance instance) throws Exception {
        if (processed_InstanceID >= database.size()) processed_InstanceID = 0;
        int cnum = (database.getDataObject(Integer.toString(processed_InstanceID++))).getClusterLabel();
        if (cnum == DataObject.NOISE)
            throw new Exception();
        else
            return cnum;
    }

    /**
     * Returns the number of clusters.
     *
     * @return int The number of clusters generated for a training dataset.
     * @throws java.lang.Exception if number of clusters could not be returned
     * successfully
     */
    public int numberOfClusters() throws Exception {
        return numberOfGeneratedClusters;
    }

    /**
     * Returns an enumeration of all the available options..
     *
     * @return Enumeration An enumeration of all available options.
     */
    public Enumeration listOptions() {
        Vector vector = new Vector();

        vector.addElement(
                new Option("\tepsilon (default = 0.9)",
                        "E",
                        1,
                        "-E <double>"));
        vector.addElement(
                new Option("\tminPoints (default = 6)",
                        "M",
                        1,
                        "-M <int>"));
        vector.addElement(
                new Option("\tindex (database) used for DBScan (default = weka.clusterers.forOPTICSAndDBScan.Databases.SequentialDatabase)",
                        "I",
                        1,
                        "-I <String>"));
        vector.addElement(
                new Option("\tdistance-type (default = weka.clusterers.forOPTICSAndDBScan.DataObjects.EuclidianDataObject)",
                        "D",
                        1,
                        "-D <String>"));
        return vector.elements();
    }

    /**
     * Sets the OptionHandler's options using the given list. All options
     * will be set (or reset) during this call (i.e. incremental setting
     * of options is not possible). <p/>
     *
     <!-- options-start -->
     * Valid options are: <p/>
     * 
     * <pre> -E &lt;double&gt;
     *  epsilon (default = 0.9)</pre>
     * 
     * <pre> -M &lt;int&gt;
     *  minPoints (default = 6)</pre>
     * 
     * <pre> -I &lt;String&gt;
     *  index (database) used for DBScan (default = weka.clusterers.forOPTICSAndDBScan.Databases.SequentialDatabase)</pre>
     * 
     * <pre> -D &lt;String&gt;
     *  distance-type (default = weka.clusterers.forOPTICSAndDBScan.DataObjects.EuclidianDataObject)</pre>
     * 
     <!-- options-end -->
     *
     * @param options The list of options as an array of strings
     * @throws java.lang.Exception If an option is not supported
     */
    public void setOptions(String[] options) throws Exception {
        String optionString = Utils.getOption('E', options);
        if (optionString.length() != 0) {
            setEpsilon(Double.parseDouble(optionString));
        }

        optionString = Utils.getOption('M', options);
        if (optionString.length() != 0) {
            setMinPoints(Integer.parseInt(optionString));
        }

        optionString = Utils.getOption('I', options);
        if (optionString.length() != 0) {
            setDatabase_Type(optionString);
        }

        optionString = Utils.getOption('D', options);
        if (optionString.length() != 0) {
            setDatabase_distanceType(optionString);
        }
    }

    /**
     * Gets the current option settings for the OptionHandler.
     *
     * @return String[] The list of current option settings as an array of strings
     */
    public String[] getOptions() {
        String[] options = new String[8];
        int current = 0;

        options[current++] = "-E";
        options[current++] = "" + getEpsilon();
        options[current++] = "-M";
        options[current++] = "" + getMinPoints();
        options[current++] = "-I";
        options[current++] = "" + getDatabase_Type();
        options[current++] = "-D";
        options[current++] = "" + getDatabase_distanceType();

        return options;
    }

    /**
     * Returns a new Class-Instance of the specified database
     * @param database_Type String of the specified database
     * @param instances Instances that were delivered from WEKA
     * @return Database New constructed Database
     */
    public Database databaseForName(String database_Type, Instances instances) {
        Object o = null;

        Constructor co = null;
        try {
            co = (Class.forName(database_Type)).getConstructor(new Class[]{Instances.class});
            o = co.newInstance(new Object[]{instances});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return (Database) o;
    }

    /**
     * Returns a new Class-Instance of the specified database
     * @param database_distanceType String of the specified distance-type
     * @param instance The original instance that needs to hold by this DataObject
     * @param key Key for this DataObject
     * @param database Link to the database
     * @return DataObject New constructed DataObject
     */
    public DataObject dataObjectForName(String database_distanceType, Instance instance, String key, Database database) {
        Object o = null;

        Constructor co = null;
        try {
            co = (Class.forName(database_distanceType)).
                    getConstructor(new Class[]{Instance.class, String.class, Database.class});
            o = co.newInstance(new Object[]{instance, key, database});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return (DataObject) o;
    }

    /**
     * Sets a new value for minPoints
     * @param minPoints MinPoints
     */
    public void setMinPoints(int minPoints) {
        this.minPoints = minPoints;
    }

    /**
     * Sets a new value for epsilon
     * @param epsilon Epsilon
     */
    public void setEpsilon(double epsilon) {
        this.epsilon = epsilon;
    }

    /**
     * Returns the value of epsilon
     * @return double Epsilon
     */
    public double getEpsilon() {
        return epsilon;
    }

    /**
     * Returns the value of minPoints
     * @return int MinPoints
     */
    public int getMinPoints() {
        return minPoints;
    }

    /**
     * Returns the distance-type
     * @return String Distance-type
     */
    public String getDatabase_distanceType() {
        return database_distanceType;
    }

    /**
     * Returns the type of the used index (database)
     * @return String Index-type
     */
    public String getDatabase_Type() {
        return database_Type;
    }

    /**
     * Sets a new distance-type
     * @param database_distanceType The new distance-type
     */
    public void setDatabase_distanceType(String database_distanceType) {
        this.database_distanceType = database_distanceType;
    }

    /**
     * Sets a new database-type
     * @param database_Type The new database-type
     */
    public void setDatabase_Type(String database_Type) {
        this.database_Type = database_Type;
    }

    /**
     * Returns the tip text for this property
     * @return tip text for this property suitable for
     * displaying in the explorer/experimenter gui
     */
    public String epsilonTipText() {
        return "radius of the epsilon-range-queries";
    }

    /**
     * Returns the tip text for this property
     * @return tip text for this property suitable for
     * displaying in the explorer/experimenter gui
     */
    public String minPointsTipText() {
        return "minimun number of DataObjects required in an epsilon-range-query";
    }

    /**
     * Returns the tip text for this property
     * @return tip text for this property suitable for
     * displaying in the explorer/experimenter gui
     */
    public String database_TypeTipText() {
        return "used database";
    }

    /**
     * Returns the tip text for this property
     * @return tip text for this property suitable for
     * displaying in the explorer/experimenter gui
     */
    public String database_distanceTypeTipText() {
        return "used distance-type";
    }

    /**
     * Returns a string describing this DataMining-Algorithm
     * @return String Information for the gui-explorer
     */
    public String globalInfo() {
        return getTechnicalInformation().toString();
    }

    /**
     * Returns an instance of a TechnicalInformation object, containing 
     * detailed information about the technical background of this class,
     * e.g., paper reference or book this class is based on.
     * 
     * @return the technical information about this class
     */
    public TechnicalInformation getTechnicalInformation() {
      TechnicalInformation 	result;
      
      result = new TechnicalInformation(Type.INPROCEEDINGS);
      result.setValue(Field.AUTHOR, "Martin Ester and Hans-Peter Kriegel and Joerg Sander and Xiaowei Xu");
      result.setValue(Field.TITLE, "A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases with Noise");
      result.setValue(Field.BOOKTITLE, "Second International Conference on Knowledge Discovery and Data Mining");
      result.setValue(Field.EDITOR, "Evangelos Simoudis and Jiawei Han and Usama M. Fayyad");
      result.setValue(Field.YEAR, "1996");
      result.setValue(Field.PAGES, "226-231");
      result.setValue(Field.PUBLISHER, "AAAI Press");
      
      return result;
    }

    /**
     * Returns a description of the clusterer
     * 
     * @return a string representation of the clusterer
     */
    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("DBScan clustering results\n" +
                "========================================================================================\n\n");
        stringBuffer.append("Clustered DataObjects: " + database.size() + "\n");
        stringBuffer.append("Number of attributes: " + database.getInstances().numAttributes() + "\n");
        stringBuffer.append("Epsilon: " + getEpsilon() + "; minPoints: " + getMinPoints() + "\n");
        stringBuffer.append("Index: " + getDatabase_Type() + "\n");
        stringBuffer.append("Distance-type: " + getDatabase_distanceType() + "\n");
        stringBuffer.append("Number of generated clusters: " + numberOfGeneratedClusters + "\n");
        DecimalFormat decimalFormat = new DecimalFormat(".##");
        stringBuffer.append("Elapsed time: " + decimalFormat.format(elapsedTime) + "\n\n");

        for (int i = 0; i < database.size(); i++) {
            DataObject dataObject = database.getDataObject(Integer.toString(i));
            stringBuffer.append("(" + Utils.doubleToString(Double.parseDouble(dataObject.getKey()),
                    (Integer.toString(database.size()).length()), 0) + ".) "
                    + Utils.padRight(dataObject.toString(), 69) + "  -->  " +
                    ((dataObject.getClusterLabel() == DataObject.NOISE) ?
                    "NOISE\n" : dataObject.getClusterLabel() + "\n"));
        }
        return stringBuffer.toString() + "\n";
    }

    /**
     * Main Method for testing DBScan
     * @param args Valid parameters are: 'E' epsilon (default = 0.9); 'M' minPoints (default = 6);
     *                                   'I' index-type (default = weka.clusterers.forOPTICSAndDBScan.Databases.SequentialDatabase);
     *                                   'D' distance-type (default = weka.clusterers.forOPTICSAndDBScan.DataObjects.EuclidianDataObject);
     */
    public static void main(String[] args) {
        runClusterer(new DBScan(), args);
    }

    // *****************************************************************************************************************
    // inner classes
    // *****************************************************************************************************************

}

