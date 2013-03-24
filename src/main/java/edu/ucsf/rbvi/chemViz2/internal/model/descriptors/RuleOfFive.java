package edu.ucsf.rbvi.chemViz2.internal.model.descriptors;

import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.result.IntegerResult;
import org.openscience.cdk.qsar.descriptors.molecular.RuleOfFiveDescriptor;

import edu.ucsf.rbvi.chemViz2.internal.model.CDKUtils;
import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Descriptor;
import edu.ucsf.rbvi.chemViz2.internal.model.DescriptorManager;

public class RuleOfFive implements Descriptor <Integer> {

	public RuleOfFive() { }

	public String toString() {return "Lipinski's Rule of Five Failures"; }
	public String getShortName() {return "roff";}
	public Class getClassType() {return Integer.class;}
	public List<String> getDescriptorList() {return null;}

	@Override
	public Integer getDescriptor(Compound c) {
		IAtomContainer iMolecule = c.getMolecule();

		if (iMolecule == null) return null;
		IMolecularDescriptor descriptor = new RuleOfFiveDescriptor();
		IntegerResult retval = (IntegerResult)(descriptor.calculate(CDKUtils.addh(iMolecule)).getValue());
		return retval.intValue();
	}
}
