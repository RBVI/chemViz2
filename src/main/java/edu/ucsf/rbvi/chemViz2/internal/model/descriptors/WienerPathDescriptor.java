package edu.ucsf.rbvi.chemViz2.internal.model.descriptors;

import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.result.DoubleArrayResult;
import org.openscience.cdk.qsar.descriptors.molecular.WienerNumbersDescriptor;

import edu.ucsf.rbvi.chemViz2.internal.model.CDKUtils;
import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Descriptor;

public class WienerPathDescriptor implements Descriptor <Double> {

	public WienerPathDescriptor() { }

	public String toString() {return "Wiener Path"; }
	public String getShortName() {return "wienerpath";}
	public Class getClassType() {return Double.class;}
	public List<String> getDescriptorList() {return null;}

	@Override
	public Double getDescriptor(Compound c) {
		IAtomContainer iMolecule = c.getMolecule();
		if (iMolecule == null) return null;
		IMolecularDescriptor descriptor = new WienerNumbersDescriptor();
		DoubleArrayResult retval = (DoubleArrayResult)(descriptor.calculate(CDKUtils.addh(iMolecule)).getValue());
		return retval.get(0);
	}
}
