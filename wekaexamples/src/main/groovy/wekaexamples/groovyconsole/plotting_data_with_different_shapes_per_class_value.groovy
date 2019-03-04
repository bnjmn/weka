// From Eibe's Wekalist post:
// https://list.waikato.ac.nz/pipermail/wekalist/2017-August/044302.html

// Set up a window for the plot and a Plot2D object
jf = new javax.swing.JFrame("Visualize groups of instances using different glyphs")
jf.setSize(500, 400)
jf.getContentPane().setLayout(new java.awt.BorderLayout());
p2D = new weka.gui.visualize.Plot2D();
jf.getContentPane().add(p2D, java.awt.BorderLayout.CENTER)

// Load the data and configure the plot
data = (new weka.core.converters.ConverterUtils.DataSource("/Users/eibe/datasets/UCI/iris.arff")).getDataSet() // Change file name here
pd1 = new weka.gui.visualize.PlotData2D(data)
shapeTypesForInstances = new ArrayList<Integer>(data.numInstances())
shapeSizesForInstances = new ArrayList<Integer>(data.numInstances())
for (inst in data) {
  shapeTypesForInstances.add((int)inst.value(data.attribute("class").index())) // Change attribute giving group indicators here
  shapeSizesForInstances.add(3)
}
pd1.setShapeType(shapeTypesForInstances)
pd1.setShapeSize(shapeSizesForInstances)
pd1.setCustomColour(java.awt.Color.BLACK)
p2D.setMasterPlot(pd1)
p2D.setXindex(data.attribute("petallength").index()) // Change attribute name for X axis here
p2D.setYindex(data.attribute("petalwidth").index()) // Change attribute name for Y axis here

// Make plot visible
jf.setVisible(true)
