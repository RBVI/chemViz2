package edu.ucsf.rbvi.chemViz2.internal.model.descriptors;

import java.util.List;

import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.HBondDonorCountDescriptor;
import org.openscience.cdk.qsar.result.IntegerResult;

import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Descriptor;

public class HBondDonorDescriptor implements Descriptor <Integer> {

	public HBondDonorDescriptor() { }

	public String toString() {return "HBond Donors"; }
	public String getShortName() {return "donors";}
	public Class getClassType() {return Integer.class;}
	public List<String> getDescriptorList() {return null;}

	@Override
	public Integer getDescriptor(Compound c) {
		IMolecule iMolecule = c.getMolecule();
		if (iMolecule == null) return null;
		IMolecularDescriptor descriptor = new HBondDonorCountDescriptor();
		IntegerResult retval = (IntegerResult)(descriptor.calculate(iMolecule).getValue());
		return retval.intValue();
	}
}
