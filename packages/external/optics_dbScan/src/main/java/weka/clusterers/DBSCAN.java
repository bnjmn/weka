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
 *    Copyright (C) 2004
 *    & Matthias Schubert (schubert@dbs.ifi.lmu.de)
 *    & Zhanna Melnikova-Albrecht (melnikov@cip.ifi.lmu.de)
 *    & Rainer Holzmann (holzmann@cip.ifi.lmu.de)
 */

package weka.clusterers;

import weka.clusterers.forOPTICSAndDBScan.DataObjects.DataObject;
import weka.clusterers.forOPTICSAndDBScan.Databases.Database;
import weka.core.DistanceFunction;
import weka.core.EuclideanDistance;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
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
 * Basic implementation of DBSCAN clustering algorithm that should *not* be used as a reference for runtime benchmarks: more sophisticated implementations exist! Clustering of new instances is not supported. More info:<br/>
 * <br/>
 *  Martin Ester, Hans-Peter Kriegel, Joerg Sander, Xiaowei Xu: A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases with Noise. In: Second International Conference on Knowledge Discovery and Data Mining, 226-231, 1996.
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
 *  index (database) used for DBSCAN (default = weka.clusterers.forOPTICSAndDBScan.Databases.SequentialDatabase)</pre>
 * 
 * <pre> -D &lt;String&gt;
 *  distance-type (default = weka.clusterers.forOPTICSAndDBScan.DataObjects.EuclideanDataObject)</pre>
 * 
 <!-- options-end -->
 *
 * @author Matthias Schubert (schubert@dbs.ifi.lmu.de)
 * @author Zhanna Melnikova-Albrecht (melnikov@cip.ifi.lmu.de)
 * @author Rainer Holzmann (holzmann@cip.ifi.lmu.de)
 * @version $Revision: 8108 $
 */
public class DBSCAN 
    extends AbstractClusterer 
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

  /** the distance function used. */
  private DistanceFunction m_DistanceFunction = new EuclideanDistance();

    /**
     * The database that is used for DBSCAN
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
      result.disableAll();
      result.enable(Capability.NO_CLASS);

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
     * Generate Clustering via DBSCAN
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

        
        database = new Database(getDistanceFunction(), filteredInstances);
        for (int i = 0; i < database.getInstances().numInstances(); i++) {
          DataObject dataObject = new DataObject(
                    database.getInstances().instance(i),
                    Integer.toString(i),
                    database);
            database.insert(dataObject);
        }

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
        vector.add(new Option("\tDistance function to use.\n"
                              + "\t(default: weka.core.EuclideanDistance)", "A", 1,
                              "-A <classname and options>"));
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
     *  index (database) used for DBSCAN (default = weka.clusterers.forOPTICSAndDBScan.Databases.SequentialDatabase)</pre>
     * 
     * <pre> -D &lt;String&gt;
     *  distance-type (default = weka.clusterers.forOPTICSAndDBScan.DataObjects.EuclideanDataObject)</pre>
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

        optionString = Utils.getOption('A', options);
        if (optionString.length() != 0) {
          String distSpec[] = Utils.splitOptions(optionString);
          if (distSpec.length == 0) {
            throw new Exception("Invalid DistanceFunction specification string.");
          }
          String className = distSpec[0];
          distSpec[0] = "";
          
          setDistanceFunction((DistanceFunction) Utils.forName(
                                                               DistanceFunction.class, className, distSpec));
        } else {
          setDistanceFunction(new EuclideanDistance());
        }
    }

    /**
     * Gets the current option settings for the OptionHandler.
     *
     * @return String[] The list of current option settings as an array of strings
     */
    public String[] getOptions() {
      Vector<String> result;
      
      result = new Vector<String>();
      
      result.add("-E");
      result.add("" + getEpsilon());
      result.add("-M");
      result.add("" + getMinPoints());
      
      result.add("-A");
      result.add((m_DistanceFunction.getClass().getName() + " " + Utils
                  .joinOptions(m_DistanceFunction.getOptions())).trim());
      
      return result.toArray(new String[result.size()]);
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
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String distanceFunctionTipText() {
    return "The distance function to use for finding neighbours "
      + "(default: weka.core.EuclideanDistance). ";
  }

  /**
   * returns the distance function currently in use.
   * 
   * @return the distance function
   */
  public DistanceFunction getDistanceFunction() {
    return m_DistanceFunction;
  }

  /**
   * sets the distance function to use for nearest neighbour search.
   * 
   * @param df the new distance function to use
   * @throws Exception if instances cannot be processed
   */
  public void setDistanceFunction(DistanceFunction df) throws Exception {
    m_DistanceFunction = df;
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
     * Returns a string describing this DataMining-Algorithm
     * @return String Information for the gui-explorer
     */
    public String globalInfo() {
        return "Basic implementation of DBSCAN clustering algorithm that should "
          + "*not* be used as a reference for runtime benchmarks: more sophisticated "
          + "implementations exist! Clustering of new instances is not supported. More info:\n\n "
          + getTechnicalInformation().toString();
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
        stringBuffer.append("DBSCAN clustering results\n" +
                "========================================================================================\n\n");
        stringBuffer.append("Clustered DataObjects: " + database.size() + "\n");
        stringBuffer.append("Number of attributes: " + database.getInstances().numAttributes() + "\n");
        stringBuffer.append("Epsilon: " + getEpsilon() + "; minPoints: " + getMinPoints() + "\n");
        stringBuffer.append("Distance-type: " + getDistanceFunction() + "\n");
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
     * Returns the revision string.
     * 
     * @return		the revision
     */
    public String getRevision() {
      return RevisionUtils.extract("$Revision: 8108 $");
    }

    /**
     * Main Method for testing DBSCAN
     * @param args Valid parameters are: 'E' epsilon (default = 0.9); 'M' minPoints (default = 6);
     *                                   'I' index-type (default = weka.clusterers.forOPTICSAndDBScan.Databases.SequentialDatabase);
     *                                   'D' distance-type (default = weka.clusterers.forOPTICSAndDBScan.DataObjects.EuclideanDataObject);
     */
    public static void main(String[] args) {
        runClusterer(new DBSCAN(), args);
    }
}

