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
 * JSONLoader.java
 * Copyright (C) 2009-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.converters;

import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import weka.core.*;
import weka.core.converters.nifti.Nifti1Dataset;

/**
 <!-- globalinfo-start -->
 <!-- globalinfo-end -->
 */
 public class NIfTIFileLoader extends AbstractFileLoader implements BatchConverter, OptionHandler {

  /** for serialization. */
  private static final long serialVersionUID = 3764733651132196582L;

  /** the file extension. */
  public static String FILE_EXTENSION = ".nii";

  /** the extension for compressed files. */
  public static String FILE_EXTENSION_COMPRESSED = FILE_EXTENSION + ".gz";

  /** The Nifti1Dataset object to be used. */
  protected Nifti1Dataset m_dataSet = null;

  /** The file with the volume attributes (if any). */
  protected File m_attributesFile = new File(System.getProperty("user.dir"));

  /** The field separator for the file with the volume attributes. */
  protected String m_FieldSeparator = " ";

  /** The Instances object with the volume attributes (if any). */
  protected Instances m_attributeData = null;

  /** Hold the source of the mask file (if there is one). */
  protected File m_maskFile = new File(System.getProperty("user.dir"));

  /** The mask data (if there is one). */
  protected double[][][] m_mask = null;

  /**
   * Returns a string describing this Loader.
   *
   * @return 		a description of the Loader suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return
            "Reads a source that is in the .nii format. It automatically decompresses the data if the extension is '" +
                    FILE_EXTENSION_COMPRESSED + "'.";
  }

  /**
   * Lists the available options
   *
   * @return an enumeration of the available options
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> result = new Vector<Option>();

    result.add(new Option("\tThe mask data to apply to every volume.\n"
            + "\t(default: user home directory)", "mask", 0, "-mask <filename>"));

    result.add(new Option("\tThe attribute data for every volume.\n"
            + "\t(default: user home directory)", "attributes", 0, "-attributes <filename>"));

    result.addElement(new Option("\tThe field separator to be used for the attributes file.\n"
            + "\t'\\t' can be used as well.\n" + "\t(default: ' ')", "F", 1,
            "-F <separator>"));

    return result.elements();
  }

  /**
   * Parses a given list of options.
   * <p/>
   *
   <!-- options-start -->
   <!-- options-end -->
   *
   * @param options the options
   * @throws Exception if options cannot be set
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    String tmpStr = Utils.getOption("mask", options);
    if (tmpStr.length() != 0) {
      setMaskFile(new File(tmpStr));
    } else {
      setMaskFile(new File(System.getProperty("user.dir")));
    }

    tmpStr = Utils.getOption("attributes", options);
    if (tmpStr.length() != 0) {
      setAttributesFile(new File(tmpStr));
    } else {
      setAttributesFile(new File(System.getProperty("user.dir")));
    }

    tmpStr = Utils.getOption('F', options);
    if (tmpStr.length() != 0) {
      setFieldSeparator(tmpStr);
    } else {
      setFieldSeparator(" ");
    }
  }

  /**
   * Gets the setting
   *
   * @return the current setting
   */
  @Override
  public String[] getOptions() {
    Vector<String> options = new Vector<String>();

    options.add("-mask");
    options.add(getMaskFile().getAbsolutePath());

    options.add("-attributes");
    options.add(getAttributesFile().getAbsolutePath());

    options.add("-F");
    options.add(getFieldSeparator());

    return options.toArray(new String[options.size()]);
  }

  /**
   * the tip text for this property
   *
   * @return the tip text
   */
  public String attributesFileTipText() {
    return "The file with the attributes for each volume, in CSV format.";
  }

  /**
   * get the mask file
   *
   * @return the mask file
   */
  public File getAttributesFile() {
    return new File(m_attributesFile.getAbsolutePath());
  }

  /**
   * sets mask file
   *
   * @param file the mask file
   * @throws IOException if an error occurs
   */
  public void setAttributesFile(File file) throws IOException {
    m_attributesFile = file;
  }

  /**
   * Returns the tip text for this property.
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String fieldSeparatorTipText() {
    return "The character to use as separator for the attributes file (use '\\t' for TAB).";
  }

  /**
   * Returns the character used as column separator.
   *
   * @return the character to use
   */
  public String getFieldSeparator() {
    return Utils.backQuoteChars(m_FieldSeparator);
  }

  /**
   * Sets the character used as column separator.
   *
   * @param value the character to use
   */
  public void setFieldSeparator(String value) {
    m_FieldSeparator = Utils.unbackQuoteChars(value);
    if (m_FieldSeparator.length() != 1) {
      m_FieldSeparator = " ";
      System.err
              .println("Field separator can only be a single character (exception being '\t'), "
                      + "defaulting back to '" + m_FieldSeparator + "'!");
    }
  }

  /**
   * the tip text for this property
   *
   * @return the tip text
   */
  public String maskFileTipText() {
    return "The NIfTI file to load the mask data from.";
  }

  /**
   * get the mask file
   *
   * @return the mask file
   */
  public File getMaskFile() {
    return new File(m_maskFile.getAbsolutePath());
  }

  /**
   * sets mask file
   *
   * @param file the mask file
   * @throws IOException if an error occurs
   */
  public void setMaskFile(File file) throws IOException {
    m_maskFile = file;
  }

  /**
   * Get the file extension used for JSON files.
   *
   * @return 		the file extension
   */
  public String getFileExtension() {
    return FILE_EXTENSION;
  }

  /**
   * Gets all the file extensions used for this type of file.
   *
   * @return the file extensions
   */
  public String[] getFileExtensions() {
    return new String[]{FILE_EXTENSION, FILE_EXTENSION_COMPRESSED};
  }

  /**
   * Returns a description of the file type.
   *
   * @return 		a short file description
   */
  public String getFileDescription() {
    return "NIfTI .nii files";
  }

  /**
   * Resets the Loader ready to read a new data set.
   *
   * @throws IOException 	if something goes wrong
   */
  public void reset() throws IOException {
    m_structure = null;

    setRetrieval(NONE);

    if (m_File != null) {
      setFile(new File(m_File));
    }
  }

  /**
   * Resets the Loader object and sets the source of the data set to be
   * the supplied File object.
   *
   * @param file 		the source file.
   * @throws IOException 	if an error occurs
   */
  public void setSource(File file) throws IOException {
    m_structure = null;

    setRetrieval(NONE);

    if (file == null)
      throw new IOException("Source file object is null!");

    m_sourceFile = file;
    m_File       = file.getPath();
  }

  /**
   * Determines and returns (if possible) the structure (internally the
   * header) of the data set as an empty set of instances.
   *
   * @return 			the structure of the data set as an empty set
   * 				of Instances
   * @throws IOException 	if an error occurs
   */
  public Instances getStructure() throws IOException {

    if (m_sourceFile == null) {
      throw new IOException("No source has been specified");
    }

    if (m_structure == null) {
      m_dataSet = new Nifti1Dataset(m_File);
      m_dataSet.readHeader();
      if (!m_dataSet.exists()) {
        System.err.println("The file " + m_File + " is not a valid dataset in Nifti1 format -- skipping.");
      }


      // Do we have a mask file?
      m_mask = null;
      if (m_maskFile.exists() && m_maskFile.isFile()) {
        try {
          String filename = m_maskFile.getAbsolutePath();
          Nifti1Dataset mask = new Nifti1Dataset(filename);
          mask.readHeader();
          if (!mask.exists()) {
            System.err.println("The file " + filename + " is not a valid dataset in Nifti1 format -- skipping.");
          } else {
            if (mask.XDIM != m_dataSet.XDIM) {
              throw new IOException("X dimension for mask in " + filename + " not equal to data X dimension.");
            }
            if (mask.YDIM != m_dataSet.YDIM) {
              throw new IOException("Y dimension for mask in " + filename + " not equal to data Y dimension.");
            }
            if (mask.ZDIM != m_dataSet.ZDIM) {
              throw new IOException("Z dimension for mask in " + filename + " not equal to data Z dimension.");
            }
          }
          m_mask = mask.readDoubleVol((short) 0);
        } catch (Exception ex) {
          System.err.println("Skipping mask file.");
          System.err.println(ex.getMessage());
          m_mask = null;
        }
      }

      // Do we have a file with attributes?
      m_attributeData = null;
      if (m_attributesFile.exists() && m_attributesFile.isFile()) {
        try {
          CSVLoader attLoader = new CSVLoader();
          attLoader.setNoHeaderRowPresent(true);
          attLoader.setFieldSeparator(getFieldSeparator());
          attLoader.setSource(m_attributesFile);
          m_attributeData = attLoader.getDataSet();
          if ((m_dataSet.TDIM == 0 && m_attributeData.numInstances() != 1) ||
                  (m_attributeData.numInstances() != m_dataSet.TDIM)) {
            System.err.println("WARNING: Attribute information inconsistent with number of time slots in " +
                    "NIfTI dataset, ignoring attribute information");
            m_attributeData = null;
          }
        } catch (Exception ex) {
          System.err.println("Skipping attributes file.");
          System.err.println(ex.getMessage());
          m_attributeData = null;
        }
      }

      // Create header info for Instances object
      ArrayList<Attribute> atts = new ArrayList<Attribute>();
      if (m_attributeData != null) {
        for (int i = 0; i < m_attributeData.numAttributes(); i++) {
          atts.add((Attribute) m_attributeData.attribute(i).copy());
        }
      }
      if (m_dataSet.ZDIM == 0) {
        for (int y = 0; y < m_dataSet.YDIM; y++) {
          for (int x = 0; x < m_dataSet.XDIM; x++) {
            atts.add(new Attribute("X" + x + "Y" + y));
          }
        }
      } else {
        for (int z = 0; z < m_dataSet.ZDIM; z++) {
          for (int y = 0; y < m_dataSet.YDIM; y++) {
            for (int x = 0; x < m_dataSet.XDIM; x++) {
              atts.add(new Attribute("X" + x + "Y" + y + "Z" + z));
            }
          }
        }
      }

      String relName = m_File.replaceAll("/", "_");
      relName = relName.replaceAll("\\\\", "_").replaceAll(":", "_");
      m_structure = new Instances(relName, atts, 0);
      m_structure.setClassIndex(m_structure.numAttributes() - 1);
    }

    return new Instances(m_structure, 0);
  }

  /**
   * Return the full data set. If the structure hasn't yet been determined
   * by a call to getStructure then method should do so before processing
   * the rest of the data set.
   *
   * @return 			the structure of the data set as an empty
   * 				set of Instances
   * @throws IOException 	if there is no source or parsing fails
   */
  public Instances getDataSet() throws IOException {
    if (m_sourceFile == null)
      throw new IOException("No source has been specified");

    if (getRetrieval() == INCREMENTAL)
      throw new IOException("Cannot mix getting Instances in both incremental and batch modes");

    setRetrieval(BATCH);
    Instances data = getStructure();
    if (m_dataSet.TDIM == 0) {
      data.add(new SparseInstance(1.0, make1Darray(0)));
    } else {
      for (int i = 0; i < m_dataSet.TDIM; i++) {
        data.add(new SparseInstance(1.0, make1Darray(i)));
      }
    }


    return data;
  }

  /**
   * Method that turns an volume at the given time slot into an instance, incorporating
   * the corresponding information from the attribute file (if any).
   */
  protected double[] make1Darray(int timeSlot) throws IOException {

    double[][][] doubles = m_dataSet.readDoubleVol((short) timeSlot);
    double[] newInst = new double[m_structure.numAttributes()];
    int counter = 0;
    if (m_attributeData != null) {
      for (int i = 0; i < m_attributeData.numAttributes(); i++) {
        newInst[counter++] = m_attributeData.instance(timeSlot).value(i);
      }
    }
    if (m_dataSet.ZDIM == 0) {
      for (int y = 0; y < m_dataSet.YDIM; y++) {
        for (int x = 0; x < m_dataSet.XDIM; x++) {
          newInst[counter++] = (m_mask == null || m_mask[0][y][x] > 0) ? doubles[0][y][x] : 0.0;
        }
      }
    } else {
      for (int z = 0; z < m_dataSet.ZDIM; z++) {
        for (int y = 0; y < m_dataSet.YDIM; y++) {
          for (int x = 0; x < m_dataSet.XDIM; x++) {
            newInst[counter++] = (m_mask == null || m_mask[z][y][x] > 0) ? doubles[z][y][x] : 0.0;
          }
        }
      }
    }

    return newInst;
  }
  /**
   * JSONLoader is unable to process a data set incrementally.
   *
   * @param structure		ignored
   * @return 			never returns without throwing an exception
   * @throws IOException 	always. JSONLoader is unable to process a
   * 				data set incrementally.
   */
  public Instance getNextInstance(Instances structure) throws IOException {
    throw new IOException("NIfTIFileLoader can't read data sets incrementally.");
  }

  /**
   * Returns the revision string.
   *
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 8034 $");
  }

  /**
   * Main method.
   *
   * @param args 	should contain the name of an input file.
   */
  public static void main(String[] args) {
    runFileLoader(new NIfTIFileLoader(), args);
  }
}
