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
 * NIfTIDirectoryLoader.java
 * Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core.converters;

import java.io.File;
import java.io.IOException;
import java.util.*;

import weka.core.*;

import weka.core.converters.nifti.Nifti1Dataset;

/**
 <!-- globalinfo-start -->
 * Package for loading a directory containing MRI data in NIfTI format. The directory to be loaded must contain as many subdirectories as there are classes of MRI data. Each subdirectory name will be used as the class label for the corresponding .nii files in that subdirectory. (This is the same strategy as the one used by WEKA's TextDirectoryLoader.)<br>
 * <br>
 *  Currently, the package only reads volume information for the first time slot from each .nii file. A mask file can also be specified as a parameter. This mask is must be consistent + with the other data and is applied to every 2D/3D volume in this other data.<br>
 * <br>
 * The readDoubleVol(short ttt) method from the Nifti1Dataset class (http://niftilib.sourceforge.net/java_api_html/Nifti1Dataset.html) is used to read the data for each volume into a sparse WEKA instance (with ttt=0). For an LxMxN volume (the dimensions must be the same for each .nii file in the directory!), the order of values in the generated instance is [(z_1, y_1, x_1), ..., (z_1, y_1, x_L), (z_1, y_2, x_1), ..., (z_1, y_M, x_L), (z_2, y_1, x_1), ..., (z_N, y_M, x_L)]. If the volume is an image and not 3D, then only x and y coordinates are used.
 * <br><br>
 <!-- globalinfo-end -->
 * 
 <!-- options-start -->
 * Valid options are: <p>
 * 
 * <pre> -D
 *  Enables debug output.
 *  (default: off)</pre>
 * 
 * <pre> -mask &lt;filename&gt;
 *  The mask data to apply to every volume.
 *  (default: current directory)</pre>
 * 
 <!-- options-end -->
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @see Loader
 */
