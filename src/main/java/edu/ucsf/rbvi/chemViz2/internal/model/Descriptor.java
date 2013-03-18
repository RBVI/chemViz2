package edu.ucsf.rbvi.chemViz2.internal.model;

import java.util.List;

public interface Descriptor<T> {
	public String toString();
	public String getShortName();
	public Class getClassType();
	public List<String> getDescriptorList();

	public T getDescriptor(Compound c);

}
