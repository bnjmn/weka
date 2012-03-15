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
 * SortedListPanel.java
 *
 * Created on 31 janvier 2003, 16:41
 */

/*
 *    SortedListPanel.java
 *    Copyright (C) 2003 DESS IAGL of Lille
 *
 */
package sortedListPanel;

import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *
 * @author  beleg
 */
public class SortedListPanel extends JPanel implements ListSelectionListener {

	/** This field is the ComparatorFactory with which the list will be sorted.
	 */
	private ParameterComparatorFactory factory;

	/** This field is the number of combined criterias by which we can sort the list.
	 */
	private int nbCriteres;

	/** This field is the ListCellRenderer used by the JList to display the elements.
	 */
	private ListCellRenderer theCellRenderer;

	/** The title of the Panel
	 */
	private String title;

	/** The model to fill the JList with.
	 */
	private SortedListModel theListModel = null;

	/** The model to fill the JComboBoxs with.
	 */
	private SorterComboBoxModel[] theComboBoxModel = null;

	private Sorter theSorter;
	private ListSelectionListener theListener;

	/** Creates a new SortedListPanel
	 *@param c the AbstractParameterComparator used to sort the list.
	 *@param nbCriteres the number of JComboBox used to sort the list.
	 *@param aListCellRenderer the ListCellRenderer used to show the elements of the list.
	 *@param title the title of the panel
	*/
	public SortedListPanel(
		ParameterComparatorFactory pcf,
		int nbCriteres,
		ListCellRenderer aListCellRenderer,
		String title) {
		this.factory = pcf;
		this.nbCriteres = nbCriteres;
		this.theCellRenderer = aListCellRenderer;
		this.title = title;
		theSorter = new Sorter();
		initComponents();
	}

	public void addListSelectionListener(ListSelectionListener listener) {
		theListener = listener;
	}

	public void valueChanged(ListSelectionEvent e) {
		theListener.valueChanged(
			new ListSelectionEvent(
				this,
				e.getFirstIndex(),
				e.getLastIndex(),
				e.getValueIsAdjusting()));
	}

	public void setSorter(Sorter aSorter) {
		theSorter = aSorter;
	}

	public Sorter getSorter() {
		return theSorter;
	}

	/**Set the contents of the list and the criteria used to sort the list
	 *@param objects the elements of the list
	 *@param criteres the criteria used to sort the list
	 */
	public void setContents(Object[] objects, String[] criteres) {

		theSorter.clear();

		if (theListModel == null) {
			theListModel = new SortedListModel(factory, theSorter, objects);
			theJList.setModel(theListModel);
		} else {
			theListModel.setElements(objects);
			theJList.clearSelection();
		}

		if (theComboBoxModel == null) {
			theComboBoxModel = new SorterComboBoxModel[nbCriteres];

			for (int i = 0; i < nbCriteres; i++) {
				theComboBoxModel[i] = new SorterComboBoxModel(criteres);
				critereComboBox[i].setModel(theComboBoxModel[i]);
			}
		} else {
			for (int i = 0; i < nbCriteres; i++)
				theComboBoxModel[i].setElements(criteres);
		}

	}

	public Object[] getSelectedElements() {
		return theJList.getSelectedValues();
	}

	public void setSelectedElements(Object[] objects) {
		theJList.clearSelection();

		LinkedList indices = new LinkedList();
		ListModel theObjects = theJList.getModel();
		for (int i = 0; i < theObjects.getSize(); i++) {
			for (int j = 0; j < objects.length; j++) {
				if (theObjects.getElementAt(i).equals(objects[j])) {
					indices.add(new Integer(i));
				}
			}
		}

		int[] theIndices = new int[indices.size()];
		Iterator it = indices.listIterator();
		int i = 0;
		while (it.hasNext()) {
			Integer indice = (Integer) it.next();
			theIndices[i] = indice.intValue();
			i++;
		}


		theJList.setSelectedIndices(theIndices);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 */
	protected void initComponents() {

		listScrollPane = new javax.swing.JScrollPane();
		theJList = new javax.swing.JList();
		theJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		theJList.addListSelectionListener(this);
		critereComboBox = new JComboBox[nbCriteres];
		sortLabel = new JLabel[nbCriteres];
		selectButton = new javax.swing.JButton();
		unselectButton = new javax.swing.JButton();

		setLayout(new java.awt.GridBagLayout());

		if (title != null)
			setBorder(new javax.swing.border.TitledBorder(title));
		theJList.setCellRenderer(theCellRenderer);
		listScrollPane.setViewportView(theJList);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 100.0;
		gridBagConstraints.weighty = 100.0;
		add(listScrollPane, gridBagConstraints);

		ActionListener combosListener = new ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				sort(evt);
			}
		};

		for (int i = 1; i <= nbCriteres; i++) {

			sortLabel[i - 1] = new JLabel();
			sortLabel[i - 1].setText("Sort by :");
			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = i + 1;
			gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
			gridBagConstraints.weightx = 20.0;
			add(sortLabel[i - 1], gridBagConstraints);

			critereComboBox[i - 1] = new JComboBox();
			critereComboBox[i - 1].addActionListener(combosListener);

			gridBagConstraints = new java.awt.GridBagConstraints();
			gridBagConstraints.gridx = 1;
			gridBagConstraints.gridy = i + 1;
			gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gridBagConstraints.weightx = 80.0;
			add(critereComboBox[i - 1], gridBagConstraints);

		}

	}

	/** sort the list
	 */
	protected void sort(java.awt.event.ActionEvent evt) {
		JComboBox theSource = (JComboBox) evt.getSource();
		LinkedList criteresList = new LinkedList();

		boolean end = false;
		int i = 0;

		while (!end) {

			String nameCritere = (String) critereComboBox[i].getSelectedItem();
			criteresList.add(nameCritere);
			if (theSource == critereComboBox[i])
				end = true;
			i++;

		}
		theListModel.sort(criteresList);
	}

	public void setMultipleSelection() {

		theJList.setSelectionMode(
			ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		selectButton.setText("Select All");
		selectButton.setToolTipText("Select all the rules");
		selectButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				selectAll(evt);
			}
		});

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.weightx = 50.0;
		add(selectButton, gridBagConstraints);

		unselectButton.setText("Unselect All");
		unselectButton.setToolTipText("Unselect all the rules");
		unselectButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				unselectAll(evt);
			}
		});

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
		gridBagConstraints.weightx = 50.0;
		add(unselectButton, gridBagConstraints);

	}

	private void unselectAll(java.awt.event.ActionEvent evt) {
		ListModel lm = theJList.getModel();
		int size = lm.getSize();
		theJList.removeSelectionInterval(0, size - 1);
	}

	private void selectAll(java.awt.event.ActionEvent evt) {
		ListModel lm = theJList.getModel();
		int size = lm.getSize();
		theJList.addSelectionInterval(0, size - 1);
	}

	protected JScrollPane listScrollPane;
	protected JButton selectButton;
	protected JComboBox[] critereComboBox;
	protected JList theJList;
	protected JButton unselectButton;
	protected JLabel[] sortLabel;
	protected java.awt.GridBagConstraints gridBagConstraints;

}
