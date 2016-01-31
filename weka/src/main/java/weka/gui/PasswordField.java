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
 *    PasswordField
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyEditor;

/**
 * Property editor widget that wraps and displays a JPasswordField.
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class PasswordField extends JPanel implements PropertyEditor,
  CustomPanelSupplier {

  private static final long serialVersionUID = 8180782063577036194L;

  /** The password field */
  protected JPasswordField m_password;

  /** The label for the widget */
  protected JLabel m_label;

  protected PropertyChangeSupport m_support = new PropertyChangeSupport(this);

  public PasswordField() {
    this("");
  }

  public PasswordField(String label) {
    setLayout(new BorderLayout());
    m_label = new JLabel(label);

    if (label.length() > 0) {
      m_label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
    }
    add(m_label, BorderLayout.WEST);

    m_password = new JPasswordField();
    m_password.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        super.keyReleased(e);
        m_support.firePropertyChange("", null, null);
      }
    });
    m_password.addFocusListener(new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        super.focusLost(e);
        m_support.firePropertyChange("", null, null);
      }
    });

    add(m_password, BorderLayout.CENTER);
    // setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
  }

  /**
   * Set the label for this widget.
   *
   * @param label the label to use
   */
  public void setLabel(String label) {
    m_label.setText(label);
  }

  public String getText() {
    return new String(m_password.getPassword());
  }

  public void setText(String text) {
    m_password.setText(text);
    m_support.firePropertyChange("", null, null);
  }

  @Override
  public JPanel getCustomPanel() {
    return this;
  }

  @Override
  public Object getValue() {
    return getAsText();
  }

  @Override
  public void setValue(Object value) {
    setAsText(value.toString());
  }

  @Override
  public boolean isPaintable() {
    return true;
  }

  @Override
  public void paintValue(Graphics gfx, Rectangle box) {
    // nothing to do
  }

  @Override
  public String getJavaInitializationString() {
    return null;
  }

  @Override
  public String getAsText() {
    return getText();
  }

  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    setText(text);
  }

  @Override
  public String[] getTags() {
    return null;
  }

  @Override
  public Component getCustomEditor() {
    return this;
  }

  @Override
  public boolean supportsCustomEditor() {
    return true;
  }

  @Override
  public void addPropertyChangeListener(PropertyChangeListener pcl) {
    if (pcl != null && m_support != null) {
      m_support.addPropertyChangeListener(pcl);
    }
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener pcl) {
    if (pcl != null && m_support != null) {
      m_support.removePropertyChangeListener(pcl);
    }
  }

  /**
   * Set the enabled status of the password box
   *
   * @param enabled true if the password box is to be enabled
   */
  @Override
  public void setEnabled(boolean enabled) {
    m_password.setEnabled(enabled);
  }

  /**
   * Set the editable status of the password box.
   *
   * @param editable true if the password box is editable
   */
  public void setEditable(boolean editable) {
    m_password.setEditable(editable);
  }
}
