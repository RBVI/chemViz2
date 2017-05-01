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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;

import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Descriptor;
import edu.ucsf.rbvi.chemViz2.internal.model.DescriptorManager;
import edu.ucsf.rbvi.chemViz2.internal.model.TableUtils;

public class CompoundColumn {

	public enum ColumnType { 
		ATTRIBUTE,  // A simple Cytoscape attribute
		DESCRIPTOR // A chemical descriptor
	};

	private ColumnType columnType;
	private String attributeName;
	private Class attributeType;
	private Descriptor descriptor;
	private CyNetwork network;
	private int columnWidth;
	private List<Compound> compoundList;

	public CompoundColumn(Descriptor descriptor, int width) {
		this.columnType = ColumnType.DESCRIPTOR;
		this.descriptor = descriptor;
		this.columnWidth = width;
	}

	public CompoundColumn(String attributeName, CyNetwork network, Class type, int width) {
		this.columnType = ColumnType.ATTRIBUTE;
		this.attributeName = attributeName;
		this.attributeType = type;
		this.columnWidth = width;
		this.network = network;
	}

	public CompoundColumn(String attributeString, DescriptorManager dManager) throws RuntimeException {
		String [] words = attributeString.split("[:,]");
		if (words[0].equals("DESCRIPTOR")) {
			this.columnType = ColumnType.DESCRIPTOR;
			this.descriptor = null;
			if (words.length != 3)
				throw new RuntimeException("Illegal column specification: "+attributeString);
			List<Descriptor> descriptorList = dManager.getDescriptorList(true);
			for (Descriptor type: descriptorList) {
				if (words[1].equals(type.toString())) {
					this.descriptor = type;
					break;
				}
			}
			if (this.descriptor == null) 
				throw new RuntimeException("Unknown descriptor: "+words[1]);
			columnWidth = Integer.parseInt(words[2]);
		} else if (words[0].equals("ATTRIBUTE")) {
			if (words.length != 5)
				throw new RuntimeException("Illegal column specification: "+attributeString);
			columnType = ColumnType.ATTRIBUTE;
			attributeName = words[1];
			try {
				attributeType = Class.forName(words[3]);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException("Can't find class '"+words[3]+"': "+e.getMessage());
			}
			columnWidth = Integer.parseInt(words[4]);
		} else {
			throw new RuntimeException("Illegal column specification: "+attributeString);
		}
	}

	public int getWidth() { 
		if (columnWidth == -1)
			return 100;
		return columnWidth; 
	}

	public void setWidth(int width) {
		columnWidth = width;
	}

	public String toString() {
		if (columnType == ColumnType.DESCRIPTOR) {
			return "DESCRIPTOR:"+descriptor.toString()+","+columnWidth;
		} else {
			return "ATTRIBUTE:"+attributeName+","+attributeType.getName()+","+columnWidth;
		}
	}

	public Object getValue(Compound cmpd) {
		// Get the row so we can note whether we have nodes
		if (columnType == ColumnType.ATTRIBUTE) {
			CyIdentifiable obj = cmpd.getSource();
			CyNetwork network = cmpd.getNetwork();
			CyRow attributes = network.getRow(obj);
			CyColumn column = attributes.getTable().getColumn(attributeName);

			// Special case for "ID"
			if (attributeName.equals("ID"))
				return TableUtils.getName(network, obj);

			if (attributeType == List.class) {
				// Check to see if our compound attribute is a list
				// and if this attribute is also a list.  If they
				// are the same length, just reply with the value
				// corresponding to the offset.
				Class<?> listType = column.getListElementType();
				List<?> list = attributes.getList(attributeName, listType);
				if (list == null || list.size() <= 1) return list;

				String compoundAttribute = cmpd.getAttribute();
				Class<?> compoundType = attributes.getTable().getColumn(compoundAttribute).getType();
				if (compoundType.equals(List.class)) {
					// OK, we are a list > 1 and our compound attribute is a list.  See if the
					// lengths match
					List<String> compoundList = attributes.getList(compoundAttribute, String.class);
					if (compoundList.size() == list.size()) {
						// Sizes are equal, so now figure out which compound we have and
						// return the appropriate values
						int offset = 0;
						for (String molString: compoundList) {
							if (cmpd.getMoleculeString().equals(molString))
								return list.get(offset);
							offset++;
						}
					}
				}
				return attributes.getList(attributeName, listType);
			}

			return attributes.get(attributeName, attributeType);
		} else if (columnType == ColumnType.DESCRIPTOR) {
			// Hand it off
			return descriptor.getDescriptor(cmpd);
		}
		return null;
	}

	public Class getColumnClass() {
		if (columnType == ColumnType.DESCRIPTOR)
			return descriptor.getClassType();
		else
			return attributeType;
	}

	public String getColumnName() {
		if (columnType == ColumnType.DESCRIPTOR)
			return descriptor.toString();
		return attributeName;
	}

	public ColumnType getColumnType() { return columnType; }
	public Descriptor getDescriptor() { return descriptor; }

	public void output(FileWriter writer, Compound compound) throws IOException {
		Object obj = getValue(compound);
		if (obj != null) {
			// We don't handle the images, yet
			if (obj instanceof Compound) 
				writer.write("[2D Image]");
			else
				writer.write(obj.toString());
		}
		return;
	}
}
