/*
  Copyright (c) 2012 University of California, San Francisco

  This library is free software; you can redistribute it and/or modify it
  under the terms of the GNU Lesser General Public License as published
  by the Free Software Foundation; either version 2.1 of the License, or
  any later version.

  This library is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
  MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
  documentation provided hereunder is on an "as is" basis, and the
  Institute for Systems Biology and the Whitehead Institute
  have no obligations to provide maintenance, support,
  updates, enhancements or modifications.  In no event shall the
  Institute for Systems Biology and the Whitehead Institute
  be liable to any party for direct, indirect, special,
  incidental or consequential damages, including lost profits, arising
  out of the use of this software and its documentation, even if the
  Institute for Systems Biology and the Whitehead Institute
  have been advised of the possibility of such damage.  See
  the GNU Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with this library; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
*/

package edu.ucsf.rbvi.chemViz2.internal.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;

import org.openscience.cdk.fingerprint.EStateFingerprinter;
import org.openscience.cdk.fingerprint.ExtendedFingerprinter;
import org.openscience.cdk.fingerprint.GraphOnlyFingerprinter;
import org.openscience.cdk.fingerprint.HybridizationFingerprinter;
import org.openscience.cdk.fingerprint.IFingerprinter;
import org.openscience.cdk.fingerprint.KlekotaRothFingerprinter;
import org.openscience.cdk.fingerprint.MACCSFingerprinter;
import org.openscience.cdk.fingerprint.SubstructureFingerprinter;

public enum Fingerprinter {
	CDK("CDK", org.openscience.cdk.fingerprint.Fingerprinter.class),
	ESTATE("E-State", EStateFingerprinter.class),
	EXTENDED("Extended CDK", ExtendedFingerprinter.class),
	GRAPHONLY("Graph Only", GraphOnlyFingerprinter.class),
	HYBRIDIZATION("Hybridization", HybridizationFingerprinter.class),
	KLEKOTAROTH("Klekota & Roth", KlekotaRothFingerprinter.class),
	MACCS("MACCS", MACCSFingerprinter.class),
	PUBCHEM("Pubchem", PubchemFingerprinterWrapper.class),
	SUBSTRUCTURE("Substructure bitset", SubstructureFingerprinter.class);

  private String name;
  private Class fingerprinter;
	private static Logger logger = LoggerFactory.getLogger(edu.ucsf.rbvi.chemViz2.internal.model.Fingerprinter.class);
  private Fingerprinter(String str, Class fp) { 
		name=str; 
		fingerprinter = fp;
	}
  public String toString() { return name; }
  public String getName() { return name; }
  public IFingerprinter getFingerprinter() { 
		IFingerprinter i; 
		try {
			Constructor<IFingerprinter> c = fingerprinter.getConstructor();
			i = c.newInstance(); 
		} catch (Exception e) {
			e.printStackTrace();
			logger.warn("Unable to create fingerprinter instance for "+getName()+": "+e);
			return null;
		}
		return i;
	}
}
