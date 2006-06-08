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
 *    Saver.java
 *    Copyright (C) 2004 Stefan Mutter
 *
 */

package weka.gui.beans;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.InputEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;
import java.io.Serializable;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;
import java.util.Vector;
import java.util.Enumeration;
import java.io.IOException;
import java.beans.beancontext.*;
import javax.swing.JButton;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.*;


/**
 * Saves data sets using weka.core.converter classes
 *
 * @author <a href="mailto:mutter@cs.waikato.ac.nz">Stefan Mutter</a>
 * @version $Revision: 1.2 $
 *
 */
public class Saver extends AbstractDataSink implements WekaWrapper {

  /**
   * Holds the instances to be saved
   */
  private Instances m_dataSet;

  /**
   * Holds the structure
   */
  private Instances m_structure;
  
  

  /**
   * Global info for the wrapped loader (if it exists).
   */
  protected String m_globalInfo;

  /**
   * Thread for doing IO in
   */
  private transient SaveBatchThread m_ioThread;

  /**
   * Saver
   */
  private weka.core.converters.Saver m_Saver= new ArffSaver();

  /**
   * The relation name that becomes part of the file name
   */
  private String m_fileName;
  
  
  /** Flag indicating that instances will be saved to database. Used because structure information can only be sent after a database has been configured.*/
  private boolean m_isDBSaver;
  
  
 
  /**
   * Count for structure available messages
   */
  private int m_count;

  
  
  private class SaveBatchThread extends Thread {
    private DataSink m_DS;

    public SaveBatchThread(DataSink ds) {
      m_DS= ds;
    }

    public void run() {
      try {
        m_visual.setAnimated();
        m_Saver.setInstances(m_dataSet);
        m_Saver.writeBatch();
	
      } catch (Exception ex) {
	ex.printStackTrace();
      } finally {
        block(false);
	m_visual.setStatic();
      }
    }
  }
  
  /**
   * Function used to stop code that calls acceptTrainingSet. This is 
   * needed as classifier construction is performed inside a separate
   * thread of execution.
   *
   * @param tf a <code>boolean</code> value
   */
  private synchronized void block(boolean tf) {

    if (tf) {
      try {
	if (m_ioThread.isAlive()) {
	  wait();
	  }
      } catch (InterruptedException ex) {
      }
    } else {
      notifyAll();
    }
  }

  /**
   * Global info (if it exists) for the wrapped loader
   *
   * @return the global info
   */
  public String globalInfo() {
    return m_globalInfo;
  }

  /** Contsructor */  
  public Saver() {
    super();
    setSaver(m_Saver);
    m_fileName = "";
    m_dataSet = null;
    m_count = 0;
    
  }

  

  /** Set the loader to use
   * @param saver a Saver
   */
  public void setSaver(weka.core.converters.Saver saver) {
    boolean loadImages = true;
    if (saver.getClass().getName().
	compareTo(m_Saver.getClass().getName()) == 0) {
      loadImages = false;
    }
    m_Saver = saver;
    String saverName = saver.getClass().toString();
    saverName = saverName.substring(saverName.
				      lastIndexOf('.')+1, 
				      saverName.length());
    if (loadImages) {

      if (!m_visual.loadIcons(BeanVisual.ICON_PATH+saverName+".gif",
			    BeanVisual.ICON_PATH+saverName+"_animated.gif")) {
	useDefaultVisual();
      }
    }
    m_visual.setText(saverName);

    
    // get global info
    m_globalInfo = KnowledgeFlowApp.getGlobalInfo(m_Saver);
    if(m_Saver instanceof DatabaseConverter)
        m_isDBSaver = true;
    else
        m_isDBSaver = false;
  }
  
  
  
  /** Method reacts to a dataset event and starts the writing process in batch mode
   * @param e a dataset event
   */  
  public synchronized void acceptDataSet(DataSetEvent e) {
  
      m_fileName = e.getDataSet().relationName();
      m_dataSet = e.getDataSet();
      if(e.isStructureOnly() && m_isDBSaver && ((DatabaseSaver)m_Saver).getRelationForTableName()){//
          ((DatabaseSaver)m_Saver).setTableName(m_fileName);
      }
      if(!e.isStructureOnly()){
          if(!m_isDBSaver){
            try{
                m_Saver.setDirAndPrefix(m_fileName,"");
            }catch (Exception ex){
                System.out.println(ex);
            }
          }
          saveBatch();
          System.out.println("...relation "+ m_fileName +" saved.");
      }
  }
  
  /** Method reacts to a test set event and starts the writing process in batch mode
   * @param e test set event
   */  
  public synchronized void acceptTestSet(TestSetEvent e) {
  
      m_fileName = e.getTestSet().relationName();
      m_dataSet = e.getTestSet();
      if(e.isStructureOnly() && m_isDBSaver && ((DatabaseSaver)m_Saver).getRelationForTableName()){
          ((DatabaseSaver)m_Saver).setTableName(m_fileName);
      }
      if(!e.isStructureOnly()){
          if(!m_isDBSaver){
            try{
                m_Saver.setDirAndPrefix(m_fileName,"_test_"+e.getSetNumber()+"_of_"+e.getMaxSetNumber());
            }catch (Exception ex){
                System.out.println(ex);
            }
          }
          else{
              String setName = ((DatabaseSaver)m_Saver).getTableName();
              setName = setName.replaceFirst("_[tT][eE][sS][tT]_[0-9]+_[oO][fF]_[0-9]+","");
              ((DatabaseSaver)m_Saver).setTableName(setName+"_test_"+e.getSetNumber()+"_of_"+e.getMaxSetNumber());
          }
          saveBatch();
          System.out.println("... test set "+e.getSetNumber()+" of "+e.getMaxSetNumber()+" for relation "+ m_fileName +" saved.");
      }
  }
  
