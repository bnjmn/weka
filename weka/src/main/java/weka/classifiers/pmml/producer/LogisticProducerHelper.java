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
 *    LogisticProducerHelper.java
 *    Copyright (C) 2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.pmml.producer;

import java.io.StringWriter;
import java.math.BigInteger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import weka.core.Attribute;
import weka.core.Instances;
import weka.core.pmml.jaxbbindings.DATATYPE;
import weka.core.pmml.jaxbbindings.DerivedField;
import weka.core.pmml.jaxbbindings.FIELDUSAGETYPE;
import weka.core.pmml.jaxbbindings.LocalTransformations;
import weka.core.pmml.jaxbbindings.MININGFUNCTION;
import weka.core.pmml.jaxbbindings.MISSINGVALUETREATMENTMETHOD;
import weka.core.pmml.jaxbbindings.MiningField;
import weka.core.pmml.jaxbbindings.MiningSchema;
import weka.core.pmml.jaxbbindings.NormDiscrete;
import weka.core.pmml.jaxbbindings.NumericPredictor;
import weka.core.pmml.jaxbbindings.OPTYPE;
import weka.core.pmml.jaxbbindings.Output;
import weka.core.pmml.jaxbbindings.OutputField;
import weka.core.pmml.jaxbbindings.PMML;
import weka.core.pmml.jaxbbindings.REGRESSIONNORMALIZATIONMETHOD;
import weka.core.pmml.jaxbbindings.RegressionModel;
import weka.core.pmml.jaxbbindings.RegressionTable;
import weka.core.pmml.jaxbbindings.TransformationDictionary;

/**
 * Helper class for producing PMML for a Logistic classifier. Not designed to be
 * used directly - you should call toPMML() on a trained Logistic classifier.
 * 
 * @author David Persons
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class LogisticProducerHelper extends AbstractPMMLProducerHelper {

  /**
   * Produce the PMML for a Logistic classifier
   * 
   * @param train the training data used to build the Logistic model
   * @param structureAfterFiltering the structure of the training data after
   *          filtering
   * @param par the parameters of the function(s)
   * @param numClasses the number of classes in the data
   * @return the PMML for the classifier
   */
  public static String toPMML(Instances train,
    Instances structureAfterFiltering, double[][] par, int numClasses) {

    PMML pmml = initPMML();
    addDataDictionary(train, pmml);

    String currentAttrName = null;
    TransformationDictionary transformDict = null;
    LocalTransformations localTransforms = null;
    MiningSchema schema = new MiningSchema();
    for (int i = 0; i < structureAfterFiltering.numAttributes(); i++) {
      Attribute attr = structureAfterFiltering.attribute(i);
      Attribute originalAttr = train.attribute(attr.name());
      if (i == structureAfterFiltering.classIndex()) {
        schema.addMiningFields(new MiningField(attr.name(),
          FIELDUSAGETYPE.PREDICTED));
      }

      if (originalAttr == null) {
        // this must be a derived one
        if (localTransforms == null) {
          localTransforms = new LocalTransformations();
        }
        if (transformDict == null) {
          transformDict = new TransformationDictionary();
        }
        String[] nameAndValue = getNameAndValueFromUnsupervisedNominalToBinaryDerivedAttribute(
          train, attr);

        if (!nameAndValue[0].equals(currentAttrName)) {
          currentAttrName = nameAndValue[0];
          if (i != structureAfterFiltering.classIndex()) {
            // add a mining field
            int mode = (int) train.meanOrMode(train.attribute(nameAndValue[0]));
            schema.addMiningFields(new MiningField(nameAndValue[0],
              FIELDUSAGETYPE.ACTIVE, MISSINGVALUETREATMENTMETHOD.AS_MODE, train
                .attribute(nameAndValue[0]).value(mode)));
          }
        }
        DerivedField derivedfield = new DerivedField(attr.name(),
          DATATYPE.DOUBLE, OPTYPE.CONTINUOUS);
        NormDiscrete normDiscrete = new NormDiscrete(nameAndValue[0],
          nameAndValue[1]);
        derivedfield.setNormDiscrete(normDiscrete);
        transformDict.addDerivedField(derivedfield);
      } else {
        // its either already numeric or was a binary nominal one
        if (i != structureAfterFiltering.classIndex()) {
          if (originalAttr.isNumeric()) {
            String mean = "" + train.meanOrMode(originalAttr);
            schema
              .addMiningFields(new MiningField(originalAttr.name(),
                FIELDUSAGETYPE.ACTIVE, MISSINGVALUETREATMENTMETHOD.AS_MEAN,
                mean));
          } else {
            int mode = (int) train.meanOrMode(originalAttr);
            schema.addMiningFields(new MiningField(originalAttr.name(),
              FIELDUSAGETYPE.ACTIVE, MISSINGVALUETREATMENTMETHOD.AS_MODE,
              originalAttr.value(mode)));
          }
        }
      }
    }

    RegressionModel model = new RegressionModel();
    if (transformDict != null) {
      pmml.setTransformationDictionary(transformDict);
    }
    model.addContent(schema);

    model.setFunctionName(MININGFUNCTION.CLASSIFICATION);
    model.setAlgorithmName("logisticRegression");
    model.setModelType("logisticRegression");
    model.setNormalizationMethod(REGRESSIONNORMALIZATIONMETHOD.SOFTMAX);

    Output output = new Output();
    Attribute classAttribute = structureAfterFiltering.classAttribute();
    for (int i = 0; i < classAttribute.numValues(); i++) {
      OutputField outputField = new OutputField();
      outputField.setName(classAttribute.name());
      outputField.setValue(classAttribute.value(i));
      output.addOutputField(outputField);
    }
    model.addContent(output);

    for (int i = 0; i < numClasses - 1; i++) {
      RegressionTable table = new RegressionTable(structureAfterFiltering
        .classAttribute().value(i));
      // coefficients
      int j = 1;
      for (int k = 0; k < structureAfterFiltering.numAttributes(); k++) {
        if (k != structureAfterFiltering.classIndex()) {
          Attribute attr = structureAfterFiltering.attribute(k);
          table.addNumericPredictor(new NumericPredictor(attr.name(),
            BigInteger.valueOf(1), par[j][i]));
          j++;
        }
      }
      table.setIntercept(par[0][i]);
      model.addContent(table);
    }

    pmml.addAssociationModelOrBaselineModelOrClusteringModes(model);

    try {
      StringWriter sw = new StringWriter();
      JAXBContext jc = JAXBContext.newInstance(PMML.class);
      Marshaller marshaller = jc.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      marshaller.marshal(pmml, sw);
      return sw.toString();
    } catch (JAXBException e) {
      e.printStackTrace();
    }
    return "";
  }
}
