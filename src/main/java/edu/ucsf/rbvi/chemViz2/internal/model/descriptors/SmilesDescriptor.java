package edu.ucsf.rbvi.chemViz2.internal.model.descriptors;

import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Descriptor;

public class SmilesDescriptor implements Descriptor <String> {

	public SmilesDescriptor() { }

	public String toString() {return "SMILES String"; }
	public String getShortName() {return "smiles";}
	public Class getClassType() {return String.class;}
	public List<String> getDescriptorList() {return null;}

	@Override
	public String getDescriptor(Compound c) {
		return c.getSMILESString();
	}
}
