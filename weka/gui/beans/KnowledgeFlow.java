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
 *    KnowledgeFlow.java
 *    Copyright (C) 2002 Mark Hall
 *
 */

package weka.gui.beans;

import weka.core.Utils;
import weka.gui.ListSelectorDialog;
import weka.gui.LogPanel;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Properties;
import java.util.Enumeration;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.beans.Customizer;
import java.beans.EventSetDescriptor;
import java.beans.Beans;
import java.beans.PropertyDescriptor;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.JButton;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.JScrollPane;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;
import javax.swing.JFileChooser;


import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import java.awt.Point;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Insets;
import java.awt.Component;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.IntrospectionException;

/**
 * Main GUI class for the KnowledgeFlow
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version  $Revision: 1.2 $
 * @since 1.0
 * @see JPanel
 * @see PropertyChangeListener
 */
public class KnowledgeFlow extends JPanel implements PropertyChangeListener {

  /**
   * Location of the property file for the KnowledgeFlow
   */
  protected static String PROPERTY_FILE = "weka/gui/beans/Beans.props";

  /** Contains the editor properties */
  private static Properties BEAN_PROPERTIES;

  /**
   * Holds the details needed to construct button bars for various supported
   * classes of weka algorithms/tools 
   */
  private static Vector TOOLBARS = new Vector();

  /** Loads the configuration property file */
  static {

    // Allow a properties file in the current directory to override
    try {
      BEAN_PROPERTIES = Utils.readProperties(PROPERTY_FILE);
      java.util.Enumeration keys =
        (java.util.Enumeration)BEAN_PROPERTIES.propertyNames();
      if (!keys.hasMoreElements()) {
        throw new Exception( "Could not read a configuration file for the bean\n"
         +"panel. An example file is included with the Weka distribution.\n"
         +"This file should be named \"" + PROPERTY_FILE + "\" and\n"
         +"should be placed either in your user home (which is set\n"
         + "to \"" + System.getProperties().getProperty("user.home") + "\")\n"
         + "or the directory that java was started from\n");
      }
     
      String standardToolBarNames = 
	BEAN_PROPERTIES.
	getProperty("weka.gui.beans.KnowledgeFlow.standardToolBars");
      StringTokenizer st = new StringTokenizer(standardToolBarNames, ", ");
       while (st.hasMoreTokens()) {
	 String tempBarName = st.nextToken().trim();
	 // construct details for this toolbar
	 Vector newV = new Vector();
	 // add the name of the toolbar
	 newV.addElement(tempBarName);

	 // indicate that this is a standard toolbar (no wrapper bean)
	 newV.addElement("null");
	 String toolBarContents = 
	   BEAN_PROPERTIES.
	   getProperty("weka.gui.beans.KnowledgeFlow."+tempBarName);
	 StringTokenizer st2 = new StringTokenizer(toolBarContents, ", ");
	 while (st2.hasMoreTokens()) {
	   String tempBeanName = st2.nextToken().trim();
	   newV.addElement(tempBeanName);
	 }
	 TOOLBARS.addElement(newV);
       }       
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(null,
				    ex.getMessage(),
				    "KnowledgeFlow",
				    JOptionPane.ERROR_MESSAGE);
    }

    try {
      /* now process the keys in the GenericObjectEditor.props. For each
       key that has an entry in the Beans.props associating it with a
      bean component a button tool bar will be created */
      Properties GEOProps = 
	Utils.readProperties("weka/gui/GenericObjectEditor.props");
      Enumeration en = GEOProps.propertyNames();
      while (en.hasMoreElements()) {
	String geoKey = (String)en.nextElement();
	// try to match this key with one in the Beans.props file
	String beanCompName = BEAN_PROPERTIES.getProperty(geoKey);
	if (beanCompName != null) {
	  // add details necessary to construct a button bar for this class
	  // of algorithms
	  Vector newV = new Vector();
	  // check for a naming alias for this toolbar
	  String toolBarNameAlias = 
	    BEAN_PROPERTIES.getProperty(geoKey+".alias");
	  String toolBarName = (toolBarNameAlias != null) ?
	    toolBarNameAlias :
	    geoKey.substring(geoKey.lastIndexOf('.')+1, geoKey.length());
	  // Name for the toolbar (name of weka algorithm class)
	  newV.addElement(toolBarName);
	  // Name of bean capable of handling this class of algorithm
	  newV.addElement(beanCompName);
	  // All the weka algorithms of this class of algorithm
	  String wekaAlgs = GEOProps.getProperty(geoKey);
	  StringTokenizer st = new StringTokenizer(wekaAlgs, ", ");
	  while (st.hasMoreTokens()) {
	    String current = st.nextToken().trim();
	    newV.addElement(current);
	  }
	  TOOLBARS.addElement(newV);
	}
      }      
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(null,
          "Could not read a configuration file for the generic objecte editor"
         +". An example file is included with the Weka distribution.\n"
         +"This file should be named \"GenericObjectEditor.props\" and\n"
         +"should be placed either in your user home (which is set\n"
         + "to \"" + System.getProperties().getProperty("user.home") + "\")\n"
         + "or the directory that java was started from\n",
         "KnowledgeFlow",
         JOptionPane.ERROR_MESSAGE);
    }
  } 
  
