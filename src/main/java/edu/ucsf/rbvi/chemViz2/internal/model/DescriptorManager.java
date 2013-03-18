package edu.ucsf.rbvi.chemViz2.internal.model;

import java.util.ArrayList;
import java.util.List;

import edu.ucsf.rbvi.chemViz2.internal.model.descriptors.*;

/**
 * This class manages the list of descriptors.  In the future,
 * this should be changed to listen for OSGi registrations of
 * descriptors.
 */

public class DescriptorManager {
	List<Descriptor> descriptorList;

	public DescriptorManager() {
		descriptorList = new ArrayList<Descriptor>();

		// Create our default descriptors
		addDescriptor(new ImageDescriptor());
		addDescriptor(new AttributeDescriptor());
		addDescriptor(new IdentifierDescriptor());
		addDescriptor(new LipinskiDescriptor(this));
		addDescriptor(new SDFDescriptor(this));
		addDescriptor(new ALogPDescriptor());
		addDescriptor(new ALogP2Descriptor());
		addDescriptor(new AromaticRingCountDescriptor());
		addDescriptor(new AtomicCompositionDescriptor());
		addDescriptor(new MassDescriptor());
		addDescriptor(new HeavyAtomCountDescriptor());
		addDescriptor(new HBondAcceptorDescriptor());
		addDescriptor(new HBondDonorDescriptor());
		addDescriptor(new LOBMaxDescriptor());
		addDescriptor(new LOBMinDescriptor());
		addDescriptor(new RuleOfFive());
		addDescriptor(new AMRDescriptor());
		addDescriptor(new WeightDescriptor());
		addDescriptor(new RingCountDescriptor());
		addDescriptor(new RotatableBondsDescriptor());
		addDescriptor(new TPSA());
		addDescriptor(new TotalBondsDescriptor());
		addDescriptor(new WienerPathDescriptor());
		addDescriptor(new WienerPolarityDescriptor());
		addDescriptor(new XLogP());
		addDescriptor(new ZagrebDescriptor());
	}

	public void addDescriptor(Descriptor desc) {
		descriptorList.add(desc);
	}

	public void removeDescriptor(Descriptor desc) {
		descriptorList.remove(desc);
	}

	public Descriptor getDescriptor(String name) {
		for (Descriptor desc: descriptorList) {
			if (name.equals(desc.getShortName()))
				return desc;
		}
		return null;
	}

	public List<Descriptor> getDescriptorList(boolean allDescriptors) {
		List<Descriptor> returnList = new ArrayList<Descriptor>();
		if (allDescriptors)
			returnList.addAll(descriptorList);
		else {
			for (Descriptor desc: descriptorList) {
				String descName = desc.getShortName();
				// Skip over the "internal" descriptors
				if (!descName.equals("image") &&
				    !descName.equals("attribute") &&
				    !descName.equals("molstring")) {
					returnList.add(desc);
				}
			}
		}
		return returnList;
	}
}
