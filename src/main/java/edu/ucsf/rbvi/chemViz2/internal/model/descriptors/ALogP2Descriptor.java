package edu.ucsf.rbvi.chemViz2.internal.model.descriptors;

import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.result.DoubleArrayResult;
import org.openscience.cdk.qsar.descriptors.molecular.ALOGPDescriptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsf.rbvi.chemViz2.internal.model.CDKUtils;
import edu.ucsf.rbvi.chemViz2.internal.model.Compound;
import edu.ucsf.rbvi.chemViz2.internal.model.Descriptor;

public class ALogP2Descriptor implements Descriptor <Double> {
  private static Logger logger = LoggerFactory.getLogger(ALogP2Descriptor.class);

	public ALogP2Descriptor() { }

	public String toString() {return "ALogP2"; }
	public String getShortName() {return "alogp2";}
	public Class getClassType() {return Double.class;}
	public List<String> getDescriptorList() {return null;}

	@Override
	public Double getDescriptor(Compound c) {
		IAtomContainer iMolecule = c.getMolecule();
		if (iMolecule == null) return null;
		try {
			IMolecularDescriptor descriptor = new ALOGPDescriptor();
			DoubleArrayResult retval = (DoubleArrayResult)(descriptor.calculate(CDKUtils.addh(iMolecule)).getValue());
			return retval.get(1);
		} catch (Exception e) {
			logger.warn("Unable to calculate ALogP2 values: "+e.getMessage());
		}

		return null;
	}
}
