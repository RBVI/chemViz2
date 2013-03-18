package edu.ucsf.rbvi.chemViz2.internal.model.descriptors;

import java.util.List;

import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.qsar.descriptors.molecular.ZagrebIndexDescriptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Descriptor;

public class ZagrebDescriptor implements Descriptor <Double> {
  private static Logger logger = LoggerFactory.getLogger(ZagrebDescriptor.class);

	public ZagrebDescriptor() { }

	public String toString() {return "Zagreb Index"; }
	public String getShortName() {return "zagrebindex";}
	public Class getClassType() {return Double.class;}
	public List<String> getDescriptorList() {return null;}

	@Override
	public Double getDescriptor(Compound c) {
		IMolecule iMolecule = c.getMolecule();
		if (iMolecule == null) return null;
		IMolecularDescriptor descriptor = new ZagrebIndexDescriptor();
		DoubleResult retval = (DoubleResult)(descriptor.calculate(iMolecule).getValue());
		return retval.doubleValue();
	}
}
