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
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.chemViz2.internal.model.ChemInfoSettings;
import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Compound.AttriType;
import edu.ucsf.rbvi.chemViz2.internal.model.TableUtils;
import edu.ucsf.rbvi.chemViz2.internal.tasks.ChemVizAbstractTaskFactory.Scope;


/**
 * This class is the common base class for the two compound loader tasks CreatePopupTask
 * and CreateCompoundTableTask
 */
abstract public class AbstractCompoundTask extends AbstractTask {
	boolean canceled = false;
	int maxCompounds = 0;
	int compoundCount = 0;
	List<Compound> threadResultsList = null;
	TaskMonitor monitor = null;
	ChemInfoSettings settings = null;

	// These are used for our progress meeter
	int totalObjects = 0;
	int	objectCount = 0;

	protected AbstractCompoundTask(ChemInfoSettings settings) {
		this.settings = settings;
	}

	/**
 	 * Returns all of the Compounds for a list of graph objects (Nodes or Edges) based on the SMILES
 	 * and InChI attributes.
 	 *
 	 * @param goSet the Collection of graph objects we're looking at
 	 * @param sList the list of attributes that contain SMILES strings
 	 * @param iList the list of attributes that contain InChI strings
 	 * @return the list of compounds.  If the compounds have not already been created, they are created
 	 *         as a byproduct of this method.
 	 */
	protected List<Compound> getCompounds(Collection<? extends CyIdentifiable> goSet, CyNetwork network,
	                                      List<String> sList, List<String> iList, int maxThreads) {
		List<GetCompoundTask> threadList = null;
		long startTime = Calendar.getInstance().getTimeInMillis();

		if (maxThreads != 1)
			threadList = new ArrayList<GetCompoundTask>();

		List<Compound> cList = new ArrayList<Compound>();
		for (CyIdentifiable go: goSet) {
			if (done()) break;
			if (maxThreads == 1)
				updateMonitor();
			List<Compound> compounds4id = getCompounds(go, network, sList, iList, threadList);
			if (compounds4id != null && compounds4id.size() > 0)
				cList.addAll(compounds4id);
		}

		if (threadList != null && threadList.size() > 0)
			cList.addAll(GetCompoundTask.runThreads(maxThreads, threadList));

		long endTime = Calendar.getInstance().getTimeInMillis();
		// System.out.println("getCompounds took: "+(endTime-startTime)+"ms total");
		// System.out.println(" createStructure took: "+Compound.totalTime+"ms");
		// System.out.println("  getFingerprint took: "+Compound.totalFPTime+"ms");
		// System.out.println("  SMILES parsing took: "+Compound.totalSMILESTime+"ms");
		// System.out.println("  creating the fingerprint took: "+Compound.totalGetFPTime+"ms");

		return cList;
	}

	/**
 	 * Returns all of the Compounds for a single graph object (Node or Edge) based on the SMILES
 	 * and InChI attributes.
 	 *
 	 * @param go the graph object we're looking at
 	 * @param sList the list of attributes that contain SMILES strings
 	 * @param iList the list of attributes that contain InChI strings
 	 * @return the list of compounds.  If the compounds have not already been created, they are created
 	 *         as a byproduct of this method.
 	 */
	protected List<Compound> getCompounds(CyIdentifiable go, CyNetwork network,
	                                      List<String> sList, List<String> iList, 
	                                      List<GetCompoundTask> threadList) {
		if ((sList == null || sList.size() == 0) 
		    && (iList == null || iList.size() == 0))
			return null;
		
		List<Compound> cList = new ArrayList<>();

		// Get the compound list from each attribute, but we want to give preference
		// to SMILES.  Only if we don't have SMILES do we want to add InChI attributes
		boolean foundSMILES = false;
		for (String attr: sList) {
			if (done()) break;
			List<Compound> getResults = getCompounds(go, network, attr, AttriType.smiles, threadList);
			if (getResults != null) {
				cList.addAll(getResults);
				foundSMILES = true;
			}
		}
		if (cList.size() > 0 || foundSMILES) return cList;

		for (String attr: iList) {
			if (done()) break;
			List<Compound> getResults = getCompounds(go, network, attr, AttriType.inchi, threadList);
			if (getResults != null)
				cList.addAll(getResults);
		}

		return cList;
	}