  /**
   * Used for displaying the bean components and their visible
   * connections
   *
   * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
   * @version $Revision: 1.2 $
   * @since 1.0
   * @see JPanel
   */
  protected class BeanLayout extends JPanel {
    public void paintComponent(Graphics gx) {
      super.paintComponent(gx);
      BeanInstance.paintLabels(gx);
      BeanConnection.paintConnections(gx);
      //      BeanInstance.paintConnections(gx);
    }

    public void doLayout() {
      super.doLayout();
      Vector comps = BeanInstance.getBeanInstances();
      for (int i = 0; i < comps.size(); i++) {
	BeanInstance bi = (BeanInstance)comps.elementAt(i);
	JComponent c = (JComponent)bi.getBean();
	Dimension d = c.getPreferredSize();
	c.setBounds(bi.getX(), bi.getY(), d.width, d.height);
	c.revalidate();
      }
    }
  }

  // constants for operations in progress
  protected static final int NONE = 0;
  protected static final int MOVING = 1;
  protected static final int CONNECTING = 2;
  protected static final int ADDING = 3;

  // which operation is in progress
  private int m_mode = NONE;

  /**
   * Button group to manage all toolbar buttons
   */
  private ButtonGroup m_toolBarGroup = new ButtonGroup();

  /**
   * Holds the selected toolbar bean
   */
  private Object m_toolBarBean;

  /**
   * The layout area
   */
  private BeanLayout m_beanLayout = new BeanLayout();

  /**
   * Tabbed pane to hold tool bars
   */
  private JTabbedPane m_toolBars = new JTabbedPane();

  private JToggleButton m_pointerB;
  private JButton m_saveB;
  private JButton m_loadB;
  private JButton m_stopB;

  /**
   * Reference to bean being manipulated
   */
  private BeanInstance m_editElement;

  /**
   * Event set descriptor for the bean being manipulated
   */
  private EventSetDescriptor m_sourceEventSetDescriptor;

  /**
   * Used to record screen coordinates during move and connect
   * operations
   */
  private int m_oldX, m_oldY;
  private int m_startX, m_startY;
  
  /** The file chooser for selecting layout files */
  protected JFileChooser m_FileChooser 
    = new JFileChooser(new File(System.getProperty("user.dir")));

  protected LogPanel m_logPanel = new LogPanel();

