package edu.ucsf.rbvi.chemViz2.internal.model.descriptors;

import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.HBondAcceptorCountDescriptor;
import org.openscience.cdk.qsar.result.IntegerResult;

import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Descriptor;

public class HBondAcceptorDescriptor implements Descriptor <Integer> {

	public HBondAcceptorDescriptor() { }

	public String toString() {return "HBond Acceptors"; }
	public String getShortName() {return "acceptors";}
	public Class getClassType() {return Integer.class;}
	public List<String> getDescriptorList() {return null;}

	@Override
	public Integer getDescriptor(Compound c) {
		IAtomContainer iMolecule = c.getMolecule();
		if (iMolecule == null) return null;
		IMolecularDescriptor descriptor = new HBondAcceptorCountDescriptor();
		IntegerResult retval = (IntegerResult)(descriptor.calculate(iMolecule).getValue());
		return retval.intValue();
	}
}
