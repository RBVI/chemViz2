package edu.ucsf.rbvi.chemViz2.internal.tasks;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.ContainsTunables;

import edu.ucsf.rbvi.chemViz2.internal.model.ChemInfoSettings;

public class ChemInfoSettingsTask extends AbstractTask {

	@ContainsTunables
	public ChemInfoSettings settings = null;

	public ChemInfoSettingsTask(ChemInfoSettings settings) {
		this.settings = settings;
	}

	public void run(final TaskMonitor taskMonitor) throws Exception {
	}

}
