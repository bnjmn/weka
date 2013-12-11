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
 * ODFLoader.java
 * Copyright (C) 2010 University of Waikato, Hamilton, New Zealand
 * Copyright (C) 2009 DZone, Inc.
 *
 */

package weka.core.converters;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Vector;

import org.jopendocument.dom.ODPackage;
import org.jopendocument.dom.ODValueType;
import org.jopendocument.dom.spreadsheet.Sheet;
import org.jopendocument.dom.spreadsheet.SpreadSheet;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.SingleIndex;
import weka.core.Utils;

/**
 * <!-- globalinfo-start --> Reads a source that is in the ODF spreadsheet
 * format.<br/>
 * For instance, a spreadsheet generated with OpenOffice.org.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -sheet &lt;index&gt;
 *  The index of the sheet to load; 'first' and 'last' are accepted as well.
 * </pre>
 * 
 * <pre>
 * -M &lt;str&gt;
 *  The string representing a missing value.
 *  (default: '')
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * For a tutorial on ODFDOM, see: <br/>
 * <a href="http://java.dzone.com/news/integrate-openoffice-java"
 * target="_blank">http://java.dzone.com/news/integrate-openoffice-java</a>
 * 
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @author Geertjan Wielenga
 * @version $Revision$
 * @see Loader
 */