  /**
   * Creates a new <code>KnowledgeFlow</code> instance.
   */
  public KnowledgeFlow() {

    m_beanLayout.setLayout(null);

    // handle mouse events
    m_beanLayout.addMouseListener(new MouseAdapter() {

	public void mousePressed(MouseEvent me) {
	  if (m_toolBarBean == null) {
	    if (((me.getModifiers() & InputEvent.BUTTON1_MASK)
		 == InputEvent.BUTTON1_MASK) && m_mode == NONE) {
	      BeanInstance bi = BeanInstance.findInstance(me.getPoint());
	      JComponent bc = null;
	      if (bi != null) {
		bc = (JComponent)(bi.getBean());
	      }
	      if (bc != null && (bc instanceof Visible)) {
		ImageIcon ic = ((Visible)bc).getVisual().getStaticIcon();
		//		bc.setVisible(false); // hide the component
		m_editElement = bi;
		int width = ic.getIconWidth() / 2;
		int height = ic.getIconHeight() / 2;

		Graphics2D g = (Graphics2D)m_beanLayout.getGraphics();
		//		g.setPaint(java.awt.Color.black);
		g.setXORMode(java.awt.Color.white);
		ic.paintIcon(m_beanLayout, g, me.getX()-width, 
			     me.getY()-height);
		m_oldX = me.getX();
		m_oldY = me.getY();
		g.dispose();
		m_mode = MOVING;
	      }
	    }
	  }
	}

	public void mouseReleased(MouseEvent me) {
	  if (m_editElement != null && m_mode == MOVING) {
	    ImageIcon ic = ((Visible)m_editElement.getBean()).
	      getVisual().getStaticIcon();
	    int width = ic.getIconWidth() / 2;
	    int height = ic.getIconHeight() / 2;
	    Graphics2D g = (Graphics2D)m_beanLayout.getGraphics();
	    //		g.setPaint(java.awt.Color.black);
	    g.setXORMode(java.awt.Color.white);
	    ic.paintIcon(m_beanLayout, g, me.getX()-width, me.getY()-height);
	    //	    m_editElement.setLocation(me.getX(), me.getY());
	    m_editElement.setX(me.getX()-width);
	    m_editElement.setY(me.getY()-height);
	    //	    m_editElement.validate();
	    revalidate();
	    //	    m_editElement.setVisible(true);
	    m_editElement = null;
	    g.dispose();
	    revalidate();
	    m_beanLayout.repaint();
	    m_mode = NONE;
	  }
	}

	public void mouseClicked(MouseEvent me) {
	  BeanInstance bi = BeanInstance.findInstance(me.getPoint());
	  if (m_mode == ADDING || m_mode == NONE) {
	    // try and popup a context sensitive menu if we have
	    // been clicked over a bean.
	    if (bi != null) {
	      JComponent bc = (JComponent)bi.getBean();
	      if ((me.getModifiers() & InputEvent.BUTTON1_MASK)
		  != InputEvent.BUTTON1_MASK) {
		doPopup(me.getPoint(), bi, me.getX(), me.getY());
	      }
	    } else {
	      if ((me.getModifiers() & InputEvent.BUTTON1_MASK)
		  != InputEvent.BUTTON1_MASK) {
		// find connections if any close to this point
		int delta = 10;
		deleteConnectionPopup(BeanConnection.
		      getClosestConnections(new Point(me.getX(), me.getY()), 
					    delta), me.getX(), me.getY());
	      } else if (m_toolBarBean != null) {
		// otherwise, if a toolbar button is active then 
		// add the component
		addComponent(me.getX(), me.getY());
	      }
	    }
	  }
	
	  if (m_mode == CONNECTING) {
	    // undraw rubberbanding line and turn off connecting points
	    Vector beanInstances = BeanInstance.getBeanInstances();
	    for (int i = 0; i < beanInstances.size(); i++) {
	      JComponent bean = 
		(JComponent)((BeanInstance)beanInstances.elementAt(i)).
		getBean();
	      if (bean instanceof Visible) {
		((Visible)bean).getVisual().setDisplayConnectors(false);
	      }
	    }
	    Graphics2D gx = (Graphics2D)m_beanLayout.getGraphics();
	    gx.setXORMode(java.awt.Color.white);
	    // remove the old rubberbanded line
	    gx.drawLine(m_startX, m_startY, m_oldX, m_oldY);

	    if (bi != null) {
	      boolean doConnection = false;
	      if (!(bi.getBean() instanceof BeanCommon)) {
		doConnection = true;
	      } else {
		// Give the target bean a chance to veto the proposed
		// connection
		if (((BeanCommon)bi.getBean()).
		    connectionAllowed(m_sourceEventSetDescriptor.getName())) {
		  doConnection = true;
		}
	      }
	      if (doConnection) {
		// attempt to connect source and target beans
		BeanConnection bc = 
		  new BeanConnection(m_editElement, bi, 
				     m_sourceEventSetDescriptor);
	      }
	      m_beanLayout.repaint();
	    }
	    gx.dispose();
	    m_mode = NONE;
	    m_editElement = null;
	    m_sourceEventSetDescriptor = null;
	  }
	}
      });
    
     m_beanLayout.addMouseMotionListener(new MouseMotionAdapter() {

	public void mouseDragged(MouseEvent me) {
	  if (m_editElement != null && m_mode == MOVING) {
	    ImageIcon ic = ((Visible)m_editElement.getBean()).
	      getVisual().getStaticIcon();
	    int width = ic.getIconWidth() / 2;
	    int height = ic.getIconHeight() / 2;

	    Graphics2D g = (Graphics2D)m_beanLayout.getGraphics();
	    
	    // undraw the old image
	    g.setXORMode(java.awt.Color.white);
	    ic.paintIcon(m_beanLayout, g, m_oldX-width, m_oldY-height);
	    
	    // note the new points
	    m_oldX = me.getX(); m_oldY = me.getY();

	    // draw the image
	    ic.paintIcon(m_beanLayout, g, m_oldX-width, m_oldY-height);
	    g.dispose();
	  }
	}

	 public void mouseMoved(MouseEvent e) {
	   if (m_mode == CONNECTING) {
	     Graphics2D gx = (Graphics2D)m_beanLayout.getGraphics();
	     gx.setXORMode(java.awt.Color.white);
	     
	     // remove the old rubberbanded line
	     gx.drawLine(m_startX, m_startY, m_oldX, m_oldY);
	     // note the new coordinates
	     m_oldX = e.getX(); m_oldY = e.getY();
	     // draw the new rubberbanded line
	     gx.drawLine(m_startX, m_startY, m_oldX, m_oldY);
	     gx.dispose();
	   }
	 }
       });
     
     String date = (new SimpleDateFormat("EEEE, d MMMM yyyy"))
       .format(new Date());
     m_logPanel.logMessage("Weka Knowledge Flow was written by Mark Hall");
     m_logPanel.logMessage("Weka Knowledge Flow");
     m_logPanel.logMessage("(c) 2002 Mark Hall");
     m_logPanel.logMessage("web: http://www.cs.waikato.ac.nz/~ml/");
     m_logPanel.logMessage("email: wekasupport@cs.waikato.ac.nz");
     m_logPanel.logMessage( date);
     m_logPanel.statusMessage("Welcolme to the Weka Knowledge Flow");
    
     JPanel p1 = new JPanel();
     p1.setLayout(new BorderLayout());
     p1.setBorder(javax.swing.BorderFactory.createCompoundBorder(
			    javax.swing.BorderFactory.
			    createTitledBorder("Knowledge Flow Layout"),
                   javax.swing.BorderFactory.createEmptyBorder(0, 5, 5, 5)
                   ));
     final JScrollPane js = new JScrollPane(m_beanLayout);
     p1.add(js, BorderLayout.CENTER);

     setLayout(new BorderLayout());
     
     add(p1, BorderLayout.CENTER);
     m_beanLayout.setSize(1024, 768);
     Dimension d = m_beanLayout.getPreferredSize();
     m_beanLayout.setMinimumSize(d);
     m_beanLayout.setMaximumSize(d);
     m_beanLayout.setPreferredSize(d);

     add(m_logPanel, BorderLayout.SOUTH);
     
     setUpToolBars();
  }
  
