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
 * ExcelLoader.java
 * Copyright (C) 2010-2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.converters;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.SingleIndex;
import weka.core.Utils;

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

/**
 * <!-- globalinfo-start --> Reads a source that is in the Excel spreadsheet
 * format.<br/>
 * For instance, a spreadsheet generated with the Microsoft Office Suite.
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
 *  (default is ? but empty cell is also treated as missing value)
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * For a tutorial on ExcelDOM, see: <br/>
 * <a href="http://java.dzone.com/news/integrate-openoffice-java"
 * target="_blank">http://java.dzone.com/news/integrate-openoffice-java</a>
 * 
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @author Geertjan Wielenga
 * @version $Revision$
 * @see Loader
 */
public class ExcelLoader extends AbstractFileLoader implements BatchConverter,
  URLSourcedLoader, OptionHandler {

  /** for serialization. */
  private static final long serialVersionUID = 9164120515718983413L;

  /** the binary file extension. */
  public static String FILE_EXTENSION = ".xls";

  /** the OOXML file extension. */
  public static String FILE_EXTENSION_OOXML = ".xlsx";

  /** the file description. */
  public static String FILE_DESCRIPTION = "Excel Spreadsheets";

  /** the url. */
  protected String m_URL = "http://";

  /** The stream for the source file. */
  protected transient InputStream m_sourceStream = null;

  /** the currently open Excel document. */
  protected transient Workbook m_Workbook;

  /** the sheet to load. */
  protected SingleIndex m_SheetIndex = new SingleIndex("first");

  /** The placeholder for missing values. */
  protected String m_MissingValue = "?";

  /**
   * Returns a string describing this Loader.
   * 
   * @return a description of the Loader suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "Reads a source that is in the Excel spreadsheet format.\n"
      + "For instance, a spreadsheet generated with the Microsoft Office Suite.";
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
      + "\t(default is ? but empty cell is also treated as missing value)", "M",
            1, "-M <str>"));

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
   *  (default is ? but empty cell is also treated as missing value)
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
    } else {
      setSheetIndex("first");
    }

    tmpStr = Utils.getOption('M', options);
    if (tmpStr.length() != 0) {
      setMissingValue(tmpStr);
    } else {
      setMissingValue("?");
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
    return "The placeholder for missing values, default is ? " +
            "but empty cell is also treated as missing value.";
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
    return new String[] { FILE_EXTENSION, FILE_EXTENSION_OOXML };
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
    m_Workbook = null;
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
    m_Workbook = null;

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
        m_Workbook = WorkbookFactory.create(m_sourceStream);
        m_SheetIndex.setUpper(m_Workbook.getNumberOfSheets() - 1);
        Sheet sheet = m_Workbook.getSheetAt(m_SheetIndex.getIndex());
        if (sheet.getLastRowNum() == 0) {
          throw new IllegalStateException("No rows in sheet #"
            + m_SheetIndex.getSingleIndex());
        }
        ArrayList<Attribute> atts = new ArrayList<Attribute>();
        Row row = sheet.getRow(0);
        for (int i = 0; i < row.getLastCellNum(); i++) {
          Cell cell = row.getCell(i);
          switch (cell.getCellType()) {
          case Cell.CELL_TYPE_BLANK:
          case Cell.CELL_TYPE_ERROR:
            atts.add(new Attribute("column-" + (i + 1)));
            break;
          case Cell.CELL_TYPE_NUMERIC:
            atts.add(new Attribute("" + cell.getNumericCellValue()));
            break;
          default:
            atts.add(new Attribute(cell.getStringCellValue()));
          }
        }
        m_structure = new Instances("WekaExcel", atts, 0);
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
      Sheet sheet = m_Workbook.getSheetAt(m_SheetIndex.getIndex());
      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Object[] dataRow = new Object[m_structure.numAttributes()];
        data.add(dataRow);
        Row row = sheet.getRow(i);
        for (int n = 0; n < row.getLastCellNum(); n++) {
          Cell cell = row.getCell(n);
          switch (cell.getCellType()) {
          case Cell.CELL_TYPE_BLANK:
          case Cell.CELL_TYPE_ERROR:
            dataRow[n] = null;
            break;
          case Cell.CELL_TYPE_NUMERIC:
            dataRow[n] = cell.getNumericCellValue();
            break;
          default:
            if (cell.getStringCellValue().equals(m_MissingValue)) {
              dataRow[n] = null;
            } else {
              dataRow[n] = cell.getStringCellValue();
              attType[n] = Attribute.NOMINAL;
              newHeader = true;
            }
          }
        }
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
              if (data.get(n)[i] != null) {
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
        m_structure = new Instances("WekaExcel", atts, 0);
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
              (String) dataRow[n]);
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
      m_Workbook = null;
    } catch (Exception ex) {
      System.err.println("Failed to load Excel document");
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
    throw new IOException("ExcelLoader can't read data sets incrementally.");
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
    runFileLoader(new ExcelLoader(), args);
  }
}
