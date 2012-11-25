/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    SetInstancesPanel.java
 *    Copyright (C) 1999 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import weka.core.Instances;
import weka.core.converters.ConverterUtils;
import weka.core.converters.FileSourcedConverter;
import weka.core.converters.IncrementalConverter;
import weka.core.converters.URLSourcedLoader;

/**
 * A panel that displays an instance summary for a set of instances and lets the
 * user open a set of instances from either a file or URL.
 * 
 * Instances may be obtained either in a batch or incremental fashion. If
 * incremental reading is used, then the client should obtain the Loader object
 * (by calling getLoader()) and read the instances one at a time. If batch
 * loading is used, then SetInstancesPanel will load the data into memory inside
 * of a separate thread and notify the client when the operation is complete.
 * The client can then retrieve the instances by calling getInstances().
 * 
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision$
 */
public class SetInstancesPanel extends JPanel {

  /** for serialization */
  private static final long serialVersionUID = -384804041420453735L;

  /** Click to open instances from a file */
  protected JButton m_OpenFileBut = new JButton(Messages.getInstance()
      .getString("SetInstancesPanel_OpenFileBut_JButton_Text"));

  /** Click to open instances from a URL */
  protected JButton m_OpenURLBut = new JButton(Messages.getInstance()
      .getString("SetInstancesPanel_OpenURLBut_JButton_Text"));

  /** Click to close the dialog */
  protected JButton m_CloseBut = new JButton(Messages.getInstance().getString(
      "SetInstancesPanel_CloseBut_JButton_Text"));

  /** The instance summary component */
  protected InstancesSummaryPanel m_Summary = new InstancesSummaryPanel();

  /** The file chooser for selecting arff files */
  protected ConverterFileChooser m_FileChooser = new ConverterFileChooser(
      new File(System.getProperty("user.dir")));

  /** Stores the last URL that instances were loaded from */
  protected String m_LastURL = "http://";

  /** The thread we do loading in */
  protected Thread m_IOThread;

  /**
   * Manages sending notifications to people when we change the set of working
   * instances.
   */
  protected PropertyChangeSupport m_Support = new PropertyChangeSupport(this);

  /** The current set of instances loaded */
  protected Instances m_Instances;

  /** The current loader used to obtain the current instances */
  protected weka.core.converters.Loader m_Loader;

  /** the parent frame. if one is provided, the close-button is displayed */
  protected JFrame m_ParentFrame = null;

  /** the panel the Close-Button is located in */
  protected JPanel m_CloseButPanel = null;

  /** whether to read the instances incrementally, if possible. */
  protected boolean m_readIncrementally = true;

  /** whether to display zero instances as unknown ("?"). */
  protected boolean m_showZeroInstancesAsUnknown = false;

  /**
   * Default constructor
   */
  public SetInstancesPanel() {
    this(false, null);
  }

