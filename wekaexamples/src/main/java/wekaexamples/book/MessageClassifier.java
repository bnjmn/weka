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
 *    MessageClassifier.java
 *    Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 *
 */

package wekaexamples.book;

import weka.classifiers.Classifier;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Serializable;

/**
 * Java program for classifying short text messages into two classes 'miss'
 * and 'hit'.
 * <p/>
 * See also wiki article <a href="http://weka.wiki.sourceforge.net/MessageClassifier">MessageClassifier</a>.
 */
public class MessageClassifier
  implements Serializable {

  /** for serialization. */
  private static final long serialVersionUID = -123455813150452885L;

  /** The training data gathered so far. */
  private Instances m_Data = null;

  /** The filter used to generate the word counts. */
  private StringToWordVector m_Filter = new StringToWordVector();

  /** The actual classifier. */
  private Classifier m_Classifier = new J48();

  /** Whether the model is up to date. */
  private boolean m_UpToDate;

  /**
   * Constructs empty training dataset.
   */
  public MessageClassifier() {
    String nameOfDataset = "MessageClassificationProblem";

    // Create vector of attributes.
    FastVector attributes = new FastVector(2);

    // Add attribute for holding messages.
    attributes.addElement(new Attribute("Message", (FastVector) null));

    // Add class attribute.
    FastVector classValues = new FastVector(2);
    classValues.addElement("miss");
    classValues.addElement("hit");
    attributes.addElement(new Attribute("Class", classValues));

    // Create dataset with initial capacity of 100, and set index of class.
    m_Data = new Instances(nameOfDataset, attributes, 100);
    m_Data.setClassIndex(m_Data.numAttributes() - 1);
  }

  /**
   * Updates model using the given training message.
   * 
   * @param message	the message content
   * @param classValue	the class label
   */
  public void updateData(String message, String classValue) {
    // Make message into instance.
    Instance instance = makeInstance(message, m_Data);

    // Set class value for instance.
    instance.setClassValue(classValue);

    // Add instance to training data.
    m_Data.add(instance);
    
    m_UpToDate = false;
  }

  /**
   * Classifies a given message.
   * 
   * @param message	the message content
   * @throws Exception 	if classification fails
   */
  public void classifyMessage(String message) throws Exception {
    // Check whether classifier has been built.
    if (m_Data.numInstances() == 0)
      throw new Exception("No classifier available.");

    // Check whether classifier and filter are up to date.
    if (!m_UpToDate) {
      // Initialize filter and tell it about the input format.
      m_Filter.setInputFormat(m_Data);

      // Generate word counts from the training data.
      Instances filteredData  = Filter.useFilter(m_Data, m_Filter);

      // Rebuild classifier.
      m_Classifier.buildClassifier(filteredData);
      
      m_UpToDate = true;
    }

    // Make separate little test set so that message
    // does not get added to string attribute in m_Data.
    Instances testset = m_Data.stringFreeStructure();

    // Make message into test instance.
    Instance instance = makeInstance(message, testset);

    // Filter instance.
    m_Filter.input(instance);
    Instance filteredInstance = m_Filter.output();

    // Get index of predicted class value.
    double predicted = m_Classifier.classifyInstance(filteredInstance);

    // Output class value.
    System.err.println("Message classified as : " +
		       m_Data.classAttribute().value((int) predicted));
  }

  /**
   * Method that converts a text message into an instance.
   * 
   * @param text	the message content to convert
   * @param data	the header information
   * @return		the generated Instance
   */
  private Instance makeInstance(String text, Instances data) {
    // Create instance of length two.
    Instance instance = new Instance(2);

    // Set value for message attribute
    Attribute messageAtt = data.attribute("Message");
    instance.setValue(messageAtt, messageAtt.addStringValue(text));

    // Give instance access to attribute information from the dataset.
    instance.setDataset(data);
    
    return instance;
  }

  /**
   * Main method. The following parameters are recognized:
   * <ul>
   *   <li>
   *      <code>-m messagefile</code><br/>
   *      Points to the file containing the message to classify or use for
   *      updating the model.
   *   </li>
   *   <li>
   *      <code>-c classlabel</code><br/>
   *      The class label of the message if model is to be updated. Omit for
   *      classification of a message.
   *   </li>
   *   <li>
   *      <code>-t modelfile</code><br/>
   *      The file containing the model. If it doesn't exist, it will be
   *      created automatically.
   *   </li>
   * </ul>
   * 
   * @param args	the commandline options
   */
  public static void main(String[] args) {
    try {
      // Read message file into string.
      String messageName = Utils.getOption('m', args);
      if (messageName.length() == 0)
        throw new Exception("Must provide name of message file ('-m <file>').");
      FileReader m = new FileReader(messageName);
      StringBuffer message = new StringBuffer();
      int l;
      while ((l = m.read()) != -1)
	message.append((char) l);
      m.close();
      
      // Check if class value is given.
      String classValue = Utils.getOption('c', args);
      
      // If model file exists, read it, otherwise create new one.
      String modelName = Utils.getOption('t', args);
      if (modelName.length() == 0)
	throw new Exception("Must provide name of model file ('-t <file>').");
      MessageClassifier messageCl;
      try {
	messageCl = (MessageClassifier) SerializationHelper.read(modelName);
      }
      catch (FileNotFoundException e) {
	messageCl = new MessageClassifier();
      }
      
      // Check if there are any options left
      Utils.checkForRemainingOptions(args);
      
      // Process message.
      if (classValue.length() != 0)
        messageCl.updateData(message.toString(), classValue);
      else
        messageCl.classifyMessage(message.toString());
      
      // Save message classifier object only if it was updated.
      if (classValue.length() != 0)
	SerializationHelper.write(modelName, messageCl);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
