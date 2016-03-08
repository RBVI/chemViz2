package edu.ucsf.rbvi.chemViz2.internal.view;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.cytoscape.view.presentation.customgraphics.PaintedShape;

import org.openscience.cdk.ChemModel;
import org.openscience.cdk.ReactionSet;
// import org.openscience.cdk.depict.DepictionGenerator;
// import org.openscience.cdk.depict.Depiction;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IReaction;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.ChemModelRenderer;
import org.openscience.cdk.renderer.ReactionRenderer;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.SymbolVisibility;
import org.openscience.cdk.renderer.color.CDK2DAtomColors;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.generators.BasicAtomGenerator;
import org.openscience.cdk.renderer.generators.BasicBondGenerator;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.ReactionArrowGenerator;
import org.openscience.cdk.renderer.generators.ReactionPlusGenerator;
import org.openscience.cdk.renderer.generators.ReactionSceneGenerator;
import org.openscience.cdk.renderer.generators.RingGenerator;
import org.openscience.cdk.renderer.generators.IGenerator;
import org.openscience.cdk.renderer.generators.standard.StandardGenerator;
import org.openscience.cdk.renderer.generators.standard.StandardGenerator.Visibility;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;

import edu.ucsf.rbvi.chemViz2.internal.depict.Depiction;
import edu.ucsf.rbvi.chemViz2.internal.depict.DepictionGenerator;

public class ViewUtils {

	public static Image createImage(IReaction iReaction, int width, int height,
	                                Paint background, boolean showLabel) {
		if (iReaction == null || width == 0 || height == 0) {
			return blankImage(width, height);
		}

		int renderWidth = width;
		// if (renderWidth < 200) renderWidth = 200;
		int renderHeight = height;
		// if (renderHeight < 200) renderHeight = 200;

		DepictionGenerator generator = 
					getDepictionGenerator(renderWidth, renderHeight, (Color)background, showLabel);
		BufferedImage bufferedImage = null;
		try {
			Depiction depiction = generator.depict(iReaction);
			bufferedImage =  depiction.toImg();
		} catch (CDKException cdke) {
			return blankImage(width, height);
		}

		if (renderWidth != width || renderHeight != height) {
			AffineTransform tx = new AffineTransform();
			if (width < height) {
				tx.scale((double)width/(double)renderWidth, 
				         (double)width/(double)renderWidth);
			} else {
				tx.scale((double)height/(double)renderHeight, 
				         (double)height/(double)renderHeight);
			}

			AffineTransformOp op = 
				new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
			bufferedImage = op.filter(bufferedImage, null);
		}
		return bufferedImage;
	}

	public static Image createImage(IAtomContainer iMolecule, int width, int height,
	                                Paint background, boolean showLabel) {
		if (iMolecule == null || width == 0 || height == 0) {
			return blankImage(width, height);
		}

		int renderWidth = width;
		int renderHeight = height;

		DepictionGenerator generator = 
					getDepictionGenerator(renderWidth, renderHeight, (Color)background, showLabel);
		BufferedImage bufferedImage = null;
		try {
			Depiction depiction = generator.depict(iMolecule);
			bufferedImage =  depiction.toImg();
		} catch (CDKException cdke) {
			return blankImage(width, height);
		}

		if (renderWidth != width || renderHeight != height) {
			AffineTransform tx = new AffineTransform();
			if (width < height) {
				tx.scale((double)width/(double)renderWidth, 
				         (double)width/(double)renderWidth);
			} else {
				tx.scale((double)height/(double)renderHeight, 
				         (double)height/(double)renderHeight);
			}

			AffineTransformOp op = 
				new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
			bufferedImage = op.filter(bufferedImage, null);
		}
		return bufferedImage;
	}

	public static List<PaintedShape> createShapes(double x, double y, double width, double height,
	                                              IReaction reaction, Paint background) {
		if (reaction == null) return null;

		double boxSize = 100.0;

		DepictionGenerator generator = 
					getDepictionGenerator((int)width, (int)height, (Color)background, false).
		                            withPadding(1.5);

		try {
			double scale = Math.min(width/boxSize, height/boxSize);
			Depiction depiction = generator.depict(reaction);
			PaintedShapeVisitor v = new PaintedShapeVisitor(x, y, 1.0, background);
			depiction.toShapes(v);
			List<PaintedShape> shapes = v.getPaintedShapes();
			return shapes;
		} catch (Exception e) {
			// TODO: Log message
			System.out.println("Unable to render molecule: "+e);
			return null;
		}
	}

