package edu.ucsf.rbvi.chemViz2.internal.model.descriptors;

import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.qsar.DescriptorValue;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.LengthOverBreadthDescriptor;
import org.openscience.cdk.qsar.result.DoubleArrayResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Descriptor;

public class LOBMaxDescriptor implements Descriptor <Double> {
  private static Logger logger = LoggerFactory.getLogger(LOBMaxDescriptor.class);

	public LOBMaxDescriptor() { }

	public String toString() {return "Length over Breadth Max"; }
	public String getShortName() {return "lobmax";}
	public Class getClassType() {return Double.class;}
	public List<String> getDescriptorList() {return null;}

	@Override
	public Double getDescriptor(Compound c) {
		IAtomContainer iMolecule3D = c.getMolecule3D();
		if (iMolecule3D == null) return null;
		IMolecularDescriptor descriptor = new LengthOverBreadthDescriptor();
		DescriptorValue val = descriptor.calculate(iMolecule3D);
		if (val.getException() != null) {
			logger.warn("Unable to calculate LengthOverBreadthDescriptor: "+val.getException().getMessage());
			return null;
		}
		DoubleArrayResult retval = (DoubleArrayResult)(val.getValue());
		return retval.get(0);
	}
}