public class NIfTIDirectoryLoader extends AbstractLoader implements
  BatchConverter, IncrementalConverter, OptionHandler {

  /** for serialization */
  private static final long serialVersionUID = 3492918763718247647L;

  /** Holds the determined structure (header) of the data set. */
  protected Instances m_structure = null;

  /** Holds the source of the data set. */
  protected File m_sourceFile = new File(System.getProperty("user.dir"));

  /** Hold the source of the mask file (if there is one). */
  protected File m_maskFile = new File(System.getProperty("user.dir"));

  /** The mask data (if there is one). */
  protected double[][][] m_mask = null;

  /** whether to print some debug information */
  protected boolean m_Debug = false;

  /**
   * default constructor
   */
  public NIfTIDirectoryLoader() {
    // No instances retrieved yet
    setRetrieval(NONE);
  }

  /**
   * Returns a string describing this loader
   * 
   * @return a description of the evaluator suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "Package for loading a directory containing MRI data in NIfTI format. The directory to be loaded must" +
            " contain as many subdirectories as there are classes of MRI" +
            " data. Each subdirectory name will be used as the class label for the corresponding .nii files in that" +
            " subdirectory. (This is the same strategy as the one used by WEKA's TextDirectoryLoader.)\n\n" +
            " Currently, the package only reads volume information for the first time slot from" +
            " each .nii file. A mask file can also be specified as a parameter. This mask is must be consistent +" +
            " with the other data and is applied to every 2D/3D volume in this other data.\n\n"
            + "The readDoubleVol(short ttt) method from the Nifti1Dataset class" +
            " (http://niftilib.sourceforge.net/java_api_html/Nifti1Dataset.html) is used to read the" +
            " data for each volume into a sparse WEKA instance (with ttt=0). For an LxMxN volume (the dimensions" +
            " must be the same for each .nii file in the directory!), the order of values in the generated instance" +
            " is [(z_1, y_1, x_1), ..., (z_1, y_1, x_L), (z_1, y_2, x_1), ..., (z_1, y_M, x_L), (z_2, y_1, x_1), ...," +
            " (z_N, y_M, x_L)]. If the volume is an image and not 3D, then only x and y coordinates are used.";
  }

  /**
   * Lists the available options
   * 
   * @return an enumeration of the available options
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> result = new Vector<Option>();

    result.add(new Option("\tEnables debug output.\n" + "\t(default: off)",
      "D", 0, "-D"));

    result.add(new Option("\tThe mask data to apply to every volume.\n"
            + "\t(default: current directory)", "mask", 0, "-mask <filename>"));

    return result.elements();
  }

  /**
   * Parses a given list of options.
   * <p>
   * 
   <!-- options-start -->
   * Valid options are: <p>
   * 
   * <pre> -D
   *  Enables debug output.
   *  (default: off)</pre>
   * 
   * <pre> -mask &lt;filename&gt;
   *  The mask data to apply to every volume.
   *  (default: current directory)</pre>
   * 
   <!-- options-end -->
   * 
   * @param options the options
   * @throws Exception if options cannot be set
   */
  @Override
  public void setOptions(String[] options) throws Exception {
    setDebug(Utils.getFlag("D", options));

    setDirectory(new File(Utils.getOption("dir", options)));

    setMask(new File(Utils.getOption("mask", options)));
  }

  /**
   * Gets the setting
   * 
   * @return the current setting
   */
  @Override
  public String[] getOptions() {
    Vector<String> options = new Vector<String>();

    if (getDebug()) {
      options.add("-D");
    }

    options.add("-dir");
    options.add(getDirectory().getAbsolutePath());

    options.add("-mask");
    options.add(getMask().getAbsolutePath());

    return options.toArray(new String[options.size()]);
  }

  /**
   * Sets whether to print some debug information.
   * 
   * @param value if true additional debug information will be printed.
   */
  public void setDebug(boolean value) {
    m_Debug = value;
  }

  /**
   * Gets whether additional debug information is printed.
   * 
   * @return true if additional debug information is printed
   */
  public boolean getDebug() {
    return m_Debug;
  }

  /**
   * the tip text for this property
   *
   * @return the tip text
   */
  public String debugTipText() {
    return "Whether to print additional debug information to the console.";
  }

  /**
   * Returns a description of the file type, actually it's directories.
   * 
   * @return a short file description
   */
  public String getFileDescription() {
    return "Directories";
  }

  /**
   * the tip text for this property
   *
   * @return the tip text
   */
  public String directoryTipText() {
    return "The directory to load data from (not required when used from GUI).";
  }

  /**
   * get the Dir specified as the source
   *
   * @return the source directory
   */
  public File getDirectory() {
    return new File(m_sourceFile.getAbsolutePath());
  }

  /**
   * sets the source directory
   *
   * @param dir the source directory
   * @throws IOException if an error occurs
   */
  public void setDirectory(File dir) throws IOException {
    setSource(dir);
  }

  /**
   * the tip text for this property
   *
   * @return the tip text
   */
  public String maskTipText() {
    return "The NIfTI file to load the mask data from.";
  }

  /**
   * get the mask file
   *
   * @return the mask file
   */
  public File getMask() {
    return new File(m_maskFile.getAbsolutePath());
  }

  /**
   * sets mask file
   *
   * @param file the mask file
   * @throws IOException if an error occurs
   */
  public void setMask(File file) throws IOException {
    m_maskFile = file;
  }

  /**
   * Resets the loader ready to read a new data set
   */
  @Override
  public void reset() {
    m_structure = null;
    m_filesByClass = null;
    m_lastClassDir = 0;
    setRetrieval(NONE);
  }

  /**
   * Resets the Loader object and sets the source of the data set to be the
   * supplied File object.
   * 
   * @param dir the source directory.
   * @throws IOException if an error occurs
   */
  @Override
  public void setSource(File dir) throws IOException {
    reset();

    if (dir == null) {
      throw new IOException("Source directory object is null!");
    }

    m_sourceFile = dir;
    if (!dir.exists() || !dir.isDirectory()) {
      throw new IOException("Directory '" + dir + "' not found");
    }
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
    if (getDirectory() == null) {
      throw new IOException("No directory/source has been specified");
    }

    // determine class labels, i.e., sub-dirs
    if (m_structure == null) {

      String directoryPath = getDirectory().getAbsolutePath();
      ArrayList<Attribute> atts = new ArrayList<Attribute>();
      ArrayList<String> classes = new ArrayList<String>();

      File dir = new File(directoryPath);
      String[] subdirs = dir.list();

      Nifti1Dataset header = null;
      for (String subdir2 : subdirs) {
        File subdir = new File(directoryPath + File.separator + subdir2);
        if (subdir.isDirectory()) {
          classes.add(subdir2);
          String[] files = subdir.list();
          for (String file : files) {
            String filename = directoryPath + File.separator + subdir2 + File.separator + file;
            Nifti1Dataset data = new Nifti1Dataset(filename);
            data.readHeader();
            if (!data.exists()) {
              System.err.println("The file " + filename + " is not a valid dataset in Nifti1 format -- skipping.");
            } else {
              if (header == null) {
                header = new Nifti1Dataset();
                header.copyHeader(data);
              } else {
                if (header.XDIM != data.XDIM) {
                  throw new IOException("X dimension for " + filename + " inconsistent with previous X dimensions.");
                }
                if (header.YDIM != data.YDIM) {
                  throw new IOException("Y dimension for " + filename + " inconsistent with previous Y dimensions.");
                }
                if (header.ZDIM != data.ZDIM) {
                  throw new IOException("Z dimension for " + filename + " inconsistent with previous Z dimensions.");
                }
              }
            }
          }
        }
      }

      // Sort class values so that order is clearly defined
      Collections.sort(classes);

      // Do we have a mask file?
      m_mask = null;
      if (m_maskFile.exists() && m_maskFile.isFile()) {
        try {
          String filename = m_maskFile.getAbsolutePath();
          Nifti1Dataset data = new Nifti1Dataset(filename);
          data.readHeader();
          if (!data.exists()) {
            System.err.println("The file " + filename + " is not a valid dataset in Nifti1 format -- skipping.");
          } else {
            if (header.XDIM != data.XDIM) {
              throw new IOException("X dimension for mask in " + filename + " data X dimension.");
            }
            if (header.YDIM != data.YDIM) {
              throw new IOException("Y dimension for mask in " + filename + " data Y dimensions.");
            }
            if (header.ZDIM != data.ZDIM) {
              throw new IOException("Z dimension for mask in " + filename + " data Z dimensions.");
            }
          }
          m_mask = data.readDoubleVol((short) 0);
        } catch (Exception ex) {
          System.err.println(ex.getMessage());
          m_mask = null;
        }
      }

      // Create header info for Instances object
      if (header.ZDIM == 0) {
        for (int y = 0; y < header.YDIM; y++) {
          for (int x = 0; x < header.XDIM; x++) {
            atts.add(new Attribute("X" + x + "Y" + y));
          }
        }
      } else {
        for (int z = 0; z < header.ZDIM; z++) {
          for (int y = 0; y < header.YDIM; y++) {
            for (int x = 0; x < header.XDIM; x++) {
              atts.add(new Attribute("X" + x + "Y" + y + "Z" + z));
            }
          }
        }
      }

      atts.add(new Attribute("class", classes));

      String relName = directoryPath.replaceAll("/", "_");
      relName = relName.replaceAll("\\\\", "_").replaceAll(":", "_");
      m_structure = new Instances(relName, atts, 0);
      m_structure.setClassIndex(m_structure.numAttributes() - 1);
    }

    return m_structure;
  }

  /**
   * Method that Nifti dataset (time slot 0) into an instance, taking the mask (if any) into account.
   */
  protected double[] make1Darray(Nifti1Dataset dataSet, Instances structure, int classValue) throws IOException {

    double[][][] doubles = dataSet.readDoubleVol((short) 0);
    double[] newInst = new double[structure.numAttributes()];
    int counter = 0;
    if (dataSet.ZDIM == 0) {
      for (int y = 0; y < dataSet.YDIM; y++) {
        for (int x = 0; x < dataSet.XDIM; x++) {
          newInst[counter++] = (m_mask == null || m_mask[0][y][x] > 0) ? doubles[0][y][x] : 0.0;
        }
      }
    } else {
      for (int z = 0; z < dataSet.ZDIM; z++) {
        for (int y = 0; y < dataSet.YDIM; y++) {
          for (int x = 0; x < dataSet.XDIM; x++) {
            newInst[counter++] = (m_mask == null || m_mask[z][y][x] > 0) ? doubles[z][y][x] : 0.0;
          }
        }
      }
    }
    newInst[structure.classIndex()] = classValue;

    return newInst;
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
    if (getDirectory() == null) {
      throw new IOException("No directory/source has been specified");
    }

    String directoryPath = getDirectory().getAbsolutePath();
    ArrayList<String> classes = new ArrayList<String>();
    Enumeration<Object> enm = getStructure().classAttribute().enumerateValues();
    while (enm.hasMoreElements()) {
      Object oo = enm.nextElement();
      if (oo instanceof SerializedObject) {
        classes.add(((SerializedObject) oo).getObject().toString());
      } else {
        classes.add(oo.toString());
      }
    }

    Instances data = getStructure();
    int fileCount = 0;
    for (int k = 0; k < classes.size(); k++) {
      String subdirPath = classes.get(k);
      File subdir = new File(directoryPath + File.separator + subdirPath);
      String[] files = subdir.list();
      for (String file : files) {
        try {
          fileCount++;
          if (getDebug()) {
            System.err.println("processing " + fileCount + " : " + subdirPath + " : " + file);
          }

          String filename = directoryPath + File.separator + subdirPath + File.separator + file;
          Nifti1Dataset dataSet = new Nifti1Dataset(filename);
          dataSet.readHeader();
          if (!dataSet.exists()) {
            System.err.println("The file " + filename + " is not a valid dataset in Nifti1 format -- skipping.");
          } else {
            data.add(new SparseInstance(1.0, make1Darray(dataSet, m_structure, k)));
          }
        } catch (Exception e) {
          System.err.println("failed to convert file: " + directoryPath
            + File.separator + subdirPath + File.separator + file);
        }
      }
    }

    return data;
  }

  protected List<LinkedList<String>> m_filesByClass;
  protected int m_lastClassDir = 0;

  /**
   * Process input directories/files incrementally.
   * 
   * @param structure ignored
   * @return never returns without throwing an exception
   * @throws IOException if a problem occurs
   */
  @Override
  public Instance getNextInstance(Instances structure) throws IOException {
    // throw new
    // IOException("NIfTIDirectoryLoader can't read data sets incrementally.");

    String directoryPath = getDirectory().getAbsolutePath();
    Attribute classAtt = structure.classAttribute();
    if (m_filesByClass == null) {
      m_filesByClass = new ArrayList<LinkedList<String>>();
      for (int i = 0; i < classAtt.numValues(); i++) {
        File classDir =
          new File(directoryPath + File.separator + classAtt.value(i));
        String[] files = classDir.list();
        LinkedList<String> classDocs = new LinkedList<String>();
        for (String cd : files) {
          File txt =
            new File(directoryPath + File.separator + classAtt.value(i)
              + File.separator + cd);
          if (txt.isFile()) {
            classDocs.add(cd);
          }
        }
        m_filesByClass.add(classDocs);
      }
    }

    // cycle through the classes
    int count = 0;
    LinkedList<String> classContents = m_filesByClass.get(m_lastClassDir);
    boolean found = (classContents.size() > 0);
    while (classContents.size() == 0) {
      m_lastClassDir++;
      count++;
      if (m_lastClassDir == structure.classAttribute().numValues()) {
        m_lastClassDir = 0;
      }
      classContents = m_filesByClass.get(m_lastClassDir);
      if (classContents.size() > 0) {
        found = true; // we have an instance we can create
        break;
      }
      if (count == structure.classAttribute().numValues()) {
        break; // must be finished
      }
    }

    if (found) {
      String nextDoc = classContents.poll();

      String filename = directoryPath + File.separator
              + classAtt.value(m_lastClassDir) + File.separator + nextDoc;
      Nifti1Dataset dataSet = new Nifti1Dataset(filename);
      dataSet.readHeader();
      Instance inst = new SparseInstance(1.0, make1Darray(dataSet, structure, m_lastClassDir));
      inst.setDataset(structure);

      m_lastClassDir++;
      if (m_lastClassDir == structure.classAttribute().numValues()) {
        m_lastClassDir = 0;
      }

      return inst;
    } else {
      return null; // done!
    }
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 10857 $");
  }

  /**
   * Main method.
   * 
   * @param args should contain the name of an input file.
   */
  public static void main(String[] args) {
    if (args.length > 0) {
      try {
        NIfTIDirectoryLoader loader = new NIfTIDirectoryLoader();
        loader.setOptions(args);
        // System.out.println(loader.getDataSet());
        Instances structure = loader.getStructure();
        System.out.println(structure);
        Instance temp;
        do {
          temp = loader.getNextInstance(structure);
          if (temp != null) {
            System.out.println(temp);
          }
        } while (temp != null);
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      System.err.println("\nUsage:\n" + "\tNIfTIDirectoryLoader [options]\n"
        + "\n" + "Options:\n");

      Enumeration<Option> enm = (new NIfTIDirectoryLoader()).listOptions();
      while (enm.hasMoreElements()) {
        Option option = enm.nextElement();
        System.err.println(option.synopsis());
        System.err.println(option.description());
      }

      System.err.println();
    }
  }
}

