/*
 *    InstanceListener.java
 *    Copyright (C) 1999 Len Trigg
 *
 */


package weka.gui.streams;

import java.util.EventListener;

/** 
 * An interface for objects interested in listening to streams of instances.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public interface InstanceListener extends EventListener {
  
  void instanceProduced(InstanceEvent e);
}
