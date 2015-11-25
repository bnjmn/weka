// This Groovy script, which can be executed using the GroovyConsole that is available
// from the tools menu of WEKA's GUIChooser once the kfGroovy package has been installed,
// processes an .nii example file containing an entire dataset of volumes and a separate
// text file with labels for these volumes, into two folders containing individual .nii
// files for each volume. The labels are used to create subdirectories containing these
// .nii files. The script creates two directories, one for training (WEKA_train) and one
// for testing (WEKA_test). These can be opened in WEKA using the NiftiDirectoryLoader.
//
// The script then uses the NIfTIDirectoryLoader for WEKA to create training and test
// files for WEKA. It builds a naive Bayes model from the training data and evaluates it
// on the test data.

// Import the Java libraries for dealing with NIFTI data using WEKA
import weka.core.converters.nifti.Nifti1Dataset
import weka.core.converters.NIfTIDirectoryLoader

// Import WEKA libraries for training and evaluation
import weka.classifiers.bayes.NaiveBayes
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

println "Reading attributes_literal.txt file with labels"
reader = new BufferedReader(new FileReader(inputPrefix + "pymvpa-exampledata" + File.separator + "attributes_literal.txt"))
labels = new ArrayList<String>()
while ((label = reader.readLine()) != null) {
  labels.add(label.split(" ")[0])
}
reader.close()

println "Creating folder pymvpa-exampledata-processed-for-weka for output data in user's home"
outputFolder = new File(System.getProperty("user.home") + File.separator + "pymvpa-exampledata-processed-for-weka")
outputFolder.delete()
outputFolder.mkdir()
outputPrefix = outputFolder.getAbsolutePath() + File.separator

println "Creating folders for training and test data in output folder"
for (label in labels) {
  dir = new File(outputPrefix + "WEKA_train" + File.separator + label);
  dir.mkdirs();
  dir = new File(outputPrefix + "WEKA_test" + File.separator + label);
  dir.mkdirs();
}

println "Loading header of bold.nii file" 
data = new Nifti1Dataset(inputPrefix + "pymvpa-exampledata" + File.separator + "bold.nii")
data.readHeader()

println "Creating .nii files in folders, using first eight sessions for training (including the first 968 volumes)"
splitPoint = 968
for (int i = 0; i < splitPoint; i++) {
  localName = outputPrefix + "WEKA_train" + File.separator + labels.get(i) + File.separator + i
  volume = data.readDoubleVol((short)i)
  outputFile = new Nifti1Dataset()
  outputFile.copyHeader(data)
  outputFile.setHeaderFilename(localName + ".nii")
  outputFile.writeHeader()
  outputFile.setDataFilename(localName)
  outputFile.writeVol(volume, (short)0)
}

println "Creating .nii files in folders, using remaining four sessions for testing"
for (int i = splitPoint; i < data.TDIM; i++) {
  localName = outputPrefix + "WEKA_test" + File.separator + labels.get(i) + File.separator + i
  volume = data.readDoubleVol((short)i)
  outputFile = new Nifti1Dataset()
  outputFile.copyHeader(data)
  outputFile.setHeaderFilename(localName + ".nii")
  outputFile.writeHeader()
  outputFile.setDataFilename(localName)
  outputFile.writeVol(volume, (short)0)
}

println "Deleting temporary folder with downloaded archive"
inputFolder.delete()

println "Loading training data as a WEKA Instances object using the NIfTIDirectoryLoader"
loader = new NIfTIDirectoryLoader()
loader.setDirectory(new File(outputPrefix + "WEKA_train"))
trainingData = loader.getDataSet()

println "Building naive Bayes model on training data"
model = new NaiveBayes()
model.buildClassifier(trainingData)

println "Loading test data as a WEKA Instances object using the NIfTIDirectoryLoader"
loader = new NIfTIDirectoryLoader()
loader.setDirectory(new File(outputPrefix + "WEKA_test"))
testData = loader.getDataSet()

println "Evaluating model on test data"
evaluation = new Evaluation(trainingData)
evaluation.evaluateModel(model, testData)
println evaluation.toSummaryString()
println evaluation.toMatrixString()