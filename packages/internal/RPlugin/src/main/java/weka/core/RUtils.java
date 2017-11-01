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
 *    RUtils.java
 *    Copyright (C) 2012-2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import java.util.ArrayList;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPFactor;
import org.rosuda.REngine.REXPGenericVector;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.REXPVector;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.RList;

/**
 * Static utility methods for pushing/pulling data to/from R.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @Version $Revision$
 */
public class RUtils {

  /**
   * Transfer a set of instances into a R data frame in the workspace.
   *
   * This method returns an array with the attribute names
   * and attribute values in R's cleaned-up form. The first object in
   * the array that is returned is a 1D string array with the 
   * cleaned-up attribute names. The second object in the array
   * is a 2D string array with the cleaned-up attribute values.
   * Rows for non-nominal attributes in this 2D array are null.
   * 
   * @param session the RSession to use
   * @param requester the requesting object
   * @param insts the instances to transfer
   * @param frameName the name of the data frame in R
   * @throws RSessionException if the requesting object is not the current
   *           session holder
   * @throws REngineException if a problem occurs on the R end
   * @throws REXPMismatchException if a problem occurs on the R end
   * @return the attribute names and values in R
   */
  public static Object[] instancesToDataFrame(RSession session, Object requester,
    Instances insts, String frameName) throws RSessionException,
    REngineException, REXPMismatchException {

    // checkSessionHolder(requester);

    // First, clean up all attribute names using R's make.names() function
    StringBuffer sb = new StringBuffer("make.names(c(");
    for (int i = 0; i < insts.numAttributes(); i++) {
      Attribute att = insts.attribute(i);
      if (i > 0) {
        sb.append(",");
      }
      sb.append("\"" + att.name() + "\"");
    }
    sb.append("), unique = TRUE)");
    String[] cleanedAttNames = session.parseAndEval(requester, sb.toString()).asStrings();

    // Now, clean up all values
    String[][] cleanedAttValues = new String[insts.numAttributes()][];
    for (int i = 0; i < insts.numAttributes(); i++) {
      if (insts.attribute(i).isNominal()) {
        sb = new StringBuffer("make.names(c(");
        Attribute att = insts.attribute(i);
        for (int j = 0; j < att.numValues(); j++) {
          if (j > 0) {
            sb.append(",");
          }
          sb.append("\"" + att.value(j) + "\"");

        }
        sb.append("), unique = TRUE)");
        cleanedAttValues[i] = session.parseAndEval(requester, sb.toString()).asStrings();
      }
    }

    // transfer data to R, one column at a time
    for (int i = 0; i < insts.numAttributes(); i++) {
      Attribute att = insts.attribute(i);

      if (att.isNumeric()) {
        double[] d = new double[insts.numInstances()];
        for (int j = 0; j < insts.numInstances(); j++) {
          if (insts.instance(j).isMissing(i)) {
            d[j] = REXPDouble.NA;
          } else {
            d[j] = insts.instance(j).value(i);
          }
        }
        session.assign(requester, cleanedAttNames[i], new REXPDouble(d));
      } else if (att.isNominal()) {
        int[] d = new int[insts.numInstances()];
        String[] labels = new String[att.numValues()];
        for (int j = 0; j < att.numValues(); j++) {
          labels[j] = cleanedAttValues[i][j];
        }
        for (int j = 0; j < insts.numInstances(); j++) {
          if (insts.instance(j).isMissing(i)) {
            d[j] = REXPInteger.NA;
          } else {
            d[j] = (int) insts.instance(j).value(i) + 1;
          }
        }
        session.assign(requester, cleanedAttNames[i], new REXPFactor(d, labels));
      } else if (att.isString()) {
        String[] d = new String[insts.numInstances()];
        for (int j = 0; j < insts.numInstances(); j++) {
          if (insts.instance(j).isMissing(i)) {
            d[j] = ""; // doesn't seem to be a missing value constant in
                       // REXPString
          } else {
            d[j] = insts.instance(j).stringValue(i);
          }
        }
        session.assign(requester, cleanedAttNames[i], new REXPString(d));
      }
    }

    // create the named data frame from the column objects
    // and then clean up the workspace (remove column objects)

    // first try and remove any existing data frame
    session.parseAndEval(requester, "remove(" + frameName + ")");

    // create the frame
    StringBuffer temp = new StringBuffer();
    temp.append(frameName + "=data.frame(");
    for (int i = 0; i < insts.numAttributes(); i++) {
      Attribute att = insts.attribute(i);
      temp.append("\"" + cleanedAttNames[i] + "\"" +  "=" + cleanedAttNames[i]);
      if (i < insts.numAttributes() - 1) {
        temp.append(",");
      }
    }
    temp.append(")");
    session.parseAndEval(requester, temp.toString());

    // clean up column objects
    temp = new StringBuffer();
    temp.append("remove(");
    for (int i = 0; i < insts.numAttributes(); i++) {
      Attribute att = insts.attribute(i);
      temp.append(cleanedAttNames[i]);
      if (i < insts.numAttributes() - 1) {
        temp.append(",");
      }
    }
    temp.append(")");

    // System.err.println("Executing: " + temp.toString());
    session.parseAndEval(requester, temp.toString());

    // Return cleaned-up names and values
    Object[] result = new Object[2];
    result[0] = cleanedAttNames;
    result[1] = cleanedAttValues;
    return result;
  }

