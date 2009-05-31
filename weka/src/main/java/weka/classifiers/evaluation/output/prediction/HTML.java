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
 * HTML.java
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.evaluation.output.prediction;

import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Utils;

/**
 <!-- globalinfo-start -->
 * Outputs the predictions in HTML.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -p &lt;range&gt;
 *  The range of attributes to print in addition to the classification.
 *  (default: none)</pre>
 * 
 * <pre> -distribution
 *  Whether to turn on the output of the class distribution.
 *  Only for nominal class attributes.
 *  (default: off)</pre>
 * 
 <!-- options-end -->
 *
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class HTML
  extends AbstractOutput {
  
  /** for serialization. */
  private static final long serialVersionUID = 7241252244954353300L;

  /**
   * Returns a string describing the output generator.
   * 
   * @return 		a description suitable for
   * 			displaying in the GUI
   */
  public String globalInfo() {
    return "Outputs the predictions in HTML.";
  }
  
  /**
   * Returns a short display text, to be used in comboboxes.
   * 
   * @return 		a short display text
   */
  public String getDisplay() {
    return "HTML";
  }

  /**
   * Replaces certain characters with their HTML entities.
   * 
   * @param s		the string to process
   * @return		the processed string
   */
  protected String sanitize(String s) {
    String 	result;
    
    result = s;
    result = result.replaceAll("&", "&amp;");
    result = result.replaceAll("<", "&lt;");
    result = result.replaceAll(">", "&gt;");
    result = result.replaceAll("\"", "&quot;");
    
    return result;
  }
  
  /**
   * Performs the actual printing of the header.
   */
  protected void doPrintHeader() {
    m_Buffer.append("<html>\n");
    m_Buffer.append("<head>\n");
    m_Buffer.append("<title>Predictions for dataset " + sanitize(m_Header.relationName()) + "</title>\n");
    m_Buffer.append("</head>\n");
    m_Buffer.append("<body>\n");
    m_Buffer.append("<div align=\"center\">\n");
    m_Buffer.append("<h3>Predictions for dataset " + sanitize(m_Header.relationName()) + "</h3>\n");
    m_Buffer.append("<table border=\"1\">\n");
    m_Buffer.append("<tr>\n");
    if (m_Header.classAttribute().isNominal())
      if (m_OutputDistribution)
	m_Buffer.append("<td>inst#</td><td>actual</td><td>predicted</td><td>error</td><td colspan=\"" + m_Header.classAttribute().numValues() + "\">distribution</td>");
      else
	m_Buffer.append("<td>inst#</td><td>actual</td><td>predicted</td><td>error</td><td>prediction</td>");
    else
      m_Buffer.append("<td>inst#</td><td>actual</td><td>predicted</td><td>error</td>");
    
    if (m_Attributes != null) {
      m_Buffer.append("<td>");
      boolean first = true;
      for (int i = 0; i < m_Header.numAttributes(); i++) {
        if (i == m_Header.classIndex())
          continue;

        if (m_Attributes.isInRange(i)) {
          if (!first)
            m_Buffer.append("</td><td>");
          m_Buffer.append(sanitize(m_Header.attribute(i).name()));
          first = false;
        }
      }
      m_Buffer.append("</td>");
    }
    
    m_Buffer.append("</tr>\n");
  }

  /**
   * Builds a string listing the attribute values in a specified range of indices,
   * separated by commas and enclosed in brackets.
   *
   * @param instance 	the instance to print the values from
   * @return 		a string listing values of the attributes in the range
   */
  protected String attributeValuesString(Instance instance) {
    StringBuffer text = new StringBuffer();
    if (m_Attributes != null) {
      boolean firstOutput = true;
      m_Attributes.setUpper(instance.numAttributes() - 1);
      for (int i=0; i<instance.numAttributes(); i++)
	if (m_Attributes.isInRange(i) && i != instance.classIndex()) {
	  if (!firstOutput)
	    text.append("</td>");
	  if (m_Header.attribute(i).isNumeric())
	    text.append("<td align=\"right\">");
	  else
	    text.append("<td>");
	  text.append(sanitize(instance.toString(i)));
	  firstOutput = false;
	}
      if (!firstOutput)
	text.append("</td>");
    }
    return text.toString();
  }

  /**
   * Store the prediction made by the classifier as a string.
   * 
   * @param classifier	the classifier to use
   * @param inst	the instance to generate text from
   * @param index	the index in the dataset
   * @throws Exception	if something goes wrong
   */
  protected void doPrintClassification(Classifier classifier, Instance inst, int index) throws Exception {
    int prec = m_NumDecimals;

    Instance withMissing = (Instance)inst.copy();
    withMissing.setDataset(inst.dataset());
    double predValue = ((Classifier)classifier).classifyInstance(withMissing);

    // index
    m_Buffer.append("<tr>");
    m_Buffer.append("<td>" + (index+1) + "</td>");

    if (inst.dataset().classAttribute().isNumeric()) {
      // actual
      if (inst.classIsMissing())
	m_Buffer.append("<td align=\"right\">" + "?" + "</td>");
      else
	m_Buffer.append("<td align=\"right\">" + Utils.doubleToString(inst.classValue(), prec) + "</td>");
      // predicted
      if (Instance.isMissingValue(predValue))
	m_Buffer.append("<td align=\"right\">" + "?" + "</td>");
      else
	m_Buffer.append("<td align=\"right\">" + Utils.doubleToString(predValue, prec) + "</td>");
      // error
      if (Instance.isMissingValue(predValue) || inst.classIsMissing())
	m_Buffer.append("<td align=\"right\">" + "?" + "</td>");
      else
	m_Buffer.append("<td align=\"right\">" + Utils.doubleToString(predValue - inst.classValue(), prec) + "</td>");
    } else {
      // actual
      m_Buffer.append("<td>" + ((int) inst.classValue()+1) + ":" + sanitize(inst.toString(inst.classIndex())) + "</td>");
      // predicted
      if (Instance.isMissingValue(predValue))
	m_Buffer.append("<td>" + "?" + "</td>");
      else
	m_Buffer.append("<td>" + ((int) predValue+1) + ":" + sanitize(inst.dataset().classAttribute().value((int)predValue)) + "</td>");
      // error?
      if ((int) predValue+1 != (int) inst.classValue()+1)
	m_Buffer.append("<td>" + "+" + "</td>");
      else
	m_Buffer.append("<td>" + "&nbsp;" + "</td>");
      // prediction/distribution
      if (m_OutputDistribution) {
	if (Instance.isMissingValue(predValue)) {
	  m_Buffer.append("<td>" + "?" + "</td>");
	}
	else {
	  m_Buffer.append("<td align=\"right\">");
	  double[] dist = classifier.distributionForInstance(withMissing);
	  for (int n = 0; n < dist.length; n++) {
	    if (n > 0)
	      m_Buffer.append("</td><td align=\"right\">");
	    if (n == (int) predValue)
	      m_Buffer.append("*");
            m_Buffer.append(Utils.doubleToString(dist[n], prec));
	  }
	  m_Buffer.append("</td>");
	}
      }
      else {
	if (Instance.isMissingValue(predValue))
	  m_Buffer.append("<td align=\"right\">" + "?" + "</td>");
	else
	  m_Buffer.append("<td align=\"right\">" + Utils.doubleToString(classifier.distributionForInstance(withMissing) [(int)predValue], prec) + "</td>");
      }
    }

    // attributes
    m_Buffer.append(attributeValuesString(withMissing) + "</tr>\n");
  }
  
  /**
   * Does nothing.
   */
  protected void doPrintFooter() {
    m_Buffer.append("</table>\n");
    m_Buffer.append("</div>\n");
    m_Buffer.append("</body>\n");
    m_Buffer.append("</html>\n");
  }
}
