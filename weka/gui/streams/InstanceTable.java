/*
 *    InstanceTable.java
 *    Copyright (C) 1999 Len Trigg
 *
 */


package weka.gui.streams;

import weka.core.Instance;
import weka.core.Instances;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Vector;
import java.io.Serializable;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;
import javax.swing.table.AbstractTableModel;

/**
 * A bean that takes a stream of instances and displays in a table.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public class InstanceTable extends JPanel
  implements Serializable, InstanceListener {
  
  private JTable m_InstanceTable;
  private boolean m_Debug;
  private boolean m_Clear;
  private String m_UpdateString;
  private Instances m_Instances;

  
  public void inputFormat(Instances instanceInfo) {
    
    if (m_Debug) {
      System.err.println("InstanceTable::inputFormat()\n"
			 + instanceInfo.toString());
    }
    m_Instances = instanceInfo;
  }

  public void input(Instance instance) throws Exception {
    
    if (m_Debug) {
      System.err.println("InstanceTable::input(" + instance +")");
    }
    m_Instances.add(instance);
  }
  
  public void batchFinished() {
    
    TableModel newModel = new AbstractTableModel() {
      public String getColumnName(int col) {
	return m_Instances.attribute(col).name();
      }
      public Class getColumnClass(int col) {
	return "".getClass();
      }
      public int getColumnCount() {
	return m_Instances.numAttributes();
      }
      public int getRowCount() {
	return m_Instances.numInstances();
      }
      public Object getValueAt(int row, int col) {
	return new String(m_Instances.instance(row).toString(col));
      }
    };
    m_InstanceTable.setModel(newModel);
    if (m_Debug) {
      System.err.println("InstanceTable::batchFinished()");
    }
  }

  public InstanceTable() {
    
    setLayout(new BorderLayout());
    m_InstanceTable = new JTable();
    add("Center", new JScrollPane(m_InstanceTable));
  }

  public void setDebug(boolean debug) {
    
    m_Debug = debug;
  }
  
  public boolean getDebug() {
    
    return m_Debug;
  }

  public void instanceProduced(InstanceEvent e) {
    
    Object source = e.getSource();
    if (source instanceof InstanceProducer) { 
      try {
	InstanceProducer a = (InstanceProducer) source;
	switch (e.getID()) {
	case InstanceEvent.FORMAT_AVAILABLE:
	  inputFormat(a.outputFormat());
	  break;
	case InstanceEvent.INSTANCE_AVAILABLE:
	  input(a.outputPeek());
	  break;
	case InstanceEvent.BATCH_FINISHED:
	  batchFinished();
	  break;
	default:
	  System.err.println("InstanceTable::instanceProduced()"
			     + " - unknown event type");
	  break;
	}
      } catch (Exception ex) {
	System.err.println(ex.getMessage());
      }
    } else {
      System.err.println("InstanceTable::instanceProduced()"
			 + " - Unknown source object type");
    }
  }
}




