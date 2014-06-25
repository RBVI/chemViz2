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

package edu.ucsf.rbvi.chemViz2.internal.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cytoscape.command.util.EdgeList;
import org.cytoscape.command.util.NodeList;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListMultipleSelection;

import edu.ucsf.rbvi.chemViz2.internal.model.ChemInfoSettings;
import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Compound.AttriType;
import edu.ucsf.rbvi.chemViz2.internal.model.Descriptor;
import edu.ucsf.rbvi.chemViz2.internal.model.DescriptorManager;
import edu.ucsf.rbvi.chemViz2.internal.tasks.ChemVizAbstractTaskFactory.Scope;
import edu.ucsf.rbvi.chemViz2.internal.ui.CompoundTable;

/**
 * The CompoundtableTask fetches all of the compounds defined by the
 * object passed in its constructor and then creates a popup Dialog that provides
 * a 2D image of all of the compuonds defined.
 */
public class CreateAttributesTask extends AbstractCompoundTask {
	CyIdentifiable context;
	CyNetwork argNetwork;
	Scope scope;
	DescriptorManager manager = null;
	String title = null;

	@Tunable(description="Network to operate on", context="nogui")
	public CyNetwork network;

	NodeList nodeList = new NodeList(null);
	@Tunable(description="The list of nodes to create the attributes for", context="nogui")
	public NodeList getnodeList() {
		if (network == null)
			network = settings.getCurrentNetwork();
		nodeList.setNetwork(network);
		return nodeList;
	}
	public void setnodeList(NodeList list) {};

	EdgeList edgeList = new EdgeList(null);
	@Tunable(description="The list of edges to create the attributes for", context="nogui")
	public EdgeList getedgeList() {
		if (network == null)
			network = settings.getCurrentNetwork();
		edgeList.setNetwork(network);
		return edgeList;
	}
	public void setedgeList(EdgeList list) {};


	@Tunable(description="Choose Descriptors to Create Attributes")
	public ListMultipleSelection<Descriptor> descriptors = null;

	/**
 	 * Creates the task.
 	 *
 	 * @param view the view object that we're creating the popup for
 	 * @param dialog the settings dialog, which we use to pull the attribute names that contain the compound descriptors
 	 */
  public CreateAttributesTask(CyNetwork network, CyIdentifiable context, Scope scope, 
	                            ChemInfoSettings settings) {
		super(settings);
		this.scope = scope;
		this.argNetwork = network;
		this.context = context;
		this.manager = settings.getDescriptorManager();

		descriptors = new ListMultipleSelection<Descriptor>(manager.getDescriptorList(false));
	}

	public void setDialogTitle(String title) {
		this.title = title;
	}

	/**
 	 * Runs the task -- this will get all of the compounds, fetching the images (if necessary) and creates the popup.
 	 */
	public void run(TaskMonitor taskMonitor) {
		if (network == null && argNetwork == null)
			network = settings.getCurrentNetwork();
		else if (network == null)
			network = argNetwork;

		List<CyIdentifiable> objectList = getObjectList(network, context, scope,
		                                                nodeList.getValue(), edgeList.getValue());
		String type = "node";
		if (objectList.get(0) instanceof CyEdge)
			type = "edge";

		List<Descriptor> descList = descriptors.getSelectedValues();
		if (descList == null || descList.size() == 0) return;

		List<Compound> compoundList = getCompounds(objectList, network,
  	                                           settings.getCompoundAttributes(type,AttriType.smiles),
  	                                           settings.getCompoundAttributes(type,AttriType.inchi),
		                                           settings.getMaxThreads());

		Map<CyIdentifiable, List<Compound>> idMap = new HashMap<CyIdentifiable, List<Compound>>();
		for (Compound cmpnd: compoundList) {
			CyIdentifiable source = cmpnd.getSource();
			if (!idMap.containsKey(source)) {
				idMap.put(source, new ArrayList<Compound>());
			}
			List<Compound> list = idMap.get(source);
			list.add(cmpnd);
		}

		if (compoundList.size() > 0 && !canceled) {
			for (CyIdentifiable object: objectList) {
				CyRow row = network.getRow(object);
				List<Compound> objectCompoundList = idMap.get(object);
				if (objectCompoundList == null || objectCompoundList.size() == 0) continue;
				for (Descriptor desc: descList) {
					createAttribute(desc, row, objectCompoundList);
				}
			}
		}
	}

	private void createAttribute(Descriptor desc, CyRow row, List<Compound> compoundList) {
		if (desc == null) return;
		if (desc.getClassType() == Map.class) {
			List<String> descList = desc.getDescriptorList();
			for (String descName: descList ) {
				createAttribute(manager.getDescriptor(descName), row, compoundList);
			}
		} else {
			if(compoundList.size() > 1)
				createListAttribute(desc, row, compoundList);
			else
				createValueAttribute(desc, row, compoundList.get(0));
		}
	}

	private void createValueAttribute(Descriptor desc, CyRow row, Compound compound) {
		CyTable table = row.getTable();
		String columnName = desc.toString();
		Class descClass = desc.getClassType();
		CyColumn column = table.getColumn(columnName);
		if (column == null) {
			table.createColumn(columnName, descClass, false);
		} else {
			row.set(columnName, desc.getDescriptor(compound));
		}
	}

	private void createListAttribute(Descriptor desc, CyRow row, List<Compound> compoundList) {
		CyTable table = row.getTable();
		String columnName = desc.toString();
		Class descClass = desc.getClassType();
		CyColumn column = table.getColumn(columnName);
		if (column == null) {
			table.createListColumn(columnName, descClass, false);
		} else {
			// If we already have a column, and it's *not* a list, throw a warning
			// and return
			if (column.getType() != List.class) {
				return;
			}

			List descValues = new ArrayList();
			for (Compound compound: compoundList)
				descValues.add(desc.getDescriptor(compound));
			row.set(columnName, descValues);
		}
	}
}
