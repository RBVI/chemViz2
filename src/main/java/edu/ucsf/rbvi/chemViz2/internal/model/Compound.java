package edu.ucsf.rbvi.chemViz2.internal.model;

import java.awt.Color;
import java.awt.Image;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;

import edu.ucsf.rbvi.chemViz2.internal.view.ViewUtils;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.CDKHueckelAromaticityDetector;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.fingerprint.IFingerprinter;
import org.openscience.cdk.inchi.InChIGenerator;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.inchi.InChIToStructure;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.modeling.builder3d.ModelBuilder3D;
import org.openscience.cdk.modeling.builder3d.TemplateHandler3D;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.CDKHydrogenAdder;

import net.sf.jniinchi.INCHI_RET;

/**
 * The Compound class provides the main interface to molecule compounds.  A 
 * given node or edge in Cytoscape could have multiple Compounds, either 
 * by having multiple attributes that contain compound descriptors or
 * by having a single attribute that contains multiple descriptors 
 * (e.g. comma-separated SMILES strings).  The creation of a Compound 
 * results in the building of a cached 2D image for that compound, as 
 * well as the creation of the CDK IAtomContainer, which is used for conversion 
 * from InChI to SMILES, for calculation of the molecular weight,
 * and for the calculation of Tanimoto coefficients.
 */

public class Compound {
	public enum AttriType {smiles, inchi};

	// Class variables
	public static long totalTime = 0L;
	public static long totalFPTime = 0L;
	public static long totalSMILESTime = 0L;
	public static long totalGetFPTime = 0L;

	static private Logger logger = LoggerFactory.getLogger(Compound.class);
	static private Fingerprinter fingerprinter = Fingerprinter.PUBCHEM;

	// Instance variables
	private CyIdentifiable source;
	private CyNetwork network;
	private String smilesStr;
	private String moleculeString;
	private String attribute;
	protected Image renderedImage;
	protected boolean laidOut;
	private AttriType attrType;
	private IAtomContainer iMolecule;
	private IAtomContainer iMolecule3D;
	private BitSet fingerPrint;
	private IFingerprinter fp;
	private int lastImageWidth = -1;
	private int lastImageHeight = -1;
	private	boolean	lastImageFailed = false;
	private ChemInfoSettings settings = null;

	/**
 	 * The constructor is creates a compound and stores it in
 	 * the compound map.  As a byproduct of the compound creation,
 	 * the CDK iMolecule is also created.  This form of the constructor
 	 * assumes that the compound is connected to a node or edge.
 	 *
 	 * @param source the graph object that holds this compound
 	 * @param network the network this source object is in
 	 * @param attribute the attribute that has the compound string
 	 * @param mstring the compound descriptor itself
 	 * @param attrType the type of the compound descriptor (inchi or smiles)
 	 */
	public Compound(ChemInfoSettings settings, CyIdentifiable source, 
	                CyNetwork network, String attribute, 
	                String mstring, AttriType attrType) {
		this(settings, source, network, attribute, mstring, null, attrType);
	}

	/**
 	 * This alternative form of the constructor is called when we've calculated an IAtomContainer internally and it
 	 * bypasses the creation.
 	 *
 	 * @param source the graph object that holds this compound
 	 * @param network the network this source object is in
 	 * @param attribute the attribute that has the compound string
 	 * @param mstring the compound descriptor itself
 	 * @param attrType the type of the compound descriptor (inchi or smiles)
 	 */
	public Compound(ChemInfoSettings settings, CyIdentifiable source, CyNetwork network, 
	                String attribute, String mstring, 
	                IAtomContainer molecule, AttriType attrType) {
		this.source = source;
		this.attribute = attribute;
		this.moleculeString = mstring.trim();
		this.attrType = attrType;
		this.network = network;
		this.settings = settings;

		createStructure(molecule);

		if (source != null) {
			settings.getCompoundManager().addCompound(network, source, this);
		}
	}

	public void reloadStructure() {
		createStructure(null);
	}

	public String getAttribute() {
		return attribute;
	}

	public String getMoleculeString() {
		return moleculeString;
	}

	public CyNetwork getNetwork() {
		return network;
	}

	public IAtomContainer getMolecule() {
		return iMolecule;
	}

	public CyIdentifiable getSource() {
		return source;
	}

	public String getSMILESString() {
		return smilesStr;
	}

	public String toString() {
		return "source="+source+" attribute="+attribute+" molString="+moleculeString;
	}

	public IAtomContainer getMolecule3D() {
		if (iMolecule3D == null) {
			try {
				ModelBuilder3D mb3d = 
					ModelBuilder3D.getInstance(TemplateHandler3D.getInstance(), "mm2");
				iMolecule3D = mb3d.generate3DCoordinates(CDKUtils.addh(iMolecule), true);
			} catch (Exception e) {
				logger.warn("Unable to calculate 3D coordinates: "+e.getMessage());
				iMolecule3D = null;
			}
		}
		return iMolecule3D;
	}

