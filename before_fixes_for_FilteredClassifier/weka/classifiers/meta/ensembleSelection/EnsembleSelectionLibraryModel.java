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
 *    EnsembleSelection.java
 *    Copyright (C) 2006 David Michael
 *
 */

package weka.classifiers.meta.ensembleSelection;

import weka.classifiers.Classifier;
import weka.classifiers.EnsembleLibraryModel;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.zip.Adler32;

/**
 * This class represents a library model that is used for EnsembleSelection. At
 * this level the concept of cross validation is abstracted away. This class
 * keeps track of the performance statistics and bookkeeping information for its
 * "model type" accross all the CV folds. By "model type", I mean the
 * combination of both the Classifier type (e.g. J48), and its set of parameters
 * (e.g. -C 0.5 -X 1 -Y 5). So for example, if you are using 5 fold cross
 * validaiton, this model will keep an array of classifiers[] of length 5 and
 * will keep track of their performances accordingly. This class also has
 * methods to deal with serializing all of this information into the .elm file
 * that will represent this model.
 * <p/>
 * Also it is worth mentioning that another important function of this class is
 * to track all of the dataset information that was used to create this model.
 * This is because we want to protect users from doing foreseeably bad things.
 * e.g., trying to build an ensemble for a dataset with models that were trained
 * on the wrong partitioning of the dataset. This could lead to artificially high
 * performance due to the fact that instances used for the test set to gauge
 * performance could have accidentally been used to train the base classifiers.
 * So in a nutshell, we are preventing people from unintentionally "cheating" by
 * enforcing that the seed, #folds, validation ration, and the checksum of the 
 * Instances.toString() method ALL match exactly.  Otherwise we throw an 
 * exception.
 * 
 * @author  Robert Jung (mrbobjung@gmail.com)
 * @version $Revision: 1.1 $ 
 */
