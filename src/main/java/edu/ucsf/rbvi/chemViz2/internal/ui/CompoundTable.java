/*
  Copyright (c) 2006, 2007, 2008 The Cytoscape Consortium (www.cytoscape.org)

  The Cytoscape Consortium is:
  - Institute for Systems Biology
  - University of California San Diego
  - Memorial Sloan-Kettering Cancer Center
  - Institut Pasteur
  - Agilent Technologies

  This library is free software; you can redistribute it and/or modify it
  under the terms of the GNU Lesser General Public License as published
  by the Free Software Foundation; either version 2.1 of the License, or
  any later version.

  This library is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
  MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
  documentation provided hereunder is on an "as is" basis, and the
  Institute for Systems Biology and the Whitehead Institute
  have no obligations to provide maintenance, support,
  updates, enhancements or modifications.  In no event shall the
  Institute for Systems Biology and the Whitehead Institute
  be liable to any party for direct, indirect, special,
  incidental or consequential damages, including lost profits, arising
  out of the use of this software and its documentation, even if the
  Institute for Systems Biology and the Whitehead Institute
  have been advised of the possibility of such damage.  See
  the GNU Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with this library; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
*/

package edu.ucsf.rbvi.chemViz2.internal.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Point;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.io.IOException;
import java.io.File;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;

import edu.ucsf.rbvi.chemViz2.internal.model.ChemInfoSettings;
import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Descriptor;
import edu.ucsf.rbvi.chemViz2.internal.model.DescriptorManager;
import edu.ucsf.rbvi.chemViz2.internal.model.HTMLObject;
import edu.ucsf.rbvi.chemViz2.internal.model.TableUtils;
import edu.ucsf.rbvi.chemViz2.internal.ui.ChemInfoTableModel;
import edu.ucsf.rbvi.chemViz2.internal.ui.CompoundColumn;
import edu.ucsf.rbvi.chemViz2.internal.ui.CompoundColumn.ColumnType;
import edu.ucsf.rbvi.chemViz2.internal.ui.TableMouseAdapter;

import edu.ucsf.rbvi.chemViz2.internal.ui.renderers.CompoundRenderer;
import edu.ucsf.rbvi.chemViz2.internal.ui.renderers.HTMLRenderer;
import edu.ucsf.rbvi.chemViz2.internal.ui.renderers.StringRenderer;

import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.smarts.SMARTSQueryTool;

