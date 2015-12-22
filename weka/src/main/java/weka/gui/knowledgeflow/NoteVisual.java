package weka.gui.knowledgeflow;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import weka.knowledgeflow.StepManagerImpl;
import weka.knowledgeflow.steps.Note;

/**
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision: $
 */
public class NoteVisual extends StepVisual {

  /** The label that displays the note text */
  protected JLabel m_label = new JLabel();

  /** Adjustment for the font size */
  protected int m_fontSizeAdjust = -1;

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
  public void setDisplayConnectors(boolean dc, Color c) {
    setDisplayConnectors(dc);
    m_connectorColor = c;
  }

  public boolean getDisplayStepLabel() {
    return false;
  }

  @Override
  public void paintComponent(Graphics gx) {
    m_label.setText(convertToHTML(((Note) getStepManager().getManagedStep())
      .getNoteText()));
  }

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
