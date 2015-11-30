// This Groovy script, which can be executed using the GroovyConsole that is available
// from the tools menu of WEKA's GUIChooser once the kfGroovy package has been installed,
// processes an .nii example file containing an entire dataset of volumes, a separate
// text file with labels for these volumes, and a .nii file with a mask applied to each volume.
//
// The script uses the NIfTIFileLoader for WEKA to create training and test
// files for WEKA. It builds a logistic regression model using LibLINEAR from 
// the training data and evaluates it on the test data.

// Import the Java libraries for dealing with NIFTI data using WEKA
import weka.core.converters.NIfTIFileLoader
import weka.core.SelectedTag
import weka.core.Instances

// Import WEKA libraries for training and evaluation
import weka.classifiers.functions.LibLINEAR
import weka.classifiers.evaluation.Evaluation

// We need some packages to download and extract example .nii data from the web
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.CompressorStreamFactory

println "Creating temporary folder to store downloaded data and extract it"
inputFolder = File.createTempFile("pymvpa-exampledata", "");
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
    buffer = new byte[4096];
    out = new BufferedOutputStream(new FileOutputStream(inputPrefix + nextEntry.getName()));
    int len;
    while((len = tarArchiveInputStream.read(buffer)) != -1) {
      out.write(buffer, 0, len);   
    }
    out.close();
  }
}
tarArchiveInputStream.close()

println "Loading data as a WEKA Instances object using the NIfTIFileLoader"
loader = new NIfTIFileLoader()
loader.setSource(new File(inputPrefix + "pymvpa-exampledata" + File.separator + "bold.nii.gz"))
loader.setAttributesFile(new File(inputPrefix + "pymvpa-exampledata" + File.separator + "attributes_literal.txt"))
loader.setMaskFile(new File(inputPrefix + "pymvpa-exampledata" + File.separator + "mask.nii.gz"))
data = loader.getDataSet()
data.setClassIndex(0) // The first attribute has the class

println "Splitting data into training and test data, using the first 968 volumes for training"
trainingData = new Instances(data, 0, 968);
testData = new Instances(data, 968, data.numInstances() - 968)

println "Building model on training data"
model = new LibLINEAR()
model.setSVMType(new SelectedTag(0, LibLINEAR.TAGS_SVMTYPE))
model.buildClassifier(trainingData)

println "Evaluating model on test data"
evaluation = new Evaluation(trainingData)
evaluation.evaluateModel(model, testData)
println evaluation.toSummaryString()
println evaluation.toMatrixString()