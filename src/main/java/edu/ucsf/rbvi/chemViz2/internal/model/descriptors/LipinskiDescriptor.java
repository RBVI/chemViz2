package edu.ucsf.rbvi.chemViz2.internal.model.descriptors;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openscience.cdk.interfaces.IMolecule;

import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Descriptor;
import edu.ucsf.rbvi.chemViz2.internal.model.DescriptorManager;

public class LipinskiDescriptor implements Descriptor <Map<Descriptor, Object>> {
	private static String[] LipinskiTypes = { "weight", "alogp", "acceptors", "donors" };
	private DescriptorManager manager;

	public LipinskiDescriptor(DescriptorManager manager) {
		this.manager = manager;
	}

	public String toString() {return "Lipinski parameters"; }
	public String getShortName() {return "lipinski";}
	public Class getClassType() {return Map.class;}
	public List<String> getDescriptorList() {return Arrays.asList(LipinskiTypes);}

	@Override
	public Map<Descriptor, Object> getDescriptor(Compound c) {
		IMolecule iMolecule = c.getMolecule();

		if (iMolecule == null) return null;
		Map<Descriptor, Object> lip = new HashMap<Descriptor,Object>();
		for (String typeName: LipinskiTypes) {
			Descriptor subType = manager.getDescriptor(typeName);
			lip.put(subType, subType.getDescriptor(c));
		}
		return lip;
	}
}
