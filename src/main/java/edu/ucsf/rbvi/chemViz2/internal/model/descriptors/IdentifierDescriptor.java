package edu.ucsf.rbvi.chemViz2.internal.model.descriptors;

import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;

import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Descriptor;

public class IdentifierDescriptor implements Descriptor <String> {

	public IdentifierDescriptor() { }

	public String toString() {return "Molecular String"; }
	public String getShortName() {return "molstring";}
	public Class getClassType() {return String.class;}
	public List<String> getDescriptorList() {return null;}

	@Override
	public String getDescriptor(Compound c) {
		return c.getMoleculeString();
	}
}
