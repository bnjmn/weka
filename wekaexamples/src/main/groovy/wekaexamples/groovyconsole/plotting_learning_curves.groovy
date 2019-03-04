// From Eibe's Wekalist post:
// https://list.waikato.ac.nz/pipermail/wekalist/2019-February/047378.html

// Load dataset and set the class index
data = (new weka.core.converters.ConverterUtils.DataSource("/Users/eibe/Downloads/database_2.arff")).getDataSet()
data.setClassIndex(data.numAttributes() - 1)

// Set up the classifiers, their options and corresponding colors
classifiers = new ArrayList<weka.classifiers.Classifier>()
colors = new HashMap<weka.classifiers.Classifier, java.awt.Color>()
classifier = new weka.classifiers.functions.SMO()
classifier.setOptions(weka.core.Utils.splitOptions(""))
classifiers.add(classifier)
colors.put(classifier, java.awt.Color.RED)
classifier = new weka.classifiers.functions.Logistic()
classifier.setOptions(weka.core.Utils.splitOptions("-C"))
classifiers.add(classifier)
colors.put(classifier, java.awt.Color.GREEN)

// Set up dataset format for learning curve data
attributes = new ArrayList<weka.core.Attribute>()
attributes.add(new weka.core.Attribute("Number of instances"))
attributes.add(new weka.core.Attribute("Percent correct"))
learningCurveDataTemplate = new weka.core.Instances("Learning curve", attributes, 0)

// Set up hash map to hold learning curve datasets
learningCurves = new HashMap<weka.classifiers.Classifier, weka.core.Instances>()

// Specify k for the k-fold cross-validation
k = 10

// Specify the number of k-fold cross-validation runs
r = 10

// Percentage increment for training set size
p = 10

// For each of the classifiers, get data for its learning curve
for (classifier in classifiers) {

  // Create new Instances object to hold data for current learning curve
  learningCurveData = new weka.core.Instances(learningCurveDataTemplate)
  learningCurveData.setRelationName(classifier.getClass().getName() + " " +
    weka.core.Utils.joinOptions(classifier.getOptions()))

  double percentage = p;
  while (percentage <= 100.0) {
    random = new Random(1) // Same random number generator for each r times k-fold CV run
    double averagePerformance = 0
    weka.classifiers.meta.FilteredClassifier fc = new weka.classifiers.meta.FilteredClassifier()
    fc.setClassifier(classifier)
    weka.filters.unsupervised.instance.Resample rs = new weka.filters.unsupervised.instance.Resample()
    rs.setSampleSizePercent(percentage)
    rs.setNoReplacement(true)
    fc.setFilter(rs)
    for (int l = 0; l < r; l++) {
        evaluationObject = new weka.classifiers.evaluation.Evaluation(data) // Set up evaluation object
        evaluationObject.crossValidateModel(fc, data, k, random) // Run k-fold CV
        averagePerformance += evaluationObject.pctCorrect()
    }
    double[] instanceVals = [ percentage, averagePerformance / r] // Get size and average performance
    learningCurveData.add(new weka.core.DenseInstance(1.0, instanceVals)) // Add data point to dataset
    percentage += p
  }
  learningCurves.put(classifier, learningCurveData)
}

// ===== Now that we have the data for the learning curves, plot it =====

// Make dataset defining boundaries of plotting area
boundaryData = new weka.core.Instances("Boundary data", attributes, 0)
double[] p1 = [ weka.core.Utils.missingValue(), 100.0 ] // Maximum percentage is 100
double[] p2 = [ data.numInstances(), weka.core.Utils.missingValue() ]
double[] p3 = [ weka.core.Utils.missingValue(), 0.0 ]
double[] p4 = [ 0.0, weka.core.Utils.missingValue() ]
boundaryData.add(new weka.core.DenseInstance(1.0, p1))
boundaryData.add(new weka.core.DenseInstance(1.0, p2))
boundaryData.add(new weka.core.DenseInstance(1.0, p3))
boundaryData.add(new weka.core.DenseInstance(1.0, p4))
masterPlot = new weka.gui.visualize.PlotData2D(boundaryData)

// Set up the plot
p2D = new weka.gui.visualize.Plot2D()
p2D.setPreferredSize(new java.awt.Dimension(400, 400))
p2D.setMasterPlot(masterPlot)
p2D.setXindex(learningCurveDataTemplate.attribute("Number of instances").index())
p2D.setYindex(learningCurveDataTemplate.attribute("Percent correct").index())

// Need list of plot lists for legend panel
plotList = new ArrayList<weka.gui.visualize.PlotData2D>()

// Add the actual learning curve(s) to the plot
for (classifier in classifiers) {
  learningCurveData = learningCurves.get(classifier)
  shapeTypesForInstances = new ArrayList<Integer>()
  shapeSizesForInstances = new ArrayList<Integer>()
  connectPoints = new ArrayList<Boolean>()
  for (inst in learningCurveData) {
    shapeTypesForInstances.add(weka.gui.visualize.Plot2D.DIAMOND_SHAPE)
    shapeSizesForInstances.add(1)
    connectPoints.add(true)
  }
  pd = new weka.gui.visualize.PlotData2D(learningCurveData)
  pd.setShapeType(shapeTypesForInstances)
  pd.setShapeSize(shapeSizesForInstances)
  pd.setConnectPoints(connectPoints)
  pd.setCustomColour(colors.get(classifier))
  pd.setPlotName(learningCurveData.relationName())
  p2D.addPlot(pd)
  plotList.add(pd)
}

// Make sure legend is created
legendPanel = new weka.gui.visualize.LegendPanel()
legendPanel.setPlotList(plotList)
legendPanel.setPreferredSize(new java.awt.Dimension(800, 400))

// Make a window with the plot
jf = new javax.swing.JFrame("Learning curve(s)")
jf.getContentPane().setLayout(new java.awt.BorderLayout());
jf.getContentPane().add(p2D, java.awt.BorderLayout.CENTER)
jf.getContentPane().add(legendPanel, java.awt.BorderLayout.EAST)
jf.pack()
jf.setVisible(true)