public class ODFLoader extends AbstractFileLoader implements BatchConverter,
  URLSourcedLoader, OptionHandler {

  /** for serialization. */
  private static final long serialVersionUID = 9164120515718983413L;

  /** the file extension. */
  public static String FILE_EXTENSION = ".ods";

  /** the file description. */
  public static String FILE_DESCRIPTION = "ODF Spreadsheets";

  /** the url. */
  protected String m_URL = "http://";

  /** The stream for the source file. */
  protected transient InputStream m_sourceStream = null;

  /** the currently open ODF document. */
  protected SpreadSheet m_Spreadsheet;

  /** the sheet to load. */
  protected SingleIndex m_SheetIndex = new SingleIndex("first");

  /** The placeholder for missing values. */
  protected String m_MissingValue = "";

  /**
   * Returns a string describing this Loader.
   * 
   * @return a description of the Loader suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "Reads a source that is in the ODF spreadsheet format.\n"
      + "For instance, a spreadsheet generated with OpenOffice.org.";
  }

  /**
   * Returns an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result;

    result = new Vector<Option>();

    result
      .addElement(new Option(
        "\tThe index of the sheet to load; 'first' and 'last' are accepted as well.",
        "sheet", 1, "-sheet <index>"));

    result.addElement(new Option("\tThe string representing a missing value.\n"
      + "\t(default: '')", "M", 1, "-M <str>"));

    return result.elements();
  }

  /**
   * returns the options of the current setup.
   * 
   * @return the current options
   */
  @Override
  public String[] getOptions() {
    Vector<String> result;

    result = new Vector<String>();

    result.add("-sheet");
    result.add(getSheetIndex());

    result.add("-M");
    result.add(getMissingValue());

    return result.toArray(new String[result.size()]);
  }

  /**
   * Parses the options for this object.
   * <p/>
   * 
   * <!-- options-start --> Valid options are:
   * <p/>
   * 
   * <pre>
   * -sheet &lt;index&gt;
   *  The index of the sheet to load; 'first' and 'last' are accepted as well.
   * </pre>
   * 
   * <pre>
   * -M &lt;str&gt;
   *  The string representing a missing value.
   *  (default: '')
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the options to use
   * @throws Exception if setting of options fails
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    String tmpStr;

    tmpStr = Utils.getOption("sheet", options);
    if (tmpStr.length() > 0) {
      setSheetIndex(tmpStr);
    }

    tmpStr = Utils.getOption('M', options);
    if (tmpStr.length() != 0) {
      setMissingValue(tmpStr);
    }

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Sets the placeholder for missing values.
   * 
   * @param value the placeholder
   */
  public void setMissingValue(String value) {
    m_MissingValue = value;
  }

  /**
   * Returns the current placeholder for missing values.
   * 
   * @return the placeholder
   */
  public String getMissingValue() {
    return m_MissingValue;
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String missingValueTipText() {
    return "The placeholder for missing values, default is '' (empty cell).";
  }

  /**
   * Get the file extension used for JSON files.
   * 
   * @return the file extension
   */
  @Override
  public String getFileExtension() {
    return FILE_EXTENSION;
  }

  /**
   * Gets all the file extensions used for this type of file.
   * 
   * @return the file extensions
   */
  @Override
  public String[] getFileExtensions() {
    return new String[] { FILE_EXTENSION };
  }

  /**
   * Returns a description of the file type.
   * 
   * @return a short file description
   */
  @Override
  public String getFileDescription() {
    return FILE_DESCRIPTION;
  }

  /**
   * Resets the Loader ready to read a new data set.
   * 
   * @throws IOException if something goes wrong
   */
  @Override
  public void reset() throws IOException {
    m_structure = null;
    m_Spreadsheet = null;
    m_SheetIndex.setSingleIndex("first");

    setRetrieval(NONE);

    if (m_File != null) {
      setFile(new File(m_File));
    } else if ((m_URL != null) && !m_URL.equals("http://")) {
      setURL(m_URL);
    }
  }

  /**
   * Sets the index of the sheet to load.
   * 
   * @param value the index (1-based, 'first' and 'last' accepted as well)
   */
  public void setSheetIndex(String value) {
    m_SheetIndex.setSingleIndex(value);
  }

  /**
   * Returns the index of the sheet to load.
   * 
   * @return the index (1-based, 'first' and 'last' accepted as well)
   */
  public String getSheetIndex() {
    return m_SheetIndex.getSingleIndex();
  }

  /**
   * Returns the tip text for this property.
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String sheetIndexTipText() {
    return "The 1-based index of the sheet to load; 'first' and 'last' are accepted as well.";
  }

  /**
   * Resets the Loader object and sets the source of the data set to be the
   * supplied url.
   * 
   * @param url the source url.
   * @throws IOException if an error occurs
   */
  public void setSource(URL url) throws IOException {
    m_structure = null;
    m_Spreadsheet = null;

    setRetrieval(NONE);

    setSource(url.openStream());

    m_URL = url.toString();
  }

  /**
   * Set the url to load from.
   * 
   * @param url the url to load from
   * @throws IOException if the url can't be set.
   */
  @Override
  public void setURL(String url) throws IOException {
    m_URL = url;
    setSource(new URL(url));
  }

  /**
   * Return the current url.
   * 
   * @return the current url
   */
  @Override
  public String retrieveURL() {
    return m_URL;
  }

  /**
   * Resets the Loader object and sets the source of the data set to be the
   * supplied InputStream.
   * 
   * @param in the source InputStream.
   * @throws IOException if initialization of reader fails.
   */
  @Override
  public void setSource(InputStream in) throws IOException {
    m_File = (new File(System.getProperty("user.dir"))).getAbsolutePath();
    m_URL = "http://";

    m_sourceStream = new BufferedInputStream(in);
  }

  /**
   * Determines and returns (if possible) the structure (internally the header)
   * of the data set as an empty set of instances.
   * 
   * @return the structure of the data set as an empty set of Instances
   * @throws IOException if an error occurs
   */
  @Override
  public Instances getStructure() throws IOException {
    if (m_sourceStream == null) {
      throw new IOException("No source has been specified");
    }

    if (m_structure == null) {
      try {
        m_Spreadsheet = org.jopendocument.dom.spreadsheet.SpreadSheet
          .get(new ODPackage(m_sourceStream));
        m_SheetIndex.setUpper(m_Spreadsheet.getSheetCount() - 1);
        Sheet sheet = m_Spreadsheet.getSheet(m_SheetIndex.getIndex());
        ArrayList<Attribute> atts = new ArrayList<Attribute>();
        for (int i = 0; i < sheet.getColumnCount(); i++) {
          if (sheet.getCellAt(i, 0).getTextValue().length() == 0) {
            break;
          }
          String cellStr = sheet.getCellAt(i, 0).getTextValue();
          if (cellStr.length() == 0) {
            atts.add(new Attribute("column-" + (i + 1)));
          } else {
            atts.add(new Attribute(cellStr));
          }
        }
        m_structure = new Instances("WekaODF", atts, 0);
      } catch (IOException ioe) {
        // just re-throw it
        throw ioe;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    return new Instances(m_structure, 0);
  }

  /**
   * Converts the cell string into a number if possible.
   * 
   * @param cell the cell to convert
   * @return the converted value
   */
  protected Object convert(String cell) {
    Object result;

    try {
      result = Integer.parseInt(cell);
    } catch (Exception e) {
      try {
        result = Double.parseDouble(cell);
      } catch (Exception ex) {
        result = cell;
      }
    }

    return result;
  }

  /**
   * Return the full data set. If the structure hasn't yet been determined by a
   * call to getStructure then method should do so before processing the rest of
   * the data set.
   * 
   * @return the structure of the data set as an empty set of Instances
   * @throws IOException if there is no source or parsing fails
   */
  @Override
  public Instances getDataSet() throws IOException {
    if (m_sourceStream == null) {
      throw new IOException("No source has been specified");
    }

    if (getRetrieval() == INCREMENTAL) {
      throw new IOException(
        "Cannot mix getting Instances in both incremental and batch modes");
    }

    setRetrieval(BATCH);
    if (m_structure == null) {
      getStructure();
    }

    Instances result = null;

    try {
      // collect data
      Vector<Object[]> data = new Vector<Object[]>();
      boolean newHeader = false;
      int[] attType = new int[m_structure.numAttributes()];
      m_SheetIndex.setUpper(m_Spreadsheet.getSheetCount() - 1);
      Sheet sheet = m_Spreadsheet.getSheet(m_SheetIndex.getIndex());
      for (int n = 1; n < sheet.getRowCount(); n++) {
        Object[] row = new Object[m_structure.numAttributes()];
        boolean empty = true;
        for (int i = 0; i < row.length; i++) {
          row[i] = sheet.getCellAt(i, n).getValue();
          if (sheet.getCellAt(i, n).getTextValue().length() > 0) {
            empty = false;
          }
          // missing value?
          if (sheet.getCellAt(i, n).getTextValue().equals(m_MissingValue)) {
            row[i] = null;
          }
        }
        // no more data?
        if (empty) {
          break;
        }
        // analyze type
        for (int i = 0; i < row.length; i++) {
          if (sheet.getCellAt(i, n).getValueType() != ODValueType.FLOAT) {
            attType[i] = Attribute.NOMINAL;
            newHeader = true;
          }
        }
        // add row
        data.add(row);
      }

      // new structure necessary?
      if (newHeader) {
        ArrayList<Attribute> atts = new ArrayList<Attribute>();
        for (int i = 0; i < attType.length; i++) {
          if (attType[i] == Attribute.NUMERIC) {
            atts.add(new Attribute(m_structure.attribute(i).name()));
          } else if (attType[i] == Attribute.NOMINAL) {
            HashSet<String> strings = new HashSet<String>();
            for (int n = 0; n < data.size(); n++) {
              if ((data.get(n)[i] != null)
                && !data.get(n)[i].toString().equals(m_MissingValue)) {
                strings.add(data.get(n)[i].toString());
              }
            }
            ArrayList<String> attValues = new ArrayList<String>(strings);
            Collections.sort(attValues);
            atts.add(new Attribute(m_structure.attribute(i).name(), attValues));
          } else {
            throw new IllegalStateException("Unhandlded attribute type: "
              + attType[i]);
          }
        }
        m_structure = new Instances("WekaODF", atts, 0);
      }

      // generate output data
      result = new Instances(m_structure, data.size());
      for (int i = 0; i < data.size(); i++) {
        double[] values = new double[m_structure.numAttributes()];
        Object[] dataRow = data.get(i);
        for (int n = 0; n < dataRow.length; n++) {
          if (dataRow[n] == null) {
            values[n] = Utils.missingValue();
          } else if (attType[n] == Attribute.NOMINAL) {
            values[n] = m_structure.attribute(n).indexOfValue(
              dataRow[n].toString());
          } else if (attType[n] == Attribute.NUMERIC) {
            values[n] = ((Number) dataRow[n]).doubleValue();
          } else {
            throw new IllegalStateException("Unhandlded attribute type: "
              + attType[n]);
          }
        }
        Instance inst = new DenseInstance(1.0, values);
        result.add(inst);
      }

      // close the stream
      m_sourceStream.close();
    } catch (Exception ex) {
      System.err.println("Failed to load ODF document");
      ex.printStackTrace();
      // ignored
    }

    return result;
  }

  /**
   * JSONLoader is unable to process a data set incrementally.
   * 
   * @param structure ignored
   * @return never returns without throwing an exception
   * @throws IOException always. JSONLoader is unable to process a data set
   *           incrementally.
   */
  @Override
  public Instance getNextInstance(Instances structure) throws IOException {
    throw new IOException("ODFLoader can't read data sets incrementally.");
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }

  /**
   * Main method.
   * 
   * @param args should contain the name of an input file.
   */
  public static void main(String[] args) {
    runFileLoader(new ODFLoader(), args);
  }
}
