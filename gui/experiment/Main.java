
package weka.gui.experiment;

import weka.experiment.Experiment;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import weka.core.Utils;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.BufferedOutputStream;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import java.awt.GridLayout;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import java.io.File;
import javax.swing.JOptionPane;


public class Main extends JPanel {

  protected SetupPanel m_SetupPanel;
  protected RunPanel m_RunPanel;
  protected ResultsPanel m_ResultsPanel;

  protected JButton m_OpenBut = new JButton("Open...");
  protected JButton m_SaveBut = new JButton("Save...");
  protected JButton m_NewBut = new JButton("New");
  
  protected FileFilter m_ExpFilter = new FileFilter() {
    public String getDescription() {
      return "Experiment configuration files";
    }
    public boolean accept(File file) {
      String name = file.getName().toLowerCase();
      if (file.isDirectory()) {
	return true;
      }
      if (name.endsWith(".exp")) {
	return true;
      }
      return false;
    }
  };
  protected JFileChooser m_FileChooser = new JFileChooser();

  protected Experiment m_Exp;
  
  public Main() {

    m_NewBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	setExperiment(new Experiment());
      }
    });
    m_SaveBut.setEnabled(false);
    m_SaveBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	saveExperiment();
      }
    });
    m_OpenBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	openExperiment();
      }
    });
    m_FileChooser.setFileFilter(m_ExpFilter);
    m_FileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    
    m_SetupPanel = new SetupPanel();
    m_RunPanel = new RunPanel();
    m_ResultsPanel = new ResultsPanel();

    JPanel p1 = new JPanel();
    p1.setLayout(new GridLayout(1, 3));
    p1.add(m_OpenBut);
    p1.add(m_SaveBut);
    p1.add(m_NewBut);
    
    JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.addTab("Setup", null, m_SetupPanel, "Set up experiment");
    tabbedPane.addTab("Run", null, m_RunPanel, "Run experiment");
    tabbedPane.addTab("Results", null, m_ResultsPanel,
		      "Analyse experiment results");
    tabbedPane.setSelectedIndex(0);
    
    setLayout(new BorderLayout());
    add(p1, BorderLayout.NORTH);
    add(tabbedPane, BorderLayout.CENTER);
  }

  public Main(Experiment exp) {

    this();
    setExperiment(exp);
  }

  public void setExperiment(Experiment exp) {

    m_Exp = exp;
    m_SaveBut.setEnabled(true);
    m_SetupPanel.setExperiment(exp);
    m_RunPanel.setExperiment(exp);
    m_ResultsPanel.setExperiment(exp);
  }

  private void openExperiment() {
    
    int returnVal = m_FileChooser.showOpenDialog(this);
    if (returnVal != JFileChooser.APPROVE_OPTION) {
      return;
    }
    File expFile = m_FileChooser.getSelectedFile();
    try {
      FileInputStream fi = new FileInputStream(expFile);
      ObjectInputStream oi = new ObjectInputStream(
			     new BufferedInputStream(fi));
      Experiment exp = (Experiment)oi.readObject();
      oi.close();
      setExperiment(exp);
      System.err.println("Opened experiment:\n" + m_Exp);
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(this, "Couldn't open experiment file:\n"
				    + expFile
				    + "\nReason:\n" + ex.getMessage(),
				    "Open Experiment",
				    JOptionPane.ERROR_MESSAGE);
      // Pop up error dialog
    }
  }

  private void saveExperiment() {

    int returnVal = m_FileChooser.showSaveDialog(this);
    if (returnVal != JFileChooser.APPROVE_OPTION) {
      return;
    }
    File expFile = m_FileChooser.getSelectedFile();
    if (!expFile.getName().toLowerCase().endsWith(".exp")) {
      expFile = new File(expFile.getParent(), expFile.getName() + ".exp");
    }
    try {
      FileOutputStream fo = new FileOutputStream(expFile);
      ObjectOutputStream oo = new ObjectOutputStream(
			      new BufferedOutputStream(fo));
      oo.writeObject(m_Exp);
      oo.close();
      System.err.println("Saved experiment:\n" + m_Exp);
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(this, "Couldn't save experiment file:\n"
				    + expFile
				    + "\nReason:\n" + ex.getMessage(),
				    "Save Experiment",
				    JOptionPane.ERROR_MESSAGE);
      // Pop up error dialog
    }
  }
  
  /**
   * Tests out the classifier editor from the command line.
   *
   * @param args may contain the class name of a classifier to edit
   */
  public static void main(String [] args) {

    try {
      final JFrame jf = new JFrame("Weka Experiment Environment");
      jf.getContentPane().setLayout(new BorderLayout());
      jf.getContentPane().add(new Main(), BorderLayout.CENTER);
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
