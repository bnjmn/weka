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
 * Copyright (C) 2006 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.meta;

import weka.classifiers.AbstractClassifierTest;
import weka.classifiers.Classifier;
import weka.classifiers.meta.ensembleSelection.EnsembleSelectionLibrary;

import java.io.File;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests EnsembleSelection. Run from the command line with:<p/>
 * java weka.classifiers.meta.EnsembleSelectionTest
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.1 $
 */
public class EnsembleSelectionTest 
  extends AbstractClassifierTest {

  /** Default root location, relative to the users home direcory. */
  private final static String DEFAULT_ROOT = "ensembleSelection/setup-1";

  /**
   * The name of the system property that can be used to override the
   * location of the reference root.
   */
  private static final String ROOT_PROPERTY = "weka.classifiers.meta.EnsembleSelection.root";

  /** Stores the root location under which output files are stored. */
  private static File ROOT;

  /** the test example setup */
  private final static String DEFAULT_SETUP = "weka/classifiers/meta/EnsembleSelectionTest.model.xml";

  /**
   * default constructor
   *
   * @param name    the name
   */
  public EnsembleSelectionTest(String name) { 
    super(name);  
  }

  /**
   * Returns a <code>File</code> corresponding to the root of the ensemble
   * output directory.
   *
   * @return the ensemble root directory (always the first one). 
   */
  private static File getRoot() {
    if (ROOT == null) {
      String root = System.getProperty(ROOT_PROPERTY);
      if (root == null) {
        root = System.getProperty("user.dir");
        ROOT = new File(root, DEFAULT_ROOT);
      }
      else {
        ROOT = new File(root);
      }
    }

    return ROOT;
  }

  /**
   * returns the root directory with the specified index
   * 
   * @param index	the index for the root dir
   * @return		the File representing the root dir
   */
  private static File getRoot(int index) {
    File	result;
    File	root;
    
    root   = getRoot();
    result = new File(root.getAbsolutePath().replaceAll("-[0-9]*$", "-" + index));
    
    return result;
  }
  
  /**
   * returns the next available root directory
   * 
   * @return		the next available root directory
   */
  private static File getNextRoot() {
    int		i;
    File	result;
    
    i = 0;
    do {
      i++;
      result = getRoot(i);
    }
    while (result.exists());
    
    return result;
  }
  
  /**
   * returns the next available root directory and creates it if it doesn't
   * exist yet
   * 
   * @return		the next available root directory
   */
  private static File getNextRoot(boolean create) {
    File	result;
    
    result = getNextRoot();
    
    if (!result.exists() && create)
      result.mkdirs();
    
    return result;
  }
  
  /** 
   * Deletes all files and subdirectories under dir.  
   * Returns true if all deletions were successful.
   * If a deletion fails, the method stops attempting 
   * to delete and returns false.
   * 
   * @param dir		the directory to delete
   * @return		true if successful
   */
  public static boolean deleteDir(File dir) {
    int		i;
    File[]	files;
    boolean	ok;
    
    if (dir.isDirectory()) {
      files = dir.listFiles();
      for (i = 0; i < files.length; i++) {
	ok = deleteDir(files[i]);
	// problem deleting directory?
	if (!ok)
	  return false;
      }
    }
    
    // The directory is now empty so delete it
    return dir.delete();
  }

  /**
   * removes all the temporary directories created during a test run
   */
  private void deleteDirs() throws Exception {
    File	root;
    int		i;
    
    root = getRoot();
    if (root.exists()) {
      i = 1;
      do {
	if (!deleteDir(root))
	  System.out.println(
	      "Couldn't delete output directory '" + root + "'!");
	i++;
	root = getRoot(i);
      }
      while (root.exists());
    }
  }
  
  /**
   * Called by JUnit before each test method. This implementation creates
   * the default classifier to test and loads a test set of Instances.
   *
   * @throws Exception if an error occurs reading the example instances.
   */
  protected void setUp() throws Exception {
    super.setUp();
    
    // delete output directories
    deleteDirs();
  }

  /** Creates a default EnsembleSelection */
  public Classifier getClassifier() {
    EnsembleSelection   cls;

    cls = new EnsembleSelection();
    cls.setWorkingDirectory(getNextRoot(true));
    try {
      cls.setLibrary(
	  new EnsembleSelectionLibrary(
	      ClassLoader.getSystemResourceAsStream(DEFAULT_SETUP)));
    }
    catch (Exception e) {
      cls.setLibrary(null);
      e.printStackTrace();
    }

    return cls;
  }

  public static Test suite() {
    return new TestSuite(EnsembleSelectionTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }
}
