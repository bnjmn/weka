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
 *    SerializedInsancesSaver.java
 *    Copyright (C) 2004 Stefan Mutter
 *
 */

package weka.core.converters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.ObjectOutputStream;
import java.io.BufferedOutputStream;
import java.util.Enumeration;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;

/**
 * Serialzes to a destination.
 *
 * Valid options:
 *
 * -i input arff file <br>
 * The input filw in arff format. <p>
 *
 * -o the output file <br>
 * The output file. The prefix of the output file is sufficient. If no output file is given, Saver tries to use standard out. <p>
 *
 * @author Stefan Mutter (mutter@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 * @see Saver
 */
public class SerializedInstancesSaver extends AbstractFileSaver implements BatchConverter, IncrementalConverter {

   /** Constructor */  
  public SerializedInstancesSaver(){
  
      resetOptions();
  }
    
    
  /**
   * Returns a string describing this Saver
   * @return a description of the Saver suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Serializes the instances to a file with extension bsi.";
  }

  
  /**
   * Returns a description of the file type.
   *
   * @return a short file description
   */
  public String getFileDescription() {
    return "Serializes the instaces to a file";
  }

  /**
   * Resets the Saver 
   */
  public void resetOptions() {

    super.resetOptions();
    setFileExtension(".bsi");
  }

/**
   * Sets the destination file (and directories if necessary).
   *
   * @param file the File
   * @exception IOException always
   */
  public void setDestination(File file) throws IOException {

    boolean success = false;
    String out = file.getAbsolutePath();
    
    if(retrieveFile() != null){
        try{
            if(out.lastIndexOf(File.separatorChar) == -1)
                success = file.createNewFile();
            else{
                String outPath = out.substring(0,out.lastIndexOf(File.separatorChar));
                File dir = new File(outPath);
                if(dir.exists())
                    success = file.createNewFile();
                else{
                    dir.mkdirs();
                    success = file.createNewFile();
                }
            }
            if(success){ 
                setFile(file);
            }
        } catch(Exception ex){
            throw new IOException("Cannot create a new output file. Standard out is used.");
        } finally{
            if(!success){
                System.err.println("Cannot create a new output file. Standard out is used.");
                setFile(null); //use standard out
            }
        }
    }
  }
  
  
  
  /** Writes a Batch of instances
   * @throws IOException throws IOException if saving in batch mode is not possible
   */
  public void writeBatch() throws IOException {
  
      
      resetWriter();
      
      if(getRetrieval() == INCREMENTAL)
          throw new IOException("Batch and incremental saving cannot be mixed.");
      if(getInstances() == null)
          throw new IOException("No instances to save");
      setRetrieval(BATCH);
      if(retrieveFile() == null){
        throw new IOException("No output to standard out for serialization.");
      }
      setWriteMode(WRITE);
      ObjectOutputStream outW = 
		  new ObjectOutputStream(
		  new BufferedOutputStream(
		  new FileOutputStream(retrieveFile())));

      outW.writeObject(getInstances());
      outW.flush();
      outW.close();
      setWriteMode(WAIT);
  }

  /**
   * Main method.
   *
   * @param options should contain the options of a Saver.
   */
  public static void main(String [] options) {
      
      StringBuffer text = new StringBuffer();
      try {
	SerializedInstancesSaver ssv = new SerializedInstancesSaver();
        text.append("\n\nSerializedInstancesSaver options:\n\n");
        Enumeration enumi = ssv.listOptions();
        while (enumi.hasMoreElements()) {
            Option option = (Option)enumi.nextElement();
            text.append(option.synopsis()+'\n');
            text.append(option.description()+'\n');
        }
        try {
          ssv.setOptions(options);  
        } catch (Exception ex) {
            System.out.println("\n"+text);
            System.exit(1);
	}
        ssv.writeBatch();
      } catch (Exception ex) {
	ex.printStackTrace();
	}
      
    }
}
  
  