	public static List<PaintedShape> createShapes(double x, double y, double width, double height,
	                                              IAtomContainer mol, Paint background) {
		if (mol == null) return null;

		// double boxSize = 100.0;

		DepictionGenerator generator = 
					getDepictionGenerator((int)width, (int)height, (Color)background, false).
		                            withPadding(1.5);
		try {
			// double scale = Math.min(width/boxSize, height/boxSize);
			Depiction depiction = generator.depict(mol);
			PaintedShapeVisitor v = new PaintedShapeVisitor(x, y, 1.0, background);
			depiction.toShapes(v);
			List<PaintedShape> shapes = v.getPaintedShapes();
			return shapes;
		} catch (Exception e) {
			// TODO: Log message
			System.out.println("Unable to render molecule: "+e);
			return null;
		}

/*
		try {
			AtomContainerRenderer renderer = getRenderer((Color)background);
			double scale = Math.min(width/boxSize, height/boxSize);
			Rectangle2D bbox = new Rectangle2D.Double(x/scale, y/scale, width/scale, height/scale);
			renderer.setup(mol, new Rectangle((int)width, (int)height));
			PaintedShapeVisitor v = new PaintedShapeVisitor(scale, background);
			renderer.paint(mol, v, bbox, true);
			return v.getPaintedShapes();
		} catch (Exception e) {
			// TODO: Log message
			System.out.println("Unable to render molecule: "+e);
			return null;
		}
*/
	}

	private static Image blankImage(int width, int height) {
		final String noImage = "Image Unavailable";

		if (width == 0 || height == 0)
			return null;

		BufferedImage bufferedImage = 
			new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = bufferedImage.createGraphics();
		graphics.setBackground(Color.WHITE);

		graphics.setColor(Color.WHITE);
		graphics.fillRect(0,0,width,height);
		graphics.setColor(Color.BLACK);

		// Create our font
		Font font = new Font("SansSerif", Font.PLAIN, 18);
		graphics.setFont(font);
		FontMetrics metrics = graphics.getFontMetrics();

		int length = metrics.stringWidth(noImage);
		while (length+6 >= width) {
			font = font.deriveFont((float)(font.getSize2D() * 0.9)); // Scale our font
			graphics.setFont(font);
			metrics = graphics.getFontMetrics();
			length = metrics.stringWidth(noImage);
		}

		int lineHeight = metrics.getHeight();

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
		                          RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.drawString(noImage, (width-length)/2, (height+lineHeight)/2);

		return bufferedImage;
	}

	private static DepictionGenerator getDepictionGenerator(int width, int height, 
	                                                        Color background, boolean showLabel) {
		DepictionGenerator dg = new DepictionGenerator()
		                                 .withAtomColors()
																		 .withBackgroundColor(background)
																		 .withSize(width, height)
																		 .withTerminalCarbons();
		if (showLabel) 
			return dg.withMolTitle().withRxnTitle().withTitleColor(Color.BLACK);
		else
			return dg;
	}

	private static AtomContainerRenderer getRenderer(Color background) {
		// generators make the image elements
		List<IGenerator<IAtomContainer>> generators = 
						new ArrayList<IGenerator<IAtomContainer>>();

		Font font = new Font("Arial", Font.PLAIN, 24);
		AtomContainerRenderer renderer = 
				new AtomContainerRenderer(Arrays.asList(new BasicSceneGenerator(),
				                                        new StandardGenerator(font)),
				                                        new AWTFontManager());

		RendererModel model = renderer.getRenderer2DModel();
		model.set(StandardGenerator.Visibility.class, SymbolVisibility.iupacRecommendations());
		model.set(StandardGenerator.AtomColor.class, new CDK2DAtomColors());
		model.set(StandardGenerator.StrokeRatio.class, 0.85);
		model.set(StandardGenerator.SymbolMarginRatio.class, 4d);

		if (background == null)
			background = new Color(255,255,255,255);

		return renderer;
	}
}
