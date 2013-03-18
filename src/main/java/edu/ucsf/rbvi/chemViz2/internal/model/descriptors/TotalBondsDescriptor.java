package edu.ucsf.rbvi.chemViz2.internal.model.descriptors;

import java.util.List;

import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.BondCountDescriptor;
import org.openscience.cdk.qsar.result.IntegerResult;

import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Descriptor;

public class TotalBondsDescriptor implements Descriptor <Integer> {

	public TotalBondsDescriptor() { }

	public String toString() {return "Total Number of Bonds"; }
	public String getShortName() {return "totbonds";}
	public Class getClassType() {return Integer.class;}
	public List<String> getDescriptorList() {return null;}

	@Override
	public Integer getDescriptor(Compound c) {
		IMolecule iMolecule = c.getMolecule();
		if (iMolecule == null) return null;
		IMolecularDescriptor descriptor = new BondCountDescriptor();
		IntegerResult retval = (IntegerResult)(descriptor.calculate(iMolecule).getValue());
		return retval.intValue();
	}
}
