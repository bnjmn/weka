// This Groovy script, which can be executed using the GroovyConsole that is available
// from the tools menu of WEKA's GUIChooser once the kfGroovy package has been installed,
// processes an .nii example file containing an entire dataset of volumes, a separate
// text file with labels for these volumes, and a .nii file with a mask applied to each volume.
//
// The script uses the NIfTIFileLoader for WEKA to load the data. 
// It builds a linear SVM using LibLINEAR on the first 10 sessions and evaluates it on the last 2.
//
// This script requires WEKA versions > 3.7.13 to run. Also, the niftiLoader, multiInstanceFilters, and
// LibLINEAR packages need to have been installed.

// Import the Java libraries for dealing with NIFTI data using WEKA
import weka.core.converters.NIfTIFileLoader

// Import WEKA libraries for preprocessing, training, and evaluation
import weka.filters.Filter
import weka.filters.unsupervised.attribute.PropositionalToMultiInstance
import weka.filters.unsupervised.attribute.RELAGGS
import weka.filters.unsupervised.attribute.CartesianProduct
import weka.filters.unsupervised.attribute.Remove
import weka.filters.unsupervised.instance.RemoveWithValues
import weka.classifiers.meta.FilteredClassifier
import weka.classifiers.functions.LibLINEAR
import weka.classifiers.evaluation.Evaluation

// We need some packages to download and extract example .nii data from the web
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.CompressorStreamFactory

println "Creating temporary folder to store downloaded data and extract it"
inputFolder = File.createTempFile("pymvpa-exampledata", "")
inputFolder.delete()
inputFolder.mkdir()
inputPrefix = inputFolder.getAbsolutePath() + File.separator

println "Downloading and extracting http://www.pymvpa.org/files/pymvpa_exampledata.tar.bz2"
tarArchiveInputStream = new TarArchiveInputStream(new CompressorStreamFactory()
    .createCompressorInputStream(new BufferedInputStream(new URL("http://www.pymvpa.org/files/pymvpa_exampledata.tar.bz2").openStream())))
while ((nextEntry = tarArchiveInputStream.getNextTarEntry()) != null) {
  if (nextEntry.isDirectory()) {
    new File(inputPrefix + nextEntry.getName()).mkdirs()
  } else {
    buffer = new byte[4096]
    out = new BufferedOutputStream(new FileOutputStream(inputPrefix + nextEntry.getName()))
    while((len = tarArchiveInputStream.read(buffer)) != -1) {
      out.write(buffer, 0, len)
    }
    out.close()
  }
}
tarArchiveInputStream.close()

println "Loading data as a WEKA Instances object using the NIfTIFileLoader"
loader = new NIfTIFileLoader()
loader.setSource(new File(inputPrefix + "pymvpa-exampledata" + File.separator + "bold.nii.gz"))
loader.setAttributesFile(new File(inputPrefix + "pymvpa-exampledata" + File.separator + "attributes_literal.txt"))
loader.setMaskFile(new File(inputPrefix + "pymvpa-exampledata" + File.separator + "mask.nii.gz"))
data = loader.getDataSet()

println "Removing all instances corresponding to rest condition"
rWV = new RemoveWithValues();
rWV.setAttributeIndex("1")
rWV.setNominalIndices("1")
rWV.setModifyHeader(true)
rWV.setInputFormat(data)
data = Filter.useFilter(data, rWV)

println "Adding Cartesian product of class and session ID as last attribute"
addBagID = new CartesianProduct()
addBagID.setAttributeIndices("1-2")
addBagID.setInputFormat(data)
data = Filter.useFilter(data, addBagID)

println "Removing session ID attribute"
remove = new Remove()
remove.setAttributeIndices("2")
remove.setInputFormat(data)
data = Filter.useFilter(data, remove)

println "Converting into relational format by collecting one bag for each combination of class value and session ID"
toRelational = new PropositionalToMultiInstance()
toRelational.setBagID("last")
toRelational.setDoNotWeightBags(true) // We do not want to weight instances by the size of their bags
data.setClassIndex(0) // The first attribute has the class (indexing is 0-based)
toRelational.setInputFormat(data)
data = Filter.useFilter(data, toRelational)

println "Aggregating relational values (i.e., volumes in one bag) by computing the centroid for each bag"
toSummaryStats = new RELAGGS()
toSummaryStats.setGenerateSparseInstances(true) // Important to generate sparse rather than dense data!
toSummaryStats.setDisableMIN(true)
toSummaryStats.setDisableMAX(true)
toSummaryStats.setDisableSTDEV(true)
toSummaryStats.setDisableSUM(true)
toSummaryStats.setInputFormat(data)
data = Filter.useFilter(data, toSummaryStats)  // Compute summary statistics for each bag

println "Creating training set using first 10 sessions"
rWV = new RemoveWithValues();
rWV.setAttributeIndex("1")
rWV.setNominalIndices("11,12,23,24,35,36,47,48,59,60,71,72,83,84,95,96")
rWV.setInputFormat(data)
train = Filter.useFilter(data, rWV)

println "Creating test set using last 2 sessions"
rWV = new RemoveWithValues();
rWV.setAttributeIndex("1")
rWV.setNominalIndices("11,12,23,24,35,36,47,48,59,60,71,72,83,84,95,96")
rWV.setInvertSelection(true)
rWV.setInputFormat(data)
test = Filter.useFilter(data, rWV)

println "Setting up model configuration with FilteredClassifier to remove bag identifier"
model = new FilteredClassifier()
svm = new LibLINEAR()
svm.setNormalize(true)
model.setClassifier(svm)
filter = new Remove();
filter.setAttributeIndices("first")
model.setFilter(filter)

println "Building model on training set"
model.buildClassifier(train)

println "Evaluating model on test set"
evaluation = new Evaluation(train)
evaluation.evaluateModel(model, test)
println evaluation.toSummaryString()
println evaluation.toMatrixString()