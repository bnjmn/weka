package weka.gui;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.beans.PropertyEditor;
import javax.swing.BorderFactory;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.SystemColor;

// Support for drawing a property value in a component
public class PropertyPanel extends JPanel {

  private PropertyEditor m_Editor;

  public PropertyPanel(PropertyEditor pe) {

    //    System.err.println("PropertyPanel::PropertyPanel()");
    //    setBorder(BorderFactory.createEtchedBorder());
    setOpaque(true);
    m_Editor = pe;
    addMouseListener(new MouseAdapter() {
      private PropertyDialog pd = null;
      public void mouseClicked(MouseEvent evt) {
	if (pd == null) {
	  int x = getLocation().x - 30;
	  int y = getLocation().y + 50;
	  pd = new PropertyDialog(m_Editor, x, y);
	} else {
	  pd.setVisible(true);
	}
      }
    });
    Dimension newPref = getPreferredSize();
    newPref.height = getFontMetrics(getFont()).getHeight() * 5 / 4;
    newPref.width = newPref.height * 5;
    setPreferredSize(newPref);
  }

  public void paintComponent(Graphics g) {

    Insets i = getInsets();
    Rectangle box = new Rectangle(i.left, i.top,
				  getSize().width - i.left - i.right - 1,
				  getSize().height - i.top - i.bottom - 1);
    
    g.clearRect(i.left, i.top,
		getSize().width - i.right - i.left,
		getSize().height - i.bottom - i.top);
    m_Editor.paintValue(g, box);
  }

}
