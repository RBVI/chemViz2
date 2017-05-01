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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.JTable;
import javax.swing.table.TableRowSorter;

import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Descriptor;
import edu.ucsf.rbvi.chemViz2.internal.model.TableUtils;
import edu.ucsf.rbvi.chemViz2.internal.ui.ChemInfoTableModel;
import edu.ucsf.rbvi.chemViz2.internal.ui.CompoundColumn;
import edu.ucsf.rbvi.chemViz2.internal.ui.CompoundPopup;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableUtil;

class TableMouseAdapter extends MouseAdapter implements ActionListener {
	private	TableRowSorter sorter;
	private JTable table;
	private ChemInfoTableModel tableModel;
	private CyNetwork network;

	TableMouseAdapter(JTable table, ChemInfoTableModel tableModel, TableRowSorter sorter) {
		this.table = table;
		this.sorter = sorter;
		this.tableModel = tableModel;
		this.network = tableModel.getNetwork();
	}

	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() == 2 && e.getComponent() == table)
		{
			Point p = e.getPoint();
			// int row = table.convertRowIndexToModel(table.rowAtPoint(p));
			int row = sorter.convertRowIndexToModel(table.rowAtPoint(p));
			int column = table.convertColumnIndexToModel(table.columnAtPoint(p));
			if (tableModel.getColumnClass(column) == Compound.class) {
				final Compound c = (Compound)tableModel.getValueAt(row, column);
				Runnable t = new Runnable() {
  				public void run() {
   	 				CompoundPopup popup = new CompoundPopup(tableModel.getNetwork(), 
						                                        Collections.singletonList(c), 
						                                        Collections.singletonList(c.getSource()), 
						                                        null, null);
						popup.toFront();
					}
				};
				new Thread(t).start();
			}
		} else if (e.getComponent() == table.getTableHeader() && 
		           ((e.getButton() == MouseEvent.BUTTON3) ||
		            (e.getButton() == MouseEvent.BUTTON1 && e.isMetaDown()) ||
		            (e.getButton() == MouseEvent.BUTTON1 && e.isControlDown()))) {
			// Popup header context menu
			JPopupMenu headerMenu = new JPopupMenu();
			// Get our column title
			Point p = e.getPoint();
			int column = table.convertColumnIndexToModel(table.columnAtPoint(p));
			String name = tableModel.getColumnName(column);
			// Add removeMenu if we have more than 1 column
			if (tableModel.getColumnCount() > 1) {
				JMenuItem removeMenu = new JMenuItem("Remove Column "+name);
				removeMenu.setActionCommand("removeColumn:"+column);
				removeMenu.addActionListener(this);
				headerMenu.add(removeMenu);
			}
			JMenu addMenu = new JMenu("Add New Column");
			JMenu attrMenu = new JMenu("Cytoscape attributes");
			if (tableModel.hasNodes()) {
				addAttributeMenus(attrMenu, network.getDefaultNodeTable(), "node.", column);
			}
			if (tableModel.hasEdges()) {
				addAttributeMenus(attrMenu, network.getDefaultEdgeTable(), "edge.", column);
			}
			if (attrMenu.getItemCount() > 0) 
				addMenu.add(attrMenu);

			JMenu descMenu = new JMenu("Molecular descriptors");
			addDescriptorMenus(descMenu, column);
			if (descMenu.getItemCount() > 0) 
				addMenu.add(descMenu);

			headerMenu.add(addMenu);
			headerMenu.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().startsWith("removeColumn:")) {
			String columnNumber = e.getActionCommand().substring(13);
			int column = Integer.parseInt(columnNumber);
			tableModel.removeColumn(column);
		} else if (e.getActionCommand().startsWith("addColumn:")) {
			String columnNumber = e.getActionCommand().substring(10);
			int column = Integer.parseInt(columnNumber);
		}
	}

	void addAttributeMenus(JMenu addMenu, CyTable attributes, String type, int column) {
		List<String> attNames = new ArrayList<String>(CyTableUtil.getColumnNames(attributes));
		Collections.sort(attNames);
		for (String att: attNames) {
			if (tableModel.findColumn(att) < 0) {
				addMenu.add(new AddMenu(att, type, column, TableUtils.getColumnType(attributes, att)));
			}
		}
	}

	void addDescriptorMenus(JMenu addMenu, int column) {
		List<Descriptor> descList = tableModel.getSettings().getDescriptorManager().getDescriptorList(true);
		for (Descriptor type: descList) {
			if (tableModel.findColumn(type.toString()) < 0) {
				addMenu.add(new AddMenu(type, column));
			}
		}
	}

	class AddMenu extends JMenuItem implements ActionListener {
		int column;
		CompoundColumn newColumn;
		Descriptor descType;
		
		AddMenu(String name, int column) {
			this(name, "", column, String.class);
		}
	
		AddMenu(String name, String prefix, int column, Class type) {
			super(prefix+name);
			this.newColumn = new CompoundColumn(name, network, type, -1);
			this.column = column;
			this.descType = null;
			addActionListener(this);
		}
	
		AddMenu(Descriptor descriptor, int column) {
			super(descriptor.toString());
			this.descType = descriptor;
			if (descriptor.getClassType() != Map.class)
				this.newColumn = new CompoundColumn(descriptor, -1);
			this.column = column;
			addActionListener(this);
		}
	
		public void actionPerformed(ActionEvent e) {
			if (newColumn != null)
				tableModel.addColumn(column, newColumn);
			else if (descType.getClassType() == Map.class) {
				List<String> descList = descType.getDescriptorList();
				for (String desc: descList) {
					// Only add the column if we don't already have it in the table...
					Descriptor descriptor = tableModel.getSettings().getDescriptorManager().getDescriptor(desc);
					if (descriptor != null && !tableModel.containsColumn(descriptor.toString()))
						tableModel.addColumn(column, new CompoundColumn(descriptor, -1));
				}
			}
		}
	}
}
