/*
 *    SerialInstanceListener.java
 *    Copyright (C) 1998  Eibe Frank, Leonard Trigg
 *
 */


package weka.gui.streams;


/**
 * Defines an interface for objects able to produce two output streams of
 * instances.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public interface SerialInstanceListener extends java.util.EventListener {
  
  void secondInstanceProduced(InstanceEvent e);
}
