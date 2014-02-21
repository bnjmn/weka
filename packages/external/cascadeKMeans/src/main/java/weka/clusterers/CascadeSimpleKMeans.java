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
 *    CascadeSimpleKMeans.java
 *    Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.clusterers;

import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;
import java.util.ArrayList;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import weka.clusterers.Clusterer;
import weka.clusterers.RandomizableClusterer;
import weka.clusterers.SimpleKMeans;
import weka.core.Capabilities;
import weka.core.DistanceFunction;
import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.core.Option;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;

/**
 * cascade simple k means, selects the best k according to calinski-harabasz criterion
 * 
 * analogous to: http://cc.oulu.fi/~jarioksa/softhelp/vegan/html/cascadeKM.html
 * 
 * see Calinski, T. and J. Harabasz. 1974. A dendrite method for cluster analysis. Commun. Stat. 3: 1-27.
 * quoted in German: http://books.google.com/books?id=-f9Ox0p1-D4C&lpg=PA394&ots=SV3JfRIkQn&dq=Calinski%20and%20Harabasz&hl=de&pg=PA394#v=onepage&q&f=false
 * 
 * @author Martin Gütlein (martin.guetlein@gmail.com)
 */
public class CascadeSimpleKMeans extends RandomizableClusterer implements Clusterer,
                                                                          TechnicalInformationHandler {

  static final long serialVersionUID = -227184458402639337L;

  protected int minNumClusters = 2;
  protected int maxNumClusters = 10;
  protected int restarts = 10;
  protected boolean printDebug = true;
  protected DistanceFunction distanceFunction = new EuclideanDistance();
  protected int maxIterations = 500;
  protected boolean manuallySelectNumClusters = false;
  protected boolean initializeWithKMeansPlusPlus = false;

  protected SimpleKMeans kMeans = new SimpleKMeans();
  protected Instance meanInstance;
  protected int numInstances;
  protected DecimalFormat df = new DecimalFormat("#.##");

  protected int finalBestK = -1;
  protected int finalBestSeed = -1;
  protected String finalMeanCH;

  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation result;
    
    result = new TechnicalInformation(Type.ARTICLE);
    result.setValue(Field.AUTHOR, "T. Calinski and J. Harabasz");
    result.setValue(Field.TITLE, "A dendrite method for cluster analysis");
    result.setValue(Field.BOOKTITLE, "Communications in Statistics");
    result.setValue(Field.VOLUME, "3");
    result.setValue(Field.NUMBER, "1");
    result.setValue(Field.YEAR, "1974");
    result.setValue(Field.PAGES, "1-27");
    
    return result;
  }

  protected void reset() {
    finalBestK = -1;
    finalBestSeed = -1;
    finalMeanCH = "";
  }

  public String toString() {
    if (finalBestK == -1) {
      return "CascadeSimpleKMeans has not been build yet!";
    }

    StringBuffer buff = new StringBuffer();
    buff.append("CascadeSimpleKMeans:\n\n");
    buff.append(finalMeanCH);

    buff.append("cascade> k (yields highest mean CH): " + finalBestK);
    buff.append("\n\ncascade> seed (highest CH for k=" + finalBestK + ") : " 
                + finalBestSeed + "\n\n");

    return buff.toString();
  }

  /**
   * Returns a string describing this clusterer.
   * @return a description of the evaluator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Cascade simple k means, selects the best k according to calinski-harabasz criterion."
      + " For more information see:\n\n" 
      + getTechnicalInformation().toString();
  }

  @Override
    public void buildClusterer(Instances data) throws Exception
  {
    reset();
    meanInstance = new DenseInstance(data.numAttributes());
    for (int i = 0; i < data.numAttributes(); i++)
      meanInstance.setValue(i, data.meanOrMode(i));
    numInstances = data.numInstances();

    kMeans.setDistanceFunction(distanceFunction);
    kMeans.setMaxIterations(maxIterations);
    //    kMeans.setInitializeUsingKMeansPlusPlusMethod(initializeWithKMeansPlusPlus);
    if (initializeWithKMeansPlusPlus) {
      kMeans.setInitializationMethod(new weka.core.SelectedTag(SimpleKMeans.KMEANS_PLUS_PLUS, SimpleKMeans.TAGS_SELECTION));
    }

    /**
     * step 1: iterate over all restarts and possible k values, record CH-scores
     */
    Random r = new Random(m_Seed);
    double meanCHs[] = new double[maxNumClusters + 1 - minNumClusters];
    double maxCHs[] = new double[maxNumClusters + 1 - minNumClusters];
    int maxSeed[] = new int[maxNumClusters + 1 - minNumClusters];

    for (int i = 0; i < restarts; i++)
      {
        if (printDebug)
          System.out.println("cascade> restarts: " + (i + 1) + " / " + restarts);

        for (int k = minNumClusters; k <= maxNumClusters; k++)
          {
            if (printDebug)
              System.out.print("cascade>  k:" + k + " ");

            int seed = r.nextInt();
            kMeans.setSeed(seed);
            kMeans.setNumClusters(k);
            kMeans.buildClusterer(data);
            double ch = getCalinskiHarabasz();

            int index = k - minNumClusters;
            meanCHs[index] = (meanCHs[index] * i + ch) / (double) (i + 1);
            if (i == 0 || ch > maxCHs[index])
              {
                maxCHs[index] = ch;
                maxSeed[index] = seed;
              }

            if (printDebug)
              System.out.println(" CH:" + df.format(ch) + "  W:"
                                 + df.format(kMeans.getSquaredError() / (double) (numInstances - kMeans.getNumClusters()))
                                 + " (unweighted:" + df.format(kMeans.getSquaredError()) + ")  B:"
                                 + df.format(getSquaredErrorBetweenClusters() / (double) (kMeans.getNumClusters() - 1))
                                 + " (unweighted:" + df.format(getSquaredErrorBetweenClusters()) + ") ");
          }
      }
    if (printDebug)
      {
        String s = "cascade> max CH: [ ";
        for (int i = 0; i < maxSeed.length; i++)
          s += df.format(maxCHs[i]) + " ";
        System.out.println(s + "]");
      }
    String s = "cascade> mean CH: [ ";
    for (int i = 0; i < maxSeed.length; i++)
      s += df.format(meanCHs[i]) + " ";

    finalMeanCH = s + "]";
    //    System.out.println(s + "]");

    /**
     * step 2: select k with best mean CH-score; select seed for max CH score for this k
     */
    int bestK = -1;
    double maxCH = -1;
    for (int k = minNumClusters; k <= maxNumClusters; k++)
      {
        int index = k - minNumClusters;
        if (bestK == -1 || meanCHs[index] > maxCH)
          {
            maxCH = meanCHs[index];
            bestK = k;
          }
      }
    if (manuallySelectNumClusters)
      {
        int selectedK = selectKManually(meanCHs, bestK);
        if (selectedK != -1)
          bestK = selectedK;
      }
    int bestSeed = maxSeed[bestK - minNumClusters];

    finalBestK = bestK;
    finalBestSeed = bestSeed;
    //    System.out.println("cascade> k (yields highest mean CH): " + bestK);
    //    System.out.println("cascade> seed (highest CH for k=" + bestK + ") : " + bestSeed);

    kMeans.setSeed(bestSeed);
    kMeans.setNumClusters(bestK);
    kMeans.buildClusterer(data);
  }

  private int selectKManually(double[] meanCHs, int bestK)
  {
    DefaultTableModel m = new DefaultTableModel()
      {
        public boolean isCellEditable(int row, int column)
        {
          return false;
        }
      };
    JTable t = new JTable(m);
    t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer()
      {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column)
        {
          return super.getTableCellRendererComponent(table, column == 1 ? df.format((Double) value) : value,
                                                     isSelected, hasFocus, row, column);
        }
    });
    m.addColumn("Num clusters");
    m.addColumn("Mean CH score");
    for (int i = 0; i < meanCHs.length; i++)
      m.addRow(new Object[] { new Integer(minNumClusters + i), new Double(meanCHs[i]) });
    t.setRowSelectionInterval(bestK - minNumClusters, bestK - minNumClusters);
    JScrollPane s = new JScrollPane(t);
    if (meanCHs.length < 20)
      s.setPreferredSize(new Dimension(300, t.getRowHeight() * (meanCHs.length + 2)));
    JOptionPane.showConfirmDialog(null, s, "Select number of clusters", JOptionPane.DEFAULT_OPTION);
    return (t.getSelectedRow() + minNumClusters);
  }

  @Override
    public int clusterInstance(Instance instance) throws Exception
  {
    return kMeans.clusterInstance(instance);
  }

  private double getSquaredErrorBetweenClusters()
  {
    double errorSum = 0;
    for (int i = 0; i < kMeans.getNumClusters(); i++)
      {
        double dist = kMeans.getDistanceFunction().distance(kMeans.getClusterCentroids().instance(i), meanInstance);
        if (kMeans.getDistanceFunction() instanceof EuclideanDistance)//Euclidean distance to Squared Euclidean distance
          dist *= dist;
        dist *= kMeans.getClusterSizes()[i];
        errorSum += dist;
      }
    return errorSum;
  }

  /**
   * see Calinski, T. and J. Harabasz. 1974. A dendrite method for cluster analysis. Commun. Stat. 3: 1-27.
   * quoted in German: http://books.google.com/books?id=-f9Ox0p1-D4C&lpg=PA394&ots=SV3JfRIkQn&dq=Calinski%20and%20Harabasz&hl=de&pg=PA394#v=onepage&q&f=false
   * 
   * @param kMeans
   * @param data
   * @return
   */
  private double getCalinskiHarabasz()
  {
    double betweenClusters = getSquaredErrorBetweenClusters() / (double) (kMeans.getNumClusters() - 1);
    double withinClusters = kMeans.getSquaredError() / (double) (numInstances - kMeans.getNumClusters());
    return betweenClusters / withinClusters;
  }

  @Override
    public double[] distributionForInstance(Instance instance) throws Exception
  {
    return kMeans.distributionForInstance(instance);
  }

  @Override
    public int numberOfClusters() throws Exception
  {
    return kMeans.numberOfClusters();
  }

  @Override
    public Capabilities getCapabilities()
  {
    return kMeans.getCapabilities();
  }

  public String minNumClustersTipText() {
    return "The minimum number of clusters to consider";
  }

  public int getMinNumClusters()
  {
    return minNumClusters;
  }

  public void setMinNumClusters(int minNumClusters)
  {
    this.minNumClusters = minNumClusters;
  }

  public String maxNumClustersTipText() {
    return "The maximum number of clusters to consider";
  }

  public int getMaxNumClusters()
  {
    return maxNumClusters;
  }

  public void setMaxNumClusters(int maxNumClusters)
  {
    this.maxNumClusters = maxNumClusters;
  }

  public String restartsTipText() {
    return "The number of restarts to use";
  }

  public int getRestarts()
  {
    return restarts;
  }

  public void setRestarts(int restarts)
  {
    this.restarts = restarts;
  }

  public String printDebugTipText() {
    return "Print debugging information to the console";
  }

  public boolean isPrintDebug()
  {
    return printDebug;
  }

  public void setPrintDebug(boolean printDebug)
  {
    this.printDebug = printDebug;
  }

  public String distanceFunctionTipText() {
    return "The distance function to use - only euclidean and manhattan "
      + "are allowed";
  }

  public DistanceFunction getDistanceFunction()
  {
    return distanceFunction;
  }

  public void setDistanceFunction(DistanceFunction distanceFunction)
  {
    this.distanceFunction = distanceFunction;
  }

  public String maxIterationsTipText() {
    return "Maximum number of iterations for k-means";
  }

  public int getMaxIterations()
  {
    return maxIterations;
  }

  public void setMaxIterations(int maxIterations)
  {
    this.maxIterations = maxIterations;
  }

  public String manuallySelectNumClustersTipText() {
    return "Manually select the number of clusters to use from "
      + "the results generated";
  }

  public boolean isManuallySelectNumClusters()
  {
    return manuallySelectNumClusters;
  }

  public void setManuallySelectNumClusters(boolean manuallySelectNumClusters)
  {
    this.manuallySelectNumClusters = manuallySelectNumClusters;
  }

  /**
   * Returns the tip text for this property.
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String initializeUsingKMeansPlusPlusMethodTipText() {
    return "Initialize cluster centers using the probabilistic "
      + " farthest first method of the k-means++ algorithm";
  }
  
  /**
   * Set whether to initialize using the probabilistic farthest
   * first like method of the k-means++ algorithm (rather than
   * the standard random selection of initial cluster centers).
   * 
   * @param k true if the k-means++ method is to be used to select
   * initial cluster centers.
   */
  public void setInitializeUsingKMeansPlusPlusMethod(boolean k) {
    initializeWithKMeansPlusPlus = k;
  }
  
  /**
   * Get whether to initialize using the probabilistic farthest
   * first like method of the k-means++ algorithm (rather than
   * the standard random selection of initial cluster centers).
   * 
   * @return true if the k-means++ method is to be used to select
   * initial cluster centers.
   */
  public boolean getInitializeUsingKMeansPlusPlusMethod() {
    return initializeWithKMeansPlusPlus;
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector result = new Vector();

    result.addElement(new Option(
                                 "\tMin number of clusters.\n"
                                 + "\t(default 2).", 
                                 "min", 1, "-min <num>"));

    result.addElement(new Option(
                                 "\tMax number of clusters.\n"
                                 + "\t(default 10).", 
                                 "max", 1, "-max <num>"));

    result.addElement(new Option("\tNumber of restarts.\n\t(default 10)",
                                 "restarts", 1, "-restarts <num>"));

    result.addElement(new Option("\tManually select the number of clusters.",
                                 "manual", 0, "-manual"));
    
    result.addElement(new Option(
                                 "\tInitialize using the k-means++ method.\n", 
                                 "P", 0, "-P"));

    result.add(new Option(
                          "\tDistance function to use.\n"
                          + "\t(default: weka.core.EuclideanDistance)",
                          "A", 1,"-A <classname and options>"));
    
    result.add(new Option(
                          "\tMaximum number of iterations.\n",
                          "I",1,"-I <num>"));

    result.add(new Option("\tPrint debug info.", "debug", 0, "-debug"));

    
    Enumeration en = super.listOptions();
    while (en.hasMoreElements())
      result.addElement(en.nextElement());

    return  result.elements();
  }

  /**
   * Parses a given list of options. <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -N &lt;num&gt;
   *  number of clusters.
   *  (default 2).</pre>
   * 
   * <pre> -P
   *  Initialize using the k-means++ method.
   * </pre>
   * 
   * <pre> -V
   *  Display std. deviations for centroids.
   * </pre>
   * 
   * <pre> -M
   *  Replace missing values with mean/mode.
   * </pre>
   * 
   * <pre> -A &lt;classname and options&gt;
   *  Distance function to use.
   *  (default: weka.core.EuclideanDistance)</pre>
   * 
   * <pre> -I &lt;num&gt;
   *  Maximum number of iterations.
   * </pre>
   * 
   * <pre> -O
   *  Preserve order of instances.
   * </pre>
   * 
   * <pre> -fast
   *  Enables faster distance calculations, using cut-off values.
   *  Disables the calculation/output of squared errors/distances.
   * </pre>
   * 
   * <pre> -S &lt;num&gt;
   *  Random number seed.
   *  (default 10)</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions (String[] options)
    throws Exception {
    
    String optionString = Utils.getOption("I", options);
    if (optionString.length() != 0) {
      setMaxIterations(Integer.parseInt(optionString));
    }

    optionString = Utils.getOption("max", options);
    if (optionString.length() > 0) {
      setMaxNumClusters(Integer.parseInt(optionString));
    }

    optionString = Utils.getOption("min", options);
    if (optionString.length() > 0) {
      setMinNumClusters(Integer.parseInt(optionString));
    }

    optionString = Utils.getOption("restarts", options);
    if (optionString.length() > 0) {
      setRestarts(Integer.parseInt(optionString));
    }

    setManuallySelectNumClusters(Utils.getFlag("manual", options));
    setPrintDebug(Utils.getFlag("debug", options));
    initializeWithKMeansPlusPlus = Utils.getFlag('P', options);
    
    String distFunctionClass = Utils.getOption('A', options);
    if (distFunctionClass.length() != 0) {
      String distFunctionClassSpec[] = Utils.splitOptions(distFunctionClass);
      if (distFunctionClassSpec.length == 0) { 
        throw new Exception("Invalid DistanceFunction specification string."); 
      }
      String className = distFunctionClassSpec[0];
      distFunctionClassSpec[0] = "";

      setDistanceFunction( (DistanceFunction)
                           Utils.forName( DistanceFunction.class, 
                                          className, distFunctionClassSpec) );
    }
    else {
      setDistanceFunction(new EuclideanDistance());
    }    
    
    super.setOptions(options);
  }

  /**
   * Gets the current settings of SimpleKMeans.
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions () {
    ArrayList<String> options = new ArrayList<String>();

    options.add("-I"); options.add("" + getMaxIterations());
    options.add("-min"); options.add("" + getMinNumClusters());
    options.add("-max"); options.add("" + getMaxNumClusters());
    options.add("-restarts"); options.add("" + getRestarts());
    if (isManuallySelectNumClusters()) {
      options.add("-manual");
    }
    if (getInitializeUsingKMeansPlusPlusMethod()) {
      options.add("-P");
    }
    if (isPrintDebug()) {
      options.add("-debug");
    }

    options.add("-A");
    options.add((distanceFunction.getClass().getName() + " " +
                Utils.joinOptions(distanceFunction.getOptions())).trim());

    return (String[]) options.toArray(new String[options.size()]);
  }

  /*  public static void main(String args[]) throws Exception
  {
    Instances data = new Instances(new FileReader(new File("/home/martin/software/weka-3-6-6/data/cpu.arff")));
    CascadeSimpleKMeans c = new CascadeSimpleKMeans();
    c.setManuallySelectNumClusters(true);
    c.setPrintDebug(true);
    c.buildClusterer(data);
    System.exit(0);
    } */
  
  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }

  /**
   * Main method for executing this class.
   *
   * @param args use -h to list all parameters
   */
  public static void main (String[] args) {
    runClusterer(new CascadeSimpleKMeans(), args);
  }
}