  /**
   * Create the panel.
   * 
   * @param showZeroInstancesAsUnknown whether to display zero instances as
   *          unknown (e.g., when reading data incrementally)
   * @param chooser the file chooser to use (may be null to use the default file
   *          chooser)
   */
  public SetInstancesPanel(boolean showZeroInstancesAsUnknown,
      ConverterFileChooser chooser) {
    m_showZeroInstancesAsUnknown = showZeroInstancesAsUnknown;
    if (chooser != null) {
      m_FileChooser = chooser;
    }

    m_OpenFileBut.setToolTipText(Messages.getInstance().getString(
        "SetInstancesPanel_OpenFileBut_SetToolTipText_Text"));
    m_OpenURLBut.setToolTipText(Messages.getInstance().getString(
        "SetInstancesPanel_OpenURLBut_SetToolTipText_Text"));
    m_CloseBut.setToolTipText(Messages.getInstance().getString(
        "SetInstancesPanel_CloseBut_SetToolTipText_Text"));
    m_FileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    m_OpenURLBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setInstancesFromURLQ();
      }
    });
    m_OpenFileBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setInstancesFromFileQ();
      }
    });
    m_CloseBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        closeFrame();
      }
    });
    m_Summary.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

    JPanel buttons = new JPanel();
    buttons.setLayout(new GridLayout(1, 2));
    buttons.add(m_OpenFileBut);
    buttons.add(m_OpenURLBut);

    m_CloseButPanel = new JPanel();
    m_CloseButPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
    m_CloseButPanel.add(m_CloseBut);
    m_CloseButPanel.setVisible(false);

    JPanel buttonsAll = new JPanel();
    buttonsAll.setLayout(new BorderLayout());
    buttonsAll.add(buttons, BorderLayout.CENTER);
    buttonsAll.add(m_CloseButPanel, BorderLayout.SOUTH);

    setLayout(new BorderLayout());
    add(m_Summary, BorderLayout.CENTER);
    add(buttonsAll, BorderLayout.SOUTH);
  }

  /**
   * Sets the frame, this panel resides in. Used for displaying the close
   * button, i.e., the close-button is visible if the given frame is not null.
   * 
   * @param parent the parent frame
   */
  public void setParentFrame(JFrame parent) {
    m_ParentFrame = parent;
    m_CloseButPanel.setVisible(m_ParentFrame != null);
  }

  /**
   * Returns the current frame the panel knows of, that it resides in. Can be
   * null.
   * 
   * @return the current parent frame
   */
  public JFrame getParentFrame() {
    return m_ParentFrame;
  }

  /**
   * closes the frame, i.e., the visibility is set to false
   */
  public void closeFrame() {
    if (m_ParentFrame != null)
      m_ParentFrame.setVisible(false);
  }

  /**
   * Queries the user for a file to load instances from, then loads the
   * instances in a background process. This is done in the IO thread, and an
   * error message is popped up if the IO thread is busy.
   */
  public void setInstancesFromFileQ() {

    if (m_IOThread == null) {
      int returnVal = m_FileChooser.showOpenDialog(this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        final File selected = m_FileChooser.getSelectedFile();
        m_IOThread = new Thread() {
          @Override
          public void run() {
            setInstancesFromFile(selected);
            m_IOThread = null;
          }
        };
        m_IOThread.setPriority(Thread.MIN_PRIORITY); // UI has most priority
        m_IOThread.start();
      }
    } else {
      JOptionPane
          .showMessageDialog(
              this,
              Messages
                  .getInstance()
                  .getString(
                      "SetInstancesPanel_SetInstancesFromFileQ_JOptionPaneShowMessageDialog_Text_First"),
              Messages
                  .getInstance()
                  .getString(
                      "SetInstancesPanel_SetInstancesFromFileQ_JOptionPaneShowMessageDialog_Text_Second"),
              JOptionPane.WARNING_MESSAGE);
    }
  }

  /**
   * Queries the user for a URL to load instances from, then loads the instances
   * in a background process. This is done in the IO thread, and an error
   * message is popped up if the IO thread is busy.
   */
  public void setInstancesFromURLQ() {

    if (m_IOThread == null) {
      try {
        String urlName = (String) JOptionPane
            .showInputDialog(
                this,
                Messages
                    .getInstance()
                    .getString(
                        "SetInstancesPanel_SetInstancesFromFileQ_UrlName_JOptionPaneShowMessageDialog_Text_First"),
                Messages
                    .getInstance()
                    .getString(
                        "SetInstancesPanel_SetInstancesFromFileQ_UrlName_JOptionPaneShowMessageDialog_Text_Second"),
                JOptionPane.QUESTION_MESSAGE, null, null, m_LastURL);
        if (urlName != null) {
          m_LastURL = urlName;
          final URL url = new URL(urlName);
          m_IOThread = new Thread() {
            @Override
            public void run() {
              setInstancesFromURL(url);
              m_IOThread = null;
            }
          };
          m_IOThread.setPriority(Thread.MIN_PRIORITY); // UI has most priority
          m_IOThread.start();
        }
      } catch (Exception ex) {
        JOptionPane
            .showMessageDialog(
                this,
                Messages
                    .getInstance()
                    .getString(
                        "SetInstancesPanel_SetInstancesFromFileQ_Exception_JOptionPaneShowMessageDialog_Text_First")
                    + ex.getMessage(),
                Messages
                    .getInstance()
                    .getString(
                        "SetInstancesPanel_SetInstancesFromFileQ_Exception_JOptionPaneShowMessageDialog_Text_Second"),
                JOptionPane.ERROR_MESSAGE);
      }
    } else {
      JOptionPane
          .showMessageDialog(
              this,
              Messages
                  .getInstance()
                  .getString(
                      "SetInstancesPanel_SetInstancesFromFileQ_Exception_JOptionPaneShowMessageDialog_Text_Third"),
              Messages
                  .getInstance()
                  .getString(
                      "SetInstancesPanel_SetInstancesFromFileQ_Exception_JOptionPaneShowMessageDialog_Text_Fourth"),
              JOptionPane.WARNING_MESSAGE);
    }
  }

  /**
   * Loads results from a set of instances contained in the supplied file.
   * 
   * @param f a value of type 'File'
   */
  protected void setInstancesFromFile(File f) {
    boolean incremental = m_readIncrementally;

    try {
      m_Loader = ConverterUtils.getLoaderForFile(f);
      if (m_Loader == null)
        throw new Exception(Messages.getInstance().getString(
            "SetInstancesPanel_SetInstancesFromFile_Exception_Text_First")
            + f);

      // not an incremental loader?
      if (!(m_Loader instanceof IncrementalConverter))
        incremental = false;

      // load
      ((FileSourcedConverter) m_Loader).setFile(f);
      if (incremental) {
        m_Summary.setShowZeroInstancesAsUnknown(m_showZeroInstancesAsUnknown);
        setInstances(m_Loader.getStructure());
      } else {
        m_Summary.setShowZeroInstancesAsUnknown(false);
        setInstances(m_Loader.getDataSet());
      }
    } catch (Exception ex) {
      JOptionPane
          .showMessageDialog(
              this,
              Messages
                  .getInstance()
                  .getString(
                      "SetInstancesPanel_SetInstancesFromFile_Exception_JOptionPaneShowMessageDialog_Text_First")
                  + f.getName(),
              Messages
                  .getInstance()
                  .getString(
                      "SetInstancesPanel_SetInstancesFromFile_Exception_JOptionPaneShowMessageDialog_Text_Second"),
              JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Loads instances from a URL.
   * 
   * @param u the URL to load from.
   */
  protected void setInstancesFromURL(URL u) {
    boolean incremental = m_readIncrementally;

    try {
      m_Loader = ConverterUtils.getURLLoaderForFile(u.toString());
      if (m_Loader == null)
        throw new Exception(Messages.getInstance().getString(
            "SetInstancesPanel_SetInstancesFromURL_Exception_Text_First")
            + u);

      // not an incremental loader?
      if (!(m_Loader instanceof IncrementalConverter))
        incremental = false;

      // load
      ((URLSourcedLoader) m_Loader).setURL(u.toString());
      if (incremental) {
        m_Summary.setShowZeroInstancesAsUnknown(m_showZeroInstancesAsUnknown);
        setInstances(m_Loader.getStructure());
      } else {
        m_Summary.setShowZeroInstancesAsUnknown(false);
        setInstances(m_Loader.getDataSet());
      }
    } catch (Exception ex) {
      JOptionPane
          .showMessageDialog(
              this,
              Messages
                  .getInstance()
                  .getString(
                      "SetInstancesPanel_SetInstancesFromURL_Exception_JOptionPaneShowMessageDialog_Text_First")
                  + u,
              Messages
                  .getInstance()
                  .getString(
                      "SetInstancesPanel_SetInstancesFromURL_Exception_JOptionPaneShowMessageDialog_Text_Second"),
              JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * Updates the set of instances that is currently held by the panel
   * 
   * @param i a value of type 'Instances'
   */
  public void setInstances(Instances i) {

    m_Instances = i;
    m_Summary.setInstances(m_Instances);
    // Fire property change event for those interested.
    m_Support.firePropertyChange("", null, null);
  }

  /**
   * Gets the set of instances currently held by the panel
   * 
   * @return a value of type 'Instances'
   */
  public Instances getInstances() {

    return m_Instances;
  }

  /**
   * Gets the currently used Loader
   * 
   * @return a value of type 'Loader'
   */
  public weka.core.converters.Loader getLoader() {
    return m_Loader;
  }

  /**
   * Gets the instances summary panel associated with this panel
   * 
   * @return the instances summary panel
   */
  public InstancesSummaryPanel getSummary() {
    return m_Summary;
  }

  /**
   * Sets whether or not instances should be read incrementally by the Loader.
   * If incremental reading is used, then the client should obtain the Loader
   * object (by calling getLoader()) and read the instances one at a time. If
   * batch loading is used, then SetInstancesPanel will load the data into
   * memory inside of a separate thread and notify the client when the operation
   * is complete. The client can then retrieve the instances by calling
   * getInstances().
   * 
   * @param incremental true if instances are to be read incrementally
   * 
   */
  public void setReadIncrementally(boolean incremental) {
    m_readIncrementally = incremental;
  }

  /**
   * Gets whether instances are to be read incrementally or not
   * 
   * @return true if instances are to be read incrementally
   */
  public boolean getReadIncrementally() {
    return m_readIncrementally;
  }

  /**
   * Adds a PropertyChangeListener who will be notified of value changes.
   * 
   * @param l a value of type 'PropertyChangeListener'
   */
  @Override
  public void addPropertyChangeListener(PropertyChangeListener l) {
    m_Support.addPropertyChangeListener(l);
  }

  /**
   * Removes a PropertyChangeListener.
   * 
   * @param l a value of type 'PropertyChangeListener'
   */
  @Override
  public void removePropertyChangeListener(PropertyChangeListener l) {
    m_Support.removePropertyChangeListener(l);
  }
}
