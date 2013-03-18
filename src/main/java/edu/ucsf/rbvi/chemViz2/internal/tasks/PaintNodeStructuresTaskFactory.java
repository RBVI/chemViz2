package edu.ucsf.rbvi.chemViz2.internal.tasks;

import java.util.Collections;
import java.util.List;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.chemViz2.internal.model.ChemInfoSettings;

public class PaintNodeStructuresTaskFactory extends ChemVizAbstractTaskFactory 
                                             implements NetworkViewTaskFactory, NodeViewTaskFactory {
	ChemInfoSettings settings = null;
	Scope scope;
	VisualLexicon lex;
	VisualMappingFunctionFactory vmff;
	VisualMappingManager vmm;
	boolean remove = false;

	public PaintNodeStructuresTaskFactory(VisualMappingManager vmm, VisualMappingFunctionFactory vmff, 
	                                      VisualLexicon lex, ChemInfoSettings settings, Scope scope,
	                                      boolean remove) {
		this.settings = settings;
		this.scope = scope;
		this.vmff = vmff;
		this.vmm = vmm;
		this.lex = lex;
		this.remove = remove;
	}

	public TaskIterator createTaskIterator() {
		return null;
	}

	public boolean isReady(CyNetworkView networkView) {
		if (scope == Scope.ALLNODES && settings.hasNodeCompounds(networkView.getModel().getNodeList()))
			return true;

		if (scope == Scope.SELECTEDNODES)
			return selectedNodesReady(networkView);

		return false;
	}

	public boolean isReady(View<CyNode> nView, CyNetworkView netView) {
		if (nView != null && settings.hasNodeCompounds(Collections.singletonList(nView.getModel())))
			return true;

		return selectedNodesReady(netView);
	}

	public TaskIterator createTaskIterator(CyNetworkView networkView) {
		return new TaskIterator(new PaintStructuresTask(vmm, vmff, lex, networkView, null, scope, settings, remove));
	}

	public TaskIterator createTaskIterator(View<CyNode> nView, CyNetworkView netView) {
		return new TaskIterator(new PaintStructuresTask(vmm, vmff, lex, netView, nView.getModel(), scope, settings, remove));
	}

	private boolean selectedNodesReady(CyNetworkView networkView) {
		List<CyNode> selectedNodes = CyTableUtil.getNodesInState(networkView.getModel(), CyNetwork.SELECTED, true);
		if (selectedNodes != null && selectedNodes.size() > 0) {
			if (settings.hasNodeCompounds(selectedNodes))
				return true;
		}
		return false;
	}
}
