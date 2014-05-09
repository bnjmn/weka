package weka.gui.beans;

import java.awt.Image;

/**
 * Interface to something that can produce an image as output
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public interface ImageProducer {

  /**
   * Get the image
   * 
   * @return the image
   */
  Image getImage();
}
