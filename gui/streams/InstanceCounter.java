/*
 *    InstanceCounter.java
 *    Copyright (C) 1999 Len Trigg
 *
 */


package weka.gui.streams;

import javax.swing.JPanel;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.Serializable;

import weka.core.Instance;
import weka.core.Instances;


/** 
 * A bean that counts instances streamed to it.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public class InstanceCounter extends JPanel
  implements Serializable, InstanceListener {
  
  private JLabel m_Count_Lab;
  private int m_Count;
  private boolean m_Debug;

  public void input(Instance instance) throws Exception {
    
    if (m_Debug) {
      System.err.println("InstanceCounter::input(" + instance +")");
    }
    m_Count++;
    m_Count_Lab.setText(""+m_Count+" instances");
    repaint();
  }
  
  public void inputFormat(Instances instanceInfo) {
    
    if (m_Debug) {
      System.err.println("InstanceCounter::inputFormat()");
    }
    Instances inputInstances = new Instances(instanceInfo,0);
    m_Count = 0;
    m_Count_Lab.setText(""+m_Count+" instances");
  }

  public void setDebug(boolean debug) {
    
    m_Debug = debug;
  }
  
  public boolean getDebug() {
    
    return m_Debug;
  }

  public InstanceCounter() {
    
    m_Count = 0;
    m_Count_Lab = new JLabel("no instances");
    add(m_Count_Lab);
    //    setSize(60,40);
    setBackground(Color.lightGray);
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
	  if (m_Debug)
	    System.err.println("InstanceCounter::instanceProduced() - End of instance batch");
	  break;
	default:
	  System.err.println("InstanceCounter::instanceProduced() - unknown event type");
	  break;
	}
      } catch (Exception ex) {
	System.err.println(ex.getMessage());
      }
    } else {
      System.err.println("InstanceCounter::instanceProduced() - Unknown source object type");
    }
  }
}








