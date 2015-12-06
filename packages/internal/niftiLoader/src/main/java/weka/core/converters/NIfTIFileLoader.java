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
 * NIfTIFileLoader.java
 * Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.converters;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import weka.core.*;
import weka.core.converters.nifti.Nifti1Dataset;

/**
 <!-- globalinfo-start -->
 * Reads a file in NIfTI format. It automatically decompresses the data if the extension is '.nii.gz'.<br>
 * <br>
 * A mask file can be specified as a parameter. The mask must be consistent with the main dataset and it is applied to every 2D/3D volume in the main dataset.<br>
 * <br>
 * A file with volume attributes (e.g., class labels) can also be specified as a parameter. The number of records with attributes must be the same as the number of volumes in the main dataset.<br>
 * <br>
 * The attributes are read using the loader that is specified as a third parameter. The loader must be configured appropriately to read the attribute information correctly.<br>
 * <br>
 * The readDoubleVol(short ttt) method from the Nifti1Dataset class (http://niftilib.sourceforge.net/java_api_html/Nifti1Dataset.html) is used to read the data for each volume into a sparse WEKA instance. For an LxMxN volume , the order of values in the generated instance is [(z_1, y_1, x_1), ..., (z_1, y_1, x_L), (z_1, y_2, x_1), ..., (z_1, y_M, x_L), (z_2, y_1, x_1), ..., (z_N, y_M, x_L)]. If the volume is an image and not 3D, then only x and y coordinates are used.
 * <br><br>
 <!-- globalinfo-end -->
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @see Loader
 */
 public class NIfTIFileLoader extends AbstractFileLoader
        implements BatchConverter, IncrementalConverter, OptionHandler {

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

  /** The loader to use for loading the volume attributes. */
  protected Loader m_attributeLoader = new CSVLoader();
  
  /** The Instances object with the volume attributes (if any). */
  protected Instances m_attributeData = null;

  /** Hold the source of the mask file (if there is one). */
  protected File m_maskFile = new File(System.getProperty("user.dir"));

  /** The mask data (if there is one). */
  protected double[][][] m_mask = null;

  /** Current time slot in NIfTI file, if loading in incremental mode. */
  protected int m_currentTimeSlot = 0;

  /**
   * Returns a string describing this Loader.
   *
   * @return 		a description of the Loader suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Reads a file in NIfTI format. It automatically decompresses the data if the extension is '" +
            FILE_EXTENSION_COMPRESSED + "'.\n\nA mask file can be specified as a parameter. The mask must be consistent" +
            " with the main dataset and it is applied to every 2D/3D volume in the main dataset.\n\n" +
            "A file with volume attributes (e.g., class labels) can also be specified as a parameter. " +
            "The number of records with attributes must be the same as the number of volumes in the main dataset.\n\n" +
            "The attributes are read using the loader that is specified as a third parameter. The loader must be " +
            "configured appropriately to read the attribute information correctly.\n\n" +
            "The readDoubleVol(short ttt) method from the Nifti1Dataset class" +
            " (http://niftilib.sourceforge.net/java_api_html/Nifti1Dataset.html) is used to read the data for each" +
            " volume into a sparse WEKA instance. For an LxMxN volume , the order of values in the generated instance" +
            " is [(z_1, y_1, x_1), ..., (z_1, y_1, x_L), (z_1, y_2, x_1), ..., (z_1, y_M, x_L), (z_2, y_1, x_1), ...," +
            " (z_N, y_M, x_L)]. If the volume is an image and not 3D, then only x and y coordinates are used." +
            " The loader is currently very slow.";
  }

  /**
   * String describing default attribute loader.
   */
  protected String defaultLoaderString() {

    return "weka.core.converters.CSVLoader -H -N first-last -F \" \"";
  }

  /**
   * Constructor initializes the attribute loader with default settings.
   */
  public NIfTIFileLoader() {
    try {
      String[] loaderSpec = Utils.splitOptions(defaultLoaderString());
      if (loaderSpec.length == 0) {
        throw new IllegalArgumentException("Invalid loader specification string");
      }
      String loaderName = loaderSpec[0];
      loaderSpec[0] = "";
      setAttributeLoader((Loader) Utils.forName(Loader.class, loaderName, loaderSpec));
    } catch (Exception ex) {
      System.err.println("Could not parse default loader string in NIfTIFileLoader: " + ex.getMessage());
    }
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
    result.addElement(new Option(
            "\tClass name of loader to use, followed by loader options.\n"
                    + "\t(default: " + defaultLoaderString() + ")",
            "attributeLoader", 1, "-attributeLoader <loader specification>"));

    if (getAttributeLoader() instanceof OptionHandler) {
      result.addElement(new Option("", "", 0,
              "\nOptions specific to loader " + getAttributeLoader().getClass().getName() + ":"));
      result.addAll(Collections.list(((OptionHandler) getAttributeLoader()).listOptions()));
    }
    return result.elements();
  }

  /**
   * Parses a given list of options.
   * <p>
   *
   <!-- options-start -->
   * Valid options are: <p>
   * 
   * <pre> -mask &lt;filename&gt;
   *  The mask data to apply to every volume.
   *  (default: user home directory)</pre>
   * 
   * <pre> -attributes &lt;filename&gt;
   *  The attribute data for every volume.
   *  (default: user home directory)</pre>
   * 
   * <pre> -attributeLoader &lt;loader specification&gt;
   *  Class name of loader to use, followed by loader options.
   *  (default: weka.core.converters.CSVLoader -H -N first-last -F " ")</pre>
   * 
   * <pre> 
   * Options specific to loader weka.core.converters.CSVLoader:
   * </pre>
   * 
   * <pre> -H
   *  No header row present in the data.</pre>
   * 
   * <pre> -N &lt;range&gt;
   *  The range of attributes to force type to be NOMINAL.
   *  'first' and 'last' are accepted as well.
   *  Examples: "first-last", "1,4,5-27,50-last"
   *  (default: -none-)</pre>
   * 
   * <pre> -L &lt;nominal label spec&gt;
   *  Optional specification of legal labels for nominal
   *  attributes. May be specified multiple times.
   *  Batch mode can determine this
   *  automatically (and so can incremental mode if
   *  the first in memory buffer load of instances
   *  contains an example of each legal value). The
   *  spec contains two parts separated by a ":". The
   *  first part can be a range of attribute indexes or
   *  a comma-separated list off attruibute names; the
   *  second part is a comma-separated list of labels. E.g
   *  "1,2,4-6:red,green,blue" or "att1,att2:red,green,blue"</pre>
   * 
   * <pre> -S &lt;range&gt;
   *  The range of attribute to force type to be STRING.
   *  'first' and 'last' are accepted as well.
   *  Examples: "first-last", "1,4,5-27,50-last"
   *  (default: -none-)</pre>
   * 
   * <pre> -D &lt;range&gt;
   *  The range of attribute to force type to be DATE.
   *  'first' and 'last' are accepted as well.
   *  Examples: "first-last", "1,4,5-27,50-last"
   *  (default: -none-)</pre>
   * 
   * <pre> -format &lt;date format&gt;
   *  The date formatting string to use to parse date values.
   *  (default: "yyyy-MM-dd'T'HH:mm:ss")</pre>
   * 
   * <pre> -R &lt;range&gt;
   *  The range of attribute to force type to be NUMERIC.
   *  'first' and 'last' are accepted as well.
   *  Examples: "first-last", "1,4,5-27,50-last"
   *  (default: -none-)</pre>
   * 
   * <pre> -M &lt;str&gt;
   *  The string representing a missing value.
   *  (default: ?)</pre>
   * 
   * <pre> -F &lt;separator&gt;
   *  The field separator to be used.
   *  '\t' can be used as well.
   *  (default: ',')</pre>
   * 
   * <pre> -E &lt;enclosures&gt;
   *  The enclosure character(s) to use for strings.
   *  Specify as a comma separated list (e.g. ",' (default: ",')</pre>
   * 
   * <pre> -B &lt;num&gt;
   *  The size of the in memory buffer (in rows).
   *  (default: 100)</pre>
   * 
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

    String loaderString = Utils.getOption("attributeLoader", options);
    if (loaderString.length() <= 0) {
      loaderString = defaultLoaderString();
    }
    String[] loaderSpec = Utils.splitOptions(loaderString);
    if (loaderSpec.length == 0) {
      throw new IllegalArgumentException("Invalid loader specification string");
    }
    String loaderName = loaderSpec[0];
    loaderSpec[0] = "";
    setAttributeLoader((Loader) Utils.forName(Loader.class, loaderName, loaderSpec));

    Utils.checkForRemainingOptions(options);
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

    options.add("-attributeLoader");
    Loader c = getAttributeLoader();
    if (c instanceof OptionHandler) {
      options.add(c.getClass().getName() + " " + Utils.joinOptions(((OptionHandler) c).getOptions()));
    } else {
      options.add(c.getClass().getName());
    }

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
  public String attributeLoaderTipText() {
    return "The character to use as separator for the attributes file (use '\\t' for TAB).";
  }

  /**
   * Returns the character used as column separator.
   *
   * @return the character to use
   */
  public Loader getAttributeLoader() {
    return m_attributeLoader;
  }

  /**
   * Sets the character used as column separator.
   *
   * @param value the character to use
   */
  public void setAttributeLoader(Loader value) {
    m_attributeLoader = value;
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
          m_attributeLoader.setSource(m_attributesFile);
          m_attributeData = m_attributeLoader.getDataSet();
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

    // Reset time slot in case we want to read incrementally.
    m_currentTimeSlot = 0;

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
   * Method that turns a volume at the given time slot into an instance, incorporating
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
   * Method used to read one instance at a time in incremental loading mode. Returns null
   * if no further data remains to be read.
   */
  public Instance getNextInstance(Instances structure) throws IOException {
    if (getRetrieval() == BATCH) {
      throw new IOException("Cannot mix getting instances in both incremental and batch modes");
    }
    m_structure = structure;
    setRetrieval(INCREMENTAL);

    //Have we read all the data?
    if ((m_currentTimeSlot == 0 && m_dataSet.TDIM == 0) || (m_currentTimeSlot < m_dataSet.TDIM)) {
      Instance inst = new SparseInstance(1.0, make1Darray(m_currentTimeSlot++));
      inst.setDataset(m_structure);
      return inst;
     } else {
      return null;
    }
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