	/**
 	 * Returns all of the Compounds for a single graph object (Node or Edge) based on the designated
 	 * attribute of the specific type
 	 *
 	 * @param go the graph object we're looking at
 	 * @param attr the attribute that contains the compound descriptor
 	 * @param type the type of the attribute (smiles or inchi)
 	 * @return the list of compounds.  If the compounds have not already been created, they are created
 	 *         as a byproduct of this method.
 	 */
	protected List<Compound> getCompounds(CyIdentifiable go, CyNetwork network,
	                                      String attr, AttriType type,
	                                      List<GetCompoundTask> threadList) {
		List<Compound> cList = new ArrayList();
		Class attrType = TableUtils.getColumnType(network, go, attr);
			
		if (attrType == String.class) {
			String cstring = TableUtils.getAttribute(network, go, attr, String.class);
			if (cstring == null || cstring.length() == 0) return null;
			cList.addAll(getCompounds(go, network, attr, cstring, type, threadList));
		} else if (attrType == List.class) {
			List<String> stringList = TableUtils.getListAttribute(network, go, attr, String.class);
			if (stringList == null || stringList.size() == 0) return null;
			for (String cstring: stringList) {
				if (cstring != null && cstring.length() > 0)
					cList.addAll(getCompounds(go, network, attr, cstring, type, threadList));
				if (done()) break;
			}
		}
		return cList;
	}

	protected List<Compound> getCompounds(CyIdentifiable go, CyNetwork network, String attr, 
	                                      String compoundString, AttriType type,
	                                      List<GetCompoundTask> threadList) {
		List<Compound> cList = new ArrayList();

		String[] cstrings = null;

		if (type == AttriType.smiles) {
			cstrings = compoundString.split(",");
		} else {
			cstrings = new String[1];
			cstrings[0] = compoundString;
		}

		for (int i = 0; i < cstrings.length; i++) {
			Compound c = settings.getCompoundManager().getCompound(go, network, attr, cstrings[i], type);
			if (c == null) {
				if (threadList != null) {
					threadList.add(new GetCompoundTask(settings, go, network, attr, cstrings[i], type));
					continue;
				} 

				c = new Compound(settings, go, network, attr, cstrings[i], type);
			} 

			cList.add(c);
			compoundCount++;
			if (done())
				return cList;
		}

		return cList;
	}

	protected List<CyIdentifiable> getObjectList(CyNetwork network, CyIdentifiable context, Scope scope,
	                                             List<CyNode> nodeList, List<CyEdge> edgeList) {
		if (network == null) return null;
		List<CyIdentifiable> objectList = new ArrayList<CyIdentifiable>();
		if (nodeList != null && nodeList.size() > 0) {
			objectList.addAll(nodeList);
			return objectList;
		} else if (edgeList != null && edgeList.size() > 0) {
			objectList.addAll(edgeList);
			return objectList;
		}

		switch (scope) {
		case ALLNODES:
			objectList.addAll(network.getNodeList());
			break;

		case ALLEDGES:
			objectList.addAll(network.getEdgeList());
			break;

		case SELECTEDNODES:
			List<CyNode> selectedNodes = CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true);
			if (selectedNodes == null || selectedNodes.size() == 0 && context != null)
				objectList.add(context);
			else if (selectedNodes != null && selectedNodes.size() > 0)
				objectList.addAll(selectedNodes);
			break;

		case SELECTEDEDGES:
			List<CyEdge> selectedEdges = CyTableUtil.getEdgesInState(network, CyNetwork.SELECTED, true);
			if (selectedEdges == null || selectedEdges.size() == 0 && context != null)
				objectList.add(context);
			else if (selectedEdges != null && selectedEdges.size() > 0)
				objectList.addAll(selectedEdges);
			break;

		}
		return objectList;
	}

	protected void updateMonitor() {
		if (monitor == null || totalObjects == 0) return;
		monitor.setProgress((int)(((double)objectCount/(double)totalObjects) * 100.0));
		objectCount++;
	}

	protected void setStatus(String status) {
		if (monitor != null) monitor.setStatusMessage(status);
	}

	private boolean done() {
		if (canceled) return true;
		if ((maxCompounds != 0) && (compoundCount >= maxCompounds)) return true;
		return false;
	}

}