  /** Method reacts to a training set event and starts the writing process in batch
   * mode
   * @param e a training set event
   */  
  public synchronized void acceptTrainingSet(TrainingSetEvent e) {
  
      m_fileName = e.getTrainingSet().relationName();
      m_dataSet = e.getTrainingSet();
      if(e.isStructureOnly() && m_isDBSaver && ((DatabaseSaver)m_Saver).getRelationForTableName()){
           ((DatabaseSaver)m_Saver).setTableName(m_fileName);
      }
      if(!e.isStructureOnly()){
          if(!m_isDBSaver){
            try{
                m_Saver.setDirAndPrefix(m_fileName,"_training_"+e.getSetNumber()+"_of_"+e.getMaxSetNumber());
            }catch (Exception ex){
                System.out.println(ex);
            }
          }
          else{
              String setName = ((DatabaseSaver)m_Saver).getTableName();
              setName = setName.replaceFirst("_[tT][rR][aA][iI][nN][iI][nN][gG]_[0-9]+_[oO][fF]_[0-9]+","");
              ((DatabaseSaver)m_Saver).setTableName(setName+"_training_"+e.getSetNumber()+"_of_"+e.getMaxSetNumber());
          }
          saveBatch();
          System.out.println("... training set "+e.getSetNumber()+" of "+e.getMaxSetNumber()+" for relation "+ m_fileName +" saved.");
      }
  }
  
  /** Saves instances in batch mode */  
  public synchronized void saveBatch(){
  
      m_Saver.setRetrieval(m_Saver.BATCH);
      m_visual.setText(m_fileName);
      m_ioThread = new SaveBatchThread(Saver.this);
      m_ioThread.setPriority(Thread.MIN_PRIORITY);
      m_ioThread.start();
      block(true);
  }
  
  /** Methods reacts to instance events and saves instances incrementally.
   * If the instance to save is null, the file is closed and the saving process is
   * ended.
   * @param e instance event
   */  
  public synchronized void acceptInstance(InstanceEvent e) {
      
      
      if(e.getStatus() == e.FORMAT_AVAILABLE){
        m_Saver.setRetrieval(m_Saver.INCREMENTAL);
        m_structure = e.getStructure();
        m_fileName = m_structure.relationName();
        m_Saver.setInstances(m_structure);
        if(m_isDBSaver)
            if(((DatabaseSaver)m_Saver).getRelationForTableName())
                ((DatabaseSaver)m_Saver).setTableName(m_fileName);
      }
      if(e.getStatus() == e.INSTANCE_AVAILABLE){
        m_visual.setAnimated();
        if(m_count == 0){
            if(!m_isDBSaver){
                try{
                    m_Saver.setDirAndPrefix(m_fileName,"");
                }catch (Exception ex){
                    System.out.println(ex);
                    m_visual.setStatic();
                }
            }
            m_count ++;
        }
        try{  
            m_visual.setText(m_fileName);
            m_Saver.writeIncremental(e.getInstance());
        } catch (Exception ex) {
            m_visual.setStatic();
            System.err.println("Instance "+e.getInstance() +" could not been saved");
            ex.printStackTrace();
        }
      }
      if(e.getStatus() == e.BATCH_FINISHED){
        try{  
            m_Saver.writeIncremental(e.getInstance());
            m_Saver.writeIncremental(null);
            //m_firstNotice = true;
            m_visual.setStatic();
            System.out.println("...relation "+ m_fileName +" saved.");
            m_count = 0;
        } catch (Exception ex) {
            m_visual.setStatic();
            System.err.println("File could not have been closed.");
            ex.printStackTrace();
        }
      }
  }
  
  

  /**
   * Get the saver
   *
   * @return a <code>weka.core.converters.Saver</code> value
   */
  public weka.core.converters.Saver getSaver() {
    return m_Saver;
  }

  /**
   * Set the saver
   *
   * @param algorithm a Saver
   */
  public void setWrappedAlgorithm(Object algorithm) 
    {

    if (!(algorithm instanceof weka.core.converters.Saver)) { 
      throw new IllegalArgumentException(algorithm.getClass()+" : incorrect "
					 +"type of algorithm (Loader)");
    }
    setSaver((weka.core.converters.Saver)algorithm);
  }

  /**
   * Get the saver
   *
   * @return a Saver
   */
  public Object getWrappedAlgorithm() {
    return getSaver();
  }

  /** Stops the bean */  
  public void stop() {
  }
  
  
  /** The main method for testing
   * @param args
   */  
  public static void main(String [] args) {
    try {
      final javax.swing.JFrame jf = new javax.swing.JFrame();
      jf.getContentPane().setLayout(new java.awt.BorderLayout());

      final Saver tv = new Saver();

      jf.getContentPane().add(tv, java.awt.BorderLayout.CENTER);
      jf.addWindowListener(new java.awt.event.WindowAdapter() {
        public void windowClosing(java.awt.event.WindowEvent e) {
          jf.dispose();
          System.exit(0);
        }
      });
      jf.setSize(800,600);
      jf.setVisible(true);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
  
}

