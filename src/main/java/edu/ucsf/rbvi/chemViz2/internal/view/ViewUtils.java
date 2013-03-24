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
import java.util.List;

import org.cytoscape.view.presentation.customgraphics.PaintedShape;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.generators.BasicAtomGenerator;
import org.openscience.cdk.renderer.generators.BasicBondGenerator;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.RingGenerator;
import org.openscience.cdk.renderer.generators.IGenerator;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;

public class ViewUtils {
	public static Image createImage(IAtomContainer iMolecule, int width, int height,
	                                Paint background) {
		if (iMolecule == null || width == 0 || height == 0) {
			return blankImage(iMolecule, width, height);
		}

		int renderWidth = width;
		if (renderWidth < 200) renderWidth = 200;
		int renderHeight = height;
		if (renderHeight < 200) renderHeight = 200;

		BufferedImage bufferedImage = 
			new BufferedImage(renderWidth, renderHeight, 
			                  BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bufferedImage.createGraphics();

		g2d.setColor((Color)background);
		g2d.setBackground((Color)background);
		g2d.fillRect(0,0,width,height);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
		                     RenderingHints.VALUE_ANTIALIAS_ON);

		AtomContainerRenderer renderer = getRenderer((Color)background);
		if (renderer == null)
			return null;

		Rectangle2D bbox = new Rectangle2D.Double(0,0,renderWidth,renderHeight);
		renderer.setup(iMolecule, new Rectangle(renderWidth, renderHeight));
		renderer.paint(iMolecule, new AWTDrawVisitor(g2d), bbox, true);

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
	                                              IAtomContainer mol, Paint background) {
		if (mol == null) return null;

		double boxSize = 100.0;

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
	}

	private static Image blankImage(IAtomContainer mol, int width, int height) {
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

	private static AtomContainerRenderer getRenderer(Color background) {
		// generators make the image elements
		List<IGenerator<IAtomContainer>> generators = 
			new ArrayList<IGenerator<IAtomContainer>>();
		generators.add(new BasicSceneGenerator());
		generators.add(new BasicBondGenerator());
		generators.add(new RingGenerator());
		generators.add(new BasicAtomGenerator());
       
		// the renderer needs to have a toolkit-specific font manager 
		AtomContainerRenderer renderer = 
			new AtomContainerRenderer(generators, new AWTFontManager());
		RendererModel model = renderer.getRenderer2DModel();

		if (background == null)
			background = new Color(255,255,255,255);

		// Set up our rendering parameters
		model.set(BasicSceneGenerator.UseAntiAliasing.class, true);
		model.set(BasicSceneGenerator.BackgroundColor.class, background);
		model.set(BasicBondGenerator.BondWidth.class, 2.0);
		model.set(RingGenerator.BondWidth.class, 2.0);
		model.set(BasicAtomGenerator.ColorByType.class, true);
		model.set(BasicAtomGenerator.ShowExplicitHydrogens.class, true);
		return renderer;
	}
}
