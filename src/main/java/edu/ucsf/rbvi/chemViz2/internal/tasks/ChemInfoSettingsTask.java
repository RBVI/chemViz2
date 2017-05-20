package edu.ucsf.rbvi.chemViz2.internal.tasks;

import java.util.HashMap;
import java.util.Map;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.ContainsTunables;
import org.cytoscape.work.ObservableTask;

import edu.ucsf.rbvi.chemViz2.internal.model.ChemInfoSettings;

public class ChemInfoSettingsTask extends AbstractTask implements ObservableTask {

	@ContainsTunables
	public ChemInfoSettings settings = null;

	public ChemInfoSettingsTask(ChemInfoSettings settings) {
		this.settings = settings;
	}

	public void run(final TaskMonitor taskMonitor) throws Exception {
		if (settings != null) {
			settings.saveSettings();
		}
	}

	@Override
	public <R> R getResults(Class <? extends R> type) {
		String settingsString = settings.getSettingsString();
		if (type.equals(String.class)) {
			return (R)settingsString;
		} else if (type.equals(Map.class)) {
			Map<String,String> sMap = new HashMap<>();
			String[] sArray = settingsString.split(";");
			for (String s: sArray) {
				String[] nv = s.split("=");
				sMap.put(nv[0],nv[1]);
			}
			return (R)sMap;
		}
		return null;
	}

}
