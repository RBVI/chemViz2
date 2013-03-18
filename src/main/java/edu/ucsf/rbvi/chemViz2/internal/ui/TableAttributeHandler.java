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

import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;

import edu.ucsf.rbvi.chemViz2.internal.ui.ChemInfoTableModel;
import edu.ucsf.rbvi.chemViz2.internal.model.Descriptor;
import edu.ucsf.rbvi.chemViz2.internal.model.DescriptorManager;
import edu.ucsf.rbvi.chemViz2.internal.ui.CompoundColumn;

public class TableAttributeHandler {

	private JTable table;
	private ChemInfoTableModel model;
	private CyNetwork network;
	public static int DEFAULT_IMAGE_SIZE=80;
	private static String tableAttribute = "_ChemVizTable";
	private static String tableWidthAttribute = "_ChemVizTableWidth";
	private static String tableHeightAttribute = "_ChemVizTableHeight";
	private static Logger logger = LoggerFactory.getLogger(TableAttributeHandler.class);

	private static String[] defaultColumns = {"ID", "attribute", "molstring", "weight", "alogp", 
	                                          "acceptors", "donors", "image"};

	public static List<CompoundColumn> getAttributes(CyNetwork network, DescriptorManager manager) {
		List<CompoundColumn> columns = new ArrayList();
		CyRow networkAttribute = network.getRow(network, CyNetwork.HIDDEN_ATTRS);

		if (networkAttribute.isSet(tableAttribute)) {
			// Get the attributes
			List<String> columnAttributes = networkAttribute.getList(tableAttribute, String.class);
			// Now iterate over the attribute list and create the columns
			for (String attr: columnAttributes) {
				try {
					columns.add(new CompoundColumn(attr, manager));
				} catch (RuntimeException e) {
					logger.warn(e.getMessage(), e);
				}
			}
		} else {
			// No, create the default table map
			for (String desc: defaultColumns) {
				Descriptor d = manager.getDescriptor(desc);
				if (d == null) continue; // Shouldn't happen

				if (desc.equals("image"))
					columns.add(new CompoundColumn(d, DEFAULT_IMAGE_SIZE));
				else
					columns.add(new CompoundColumn(d, -1));
			}
		}
		return columns;
	}

	public static void setTableAttributes(JTable table, ChemInfoTableModel model, CyNetwork network) {
		CyRow networkAttribute = network.getRow(network, CyNetwork.HIDDEN_ATTRS);

		TableColumnModel columnModel = table.getColumnModel();
		List<String> columnAttributes = new ArrayList();
		for (int column = 0; column < columnModel.getColumnCount(); column++) {
			TableColumn c = columnModel.getColumn(column);
			CompoundColumn cc = model.getColumnAt(column);
			// Update our width first
			cc.setWidth(c.getWidth());
			columnAttributes.add(cc.toString());
		}

		try {
			networkAttribute.getTable().createListColumn(tableAttribute, String.class, false);
		} catch (IllegalArgumentException e) {
			// Column already exists -- all good
		}

		networkAttribute.set(tableAttribute, columnAttributes);
	}

	public static void setSizeAttributes(JDialog dialog, CyNetwork network) {
		CyRow networkAttribute = network.getRow(network, CyNetwork.HIDDEN_ATTRS);

		int height = dialog.getHeight();
		int width = dialog.getWidth();
		try {
			networkAttribute.getTable().createColumn(tableWidthAttribute, Integer.class, false);
			networkAttribute.getTable().createColumn(tableHeightAttribute, Integer.class, false);
		} catch (IllegalArgumentException e) {
			// Columns already exist -- all good
		}

		networkAttribute.set(tableWidthAttribute, width);
		networkAttribute.set(tableHeightAttribute, height);
	}

	public static int getWidthAttribute(CyNetwork network) {
		CyRow networkAttribute = network.getRow(network, CyNetwork.HIDDEN_ATTRS);
		if (networkAttribute.isSet(tableWidthAttribute))
			return networkAttribute.get(tableWidthAttribute, Integer.class);
		return -1;
	}

	public static int getHeightAttribute(CyNetwork network) {
		CyRow networkAttribute = network.getRow(network, CyNetwork.HIDDEN_ATTRS);
		if (networkAttribute.isSet(tableHeightAttribute))
			return networkAttribute.get(tableHeightAttribute, Integer.class);
		return -1;
	}
}
