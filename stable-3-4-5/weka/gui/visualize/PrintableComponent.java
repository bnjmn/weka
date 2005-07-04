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
  *    PrintableComponent.java
  *    Copyright (C) 2005 Fracpete, Dale Fletcher
  *
  */

package weka.gui.visualize;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JFileChooser;

import weka.core.RTSI;
import weka.gui.ExtensionFileFilter;

/** 
 * This class extends the component which is handed over in the constructor
 * by a print dialog.
 * The Print dialog is accessible via Ctrl-Shft-Left Mouse Click. <p>
 * The individual JComponentWriter-descendants can be accessed by the
 * <code>getWriter(String)</code> method, if the parameters need to be changed.
 *
 * @see #getWriters()
 * @see #getWriter(String)
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.1.2.2 $
 */
public class PrintableComponent implements PrintableHandler {
  /** the parent component of this print dialog */
  protected JComponent m_Component;
  
  /** the filechooser for saving the panel */
  protected static JFileChooser m_FileChooserPanel = null;
  
  /** the title of the save dialog */
  protected String m_SaveDialogTitle = "Save as...";
  
  /** the x scale factor */
  protected double m_xScale = 1.0;
  
  /** the y scale factor */
  protected double m_yScale = 1.0;
  
  /** whether to print some debug information */
  private static final boolean DEBUG = false;
  
  /** output if we're in debug mode */
  static {
    if (DEBUG)
      System.err.println(PrintablePanel.class.getName() + ": DEBUG ON");
  }
  
  /**
   * initializes the panel
   * 
   * @param component     the component to enhance with printing functionality
   */
  public PrintableComponent(JComponent component) {
    super();
    
    m_Component = component;
    getComponent().addMouseListener(new PrintMouseListener(this));
    getComponent().setToolTipText("Click left mouse button while holding <alt> and <shift> to display a save dialog.");
    initFileChooser();
  }
  
  /**
   * returns the GUI component this print dialog is part of
   */
  public JComponent getComponent() {
    return m_Component;
  }
  
  /**
   * initializes the filechooser, i.e. locates all the available writers in
   * the current package
   */
  protected void initFileChooser() {
    Vector              writerNames;
    int                 i;
    Class               cls;
    JComponentWriter    writer;

    // already initialized?
    if (m_FileChooserPanel != null)
      return;

    m_FileChooserPanel = new JFileChooser();
    m_FileChooserPanel.resetChoosableFileFilters();
    m_FileChooserPanel.setAcceptAllFileFilterUsed(false);

    // determine all available writers and add them to the filechooser
    writerNames = RTSI.find(JComponentWriter.class.getPackage().getName(), JComponentWriter.class.getName());
    Collections.sort(writerNames);
    for (i = 0; i < writerNames.size(); i++) {
      try {
        cls    = Class.forName(writerNames.get(i).toString());
        writer = (JComponentWriter) cls.newInstance();
        m_FileChooserPanel.addChoosableFileFilter(
            new PrintableComponent.JComponentWriterFileFilter(writer.getExtension(), writer.getDescription(), writer));
      }
      catch (Exception e) {
        System.err.println(writerNames.get(i) + ": " + e);
      }
    }
    
    // set first filter as active filter
    if (m_FileChooserPanel.getChoosableFileFilters().length > 0)
      m_FileChooserPanel.setFileFilter(m_FileChooserPanel.getChoosableFileFilters()[0]);
  }
  
  /**
   * returns a Hashtable with the current available JComponentWriters in the 
   * save dialog. the key of the Hashtable is the description of the writer.
   * 
   * @return all currently available JComponentWriters 
   * @see JComponentWriter#getDescription()
   */
  public Hashtable getWriters() {
    Hashtable         result;
    int               i;
    JComponentWriter  writer;
    
    result = new Hashtable();
    
    for (i = 0; i < m_FileChooserPanel.getChoosableFileFilters().length; i++) {
      writer = ((JComponentWriterFileFilter) m_FileChooserPanel.getChoosableFileFilters()[i]).getWriter();
      result.put(writer.getDescription(), writer);
    }
    
    return result;
  }
  
