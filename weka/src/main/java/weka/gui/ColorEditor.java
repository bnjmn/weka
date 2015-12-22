package weka.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;

import javax.swing.JColorChooser;

/**
 * A property editor for colors that uses JColorChooser as the
 * underlying editor.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class ColorEditor implements PropertyEditor {

  /** The underlying JColorChooser used as the custom editor */
  protected JColorChooser m_editor = new JColorChooser();

  public ColorEditor() {
    super();
  }

  /**
   * Set the current color
   *
   * @param value the current color
   */
  @Override
  public void setValue(Object value) {
    m_editor.setColor((Color) value);
  }

  /**
   * Get the current color
   *
   * @return the current color
   */
  @Override
  public Object getValue() {
    return m_editor.getColor();
  }

  /**
   * We paint our current color into the supplied bounding box
   *
   * @return true as we are paintable
   */
  @Override
  public boolean isPaintable() {
    return true;
  }

  /**
   * Paint our current color into the supplied bounding box
   *
   * @param gfx the graphics object to use
   * @param box the bounding box
   */
  @Override
  public void paintValue(Graphics gfx, Rectangle box) {
    Color c = m_editor.getColor();
    gfx.setColor(c);
    gfx.fillRect(box.x, box.y, box.width, box.height);
  }

  /**
   * Don't really need this
   *
   * @return some arbitrary string
   */
  @Override
  public String getJavaInitializationString() {
    return "null";
  }

  /**
   * Not representable as a string
   *
   * @return null
   */
  @Override
  public String getAsText() {
    return null;
  }

  /**
   * Throws an exception as we are not representable in text form
   *
   * @param text text
   * @throws IllegalArgumentException
   */
  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    throw new IllegalArgumentException(text);
  }

  /**
   * Not applicable - returns null
   *
   * @return null
   */
  @Override
  public String[] getTags() {
    return null;
  }

  /**
   * Returns our JColorChooser object
   *
   * @return our JColorChooser object
   */
  @Override
  public Component getCustomEditor() {
    return m_editor;
  }

  /**
   * We use JColorChooser, so return true
   *
   * @return true
   */
  @Override
  public boolean supportsCustomEditor() {
    return true;
  }

  @Override
  public void addPropertyChangeListener(PropertyChangeListener listener) {
    m_editor.addPropertyChangeListener(listener);
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener listener) {
    m_editor.removePropertyChangeListener(listener);
  }
}
