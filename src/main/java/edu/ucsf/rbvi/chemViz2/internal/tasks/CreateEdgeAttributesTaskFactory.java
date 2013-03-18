package edu.ucsf.rbvi.chemViz2.internal.tasks;

import java.util.Collections;
import java.util.List;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.task.EdgeViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.chemViz2.internal.model.ChemInfoSettings;

public class CreateEdgeAttributesTaskFactory extends ChemVizAbstractTaskFactory 
                                             implements NetworkTaskFactory, EdgeViewTaskFactory {
	ChemInfoSettings settings = null;
	Scope scope;

	public CreateEdgeAttributesTaskFactory(ChemInfoSettings settings, Scope scope) {
		this.settings = settings;
		this.scope = scope;
	}

	public boolean isReady(CyNetwork network) {
		if (scope == Scope.ALLEDGES && settings.hasEdgeCompounds(network.getEdgeList()))
			return true;

		if (scope == Scope.SELECTEDEDGES)
			return selectedEdgesReady(network);

		return false;
	}

	public boolean isReady(View<CyEdge> eView, CyNetworkView netView) {
		if (eView != null && settings.hasEdgeCompounds(Collections.singletonList(eView.getModel())))
			return true;

		return selectedEdgesReady(netView.getModel());
	}

	public TaskIterator createTaskIterator() {
		return null;
	}

	public TaskIterator createTaskIterator(CyNetwork network) {
		return new TaskIterator(new CreateAttributesTask(network, null, scope, settings));
	}

	public TaskIterator createTaskIterator(View<CyEdge> eView, CyNetworkView netView) {
		return new TaskIterator(new CreateAttributesTask(netView.getModel(), eView.getModel(), scope, settings));
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
