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
 *    EnsembleSelectionLibrary.java
 *    Copyright (C) 2006 Robert Jung
 *
 */

package weka.classifiers.meta.ensembleSelection;

import weka.classifiers.Classifier;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.EnsembleLibrary;
import weka.classifiers.EnsembleLibraryModel;
import weka.classifiers.meta.EnsembleSelection;
import weka.core.Instances;
import weka.core.RevisionUtils;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.Adler32;

/**
 * This class represents an ensemble library.  That is a 
 * collection of models that will be combined via the 
 * ensemble selection algorithm.  This class is responsible for
 * tracking all of the unique model specifications in the current 
 * library and trainined them when asked.  There are also methods
 * to save/load library model list files.  
 *
 * @author  Robert Jung
 * @author  David Michael
 * @version $Revision$
 */
public class EnsembleSelectionLibrary 
  extends EnsembleLibrary 
  implements Serializable {
  
  /** for serialization */
  private static final long serialVersionUID = -6444026512552917835L;

  /** the working ensemble library directory. */
  private File m_workingDirectory;
  
  /** tha name of the model list file storing the list of
   * models currently being used by the model library */
  private String m_modelListFile = null;
  
  /** the training data used to build the library.  One per fold.*/
  private Instances[] m_trainingData;
  
  /** the test data used for hillclimbing.  One per fold. */
  private Instances[] m_hillclimbData;
  
  /** the predictions of each model.  Built by trainAll.  First index is
   * for the model.  Second is for the instance.  third is for the class
   * (we use distributionForInstance).
   */
  private double[][][] m_predictions;
  
  /** the random seed used to partition the training data into
   * validation and training folds */
  private int m_seed;
  
  /** the number of folds */
  private int m_folds;
  
  /** the ratio of validation data used to train the model */
  private double m_validationRatio;
  
  /** A helper class for notifying listeners when working directory changes */
  private transient PropertyChangeSupport m_workingDirectoryPropertySupport = new PropertyChangeSupport(this);
  
  /** Whether we should print debug messages. */
  public transient boolean m_Debug = true;
  
  /**
   * Creates a default libary.  Library should be associated with 
   *
   */  
  public EnsembleSelectionLibrary() {
    super();
    
    m_workingDirectory = new File(EnsembleSelection.getDefaultWorkingDirectory());
  }
  
  /**
   * Creates a default libary.  Library should be associated with 
   * a working directory
   *
   * @param dir 		the working directory form the ensemble library
   * @param seed		the seed value
   * @param folds		the number of folds
   * @param validationRatio	the ratio to use
   */  
  public EnsembleSelectionLibrary(String dir, int seed, 
      int folds, double validationRatio) {
    
    super();
    
    if (dir != null)
      m_workingDirectory = new File(dir);
    m_seed = seed;
    m_folds = folds;
    m_validationRatio = validationRatio;
    
  }
  
  /**
   * This constructor will create a library from a model
   * list file given by the file name argument
   * 
   * @param libraryFileName	the library filename
   */
  public EnsembleSelectionLibrary(String libraryFileName) {		
    super();
    
    File libraryFile = new File(libraryFileName);
    try {
      EnsembleLibrary.loadLibrary(libraryFile, this);
    } catch (Exception e) {
      System.err.println("Could not load specified library file: "+libraryFileName);
    }
  }
  
  /**
   * This constructor will create a library from the given XML stream.
   * 
   * @param stream	the XML library stream
   */
  public EnsembleSelectionLibrary(InputStream stream) {		
    super();
    
    try {
      EnsembleLibrary.loadLibrary(stream, this);
    }
    catch (Exception e) {
      System.err.println("Could not load library from XML stream: " + e);
    }
  }
  
  /**
   * Set debug flag for the library and all its models.  The debug flag
   * determines whether we print debugging information to stdout.
   * 
   * @param debug 	if true debug mode is on
   */
  public void setDebug(boolean debug) {
    m_Debug = debug;
    
    Iterator it = getModels().iterator();
    while (it.hasNext()) {
      ((EnsembleSelectionLibraryModel)it.next()).setDebug(m_Debug);
    }
  }
  
  /**
   * Sets the validation-set ratio.  This is the portion of the
   * training set that is set aside for hillclimbing.  Note that
   * this value is ignored if we are doing cross-validation
   * (indicated by the number of folds being > 1).
   *  
   * @param validationRatio	the new ratio
   */
  public void setValidationRatio(double validationRatio) {
    m_validationRatio = validationRatio;
  }
  
  /**
   * Set the number of folds for cross validation.  If the number
   * of folds is > 1, the validation ratio is ignored.
   * 
   * @param numFolds		the number of folds to use
   */
  public void setNumFolds(int numFolds) {
    m_folds = numFolds;
  }
  
  /**
   * This method will iterate through the TreeMap of models and
   * train all models that do not currently exist (are not 
   * yet trained). 
   * <p/>
   * Returns the data set which should be used for hillclimbing.
   * <p/>
   * If training a model fails then an error will
   * be sent to stdout and that model will be removed from the 
   * TreeMap.   FIXME Should we maybe raise an exception instead?
   * 
   * @param data	the data to work on
   * @param directory 	the working directory 
   * @param algorithm	the type of algorithm
   * @return		the data that should be used for hillclimbing
   * @throws Exception	if something goes wrong
   */
  public Instances trainAll(Instances data, String directory, int algorithm) throws Exception {
    
    createWorkingDirectory(directory);
    
    //craete the directory if it doesn't already exist
    String dataDirectoryName = getDataDirectoryName(data);
    File dataDirectory = new File(directory, dataDirectoryName);
    
    if (!dataDirectory.exists()) {
      dataDirectory.mkdirs();
    }
    
    //Now create a record of all the models trained.  This will be a .mlf 
    //flat file with a file name based on the time/date of training
    //DateFormat formatter = new SimpleDateFormat("yyyy.MM.dd.HH.mm");
    //String dateString = formatter.format(new Date());
    
    //Go ahead and save in both formats just in case:
    DateFormat formatter = new SimpleDateFormat("yyyy.MM.dd.HH.mm");
    String modelListFileName = formatter.format(new Date())+"_"+size()+"_models.mlf";
    //String modelListFileName = dataDirectory.getName()+".mlf";
    File modelListFile = new File(dataDirectory.getPath(), modelListFileName);
    EnsembleLibrary.saveLibrary(modelListFile, this, null);
    
    //modelListFileName = dataDirectory.getName()+".model.xml";
    modelListFileName = formatter.format(new Date())+"_"+size()+"_models.model.xml";
    modelListFile = new File(dataDirectory.getPath(), modelListFileName);
    EnsembleLibrary.saveLibrary(modelListFile, this, null);
    
    
    //log the instances used just in case we need to know...
    String arf = data.toString();
    FileWriter f = new FileWriter(new File(dataDirectory.getPath(), dataDirectory.getName()+".arff"));
    f.write(arf);
    f.close();
    
    // m_trainingData will contain the datasets used for training models for each fold.
    m_trainingData = new Instances[m_folds];
    // m_hillclimbData will contain the dataset which we will use for hillclimbing -
    // m_hillclimbData[i] should be disjoint from m_trainingData[i].
    m_hillclimbData = new Instances[m_folds];
    // validationSet is all of the hillclimbing data from all folds, in the same
    // order as it is in m_hillclimbData
    Instances validationSet;
    if (m_folds > 1) {
      validationSet = new Instances(data, data.numInstances());  //make a new set
      //with the same capacity and header as data.
      //instances may come from CV functions in
      //different order, so we'll make sure the
      //validation set's order matches that of
      //the concatenated testCV sets
      for (int i=0; i < m_folds; ++i) {
	m_trainingData[i] = data.trainCV(m_folds, i);
	m_hillclimbData[i] = data.testCV(m_folds, i);
      }
      // If we're doing "embedded CV" we can hillclimb on
      // the entire training set, so we just put all of the hillclimbData
      // from all folds in to validationSet (making sure it's in the appropriate
      // order).
      for (int i=0; i < m_folds; ++i) {
	for (int j=0; j < m_hillclimbData[i].numInstances(); ++j) {
	  validationSet.add(m_hillclimbData[i].instance(j));
	}
      }
    }
    else {
      // Otherwise, we're not doing CV, we're just using a validation set.
      // Partition the data set in to a training set and a hillclimb set
      // based on the m_validationRatio.
      int validation_size = (int)(data.numInstances() * m_validationRatio);
      m_trainingData[0] = new Instances(data, 0, data.numInstances() - validation_size);
      m_hillclimbData[0] = new Instances(data, data.numInstances() - validation_size, validation_size);
      validationSet = m_hillclimbData[0];
    }
    
    // Now we have all the data chopped up appropriately, and we can train all models
    Iterator it = m_Models.iterator();
    int model_index = 0;
    m_predictions = new double[m_Models.size()][validationSet.numInstances()][data.numClasses()];
    
    // We'll keep a set of all the models which fail so that we can remove them from
    // our library.
    Set invalidModels = new HashSet();
    
    while (it.hasNext()) {
      // For each model,
      EnsembleSelectionLibraryModel model = (EnsembleSelectionLibraryModel)it.next();
      
      // set the appropriate options
      model.setDebug(m_Debug);
      model.setFolds(m_folds);
      model.setSeed(m_seed);
      model.setValidationRatio(m_validationRatio);
      model.setChecksum(getInstancesChecksum(data));
      
      try {
	// Create the model.  This will attempt to load the model, if it
	// alreay exists.  If it does not, it will train the model using
	// m_trainingData and cache the model's predictions for 
	// m_hillclimbData.
	model.createModel(m_trainingData, m_hillclimbData, dataDirectory.getPath(), algorithm);
      } catch (Exception e) {
	// If the model failed, print a message and add it to our set of
	// invalid models.
	System.out.println("**Couldn't create model "+model.getStringRepresentation()
	    +" because of following exception: "+e.getMessage());
	
	invalidModels.add(model);
	continue;
      }
      
      if (!invalidModels.contains(model)) {
	// If the model succeeded, add its predictions to our array
	// of predictions.  Note that the successful models' predictions
	// are packed in to the front of m_predictions.
	m_predictions[model_index] = model.getValidationPredictions();
	++model_index;
	// We no longer need it in memory, so release it.
	model.releaseModel();				
      }
      
      
    }
    
    // Remove all invalidModels from m_Models.
    it = invalidModels.iterator();
    while (it.hasNext()) {
      EnsembleSelectionLibraryModel model = (EnsembleSelectionLibraryModel)it.next();
      if (m_Debug) System.out.println("removing invalid library model: "+model.getStringRepresentation());
      m_Models.remove(model);
    }
    
    if (m_Debug) System.out.println("model index: "+model_index+" tree set size: "+m_Models.size());
    
    if (invalidModels.size() > 0) {
      // If we had any invalid models, we have some bad predictions in the back
      // of m_predictions, so we'll shrink it to the right size.
      double tmpPredictions[][][] = new double[m_Models.size()][][];
      
      for (int i = 0; i < m_Models.size(); i++) {
	tmpPredictions[i] = m_predictions[i];
      }
      m_predictions = tmpPredictions;
    }
    
    if (m_Debug) System.out.println("Finished remapping models");
    
    return validationSet;  	//Give the appropriate "hillclimb" set back to ensemble
    				//selection.  
  }
  
  /**
   * Creates the working directory associated with this library
   * 
   * @param dirName	the new directory
   */
  public void createWorkingDirectory(String dirName) {
    File directory = new File(dirName);
    
    if (!directory.exists())
      directory.mkdirs();
  }
  
  /**
   * This will remove the model associated with the given String
   * from the model libraryHashMap
   * 
   * @param modelKey	the key of the model
   */
  public void removeModel(String modelKey) {
    m_Models.remove(modelKey);  //TODO - is this really all there is to it??
  }
  
  /**
   * This method will return a Set object containing all the 
   * String representations of the models.  The iterator across
   * this Set object will return the model name in alphebetical
   * order.
   * 
   * @return		all model representations
   */
  public Set getModelNames() {
    Set names = new TreeSet();
    
    Iterator it = m_Models.iterator();
    
    while (it.hasNext()) {
      names.add(((EnsembleLibraryModel)it.next()).getStringRepresentation());
    }
    
    return names;
  }
  
  /**
   * This method will get the predictions for all the models in the
   * ensemble library.  If cross validaiton is used, then predictions
   * will be returned for the entire training set.  If cross validation
   * is not used, then predictions will only be returned for the ratio 
   * of the training set reserved for validation.
   * 
   * @return		the predictions
   */
  public double[][][] getHillclimbPredictions() {
    return m_predictions;
  }
  
  /**
   * Gets the working Directory of the ensemble library.
   *
   * @return the working directory.
   */
  public File getWorkingDirectory() {
    return m_workingDirectory;
  }
  
  /**
   * Sets the working Directory of the ensemble library.
   *
   * @param workingDirectory 	the working directory to use.
   */
  public void setWorkingDirectory(File workingDirectory) {
    m_workingDirectory = workingDirectory;
    if (m_workingDirectoryPropertySupport != null) {
      m_workingDirectoryPropertySupport.firePropertyChange(null, null, null);
    }
  }
  
  /**
   * Gets the model list file that holds the list of models
   * in the ensemble library.
   *
   * @return the working directory.
   */
  public String getModelListFile() {
    return m_modelListFile;
  }
  
  /**
   * Sets the model list file that holds the list of models
   * in the ensemble library.
   *
   * @param modelListFile 	the model list file to use
   */
  public void setModelListFile(String modelListFile) {
    m_modelListFile = modelListFile;
  }
  
  /**
   * creates a LibraryModel from a set of arguments
   * 
   * @param classifier	the classifier to use
   * @return		the generated library model
   */
  public EnsembleLibraryModel createModel(Classifier classifier) {
    EnsembleSelectionLibraryModel model = new EnsembleSelectionLibraryModel(classifier);
    model.setDebug(m_Debug);
    
    return model;
  }
  
  /**
   * This method takes a String argument defining a classifier and
   * uses it to create a base Classifier.  
   * 
   * WARNING! This method is only called when trying to craete models
   * from flat files (.mlf).  This method is highly untested and 
   * foreseeably will cause problems when trying to nest arguments
   * within multiplte meta classifiers.  To avoid any problems we
   * recommend using only XML serialization, via saving to 
   * .model.xml and using only the createModel(Classifier) method
   * above.
   * 
   * @param modelString		the classifier definition
   * @return			the generated library model
   */
  public EnsembleLibraryModel createModel(String modelString) {
    
    String[] splitString = modelString.split("\\s+");
    String className = splitString[0];
    
    String argString = modelString.replaceAll(splitString[0], "");
    String[] optionStrings = argString.split("\\s+"); 
    
    EnsembleSelectionLibraryModel model = null;
    try {
      model = new EnsembleSelectionLibraryModel(AbstractClassifier.forName(className, optionStrings));
      model.setDebug(m_Debug);
      
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    return model;
  }
  
  
  /**
   * This method takes an Instances object and returns a checksum of its
   * toString method - that is the checksum of the .arff file that would 
   * be created if the Instances object were transformed into an arff file 
   * in the file system.
   * 
   * @param instances	the data to get the checksum for
   * @return		the checksum
   */
  public static String getInstancesChecksum(Instances instances) {
    
    String checksumString = null;
    
    try {
      
      Adler32 checkSummer = new Adler32();
      
      byte[] utf8 = instances.toString().getBytes("UTF8");;
      
      checkSummer.update(utf8);
      checksumString = Long.toHexString(checkSummer.getValue());
      
    } catch (UnsupportedEncodingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    
    return checksumString;
  }
  
  /**
   * Returns the unique name for the set of instances supplied.  This is 
   * used to create a directory for all of the models corresponding to that 
   * set of instances.  This was intended as a way to keep Working Directories 
   * "organized"
   * 
   * @param instances	the data to get the directory for
   * @return		the directory
   */
  public static String getDataDirectoryName(Instances instances) {
    
    String directory = null;
    
    
    directory = new String(instances.numInstances()+
	"_instances_"+getInstancesChecksum(instances));
    
    //System.out.println("generated directory name: "+directory);
    
    return directory;
    
  }
  
  /**
   * Adds an object to the list of those that wish to be informed when the
   * eotking directory changes.
   *
   * @param listener a new listener to add to the list
   */   
  public void addWorkingDirectoryListener(PropertyChangeListener listener) {
    
    if (m_workingDirectoryPropertySupport != null) {
      m_workingDirectoryPropertySupport.addPropertyChangeListener(listener);
      
    }
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }
}
