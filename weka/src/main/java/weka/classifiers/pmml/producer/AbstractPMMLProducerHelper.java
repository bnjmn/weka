/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    AbstractPMMLProducerHelper.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.pmml.producer;

import weka.core.Attribute;
import weka.core.Instances;
import weka.core.Version;
import weka.core.pmml.jaxbbindings.Application;
import weka.core.pmml.jaxbbindings.DataDictionary;
import weka.core.pmml.jaxbbindings.DataField;
import weka.core.pmml.jaxbbindings.Header;
import weka.core.pmml.jaxbbindings.OPTYPE;
import weka.core.pmml.jaxbbindings.PMML;
import weka.core.pmml.jaxbbindings.Value;

/**
 * Abstract base class for PMMLProducer helper classes to extend.
 * 
 * @author David Persons
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public abstract class AbstractPMMLProducerHelper {

  /** PMML version that the jaxbbindings were created from */
  public static final String PMML_VERSION = "4.1";

  /**
   * Initializes a PMML object with header information.
   * 
   * @return an initialized PMML object
   */
  public static PMML initPMML() {
    PMML pmml = new PMML();
    pmml.setVersion(PMML_VERSION);
    Header header = new Header();
    header.setCopyright("WEKA");
    header.setApplication(new Application("WEKA", Version.VERSION));
    pmml.setHeader(header);

    return pmml;
  }

  /**
   * Adds a data dictionary to the supplied PMML object.
   * 
   * @param trainHeader the training data header - i.e. the header of the data
   *          that enters the buildClassifier() method of the model in question
   * @param pmml the PMML object to add the data dictionary to
   */
  public static void addDataDictionary(Instances trainHeader, PMML pmml) {
    DataDictionary dictionary = new DataDictionary();

    for (int i = 0; i < trainHeader.numAttributes(); i++) {
      String name = trainHeader.attribute(i).name();
      OPTYPE optype = getOPTYPE(trainHeader.attribute(i).type());
      DataField field = new DataField(name, optype);
      if (trainHeader.attribute(i).isNominal()) {
        for (int j = 0; j < trainHeader.attribute(i).numValues(); j++) {
          Value val = new Value(trainHeader.attribute(i).value(j));
          field.addValue(val);
        }
      }
      dictionary.addDataField(field);
    }

    pmml.setDataDictionary(dictionary);
  }

  /**
   * Returns an OPTYPE for a weka attribute type. Note that PMML only supports
   * categorical, continuous and ordinal types.
   * 
   * @param wekaType the type of the weka attribute
   * @return the PMML type
   */
  public static OPTYPE getOPTYPE(int wekaType) {
    switch (wekaType) {
    case Attribute.NUMERIC:
    case Attribute.DATE:
      return OPTYPE.CONTINUOUS;
    default:
      return OPTYPE.CATEGORICAL;
    }
  }

  /**
   * Extracts the original attribute name and value from the name of a binary
   * indicator attribute created by unsupervised NominalToBinary. Handles the
   * case where one or more equals signs might be present in the original
   * attribute name.
   * 
   * @param train the original, unfiltered training header
   * @param derived the derived attribute from which to extract the original
   *          name and value from the name created by NominalToBinary.
   * @return
   */
  public static String[] getNameAndValueFromUnsupervisedNominalToBinaryDerivedAttribute(
    Instances train, Attribute derived) {
    String[] nameAndVal = new String[2];

    // need to try and locate the equals sign that separates the attribute name
    // from the value
    boolean success = false;
    String derivedName = derived.name();
    int currentEqualsIndex = derivedName.indexOf('=');
    String leftSide = derivedName.substring(0, currentEqualsIndex);
    String rightSide = derivedName.substring(currentEqualsIndex + 1,
      derivedName.length());
    while (!success) {
      if (train.attribute(leftSide) != null) {
        nameAndVal[0] = leftSide;
        nameAndVal[1] = rightSide;
        success = true;
      } else {
        // try the next equals sign...
        leftSide += ("=" + rightSide.substring(0, rightSide.indexOf('=')));
        rightSide = rightSide.substring(rightSide.indexOf('=') + 1,
          rightSide.length());
      }
    }

    return nameAndVal;
  }

}
