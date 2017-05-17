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
import java.awt.Desktop;
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

import java.net.URI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.table.TableRowSorter;

import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Descriptor;
import edu.ucsf.rbvi.chemViz2.internal.model.HTMLObject;
import edu.ucsf.rbvi.chemViz2.internal.model.TableUtils;
import edu.ucsf.rbvi.chemViz2.internal.ui.ChemInfoTableModel;
import edu.ucsf.rbvi.chemViz2.internal.ui.CompoundColumn;
import edu.ucsf.rbvi.chemViz2.internal.ui.CompoundPopup;
import edu.ucsf.rbvi.chemViz2.internal.ui.renderers.HTMLEditor;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableUtil;

class TableMouseAdapter extends MouseAdapter {
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

	@Override
	public void mouseMoved(MouseEvent e) {
		Point p = e.getPoint();
		int row = sorter.convertRowIndexToModel(table.rowAtPoint(p));
		int column = table.convertColumnIndexToModel(table.columnAtPoint(p));
		System.out.format("%d,%d\n",row,column);
		if (row < 0 || column < 0) return;

		System.out.format("%d,%d\n",row,column);

		// We do this so our hyperlinks work without first selecting
		// the row
		if (tableModel.getColumnClass(column).equals(HTMLObject.class)) {
			System.out.println("Requesting focus");
			((HTMLEditor)table.getCellEditor(row, column)).requestFocus();
		}
	}

	@Override
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
		}
	}

}
