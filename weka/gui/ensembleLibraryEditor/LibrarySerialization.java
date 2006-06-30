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
 * LibrarySerialization.java
 * Copyright (C) 2006 University of Waikato, Hamilton, New Zealand
 */

package weka.gui.ensembleLibraryEditor;

import weka.core.xml.XMLBasicSerialization;

/**
 * For serializing LibraryModels.  This class uses the existing weka 
 * infrastructure to let us easily serialize classifier specifications
 * in a nice solid and stable XML format.  This class is responsible for 
 * creating the .model.xml files.  Note that this truly was a life saver
 * when we found out that saving the string representations , e.g. 
 * "weka.classifiers.meta.foobar -X 1 -Y 2"  in a flat file simply did 
 * not work for lots of different kinds of classifiers.
 * 
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.1 $
 */
public class LibrarySerialization 
  extends XMLBasicSerialization {
  
  /**
   * initializes the serialization
   * 
   * @throws Exception if initialization fails
   */
  public LibrarySerialization() throws Exception {
    super();
  }
  
  /**
   * generates internally a new XML document and clears also the IgnoreList and
   * the mappings for the Read/Write-Methods
   * 
   * @throws Exception	if something goes wrong
   */
  public void clear() throws Exception {
    super.clear();
    
    // allow
    m_Properties.addAllowed(weka.classifiers.Classifier.class, "options");
  }
}