public class EnsembleSelectionLibraryModel
  extends EnsembleLibraryModel
  implements Serializable {
  
  /**
   * This is the serialVersionUID that SHOULD stay the same so that future
   * modified versions of this class will be backwards compatible with older
   * model versions.
   */
  private static final long serialVersionUID = -6426075459862947640L;
  
  /** The default file extension for ensemble library models */
  public static final String FILE_EXTENSION = ".elm";
  
  /** the models */
  private Classifier[] m_models = null;
  
  /** The seed that was used to create this model */
  private int m_seed;
  
  /**
   * The checksum of the instances.arff object that was used to create this
   * model
   */
  private String m_checksum;
  
  /** The validation ratio that was used to create this model */
  private double m_validationRatio;
  
  /**
   * The number of folds, or number of CV models that was used to create this
   * "model"
   */
  private int m_folds;
  
  /**
   * The .elm file name that this model should be saved/loaded to/from
   */
  private String m_fileName;
  
  /**
   * The debug flag as propagated from the main EnsembleSelection class.
   */
  public transient boolean m_Debug = true;
  
  /**
   * the validation predictions of this model. First index for the instance.
   * third is for the class (we use distributionForInstance).
   */
  private double[][] m_validationPredictions = null; // = new double[0][0];
  
  /**
   * Default Constructor
   */
  public EnsembleSelectionLibraryModel() {
  }
  
  /**
   * Constructor for LibaryModel
   * 
   * @param classifier		the classifier to use
   * @param seed		the random seed value
   * @param checksum		the checksum
   * @param validationRatio	the ration to use
   * @param folds		the number of folds to use
   */
  public EnsembleSelectionLibraryModel(Classifier classifier, int seed,
      String checksum, double validationRatio, int folds) {
    
    super(classifier);
    
    m_seed = seed;
    m_checksum = checksum;
    m_validationRatio = validationRatio;
    m_models = null;
    m_folds = folds;
  }
  
  /**
   * This is used to propagate the m_Debug flag of the EnsembleSelection
   * classifier to this class. There are things we would want to print out
   * here also.
   * 
   * @param debug	if true additional information is output
   */
  public void setDebug(boolean debug) {
    m_Debug = debug;
  }
  
  /**
   * Returns the average of the prediction of the models across all folds.
   * 
   * @param instance	the instance to get predictions for
   * @return		the average prediction
   * @throws Exception	if something goes wrong
   */
  public double[] getAveragePrediction(Instance instance) throws Exception {
    
    // Return the average prediction from all classifiers that make up
    // this model.
    double average[] = new double[instance.numClasses()];
    for (int i = 0; i < m_folds; ++i) {
      // Some models alter the instance (MultiLayerPerceptron), so we need
      // to copy it.
      Instance temp_instance = (Instance) instance.copy();
      double[] pred = getFoldPrediction(temp_instance, i);
      if (pred == null) {
	// Some models have bugs whereby they can return a null
	// prediction
	// array (again, MultiLayerPerceptron). We return null, and this
	// should be handled above in EnsembleSelection.
	System.err.println("Null validation predictions given: "
	    + getStringRepresentation());
	return null;
      }
      if (i == 0) {
	// The first time through the loop, just use the first returned
	// prediction array. Just a simple optimization.
	average = pred;
      } else {
	// For the rest, add the prediction to the average array.
	for (int j = 0; j < pred.length; ++j) {
	  average[j] += pred[j];
	}
      }
    }
    if (instance.classAttribute().isNominal()) {
      // Normalize predictions for classes to add up to 1.
      Utils.normalize(average);
    } else {
      average[0] /= m_folds;
    }
    return average;
  }
  
  /**
   * Basic Constructor
   * 
   * @param classifier	the classifier to use
   */
  public EnsembleSelectionLibraryModel(Classifier classifier) {
    super(classifier);
  }
  
  /**
   * Returns prediction of the classifier for the specified fold.
   * 
   * @param instance
   *            instance for which to make a prediction.
   * @param fold
   *            fold number of the classifier to use.
   * @return the prediction for the classes
   * @throws Exception if prediction fails
   */
  public double[] getFoldPrediction(Instance instance, int fold)
    throws Exception {
    
    return m_models[fold].distributionForInstance(instance);
  }
  
  /**
   * Creates the model. If there are n folds, it constructs n classifiers
   * using the current Classifier class and options. If the model has already
   * been created or loaded, starts fresh.
   * 
   * @param data		the data to work with
   * @param hillclimbData	the data for hillclimbing
   * @param dataDirectoryName	the directory to use
   * @param algorithm		the type of algorithm
   * @throws Exception		if something goeds wrong
   */
  public void createModel(Instances[] data, Instances[] hillclimbData,
      String dataDirectoryName, int algorithm) throws Exception {
    
    String modelFileName = getFileName(getStringRepresentation());
    
    File modelFile = new File(dataDirectoryName, modelFileName);
    
    String relativePath = (new File(dataDirectoryName)).getName()
    + File.separatorChar + modelFileName;
    // if (m_Debug) System.out.println("setting relative path to:
    // "+relativePath);
    setFileName(relativePath);
    
    if (!modelFile.exists()) {
      
      Date startTime = new Date();
      
      String lockFileName = EnsembleSelectionLibraryModel
      .getFileName(getStringRepresentation());
      lockFileName = lockFileName.substring(0, lockFileName.length() - 3)
      + "LCK";
      File lockFile = new File(dataDirectoryName, lockFileName);
      
      if (lockFile.exists()) {
	if (m_Debug)
	  System.out.println("Detected lock file.  Skipping: "
	      + lockFileName);
	throw new Exception("Lock File Detected: " + lockFile.getName());
	
      } else { // if (algorithm ==
	// EnsembleSelection.ALGORITHM_BUILD_LIBRARY) {
	// This lock file lets other computers that might be sharing the
	// same file
	// system that this model is already being trained so they know
	// to move ahead
	// and train other models.
	
	if (lockFile.createNewFile()) {
	  
	  if (m_Debug)
	    System.out
	    .println("lock file created: " + lockFileName);
	  
	  if (m_Debug)
	    System.out.println("Creating model in locked mode: "
		+ modelFile.getPath());
	  
	  m_models = new Classifier[m_folds];
	  for (int i = 0; i < m_folds; ++i) {
	    
	    try {
	      m_models[i] = Classifier.forName(getModelClass()
		  .getName(), null);
	      m_models[i].setOptions(getOptions());
	    } catch (Exception e) {
	      throw new Exception("Invalid Options: "
		  + e.getMessage());
	    }
	  }
	  
	  try {
	    for (int i = 0; i < m_folds; ++i) {
	      train(data[i], i);
	    }
	  } catch (Exception e) {
	    throw new Exception("Could not Train: "
		+ e.getMessage());
	  }
	  
	  Date endTime = new Date();
	  int diff = (int) (endTime.getTime() - startTime.getTime());
	  
	  // We don't need the actual model for hillclimbing. To save
	  // memory, release
	  // it.
	  
	  // if (!invalidModels.contains(model)) {
	  // EnsembleLibraryModel.saveModel(dataDirectory.getPath(),
	  // model);
	  // model.releaseModel();
	  // }
	  if (m_Debug)
	    System.out.println("Train time for " + modelFileName
		+ " was: " + diff);
	  
	  if (m_Debug)
	    System.out
	    .println("Generating validation set predictions");
	  
	  startTime = new Date();
	  
	  int total = 0;
	  for (int i = 0; i < m_folds; ++i) {
	    total += hillclimbData[i].numInstances();
	  }
	  
	  m_validationPredictions = new double[total][];
	  
	  int preds_index = 0;
	  for (int i = 0; i < m_folds; ++i) {
	    for (int j = 0; j < hillclimbData[i].numInstances(); ++j) {
	      Instance temp = (Instance) hillclimbData[i]
	                                               .instance(j).copy();// new
	      // Instance(m_hillclimbData[i].instance(j));
	      // must copy the instance because SOME classifiers
	      // (I'm not pointing fingers...
	      // MULTILAYERPERCEPTRON)
	      // change the instance!
	      
	      m_validationPredictions[preds_index] = getFoldPrediction(
		  temp, i);
	      
	      if (m_validationPredictions[preds_index] == null) {
		throw new Exception(
		    "Null validation predictions given: "
		    + getStringRepresentation());
	      }
	      
	      ++preds_index;
	    }
	  }
	  
	  endTime = new Date();
	  diff = (int) (endTime.getTime() - startTime.getTime());
	  
	  // if (m_Debug) System.out.println("Generated a validation
	  // set array of size: "+m_validationPredictions.length);
	  if (m_Debug)
	    System.out
	    .println("Time to create validation predictions was: "
		+ diff);
	  
	  EnsembleSelectionLibraryModel.saveModel(dataDirectoryName,
	      this);
	  
	  if (m_Debug)
	    System.out.println("deleting lock file: "
		+ lockFileName);
	  lockFile.delete();
	  
	} else {
	  
	  if (m_Debug)
	    System.out
	    .println("Could not create lock file.  Skipping: "
		+ lockFileName);
	  throw new Exception(
	      "Could not create lock file.  Skipping: "
	      + lockFile.getName());
	  
	}
	
      }
      
    } else {
      // This branch is responsible for loading a model from a .elm file
      
      if (m_Debug)
	System.out.println("Loading model: " + modelFile.getPath());
      // now we need to check to see if the model is valid, if so then
      // load it
      Date startTime = new Date();
      
      EnsembleSelectionLibraryModel newModel = loadModel(modelFile
	  .getPath());
      
      if (!newModel.getStringRepresentation().equals(
	  getStringRepresentation()))
	throw new EnsembleModelMismatchException(
	    "String representations "
	    + newModel.getStringRepresentation() + " and "
	    + getStringRepresentation() + " not equal");
      
      if (!newModel.getChecksum().equals(getChecksum()))
	throw new EnsembleModelMismatchException("Checksums "
	    + newModel.getChecksum() + " and " + getChecksum()
	    + " not equal");
      
      if (newModel.getSeed() != getSeed())
	throw new EnsembleModelMismatchException("Seeds "
	    + newModel.getSeed() + " and " + getSeed()
	    + " not equal");
      
      if (newModel.getFolds() != getFolds())
	throw new EnsembleModelMismatchException("Folds "
	    + newModel.getFolds() + " and " + getFolds()
	    + " not equal");
      
      if (newModel.getValidationRatio() != getValidationRatio())
	throw new EnsembleModelMismatchException("Validation Ratios "
	    + newModel.getValidationRatio() + " and "
	    + getValidationRatio() + " not equal");
      
      // setFileName(modelFileName);
      
      m_models = newModel.getModels();
      m_validationPredictions = newModel.getValidationPredictions();
      
      Date endTime = new Date();
      int diff = (int) (endTime.getTime() - startTime.getTime());
      if (m_Debug)
	System.out.println("Time to load " + modelFileName + " was: "
	    + diff);
    }
  }
  
  /**
   * The purpose of this method is to "rehydrate" the classifier object fot
   * this library model from the filesystem.
   * 
   * @param workingDirectory	the working directory to use
   */
  public void rehydrateModel(String workingDirectory) {
    
    if (m_models == null) {
      
      File file = new File(workingDirectory, m_fileName);
      
      if (m_Debug)
	System.out.println("Rehydrating Model: " + file.getPath());
      EnsembleSelectionLibraryModel model = EnsembleSelectionLibraryModel
      .loadModel(file.getPath());
      
      m_models = model.getModels();
      
    }
  }
  
  /**
   * Releases the model from memory. TODO - need to be saving these so we can
   * retrieve them later!!
   */
  public void releaseModel() {
    /*
     * if (m_unsaved) { saveModel(); }
     */
    m_models = null;
  }
  
  /** 
   * Train the classifier for the specified fold on the given data
   * 
   * @param trainData	the data to train with
   * @param fold	the fold number
   * @throws Exception	if something goes wrong, e.g., out of memory
   */
  public void train(Instances trainData, int fold) throws Exception {
    if (m_models != null) {
      
      try {
	// OK, this is it... this is the point where our code surrenders
	// to the weka classifiers.
	m_models[fold].buildClassifier(trainData);
      } catch (Throwable t) {
	m_models[fold] = null;
	throw new Exception(
	    "Exception caught while training: (null could mean out of memory)"
	    + t.getMessage());
      }
      
    } else {
      throw new Exception("Cannot train: model was null");
      // TODO: throw Exception?
    }
  }
  
  /**
   * Set the seed
   * 
   * @param seed	the seed value
   */
  public void setSeed(int seed) {
    m_seed = seed;
  }
  
  /**
   * Get the seed
   * 
   * @return the seed value
   */
  public int getSeed() {
    return m_seed;
  }
  
  /**
   * Sets the validation set ratio (only meaningful if folds == 1)
   * 
   * @param validationRatio	the new ration
   */
  public void setValidationRatio(double validationRatio) {
    m_validationRatio = validationRatio;
  }
  
  /**
   * get validationRatio
   * 
   * @return		the current ratio
   */
  public double getValidationRatio() {
    return m_validationRatio;
  }
  
  /**
   * Set the number of folds for cross validation. The number of folds also
   * indicates how many classifiers will be built to represent this model.
   * 
   * @param folds	the number of folds to use
   */
  public void setFolds(int folds) {
    m_folds = folds;
  }
  
  /**
   * get the number of folds
   * 
   * @return		the current number of folds
   */
  public int getFolds() {
    return m_folds;
  }
  
  /**
   * set the checksum
   * 
   * @param instancesChecksum	the new checksum
   */
  public void setChecksum(String instancesChecksum) {
    m_checksum = instancesChecksum;
  }
  
  /**
   * get the checksum
   * 
   * @return		the current checksum
   */
  public String getChecksum() {
    return m_checksum;
  }
  
  /**
   * Returs the array of classifiers
   * 
   * @return		the current models
   */
  public Classifier[] getModels() {
    return m_models;
  }
  
  /**
   * Sets the .elm file name for this library model
   * 
   * @param fileName	the new filename
   */
  public void setFileName(String fileName) {
    m_fileName = fileName;
  }
  
  /**
   * Gets a checksum for the string defining this classifier. This is used to
   * preserve uniqueness in the classifier names.
   * 
   * @param string	the classifier definition
   * @return		the checksum string
   */
  public static String getStringChecksum(String string) {
    
    String checksumString = null;
    
    try {
      
      Adler32 checkSummer = new Adler32();
      
      byte[] utf8 = string.toString().getBytes("UTF8");
      ;
      
      checkSummer.update(utf8);
      checksumString = Long.toHexString(checkSummer.getValue());
      
    } catch (UnsupportedEncodingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    return checksumString;
  }
  
  /**
   * The purpose of this method is to get an appropriate file name for a model
   * based on its string representation of a model. All generated filenames
   * are limited to less than 128 characters and all of them will end with a
   * 64 bit checksum value of their string representation to try to maintain
   * some uniqueness of file names.
   * 
   * @param stringRepresentation	string representation of model
   * @return				unique filename
   */
  public static String getFileName(String stringRepresentation) {
    
    // Get rid of space and quote marks(windows doesn't lke them)
    String fileName = stringRepresentation.trim().replace(' ', '_')
    .replace('"', '_');
    
    if (fileName.length() > 115) {
      
      fileName = fileName.substring(0, 115);
      
    }
    
    fileName += getStringChecksum(stringRepresentation)
    + EnsembleSelectionLibraryModel.FILE_EXTENSION;
    
    return fileName;
  }
  
  /**
   * Saves the given model to the specified file.
   * 
   * @param directory	the directory to save the model to
   * @param model	the model to save
   */
  public static void saveModel(String directory,
      EnsembleSelectionLibraryModel model) {
    
    try {
      String fileName = getFileName(model.getStringRepresentation());
      
      File file = new File(directory, fileName);
      
      // System.out.println("Saving model: "+file.getPath());
      
      // model.setFileName(new String(file.getPath()));
      
      // Serialize to a file
      ObjectOutput out = new ObjectOutputStream(
	  new FileOutputStream(file));
      out.writeObject(model);
      
      out.close();
      
    } catch (IOException e) {
      
      e.printStackTrace();
    }
  }
  
  /**
   * loads the specified model
   * 
   * @param modelFilePath	the path of the model
   * @return			the model
   */
  public static EnsembleSelectionLibraryModel loadModel(String modelFilePath) {
    
    EnsembleSelectionLibraryModel model = null;
    
    try {
      
      File file = new File(modelFilePath);
      
      ObjectInputStream in = new ObjectInputStream(new FileInputStream(
	  file));
      
      model = (EnsembleSelectionLibraryModel) in.readObject();
      
      in.close();
      
    } catch (ClassNotFoundException e) {
      
      e.printStackTrace();
      
    } catch (IOException e) {
      
      e.printStackTrace();
      
    }
    
    return model;
  }
  
  /*
   * Problems persist in this code so we left it commented out. The intent was
   * to create the methods necessary for custom serialization to allow for
   * forwards/backwards compatability of .elm files accross multiple versions
   * of this classifier. The main problem however is that these methods do not
   * appear to be called. I'm not sure what the problem is, but this would be
   * a great feature. If anyone is a seasoned veteran of this serialization
   * stuff, please help!
   * 
   * private void writeObject(ObjectOutputStream stream) throws IOException {
   * //stream.defaultWriteObject(); //stream.writeObject(b);
   * 
   * //first serialize the LibraryModel fields
   * 
   * //super.writeObject(stream);
   * 
   * //now serialize the LibraryModel fields
   * 
   * stream.writeObject(m_Classifier);
   * 
   * stream.writeObject(m_DescriptionText);
   * 
   * stream.writeObject(m_ErrorText);
   * 
   * stream.writeObject(new Boolean(m_OptionsWereValid));
   * 
   * stream.writeObject(m_StringRepresentation);
   * 
   * stream.writeObject(m_models);
   * 
   * 
   * //now serialize the EnsembleLibraryModel fields //stream.writeObject(new
   * String("blah"));
   * 
   * stream.writeObject(new Integer(m_seed));
   * 
   * stream.writeObject(m_checksum);
   * 
   * stream.writeObject(new Double(m_validationRatio));
   * 
   * stream.writeObject(new Integer(m_folds));
   * 
   * stream.writeObject(m_fileName);
   * 
   * stream.writeObject(new Boolean(m_isTrained));
   * 
   * 
   * if (m_validationPredictions == null) {
   *  }
   * 
   * if (m_Debug) System.out.println("Saving
   * "+m_validationPredictions.length+" indexed array");
   * stream.writeObject(m_validationPredictions);
   *  }
   * 
   * private void readObject(ObjectInputStream stream) throws IOException,
   * ClassNotFoundException { //stream.defaultReadObject(); //b = (String)
   * stream.readObject();
   * 
   * //super.readObject(stream);
   * 
   * //deserialize the LibraryModel fields m_Classifier =
   * (Classifier)stream.readObject();
   * 
   * m_DescriptionText = (String)stream.readObject();
   * 
   * m_ErrorText = (String)stream.readObject();
   * 
   * m_OptionsWereValid = ((Boolean)stream.readObject()).booleanValue();
   * 
   * m_StringRepresentation = (String)stream.readObject();
   * 
   * 
   * 
   * //now deserialize the EnsembleLibraryModel fields m_models =
   * (Classifier[])stream.readObject();
   * 
   * m_seed = ((Integer)stream.readObject()).intValue();
   * 
   * m_checksum = (String)stream.readObject();
   * 
   * m_validationRatio = ((Double)stream.readObject()).doubleValue();
   * 
   * m_folds = ((Integer)stream.readObject()).intValue();
   * 
   * m_fileName = (String)stream.readObject();
   * 
   * m_isTrained = ((Boolean)stream.readObject()).booleanValue();
   * 
   * m_validationPredictions = (double[][])stream.readObject();
   * 
   * if (m_Debug) System.out.println("Loaded
   * "+m_validationPredictions.length+" indexed array"); }
   * 
   */
  
  /**
   * getter for validation predictions
   * 
   * @return		the current validation predictions
   */
  public double[][] getValidationPredictions() {
    return m_validationPredictions;
  }
  
  /**
   * setter for validation predictions
   * 
   * @param predictions	the new validation predictions
   */
  public void setValidationPredictions(double[][] predictions) {
    if (m_Debug)
      System.out.println("Saving validation array of size "
	  + predictions.length);
    m_validationPredictions = new double[predictions.length][];
    System.arraycopy(predictions, 0, m_validationPredictions, 0,
	predictions.length);
  }
}