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
 *    NoteVisual.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.knowledgeflow;

import weka.knowledgeflow.StepManagerImpl;
import weka.knowledgeflow.steps.Note;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;

/**
 * Visual representation for the Note "step".
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class NoteVisual extends StepVisual {

  private static final long serialVersionUID = -3291021235652124916L;

  /** The label that displays the note text */
  protected JLabel m_label = new JLabel();

  /** Adjustment for the font size */
  protected int m_fontSizeAdjust = -1;

  /**
   * Set the {@code StepManagerImpl} for the step covered by this visual
   *
   * @param manager the step manager to wrap
   */
  @Override
  public void setStepManager(StepManagerImpl manager) {
    super.setStepManager(manager);

    removeAll();
    setLayout(new BorderLayout());
    setBorder(new ShadowBorder(2, Color.GRAY));
    m_label.setText(convertToHTML(((Note) getStepManager().getManagedStep())
      .getNoteText()));
    m_label.setOpaque(true);
    m_label.setBackground(Color.YELLOW);
    JPanel holder = new JPanel();
    holder.setLayout(new BorderLayout());
    holder.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    holder.setOpaque(true);
    holder.setBackground(Color.YELLOW);
    holder.add(m_label, BorderLayout.CENTER);
    add(holder, BorderLayout.CENTER);
  }

  /**
   * Set whether the note should appear "highlighted" (i.e. thicker border)
   * 
   * @param highlighted true if the note should appear highlighted
   */
  public void setHighlighted(boolean highlighted) {
    if (highlighted) {
      setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.BLUE));
    } else {
      // setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
      setBorder(new ShadowBorder(2, Color.GRAY));
    }
    revalidate();
  }

  /**
   * Turn on/off the connector points
   *
   * @param dc a <code>boolean</code> value
   */
  @Override
  public void setDisplayConnectors(boolean dc) {
    // m_visualHolder.setDisplayConnectors(dc);
    m_displayConnectors = dc;
    m_connectorColor = Color.blue;

    setHighlighted(dc);
  }

  /**
   * Turn on/off the connector points
   *
   * @param dc a <code>boolean</code> value
   * @param c the Color to use
   */
  @Override
  public void setDisplayConnectors(boolean dc, Color c) {
    setDisplayConnectors(dc);
    m_connectorColor = c;
  }

  @Override
  public boolean getDisplayStepLabel() {
    return false;
  }

  @Override
  public void paintComponent(Graphics gx) {
    m_label.setText(convertToHTML(((Note) getStepManager().getManagedStep())
      .getNoteText()));
  }

  /**
   * Convert plain text to HTML
   * 
   * @param text the text to convert to marked up HTML
   * @return the marked up HTML
   */
  private String convertToHTML(String text) {
    String htmlString = text.replace("\n", "<br>");
    htmlString =
      "<html><font size=" + m_fontSizeAdjust + ">" + htmlString + "</font>"
        + "</html>";

    return htmlString;
  }

  /**
   * Get the font size adjustment
   *
   * @return the font size adjustment
   */
  public int getFontSizeAdjust() {
    return m_fontSizeAdjust;
  }

  /**
   * set the font size adjustment
   *
   * @param adjust the font size adjustment
   */
  public void setFontSizeAdjust(int adjust) {
    m_fontSizeAdjust = adjust;
  }

  /**
   * Decrease the font size by one
   */
  public void decreaseFontSize() {
    m_fontSizeAdjust--;
  }

  /**
   * Increase the font size by one
   */
  public void increaseFontSize() {
    m_fontSizeAdjust++;
  }
}
