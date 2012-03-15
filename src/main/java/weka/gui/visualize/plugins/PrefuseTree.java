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
 * PrefuseTree.java
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 * Copyright (C) Jeffrey Heer (original prefuse demo)
 */

package weka.gui.visualize.plugins;

import prefuse.Constants;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.Action;
import prefuse.action.ActionList;
import prefuse.action.ItemAction;
import prefuse.action.RepaintAction;
import prefuse.action.animate.ColorAnimator;
import prefuse.action.animate.LocationAnimator;
import prefuse.action.animate.QualityControlAnimator;
import prefuse.action.animate.VisibilityAnimator;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.FontAction;
import prefuse.action.filter.FisheyeTreeFilter;
import prefuse.action.layout.CollapsedSubtreeLayout;
import prefuse.action.layout.graph.NodeLinkTreeLayout;
import prefuse.activity.SlowInSlowOutPacer;
import prefuse.controls.FocusControl;
import prefuse.controls.PanControl;
import prefuse.controls.WheelZoomControl;
import prefuse.controls.ZoomControl;
import prefuse.controls.ZoomToFitControl;
import prefuse.data.Tree;
import prefuse.data.Tuple;
import prefuse.data.event.TupleSetListener;
import prefuse.data.io.TreeMLReader;
import prefuse.data.search.PrefixSearchTupleSet;
import prefuse.data.tuple.TupleSet;
import prefuse.render.AbstractShapeRenderer;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.EdgeRenderer;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.util.FontLib;
import prefuse.visual.VisualItem;
import prefuse.visual.expression.InGroupPredicate;
import prefuse.visual.sort.TreeDepthItemSorter;

import weka.gui.treevisualizer.Edge;
import weka.gui.treevisualizer.Node;
import weka.gui.treevisualizer.TreeBuild;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

/**
 * Displays a tree in <a href="http://www.graphviz.org/doc/info/lang.html" target="_blank">GraphViz's dotty</a> 
 * format as <a href="http://prefuse.org/" target="_blank">Prefuse</a> tree.
 * <p/>
 * See also the <a href="http://www.nomencurator.org/InfoVis2003/download/treeml.dtd" target="_blank">treeml.dtd</a>.
 * <p/>
 * Based on the <code>prefuse.demos.TreeView</code> demo class.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a> (original prefuse demo)
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 * @see     prefuse.demos.TreeView
 */
