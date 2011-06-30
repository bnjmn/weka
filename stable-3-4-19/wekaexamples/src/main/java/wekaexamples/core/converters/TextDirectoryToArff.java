/*
 *    TextDirectoryToArff.java
 *    Copyright (C) 2002 Richard Kirkby
 *
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

package wekaexamples.core.converters;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

/**
 * Builds an arff dataset from the documents in a given directory.
 * Assumes that the file names for the documents end with ".txt".
 *
 * Usage: <p/>
 *
 * TextDirectoryToArff &lt;directory path&gt; <p/>
 *
 * @author Richard Kirkby (rkirkby at cs.waikato.ac.nz)
 * @version $Revision$
 */
public class TextDirectoryToArff {

  public Instances createDataset(String directoryPath) throws Exception {

    FastVector atts = new FastVector(2);
    atts.addElement(new Attribute("filename", (FastVector) null));
    atts.addElement(new Attribute("contents", (FastVector) null));
    Instances data = new Instances("text_files_in_" + directoryPath, atts, 0);

    File dir = new File(directoryPath);
    String[] files = dir.list();
    for (int i = 0; i < files.length; i++) {
      if (files[i].endsWith(".txt")) {
	try {
	  double[] newInst = new double[2];
	  newInst[0] = (double)data.attribute(0).addStringValue(files[i]);
	  File txt = new File(directoryPath + File.separator + files[i]);  
	  InputStreamReader is;
	  is = new InputStreamReader(new FileInputStream(txt));
	  StringBuffer txtStr = new StringBuffer();
	  int c;
	  while ((c = is.read()) != -1) {
	    txtStr.append((char)c);
	  }
	  newInst[1] = (double)data.attribute(1).addStringValue(txtStr.toString());
	  data.add(new Instance(1.0, newInst));
	} catch (Exception e) {
	  //System.err.println("failed to convert file: " + directoryPath + File.separator + files[i]);
	}
      }
    }
    return data;
  }
  
  public static void main(String[] args) {
    
    if (args.length == 1) {
      TextDirectoryToArff tdta = new TextDirectoryToArff();
      try {
	Instances dataset = tdta.createDataset(args[0]);
	System.out.println(dataset);
      } catch (Exception e) {
	System.err.println(e.getMessage());
	e.printStackTrace();
      }
    } else {
      System.out.println("Usage: java TextDirectoryToArff <directory name>");
    }
  }
}

