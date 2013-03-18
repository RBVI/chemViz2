package edu.ucsf.rbvi.chemViz2.internal.tasks;

import java.util.Collections;
import java.util.List;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.chemViz2.internal.model.ChemInfoSettings;

public class CompoundNodeTableTaskFactory extends ChemVizAbstractTaskFactory 
                                          implements NetworkTaskFactory, NodeViewTaskFactory {
	ChemInfoSettings settings = null;
	Scope scope;

	public CompoundNodeTableTaskFactory(ChemInfoSettings settings, Scope scope) {
		this.settings = settings;
		this.scope = scope;
	}

	public TaskIterator createTaskIterator() {
		return null;
	}

	public boolean isReady(CyNetwork network) {
		if (scope == Scope.ALLNODES && settings.hasNodeCompounds(network.getNodeList()))
			return true;

		if (scope == Scope.SELECTEDNODES)
			return selectedNodesReady(network);

		return false;
	}

	public boolean isReady(View<CyNode> nView, CyNetworkView netView) {
		if (nView != null && settings.hasNodeCompounds(Collections.singletonList(nView.getModel())))
			return true;

		return selectedNodesReady(netView.getModel());
	}

	public TaskIterator createTaskIterator(CyNetwork network) {
		return new TaskIterator(new CompoundTableTask(network, null, scope, settings));
	}

	public TaskIterator createTaskIterator(View<CyNode> nView, CyNetworkView netView) {
		return new TaskIterator(new CompoundTableTask(netView.getModel(), nView.getModel(), scope, settings));
	}

	private boolean selectedNodesReady(CyNetwork network) {
		List<CyNode> selectedNodes = CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true);
		if (selectedNodes != null && selectedNodes.size() > 0) {
			if (settings.hasNodeCompounds(selectedNodes))
				return true;
		}
		return false;
	}
}