public class PrefuseTree
  implements Serializable, TreeVisualizePlugin {

  /** for serialization. */
  private static final long serialVersionUID = 7485599985684890717L;

  /**
   * Turns the <a href="http://www.graphviz.org/doc/info/lang.html" target="_blank">GraphViz dotty</a> 
   * format into Prefuse's tree XML format (according to the tree.dtd).
   * 
   * @author  fracpete (fracpete at waikato dot ac dot nz)
   * @version $Revision$
   */
  public static class DottyToTree {
    
    /**
     * Replaces certain characters with their character entities.
     * 
     * @param s		the string to process
     * @return		the processed string
     */
    protected String sanitize(String s) {
      String	result;
      
      result = s;
      result = result.replaceAll("&", "&amp;")
                     .replaceAll("\"", "&quot;")
                     .replaceAll("'", "&apos;")
                     .replaceAll("<", "&lt;")
                     .replaceAll(">", "&gt;");
      // in addition, replace some other entities as well
      result = result.replaceAll("\n", "&#10;")
                     .replaceAll("\r", "&#13;")
                     .replaceAll("\t", "&#9;");
      
      return result;
    }
    
    /**
     * Writes the header of the GraphML file.
     * 
     * @param writer		the writer to use
     * @throws Exception	if an error occurs
     */
    protected void writeHeader(BufferedWriter writer) throws Exception {
      writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); writer.newLine();
      writer.newLine();
      writer.write("<!-- This file was generated by Weka (http://www.cs.waikato.ac.nz/ml/weka/). -->"); writer.newLine();
      writer.newLine();
      writer.write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\" \"http://www.nomencurator.org/InfoVis2003/download/treeml.dtd\">"); 
      writer.newLine();
      writer.write("<tree>"); writer.newLine();
      writer.write("<declarations>"); writer.newLine();
      writer.write("<attributeDecl name=\"name\" type=\"String\"/>"); writer.newLine();
      writer.write("</declarations>"); writer.newLine();
    }
    
    /**
     * Writes the node as GraphML.
     * 
     * @param writer		the writer to use
     * @param node		the node to write as GraphML
     * @throws Exception	if an error occurs
     */
    protected void writeNode(BufferedWriter writer, Node node) throws Exception {
      int	i;
      String	tag;
      boolean	leaf;
      
      // leaf?
      leaf = (node.getChild(0) == null);
      if (leaf)
	tag = "leaf";
      else
	tag = "branch";
      
      // the node itself
      writer.write("<" + tag + ">"); writer.newLine();
      writer.write("<attribute name=\"name\" value=\"" + sanitize(node.getLabel()) + "\"/>"); writer.newLine();
      
      // the node's children, if any
      if (!leaf) {
	for (i = 0; (node.getChild(i) != null); i++)
	  writeEdge(writer, node.getChild(i));
      }

      writer.write("</" + tag + ">"); writer.newLine();
    }
    
    /**
     * Writes the edge as GraphML. Since prefuse doesn't seem to offer edge 
     * labels, the edges get inserted as nodes as well.
     * 
     * @param writer		the writer to use
     * @param edge		the edge to write
     * @throws Exception	if an error occurs
     */
    protected void writeEdge(BufferedWriter writer, Edge edge) throws Exception {
      if (edge.getLabel().length() > 0) {
	writer.write("<branch>"); writer.newLine();
	writer.write("<attribute name=\"name\" value=\"" + sanitize(edge.getLabel()) + "\"/>"); writer.newLine();
	writeNode(writer, edge.getTarget());
	writer.write("</branch>"); writer.newLine();
      }
      else {
	writeNode(writer, edge.getTarget());
      }
    }
    
    /**
     * Writes the footer of the GraphML file.
     * 
     * @param writer		the writer to use
     * @throws Exception	if an error occurs
     */
    protected void writeFooter(BufferedWriter writer) throws Exception {
      writer.write("</tree>"); writer.newLine();
    }
    
    /**
     * Parses the incoming data and writes the generated output.
     * 
     * @param dotty		the graph in dotty format
     * @return			the TreeML data
     * @throws Exception	if parsing or writing fails
     */
    public String convert(String dotty) throws Exception {
      Node		root;
      TreeBuild		tree;
      StringWriter	output;
      BufferedWriter	writer;
      
      // parse dotty format
      tree = new TreeBuild();
      root = tree.create(new StringReader(dotty));
      
      // generate GraphML output
      output = new StringWriter();
      writer = new BufferedWriter(output);
      writeHeader(writer);
      writeNode(writer, root);
      writeFooter(writer);
      writer.flush();
      
      return output.toString();
    }
  }

  /**
   * Displays a tree.
   * <p/>
   * Based on the <code>prefuse.demos.TreeView</code> demo class.
   * 
   * @author  fracpete (fracpete at waikato dot ac dot nz)
   * @version $Revision$
   * @see     prefuse.demos.TreeView
   */
  public static class TreePanel
    extends Display {
    
    /** for serialization. */
    private static final long serialVersionUID = 8262123080545898882L;

    public class OrientAction extends AbstractAction {
      private int orientation;

      public OrientAction(int orientation) {
	this.orientation = orientation;
      }
      public void actionPerformed(ActionEvent evt) {
	setOrientation(orientation);
	getVisualization().cancel("orient");
	getVisualization().run("treeLayout");
	getVisualization().run("orient");
      }
    }

    public class AutoPanAction extends Action {
      private Point2D m_start = new Point2D.Double();
      private Point2D m_end   = new Point2D.Double();
      private Point2D m_cur   = new Point2D.Double();
      private int     m_bias  = 150;

      public void run(double frac) {
	TupleSet ts = m_vis.getFocusGroup(Visualization.FOCUS_ITEMS);
	if ( ts.getTupleCount() == 0 )
	  return;

	if ( frac == 0.0 ) {
	  int xbias=0, ybias=0;
	  switch ( m_orientation ) {
	    case Constants.ORIENT_LEFT_RIGHT:
	      xbias = m_bias;
	      break;
	    case Constants.ORIENT_RIGHT_LEFT:
	      xbias = -m_bias;
	      break;
	    case Constants.ORIENT_TOP_BOTTOM:
	      ybias = m_bias;
	      break;
	    case Constants.ORIENT_BOTTOM_TOP:
	      ybias = -m_bias;
	      break;
	  }

	  VisualItem vi = (VisualItem)ts.tuples().next();
	  m_cur.setLocation(getWidth()/2, getHeight()/2);
	  getAbsoluteCoordinate(m_cur, m_start);
	  m_end.setLocation(vi.getX()+xbias, vi.getY()+ybias);
	} else {
	  m_cur.setLocation(m_start.getX() + frac*(m_end.getX()-m_start.getX()),
	      m_start.getY() + frac*(m_end.getY()-m_start.getY()));
	  panToAbs(m_cur);
	}
      }
    }

    public static class NodeColorAction extends ColorAction {

      public NodeColorAction(String group) {
	super(group, VisualItem.FILLCOLOR);
      }

      public int getColor(VisualItem item) {
	if ( m_vis.isInGroup(item, Visualization.SEARCH_ITEMS) )
	  return ColorLib.rgb(255,190,190);
	else if ( m_vis.isInGroup(item, Visualization.FOCUS_ITEMS) )
	  return ColorLib.rgb(198,229,229);
	else if ( item.getDOI() > -1 )
	  return ColorLib.rgb(164,193,193);
	else
	  return ColorLib.rgba(255,255,255,0);
      }

    } // end of inner class TreeMapColorAction
    
    private static final String tree = "tree";
    private static final String treeNodes = "tree.nodes";
    private static final String treeEdges = "tree.edges";
    
    private LabelRenderer m_nodeRenderer;
    private EdgeRenderer m_edgeRenderer;
    
    private String m_label = "name";
    private int m_orientation = Constants.ORIENT_LEFT_RIGHT;

    /**
     * Initializes the panel.
     * 
     * @param t		the tree to visualize
     */
    public TreePanel(Tree t) {
      super(new Visualization());

      m_vis.add(tree, t);

      m_nodeRenderer = new LabelRenderer(m_label);
      m_nodeRenderer.setRenderType(AbstractShapeRenderer.RENDER_TYPE_FILL);
      m_nodeRenderer.setHorizontalAlignment(Constants.LEFT);
      m_nodeRenderer.setRoundedCorner(8,8);
      m_edgeRenderer = new EdgeRenderer(Constants.EDGE_TYPE_CURVE);

      DefaultRendererFactory rf = new DefaultRendererFactory(m_nodeRenderer);
      rf.add(new InGroupPredicate(treeEdges), m_edgeRenderer);
      m_vis.setRendererFactory(rf);

      // colors
      ItemAction nodeColor = new NodeColorAction(treeNodes);
      ItemAction textColor = new ColorAction(treeNodes,
	  VisualItem.TEXTCOLOR, ColorLib.rgb(0,0,0));
      m_vis.putAction("textColor", textColor);

      ItemAction edgeColor = new ColorAction(treeEdges,
	  VisualItem.STROKECOLOR, ColorLib.rgb(200,200,200));

      // quick repaint
      ActionList repaint = new ActionList();
      repaint.add(nodeColor);
      repaint.add(new RepaintAction());
      m_vis.putAction("repaint", repaint);

      // full paint
      ActionList fullPaint = new ActionList();
      fullPaint.add(nodeColor);
      m_vis.putAction("fullPaint", fullPaint);

      // animate paint change
      ActionList animatePaint = new ActionList(400);
      animatePaint.add(new ColorAnimator(treeNodes));
      animatePaint.add(new RepaintAction());
      m_vis.putAction("animatePaint", animatePaint);

      // create the tree layout action
      NodeLinkTreeLayout treeLayout = new NodeLinkTreeLayout(tree,
	  m_orientation, 50, 0, 8);
      treeLayout.setLayoutAnchor(new Point2D.Double(25,300));
      m_vis.putAction("treeLayout", treeLayout);

      CollapsedSubtreeLayout subLayout = 
	new CollapsedSubtreeLayout(tree, m_orientation);
      m_vis.putAction("subLayout", subLayout);

      AutoPanAction autoPan = new AutoPanAction();

      // create the filtering and layout
      ActionList filter = new ActionList();
      filter.add(new FisheyeTreeFilter(tree, 2));
      filter.add(new FontAction(treeNodes, FontLib.getFont("Tahoma", 16)));
      filter.add(treeLayout);
      filter.add(subLayout);
      filter.add(textColor);
      filter.add(nodeColor);
      filter.add(edgeColor);
      m_vis.putAction("filter", filter);

      // animated transition
      ActionList animate = new ActionList(1000);
      animate.setPacingFunction(new SlowInSlowOutPacer());
      animate.add(autoPan);
      animate.add(new QualityControlAnimator());
      animate.add(new VisibilityAnimator(tree));
      animate.add(new LocationAnimator(treeNodes));
      animate.add(new ColorAnimator(treeNodes));
      animate.add(new RepaintAction());
      m_vis.putAction("animate", animate);
      m_vis.alwaysRunAfter("filter", "animate");

      // create animator for orientation changes
      ActionList orient = new ActionList(2000);
      orient.setPacingFunction(new SlowInSlowOutPacer());
      orient.add(autoPan);
      orient.add(new QualityControlAnimator());
      orient.add(new LocationAnimator(treeNodes));
      orient.add(new RepaintAction());
      m_vis.putAction("orient", orient);

      // ------------------------------------------------

      // initialize the display
      setSize(700,600);
      setItemSorter(new TreeDepthItemSorter());
      addControlListener(new ZoomToFitControl());
      addControlListener(new ZoomControl());
      addControlListener(new WheelZoomControl());
      addControlListener(new PanControl());
      addControlListener(new FocusControl(1, "filter"));

      registerKeyboardAction(
	  new OrientAction(Constants.ORIENT_LEFT_RIGHT),
	  "left-to-right", KeyStroke.getKeyStroke("ctrl 1"), WHEN_FOCUSED);
      registerKeyboardAction(
	  new OrientAction(Constants.ORIENT_TOP_BOTTOM),
	  "top-to-bottom", KeyStroke.getKeyStroke("ctrl 2"), WHEN_FOCUSED);
      registerKeyboardAction(
	  new OrientAction(Constants.ORIENT_RIGHT_LEFT),
	  "right-to-left", KeyStroke.getKeyStroke("ctrl 3"), WHEN_FOCUSED);
      registerKeyboardAction(
	  new OrientAction(Constants.ORIENT_BOTTOM_TOP),
	  "bottom-to-top", KeyStroke.getKeyStroke("ctrl 4"), WHEN_FOCUSED);

      // ------------------------------------------------

      // filter graph and perform layout
      setOrientation(m_orientation);
      m_vis.run("filter");

      TupleSet search = new PrefixSearchTupleSet(); 
      m_vis.addFocusGroup(Visualization.SEARCH_ITEMS, search);
      search.addTupleSetListener(new TupleSetListener() {
	public void tupleSetChanged(TupleSet t, Tuple[] add, Tuple[] rem) {
	  m_vis.cancel("animatePaint");
	  m_vis.run("fullPaint");
	  m_vis.run("animatePaint");
	}
      });
    }

    public void setOrientation(int orientation) {
      NodeLinkTreeLayout rtl 
      = (NodeLinkTreeLayout)m_vis.getAction("treeLayout");
      CollapsedSubtreeLayout stl
      = (CollapsedSubtreeLayout)m_vis.getAction("subLayout");
      switch ( orientation ) {
	case Constants.ORIENT_LEFT_RIGHT:
	  m_nodeRenderer.setHorizontalAlignment(Constants.LEFT);
	  m_edgeRenderer.setHorizontalAlignment1(Constants.RIGHT);
	  m_edgeRenderer.setHorizontalAlignment2(Constants.LEFT);
	  m_edgeRenderer.setVerticalAlignment1(Constants.CENTER);
	  m_edgeRenderer.setVerticalAlignment2(Constants.CENTER);
	  break;
	case Constants.ORIENT_RIGHT_LEFT:
	  m_nodeRenderer.setHorizontalAlignment(Constants.RIGHT);
	  m_edgeRenderer.setHorizontalAlignment1(Constants.LEFT);
	  m_edgeRenderer.setHorizontalAlignment2(Constants.RIGHT);
	  m_edgeRenderer.setVerticalAlignment1(Constants.CENTER);
	  m_edgeRenderer.setVerticalAlignment2(Constants.CENTER);
	  break;
	case Constants.ORIENT_TOP_BOTTOM:
	  m_nodeRenderer.setHorizontalAlignment(Constants.CENTER);
	  m_edgeRenderer.setHorizontalAlignment1(Constants.CENTER);
	  m_edgeRenderer.setHorizontalAlignment2(Constants.CENTER);
	  m_edgeRenderer.setVerticalAlignment1(Constants.BOTTOM);
	  m_edgeRenderer.setVerticalAlignment2(Constants.TOP);
	  break;
	case Constants.ORIENT_BOTTOM_TOP:
	  m_nodeRenderer.setHorizontalAlignment(Constants.CENTER);
	  m_edgeRenderer.setHorizontalAlignment1(Constants.CENTER);
	  m_edgeRenderer.setHorizontalAlignment2(Constants.CENTER);
	  m_edgeRenderer.setVerticalAlignment1(Constants.TOP);
	  m_edgeRenderer.setVerticalAlignment2(Constants.BOTTOM);
	  break;
	default:
	  throw new IllegalArgumentException(
	      "Unrecognized orientation value: "+orientation);
      }
      m_orientation = orientation;
      rtl.setOrientation(orientation);
      stl.setOrientation(orientation);
    }

    public int getOrientation() {
      return m_orientation;
    }
  }
  
  /** the constant for "tree". */
  public final static String TREE = "tree";

  /** the constant for "tree.nodes". */
  public final static String TREE_NODES = "tree.nodes";

  /** the constant for "tree.edges". */
  public final static String TREE_EDGES = "tree.edges";

  /** the constant for "label". */
  public final static String LABEL = "label";

  /**
   * Get the minimum version of Weka, inclusive, the class
   * is designed to work with.  eg: <code>3.5.0</code>
   * 
   * @return		the minimum version
   */
  public String getMinVersion() {
    return "3.5.9";
  }

  /**
   * Get the maximum version of Weka, exclusive, the class
   * is designed to work with.  eg: <code>3.6.0</code>
   * 
   * @return		the maximum version
   */
  public String getMaxVersion() {
    return "3.7.1";
  }

  /**
   * Get the specific version of Weka the class is designed for.
   * eg: <code>3.5.1</code>
   * 
   * @return		the version the plugin was designed for
   */
  public String getDesignVersion() {
    return "3.5.9";
  }

  /**
   * Get a JMenu or JMenuItem which contain action listeners
   * that perform the visualization of the tree in GraphViz's dotty format.  
   * Exceptions thrown because of changes in Weka since compilation need to 
   * be caught by the implementer.
   *
   * @see NoClassDefFoundError
   * @see IncompatibleClassChangeError
   *
   * @param dotty 	the tree in dotty format
   * @param name	the name of the item (in the Explorer's history list)
   * @return menuitem 	for opening visualization(s), or null
   *         		to indicate no visualization is applicable for the input
   */
  public JMenuItem getVisualizeMenuItem(String dotty, String name) {
    JMenuItem	result;
    
    final String dottyF = dotty;
    final String nameF = name;
    result = new JMenuItem("Prefuse tree");
    result.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
	display(dottyF, nameF);
      }
    });
    
    return result;
  }

  /**
   * Displays the error.
   * 
   * @param msg		the error to display
   */
  protected void displayError(String msg) {
    JOptionPane.showMessageDialog(null, msg, "Error displaying graph", JOptionPane.ERROR_MESSAGE);
  }
  
  /**
   * Converts the dotty format to GraphML.
   * 
   * @param dotty	the graph in dotty format
   * @return		the graph in tree XML or null in case of an error
   */
  protected String convert(String dotty) {
    String	result;
    DottyToTree	d2gml;

    d2gml = new DottyToTree();
    try {
      result = d2gml.convert(dotty);
    }
    catch (Exception e) {
      result = null;
      e.printStackTrace();
      displayError(e.toString());
    }
    
    return result;
  }
  
  /**
   * Parses the graph in GraphML and returns the built graph.
   * 
   * @param graphml	the graph in GraphML
   * @return		the graph or null in case of an error
   */
  protected Tree parse(String graphml) {
    ByteArrayInputStream	inStream;
    Tree			result;

    try {
      inStream = new ByteArrayInputStream(graphml.getBytes());
      result   = (Tree) new TreeMLReader().readGraph(inStream);
    }
    catch ( Exception e ) {
      result = null;
      e.printStackTrace();
      displayError(e.toString());
    }
    
    return result;
  }
  
  /**
   * Displays the graph.
   * 
   * @param dotty	the graph in dotty format
   * @param name	the name of the graph
   */
  protected void display(String dotty, String name) {
    String	treeml;
    Tree	tree;
    TreePanel	panel;
    JFrame	frame;

    // convert dotty graph
    treeml = convert(dotty);
    if (treeml == null)
      return;
    
    // parse graph
    tree = parse(treeml);
    if (tree == null)
      return;
    
    // display graph
    panel = new TreePanel(tree);
    frame = new JFrame("Prefuse tree [" + name + "]");
    frame.setSize(800, 600);
    frame.setContentPane(panel);
    frame.setVisible(true);
  }

  /**
   * Gets a container for displaying the graph.
   * 
   * @param dotty	the graph in dotty format
   * @return a java.awt.Container that can be shown in a JFrame
   * for example.
   */
  public javax.swing.JComponent getDisplay(String dotty) 
    throws Exception {
    String	treeml;
    Tree	tree;
    TreePanel	panel;

    // convert dotty graph
    treeml = convert(dotty);
    if (treeml == null) {
      throw new Exception("Problem parsing dotty.");
    }
    
    // parse graph
    tree = parse(treeml);
    if (tree == null) {
      throw new Exception("Problem constructing visualization");
    }
    
    // display graph
    panel = new TreePanel(tree);

    return panel;
  }
}
