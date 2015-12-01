// This Groovy script, which can be executed using the GroovyConsole that is available
// from the tools menu of WEKA's GUIChooser once the kfGroovy package has been installed,
// processes an .nii example file containing an entire dataset of volumes, a separate
// text file with labels for these volumes, and a .nii file with a mask applied to each volume.
//
// The script uses the NIfTIFileLoader for WEKA to load the data. 
// It performs a leave-one-out cross-validation with a logistic regression model using LibLINEAR.

// Import the Java libraries for dealing with NIFTI data using WEKA
import weka.core.converters.NIfTIFileLoader
import weka.core.SelectedTag
import weka.core.Instances

// Import WEKA libraries for preprocessing, training, and evaluation
import weka.filters.Filter
import weka.filters.unsupervised.attribute.PropositionalToMultiInstance
import weka.filters.unsupervised.attribute.RELAGGS
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
data.setClassIndex(0) // The first attribute has the class (indexing is 0-based)

println "Turning data into relational format and propositionalising it again by computing summary statistics"
toRelational = new PropositionalToMultiInstance()
toRelational.setBagID(2) // The second attribute is the bag ID (indexing is 1-based)
toRelational.setInputFormat(data)
data = Filter.useFilter(data, toRelational)
toSummaryStats = new RELAGGS()
toSummaryStats.setInputFormat(data)
data = Filter.useFilter(data, toSummaryStats)

println data.classAttribute().toString()
for (int i = 0; i < data.numInstances(); i++) {
  println data.instance(i).classValue()
}

println "Setting up model configuration"
model = new LibLINEAR()
model.setSVMType(new SelectedTag(0, LibLINEAR.TAGS_SVMTYPE))

println "Performing leave-one-out cross-validation on the data"
evaluation = new Evaluation(data)
evaluation.crossValidateModel(model, data, data.numInstances(), new Random(1))
println evaluation.toSummaryString()
println evaluation.toMatrixString()