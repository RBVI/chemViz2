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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.cytoscape.command.util.EdgeList;
import org.cytoscape.command.util.NodeList;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyEdge.Type;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Compound.AttriType;
import edu.ucsf.rbvi.chemViz2.internal.model.ChemInfoSettings;
import edu.ucsf.rbvi.chemViz2.internal.model.TableUtils;
import edu.ucsf.rbvi.chemViz2.internal.similarity.CDKTanimotoScore;

/**
 * The TanimotoScorerTask fetches all of the compounds defined by the
 * object passed in its constructor and then calculates the tanimoto distances
 * between each of them, storing the results in attributes or by creating edges
 * in a new network.
 */
public class TanimotoScorerTask extends AbstractCompoundTask {
	List<CyNode> objectList;
	CyNetwork argNetwork;
	CyNetworkViewFactory networkViewFactory;
	CyNetworkViewManager networkViewManager;
	CyNetworkManager networkManager;
	CyNetworkView origNetworkView;
	ChemInfoSettings settings;
	VisualMappingManager visualMappingManager;
	boolean canceled = false;
	static private Logger logger = LoggerFactory.getLogger(TanimotoScorerTask.class);

	@Tunable(description="Network to operate on", context="nogui")
	public CyNetwork network;

	NodeList nodeList = new NodeList(null);
	@Tunable(description="The list of nodes to use for the similarity calculation", context="nogui")
	public NodeList getnodeList() {
		if (network == null)
			network = settings.getCurrentNetwork();
		nodeList.setNetwork(network);
		return nodeList;
	}
	public void setnodeList(NodeList nl) {}

	@Tunable(description="Create a new network from the calculated edges", context="nogui")
	public boolean createNewNetwork = false;
	

	/**
 	 * Creates the task.
 	 *
 	 * @param selection the graph objects that we're comparing
 	 * @param dialog the settings dialog, which we use to pull the attribute names that contain the compound descriptors
 	 * @param newNetwork if 'true' create a new network
 	 */
  public TanimotoScorerTask(CyNetworkView networkView, List<CyNode> objectList, 
	                          CyNetworkViewFactory networkViewFactory, 
	                          CyNetworkManager networkManager, 
	                          CyNetworkViewManager networkViewManager, 
	                          VisualMappingManager visualManager,
	                          ChemInfoSettings settings, boolean newNetwork) {
		super(settings);
		if (networkView != null)
			this.argNetwork = networkView.getModel();
		else
			this.argNetwork = null;
		this.objectList = objectList;
		this.settings = settings;
		this.createNewNetwork = newNetwork;
		this.networkViewFactory = networkViewFactory;
		this.origNetworkView = networkView;
		this.visualMappingManager = visualManager;
		this.networkViewManager = networkViewManager;
		this.networkManager = networkManager;
	}

	public String getTitle() {
		return "Creating Scores Table";
	}