public class CompoundTable extends JDialog implements ListSelectionListener,
                                                      RowsSetListener,
                                                      ActionListener {
	
	private Map<CyIdentifiable,List<Integer>> rowMap;
	private ChemInfoTableModel tableModel;
	private	TableColumnModel columnModel;
	private	ListSelectionModel selectionModel;
	private	JTable table;
	private	TableRowSorter sorter;
	private	JTableHeader tableHeader;
	private CyNetwork network;
	private	boolean modifyingSelection = false;
	private CompoundTable thisDialog;
	private ChemInfoSettings settings;
	private static Logger logger = LoggerFactory.getLogger(CompoundTable.class);

	private	List<CompoundColumn> columns;
	private	List<Compound> compoundList;

	public CompoundTable (CyNetwork network, List<Compound> compoundList, 
	                      List<String> columnList, ChemInfoSettings settings) {
		super();
		this.network = network;
		this.settings = settings;
		this.compoundList = compoundList;
		setTitle("2D Structure Table");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.rowMap = new HashMap<CyIdentifiable, List<Integer>>();

		CyIdentifiable source = compoundList.get(0).getSource();
		CyRow attributes = network.getRow(source);
		DescriptorManager dManager = settings.getDescriptorManager();

		if (columnList != null && columnList.size() > 0) {
			columns = new ArrayList<CompoundColumn>();
			for (String s: columnList) {
				String[] tokens = s.trim().split("[:;]");
				if (tokens.length != 3 && tokens.length != 2)
					throw new RuntimeException("Illegal column specification: "+s);
				if (tokens[0].equalsIgnoreCase("descriptor") || tokens[0].equalsIgnoreCase("desc")) {
					Descriptor type = dManager.getDescriptor(tokens[1]);
					int columnWidth = -1;
					if (tokens.length == 3) columnWidth = Integer.parseInt(tokens[2]);
					columns.add(new CompoundColumn(type, columnWidth));
				} else if (tokens[0].equalsIgnoreCase("attribute") || tokens[0].equalsIgnoreCase("attr")) {
					String attribute = tokens[1];
					int columnWidth = -1;
					if (tokens.length == 3) columnWidth = Integer.parseInt(tokens[2]);

					if (attribute.equals("ID"))
						columns.add(new CompoundColumn("ID", network, String.class, columnWidth));
					else {
						Class type = TableUtils.getColumnType(network, source, attribute);
						columns.add(new CompoundColumn(attribute, network, type, columnWidth));
					}
				}
			}
		} else {
			// See if we have any table attributes stored
			columns = TableAttributeHandler.getAttributes(network, dManager);
		}

		// Create the table
		initTable();

		pack();

		// Now, see if we need to adjust the width and height
		int height = TableAttributeHandler.getHeightAttribute(network);
		int width = TableAttributeHandler.getWidthAttribute(network);
		if (height != -1 && width != -1) {
			this.setSize(width, height);
		}

		thisDialog = this;
		setVisible(true);

		// Finally, add ourselves to listen for table row changes so that we
		// can know when selections happen
		settings.getServiceRegistrar().registerService(this, RowsSetListener.class, new Properties());
	}

	public void setCompounds(List<Compound> newList) {
		this.rowMap = new HashMap();
		tableModel.setCompoundList(newList);
		tableModel.fireTableDataChanged();
	}

	private void initTable() {
		JPanel mainPanel = new JPanel(new BorderLayout());

		// create our table model
		tableModel = new ChemInfoTableModel(network, compoundList, settings);
		table = new JTable(tableModel);
		sorter = new TableRowSorter<TableModel>(tableModel);
		table.setRowSorter(sorter);

		// Create our default columns
		int column = 0;
		for (CompoundColumn c: columns) {
			tableModel.addColumn(column++, c);
		}

		MouseAdapter mouseAdapter = new TableMouseAdapter(table, tableModel, sorter);
		tableHeader = table.getTableHeader();
		tableHeader.addMouseListener(mouseAdapter);
		TableCellRenderer renderer = tableHeader.getDefaultRenderer();
		((DefaultTableCellRenderer)renderer).setHorizontalAlignment(SwingConstants.CENTER);
		// sorter.setTableHeader(tableHeader);

		table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		table.setDefaultRenderer(Compound.class, new CompoundRenderer(sorter, rowMap));
		table.setDefaultRenderer(String.class, new StringRenderer());
		table.setDefaultRenderer(HTMLObject.class, new HTMLRenderer());

		// Figure out all of our default column widths
		columnModel = table.getColumnModel();
		int rowHeight = TableAttributeHandler.DEFAULT_IMAGE_SIZE;
		column = 0;

		for (CompoundColumn c: columns) {
			columnModel.getColumn(column++).setPreferredWidth(c.getWidth());
			// See if we've got an image -- if so, use it to set the default row height
			if (c.getColumnType() == ColumnType.DESCRIPTOR && c.getDescriptor().getClassType() == Compound.class) {
				rowHeight = c.getWidth();
			}
		}

		table.setRowHeight(rowHeight);

		// Add our mouse listener (specific for 2D image popup)
		// table.addMouseListener(mouseAdapter);

		// Add our row selection listener
		// selectionModel = table.getSelectionModel();
		// selectionModel.addListSelectionListener(this);

		JScrollPane pane = new JScrollPane(table);
		pane.setPreferredSize(new Dimension(500+TableAttributeHandler.DEFAULT_IMAGE_SIZE+20,520));
		mainPanel.add(pane, BorderLayout.CENTER);

		// Now add our button-box
		JPanel buttonBox = new JPanel();
		buttonBox.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

		{
			JButton searchButton = new JButton("Search Table using SMARTS...");
			searchButton.addActionListener(this);
			searchButton.setActionCommand("search");
			// exportButton.setEnabled(false);
			buttonBox.add(searchButton);
		}
		{
			JButton exportButton = new JButton("Export Table...");
			exportButton.addActionListener(this);
			exportButton.setActionCommand("export");
			// exportButton.setEnabled(false);
			buttonBox.add(exportButton);
		}
		{
			JButton printButton = new JButton("Print Table...");
			printButton.addActionListener(this);
			printButton.setActionCommand("print");
			// printButton.setEnabled(false);
			buttonBox.add(printButton);
		}
		{
			JButton closeButton = new JButton("Close");
			closeButton.addActionListener(this);
			closeButton.setActionCommand("close");
			buttonBox.add(closeButton);
		}
		mainPanel.add(buttonBox, BorderLayout.SOUTH);
		add(mainPanel);
	}


	/**
 	 * valueChanged is called when a user changes the selection in the table.
 	 *
 	 * @param e the ListSelectionEvent that tells us what was done.
 	 */
	public void valueChanged(ListSelectionEvent e) {
		if (modifyingSelection) return;
		if (e.getSource() == table.getSelectionModel()) {
			modifyingSelection = true;
			int[] rows = table.getSelectedRows();
			unSelectAll(network.getDefaultNodeTable());
			unSelectAll(network.getDefaultEdgeTable());
			for (int i = 0; i < rows.length; i++) {
				Compound c = compoundList.get(sorter.convertRowIndexToModel(rows[i]));
				CyIdentifiable obj = c.getSource();
				network.getRow(obj).set(CyNetwork.SELECTED, true);
			}
			modifyingSelection = false;
		}
	}

	/**
	 * onSelectEvent is called when a user changes the selection
	 * in the network.
	 *
	 * @param event the network selection event
	 */
	public void handleEvent(RowsSetEvent event) {
		if (modifyingSelection) return;

		if (!event.containsColumn(CyNetwork.SELECTED))
			return;

		modifyingSelection = true;
		selectionModel.clearSelection();
		selectObjects(CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, Boolean.TRUE));
		selectObjects(CyTableUtil.getEdgesInState(network, CyNetwork.SELECTED, Boolean.TRUE));
		modifyingSelection = false;
	}

	private void selectObjects(List<? extends CyIdentifiable> selectedObjects) {
		for (CyIdentifiable obj: selectedObjects) {
			if (rowMap.containsKey(obj)) {
				for (Integer r: rowMap.get(obj)) {
					int row = sorter.convertRowIndexToView(r.intValue());
					selectionModel.addSelectionInterval(row,row);
				}
			}
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("close")) {
			TableAttributeHandler.setTableAttributes(table, tableModel, network);
			TableAttributeHandler.setSizeAttributes(this, network);
			dispose();
		} else if (e.getActionCommand().equals("export")) {
			// Get the file name
			JFileChooser chooser = new JFileChooser();
			FileNameExtensionFilter filter = new FileNameExtensionFilter(
			        "Text file formats", "txt", "tsv");
			chooser.setFileFilter(filter);
			chooser.setDialogTitle("Export Table to File");
			int returnVal = chooser.showSaveDialog(this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				// Open the file
				File exportFile = chooser.getSelectedFile();
				try {
					// Output the table
					outputTable(exportFile);
				} catch (IOException ioe) {
					logger.error("Unable to export file: "+ioe.getMessage());
				}
			}
		} else if (e.getActionCommand().equals("print")) {
			try {
				((JTable)table).print();
			} catch (Exception ePrint) {
				logger.error("Unable to print table: "+ePrint.getMessage(), ePrint);
			}
		} else if (e.getActionCommand().equals("search")) {
			String smartsQuery = JOptionPane.showInputDialog(this, "", "Enter SMARTS query string", JOptionPane.PLAIN_MESSAGE);
			if (smartsQuery == null || smartsQuery.length() < 2) return;

			List<Compound> matches = new ArrayList<Compound>();
			try {
				SMARTSQueryTool queryTool = 
					new SMARTSQueryTool(smartsQuery, SilentChemObjectBuilder.getInstance());
				for (Compound compound: compoundList) {
					boolean status = queryTool.matches(compound.getMolecule());
					if (status && queryTool.countMatches() > 0)
						matches.add(compound);
				}
			} catch (Exception cdkException) {
				JOptionPane.showConfirmDialog(this, "CDK Exception: "+cdkException.getMessage(), "CDK Error", 
				                              JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE);
				return;
			}

			// Clear our current selection
			table.clearSelection();

			for (Compound compound: matches) {
				// get the row number
				int rowNumber = compoundList.indexOf(compound);
				// Convert to view row
				int viewRow = sorter.convertRowIndexToView(rowNumber);
				// Select it
				table.addRowSelectionInterval(viewRow,viewRow);
			}
		}
	}

	private void outputTable(File file) throws IOException {
		FileWriter writer = new FileWriter(file);
		for (int viewRow = 0; viewRow < compoundList.size(); viewRow++ ) {
			int row = sorter.convertRowIndexToModel(viewRow);
			Compound cmpd = compoundList.get(row);
			for (int viewCol = 0; viewCol < columns.size(); viewCol++) {
				int col = table.convertColumnIndexToModel(viewCol);
				CompoundColumn c = columns.get(col);

				// warning -- if the image column is the first column, then we'll get a
				// leading tab
				if (c.getColumnType() != ColumnType.DESCRIPTOR || 
				    c.getDescriptor().getClassType() != Compound.class) {
					if (viewCol > 0)
						writer.write("\t");
					c.output(writer, cmpd);
				}
			}
			writer.write("\n");
		}
		writer.close();
	}

	private void unSelectAll(CyTable table) {
		Collection<CyRow> rows = table.getMatchingRows(CyNetwork.SELECTED, Boolean.TRUE);
		for (CyRow row: rows) {
			row.set(CyNetwork.SELECTED, Boolean.FALSE);
		}
	}
}
