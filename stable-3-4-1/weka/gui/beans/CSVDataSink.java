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
 *    CSVDataSink.java
 *    Copyright (C) 2003 Mark Hall
 *
 */

package weka.gui.beans;

import weka.core.Instances;
import weka.core.Utils;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;
import java.util.Vector;
import java.awt.*;
import java.io.*;

/**
 * Data sink that stores instances to a comma separated values (CSV) text
 * file 
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 1.1 $
 * @since 1.0
 * @see JPanel
 * @see Serializable
 */

public class CSVDataSink extends AbstractDataSink {

  protected String m_fileStem = 
    (new File(System.getProperty("user.dir"))).getAbsolutePath();

  public CSVDataSink() {
    super();
    m_visual.setText("CSVDataSink");
  }

  /**
   * Gets the destination file stem
   *
   * @return a <code>File</code> value
   */
  public File getFileStem() {
    return new File(m_fileStem);
  }

  /**
   * Sets the destination file stem
   *
   * @param file the source file
   * @exception IOException if an error occurs
   */
  public void setFileStem(File file) throws IOException {
    m_fileStem = file.getAbsolutePath();
  }
  
  public synchronized void acceptDataSet(DataSetEvent e) {
    try {
      m_visual.setAnimated();
      Instances dataSet = e.getDataSet();
      String relationName = Utils.replaceSubstring(dataSet.relationName(),
						   " ","_");
      String fileStemCopy = new String(m_fileStem);
      File outF = new File(m_fileStem);
      if (outF.isDirectory()) {
	if (!m_fileStem.endsWith(File.separator)) {
	  fileStemCopy += File.separatorChar;
	}
      }
      outF = new File(fileStemCopy + relationName + ".csv");
      PrintWriter outW = 
	new PrintWriter(new BufferedWriter(new FileWriter(outF)));

      // print out attribute names as first row
      for (int i = 0; i < dataSet.numAttributes(); i++) {
	outW.print(dataSet.attribute(i).name());
	if (i < dataSet.numAttributes()-1) {
	  outW.print(",");
	} else {
	  outW.println();
	}
      }
      for (int i = 0; i < dataSet.numInstances(); i++) {
	outW.println(dataSet.instance(i));
      }
      outW.flush();
      outW.close();
    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      m_visual.setStatic();
    }
  }

  public void stop() {

  }
}
