package edu.ucsf.rbvi.chemViz2.internal.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;

public class CompoundManager {
	private Map<CyNetwork, Map<CyIdentifiable, List<Compound>>> compoundMap;
	private Map<String, Compound> molStringMap;

	public CompoundManager() {
		compoundMap = new ConcurrentHashMap<CyNetwork, Map<CyIdentifiable, List<Compound>>>();
		molStringMap = new ConcurrentHashMap<String, Compound>();
	}

	public void addCompound(CyNetwork network, CyIdentifiable id, Compound compound) {
		List<Compound> mapList = null;
		Map<CyIdentifiable, List<Compound>> idMap = null;

		if (!compoundMap.containsKey(network)) {
			idMap = new ConcurrentHashMap<CyIdentifiable, List<Compound>>();
			compoundMap.put(network, idMap);
		} else {
			idMap = compoundMap.get(network);
		}
		
		if (idMap.containsKey(id)) {
			mapList = idMap.get(id);
		} else {
			mapList = new ArrayList<Compound>();
		}
		mapList.add(compound);
		idMap.put(id, mapList);
		if (compound.getSMILESString() != null)
			molStringMap.put(compound.getSMILESString(), compound);
	}

	public void removeCompound(CyNetwork network, CyIdentifiable id, Compound compound) {
		List<Compound> compoundList = getCompounds(network, id);
		if (compoundList != null) 
			compoundList.remove(compound);
		if (molStringMap.containsKey(compound.getSMILESString()))
			molStringMap.remove(compound.getSMILESString());
	}

	public List<Compound> getCompounds(CyNetwork network, CyIdentifiable id) {
		if (compoundMap != null && compoundMap.containsKey(network)) {
			Map<CyIdentifiable, List<Compound>> idMap = compoundMap.get(network);
			if (idMap.containsKey(id))
				return idMap.get(id);
		}
		return null;
	}

  /**
	 * Returns the compound that matches the passed arguments or null if no such compound exists.
	 *
	 * @param id the graph object we're looking at
	 * @param network the network we're looking at
	 * @param attr the attribute that contains the compound descriptor
	 * @param molString the compound descriptor
	 * @param type the type of the attribute (smiles or inchi)
	 * @return the compound that matched or 'null' if no such compound exists.
	 */
	public Compound getCompound(CyIdentifiable id, CyNetwork network, 
	                            String attr, String molString, Compound.AttriType type) {
		List<Compound> compoundList = getCompounds(network, id);
		if (compoundList == null) return null;

		for (Compound c: compoundList) {
			if (c.getAttribute().equals(attr) && c.getMoleculeString().equals(molString))
				return c;
		}
		return null;
	}

  /**
	 * Returns the compound that matches the compound descriptor.
	 *
	 * @param molString the compound descriptor (SMILES)
	 * @return the compound that matched or 'null' if no such compound exists.
	 */
	public Compound getCompound(String molString) {
		if (molStringMap.containsKey(molString))
			return molStringMap.get(molString);
		return null;
	}

	public void clearStructures(CyIdentifiable id, CyNetwork network) {
		if (id == null && compoundMap.containsKey(network)) {
			compoundMap.remove(network);
		} else {
			if (compoundMap.get(network).containsKey(id))
				compoundMap.get(network).remove(id);
		}
	}

	public void reloadStructures(CyIdentifiable id, CyNetwork network) {
		if (compoundMap.containsKey(network)) {
			Map<CyIdentifiable, List<Compound>> idMap = compoundMap.get(network);

			if (id == null) {
				// Clear all compounds in this network
				for (List<Compound> cList: idMap.values()) {
					for (Compound c: cList) {
						c.reloadStructure();
					}
				}
			} else if (idMap.containsKey(id)) {
				List<Compound> cList = idMap.get(id);
				for (Compound c: cList) {
					c.reloadStructure();
				}
			}
		}
	}
}