  /**
   * Convert an R data frame to instances
   * 
   * @param r the result from R containing a data frame
   * @return the Instances
   * @throws Exception if the result does not contain a data frame or a problem
   *           occurs during conversion
   */
  public static Instances dataFrameToInstances(REXP r) throws Exception {

    RList frame = r.asList();

    String attributeNames[] = null;
    try {
      attributeNames = ((REXPString) ((REXPGenericVector) r)._attr().asList()
        .get("names")).asStrings();
    } catch (Exception ex) {
      attributeNames = new String[frame.values().size()];
      for (int i = 0; i < frame.values().size(); i++) {
        attributeNames[i] = "Att " + i;
      }
    }

    double[][] values = new double[frame.values().size()][];
    String[][] nominalVals = new String[frame.values().size()][];
    int i = 0;
    int numInstances = -1;
    int factorCount = 0;
    boolean nonNumericCol = false;
    for (Object columnObject : frame.values()) {
      REXPVector colVector = (REXPVector) columnObject;
      try {
        values[i] = colVector.asDoubles();
      } catch (REXPMismatchException me) {
        nonNumericCol = true;
        break;
      }

      // shift the index of nominal values to be zero-based
      if (colVector.isFactor()) {
        factorCount++;
        nominalVals[i] = colVector.asFactor().levels();
        int offset = colVector.asFactor().indexBase();
        for (int j = 0; j < values[i].length; j++) {
          values[i][j] -= offset;
        }
      }

      if (numInstances > -1 && numInstances != values[i].length) {
        throw new Exception(
          "Not all columns seem to have the same number of values!");
      }
      numInstances = values[i].length;

      i++;
    }

    if (nonNumericCol && frame.values().size() == 1) {
      // TODO perhaps handle these as string attributes
      throw new Exception("A problem occured whilst converting data frame"
        + " to instances!");
    }

    ArrayList<Attribute> atts = new ArrayList<Attribute>();
    for (i = 0; i < attributeNames.length; i++) {
      Attribute newAtt = null;
      if (nominalVals[i] != null) {
        // nominal
        ArrayList<String> nomVals = new ArrayList<String>();
        for (int j = 0; j < nominalVals[i].length; j++) {
          nomVals.add(nominalVals[i][j]);
        }
        newAtt = new Attribute(attributeNames[i], nomVals);
      } else {
        // numeric
        newAtt = new Attribute(attributeNames[i]);
      }

      atts.add(newAtt);
    }

    Instances insts = new Instances("R-data-frame", atts, numInstances);
    for (i = 0; i < numInstances; i++) {
      double[] v = new double[attributeNames.length];

      for (int j = 0; j < attributeNames.length; j++) {
        if (REXPDouble.isNA(values[j][i])) {
          v[j] = Utils.missingValue();
        } else {
          v[j] = values[j][i];
        }
      }

      Instance newInst = new DenseInstance(1.0, v);
      insts.add(newInst);
    }

    return insts;
  }

}
