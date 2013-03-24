package edu.ucsf.rbvi.chemViz2.internal.model.descriptors;

import java.util.List;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Descriptor;
import edu.ucsf.rbvi.chemViz2.internal.model.DescriptorManager;

public class SDFDescriptor implements Descriptor <Map<Descriptor, Object>> {
	private static String[] SDFTypes = { "xlogp", "polarsurface", "zagrebindex"};
	private DescriptorManager manager;

	public SDFDescriptor(DescriptorManager manager) {
		this.manager = manager;
	}

	public String toString() {return "SDF parameters"; }
	public String getShortName() {return "sdf";}
	public Class getClassType() {return Map.class;}
	public List<String> getDescriptorList() {return Arrays.asList(SDFTypes);}

	@Override
	public Map<Descriptor, Object> getDescriptor(Compound c) {
		IAtomContainer iMolecule = c.getMolecule();

		if (iMolecule == null) return null;
		Map<Descriptor, Object> lip = new HashMap<Descriptor,Object>();
		for (String typeName: SDFTypes) {
			Descriptor subType = manager.getDescriptor(typeName);
			lip.put(subType, subType.getDescriptor(c));
		}
		return lip;
	}
}