  private Image loadImage(String path) {
    Image pic = null;
    java.net.URL imageURL = ClassLoader.getSystemResource(path);
    if (imageURL == null) {
      System.err.println("Warning: unable to load "+path);
    } else {
      pic = Toolkit.getDefaultToolkit().
	getImage(imageURL);
    }
    return pic;
  }

  /**
   * Describe <code>setUpToolBars</code> method here.
   */
  private void setUpToolBars() {
    JPanel toolBarPanel = new JPanel();
    toolBarPanel.setLayout(new BorderLayout());

    // first construct the toolbar for saving, loading etc
    JToolBar fixedTools = new JToolBar();
    fixedTools.setOrientation(JToolBar.VERTICAL);
    m_saveB = new JButton(new ImageIcon(loadImage(BeanVisual.ICON_PATH
						  +"Save24.gif")));
    m_saveB.setToolTipText("Save layout");
    m_loadB = new JButton(new ImageIcon(loadImage(BeanVisual.ICON_PATH
						  +"Open24.gif")));
    m_stopB = new JButton(new ImageIcon(loadImage(BeanVisual.ICON_PATH
						  +"Stop24.gif")));
    m_stopB.setToolTipText("Stop all execution");
    m_loadB.setToolTipText("Load layout");
    Image tempI = loadImage(BeanVisual.ICON_PATH+"Pointer.gif");
    m_pointerB = new JToggleButton(new ImageIcon(tempI));
    m_pointerB.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  m_toolBarBean = null;
	  m_mode = NONE;
	  setCursor(Cursor.
		    getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
      });

    m_toolBarGroup.add(m_pointerB);
    fixedTools.add(m_pointerB);
    fixedTools.add(m_saveB);
    fixedTools.add(m_loadB);
    fixedTools.add(m_stopB);
    Dimension dP = m_saveB.getPreferredSize();
    Dimension dM = m_saveB.getMaximumSize();
    fixedTools.setFloatable(false);
    m_pointerB.setPreferredSize(dP);
    m_pointerB.setMaximumSize(dM);
    toolBarPanel.add(fixedTools, BorderLayout.WEST);

