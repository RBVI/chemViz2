package edu.ucsf.rbvi.chemViz2.internal.model.descriptors;

import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.RotatableBondsCountDescriptor;
import org.openscience.cdk.qsar.result.IntegerResult;

import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Descriptor;

public class RotatableBondsDescriptor implements Descriptor <Integer> {

	public RotatableBondsDescriptor() { }

	public String toString() {return "Rotatable Bonds Count"; }
	public String getShortName() {return "rotbonds";}
	public Class getClassType() {return Integer.class;}
	public List<String> getDescriptorList() {return null;}

	@Override
	public Integer getDescriptor(Compound c) {
		IAtomContainer iMolecule = c.getMolecule();
		if (iMolecule == null) return null;
		IMolecularDescriptor descriptor = new RotatableBondsCountDescriptor();
		IntegerResult retval = (IntegerResult)(descriptor.calculate(iMolecule).getValue());
		return retval.intValue();
	}
}