	public void layoutStructure() {
		if (!laidOut) {
			try {
				iMolecule = CDKUtils.layoutMolecule(iMolecule);
				laidOut = true;
			} catch (CDKException e) {
				logger.warn("Unable to layout 2D structure: "+e.getMessage());
				return;
			}
		}
	}

	public BitSet getFingerprint() {
		if (fingerPrint == null) {
			try {
				synchronized (fp) {
					fingerPrint = fp.getBitFingerprint(CDKUtils.addh(iMolecule)).asBitSet();
				}
			} catch (Exception e) {
				logger.warn("Error calculating fingerprint: "+e);
				return null;
			}
		}
		return fingerPrint;
	}

	/**
 	 * Return the 2D image for this compound. 
 	 *
 	 * @param width the width of the rendered image
 	 * @param height the height of the rendered image
 	 * @return the image
 	 */
	public Image getImage(int width, int height) {
		return getImage(width, height, new Color(255,255,255,0));
	}

	/**
 	 * Return the 2D image for this compound.
 	 *
 	 * @param width the width of the rendered image
 	 * @param height the height of the rendered image
 	 * @param background the background color to use for the image
 	 * @return the fetched image
 	 */
	public Image getImage(int width, int height, Color background) {
		if (lastImageWidth != width || lastImageHeight != height || 
		    (renderedImage == null && lastImageFailed == false)) {

			layoutStructure();

			renderedImage = 
				ViewUtils.createImage(iMolecule, width, height, background);
			lastImageWidth = width;
			lastImageHeight = height;
		}

		return renderedImage;
	}
	

	private void createStructure(IAtomContainer molecule) {
		long startTime = Calendar.getInstance().getTimeInMillis();

		this.renderedImage = null;
		this.laidOut = false;
		this.iMolecule = molecule;
		this.iMolecule3D = null;
		this.smilesStr = null;
		this.fp = Compound.fingerprinter.getFingerprinter();
		this.fingerPrint = null;
		long fpTime = Calendar.getInstance().getTimeInMillis();
		totalFPTime += fpTime-startTime;

		if (attrType == AttriType.inchi) {
			// Convert to smiles 
			this.smilesStr = convertInchiToSmiles(moleculeString);
		} else {
			if (moleculeString != null && moleculeString.length() > 0) {
				// Strip any blanks in the string
				this.smilesStr = moleculeString.replaceAll(" ", "");
			}
		}

		if (smilesStr == null)
			return;

		logger.debug("smiles string = "+smilesStr);
		// System.out.println("smiles string = "+smilesStr);

		if (this.iMolecule == null) {
			// Create the CDK Molecule object
			SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
			try {
				iMolecule = sp.parseSmiles(this.smilesStr);
			} catch (InvalidSmilesException e) {
				iMolecule = null;
				logger.warn("Unable to parse SMILES: "+smilesStr+" for "+TableUtils.getName(network, source)+": "+e.getMessage());
				// Try again -- just in case.  CDK 1.5.1 gets a little confused sometimes...
				sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
				try {
					iMolecule = sp.parseSmiles(this.smilesStr);
				} catch (InvalidSmilesException e2) {
					return;
				}
			}
		}

		long smilesTime = Calendar.getInstance().getTimeInMillis();
		totalSMILESTime += smilesTime-fpTime;

		// At this point, we should have an IAtomContainer
		try { 
			CDKHueckelAromaticityDetector.detectAromaticity(iMolecule);

			// Make sure we update our implicit hydrogens
			CDKHydrogenAdder adder = CDKHydrogenAdder.getInstance(iMolecule.getBuilder());
			adder.addImplicitHydrogens(iMolecule);

			// Don't calculate the fingerprint here -- this is *very* expensive
			// fingerPrint = fp.getFingerprint(addh(iMolecule));
			fingerPrint = null;
		} catch (CDKException e1) {
			fingerPrint = null;
		}

		long getFPTime = Calendar.getInstance().getTimeInMillis();
		totalGetFPTime += getFPTime-smilesTime;

		totalTime += getFPTime-startTime;
	}

	private String convertInchiToSmiles(String inchi) {
		try {
			// Get the factory	
			InChIGeneratorFactory factory = InChIGeneratorFactory.getInstance();
			if (!inchi.startsWith("InChI="))
				inchi = "InChI="+inchi;

			logger.debug("Getting structure for: "+inchi);

			InChIToStructure intostruct = factory.getInChIToStructure(inchi, DefaultChemObjectBuilder.getInstance());

			// Get the structure
			INCHI_RET ret = intostruct.getReturnStatus();
			if (ret == INCHI_RET.WARNING) {
				logger.warn("InChI warning: " + intostruct.getMessage());
			} else if (ret != INCHI_RET.OKAY) {
				logger.warn("Structure generation failed: " + ret.toString()
     	               + " [" + intostruct.getMessage() + "]");
				return null;
			}

			IAtomContainer molecule = new AtomContainer(intostruct.getAtomContainer());
			// Use the molecule to create a SMILES string
			SmilesGenerator sg = new SmilesGenerator();
			return sg.createSMILES(molecule);
		} catch (Exception e) {
			logger.warn("Structure generation failed: " + e.getMessage(), e);
			return null;
		}
	}
}