    m_saveB.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  saveLayout();
	}
      });

    m_loadB.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  loadLayout();
	}
      });

    m_stopB.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  Vector components = BeanInstance.getBeanInstances();
	  for (int i = 0; i < components.size(); i++) {
	    Object temp = ((BeanInstance)components.elementAt(i)).getBean();
	    if (temp instanceof BeanCommon) {
	      ((BeanCommon)temp).stop();
	    }
	  }
	}
      });

    final int STANDARD_TOOLBAR = 0;
    final int WEKAWRAPPER_TOOLBAR = 1;

    int toolBarType = STANDARD_TOOLBAR;
    // set up wrapper toolbars
    for (int i = 0; i < TOOLBARS.size(); i++) {
      Vector tempBarSpecs = (Vector)TOOLBARS.elementAt(i);
      
      // name for the tool bar
      String tempBarName = (String)tempBarSpecs.elementAt(0);
      // name of the bean component to handle this class of weka algorithms
      String tempBeanCompName = (String)tempBarSpecs.elementAt(1);
      Object tempBean;
      
      // Is this a wrapper toolbar?
      if (tempBeanCompName.compareTo("null") != 0) {
	tempBean = null;
	toolBarType = WEKAWRAPPER_TOOLBAR;
	try {
	  tempBean = Beans.instantiate(null, tempBeanCompName);
	} catch (Exception ex) {
	  // ignore
	  System.err.println("Failed to instantiate: "+tempBeanCompName);
	  break;
	}
      } else {
	toolBarType = STANDARD_TOOLBAR;
      }
      // a toolbar to hold buttons---one for each algorithm
      JToolBar tempToolBar = new JToolBar();

      for (int j = 2; j < tempBarSpecs.size(); j++) {
	tempBean = null;
	String algName = "";
	if (toolBarType == WEKAWRAPPER_TOOLBAR) {
	  try {
	    tempBean = Beans.instantiate(null, tempBeanCompName);
	  } catch (Exception ex) {
	    System.err.println("Failed to instantiate :"+tempBeanCompName
			       +"KnowledgeFlow.setUpToolBars()");
	    break;
	  }
	  if (tempBean instanceof WekaWrapper) {
	    algName = (String)tempBarSpecs.elementAt(j);
	    Class c = null;
	    try {
	      c = Class.forName(algName);
	    } catch (Exception ex) {
	      System.err.println("Can't find class called: "+algName);
	    }
	    try {
	      Object o = c.newInstance();
	      ((WekaWrapper)tempBean).setWrappedAlgorithm(o);
	    } catch (Exception ex) {
	      System.err.println("Failed to configure "+tempBeanCompName
				 +" with "+algName);
	      tempBean = null;
	    }
	  }
	} else {
	  tempBeanCompName = (String)tempBarSpecs.elementAt(j);
	  try {
	    tempBean = Beans.instantiate(null, tempBeanCompName);
	  } catch (Exception ex) {
	    System.err.println("Failed to instantiate :"+tempBeanCompName
			       +"KnowledgeFlow.setUpToolBars()");
	    break;
	  }
	}

	if (tempBean != null) {
	  // add a button with the bean's visual to the toolbar
	  JToggleButton tempButton;
	  JPanel tempP = new JPanel();
	  JLabel tempL = new JLabel();
	  tempL.setFont(new Font("Monospaced", Font.PLAIN, 10));
	  String labelName = (toolBarType == WEKAWRAPPER_TOOLBAR) ?
	    algName :
	    tempBeanCompName;
	  tempL.setText(" "+labelName.
			substring(labelName.lastIndexOf('.')+1, 
				  labelName.length())+" ");
	  tempL.setHorizontalAlignment(JLabel.CENTER);
	  tempP.setLayout(new BorderLayout());
	  if (tempBean instanceof Visible) {
	    BeanVisual bv = ((Visible)tempBean).getVisual();
	    tempButton = 
	      new JToggleButton(bv.getStaticIcon());
	  } else {
	    tempButton = new JToggleButton();
	  }
	  tempP.add(tempButton, BorderLayout.CENTER);
	  tempP.add(tempL, BorderLayout.SOUTH);
	  //	  tempToolBar.add(tempButton);
	  tempToolBar.add(tempP);
	  m_toolBarGroup.add(tempButton);

	  // add an action listener for the button here
	  final String tempName = tempBeanCompName;
	  final Object tempBN = tempBean;
	  //	  final JToggleButton tempButton2 = tempButton;
	  tempButton.addActionListener(new ActionListener() {
	      public void actionPerformed(ActionEvent e) {
		try {
		  m_toolBarBean = null;
		  m_toolBarBean = Beans.instantiate(null, tempName);
		  if (m_toolBarBean instanceof WekaWrapper) {
		    Object wrappedAlg = 
		      ((WekaWrapper)tempBN).getWrappedAlgorithm();
		    
		    ((WekaWrapper)m_toolBarBean).setWrappedAlgorithm(wrappedAlg.getClass().newInstance());
		    //		    tempButton2.setSelected(false);
		  }
		  setCursor(Cursor.
			    getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		  m_mode = ADDING;
		} catch (Exception ex) {
		  System.err.
		    println("Problem adding bean to data flow layout");
		}
	      }
	    });
	}
      }

      // ok, now create tabbed pane to hold this toolbar
      JScrollPane tempJScrollPane = new JScrollPane(tempToolBar, 
					JScrollPane.VERTICAL_SCROLLBAR_NEVER,
					JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

      Dimension d = tempToolBar.getPreferredSize();
      tempJScrollPane.setMinimumSize(new Dimension((int)d.getWidth(),
						   (int)(d.getHeight()+15)));
      tempJScrollPane.setPreferredSize(new Dimension((int)d.getWidth(),
						    (int)(d.getHeight()+15)));
      m_toolBars.addTab(tempBarName, null, 
			tempJScrollPane,
			tempBarName);
      
    }
    toolBarPanel.add(m_toolBars, BorderLayout.CENTER);

    //    add(m_toolBars, BorderLayout.NORTH);
    add(toolBarPanel, BorderLayout.NORTH);
  }

  /**
   * Popup a context sensitive menu for the bean component
   *
   * @param pt holds the panel coordinates for the component
   * @param bi the bean component over which the user right clicked the mouse
   * @param x the x coordinate at which to popup the menu
   * @param y the y coordinate at which to popup the menu
   */
  private void doPopup(Point pt, final BeanInstance bi,
		       int x, int y) {

    final JComponent bc = (JComponent)bi.getBean();
    final int xx = x;
    final int yy = y;
    int menuItemCount = 0;
    JPopupMenu beanContextMenu = new JPopupMenu();

    beanContextMenu.insert(new JLabel("Edit", 
				      SwingConstants.CENTER), 
			   menuItemCount);
    menuItemCount++;
    JMenuItem deleteItem = new JMenuItem("Delete");
    deleteItem.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  BeanConnection.removeConnections(bi);
	  bi.removeBean(m_beanLayout);
	  revalidate();
	}
      });
    beanContextMenu.add(deleteItem);
    menuItemCount++;
    // first determine if there is a customizer for this bean
    try {
      BeanInfo compInfo = Introspector.getBeanInfo(bc.getClass());
      if (compInfo == null) {
	System.err.println("Error");
      } else {
	System.err.println("Got bean info");
	final Class custClass = 
	  compInfo.getBeanDescriptor().getCustomizerClass();
	if (custClass != null) {
	  System.err.println("Got customizer class");
	  //	  popupCustomizer(custClass, bc);
	  JMenuItem custItem = new JMenuItem("Configure...");
	  custItem.addActionListener(new ActionListener() {
	      public void actionPerformed(ActionEvent e) {
		popupCustomizer(custClass, bc);
	      }
	    });
	  beanContextMenu.add(custItem);
	  menuItemCount++;
	} else {
	  System.err.println("No customizer class");
	}
	EventSetDescriptor [] esds = compInfo.getEventSetDescriptors();
	if (esds != null && esds.length > 0) {
	  beanContextMenu.insert(new JLabel("Connections", 
					    SwingConstants.CENTER), 
				 menuItemCount);
	  menuItemCount++;
	}
	for (int i = 0; i < esds.length; i++) {
	  //	  System.err.println(esds[i].getName());
	  // add each event name to the menu
	  JMenuItem evntItem = new JMenuItem(esds[i].getName());
	  final EventSetDescriptor esd = esds[i];
	  // Check EventConstraints (if any) here
	  boolean ok = true;
	  if (bc instanceof EventConstraints) {
	    ok = ((EventConstraints) bc).eventGeneratable(esd.getName());
	  }
	  if (ok) {
	    evntItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		  connectComponents(esd, bi, xx, yy);
		}
	      });
	  } else {
	    evntItem.setEnabled(false);
	  }
	  beanContextMenu.add(evntItem);
	  menuItemCount++;
	}
      }
    } catch (IntrospectionException ie) {
      ie.printStackTrace();
    }
    System.err.println("Just before look for other options");
    // now look for other options for this bean
    if (bc instanceof UserRequestAcceptor) {
      Enumeration req = ((UserRequestAcceptor)bc).enumerateRequests();
      if (req.hasMoreElements()) {
	beanContextMenu.insert(new JLabel("Actions", 
					  SwingConstants.CENTER), 
			       menuItemCount);
	menuItemCount++;
      }
      while (req.hasMoreElements()) {
	final String tempS = (String)req.nextElement();
	JMenuItem custItem = new JMenuItem(tempS);
	custItem.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
	      ((UserRequestAcceptor)bc).performRequest(tempS);
	      
	    }
	  });
	beanContextMenu.add(custItem);
	menuItemCount++;
      }
    }
    System.err.println("Just before showing menu");
    // popup the menu
    if (menuItemCount > 0) {
      beanContextMenu.show(m_beanLayout, x, y);
    }
  }

  /**
   * Popup the customizer for this bean
   *
   * @param custClass the class of the customizer
   * @param bc the bean to be customized
   */
  private void popupCustomizer(Class custClass, JComponent bc) {
    try {
      // instantiate
      Object customizer = custClass.newInstance();
      ((Customizer)customizer).setObject(bc);
      final javax.swing.JFrame jf = new javax.swing.JFrame();
      jf.getContentPane().setLayout(new BorderLayout());
      jf.getContentPane().add((JComponent)customizer, BorderLayout.CENTER);
      jf.addWindowListener(new java.awt.event.WindowAdapter() {
	  public void windowClosing(java.awt.event.WindowEvent e) {
	    jf.dispose();
	  }
	});
      jf.pack();
      jf.setVisible(true);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Popup a menu giving choices for connections to delete (if any)
   *
   * @param closestConnections a vector containing 0 or more BeanConnections
   * @param x the x coordinate at which to popup the menu
   * @param y the y coordinate at which to popup the menu
   */
  private void deleteConnectionPopup(Vector closestConnections,
				     int x, int y) {
    if (closestConnections.size() > 0) {
      int menuItemCount = 0;
      JPopupMenu deleteConnectionMenu = new JPopupMenu();

      deleteConnectionMenu.insert(new JLabel("Delete Connection", 
					     SwingConstants.CENTER), 
				  menuItemCount);
      menuItemCount++;
      for (int i = 0; i < closestConnections.size(); i++) {
	final BeanConnection bc = 
	  (BeanConnection)closestConnections.elementAt(i);
	String connName = bc.getSourceEventSetDescriptor().getName();
	JMenuItem deleteItem = new JMenuItem(connName);
	deleteItem.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
	      bc.remove();
	      m_beanLayout.revalidate();
	      m_beanLayout.repaint();
	    }
	  });
	deleteConnectionMenu.add(deleteItem);
	menuItemCount++;
      }
      deleteConnectionMenu.show(m_beanLayout, x, y);
    }
  }

  /**
   * Initiates the connection process for two beans
   *
   * @param esd the EventSetDescriptor for the source bean
   * @param bi the source bean
   * @param x the x coordinate to start connecting from
   * @param y the y coordinate to start connecting from
   */
  private void connectComponents(EventSetDescriptor esd, 
				 BeanInstance bi,
				 int x,
				 int y) {
    // record the event set descriptior for this event
    m_sourceEventSetDescriptor = esd;

    Class listenerClass = esd.getListenerType(); // class of the listener
    JComponent source = (JComponent)bi.getBean();
    // now determine which (if any) of the other beans implement this
    // listener
    int targetCount = 0;
    Vector beanInstances = BeanInstance.getBeanInstances();
    for (int i = 0; i < beanInstances.size(); i++) {
      JComponent bean = 
	(JComponent)((BeanInstance)beanInstances.elementAt(i)).getBean();
      boolean connectable = false;
      if (listenerClass.isInstance(bean) && bean != source) {
	if (!(bean instanceof BeanCommon)) {
	  connectable = true; // assume this bean is happy to receive a connection
	} else {
	  // give this bean a chance to veto any proposed connection via
	  // the listener interface
	  if (((BeanCommon)bean).
	      connectionAllowed(esd.getName())) {
	    connectable = true;
	  }
	}
	if (connectable) {
	  if (bean instanceof Visible) {
	    targetCount++;
	    ((Visible)bean).getVisual().setDisplayConnectors(true);
	  }
	}
      }
    }
    
    // have some possible beans to connect to?
    if (targetCount > 0) {
      //      System.err.println("target count "+targetCount);
      if (source instanceof Visible) {
	((Visible)source).getVisual().setDisplayConnectors(true);
      }

      m_editElement = bi;
      Point closest = ((Visible)source).getVisual().
	getClosestConnectorPoint(new Point(x, y));

      m_startX = (int)closest.getX();
      m_startY = (int)closest.getY();
      m_oldX = m_startX;
      m_oldY = m_startY;

      Graphics2D gx = (Graphics2D)m_beanLayout.getGraphics();
      gx.setXORMode(java.awt.Color.white);
      gx.drawLine(m_startX, m_startY, m_startX, m_startY);
      gx.dispose();
      m_mode = CONNECTING;
    }
  }

  private void addComponent(int x, int y) {
    BeanInstance bi = new BeanInstance(m_beanLayout, m_toolBarBean, x, y);
    //    addBean((JComponent)bi.getBean());

    if (m_toolBarBean instanceof Visible) {
      ((Visible)m_toolBarBean).getVisual().addPropertyChangeListener(this);
    }
    if (m_toolBarBean instanceof BeanCommon) {
      ((BeanCommon)m_toolBarBean).setLog(m_logPanel);
    }
    m_toolBarBean = null;
    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    m_beanLayout.repaint();
    m_pointerB.setSelected(true);
    m_mode = NONE;
  }

  /**
   * Accept property change events
   *
   * @param e a <code>PropertyChangeEvent</code> value
   */
  public void propertyChange(PropertyChangeEvent e) {
    revalidate();
    m_beanLayout.repaint();
  }
  
  /**
   * Load a pre-saved layout
   */
  private void loadLayout() {
    m_loadB.setEnabled(false);
    m_saveB.setEnabled(false);
    int returnVal = m_FileChooser.showOpenDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      try {
	File oFile = m_FileChooser.getSelectedFile();
	InputStream is = new FileInputStream(oFile);
	ObjectInputStream ois = new ObjectInputStream(is);
	Vector beans = (Vector) ois.readObject();
	Vector connections = (Vector) ois.readObject();
	ois.close();

	// register this panel as a property change listener with each
	// bean
	for (int i = 0; i < beans.size(); i++) {
	  BeanInstance tempB = (BeanInstance)beans.elementAt(i);
	  if (tempB.getBean() instanceof Visible) {
	    ((Visible)(tempB.getBean())).getVisual().
	      addPropertyChangeListener(this);
	  }
	  if (tempB.getBean() instanceof BeanCommon) {
	    ((BeanCommon)(tempB.getBean())).setLog(m_logPanel);
	  }
	}
	BeanInstance.setBeanInstances(beans, m_beanLayout);
	BeanConnection.setConnections(connections);
	m_beanLayout.revalidate();
	m_beanLayout.repaint();
      } catch (Exception ex) {
	ex.printStackTrace();
      }
    }
    m_loadB.setEnabled(true);
    m_saveB.setEnabled(true);
  }


  /**
   * Serialize the layout to a file
   */
  private void saveLayout() {
    m_loadB.setEnabled(false);
    m_saveB.setEnabled(false);
    int returnVal = m_FileChooser.showSaveDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      // temporarily remove this panel as a property changle listener from
      // each bean
      Vector beans = BeanInstance.getBeanInstances();
      for (int i = 0; i < beans.size(); i++) {
	BeanInstance tempB = (BeanInstance)beans.elementAt(i);
	if (tempB.getBean() instanceof Visible) {
	  ((Visible)(tempB.getBean())).getVisual().
	    removePropertyChangeListener(this);
	}
      }
      // now serialize components vector and connections vector
      try {
	File sFile = m_FileChooser.getSelectedFile();
	OutputStream os = new FileOutputStream(sFile);
	ObjectOutputStream oos = new ObjectOutputStream(os);
	oos.writeObject(beans);
	oos.writeObject(BeanConnection.getConnections());
	oos.flush();
	oos.close();
      } catch (Exception ex) {
	ex.printStackTrace();
      } finally {
	// restore this panel as a property change listener in the beans
	for (int i = 0; i < beans.size(); i++) {
	  BeanInstance tempB = (BeanInstance)beans.elementAt(i);
	  if (tempB.getBean() instanceof Visible) {
	    ((Visible)(tempB.getBean())).getVisual().
	      addPropertyChangeListener(this);
	  }
	}
      }
    }
    m_saveB.setEnabled(true);
    m_loadB.setEnabled(true);
  }

  /**
   * Main method.
   *
   * @param args a <code>String[]</code> value
   */
  public static void main(String [] args) {
    try {
      final javax.swing.JFrame jf = new javax.swing.JFrame();
      jf.getContentPane().setLayout(new java.awt.BorderLayout());
      final KnowledgeFlow tm = new KnowledgeFlow();

      jf.getContentPane().add(tm, java.awt.BorderLayout.CENTER);
      jf.addWindowListener(new java.awt.event.WindowAdapter() {
        public void windowClosing(java.awt.event.WindowEvent e) {
          jf.dispose();
          System.exit(0);
        }
      });
      jf.setSize(800,600);
      jf.setVisible(true);
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
