/*
 *    InstanceEvent.java
 *    Copyright (C) 1999 Len Trigg
 *
 */


package weka.gui.streams;

import java.util.EventObject;

/** 
 * An event encapsulating an instance stream event.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public class InstanceEvent extends EventObject {
  
  /** Specifies that the instance format is available */
  public final static int FORMAT_AVAILABLE   = 1;

  /** Specifies that an instance is available */
  public final static int INSTANCE_AVAILABLE = 2;

  /** Specifies that the batch of instances is finished */
  public final static int BATCH_FINISHED     = 3;

  private int m_ID;

  /**
   * Constructs an InstanceEvent with the specified source object and event 
   * type
   *
   * @param source the object generating the InstanceEvent
   * @param the type of the InstanceEvent
   */
  public InstanceEvent(Object source, int ID) {

    super(source);
    m_ID = ID;
  }

  /**
   * Get the event type
   *
   * @return the event type
   */
  public int getID() {

    return m_ID;
  }
}
