package weka.gui;

import weka.core.SelectedTag;
import weka.classifiers.Classifier;

import java.lang.reflect.Array;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Font;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.FontMetrics;
import java.awt.FlowLayout;
import java.awt.event.ItemListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.ListCellRenderer;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.DefaultListModel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

public class GenericArrayEditor extends JPanel
  implements PropertyEditor {

  /** Handles property change notification */
  private PropertyChangeSupport m_Support = new PropertyChangeSupport(this);

  /** The label for when we can't edit that type */
  private JLabel m_Label = new JLabel("Can't edit", SwingConstants.CENTER);
  
  /** The list component displaying current values */
  private JList m_ElementList = new JList();

  /** The class of objects allowed in the array */
  private Class m_ElementClass = String.class;

  /** The defaultlistmodel holding our data */
  private DefaultListModel m_ListModel;

  /** The property editor for the class we are editing */
  private PropertyEditor m_ElementEditor;
  
  private JButton m_DeleteBut = new JButton("Delete");

  private JButton m_AddBut = new JButton("Add");
  
  private ActionListener m_InnerActionListener =
    new ActionListener() {

    public void actionPerformed(ActionEvent e) {

      if (e.getSource() == m_DeleteBut) {
	int selected = m_ElementList.getSelectedIndex();
	if (selected != -1) {
	  m_ListModel.removeElementAt(selected);
	  if (m_ListModel.size() > selected) {
	    m_ElementList.setSelectedIndex(selected);
	  }
	  m_Support.firePropertyChange("", null, null);
	}
	if (m_ElementList.getSelectedIndex() == -1) {
	  m_DeleteBut.setEnabled(false);
	}
      } else if (e.getSource() == m_AddBut) {
	int selected = m_ElementList.getSelectedIndex();
	if (selected != -1) {
	  m_ListModel.insertElementAt(m_ElementEditor.getValue(), selected);
	} else {
	  m_ListModel.addElement(m_ElementEditor.getValue());
	}
	m_Support.firePropertyChange("", null, null);
      } 
    }
  };

  private ListSelectionListener m_InnerSelectionListener =
    new ListSelectionListener() {

      public void valueChanged(ListSelectionEvent e) {

	if (e.getSource() == m_ElementList) {
	  // Enable the delete button
	  if (m_ElementList.getSelectedIndex() != -1) {
	    m_DeleteBut.setEnabled(true);
	  }
	}
      }
  };
    

  /**
   * Sets up the array editor.
   */
  public GenericArrayEditor() {

    setLayout(new BorderLayout());
    add(m_Label, BorderLayout.CENTER);
    m_DeleteBut.addActionListener(m_InnerActionListener);
    m_AddBut.addActionListener(m_InnerActionListener);
    m_ElementList.addListSelectionListener(m_InnerSelectionListener);
  }

  private class EditorListCellRenderer
    implements ListCellRenderer {
    
    private Class m_EditorClass;
    private Class m_ValueClass;
    public EditorListCellRenderer(Class editorClass, Class valueClass) {
      m_EditorClass = editorClass;
      m_ValueClass = valueClass;
    }

    public Component getListCellRendererComponent(final JList list,
						  final Object value,
						  final int index,
						  final boolean isSelected,
						  final boolean cellHasFocus) {
      try {
	final PropertyEditor e = (PropertyEditor)m_EditorClass.newInstance();
	if (e instanceof GenericObjectEditor) {
	  //	  ((GenericObjectEditor) e).setDisplayOnly(true);
	  ((GenericObjectEditor) e).setClassType(m_ValueClass);
	}
	e.setValue(value);
	return new JPanel() {

	  public void paintComponent(Graphics g) {

	    Insets i = getInsets();
	    Rectangle box = new Rectangle(i.left, i.top,
					  getWidth() - i.right,
					  getHeight() - i.bottom );
	    g.setColor(isSelected
		       ? list.getSelectionBackground()
		       : list.getBackground());
	    g.fillRect(0, 0, getWidth(), getHeight());
	    g.setColor(isSelected
		       ? list.getSelectionForeground()
		       : list.getForeground());
	    e.paintValue(g, box);
	  }
	  
	  public Dimension getPreferredSize() {

	    Font f = getFont();
	    FontMetrics fm = getFontMetrics(f);
	    return new Dimension(0, fm.getHeight());
	  }
	};
      } catch (Exception ex) {
	return null;
      }
    }
  }

  private void updateEditorType(Object o) {

    // Determine if the current object is an array
    m_ElementEditor = null;
    removeAll();
    if ((o != null) && (o.getClass().isArray())) {
      Class elementClass = o.getClass().getComponentType();    
      PropertyEditor editor = PropertyEditorManager.findEditor(elementClass);
      Component view = null;
      ListCellRenderer lcr = new DefaultListCellRenderer();
      if (editor != null) {
	if (editor instanceof GenericObjectEditor) {
	  ((GenericObjectEditor) editor).setClassType(elementClass);
	}
	if (editor.isPaintable() && editor.supportsCustomEditor()) {
	  view = new PropertyPanel(editor);
	  lcr = new EditorListCellRenderer(editor.getClass(), elementClass);
	} else if (editor.getTags() != null) {
	  view = new PropertyValueSelector(editor);
	} else if (editor.getAsText() != null) {
	  view = new PropertyText(editor);
	}
      }
      if (view == null) {
	System.err.println("No property editor for class: "
			   + elementClass.getName());
      } else {
	m_ElementEditor = editor;

	// Create the ListModel and populate it
	m_ListModel = new DefaultListModel();
	m_ElementClass = elementClass;
	for (int i = 0; i < Array.getLength(o); i++) {
	  m_ListModel.addElement(Array.get(o,i));
	}
	m_ElementList.setCellRenderer(lcr);
	m_ElementList.setModel(m_ListModel);
	if (m_ListModel.getSize() > 0) {
	  m_ElementList.setSelectedIndex(0);
	  m_DeleteBut.setEnabled(true);
	} else {
	  m_DeleteBut.setEnabled(false);
	}

	try {
	  if (m_ListModel.getSize() > 0) {
	    m_ElementEditor.setValue(m_ListModel.getElementAt(0));
	  } else {
	    m_ElementEditor.setValue(m_ElementClass.newInstance());
	  }
	  	  
	  JPanel panel = new JPanel();
	  panel.setLayout(new BorderLayout());
	  panel.add(view, BorderLayout.CENTER);
	  panel.add(m_AddBut, BorderLayout.EAST);
	  add(panel, BorderLayout.NORTH);
	  add(new JScrollPane(m_ElementList), BorderLayout.CENTER);
	  add(m_DeleteBut, BorderLayout.SOUTH);
	  m_ElementEditor
	    .addPropertyChangeListener(new PropertyChangeListener() {
	    public void propertyChange(PropertyChangeEvent e) {
	      repaint();
	    }
	  });
	} catch (Exception ex) {
	}
      }
    }
    if (m_ElementEditor == null) {
      add(m_Label, BorderLayout.CENTER);
    }
    m_Support.firePropertyChange("", null, null);
    validate();
  }

  /**
   * Sets the current object array.
   *
   * @param o an object that must be an array.
   */
  public void setValue(Object o) {

    // Create a new list model, put it in the list and resize?
    updateEditorType(o);
  }

  /**
   * Gets the current object array.
   *
   * @return the current object array
   */
  public Object getValue() {

    if (m_ListModel == null) {
      return null;
    }
    // Convert the listmodel to an array of strings and return it.
    int length = m_ListModel.getSize();
    Object result = Array.newInstance(m_ElementClass, length);
    for (int i = 0; i < length; i++) {
      Array.set(result, i, m_ListModel.elementAt(i));
    }
    return result;
  }
  
  /**
   * Supposedly returns an initialization string to create a classifier
   * identical to the current one, including it's state, but this doesn't
   * appear possible given that the initialization string isn't supposed to
   * contain multiple statements.
   *
   * @return the java source code initialisation string
   */
  public String getJavaInitializationString() {

    return "null";
  }

  /**
   * Returns true to indicate that we can paint a representation of the
   * string array
   *
   * @return true
   */
  public boolean isPaintable() {
    return true;
  }

  /**
   * Paints a representation of the current classifier.
   *
   * @param gfx the graphics context to use
   * @param box the area we are allowed to paint into
   */
  public void paintValue(java.awt.Graphics gfx, java.awt.Rectangle box) {

    FontMetrics fm = gfx.getFontMetrics();
    int vpad = (box.height - fm.getAscent()) /* /2 */;
    String rep = m_ListModel.getSize() + " " + m_ElementClass.getName();
    gfx.drawString(rep, 2, box.height - vpad);
  }

  /* We don't support get/set as text methods */
  public String getAsText() {
    return null;
  }
  public void setAsText(String text) throws IllegalArgumentException {
    throw new IllegalArgumentException(text);
  }
  /* We don't support setting from tags either */
  public String[] getTags() {
    return null;
  }

  /**
   * Returns true because we do support a custom editor.
   *
   * @return true
   */
  public boolean supportsCustomEditor() {
    return true;
  }
  public java.awt.Component getCustomEditor() {
    return this;
  }

  // For propertyChange "Producing"
  public void addPropertyChangeListener(PropertyChangeListener l) {
    m_Support.addPropertyChangeListener(l);
  }
  public void removePropertyChangeListener(PropertyChangeListener l) {
    m_Support.removePropertyChangeListener(l);
  }

  /**
   * Tests out the classifier editor from the command line.
   *
   * @param args may contain the class name of a classifier to edit
   */
  public static void main(String [] args) {

    try {
      System.err.println("---Registering Weka Editors---");
      java.beans.PropertyEditorManager.registerEditor(Classifier.class,
					GenericObjectEditor.class);
      java.beans.PropertyEditorManager.registerEditor(SelectedTag.class,
					SelectedTagEditor.class);
      java.beans.PropertyEditorManager.registerEditor(String [].class,
					GenericArrayEditor.class);
      final GenericArrayEditor ce = new GenericArrayEditor();

      final Classifier [] initial = {
	new weka.classifiers.ZeroR(),
	new weka.classifiers.OneR(),
	new weka.classifiers.ZeroR()
	};
      /*
      final String [] initial = {
	"Hello",
	"There",
	"Bob"
	};*/
      PropertyDialog pd = new PropertyDialog(ce, 100, 100);
      pd.setSize(200,200);
      pd.addWindowListener(new WindowAdapter() {
	public void windowClosing(WindowEvent e) {
	  System.exit(0);
	}
      });
      ce.setValue(initial);
      //ce.validate();
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }

}

