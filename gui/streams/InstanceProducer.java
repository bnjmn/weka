/*
 *    InstanceProducer.java
 *    Copyright (C) 1999 Len Trigg
 *
 */


package weka.gui.streams;

import weka.core.Instance;
import weka.core.Instances;

/** 
 * An interface for objects capable of producing streams of instances.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public interface InstanceProducer {
  
  public void addInstanceListener(InstanceListener ipl);
  
  public void removeInstanceListener(InstanceListener ipl);

  public Instances outputFormat() throws Exception;
  
  public Instance outputPeek() throws Exception;
}
