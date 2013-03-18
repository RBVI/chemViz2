package edu.ucsf.rbvi.chemViz2.internal.similarity;

import edu.ucsf.rbvi.chemViz2.internal.model.Compound;

/**
 * This will calculate the similarity between the two CyNode that is 
 * connected to an CyEdge, if they both have Smiles/InChI specified. 
 *
 */
public abstract class SimilarityScore {
	protected Compound compound1;
	protected Compound compound2;

	public SimilarityScore(Compound compound1, Compound compound2) {
		this.compound1 = compound1;
		this.compound2 = compound2;
	}
	
	/**
	 * This method will return the similarity score. 
	 * 
	 * @return
	 */
	public abstract double calculateSimilarity();
}
