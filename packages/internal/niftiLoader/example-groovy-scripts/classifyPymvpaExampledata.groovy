// This Groovy script, which can be executed using the GroovyConsole that is available
// from the tools menu of WEKA's GUIChooser once the kfGroovy package has been installed,
// processes an .nii.gz example file containing an entire dataset of volumes, a separate
// text file with labels for these volumes, and a .nii.gz file with a mask.
//
// The script uses the NIfTIFileLoader for WEKA to load the data. It attempts to replicate the 12-fold
// leave-one-run-out cross-validation experiment from http://www.pymvpa.org/tutorial_classifiers.html, but
// without the domain-specific detrending and normalisation steps described at
// http://www.pymvpa.org/tutorial_mappers.html#basic-preprocessing. Instead, standard WEKA normalization is applied
// before the linear SVM is built (-Z flag for LibLINEAR). Average accuracy across the 12-folds obtained using this
// script is 80.21%, slightly better than the 78.13% reported at the above URL.
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
import weka.classifiers.AbstractClassifier
import weka.classifiers.meta.FilteredClassifier
import weka.classifiers.functions.LibLINEAR
import weka.classifiers.evaluation.Evaluation

// We need some packages to download and extract example .nii data from the web
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.CompressorStreamFactory

println "Creating temporary folder to store downloaded data and extract it"
inputFolder = File.createTempFile("tutorial_data", "")
inputFolder.delete()
inputFolder.mkdir()
inputPrefix = inputFolder.getAbsolutePath() + File.separator

println "Downloading and extracting http://data.pymvpa.org/datasets/tutorial_data/tutorial_data-0.2.tar.gz"
tarArchiveInputStream = new TarArchiveInputStream(new CompressorStreamFactory()
        .createCompressorInputStream(new BufferedInputStream(new URL("http://data.pymvpa.org/datasets/tutorial_data/tutorial_data-0.2.tar.gz").openStream())))
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
actualInputFolder = inputPrefix + "tutorial_data" + File.separator + "data" + File.separator
loader = new NIfTIFileLoader()
loader.setSource(new File(actualInputFolder + "bold.nii.gz"))
loader.setAttributesFile(new File(actualInputFolder + "attributes.txt"))
loader.setMaskFile(new File(actualInputFolder + "mask_vt.nii.gz"))
data = loader.getDataSet()

println "Removing all instances corresponding to rest condition"
rWV = new RemoveWithValues();
rWV.setAttributeIndex("1")
rWV.setNominalIndices("1")
rWV.setModifyHeader(true)
rWV.setInputFormat(data)
data = Filter.useFilter(data, rWV)

println "Adding Cartesian product of class and run ID as last attribute"
addBagID = new CartesianProduct()
addBagID.setAttributeIndices("1-2")
addBagID.setInputFormat(data)
data = Filter.useFilter(data, addBagID)

println "Removing run ID attribute"
remove = new Remove()
remove.setAttributeIndices("2")
remove.setInputFormat(data)
data = Filter.useFilter(data, remove)

println "Converting into relational format by collecting one bag for each combination of class value and run ID"
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

println "Creating Evaluation object to hold results of 12-fold leave-one-run-out cross-validation"
evaluation = new Evaluation(data)

println "Setting up model configuration with FilteredClassifier to remove bag identifier"
template = new FilteredClassifier()
svm = new LibLINEAR()
svm.setNormalize(true)
template.setClassifier(svm)
filter = new Remove();
filter.setAttributeIndices("first")
template.setFilter(filter)

println "Running 12-fold leave-one-run-out cross-validation"
for (int i = 1; i <= 12; i++) {

  // Copy classifier so that we can be sure that no data is leaked this way
  model = AbstractClassifier.makeCopy(template)

  // order of values: scissors_x_0,...,scissors_x_11,face_x_0,...,face_x_11,cat_x_0,...,cat_x_11,shoe_x_0,...,shoe_x_11,
  // house_x_0,...,house_x_11,scrambledpix_x_0,...,scrambledpix_x_11,bottle_x_0,...,bottle_x_11,chair_x_0,...,chair_x_11
  indicesToRemove = i + "," + (i + 12) + "," + (i + 24) + "," + (i + 36) + "," + (i + 48) + "," + (i + 60) + "," + (i + 72) + "," + (i + 84)

  println "Creating training set leaving out run " + i
  rWV = new RemoveWithValues();
  rWV.setAttributeIndex("1")
  rWV.setNominalIndices(indicesToRemove)
  rWV.setInputFormat(data)
  train = Filter.useFilter(data, rWV)

  println "Creating test set using run " + i
  rWV = new RemoveWithValues();
  rWV.setAttributeIndex("1")
  rWV.setNominalIndices(indicesToRemove)
  rWV.setInvertSelection(true)
  rWV.setInputFormat(data)
  test = Filter.useFilter(data, rWV)

  println "Building model on training set"
  model.buildClassifier(train)

  println "Evaluating model on test set"
  evaluation.evaluateModel(model, test)
}
println evaluation.toSummaryString()
println evaluation.toMatrixString()