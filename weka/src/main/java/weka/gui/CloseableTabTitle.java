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
 *    CloseableTabTitle.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * Tab title widget that allows the user to click a little cross in order to
 * close the tab
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class CloseableTabTitle extends JPanel {

  /** For serialization */
  private static final long serialVersionUID = 9178081197757118130L;

  /**
   * Interface for a callback for notification of a tab's close widget being
   * clicked
   */
  public static interface ClosingCallback {
    void tabClosing(int tabIndex);
  }

  /** The enclosing JTabbedPane */
  private final JTabbedPane m_enclosingPane;

  /** The label for the tab */
  private JLabel m_tabLabel;

  /** The close widget */
  private TabButton m_tabButton;

  /** Owner to callback on closing widget click */
  private ClosingCallback m_callback;

  /**
   * Description of the keyboard accellerator used for closing the active tab
   * (if any)
   */
  private String m_closeAccelleratorText = "";

  /**
   * Constructor.
   *
   * @param pane the enclosing JTabbedPane
   * @param callback the callback to notify on closing widget click
   */
  public CloseableTabTitle(final JTabbedPane pane,
    String closeAccelleratorText, ClosingCallback callback) {
    super(new FlowLayout(FlowLayout.LEFT, 0, 0));

    if (closeAccelleratorText != null) {
      m_closeAccelleratorText = closeAccelleratorText;
    }
    m_enclosingPane = pane;
    setOpaque(false);
    setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

    // read the title from the JTabbedPane
    m_tabLabel = new JLabel() {
      /** For serialization */
      private static final long serialVersionUID = 8515052190461050324L;

      @Override
      public String getText() {
        int index = m_enclosingPane.indexOfTabComponent(CloseableTabTitle.this);
        if (index >= 0) {
          return m_enclosingPane.getTitleAt(index);
        }
        return null;
      }
    };

    add(m_tabLabel);
    m_tabLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
    m_tabButton = new TabButton();
    add(m_tabButton);
    m_callback = callback;
    m_tabLabel.setEnabled(false);
  }

  /**
   * Set a bold look for the tab
   *
   * @param bold true for bold
   */
  public void setBold(boolean bold) {
    m_tabLabel.setEnabled(bold);
  }

  /**
   * Enable/disable the close widget
   *
   * @param enabled
   */
  public void setButtonEnabled(boolean enabled) {
    m_tabButton.setEnabled(enabled);
  }

  /**
   * Class that implements the cross shaped close widget
   */
  private class TabButton extends JButton implements ActionListener {

    /** For serialization */
    private static final long serialVersionUID = -4915800749132175968L;

    public TabButton() {
      int size = 17;
      setPreferredSize(new Dimension(size, size));
      setToolTipText("close this tab");
      // Make the button looks the same for all Laf's
      setUI(new BasicButtonUI());
      // Make it transparent
      setContentAreaFilled(false);
      // No need to be focusable
      setFocusable(false);
      setBorder(BorderFactory.createEtchedBorder());
      setBorderPainted(false);
      // Making nice rollover effect
      // we use the same listener for all buttons
      addMouseListener(new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
          Component component = e.getComponent();

          if (component instanceof AbstractButton) {
            AbstractButton button = (AbstractButton) component;
            button.setBorderPainted(true);

            int i = m_enclosingPane.indexOfTabComponent(CloseableTabTitle.this);
            if (i == m_enclosingPane.getSelectedIndex()) {
              button
                .setToolTipText("close this tab " + m_closeAccelleratorText);
            } else {
              button.setToolTipText("close this tab");
            }
          }
        }

        @Override
        public void mouseExited(MouseEvent e) {
          Component component = e.getComponent();
          if (component instanceof AbstractButton) {
            AbstractButton button = (AbstractButton) component;
            button.setBorderPainted(false);
          }
        }
      });
      setRolloverEnabled(true);
      // Close the proper tab by clicking the button
      addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int i = m_enclosingPane.indexOfTabComponent(CloseableTabTitle.this);
      if (i >= 0) {
        if (m_callback != null) {
          m_callback.tabClosing(i);
        }
      }
    }

    // we don't want to update UI for this button
    @Override
    public void updateUI() {
    }

    // paint the cross
    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D g2 = (Graphics2D) g.create();
      // shift the image for pressed buttons
      if (getModel().isPressed()) {
        g2.translate(1, 1);
      }
      g2.setStroke(new BasicStroke(2));
      g2.setColor(Color.BLACK);
      if (!isEnabled()) {
        g2.setColor(Color.GRAY);
      }
      if (getModel().isRollover()) {
        g2.setColor(Color.MAGENTA);
      }
      int delta = 6;
      g2.drawLine(delta, delta, getWidth() - delta - 1, getHeight() - delta - 1);
      g2.drawLine(getWidth() - delta - 1, delta, delta, getHeight() - delta - 1);
      g2.dispose();
    }
  }
}
