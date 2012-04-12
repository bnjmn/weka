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
 * OptionTree.java
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 */

package wekaexamples.gui;

import weka.core.Utils;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 * Displays commandline options as tree.
 * 
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class OptionTree
  extends JPanel {
  
  /** for serialization. */
  private static final long serialVersionUID = 8177217505366217714L;

  /** the label of the root node. */
  public final static String LABEL_ROOT = "<root>";

  /** the label of the nested node. */
  public final static String LABEL_NESTED = "<nested>";
  
  /** the text field for pasting the complete option string. */
  protected JTextField m_TextOptionsFull;
  
  /** the button to update the tree. */
  protected JButton m_ButtonUpdate;
  
  /** the text field for options represented by the currently selected sub-tree. */
  protected JTextField m_TextOptionsSelected;
  
  /** the button to copy the partial options to clipboard. */
  protected JButton m_ButtonCopy;
  
  /** the tree for displaying the options hierarchy. */
  protected JTree m_TreeOptions;

  /**
   * Initializes the panel.
   */
  public OptionTree() {
    super();
    initGUI();
  }
  
  /**
   * For initializing the GUI.
   */
  protected void initGUI() {
    JPanel	panel;
    JLabel	label;
    
    setLayout(new BorderLayout());
    
    // full options
    panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    add(panel, BorderLayout.NORTH);
    
    
    m_TextOptionsFull = new JTextField(50);
    m_TextOptionsFull.getDocument().addDocumentListener(new DocumentListener() {
      public void changedUpdate(DocumentEvent e) {
	update();
      }
      public void insertUpdate(DocumentEvent e) {
	update();
      }
      public void removeUpdate(DocumentEvent e) {
	update();
      }
    });
    label = new JLabel("Full options");
    label.setDisplayedMnemonic('F');
    label.setLabelFor(m_TextOptionsFull);
    panel.add(label);
    panel.add(m_TextOptionsFull);
    
    m_ButtonUpdate = new JButton("Update");
    m_ButtonUpdate.setMnemonic('U');
    m_ButtonUpdate.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	updateTree();
      }
    });
    panel.add(m_ButtonUpdate);
    
    // table
    m_TreeOptions = new JTree();
    m_TreeOptions.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent e) {
	generatePartialOptions();
      }
    });
    add(new JScrollPane(m_TreeOptions));
    
    // partial options
    panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    add(panel, BorderLayout.SOUTH);

    m_TextOptionsSelected = new JTextField(50);
    m_TextOptionsSelected.getDocument().addDocumentListener(new DocumentListener() {
      public void changedUpdate(DocumentEvent e) {
	update();
      }
      public void insertUpdate(DocumentEvent e) {
	update();
      }
      public void removeUpdate(DocumentEvent e) {
	update();
      }
    });
    label = new JLabel("Selected options");
    label.setDisplayedMnemonic('S');
    label.setLabelFor(m_TextOptionsSelected);
    panel.add(label);
    panel.add(m_TextOptionsSelected);
    
    m_ButtonCopy = new JButton("Copy");
    m_ButtonCopy.setMnemonic('C');
    m_ButtonCopy.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        StringSelection selection = new StringSelection(m_TextOptionsSelected.getText());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
      }
    });
    panel.add(m_ButtonCopy);
    
    update();
    updateTree();
  }

  /**
   * Updates the GUI.
   */
  protected void update() {
    m_ButtonCopy.setEnabled(m_TextOptionsSelected.getText().length() > 0);
  }
  
  /**
   * Parses the given option and adds the generated parts to the parent.
   * If there are nested options, then the parsing follows depth-first.
   * 
   * @param parent	the parent node to add the children to
   * @param s		the string to parse
   */
  protected void parse(DefaultMutableTreeNode parent, String s) {
    String[]			options;
    String			currOption;
    int				i;
    DefaultMutableTreeNode	node;
    
    try {
      options = Utils.splitOptions(s);
      for (i = 0; i < options.length; i++) {
	currOption = options[i].trim();
	if (currOption.indexOf(" ") > -1)
	  node = new DefaultMutableTreeNode(LABEL_NESTED);
	else
	  node = new DefaultMutableTreeNode(currOption);
	parent.add(node);
	if (currOption.indexOf(" ") > -1)
	  parse(node, currOption);
      }
    }
    catch (Exception e) {
      node = new DefaultMutableTreeNode(e.toString());
      parent.add(node);
      e.printStackTrace();
    }
  }
  
  /**
   * Updates the tree with the current full options.
   */
  protected void updateTree() {
    DefaultMutableTreeNode	root;
    
    root = new DefaultMutableTreeNode(LABEL_ROOT);
    
    if (m_TextOptionsFull.getText().length() > 0)
      parse(root, m_TextOptionsFull.getText());

    m_TreeOptions.setModel(new DefaultTreeModel(root));
  }
  
  /**
   * Returns the options for this node and its subtree.
   * 
   * @param node	the node to get the options for
   * @return		the generated options
   */
  protected String getOptions(DefaultMutableTreeNode node) {
    Vector<String>	options;
    int			i;
    
    options = new Vector<String>();
    
    // the node itself
    if (!node.toString().equals(LABEL_ROOT) && !node.toString().equals(LABEL_NESTED))
      options.add(node.toString());
    
    // the node's children
    for (i = 0; i < node.getChildCount(); i++)
      options.add(getOptions((DefaultMutableTreeNode) node.getChildAt(i)));
    
    return Utils.joinOptions(options.toArray(new String[options.size()]));
  }
  
  /**
   * Generates the options based on the currently selected node and the
   * corresponding subtree.
   */
  protected void generatePartialOptions() {
    DefaultMutableTreeNode	node;
    
    node = (DefaultMutableTreeNode) m_TreeOptions.getSelectionPath().getLastPathComponent();
    m_TextOptionsSelected.setText(getOptions(node));
  }

  /**
   * Sets the text and re-generates the tree.
   *
   * @param s		the options to display
   */
  public void setOptions(String s) {
    m_TextOptionsFull.setText(s);
    m_ButtonUpdate.doClick();
  }

  /**
   * Displays a frame with the option tree panel.
   *
   * @param args	displayed in the full options text
   */
  public static void main(String[] args) {
    OptionTree tree = new OptionTree();
    final JFrame jf = new JFrame("Option Tree");
    jf.getContentPane().setLayout(new BorderLayout());
    jf.getContentPane().add(tree, BorderLayout.CENTER);
    jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    jf.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        jf.dispose();
      }
    });
    jf.pack();
    jf.setSize(800, 600);
    jf.setLocationRelativeTo(null);
    jf.setVisible(true);

    if (args.length > 0)
      tree.setOptions(Utils.joinOptions(args));
  }
}
