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
import weka.clusterers.forOPTICSAndDBScan.OPTICS_GUI.OPTICS_Visualizer;
import weka.clusterers.forOPTICSAndDBScan.OPTICS_GUI.SERObject;
import weka.clusterers.forOPTICSAndDBScan.Utils.EpsilonRange_ListElement;
import weka.clusterers.forOPTICSAndDBScan.Utils.UpdateQueue;
import weka.clusterers.forOPTICSAndDBScan.Utils.UpdateQueueElement;
import weka.core.*;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.*;

/**
 * <p>
 * OPTICS.java <br/>
 * Authors: Rainer Holzmann, Zhanna Melnikova-Albrecht, Matthias Schubert <br/>
 * Date: Sep 5, 2004 <br/>
 * Time: 9:18:51 PM <br/>
 * $ Revision 1.4 $ <br/>
 * <br/><br/>
 * Reference: Ankerst M., Breunig M. M., Kriegel H.-P., Sander J.:<br>
 * OPTICS: Ordering Points To Identify the Clustering Structure <br>
 * Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD'99), Philadelphia, PA, 1999, pp. 49-60. <br>
 * </p>
 *
 * @author Matthias Schubert (schubert@dbs.ifi.lmu.de)
 * @author Zhanna Melnikova-Albrecht (melnikov@cip.ifi.lmu.de)
 * @author Rainer Holzmann (holzmann@cip.ifi.lmu.de)
 * @version $Revision: 1.2 $
 */
public class OPTICS extends Clusterer implements OptionHandler {

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
     * The database that is used for OPTICS
     */
    private Database database;

    /**
     * Holds the time-value (seconds) for the duration of the clustering-process
     */
    private double elapsedTime;

    /**
     * Flag that indicates if the results are written to a file or not
     */
    private boolean writeOPTICSresults = false;

    /**
     * Holds the ClusterOrder (dataObjects with their r_dist and c_dist) for the GUI
     */
    private FastVector resultVector;

    // *****************************************************************************************************************
    // constructors
    // *****************************************************************************************************************

    // *****************************************************************************************************************
    // methods
    // *****************************************************************************************************************

    /**
     * Generate Clustering via OPTICS
     * @param instances The instances that need to be clustered
     * @throws java.lang.Exception If clustering was not successful
     */
    public void buildClusterer(Instances instances) throws Exception {
        resultVector = new FastVector();
        long time_1 = System.currentTimeMillis();
        if (instances.checkForStringAttributes()) {
            throw new Exception("Can't handle string attributes!");
        }

        numberOfGeneratedClusters = 0;

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

        UpdateQueue seeds = new UpdateQueue();

        /** OPTICS-Begin */
        Iterator iterator = database.dataObjectIterator();
        while (iterator.hasNext()) {
            DataObject dataObject = (DataObject) iterator.next();
            if (!dataObject.isProcessed()) {
                expandClusterOrder(dataObject, seeds);
            }
        }

        long time_2 = System.currentTimeMillis();
        elapsedTime = (double) (time_2 - time_1) / 1000.0;

        if (writeOPTICSresults) {
            String fileName = "";
            GregorianCalendar gregorianCalendar = new GregorianCalendar();
            String timeStamp = gregorianCalendar.get(Calendar.DAY_OF_MONTH) + "-" +
                    (gregorianCalendar.get(Calendar.MONTH) + 1) +
                    "-" + gregorianCalendar.get(Calendar.YEAR) +
                    "--" + gregorianCalendar.get(Calendar.HOUR_OF_DAY) +
                    "-" + gregorianCalendar.get(Calendar.MINUTE) +
                    "-" + gregorianCalendar.get(Calendar.SECOND);
            fileName = "OPTICS_" + timeStamp + ".TXT";

            FileWriter fileWriter = new FileWriter(fileName);
            BufferedWriter bufferedOPTICSWriter = new BufferedWriter(fileWriter);
            for (int i = 0; i < resultVector.size(); i++) {
                bufferedOPTICSWriter.write(format_dataObject((DataObject) resultVector.elementAt(i)));
            }
            bufferedOPTICSWriter.flush();
            bufferedOPTICSWriter.close();
        }

        new OPTICS_Visualizer(getSERObject(), "OPTICS Visualizer - Main Window");
    }

