package edu.ucsf.rbvi.chemViz2.internal.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics;
import org.cytoscape.view.presentation.customgraphics.CustomGraphicLayer;
import org.cytoscape.view.presentation.customgraphics.PaintedShape;

import edu.ucsf.rbvi.chemViz2.internal.model.Compound;

import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.elements.ArrowElement;
import org.openscience.cdk.renderer.elements.AtomSymbolElement;
import org.openscience.cdk.renderer.elements.ElementGroup;
import org.openscience.cdk.renderer.elements.GeneralPath;
import org.openscience.cdk.renderer.elements.IRenderingElement;
import org.openscience.cdk.renderer.elements.LineElement;
import org.openscience.cdk.renderer.elements.OvalElement;
import org.openscience.cdk.renderer.elements.PathElement;
import org.openscience.cdk.renderer.elements.RectangleElement;
import org.openscience.cdk.renderer.elements.TextElement;
import org.openscience.cdk.renderer.elements.TextGroupElement;
import org.openscience.cdk.renderer.elements.WedgeLineElement;
import org.openscience.cdk.renderer.elements.path.Type;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.font.IFontManager;
import org.openscience.cdk.renderer.generators.BasicBondGenerator.WedgeWidth;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator.ArrowHeadWidth;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator.Scale;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator.UseAntiAliasing;
import org.openscience.cdk.renderer.visitor.IDrawVisitor;


/**
 * Implementation of the {@link IDrawVisitor} interface for the AWT
 * widget toolkit, allowing molecules to be rendered with toolkits based on
 * AWT, like the Java reference graphics platform Swing.  This implementation
 * returns a Java Shape rather than directly rendering the content.
 *
 * @cdk.module renderawt
 */

public class ChemVizCustomGraphics implements CyCustomGraphics<PaintedShape> {

	private	Paint backgroundColor;
	private float fitRatio = 0.9f;
	private Long id;
	private int height = 100;
	private int width = 100;
	private String displayName = "chemViz";
	private Compound compound;

	public ChemVizCustomGraphics(Compound compound, double scale) {
		backgroundColor = Color.WHITE;
		this.compound = compound;
		// System.out.println("ChemVizCustomGraphics("+compound+","+scale+")");
	}

	// CyCustomGraphics implementation -- public methods
	public String getDisplayName() {return displayName;}
	public float getFitRatio() {return fitRatio;}
	public int getHeight() { return height; }
	public Long getIdentifier() {return id;}
	public List<PaintedShape> getLayers(CyNetworkView networkView, View<? extends CyIdentifiable> grView) {
		// Set offsets
		// System.out.println("Molecule = "+compound.getMolecule());
		return ViewUtils.createShapes((double)(-width)/2.0, (double)(-height)/2.0, (double)width, (double)height, 
		                              compound.getMolecule(), backgroundColor);
	}
	public Image getRenderedImage() {
		return ViewUtils.createImage(compound.getMolecule(), width, height, backgroundColor);
	}
	public int getWidth() { return width; }
	public void setDisplayName(String displayName) {this.displayName = displayName;}
	public void setFitRatio(float ratio) {this.fitRatio = ratio;}
	public void setHeight(int height) {this.height = height;}
	public void setIdentifier(Long id) {this.id = id;}
	public void setWidth(int width) {this.width = width;}
	public String toSerializableString() { return this.id.toString()+","+displayName; }

}