	/**
 	 * Runs the task -- this will get all of the compounds, and compute the tanimoto values
 	 */
	public void run(TaskMonitor taskMonitor) {
		if (argNetwork == null) return;
		CyNetwork newNetwork = null;

		if (network == null && argNetwork == null)
			network = settings.getCurrentNetwork();
		else if (network == null)
			network = argNetwork;

		if (objectList == null) {
			List<CyIdentifiable> objList = getObjectList(network, null, null, 
			                                             nodeList.getValue(), null);
			if (objList != null && objList.size() > 0) {
				objectList = new ArrayList<CyNode>(objList.size());
				for (CyIdentifiable id: objList) {
					if (id instanceof CyNode)
						objectList.add((CyNode)id);
				}
			}
		}

		if (objectList == null || objectList.size() == 0) {
			monitor.showMessage(TaskMonitor.Level.ERROR, "Nothing selected");
			return;
		}

		int maxThreads = settings.getMaxThreads();
		int nThreads = Runtime.getRuntime().availableProcessors()-1;
		if (maxThreads > 0)
			nThreads = maxThreads;

		List<Compound>compoundList = getCompounds(objectList, network,
                                              settings.getCompoundAttributes("node",AttriType.smiles),
                                              settings.getCompoundAttributes("node",AttriType.inchi), maxThreads);
		double tcCutoff = 0.25;
		if (settings != null)
			tcCutoff = settings.getTcCutoff();

		if (createNewNetwork) {
			CyRootNetwork rootNetwork = ((CySubNetwork)network).getRootNetwork();
			String name = network.getRow(network).get(CyNetwork.NAME, String.class);
			
			newNetwork = (CyNetwork)rootNetwork.addSubNetwork(objectList, null);
			newNetwork.getRow(newNetwork).set(CyNetwork.NAME, name+" copy");
		}

		List<CyEdge> edgeList = Collections.synchronizedList(new ArrayList<CyEdge>());

		List<CalculateTanimotoTask> taskList = new ArrayList<CalculateTanimotoTask>();

		for (int index1 = 0; index1 < objectList.size(); index1++) {
			CyNode node1 = objectList.get(index1);
			if (canceled) break;

			if (nThreads == 1)
				setStatus("Calculating similarities for "+node1);

			for (int index2 = 0; index2 < index1; index2++) {
				if (canceled) break;
				CyNode node2 = objectList.get(index2);

				if (node2 == node1)
					continue;

				CalculateTanimotoTask task = new CalculateTanimotoTask(network, newNetwork, node1, node2, tcCutoff);
				if (nThreads == 1) {
					task.call();
				} else {
					taskList.add(task);
				}
			}
		}

		ExecutorService threadPool = Executors.newFixedThreadPool(nThreads);

		try {
			List<Future<CyEdge>> futures = threadPool.invokeAll(taskList);
			// System.out.println("invokeAll completes");
			for (Future<CyEdge> future: futures) {
				CyEdge edge = future.get();
				if (edge != null)
					edgeList.add(edge);
			}
		} catch (Exception e) {
			logger.warn("Thread execution exception: "+e);
		}

		if (createNewNetwork) {
			networkManager.addNetwork(newNetwork); // Register the network...
			CyNetworkView newNetworkView = networkViewFactory.createNetworkView(newNetwork);

			for (CyNode node: objectList) {
				View<CyNode> orig = origNetworkView.getNodeView(node);
				View<CyNode> newv = newNetworkView.getNodeView(node);
				Double x = orig.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
				Double y = orig.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);
				newv.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, x);
				newv.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, y);
			}

			// All done -- create and update the view
			VisualStyle vs = visualMappingManager.getVisualStyle(origNetworkView);
			visualMappingManager.setVisualStyle(vs, newNetworkView);
			newNetworkView.fitContent();
			networkViewManager.addNetworkView(newNetworkView); // Register the network view
		}

	}

	class CalculateTanimotoTask implements Callable <CyEdge> {
		CyNode node1;
		CyNode node2;
		CyNetwork origNetwork;
		CyNetwork newNetwork;
		double tcCutoff = 0.25;
		CyEdge newEdge = null;

		public CalculateTanimotoTask(CyNetwork network, CyNetwork newNetwork, 
		                             CyNode node1, CyNode node2, double tcCutoff) {
			this.node1 = node1;
			this.node2 = node2;
			this.origNetwork = network;
			this.newNetwork = newNetwork;
			this.tcCutoff = tcCutoff;
		}

		public CyEdge call() {
			List<Compound> cList1 = getCompounds(node1, origNetwork,
																					 settings.getCompoundAttributes("node",AttriType.smiles),
																					 settings.getCompoundAttributes("node",AttriType.inchi), null);
			if (cList1 == null) return null;

			List<Compound> cList2 = getCompounds(node2, origNetwork,
																					 settings.getCompoundAttributes("node",AttriType.smiles),
																					 settings.getCompoundAttributes("node",AttriType.inchi), null);
			if (cList2 == null) return null;

			int nScores = cList1.size()*cList2.size();
			double maxScore = -1;
			double minScore = 10000000;
			double averageScore = 0;
			for (Compound compound1: cList1) {
				if (compound1 == null) return null;
				for (Compound compound2: cList2) {
					if (canceled) break;
					if (compound2 == null) return null;

					CDKTanimotoScore scorer = new CDKTanimotoScore(compound1, compound2);
					double score = scorer.calculateSimilarity();
					averageScore = averageScore + score/nScores;
					if (score > maxScore) maxScore = score;
					if (score < minScore) minScore = score;
				}
			}

			// Create the edge if we're supposed to
			CyEdge edge = null;
			if (createNewNetwork) {
				// We need the root network to create the new edge
				if (averageScore <= tcCutoff)
					return null;
				edge = newNetwork.addEdge(node1, node2, false);
				newNetwork.getRow(edge).set(CyRootNetwork.SHARED_INTERACTION, "similarity");
				// System.out.println("...done");
			} else {
				newNetwork = origNetwork;
				// Otherwise, get the edges connecting these nodes (if any)
				List<CyEdge> edgeList = origNetwork.getConnectingEdgeList(node1, node2, Type.ANY);
				if (edgeList == null || edgeList.size() == 0) return null;
				edge = edgeList.get(0);
			}

			if (nScores > 1) {
				setAttribute(newNetwork, edge, "AverageTanimotoSimilarity", Double.valueOf(averageScore));
				setAttribute(newNetwork, edge, "MaxTanimotoSimilarity", Double.valueOf(maxScore));
				setAttribute(newNetwork, edge, "MinTanimotoSimilarity", Double.valueOf(minScore));
			} else {
				setAttribute(newNetwork, edge, "TanimotoSimilarity", Double.valueOf(averageScore));
			}

			newEdge = edge;

			return edge;
		}

		CyEdge get() {
			return newEdge;
		}

		private void setAttribute(CyNetwork net, CyEdge edge, String column, Double value) {
			CyTable table = net.getDefaultEdgeTable();
			if (table.getColumn(column) == null) {
				table.createColumn(column, Double.class, false);
			}
			net.getRow(edge).set(column, value);
		}
	}
}
