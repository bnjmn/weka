/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    JavaGDOffscreenRenderer.java
 *    Copyright (C) 2012-2014 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.rosuda.javaGD.JGDPanel;

/**
 * A class that extends the JavaGD JGDPanel class to provide off-screen
 * rendering (to BufferedImage) of graphics produced in R.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class JavaGDOffscreenRenderer extends JGDPanel implements JavaGDNotifier {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -7346329523048114317L;

  /** Singleton renderer */
  protected static JavaGDOffscreenRenderer s_renderer = new JavaGDOffscreenRenderer(
    800, 600);

  /** Listeners interested in receiving notification of graphics produced */
  protected ArrayList<JavaGDListener> m_listeners = new ArrayList<JavaGDListener>();

  /**
   * Holds the last image generated. Because there is no way to know when a
   * complete rendering process has finished - only that a batch of rendering
   * has finished. When the client releases their RSession it means that any
   * rendering process must be complete. At that time we can pass on the last
   * image created.
   */
  protected BufferedImage m_image;

  protected static boolean s_javaGD_checked = false;
  protected static boolean s_javaGD_available = false;

  public static JavaGDNotifier getJavaGDNotifier() {
    return s_renderer;
  }

  private JavaGDOffscreenRenderer(int w, int h) {
    super(w, h);
    // setSize(800, 600);
  }

  /**
   * Paints the current display to a BufferedImage
   * 
   * @param w the width of the image to paint to
   * @param h the height of the image to paint to
   * @return the BufferedImage containing the result
   */
  public synchronized BufferedImage paintToImage(int w, int h) {
    BufferedImage osi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

    /*
     * if (w != getWidth() && h != getHeight()) { setSize(w, h); initRefresh();
     * }
     */
    // setSize(800, 600);
    // initRefresh();

    java.awt.Graphics g = osi.getGraphics();
    super.paintComponent(g);

    return osi;
  }

  @Override
  public void syncDisplay(boolean finish) {
    super.syncDisplay(finish);

    if (finish) {
      m_image = paintToImage(getWidth(), getHeight());
    }
  }

  /**
   * Add a listener
   * 
   * @param l the listener to add
   */
  @Override
  public void addListener(JavaGDListener l) {
    if (!m_listeners.contains(l)) {
      m_listeners.add(l);
    }
  }

  /**
   * Remove a listener
   * 
   * @param l the listener to remove
   */
  @Override
  public void removeListener(JavaGDListener l) {
    m_listeners.remove(l);
  }

  /**
   * Notify the listeners of any cached graphics
   * 
   * @param additional a varargs list of additional listeners (beyond those
   *          maintained by this notifier) to notify.
   */
  @Override
  public void notifyListeners(JavaGDListener... additional) {
    if (m_image == null) {
      return;
    }

    List<JavaGDListener> l;

    synchronized (this) {
      l = (List<JavaGDListener>) m_listeners.clone();
    }

    for (JavaGDListener ll : l) {
      ll.imageGenerated(m_image);
    }

    if (additional != null && additional.length > 0) {
      for (JavaGDListener ll : additional) {
        // don't notify any additional listeners twice!
        if (!l.contains(ll)) {
          ll.imageGenerated(m_image);
        }
      }
    }

    // all interested parties have the last generated image now
    m_image = null;
  }

  /**
   * Returns true if the JavaGD graphics device is available
   * 
   * @return true if JavaGD is available
   */
  public static boolean javaGDAvailable() {
    if (!s_javaGD_checked) {
      init();
    }

    return s_javaGD_available;
  }

  private static void init() {
    if (s_javaGD_checked) {
      return;
    }

    RSessionAPI eng = null;
    Object session = new Object();

    // we can reference RSessionImpl directly here because we get injected
    // into the root class loader (and never get loaded by child class loaders)
    if (RSessionImpl.rAvailable()) {
      try {

        eng = RSessionImpl.acquireSession(session);

        if (!s_javaGD_checked) {
          s_javaGD_available = eng.loadLibrary(session, "JavaGD");
          s_javaGD_checked = true;
        }

        if (!s_javaGD_available) {
          // try to install
          System.err.println("Trying to install the JavaGD library in R...");
          eng.installLibrary(session, "JavaGD");

          // now try to load it again
          s_javaGD_available = eng.loadLibrary(session, "JavaGD");

          if (!s_javaGD_available) {
            System.err.println("Was unable to load the JavaGD graphics device");
          }
        }

        if (s_javaGD_available) {
          /*
           * eng.parseAndEval(session,
           * "Sys.putenv('JAVAGD_CLASS_NAME'='weka/core/WekaJavaGD')");
           */
          eng
            .parseAndEval(session,
              ".setenv <- if (exists(\"Sys.setenv\")) Sys.setenv else Sys.putenv");
          eng.parseAndEval(session,
            ".setenv(\"JAVAGD_CLASS_NAME\"=\"weka/core/WekaJavaGD\")");
          eng.parseAndEval(session, "JavaGD(width=800, height=600)");
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      } finally {
        RSessionImpl.releaseSession(session);
      }
    }
    s_javaGD_checked = true;
  }
}
