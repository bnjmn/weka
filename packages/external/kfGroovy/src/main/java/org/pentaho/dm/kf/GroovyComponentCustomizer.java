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
 *    GroovyComponentCustomizer.java
 *    Copyright (C) 2009 Pentaho Corporation
 *
 */

package org.pentaho.dm.kf;

import weka.core.Utils;
import weka.gui.beans.BeanCustomizer;
import weka.gui.beans.CustomizerCloseRequester;
import weka.gui.scripting.GroovyScript;
import weka.gui.visualize.VisualizeUtils;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.text.DefaultStyledDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.lang.reflect.Constructor;
import java.util.Properties;

/**
 * Customizer class for the GroovyComponent.
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision $
 */
public class GroovyComponentCustomizer extends JPanel
  implements BeanCustomizer, CustomizerCloseRequester {

  /** The object to edit */
  protected GroovyComponent m_groovyP;

  /** The script from the GroovyComponent */
  protected GroovyScript m_script;

  protected JTextPane m_textPane = new JTextPane();

  protected Window m_parentWindow;

  /** Holds the template text of a new script */
  protected String m_newScript;

  protected JMenuBar m_menuBar;

  protected ModifyListener m_modifyListener;

  /** the Groovy setup. */
  public final static String PROPERTIES_FILE =
    "weka/gui/scripting/Groovy.props";

  /*
   * (non-Javadoc)
   * 
   * @see java.beans.Customizer#setObject(java.lang.Object)
   */
  public void setObject(Object object) {
    m_groovyP = (GroovyComponent) object;

    m_script = new GroovyScript(m_textPane.getDocument());

    String script = m_groovyP.getScript();
    if (script != null && script.length() > 0) {
      m_script.setContent(script);
    }
  }

  public GroovyComponentCustomizer() {
    Properties props;

    try {
      props = Utils.readProperties(PROPERTIES_FILE);
    } catch (Exception ex) {
      ex.printStackTrace();
      props = new Properties();
    }

    // check for SyntaxDocument
    boolean syntaxDocAvailable = true;
    try {
      Class.forName("weka.gui.scripting.SyntaxDocument");
    } catch (Exception ex) {
      syntaxDocAvailable = false;
    }

    if (props.getProperty("Syntax", "false").equals("true")
      && syntaxDocAvailable) {
      try {
        Class syntaxClass = Class.forName("weka.gui.scripting.SyntaxDocument");
        Constructor constructor = syntaxClass.getConstructor(Properties.class);
        Object doc = constructor.newInstance(props);
        // SyntaxDocument doc = new SyntaxDocument(props);
        m_textPane.setDocument((DefaultStyledDocument) doc);
        // m_textPane.setBackground(doc.getBackgroundColor());
        m_textPane.setBackground(VisualizeUtils.processColour(
          props.getProperty("BackgroundColor", "white"), Color.WHITE));
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    } else {
      m_textPane.setForeground(VisualizeUtils.processColour(
        props.getProperty("ForegroundColor", "black"), Color.BLACK));
      m_textPane.setBackground(VisualizeUtils.processColour(
        props.getProperty("BackgroundColor", "white"), Color.WHITE));
      m_textPane.setFont(new Font(props.getProperty("FontName", "monospaced"),
        Font.PLAIN, Integer.parseInt(props.getProperty("FontSize", "12"))));
    }

    final JFileChooser fileChooser = new JFileChooser();
    fileChooser.setAcceptAllFileFilterUsed(true);
    fileChooser.setMultiSelectionEnabled(false);

    setUpNewScript();

    setLayout(new BorderLayout());
    add(new JScrollPane(m_textPane), BorderLayout.CENTER);

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BorderLayout());
    JPanel newCompile = new JPanel();
    newCompile.setLayout(new BorderLayout());

    JButton newBut = new JButton("New");
    JButton compileBut = new JButton("Compile");
    JButton okBut = new JButton("OK");
    JButton cancelBut = new JButton("Cancel");
    newCompile.add(newBut, BorderLayout.WEST);
    newCompile.add(compileBut, BorderLayout.EAST);

    buttonPanel.add(newCompile, BorderLayout.WEST);
    buttonPanel.add(okBut, BorderLayout.CENTER);
    buttonPanel.add(cancelBut, BorderLayout.EAST);
    add(buttonPanel, BorderLayout.SOUTH);

    newBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        newScript(fileChooser);
      }
    });

    compileBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        doCompile();
      }
    });

    okBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (m_groovyP != null) {
          m_groovyP.setScript(m_script.getContent());

          if (m_modifyListener != null) {
            m_modifyListener.setModifiedStatus(GroovyComponentCustomizer.this,
              true);
          }

          // close the dialog
          m_parentWindow.dispose();
        }
      }
    });

    cancelBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        // just close the dialog
        // TODO if their are mods, ask the user (same if the frame is closed
        // from the close widget)
        if (m_modifyListener != null) {
          m_modifyListener.setModifiedStatus(GroovyComponentCustomizer.this,
            false);
        }

        m_parentWindow.dispose();
      }
    });

    m_menuBar = new JMenuBar();

    JMenu fileM = new JMenu();
    m_menuBar.add(fileM);
    fileM.setText("File");
    fileM.setMnemonic('F');

    JMenuItem newItem = new JMenuItem();
    fileM.add(newItem);
    newItem.setText("New");
    newItem.setAccelerator(
      KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_MASK));
    newItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        newScript(fileChooser);
      }
    });

    JMenuItem loadItem = new JMenuItem();
    fileM.add(loadItem);
    loadItem.setText("Open File...");
    loadItem.setAccelerator(
      KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_MASK));
    loadItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        int retVal = fileChooser.showOpenDialog(GroovyComponentCustomizer.this);
        if (retVal == JFileChooser.APPROVE_OPTION) {
          boolean ok = m_script.open(fileChooser.getSelectedFile());
          if (!ok) {
            JOptionPane.showMessageDialog(GroovyComponentCustomizer.this,
              "Couldn't open file '" + fileChooser.getSelectedFile() + "'!");
          }
        }
      }
    });

    JMenuItem saveItem = new JMenuItem();
    fileM.add(saveItem);

    saveItem.setText("Save");
    saveItem.setAccelerator(
      KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_MASK));
    saveItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (m_script.getFilename() != null) {
          save(null);
        } else {
          save(fileChooser);
        }
      }
    });

    JMenuItem saveAsItem = new JMenuItem();
    fileM.add(saveAsItem);

    saveAsItem.setText("Save As...");
    saveAsItem.setAccelerator(
      KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_MASK));
    saveAsItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        save(fileChooser);
      }
    });

    JMenu scriptM = new JMenu();
    m_menuBar.add(scriptM);
    scriptM.setText("Script");
    scriptM.setMnemonic('S');

    JMenuItem compileItem = new JMenuItem();
    scriptM.add(compileItem);
    compileItem.setText("Compile");
    compileItem.setAccelerator(
      KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK));
    compileItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        doCompile();
      }
    });

    Dimension d = new Dimension(600, 800);
    m_textPane.setMinimumSize(d);
    m_textPane.setPreferredSize(d);
  }

  private void save(JFileChooser fileChooser) {
    boolean ok = false;
    int retVal;

    if (m_script.getFilename() == null || fileChooser != null) {
      retVal = fileChooser.showSaveDialog(GroovyComponentCustomizer.this);
      if (retVal == JFileChooser.APPROVE_OPTION) {
        ok = m_script.saveAs(fileChooser.getSelectedFile());
      }
    } else {
      ok = m_script.save();
    }

    if (!ok) {
      if (m_script.getFilename() != null) {
        JOptionPane.showMessageDialog(GroovyComponentCustomizer.this,
          "Failed to save file '" + fileChooser.getSelectedFile() + "'!");
      } else {
        JOptionPane.showMessageDialog(GroovyComponentCustomizer.this,
          "Failed to save file!");
      }
    }
  }

  private void doCompile() {
    if (m_groovyP != null) {
      String script = m_script.getContent();
      if (script != null && script.length() > 0) {
        try {
          m_groovyP.compileScript(script);
          JOptionPane.showMessageDialog(GroovyComponentCustomizer.this,
            "Script compiled OK.", "Script Status",
            JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
          ex.printStackTrace();
          JOptionPane.showMessageDialog(GroovyComponentCustomizer.this,
            "Problem compiling script:\n" + ex.getMessage(), "Script Status",
            JOptionPane.INFORMATION_MESSAGE);
        }
      }
    }
  }

  private void newScript(JFileChooser fileChooser) {
    if (m_groovyP != null) {
      if (m_script.isModified()) {
        // prompt for are you sure?
        int retVal =
          JOptionPane.showConfirmDialog(GroovyComponentCustomizer.this,
            "Save changes" + ((m_script.getFilename() != null)
              ? (" to " + m_script.getFilename() + "?") : " first?"));
        if (retVal == JOptionPane.OK_OPTION) {
          if (m_script.getFilename() != null) {
            save(null);
          } else {
            save(fileChooser);
          }
        }
      }
      m_script.empty();
      m_script.setContent(m_newScript);
    }
  }

  public void setParentWindow(Window parent) {
    m_parentWindow = parent;
    if (parent instanceof javax.swing.JDialog) {
      ((javax.swing.JDialog) m_parentWindow).setJMenuBar(m_menuBar);
      ((javax.swing.JDialog) m_parentWindow).setTitle("Groovy Script Editor");
    }
  }

  protected void setUpNewScript() {
    StringBuffer temp = new StringBuffer();
    temp.append("import java.beans.*\n");
    temp.append("import java.io.Serializable\n");
    temp.append("import java.util.Vector\n");
    temp.append("import java.util.Enumeration\n");
    temp.append("import org.pentaho.dm.kf.KFGroovyScript\n");
    temp.append("import org.pentaho.dm.kf.GroovyHelper\n");
    temp.append("import weka.core.*\n");
    temp.append("import weka.gui.Logger\n");
    temp.append("import weka.gui.beans.*\n");
    temp.append("// add further imports here if necessary\n\n");
    temp.append("class MyScript\n");
    temp.append("\timplements\n");
    temp.append("\t\tKFGroovyScript,\n");
    temp.append("\t\tEnvironmentHandler,\n");
    temp.append("\t\tBeanCommon,\n");
    temp.append("\t\tEventConstraints,\n");
    temp.append("\t\tUserRequestAcceptor,\n");
    temp.append("\t\tTrainingSetListener,\n");
    temp.append("\t\tTestSetListener,\n");
    temp.append("\t\tDataSourceListener,\n");
    temp.append("\t\tInstanceListener,\n");
    temp.append("\t\tTextListener,\n");
    temp.append("\t\tBatchClassifierListener,\n");
    temp.append("\t\tIncrementalClassifierListener,\n");
    temp.append("\t\tBatchClustererListener,\n");
    temp.append("\t\tGraphListener,\n");
    temp.append("\t\tChartListener,\n");
    temp.append("\t\tThresholdDataListener,\n");
    temp.append("\t\tVisualizableErrorListener,\n");
    temp.append("\t\tSerializable {\n\n");

    temp.append("\t/** Don't delete!!\n");
    temp.append("\t *  GroovyHelper has the following useful methods:\n");
    temp.append("\t *\n");
    temp.append(
      "\t *  notifyListenerType(Object event) - GroovyHelper will pass on event\n");
    temp.append("\t *    appropriate listener type for you\n");
    temp.append(
      "\t *  ArrayList<TrainingSetListener> getTrainingSetListeners() - get\n");
    temp.append(
      "\t *    a list of any directly connected components that are listening\n");
    temp.append("\t *    for TrainingSetEvents from us\n");
    temp.append("\t *  ArrayList<TestSetListener> getTestSetListeners()\n");
    temp.append("\t *  ArrayList<InstanceListener> getInstanceListeners()\n");
    temp.append("\t *  ArrayList<TextListener> getTextListeners()\n");
    temp
      .append("\t *  ArrayList<DataSourceListener> getDataSourceListeners()\n");
    temp.append(
      "\t *  ArrayList<BatchClassifierListener> getBatchClassifierListeners()\n");
    temp.append(
      "\t *  ArrayList<IncrementalClassifierListener> getIncrementalClassifierListeners()\n");
    temp.append(
      "\t *  ArrayList<BatchClustererListener> getBatchClustererListeners()\n");
    temp.append("\t *  ArrayList<GraphListenerListener> getGraphListeners()\n");
    temp.append("\t *  ArrayList<ChartListener> getChartListeners()\n");
    temp.append(
      "\t *  ArrayList<ThresholdDataListener> getThresholdDataListeners()\n");
    temp.append(
      "\t *  ArrayList<VisualizableErrorListener> getVisualizableErrorListeners()\n");
    temp.append("\t */\n");
    temp.append("\tGroovyHelper m_helper\n\n");

    temp.append("\t/** Don't delete!! */\n");
    temp.append("\tvoid setManager(GroovyHelper manager) { "
      + "m_helper = manager }\n\n");

    // BeanCommon
    temp.append("\t/** Alter or add to in order to tell the KnowlegeFlow\n");
    temp.append(
      "\t *  environment whether a certain incoming connection type is allowed\n");
    temp.append("\t */\n");
    temp.append("\tboolean connectionAllowed(String eventName) {\n");
    temp
      .append("\t\tif (eventName.equals(\"trainingSet\")) { return false }\n");
    temp.append("\t\treturn false\n");
    temp.append("\t}\n\n");

    temp.append("\t/** Alter or add to in order to tell the KnowlegeFlow\n");
    temp.append(
      "\t *  environment whether a certain incoming connection type is allowed\n");
    temp.append("\t */\n");
    temp.append("\tboolean connectionAllowed(EventSetDescriptor esd) {\n");

    temp.append("\t\treturn connectionAllowed(esd.getName())\n");
    temp.append("\t}\n\n");

    temp
      .append("\t/** Add (optional) code to do something when you have been\n");
    temp.append(
      "\t *  registered as a listener with a source for the named event\n");
    temp.append("\t */\n");
    temp.append(
      "\tvoid connectionNotification(String eventName, Object source) { }\n\n");

    temp
      .append("\t/** Add (optional) code to do something when you have been\n");
    temp.append(
      "\t *  deregistered as a listener with a source for the named event\n");
    temp.append("\t */\n");
    temp.append(
      "\tvoid disconnectionNotification(String eventName, Object source) { }\n\n");

    temp.append(
      "\t/** Custom name of this component. Do something with it if you\n");
    temp.append(
      "\t *  like. GroovyHelper already stores it and alters the icon text\n");
    temp.append("\t *  for you");
    temp.append("\t */\n");
    temp.append("\tvoid setCustomName(String name) { }\n\n");

    temp.append(
      "\t/** Custom name of this component. No need to return anything\n");
    temp.append(
      "\t *  GroovyHelper already stores it and alters the icon text\n");
    temp.append("\t *  for you");
    temp.append("\t */\n");
    temp.append("\tString getCustomName() { return null }\n\n");

    temp.append(
      "\t/** Add code to return true when you are busy doing something\n");
    temp.append("\t */\n");
    temp.append("\tboolean isBusy() { return false }\n\n");

    temp.append(
      "\t/** Store and use this logging object in order to post messages\n"
        + "\t *  to the log\n");
    temp.append("\t */\n");
    temp.append("\tvoid setLog(Logger logger) { }\n\n");

    temp.append(
      "\t/** Store and use this Environment object in order to lookup and\n"
        + "\t *  use the values of environment variables\n");
    temp.append("\t */\n");
    temp.append("\tvoid setEnvironment(Environment env) { }\n\n");

    temp.append("\t/** Stop any processing (if possible)\n");
    temp.append("\t */\n");
    temp.append("\tvoid stop() { }\n\n");

    temp.append("\t/** Alter or add to in order to tell the KnowlegeFlow\n");
    temp.append("\t *  whether, at the current time, the named event could\n");
    temp.append("\t *  be generated.\n");
    temp.append("\t */\n");
    temp.append("\tboolean eventGeneratable(String eventName) {\n");
    temp
      .append("\t\tif (eventName.equals(\"trainingSet\")) { return false }\n");
    temp.append("\t\treturn false\n");
    temp.append("\t}\n\n");

    temp
      .append("\t/** Implement this to tell KnowledgeFlow about any methods\n");
    temp.append(
      "\t *  that the user could invoke (i.e. to show a popup visualization\n");
    temp.append("\t *  or something).\n");
    temp.append("\t */\n");
    temp.append(
      "\tEnumeration enumerateRequests() { return (new Vector(0)).elements()}\n\n");

    temp.append("\t/** Make the user-requested action happen here.\n");
    temp.append("\t */\n");
    temp.append("\tvoid performRequest(String requestName) { }\n\n");

    temp.append("\t//--------------- Incoming events ------------------\n");
    temp.append("\t//--------------- Implement as necessary -----------\n\n");

    temp.append("\tvoid acceptTrainingSet(TrainingSetEvent e) { }\n\n");
    temp.append("\tvoid acceptTestSet(TestSetEvent e) { }\n\n");
    temp.append("\tvoid acceptDataSet(DataSetEvent e) { }\n\n");
    temp.append("\tvoid acceptInstance(InstanceEvent e) { }\n\n");
    temp.append("\tvoid acceptText(TextEvent e) { }\n\n");
    temp.append("\tvoid acceptClassifier(BatchClassifierEvent e) { }\n\n");
    temp
      .append("\tvoid acceptClassifier(IncrementalClassifierEvent e) { }\n\n");
    temp.append("\tvoid acceptClusterer(BatchClustererEvent e) { }\n\n");
    temp.append("\tvoid acceptGraph(GraphEvent e) { }\n\n");
    temp.append("\tvoid acceptDataPoint(ChartEvent e) { }\n\n");
    temp.append("\tvoid acceptDataSet(ThresholdDataEvent e) { }\n\n");
    temp.append("\tvoid acceptDataSet(VisualizableErrorEvent e) { }\n\n");

    temp.append("}\n");

    m_newScript = temp.toString();
  }

  public void setModifiedListener(ModifyListener l) {
    m_modifyListener = l;
  }
}