  /**
   * returns the JComponentWriter associated with the given name, is 
   * <code>null</code> if not found
   * 
   * @return the writer associated with the given name
   * @see JComponentWriter#getDescription()
   */
  public JComponentWriter getWriter(String name) {
    return (JComponentWriter) getWriters().get(name);
  }

  /**
   * sets the title for the save dialog
   */
  public void setSaveDialogTitle(String title) {
    m_SaveDialogTitle = title;
  }
  
  /**
   * returns the title for the save dialog
   */
  public String getSaveDialogTitle() {
    return m_SaveDialogTitle;
  }
  
  /**
   * sets the scale factor
   * @param x the scale factor for the x-axis 
   * @param y the scale factor for the y-axis 
   */
  public void setScale(double x, double y) {
    m_xScale = x;
    m_yScale = y;
    if (DEBUG)
      System.err.println("x = " + x + ", y = " + y);
  }
  
  /**
   * returns the scale factor for the x-axis
   */
  public double getXScale() {
    return m_xScale;
  }
  
  /**
   * returns the scale factor for the y-axis
   */
  public double getYScale() {
    return m_xScale;
  }
  
  /**
   * displays a save dialog for saving the panel to a file.  
   * Fixes a bug with the Swing JFileChooser: if you entered a new
   * filename in the save dialog and press Enter the <code>getSelectedFile</code>
   * method returns <code>null</code> instead of the filename.<br>
   * To solve this annoying behavior we call the save dialog once again s.t. the
   * filename is set. Might look a little bit strange to the user, but no 
   * NullPointerException! ;-)
   */
  public void saveComponent() {
    int                           result;
    JComponentWriter              writer;
    File                          file;
    JComponentWriterFileFilter    filter;
    
    // display save dialog
    m_FileChooserPanel.setDialogTitle(getSaveDialogTitle());
    do {
      result = m_FileChooserPanel.showSaveDialog(getComponent());
      if (result != JFileChooser.APPROVE_OPTION)
        return;
    }
    while (m_FileChooserPanel.getSelectedFile() == null);
    
    // save the file
    try {
      filter = (JComponentWriterFileFilter) m_FileChooserPanel.getFileFilter();
      file   = m_FileChooserPanel.getSelectedFile();
      writer = filter.getWriter();
      if (!file.getAbsolutePath().toLowerCase().endsWith(writer.getExtension().toLowerCase()))
        file = new File(file.getAbsolutePath() + writer.getExtension()); 
      writer.setComponent(getComponent());
      writer.setFile(file);
      writer.setScale(getXScale(), getYScale());
      writer.toOutput();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  /**
   * a specialized filter that also contains the associated filter class
   */
  protected class JComponentWriterFileFilter extends ExtensionFileFilter {
    /** the associated writer */
    private JComponentWriter m_Writer; 
    
    /**
     * Creates the ExtensionFileFilter
     *
     * @param extension       the extension of accepted files.
     * @param description     a text description of accepted files.
     * @param writer          the associated writer 
     */
    public JComponentWriterFileFilter(String extension, String description, JComponentWriter writer) {
      super(extension, description);
      m_Writer = writer;
    }
    
    /**
     * returns the associated wrtier
     */
    public JComponentWriter getWriter() {
      return m_Writer;
    }
  }

  /**
   * The listener to wait for Ctrl-Shft-Left Mouse Click
   */
  private class PrintMouseListener extends MouseAdapter{
    private PrintableComponent m_Component;
    
    /**
     * initializes the listener
     */
    PrintMouseListener(PrintableComponent component){
      m_Component = component;
    }
    
    /**
     * Invoked when the mouse has been clicked on a component.
     */
    public void mouseClicked(MouseEvent e) {
      int modifiers = e.getModifiers();
      if (((modifiers & MouseEvent.SHIFT_MASK) == MouseEvent.SHIFT_MASK) && 
          ((modifiers & MouseEvent.ALT_MASK) == MouseEvent.ALT_MASK) &&
          ((modifiers & MouseEvent.BUTTON1_MASK) == MouseEvent.BUTTON1_MASK)) {
        e.consume();
        m_Component.saveComponent();
      }
    }
  }
}
