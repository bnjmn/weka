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
 *    Copyright (C) 2004
 *    & Matthias Schubert (schubert@dbs.ifi.lmu.de)
 *    & Zhanna Melnikova-Albrecht (melnikov@cip.ifi.lmu.de)
 *    & Rainer Holzmann (holzmann@cip.ifi.lmu.de)
 */

package weka.clusterers.forOPTICSAndDBScan.OPTICS_GUI;

import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.gui.LookAndFeel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

/**
 * Start the OPTICS Visualizer from command-line: <br/>
 * <code>java weka.clusterers.forOPTICSAndDBScan.OPTICS_GUI.OPTICS_Visualizer [file.ser]</code>
 * <br/>
 * 
 * <p>
 * OPTICS_Visualizer.java <br/>
 * Authors: Rainer Holzmann, Zhanna Melnikova-Albrecht <br/>
 * Date: Sep 12, 2004 <br/>
 * Time: 8:01:13 PM <br/>
 * $ Revision 1.4 $ <br/>
 * </p>
 *
 * @author Zhanna Melnikova-Albrecht (melnikov@cip.ifi.lmu.de)
 * @author Rainer Holzmann (holzmann@cip.ifi.lmu.de)
 * @version $Revision$
 */
public class OPTICS_Visualizer
    implements RevisionHandler {

    /**
     * Holds the OPTICS clustering results
     */
    private SERObject serObject;

    /**
     * Main Window of the OPTICS-Visualizer
     */
    private JFrame frame;

    /**
     * Statistic-frame
     */
    private JFrame statisticsFrame;

    /**
     * Help-frame
     */
    private JFrame helpFrame;

    /**
     * Listener for menu- and toolBar actions
     */
    private FrameListener frameListener;

    /**
     * Holds the toolBar and its components
     */
    private JToolBar toolBar;
    private JButton toolBarButton_open;
    private JButton toolBarButton_save;
    private JButton toolBarButton_parameters;
    private JButton toolBarButton_help;
    private JButton toolBarButton_about;

    /**
     * Holds the default-menu and its components
     */
    private JMenuBar defaultMenuBar;
    private JMenuItem open;
    private JMenuItem save;
    private JMenuItem exit;
    private JMenuItem parameters;
    private JMenuItem help;
    private JMenuItem about;

    /**
     * Holds the tabbedPane and its components
     */
    private JTabbedPane tabbedPane;
    private JTable resultVectorTable;
    private GraphPanel graphPanel;
    private JScrollPane graphPanelScrollPane;

    /**
     * Holds the settingsPanel and its components
     */
    private JPanel settingsPanel;
    private JCheckBox showCoreDistances;
    private JCheckBox showReachabilityDistances;
    private int verValue = 30;
    private JSlider verticalSlider;
    private JButton coreDistanceColorButton;
    private JButton reachDistanceColorButton;
    private JButton graphBackgroundColorButton;
    private JButton resetColorButton;

    /**
     * FileChooser for saving- and open-actions
     */
    private JFileChooser jFileChooser;
    private String lastPath;

    // *****************************************************************************************************************
    // constructors
    // *****************************************************************************************************************

    public OPTICS_Visualizer(SERObject serObject, String title) {
        this.serObject = serObject;

        LookAndFeel.setLookAndFeel();
    
        frame = new JFrame(title);

        frame.addWindowListener(new WindowAdapter() {
            /**
             * Invoked when a window is in the process of being closed.
             * The close operation can be overridden at this point.
             */
            public void windowClosing(WindowEvent e) {
                frame.dispose();
            }
        });

        frame.getContentPane().setLayout(new BorderLayout());
        frame.setSize(new Dimension(800, 600));
        Dimension screenDimension = Toolkit.getDefaultToolkit().getScreenSize();
        Rectangle windowRectangle = frame.getBounds();
        frame.setLocation((screenDimension.width - windowRectangle.width) / 2,
                (screenDimension.height - windowRectangle.height) / 2);

        frameListener = new FrameListener();
        jFileChooser = new JFileChooser();
        jFileChooser.setFileFilter(new SERFileFilter("ser", "Java Serialized Object File (*.ser)"));

        createGUI();
        frame.setVisible(true);
        frame.toFront();

    }

    // *****************************************************************************************************************
    // methods
    // *****************************************************************************************************************

    /**
     * Constructs the main-layout for the OPTICS-Visualizer
     */
    private void createGUI() {
        setMenuBar(constructDefaultMenuBar());

        frame.getContentPane().add(createToolBar(), BorderLayout.NORTH);
        frame.getContentPane().add(createTabbedPane(), BorderLayout.CENTER);
        frame.getContentPane().add(createSettingsPanel(), BorderLayout.SOUTH);
        disableSettingsPanel();
    }

    /**
     * Creates the settings-panel
     * @return Settings-panel
     */
    private JComponent createSettingsPanel() {
        settingsPanel = new JPanel(new GridBagLayout());

        SettingsPanelListener panelListener = new SettingsPanelListener();

        JPanel setPanelLeft = new JPanel(new GridBagLayout());
        setPanelLeft.setBorder(BorderFactory.createTitledBorder(" General Settings "));

        JPanel checkBoxesPanel = new JPanel(new GridLayout(1, 2));
        showCoreDistances = new JCheckBox("Show Core-Distances");
        showCoreDistances.setSelected(true);
        showReachabilityDistances = new JCheckBox("Show Reachability-Distances");
        showReachabilityDistances.setSelected(true);
        showCoreDistances.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    graphPanel.setShowCoreDistances(true);
                    graphPanel.adjustSize(serObject);
                    graphPanel.repaint();
                } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                    graphPanel.setShowCoreDistances(false);
                    graphPanel.adjustSize(serObject);
                    graphPanel.repaint();
                }
            }
        });
        showReachabilityDistances.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    graphPanel.setShowReachabilityDistances(true);
                    graphPanel.adjustSize(serObject);
                    graphPanel.repaint();
                } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                    graphPanel.setShowReachabilityDistances(false);
                    graphPanel.adjustSize(serObject);
                    graphPanel.repaint();
                }
            }
        });

        checkBoxesPanel.add(showCoreDistances);
        checkBoxesPanel.add(showReachabilityDistances);

        JPanel verticalAdPanel = new JPanel(new BorderLayout());
        final JLabel verValueLabel = new JLabel("Vertical Adjustment: " + verValue);
        verticalAdPanel.add(verValueLabel, BorderLayout.NORTH);
        verticalSlider = new JSlider(JSlider.HORIZONTAL, 0, frame.getHeight(), verValue);
        verticalSlider.setMajorTickSpacing(100);
        verticalSlider.setMinorTickSpacing(10);
        verticalSlider.setPaintTicks(true);
        verticalSlider.setPaintLabels(true);
        verticalSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (!verticalSlider.getValueIsAdjusting()) {
                    verValue = verticalSlider.getValue();
                    verValueLabel.setText("Vertical Adjustment: " + verValue);
                    graphPanel.setVerticalAdjustment(verValue);
                    graphPanel.repaint();
                }
            }
        });
        verticalAdPanel.add(verticalSlider, BorderLayout.CENTER);

        setPanelLeft.add(checkBoxesPanel,
                new GridBagConstraints(0, 0, 1, 1, 1, 1,
                        GridBagConstraints.CENTER,
                        GridBagConstraints.BOTH,
                        new Insets(5, 5, 5, 5), 0, 0));
        setPanelLeft.add(verticalAdPanel,
                new GridBagConstraints(0, 1, 1, 1, 1, 1,
                        GridBagConstraints.CENTER,
                        GridBagConstraints.BOTH,
                        new Insets(5, 5, 5, 5), 0, 0));

        settingsPanel.add(setPanelLeft,
                new GridBagConstraints(0, 0, 1, 1, 3, 1,
                        GridBagConstraints.CENTER,
                        GridBagConstraints.BOTH,
                        new Insets(5, 5, 5, 0), 0, 0));

        JPanel setPanelRight = new JPanel(new GridBagLayout());
        setPanelRight.setBorder(BorderFactory.createTitledBorder(" Colors "));

        JPanel colorsPanel = new JPanel(new GridLayout(4, 2, 10, 10));

        colorsPanel.add(new JLabel("Core-Distance: "));
        coreDistanceColorButton = new JButton();
        coreDistanceColorButton.setBackground(new Color(100, 100, 100));
        coreDistanceColorButton.addActionListener(panelListener);
        colorsPanel.add(coreDistanceColorButton);

        colorsPanel.add(new JLabel("Reachability-Distance: "));
        reachDistanceColorButton = new JButton();
        reachDistanceColorButton.setBackground(Color.orange);
        reachDistanceColorButton.addActionListener(panelListener);
        colorsPanel.add(reachDistanceColorButton);

        colorsPanel.add(new JLabel("Graph Background: "));
        graphBackgroundColorButton = new JButton();
        graphBackgroundColorButton.setBackground(new Color(255, 255, 179));
        graphBackgroundColorButton.addActionListener(panelListener);
        colorsPanel.add(graphBackgroundColorButton);

        colorsPanel.add(new JLabel());
        resetColorButton = new JButton("Reset");
        resetColorButton.addActionListener(panelListener);
        colorsPanel.add(resetColorButton);

        setPanelRight.add(colorsPanel,
                new GridBagConstraints(0, 0, 1, 1, 1, 1,
                        GridBagConstraints.CENTER,
                        GridBagConstraints.BOTH,
                        new Insets(5, 5, 5, 5), 0, 0));

        settingsPanel.add(setPanelRight,
                new GridBagConstraints(1, 0, 1, 1, 1, 1,
                        GridBagConstraints.CENTER,
                        GridBagConstraints.BOTH,
                        new Insets(5, 5, 5, 5), 0, 0));

        return settingsPanel;
    }

    /**
     * Disables all components from the settingsPanel
     */
    private void disableSettingsPanel() {

        verticalSlider.setEnabled(false);
        coreDistanceColorButton.setEnabled(false);
        reachDistanceColorButton.setEnabled(false);
        graphBackgroundColorButton.setEnabled(false);
        resetColorButton.setEnabled(false);
        settingsPanel.setVisible(false);
    }

    /**
     * Enables all components from the settingsPanel
     */
    private void enableSettingsPanel() {
        verticalSlider.setEnabled(true);
        coreDistanceColorButton.setEnabled(true);
        reachDistanceColorButton.setEnabled(true);
        graphBackgroundColorButton.setEnabled(true);
        resetColorButton.setEnabled(true);
        settingsPanel.setVisible(true);
    }

    /**
     * Creates the TabbedPane
     * @return TabbedPane
     */
    private JComponent createTabbedPane() {
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Table", new ImageIcon(Toolkit.getDefaultToolkit().
            getImage(ClassLoader.getSystemResource("weka/clusterers/forOPTICSAndDBScan/OPTICS_GUI/Graphics/Table16.gif"))),
            clusteringResultsTable(),
        "Show table of DataObjects, Core- and Reachability-Distances");
        if (serObject != null)
          tabbedPane.addTab("Graph - Epsilon: " + serObject.getEpsilon() + ", MinPoints: " + serObject.getMinPoints()
              , new ImageIcon(Toolkit.getDefaultToolkit().
        	  getImage(ClassLoader.getSystemResource("weka/clusterers/forOPTICSAndDBScan/OPTICS_GUI/Graphics/Graph16.gif"))),
        	  graphPanel(),
          "Show Plot of Core- and Reachability-Distances");
        else
          tabbedPane.addTab(
              "Graph - Epsilon: --, MinPoints: --", 
              new ImageIcon(
        	  Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource("weka/clusterers/forOPTICSAndDBScan/OPTICS_GUI/Graphics/Graph16.gif"))),
        	  graphPanel(),
          "Show Plot of Core- and Reachability-Distances");

        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                int c = tabbedPane.getSelectedIndex();
                if (c == 0)
                    disableSettingsPanel();
                else
                    enableSettingsPanel();
            }
        });

        return tabbedPane;
    }

    /**
     * Creates the ToolBar
     * @return ToolBar
     */
    private JComponent createToolBar() {
        toolBar = new JToolBar();
        toolBar.setName("OPTICS Visualizer ToolBar");
        toolBar.setFloatable(false);
        toolBarButton_open = new JButton(new ImageIcon(Toolkit.getDefaultToolkit().
                getImage(ClassLoader.getSystemResource("weka/clusterers/forOPTICSAndDBScan/OPTICS_GUI/Graphics/Open16.gif"))));
        toolBarButton_open.setToolTipText("Open OPTICS-Session");
        toolBarButton_open.addActionListener(frameListener);
        toolBar.add(toolBarButton_open);

        toolBarButton_save = new JButton(new ImageIcon(Toolkit.getDefaultToolkit().
                getImage(ClassLoader.getSystemResource("weka/clusterers/forOPTICSAndDBScan/OPTICS_GUI/Graphics/Save16.gif"))));
        toolBarButton_save.setToolTipText("Save OPTICS-Session");
        toolBarButton_save.addActionListener(frameListener);
        toolBar.add(toolBarButton_save);
        toolBar.addSeparator(new Dimension(10, 25));

        toolBarButton_parameters = new JButton(new ImageIcon(Toolkit.getDefaultToolkit().
                getImage(ClassLoader.getSystemResource("weka/clusterers/forOPTICSAndDBScan/OPTICS_GUI/Graphics/Parameters16.gif"))));
        toolBarButton_parameters.setToolTipText("Show epsilon, MinPoints...");
        toolBarButton_parameters.addActionListener(frameListener);
        toolBar.add(toolBarButton_parameters);

        toolBar.addSeparator(new Dimension(10, 25));

        toolBarButton_help = new JButton(new ImageIcon(Toolkit.getDefaultToolkit().
                getImage(ClassLoader.getSystemResource("weka/clusterers/forOPTICSAndDBScan/OPTICS_GUI/Graphics/Help16.gif"))));
        toolBarButton_help.setToolTipText("Help topics");
        toolBarButton_help.addActionListener(frameListener);
        toolBar.add(toolBarButton_help);

        toolBarButton_about = new JButton(new ImageIcon(Toolkit.getDefaultToolkit().
                getImage(ClassLoader.getSystemResource("weka/clusterers/forOPTICSAndDBScan/OPTICS_GUI/Graphics/Information16.gif"))));
        toolBarButton_about.setToolTipText("About");
        toolBarButton_about.addActionListener(frameListener);
        toolBar.add(toolBarButton_about);

        return toolBar;
    }

    /**
     * Creates the OPTICS clustering results table
     * @return Table
     */
    private JComponent clusteringResultsTable() {
        resultVectorTable = new JTable();
        String[] resultVectorTableColumnNames = {"Key",
                                                 "DataObject",
                                                 "Core-Distance",
                                                 "Reachability-Distance"};

        DefaultTableColumnModel resultVectorTableColumnModel = new DefaultTableColumnModel();
        for (int i = 0; i < resultVectorTableColumnNames.length; i++) {
            TableColumn tc = new TableColumn(i);
            tc.setHeaderValue(resultVectorTableColumnNames[i]);
            resultVectorTableColumnModel.addColumn(tc);
        }

        ResultVectorTableModel resultVectorTableModel;
        if (serObject != null)
          resultVectorTableModel = new ResultVectorTableModel(serObject.getResultVector());
        else
          resultVectorTableModel = new ResultVectorTableModel(null);
        resultVectorTable = new JTable(resultVectorTableModel, resultVectorTableColumnModel);
        resultVectorTable.getColumnModel().getColumn(0).setPreferredWidth(70);
        resultVectorTable.getColumnModel().getColumn(1).setPreferredWidth(400);
        resultVectorTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        resultVectorTable.getColumnModel().getColumn(3).setPreferredWidth(150);
        resultVectorTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JScrollPane resultVectorTableScrollPane = new JScrollPane(resultVectorTable,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        return resultVectorTableScrollPane;
    }

    /**
     * Creates the OPTICS Plot
     * @return JComponent with the PLOT
     */
    private JComponent graphPanel() {

        if (serObject == null) {
          graphPanel = new GraphPanel(new ArrayList(), verValue, true, true);
        }
        else {
          graphPanel = new GraphPanel(serObject.getResultVector(), verValue, true, true);
          graphPanel.setPreferredSize(new Dimension((10 * serObject.getDatabaseSize()) +
              serObject.getDatabaseSize(), graphPanel.getHeight()));
        }
        graphPanel.setBackground(new Color(255, 255, 179));
        graphPanel.setOpaque(true);

        graphPanelScrollPane = new JScrollPane(graphPanel,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        return (graphPanelScrollPane);
    }

    /**
     * Constructs the default MenuBar
     * @return MenuBar
     */
    private JMenuBar constructDefaultMenuBar() {
        defaultMenuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');
        open = new JMenuItem("Open...", new ImageIcon(Toolkit.getDefaultToolkit().
                getImage(ClassLoader.getSystemResource("weka/clusterers/forOPTICSAndDBScan/OPTICS_GUI/Graphics/Open16.gif"))));
        open.setMnemonic('O');
        open.setAccelerator(KeyStroke.getKeyStroke('O', Event.CTRL_MASK));
        open.addActionListener(frameListener);
        fileMenu.add(open);

        save = new JMenuItem("Save...", new ImageIcon(Toolkit.getDefaultToolkit().
                getImage(ClassLoader.getSystemResource("weka/clusterers/forOPTICSAndDBScan/OPTICS_GUI/Graphics/Save16.gif"))));
        save.setMnemonic('S');
        save.setAccelerator(KeyStroke.getKeyStroke('S', Event.CTRL_MASK));
        save.addActionListener(frameListener);
        fileMenu.add(save);

        fileMenu.addSeparator();

        exit = new JMenuItem("Exit", 'X');
        exit.addActionListener(frameListener);
        fileMenu.add(exit);

        defaultMenuBar.add(fileMenu);

        JMenu toolsMenu = new JMenu("View");
        toolsMenu.setMnemonic('V');
        parameters = new JMenuItem("Parameters...", new ImageIcon(Toolkit.getDefaultToolkit().
                getImage(ClassLoader.getSystemResource("weka/clusterers/forOPTICSAndDBScan/OPTICS_GUI/Graphics/Parameters16.gif"))));
        parameters.setMnemonic('P');
        parameters.setAccelerator(KeyStroke.getKeyStroke('P', Event.CTRL_MASK));
        parameters.addActionListener(frameListener);
        toolsMenu.add(parameters);

        defaultMenuBar.add(toolsMenu);

        JMenu miscMenu = new JMenu("Help");
        miscMenu.setMnemonic('H');
        help = new JMenuItem("Help Topics", new ImageIcon(Toolkit.getDefaultToolkit().
                getImage(ClassLoader.getSystemResource("weka/clusterers/forOPTICSAndDBScan/OPTICS_GUI/Graphics/Help16.gif"))));
        help.setMnemonic('H');
        help.setAccelerator(KeyStroke.getKeyStroke('H', Event.CTRL_MASK));
        help.addActionListener(frameListener);
        miscMenu.add(help);

        about = new JMenuItem("About...", new ImageIcon(Toolkit.getDefaultToolkit().
                getImage(ClassLoader.getSystemResource("weka/clusterers/forOPTICSAndDBScan/OPTICS_GUI/Graphics/Information16.gif"))));
        about.setMnemonic('A');
        about.setAccelerator(KeyStroke.getKeyStroke('A', Event.CTRL_MASK));
        about.addActionListener(frameListener);
        miscMenu.add(about);
        defaultMenuBar.add(miscMenu);

        return defaultMenuBar;
    }

    /**
     * Sets a MenuBar for the this frame
     * @param menuBar New MenuBar
     */
    private void setMenuBar(JMenuBar menuBar) {
        frame.setJMenuBar(menuBar);
    }

    /**
     * Shows a little frame with statistic information about the OPTICS-results
     */
    private void loadStatisticsFrame() {
        statisticsFrame = new JFrame("Parameters");
        statisticsFrame.getContentPane().setLayout(new BorderLayout());

        JPanel statPanel_Labels = new JPanel(new GridBagLayout());
        JPanel statPanel_Labels_Left = new JPanel(new GridLayout(9, 1));
        JPanel statPanel_Labels_Right = new JPanel(new GridLayout(9, 1));

        statPanel_Labels_Left.add(new JLabel("Number of clustered DataObjects: "));
        statPanel_Labels_Right.add(new JLabel(Integer.toString(serObject.getDatabaseSize())));
        statPanel_Labels_Left.add(new JLabel("Number of attributes: "));
        statPanel_Labels_Right.add(new JLabel(Integer.toString(serObject.getNumberOfAttributes())));
        statPanel_Labels_Left.add(new JLabel("Epsilon: "));
        statPanel_Labels_Right.add(new JLabel(Double.toString(serObject.getEpsilon())));
        statPanel_Labels_Left.add(new JLabel("MinPoints: "));
        statPanel_Labels_Right.add(new JLabel(Integer.toString(serObject.getMinPoints())));
        statPanel_Labels_Left.add(new JLabel("Write results to file: "));
        statPanel_Labels_Right.add(new JLabel(serObject.isOpticsOutputs() ? "yes" : "no"));
        statPanel_Labels_Left.add(new JLabel("Distance-Type: "));
        statPanel_Labels_Right.add(new JLabel(serObject.getDistanceFunction().toString()));
        statPanel_Labels_Left.add(new JLabel("Number of generated clusters: "));
        statPanel_Labels_Right.add(new JLabel(Integer.toString(serObject.getNumberOfGeneratedClusters())));
        statPanel_Labels_Left.add(new JLabel("Elapsed-time: "));
        statPanel_Labels_Right.add(new JLabel(serObject.getElapsedTime()));
        statPanel_Labels.setBorder(BorderFactory.createTitledBorder(" OPTICS parameters "));

        statPanel_Labels.add(statPanel_Labels_Left,
                new GridBagConstraints(0, 0, 1, 1, 1, 1,
                        GridBagConstraints.CENTER,
                        GridBagConstraints.BOTH,
                        new Insets(0, 5, 2, 0), 0, 0));

        statPanel_Labels.add(statPanel_Labels_Right,
                new GridBagConstraints(1, 0, 1, 1, 3, 1,
                        GridBagConstraints.CENTER,
                        GridBagConstraints.BOTH,
                        new Insets(0, 5, 2, 5), 0, 0));

        statisticsFrame.getContentPane().add(statPanel_Labels, BorderLayout.CENTER);

        statisticsFrame.addWindowListener(new WindowAdapter() {
            /**
             * Invoked when a window is in the process of being closed.
             * The close operation can be overridden at this point.
             */
            public void windowClosing(WindowEvent e) {
                statisticsFrame.dispose();
            }
        });

        JPanel okButtonPanel = new JPanel(new GridBagLayout());

        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("OK")) {
                    statisticsFrame.dispose();
                }
            }
        });
        okButtonPanel.add(okButton,
                new GridBagConstraints(0, 0, 1, 1, 1, 1,
                        GridBagConstraints.CENTER,
                        GridBagConstraints.NONE,
                        new Insets(5, 0, 5, 0), 0, 0));

        statisticsFrame.getContentPane().add(okButtonPanel, BorderLayout.SOUTH);
        statisticsFrame.setSize(new Dimension(500, 300));
        Rectangle frameDimension = frame.getBounds();
        Point p = frame.getLocation();
        Rectangle statisticsFrameDimension = statisticsFrame.getBounds();
        statisticsFrame.setLocation(((frameDimension.width - statisticsFrameDimension.width) / 2) + (int) p.getX(),
                ((frameDimension.height - statisticsFrameDimension.height) / 2) + (int) p.getY());
        statisticsFrame.setVisible(true);
        statisticsFrame.toFront();
    }

    /**
     * Shows a little frame with information about handling the OPTICS Visualizer
     */
    private void loadHelpFrame() {
        helpFrame = new JFrame("Help Topics");
        helpFrame.getContentPane().setLayout(new BorderLayout());

        JPanel helpPanel = new JPanel(new GridBagLayout());
        JTextArea helpTextArea = new JTextArea();
        helpTextArea.setEditable(false);
        helpTextArea.append(
                "OPTICS Visualizer Help\n"
                + "===========================================================\n\n"
                + "Open\n"
                + " - Open OPTICS-Session\n"
                + "   [Ctrl-O], File | Open\n\n"
                + "Save\n"
                + " - Save OPTICS-Session\n"
                + "   [Ctrl-S], File | Save\n\n"
                + "Exit\n"
                + " - Exit OPTICS Visualizer\n"
                + "   [Alt-F4], File | Exit\n\n"
                + "Parameters\n"
                + " - Show epsilon, MinPoints...\n"
                + "   [Ctrl-P], View | Parameters\n\n"
                + "Help Topics\n"
                + " - Show this frame\n"
                + "   [Ctrl-H], Help | Help Topics\n\n"
                + "About\n"
                + " - Copyright-Information\n"
                + "   [Ctrl-A], Help | About\n\n\n"
                + "Table-Pane:\n"
                + "-----------------------------------------------------------\n"
                + "The table represents the calculated clustering-order.\n"
                + "To save the table please select File | Save from the\n"
                + "menubar. Restart OPTICS with the -F option to obtain\n"
                + "an ASCII-formatted file of the clustering-order.\n\n"
                + "Graph-Pane:\n"
                + "-----------------------------------------------------------\n"
                + "The graph draws the plot of core- and reachability-\n"
                + "distances. By (de-)activating core- and reachability-\n"
                + "distances in the 'General Settings'-Panel you can\n"
                + "influence the visualization in detail. Simply use the\n"
                + "'Vertical Adjustment'-Slider to emphasize the plot of\n"
                + "distances. The 'Colors'-Panel lets you define different\n"
                + "colors of the graph background, core- and reachability-\n"
                + "distances. Click the 'Reset'-Button to restore the\n"
                + "defaults.\n"
        );
        final JScrollPane helpTextAreaScrollPane = new JScrollPane(helpTextArea,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        helpTextAreaScrollPane.setBorder(BorderFactory.createEtchedBorder());
        helpPanel.add(helpTextAreaScrollPane,
                new GridBagConstraints(0, 0, 1, 1, 1, 1,
                        GridBagConstraints.CENTER,
                        GridBagConstraints.BOTH,
                        new Insets(5, 5, 7, 5), 0, 0));

        helpFrame.getContentPane().add(helpPanel, BorderLayout.CENTER);

        helpFrame.addWindowListener(new WindowAdapter() {
            /**
             * Invoked when a window is in the process of being closed.
             * The close operation can be overridden at this point.
             */
            public void windowClosing(WindowEvent e) {
                helpFrame.dispose();
            }

            /**
             * Invoked when a window has been opened.
             */
            public void windowOpened(WindowEvent e) {
                helpTextAreaScrollPane.getVerticalScrollBar().setValue(0);
            }
        });

        JPanel closeButtonPanel = new JPanel(new GridBagLayout());

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("Close")) {
                    helpFrame.dispose();
                }
            }
        });
        closeButtonPanel.add(closeButton,
                new GridBagConstraints(0, 0, 1, 1, 1, 1,
                        GridBagConstraints.CENTER,
                        GridBagConstraints.NONE,
                        new Insets(0, 0, 5, 0), 0, 0));

        helpFrame.getContentPane().add(closeButtonPanel, BorderLayout.SOUTH);
        helpFrame.setSize(new Dimension(480, 400));
        Rectangle frameDimension = frame.getBounds();
        Point p = frame.getLocation();
        Rectangle helpFrameDimension = helpFrame.getBounds();
        helpFrame.setLocation(((frameDimension.width - helpFrameDimension.width) / 2) + (int) p.getX(),
                ((frameDimension.height - helpFrameDimension.height) / 2) + (int) p.getY());
        helpFrame.setVisible(true);
        helpFrame.toFront();
    }
    
    /**
     * Returns the revision string.
     * 
     * @return		the revision
     */
    public String getRevision() {
      return RevisionUtils.extract("$Revision$");
    }
    
    /**
     * Displays the GUI. If an optics file is provided as first parameter,
     * this will be loaded and displayed automatically.
     * 
     * @param args		the commandline parameters
     * @throws Exception	if something goes wrong
     */
    public static void main(String[] args) throws Exception {
      SERObject serObject = null;
      if (args.length == 1) {
	System.out.println("Attempting to load: " + args[0]);
	ObjectInputStream is = null;
	try {
	  FileInputStream fs = new FileInputStream(args[0]);
	  is = new ObjectInputStream(fs);
	  serObject = (SERObject) is.readObject();
	}
	catch (Exception e) {
	  serObject = null;
	  JOptionPane.showMessageDialog(
	      null,
	      "Error loading file:\n" + e,
	      "Error", JOptionPane.ERROR_MESSAGE);
	}
	finally {
	  try {
	    is.close();
	  }
	  catch (Exception e) {
	    // ignored
	  }
	}
      }
      
      // open GUI
      new OPTICS_Visualizer(serObject, "OPTICS Visualizer - Main Window");
    }

    // *****************************************************************************************************************
    // inner classes
    // *****************************************************************************************************************

    private class FrameListener
        implements ActionListener, RevisionHandler {
      
        /**
         * Invoked when an action occurs.
         */
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == parameters || e.getSource() == toolBarButton_parameters) {
                loadStatisticsFrame();
            }

            if (e.getSource() == about || e.getSource() == toolBarButton_about) {
                JOptionPane.showMessageDialog(frame,
                        "OPTICS Visualizer\n$ Rev 1.4 $\n\nCopyright (C) 2004 " +
                        "Rainer Holzmann, Zhanna Melnikova-Albrecht",
                        "About", JOptionPane.INFORMATION_MESSAGE);
            }

            if (e.getSource() == help || e.getSource() == toolBarButton_help) {
                loadHelpFrame();
            }

            if (e.getSource() == exit) {
                frame.dispose();
            }

            if (e.getSource() == open || e.getSource() == toolBarButton_open) {
                jFileChooser.setDialogTitle("Open OPTICS-Session");
                if (lastPath == null) {
                    lastPath = System.getProperty("user.dir");
                }
                jFileChooser.setCurrentDirectory(new File(lastPath));
                int ret = jFileChooser.showOpenDialog(frame);
                SERObject serObject_1 = null;
                if (ret == JFileChooser.APPROVE_OPTION) {
                    File f = jFileChooser.getSelectedFile();
                    try {
                        FileInputStream fs = new FileInputStream(f.getAbsolutePath());
                        ObjectInputStream is = new ObjectInputStream(fs);
                        serObject_1 = (SERObject) is.readObject();
                        is.close();
                    } catch (FileNotFoundException e1) {
                        JOptionPane.showMessageDialog(frame,
                                "File not found.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                    } catch (ClassNotFoundException e1) {
                        JOptionPane.showMessageDialog(frame,
                                "OPTICS-Session could not be read.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                    } catch (IOException e1) {
                        JOptionPane.showMessageDialog(frame,
                                "This file does not contain a valid OPTICS-Session.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    if (serObject_1 != null) {
                        int ret_1 = JOptionPane.showConfirmDialog(frame,
                                "Open OPTICS-Session in a new window?",
                                "Open", 1);
                        switch (ret_1) {
                            case 0:
                                new OPTICS_Visualizer(serObject_1, "OPTICS Visualizer - " + f.getName());
                                break;
                            case 1:
                                serObject = serObject_1;
                                resultVectorTable.setModel(new ResultVectorTableModel(serObject.getResultVector()));
                                tabbedPane.setTitleAt(1,
                                        "Graph - Epsilon: " + serObject.getEpsilon() +
                                        ", MinPoints: " + serObject.getMinPoints());
                                graphPanel.setResultVector(serObject.getResultVector());
                                graphPanel.adjustSize(serObject);
                                graphPanel.repaint();
                                break;
                            default:
                                break;
                        }
                    }
                }
            }

            if (e.getSource() == save || e.getSource() == toolBarButton_save) {
                jFileChooser.setDialogTitle("Save OPTICS-Session");

                GregorianCalendar gregorianCalendar = new GregorianCalendar();
                String timeStamp = gregorianCalendar.get(Calendar.DAY_OF_MONTH) + "-" +
                        (gregorianCalendar.get(Calendar.MONTH) + 1) +
                        "-" + gregorianCalendar.get(Calendar.YEAR) +
                        "--" + gregorianCalendar.get(Calendar.HOUR_OF_DAY) +
                        "-" + gregorianCalendar.get(Calendar.MINUTE) +
                        "-" + gregorianCalendar.get(Calendar.SECOND);
                String filename = "OPTICS_" + timeStamp + ".ser";

                File file = new File(filename);
                jFileChooser.setSelectedFile(file);
                if (lastPath == null) {
                    lastPath = System.getProperty("user.dir");
                }
                jFileChooser.setCurrentDirectory(new File(lastPath));

                int ret = jFileChooser.showSaveDialog(frame);
                if (ret == JFileChooser.APPROVE_OPTION) {
                    file = jFileChooser.getSelectedFile();
                    try {
                        FileOutputStream fs = new FileOutputStream(file.getAbsolutePath());
                        ObjectOutputStream os = new ObjectOutputStream(fs);
                        os.writeObject(serObject);
                        os.flush();
                        os.close();
                    } catch (IOException e1) {
                        JOptionPane.showMessageDialog(frame,
                                "OPTICS-Session could not be saved.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }
        
        /**
         * Returns the revision string.
         * 
         * @return		the revision
         */
        public String getRevision() {
          return RevisionUtils.extract("$Revision$");
        }
    }

    private class SettingsPanelListener
        implements ActionListener, RevisionHandler {
      
        /**
         * Invoked when an action occurs.
         */
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == coreDistanceColorButton) {
                Color c = getSelectedColor("Select 'Core-Distance' color");
                if (c != null) {
                    coreDistanceColorButton.setBackground(c);
                    graphPanel.setCoreDistanceColor(c);
                }
            }
            if (e.getSource() == reachDistanceColorButton) {
                Color c = getSelectedColor("Select 'Reachability-Distance' color");
                if (c != null) {
                    reachDistanceColorButton.setBackground(c);
                    graphPanel.setReachabilityDistanceColor(c);
                }
            }
            if (e.getSource() == graphBackgroundColorButton) {
                Color c = getSelectedColor("Select 'Graph Background' color");
                if (c != null) {
                    graphBackgroundColorButton.setBackground(c);
                    graphPanel.setBackground(c);
                }
            }
            if (e.getSource() == resetColorButton) {
                coreDistanceColorButton.setBackground(new Color(100, 100, 100));
                graphPanel.setCoreDistanceColor(new Color(100, 100, 100));
                reachDistanceColorButton.setBackground(Color.orange);
                graphPanel.setReachabilityDistanceColor(Color.orange);
                graphBackgroundColorButton.setBackground(new Color(255, 255, 179));
                graphPanel.setBackground(new Color(255, 255, 179));
                graphPanel.repaint();
            }
        }

        private Color getSelectedColor(String title) {
            Color c = JColorChooser.showDialog(frame, title, Color.black);
            return c;
        }
        
        /**
         * Returns the revision string.
         * 
         * @return		the revision
         */
        public String getRevision() {
          return RevisionUtils.extract("$Revision$");
        }
    }
}
