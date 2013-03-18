package edu.ucsf.rbvi.chemViz2.internal.tasks;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.work.AbstractTaskFactory;

import edu.ucsf.rbvi.chemViz2.internal.model.ChemInfoSettings;

public abstract class ChemVizAbstractTaskFactory extends AbstractTaskFactory {
	public enum Scope {ALLNODES, ALLEDGES, SELECTEDNODES, SELECTEDEDGES};
}
