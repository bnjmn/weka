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
 * ODFSaver.java
 * Copyright (C) 2010 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.converters;

import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.core.Capabilities.Capability;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.table.DefaultTableModel;

import org.jopendocument.dom.spreadsheet.SpreadSheet;

/**
 <!-- globalinfo-start -->
 * Writes to a destination that is in ODF spreadsheet format.<br/>
 * For instance for spreadsheets that can be read with OpenOffice.org.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 *
 * <pre> -M &lt;str&gt;
 *  The string representing a missing value.
 *  (default: ?)</pre>
 *
 * <pre> -i &lt;the input file&gt;
 *  The input file</pre>
 *
 * <pre> -o &lt;the output file&gt;
 *  The output file</pre>
 *
 <!-- options-end -->
 *
 * <p/>
 *
 * For a tutorial on ODFDOM, see: <br/>
 * <a href="http://www.langintro.com/odfdom_tutorials/create_ods.html"
 * target="_blank">http://www.langintro.com/odfdom_tutorials/create_ods.html</a>
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 * @see Saver
 */
public class ODFSaver
  extends AbstractFileSaver
  implements BatchConverter {

  /** for serialization. */
  private static final long serialVersionUID = -7446832500561589653L;

  /** The placeholder for missing values. */
  protected String m_MissingValue = "";

  /**
   * Constructor.
   */
  public ODFSaver() {
    resetOptions();
  }

  /**
   * Returns a string describing this Saver.
   *
   * @return 		a description of the Saver suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return
        "Writes to a destination that is in ODF spreadsheet format.\n"
      + "For instance for spreadsheets that can be read with OpenOffice.org.";
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector<Option> result = new Vector<Option>();

    result.addElement(new Option(
        "\tThe string representing a missing value.\n"
        + "\t(default: ?)",
        "M", 1, "-M <str>"));

    Enumeration en = super.listOptions();
    while (en.hasMoreElements())
      result.addElement((Option)en.nextElement());

    return result.elements();
  }

  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   *
   * <pre> -M &lt;str&gt;
   *  The string representing a missing value.
   *  (default: ?)</pre>
   *
   * <pre> -i &lt;the input file&gt;
   *  The input file</pre>
   *
   * <pre> -o &lt;the output file&gt;
   *  The output file</pre>
   *
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    String	tmpStr;

    super.setOptions(options);

    tmpStr = Utils.getOption('M', options);
    if (tmpStr.length() != 0)
      setMissingValue(tmpStr);
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    Vector<String>	result;
    String[]		options;
    int			i;

    result  = new Vector<String>();

    result.add("-M");
    result.add(getMissingValue());

    options = super.getOptions();
    for (i = 0; i < options.length; i++)
      result.add(options[i]);

    return result.toArray(new String[result.size()]);
  }

  /**
   * Sets the placeholder for missing values.
   *
   * @param value	the placeholder
   */
  public void setMissingValue(String value) {
    m_MissingValue = value;
  }

  /**
   * Returns the current placeholder for missing values.
   *
   * @return		the placeholder
   */
  public String getMissingValue() {
    return m_MissingValue;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return 		tip text for this property suitable for
   *         		displaying in the explorer/experimenter gui
   */
  public String missingValueTipText() {
    return "The placeholder for missing values, default is '' (empty cell).";
  }

  /**
   * Resets the Saver.
   */
  public void resetOptions() {
    super.resetOptions();

    setFileExtension(ODFLoader.FILE_EXTENSION);
    setMissingValue("");
  }

  /**
   * Returns a description of the file type.
   *
   * @return a short file description
   */
  public String getFileDescription() {
    return ODFLoader.FILE_DESCRIPTION;
  }

  /**
   * Gets all the file extensions used for this type of file.
   *
   * @return the file extensions
   */
  public String[] getFileExtensions() {
    return new String[]{ODFLoader.FILE_EXTENSION};
  }

  /**
   * Returns the Capabilities of this saver.
   *
   * @return            the capabilities of this object
   * @see               Capabilities
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.STRING_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.NUMERIC_CLASS);
    result.enable(Capability.STRING_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);
    result.enable(Capability.NO_CLASS);

    return result;
  }

  /**
   * Writes a Batch of instances.
   *
   * @throws IOException 	throws IOException if saving in batch mode
   * 				is not possible
   */
  public void writeBatch() throws IOException {
    if (getInstances() == null)
      throw new IOException("No instances to save");

    if (getRetrieval() == INCREMENTAL)
      throw new IOException("Batch and incremental saving cannot be mixed.");

    setRetrieval(BATCH);
    setWriteMode(WRITE);

    try {
      Instances data = getInstances();
      DefaultTableModel model = new DefaultTableModel(data.numInstances() + 1, data.numAttributes());  // +1 for header
      // header
      String[] header = new String[data.numAttributes()];
      for (int i = 0; i < data.numAttributes(); i++)
	header[i] = data.attribute(i).name();
      model.setColumnIdentifiers(header);
      // data
      for (int n = 0; n < data.numInstances(); n++) {
	Instance inst = data.instance(n);
	for (int i = 0; i < data.numAttributes(); i++) {
	  if (inst.isMissing(i))
	    model.setValueAt(m_MissingValue, n, i);
	  else if (inst.attribute(i).isNumeric())
	    model.setValueAt(inst.value(i), n, i);
	  else
	    model.setValueAt(inst.stringValue(i), n, i);
	}
      }
      SpreadSheet spreadsheet = SpreadSheet.createEmpty(model);
      if (retrieveFile() == null)
	spreadsheet.getPackage().save(System.out);
      else
        spreadsheet.saveAs(retrieveFile());
    }
    catch (Exception e) {
      throw new IOException(e);
    }

    setWriteMode(WAIT);
    resetWriter();
    setWriteMode(CANCEL);
  }

  /**
   * Returns the revision string.
   *
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }

  /**
   * Main method.
   *
   * @param args 	should contain the options of a Saver.
   */
  public static void main(String[] args) {
    runFileSaver(new ODFSaver(), args);
  }
}
