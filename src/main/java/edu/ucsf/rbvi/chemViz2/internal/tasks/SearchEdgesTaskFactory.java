package edu.ucsf.rbvi.chemViz2.internal.tasks;

import java.util.Collections;
import java.util.List;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.task.EdgeViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.chemViz2.internal.model.ChemInfoSettings;

public class SearchEdgesTaskFactory extends ChemVizAbstractTaskFactory 
                                    implements NetworkTaskFactory, EdgeViewTaskFactory {
	ChemInfoSettings settings = null;
	Scope scope;
	boolean showResult = false;

	public SearchEdgesTaskFactory(ChemInfoSettings settings, boolean showResult, 
	                              Scope scope) {
		this.settings = settings;
		this.scope = scope;
		this.showResult = showResult;
	}

	public TaskIterator createTaskIterator() {
		return null;
	}

	public boolean isReady(CyNetwork network) {
		if (scope == Scope.ALLNODES && settings.hasEdgeCompounds(network.getEdgeList()))
			return true;

		if (scope == Scope.SELECTEDNODES)
			return selectedEdgesReady(network);

		return false;
	}

	public boolean isReady(View<CyEdge> eView, CyNetworkView netView) {
		if (eView != null && settings.hasEdgeCompounds(Collections.singletonList(eView.getModel())))
			return true;

		return selectedEdgesReady(netView.getModel());
	}

	public TaskIterator createTaskIterator(CyNetwork network) {
		if (scope == Scope.ALLNODES)
			return new TaskIterator(new SMARTSSearchTask(network, network.getEdgeList(), scope, 
			                                             showResult, settings));
		else {
			List<CyEdge> selectedEdges = CyTableUtil.getEdgesInState(network, CyNetwork.SELECTED, true);
			return new TaskIterator(new SMARTSSearchTask(network, selectedEdges, scope, showResult, settings));
		}
	}

	public TaskIterator createTaskIterator(View<CyEdge> eView, CyNetworkView netView) {
		List<CyEdge> selectedEdges = CyTableUtil.getEdgesInState(netView.getModel(), CyNetwork.SELECTED, true);
		if (selectedEdges == null || selectedEdges.size() == 0)
			selectedEdges = Collections.singletonList(eView.getModel());

		return new TaskIterator(new SMARTSSearchTask(netView.getModel(), selectedEdges, scope, showResult, settings));
	}

	private boolean selectedEdgesReady(CyNetwork network) {
		List<CyEdge> selectedEdges = CyTableUtil.getEdgesInState(network, CyNetwork.SELECTED, true);
		if (selectedEdges != null && selectedEdges.size() > 0) {
			if (settings.hasEdgeCompounds(selectedEdges))
				return true;
		}
		return false;
	}
}