    /**
     * Expands the ClusterOrder for this dataObject
     * @param dataObject Start-DataObject
     * @param seeds SeedList that stores dataObjects with reachability-distances
     */
    private void expandClusterOrder(DataObject dataObject, UpdateQueue seeds) {
        List list = database.coreDistance(getMinPoints(), getEpsilon(), dataObject);
        List epsilonRange_List = (List) list.get(1);
        dataObject.setReachabilityDistance(DataObject.UNDEFINED);
        dataObject.setCoreDistance(((Double) list.get(2)).doubleValue());
        dataObject.setProcessed(true);

        resultVector.addElement(dataObject);

        if (dataObject.getCoreDistance() != DataObject.UNDEFINED) {
            update(seeds, epsilonRange_List, dataObject);
            while (seeds.hasNext()) {
                UpdateQueueElement updateQueueElement = seeds.next();
                DataObject currentDataObject = (DataObject) updateQueueElement.getObject();
                currentDataObject.setReachabilityDistance(updateQueueElement.getPriority());
                List list_1 = database.coreDistance(getMinPoints(), getEpsilon(), currentDataObject);
                List epsilonRange_List_1 = (List) list_1.get(1);
                currentDataObject.setCoreDistance(((Double) list_1.get(2)).doubleValue());
                currentDataObject.setProcessed(true);

                resultVector.addElement(currentDataObject);

                if (currentDataObject.getCoreDistance() != DataObject.UNDEFINED) {
                    update(seeds, epsilonRange_List_1, currentDataObject);
                }
            }
        }
    }

    /**
     * Wraps the dataObject into a String, that contains the dataObject's key, the dataObject itself,
     * the coreDistance and its reachabilityDistance in a formatted manner.
     * @param dataObject The dataObject that is wrapped into a formatted string.
     * @return String Formatted string
     */
    private String format_dataObject(DataObject dataObject) {
        StringBuffer stringBuffer = new StringBuffer();

        stringBuffer.append("(" + Utils.doubleToString(Double.parseDouble(dataObject.getKey()),
                (Integer.toString(database.size()).length()), 0) + ".) "
                + Utils.padRight(dataObject.toString(), 40) + "  -->  c_dist: " +

                ((dataObject.getCoreDistance() == DataObject.UNDEFINED) ?
                Utils.padRight("UNDEFINED", 12) :
                Utils.padRight(Utils.doubleToString(dataObject.getCoreDistance(), 2, 3), 12)) +

                " r_dist: " +
                ((dataObject.getReachabilityDistance() == DataObject.UNDEFINED) ?
                Utils.padRight("UNDEFINED", 12) :
                Utils.doubleToString(dataObject.getReachabilityDistance(), 2, 3)) + "\n");

        return stringBuffer.toString();
    }

    /**
     * Updates reachability-distances in the Seeds-List
     * @param seeds UpdateQueue that holds DataObjects with their corresponding reachability-distances
     * @param epsilonRange_list List of DataObjects that were found in epsilon-range of centralObject
     * @param centralObject
     */
    private void update(UpdateQueue seeds, List epsilonRange_list, DataObject centralObject) {
        double coreDistance = centralObject.getCoreDistance();
        double new_r_dist = DataObject.UNDEFINED;

        for (int i = 0; i < epsilonRange_list.size(); i++) {
            EpsilonRange_ListElement listElement = (EpsilonRange_ListElement) epsilonRange_list.get(i);
            DataObject neighbourhood_object = listElement.getDataObject();
            if (!neighbourhood_object.isProcessed()) {
                new_r_dist = Math.max(coreDistance, listElement.getDistance());
                seeds.add(new_r_dist, neighbourhood_object, neighbourhood_object.getKey());
            }
        }
    }

    /**
     * Classifies a given instance.
     *
     * @param instance The instance to be assigned to a cluster
     * @return int The number of the assigned cluster as an integer
     * @exception java.lang.Exception If instance could not be clustered
     * successfully
     */
    public int clusterInstance(Instance instance) throws Exception {
        throw new Exception();
    }

    /**
     * Returns the number of clusters.
     *
     * @return int The number of clusters generated for a training dataset.
     * @exception java.lang.Exception If number of clusters could not be returned
     * successfully
     */
    public int numberOfClusters() throws Exception {
        return numberOfGeneratedClusters;
    }

