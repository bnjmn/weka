package weka.gui;

import weka.experiment.PropertyNode;

import java.beans.PropertyDescriptor;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Frame;
import java.awt.FlowLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
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
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JTree;
import javax.swing.JDialog;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;


public class PropertySelectorDialog extends JDialog {
  
  protected JButton m_SelectBut = new JButton("Select");
  protected JButton m_CancelBut = new JButton("Cancel");
  protected DefaultMutableTreeNode m_Root;
  protected Object m_RootObject;
  protected int m_Result;
  protected Object [] m_ResultPath;
  protected JTree m_Tree;
  
  public static final int APPROVE_OPTION = 0;
  public static final int CANCEL_OPTION = 1;
  
  public PropertySelectorDialog(Frame parentFrame, Object rootObject) {
    
    super(parentFrame, "Select a property", true);
    m_CancelBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	m_Result = CANCEL_OPTION;
	setVisible(false);
      }
    });
    m_SelectBut.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	// value = path from root to selected;
	TreePath tPath = m_Tree.getSelectionPath();
	if (tPath == null) {
	  m_Result = CANCEL_OPTION;
	} else {
	  m_ResultPath = tPath.getPath();
	  if ((m_ResultPath == null) || (m_ResultPath.length < 2)) {
	    m_Result = CANCEL_OPTION;
	  } else {
	    m_Result = APPROVE_OPTION;
	  }
	} 
	setVisible(false);
      }
    });
    m_RootObject = rootObject;
    m_Root = new DefaultMutableTreeNode(
	     new PropertyNode(m_RootObject));
    createNodes(m_Root);
    
    Container c = getContentPane();
    c.setLayout(new BorderLayout());
    //    setBorder(BorderFactory.createTitledBorder("Select a property"));
    Box b1 = new Box(BoxLayout.X_AXIS);
    b1.add(m_SelectBut);
    b1.add(Box.createHorizontalStrut(10));
    b1.add(m_CancelBut);
    c.add(b1, BorderLayout.SOUTH);
    m_Tree = new JTree(m_Root);
    m_Tree.getSelectionModel()
      .setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    c.add(new JScrollPane(m_Tree), BorderLayout.CENTER);
    pack();
  }

  public int showDialog() {

    m_Result = CANCEL_OPTION;
    setVisible(true);
    return m_Result;
  }

  public PropertyNode [] getPath() {

    PropertyNode [] result = new PropertyNode [m_ResultPath.length - 1];
    for (int i = 0; i < result.length; i++) {
      result[i] = (PropertyNode) ((DefaultMutableTreeNode) m_ResultPath[i + 1])
	.getUserObject();
    }
    return result;
  }

  protected void createNodes(DefaultMutableTreeNode localNode) {

    PropertyNode pNode = (PropertyNode)localNode.getUserObject();
    Object localObject = pNode.value;
    // Find all the properties of the object in the root node
    PropertyDescriptor localProperties[];
    try {
      BeanInfo bi = Introspector.getBeanInfo(localObject.getClass());
      localProperties = bi.getPropertyDescriptors();
    } catch (IntrospectionException ex) {
      System.err.println("PropertySheet: Couldn't introspect");
      return;
    }

    // Put their values into child nodes.
    for (int i = 0; i < localProperties.length; i++) {
      // Don't display hidden or expert properties.
      if (localProperties[i].isHidden() || localProperties[i].isExpert()) {
	continue;
      }
      String name = localProperties[i].getDisplayName();
      Class type = localProperties[i].getPropertyType();
      Method getter = localProperties[i].getReadMethod();
      Method setter = localProperties[i].getWriteMethod();
      Object value = null;
      // Only display read/write properties.
      if (getter == null || setter == null) {
	continue;
      }
      try {
	Object args[] = { };
	value = getter.invoke(localObject, args);
	PropertyEditor editor = null;
	Class pec = localProperties[i].getPropertyEditorClass();
	if (pec != null) {
	  try {
	    editor = (PropertyEditor)pec.newInstance();
	  } catch (Exception ex) {
	  }
	}
	if (editor == null) {
	  editor = PropertyEditorManager.findEditor(type);
	}
	if ((editor == null) || (value == null)) {
	  continue;
	}
      } catch (InvocationTargetException ex) {
	System.err.println("Skipping property " + name
			   + " ; exception on target: "
			   + ex.getTargetException());
	ex.getTargetException().printStackTrace();
	continue;
      } catch (Exception ex) {
	System.err.println("Skipping property " + name
			   + " ; exception: " + ex);
	ex.printStackTrace();
	continue;
      }
      // Make a child node
      DefaultMutableTreeNode child = new DefaultMutableTreeNode(
				     new PropertyNode(value,
						      localProperties[i],
						      localObject.getClass()));
      localNode.add(child);
      createNodes(child);
    }
  }

  
  /**
   * Tests out the classifier editor from the command line.
   *
   * @param args may contain the class name of a classifier to edit
   */
  public static void main(String [] args) {

    try {
      System.err.println("---Registering Weka Editors---");
      java.beans.PropertyEditorManager
	.registerEditor(weka.experiment.ResultProducer.class,
			weka.gui.GenericObjectEditor.class);
      java.beans.PropertyEditorManager
	.registerEditor(weka.experiment.SplitEvaluator.class,
			weka.gui.GenericObjectEditor.class);
      java.beans.PropertyEditorManager
	.registerEditor(weka.classifiers.Classifier.class,
			weka.gui.GenericObjectEditor.class);
      java.beans.PropertyEditorManager
	.registerEditor(weka.core.SelectedTag.class,
			weka.gui.SelectedTagEditor.class);
      Object rp
	= new weka.experiment.AveragingResultProducer();
      final PropertySelectorDialog jd = new PropertySelectorDialog(null, rp);
      int result = jd.showDialog();
      if (result == PropertySelectorDialog.APPROVE_OPTION) {
	System.err.println("Property Selected");
	PropertyNode [] path = jd.getPath();
	for (int i = 0; i < path.length; i++) {
	  PropertyNode pn = path[i];
	  System.err.println("" + (i + 1) + "  " + pn.toString()
			     + " " + pn.value.toString());
	}
      } else {
	System.err.println("Cancelled");
      }
      System.exit(0);
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
