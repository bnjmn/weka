
package weka.gui.experiment;

import weka.experiment.Experiment;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JFrame;
import javax.swing.SwingConstants;
import javax.swing.JTextField;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import java.awt.FlowLayout;
import javax.swing.Box;
import javax.swing.BoxLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;


public class RunNumberPanel extends JPanel {

  protected JTextField m_LowerText = new JTextField("1");
  protected JTextField m_UpperText = new JTextField("10");
  protected Experiment m_Exp;
  
  public RunNumberPanel() {
    
    // Updates occur to the values in exp whenever enter is pressed
    // or the component loses focus
    m_LowerText.addKeyListener(new KeyAdapter() {
      public void keyReleased(KeyEvent e) {
	m_Exp.setRunLower(getLower());
      }
    });
    m_LowerText.addFocusListener(new FocusAdapter() {
      public void focusLost(FocusEvent e) {
	m_Exp.setRunLower(getLower());
      }
    });
    m_UpperText.addKeyListener(new KeyAdapter() {
      public void keyReleased(KeyEvent e) {
	m_Exp.setRunUpper(getUpper());
      }
    });
    m_UpperText.addFocusListener(new FocusAdapter() {
      public void focusLost(FocusEvent e) {
	m_Exp.setRunUpper(getUpper());
      }
    });

    m_LowerText.setEnabled(false);
    m_UpperText.setEnabled(false);
    setLayout(new GridLayout(1,2));
    setBorder(BorderFactory.createTitledBorder("Runs"));
    Box b1 = new Box(BoxLayout.X_AXIS);
    b1.add(Box.createHorizontalStrut(10));
    b1.add(new JLabel("From:", SwingConstants.RIGHT));
    b1.add(Box.createHorizontalStrut(5));
    b1.add(m_LowerText);
    add(b1);
    Box b2 = new Box(BoxLayout.X_AXIS);
    b2.add(Box.createHorizontalStrut(10));
    b2.add(new JLabel("To:", SwingConstants.RIGHT));
    b2.add(Box.createHorizontalStrut(5));
    b2.add(m_UpperText);
    add(b2);
  }

  public RunNumberPanel(Experiment exp) {

    this();
    setExperiment(exp);
  }

  public void setExperiment(Experiment exp) {
    
    m_Exp = exp;
    m_LowerText.setText("" + m_Exp.getRunLower());
    m_UpperText.setText("" + m_Exp.getRunUpper());
    m_LowerText.setEnabled(true);
    m_UpperText.setEnabled(true);
  }
  
  public int getLower() {

    int result = 1;
    try {
      result = Integer.parseInt(m_LowerText.getText());
    } catch (Exception ex) {
    }
    return Math.max(1, result);
  }
  public int getUpper() {

    int result = 1;
    try {
      result = Integer.parseInt(m_UpperText.getText());
    } catch (Exception ex) {
    }
    return Math.max(1, result);
  }
  
  /**
   * Tests out the classifier editor from the command line.
   *
   * @param args may contain the class name of a classifier to edit
   */
  public static void main(String [] args) {

    try {
      final JFrame jf = new JFrame("Dataset List Editor");
      jf.getContentPane().setLayout(new BorderLayout());
      jf.getContentPane().add(new RunNumberPanel(new Experiment()),
			      BorderLayout.CENTER);
      jf.addWindowListener(new WindowAdapter() {
	public void windowClosing(WindowEvent e) {
	  jf.dispose();
	  System.exit(0);
	}
      });
      jf.pack();
      jf.setVisible(true);
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