    /**
     * Returns an enumeration of all the available options.
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
                new Option("\tindex (database) used for OPTICS (default = weka.clusterers.forOPTICSAndDBScan.Databases.SequentialDatabase)",
                        "I",
                        1,
                        "-I <String>"));
        vector.addElement(
                new Option("\tdistance-type (default = weka.clusterers.forOPTICSAndDBScan.DataObjects.EuclidianDataObject)",
                        "D",
                        1,
                        "-D <String>"));
        vector.addElement(
                new Option("\twrite results to OPTICS_#TimeStamp#.TXT - File",
                        "F",
                        0,
                        "-F"));
        return vector.elements();
    }

    /**
     * Sets the OptionHandler's options using the given list. All options
     * will be set (or reset) during this call (i.e. incremental setting
     * of options is not possible).
     *
     * @param options The list of options as an array of strings
     * @exception java.lang.Exception If an option is not supported
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

        setWriteOPTICSresults(Utils.getFlag('F', options));
    }

    /**
     * Gets the current option settings for the OptionHandler.
     *
     * @return String[] The list of current option settings as an array of strings
     */
    public String[] getOptions() {
        String[] options = new String[9];
        int current = 0;

        options[current++] = "-E";
        options[current++] = "" + getEpsilon();
        options[current++] = "-M";
        options[current++] = "" + getMinPoints();
        options[current++] = "-I";
        options[current++] = "" + getDatabase_Type();
        options[current++] = "-D";
        options[current++] = "" + getDatabase_distanceType();

        if (writeOPTICSresults) {
            options[current++] = "-F";
        }

        while (current < options.length) {
            options[current++] = "";
        }

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
     * Returns the flag for writing actions
     * @return writeOPTICSresults (flag)
     */
    public boolean getWriteOPTICSresults() {
        return writeOPTICSresults;
    }

    /**
     * Sets the flag for writing actions
     * @param writeOPTICSresults Results are written to a file if the flag is set
     */
    public void setWriteOPTICSresults(boolean writeOPTICSresults) {
        this.writeOPTICSresults = writeOPTICSresults;
    }

    /**
     * Returns the resultVector
     * @return resultVector
     */
    public FastVector getResultVector() {
        return resultVector;
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
     * Returns the tip text for this property
     * @return tip text for this property suitable for
     * displaying in the explorer/experimenter gui
     */
    public String writeOPTICSresultsTipText() {
        return "if the -F option is set, the results are written to OPTICS_#TimeStamp#.TXT";
    }

    /**
     * Returns a string describing this DataMining-Algorithm
     * @return String Information for the gui-explorer
     */
    public String globalInfo() {
        return " Ankerst M., Breunig M. M., Kriegel H.-P., Sander J.: OPTICS: Ordering Points "+
        "To Identify the Clustering Structure, Proc. ACM SIGMOD Int. Conf. on Management of Data"+
        " (SIGMOD'99), Philadelphia, PA, 1999, pp. 49-60.";
    }

    /**
     * Returns the internal database
     */
    public SERObject getSERObject() {
        SERObject serObject = new SERObject(resultVector,
                database.size(),
                database.getInstances().numAttributes(),
                getEpsilon(),
                getMinPoints(),
                writeOPTICSresults,
                getDatabase_Type(),
                getDatabase_distanceType(),
                numberOfGeneratedClusters,
                Utils.doubleToString(elapsedTime, 3, 3));
        return serObject;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("OPTICS clustering results\n" +
                "============================================================================================\n\n");
        stringBuffer.append("Clustered DataObjects: " + database.size() + "\n");
        stringBuffer.append("Number of attributes: " + database.getInstances().numAttributes() + "\n");
        stringBuffer.append("Epsilon: " + getEpsilon() + "; minPoints: " + getMinPoints() + "\n");
        stringBuffer.append("Write results to file: " + (writeOPTICSresults ? "yes" : "no") + "\n");
        stringBuffer.append("Index: " + getDatabase_Type() + "\n");
        stringBuffer.append("Distance-type: " + getDatabase_distanceType() + "\n");
        stringBuffer.append("Number of generated clusters: " + numberOfGeneratedClusters + "\n");
        DecimalFormat decimalFormat = new DecimalFormat(".##");
        stringBuffer.append("Elapsed time: " + decimalFormat.format(elapsedTime) + "\n\n");

        for (int i = 0; i < resultVector.size(); i++) {
            stringBuffer.append(format_dataObject((DataObject) resultVector.elementAt(i)));
        }
        return stringBuffer.toString() + "\n";
    }

    /**
     * Main Method for testing OPTICS
     * @param args Valid parameters are: 'E' epsilon (default = 0.9); 'M' minPoints (default = 6);
     *                                   'I' index-type (default = weka.clusterers.forOPTICSAndDBScan.Databases.SequentialDatabase);
     *                                   'D' distance-type (default = weka.clusterers.forOPTICSAndDBScan.DataObjects.EuclidianDataObject);
     *                                   'F' write results to OPTICS_#TimeStamp#.TXT - File
     */
    public static void main(String[] args) {
        try {
            System.out.println(ClusterEvaluation.evaluateClusterer(new OPTICS(), args));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // *****************************************************************************************************************
    // inner classes
    // *****************************************************************************************************************

}
