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
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics;
import org.cytoscape.view.presentation.customgraphics.CustomGraphicLayer;
import org.cytoscape.view.presentation.customgraphics.PaintedShape;

import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.elements.ArrowElement;
import org.openscience.cdk.renderer.elements.AtomSymbolElement;
import org.openscience.cdk.renderer.elements.Bounds;
import org.openscience.cdk.renderer.elements.ElementGroup;
import org.openscience.cdk.renderer.elements.GeneralPath;
import org.openscience.cdk.renderer.elements.IRenderingElement;
import org.openscience.cdk.renderer.elements.LineElement;
import org.openscience.cdk.renderer.elements.MarkedElement;
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

public class PaintedShapeVisitor implements IDrawVisitor {

	private AWTFontManager fontManager;
	private RendererModel rendererModel;
	private Color currentColor = Color.BLACK;
	private BasicStroke currentStroke = null;
	private List<PaintedShape> cgList;
	private AffineTransform transform;
	private AffineTransform scaleTransform;
	private AffineTransform offsetTransform;
	private double xOffset;
	private double yOffset;
	private	Paint backgroundColor;

	private Long id;
	private String displayName = "chemViz";

	/**
	 * Should we round coordinates to ints to circumvent graphical glitches from AWT.
	 */
	private boolean roundCoords = false;

	private final Map<Integer, BasicStroke> strokeMap = new HashMap<Integer, BasicStroke>();

	public RendererModel getRendererModel() {
		return rendererModel;
	}

	public Map<Integer, BasicStroke> getStrokeMap() {
		return strokeMap;
	}

	private final Map<TextAttribute, Object> map = new Hashtable<TextAttribute, Object>();

	public PaintedShapeVisitor(double x, double y, double scale, Paint backgroundColor) {
		this.fontManager = null;
		this.rendererModel = null;
		cgList = new ArrayList<PaintedShape>();
		map.put(TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUB);
		currentStroke = new BasicStroke(1.0f/4.0f);
		strokeMap.put(1, currentStroke);
		this.scaleTransform = AffineTransform.getScaleInstance(scale, scale);
		this.offsetTransform = AffineTransform.getTranslateInstance(x, y);
		this.xOffset = x;
		this.yOffset = y;

		if (backgroundColor == null)
			this.backgroundColor = Color.WHITE;
		else
			this.backgroundColor = backgroundColor;

		// System.out.println("PaintedShapeVisitor("+scale+","+backgroundColor+")");
	}

	public List<PaintedShape> getPaintedShapes() {
		return cgList;
	}

	public void setRounding(boolean val) {
		this.roundCoords = val;
	}

	// IDrawVisitor implementation -- public methods
	public void visit(IRenderingElement element) {
		// System.err.println("Visitor method for "
		//                     + element.getClass().getName() + " called");
		Color savedColor = currentColor;
		if (element instanceof ElementGroup)
			visit((ElementGroup) element);
		else if (element instanceof WedgeLineElement)
			visit((WedgeLineElement) element);
		else if (element instanceof LineElement)
			visit((LineElement) element);
		else if (element instanceof OvalElement)
			visit((OvalElement) element);
		else if (element instanceof TextGroupElement)
			visit((TextGroupElement) element);
		else if (element instanceof AtomSymbolElement)
			visit((AtomSymbolElement) element);
		else if (element instanceof TextElement)
			visit((TextElement) element);
		else if (element instanceof RectangleElement)
			visit((RectangleElement) element);
		else if (element instanceof PathElement)
			visit((PathElement) element);
		else if (element instanceof GeneralPath)
			visit((GeneralPath)element);
		else if (element instanceof ArrowElement)
			visit((ArrowElement) element);
		else if (element instanceof Bounds)
			visit((Bounds) element);
		else if (element instanceof MarkedElement)
			visit(((MarkedElement) element).element());
		else
			System.err.println("Visitor method for "
			                    + element.getClass().getName() + " is not implemented");
		currentColor = savedColor;
	}

	public void setFontManager(IFontManager fontManager) {
		this.fontManager = (AWTFontManager) fontManager;
	}

	public void setRendererModel(RendererModel rendererModel) {
		this.rendererModel = rendererModel;
	}

	public void setTransform(AffineTransform transform) {
		this.transform = transform;
	}

	// IDrawVisitor implementation -- private methods
	private void visit(ElementGroup elementGroup) {
		elementGroup.visitChildren(this);
	}

	private void visit(LineElement line) {
		float width = (float) (line.width * transform.getScaleX());

		BasicStroke stroke = new BasicStroke(width*1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

		double[] coordinates = new double[]{line.firstPointX, line.firstPointY, line.secondPointX, line.secondPointY};
		transform.transform(coordinates, 0, coordinates, 0, 2);
		Line2D lineShape;
		if (roundCoords) {
			lineShape = new Line2D.Double(Math.round(coordinates[0]), Math.round(coordinates[1]),
			                              Math.round(coordinates[2]), Math.round(coordinates[3]));
		} else {
			lineShape = new Line2D.Double(coordinates[0], coordinates[1],
			                              coordinates[2], coordinates[3]);
		}
		// Shape s = scaleTransform.createTransformedShape(lineShape);
		Shape s = createTransformedShape(lineShape);
		PaintedShape layer = new MyPaintedShape(s, currentColor, stroke, null);
		cgList.add(layer);
	}

	private void visit(OvalElement oval) {
		// System.out.println("OvalElement");
		double radius = scaleX(oval.radius);
		double diameter = scaleX(oval.radius * 2);
		Paint p = null;

		Ellipse2D e = new Ellipse2D.Double(transformX(oval.xCoord) - radius,
		                                   transformY(oval.yCoord) - radius,
		                                   diameter, diameter);
		
		if (oval.fill) {
			p = oval.color;
		} 

		PaintedShape layer = new MyPaintedShape(e, p, currentStroke, currentColor);
		cgList.add(layer);
	}

	private void visit(RectangleElement rectangle) {
		Paint p = null;
		// System.out.println("RectangleElement");
		double[] point1 = transform(rectangle.xCoord, rectangle.yCoord);
		double[] point2 = transform(rectangle.xCoord+rectangle.width, 
		                            rectangle.yCoord+rectangle.height);
		if (rectangle.filled)
			p = rectangle.color;

		Rectangle2D rect = new Rectangle2D.Double(point1[0], point1[1], point2[0]-point1[0], point2[1]-point1[1]);
		// PaintedShape c = new MyPaintedShape(scaleTransform.createTransformedShape(rect), p, currentStroke, currentColor);
		PaintedShape c = new MyPaintedShape(createTransformedShape(rect), p, currentStroke, currentColor);
		cgList.add(c);
	}

	private void visit(PathElement path) {
		// System.out.println("PathElement");
		Path2D pathShape = new Path2D.Double();
		for (int i = 1; i < path.points.size(); i++) {
			Point2d point1 = path.points.get(i-1);
			Point2d point2 = path.points.get(i);
			double[] start = transform(point1.x, point1.y);
			double[] end = transform(point2.x, point2.y);
			pathShape.moveTo(start[0], start[1]);
			pathShape.lineTo(end[0], end[1]);
		}

		PaintedShape c = new MyPaintedShape(createTransformedShape(pathShape), null, 
		                                    currentStroke, path.color);
		cgList.add(c);
	}

	private void visit(GeneralPath path) {
		// System.out.println("GeneralPathElement");
		java.awt.geom.GeneralPath generalPath = new java.awt.geom.GeneralPath();
		generalPath.append( getPathIterator(path, transform), false);

		PaintedShape c = new MyPaintedShape(createTransformedShape(generalPath), path.color, 
		                                    null, null);
		cgList.add(c);
	}

	private void visit(AtomSymbolElement atomSymbol) {
		Font f1 = fontManager.getFont();
		Font font = f1.deriveFont(f1.getSize2D()*.5f); // Shrink the font a little for this
		FontRenderContext frc = new FontRenderContext(null, false, false);
		TextLayout tl = new TextLayout(atomSymbol.text, font, frc);
		Shape textShape = tl.getOutline(null);
		Rectangle2D textBounds = textShape.getBounds2D();
		double textWidth = textBounds.getWidth();
		double textHeight = textBounds.getHeight();

		// XXX Check to make sure this is right....
		double textStartX = transformX(atomSymbol.xCoord) - textWidth/2;
		double textStartY = transformY(atomSymbol.yCoord) + textHeight/2;
		createTextCustomGraphics(textShape, atomSymbol.color, textStartX, textStartY, true);

		// TODO: Handle formal charges...
		double offset = 10;    // XXX
		String chargeString;
		if (atomSymbol.formalCharge == 0) {
			return;
		} else if (atomSymbol.formalCharge == 1) {
			chargeString = "+";
		} else if (atomSymbol.formalCharge > 1) {
			chargeString = atomSymbol.formalCharge + "+";
		} else if (atomSymbol.formalCharge == -1) {
			chargeString = "-";
		} else if (atomSymbol.formalCharge < -1) {
			int absCharge = Math.abs(atomSymbol.formalCharge);
			chargeString = absCharge + "-";
		} else {
			return;
		}

		TextLayout cl = new TextLayout(chargeString, font, frc);
		Shape chargeShape = cl.getOutline(null);

		double xCoord = textBounds.getCenterX();
		double yCoord = textBounds.getCenterY();
		if (atomSymbol.alignment == 1) {           // RIGHT
			createTextCustomGraphics(chargeShape, atomSymbol.color, xCoord+offset, textBounds.getMinY(), false);
		} else if (atomSymbol.alignment == -1) {   // LEFT
			createTextCustomGraphics(chargeShape, atomSymbol.color, xCoord-offset, textBounds.getMinY(), false);
		} else if (atomSymbol.alignment == 2) {    // TOP
			createTextCustomGraphics(chargeShape, atomSymbol.color, xCoord, yCoord-offset, false);
		} else if (atomSymbol.alignment == -2) {   // BOT
			createTextCustomGraphics(chargeShape, atomSymbol.color, xCoord, yCoord+offset, false);
		}
	}

	private void visit(TextElement textElement) {
		// System.out.println("TextElement");
		Font f1 = fontManager.getFont();
		Font font = f1.deriveFont(f1.getSize2D()*.5f); // Shrink the font a little for this
		FontRenderContext frc = new FontRenderContext(null, false, false);
		TextLayout tl = new TextLayout(textElement.text, font, frc);
		Shape textShape = tl.getOutline(null);
		Rectangle2D textBounds = textShape.getBounds2D();
		double textWidth = textBounds.getWidth();
		double textHeight = textBounds.getHeight();

		// XXX Check to make sure this is right....
		double textStartX = transformX(textElement.xCoord) - textWidth/2;
		double textStartY = transformY(textElement.yCoord) + textHeight/2;
		createTextCustomGraphics(textShape, textElement.color, textStartX, textStartY, true);
	}

	private void visit(TextGroupElement textGroup) {
		// System.out.println("TextGroupElement");
		Font f1 = fontManager.getFont();
		Font font = f1.deriveFont(f1.getSize2D()*.5f); // Shrink the font a little for this
		FontRenderContext frc = new FontRenderContext(null, false, false);
		TextLayout tl = new TextLayout(textGroup.text, font, frc);
		Shape textShape = tl.getOutline(null);
		Rectangle2D textBounds = textShape.getBounds2D();
		double textWidth = textBounds.getWidth();
		double textHeight = textBounds.getHeight();

		double textStartX = transformX(textGroup.xCoord) - textWidth/2;
		double textStartY = transformY(textGroup.yCoord) + textHeight/2;
		createTextCustomGraphics(textShape, textGroup.color, textStartX, textStartY, true);

		double xCoord = textBounds.getCenterX();
		double yCoord = textBounds.getCenterY();
		double xCoord1 = textBounds.getMinX();
		double yCoord1 = textBounds.getMinY();
		double xCoord2 = textStartX + textBounds.getWidth();
		double yCoord2 = textBounds.getMaxY();

		double oWidth = xCoord2 - xCoord1;
		double oHeight = yCoord2 - yCoord1;
		for (TextGroupElement.Child child : textGroup.children) {
			double childx;
			double childy;

			switch (child.position) {
				case NE:
					childx = xCoord2;
					childy = yCoord1;
					break;
				case N:
					childx = xCoord1;
					childy = yCoord1;
					break;
				case NW:
					childx = xCoord1 - oWidth;
					childy = yCoord1;
					break;
				case W:
					childx = xCoord1 - oWidth;
					childy = textStartY;
					break;
				case SW:
					childx = xCoord1 - oWidth;
					childy = yCoord1 + oHeight;
					break;
				case S:
					childx = xCoord1;
					childy = yCoord2 + oHeight;
					break;
				case SE:
					childx = xCoord2;
					childy = yCoord2 + oHeight;
					break;
				case E:
					childx = xCoord2;
					childy = textStartY;
					break;
				default:
					childx = xCoord;
					childy = yCoord;
					break;
			}
	
			TextLayout tlChild = new TextLayout(child.text, font, frc);
			Shape childShape = tlChild.getOutline(null);
			createTextCustomGraphics(childShape, textGroup.color, childx, childy, false);
			if (child.subscript != null) {
				Rectangle2D childBounds = childShape.getBounds2D();
				double scx = (childx + (childBounds.getWidth() * 0.75));
				double scy = (childy + (childBounds.getHeight() / 3));
				Font subscriptFont = font.deriveFont(font.getStyle(), font.getSize() - 2);
				TextLayout tlSub = new TextLayout(child.subscript, subscriptFont, frc);
				Shape subShape = tlSub.getOutline(null);
				createTextCustomGraphics(subShape, textGroup.color, childx, childy, false);
			}
		}
	}

	private void visit(ArrowElement line) {
		System.out.println("ArrowElement");
		double scale = rendererModel.getParameter(Scale.class).getValue();

		float w = (float) (line.width * scale);

		BasicStroke stroke = new BasicStroke(w*1.5f);

		Path2D path = new Path2D.Double();

		double[] start = transform(line.startX, line.startY);
		double[] end = transform(line.endX, line.endY);
		path.moveTo(start[0], start[1]);
		path.lineTo(end[0], end[1]);

		double aW = rendererModel.getParameter( ArrowHeadWidth.class).getValue() / scale;
		if(line.direction){
			double[] c = transform(line.startX-aW, line.startY-aW);
			double[] d = transform(line.startX-aW, line.startY+aW);
			path.moveTo(start[0], start[1]);
			path.lineTo(c[0], c[1]);
			path.moveTo(start[0], start[1]);
			path.lineTo(d[0], d[1]);
		}else{
			double[] c = transform(line.endX+aW, line.endY-aW);
			double[] d = transform(line.endX+aW, line.endY+aW);
			path.lineTo(c[0], c[1]);
			path.moveTo(end[0], end[1]);
			path.lineTo(d[0], d[1]);
		}
		PaintedShape c = new MyPaintedShape(createTransformedShape(path), null, 
		                                    stroke, line.color);
		cgList.add(c);
	}

	private void visit(WedgeLineElement wedge) {
		// System.out.println("WedgeElement");
		// make the vector normal to the wedge axis
		Vector2d normal =
		     new Vector2d(wedge.firstPointY - wedge.secondPointY, wedge.secondPointX - wedge.firstPointX);
		normal.normalize();
		normal.scale(rendererModel.getParameter(WedgeWidth.class).getValue()
		             / rendererModel.getParameter(Scale.class).getValue());

		// make the triangle corners
		Point2d vertexA = new Point2d(wedge.firstPointX, wedge.firstPointY);
		Point2d vertexB = new Point2d(wedge.secondPointX, wedge.secondPointY);
		Point2d vertexC = new Point2d(vertexB);
		vertexB.add(normal);
		vertexC.sub(normal);
		if (wedge.type == WedgeLineElement.TYPE.DASHED) {
			this.drawDashedWedge(vertexA, vertexB, vertexC, wedge.color);
		} else {
			this.drawFilledWedge(vertexA, vertexB, vertexC, wedge.color);
		}
	}

	private void visit (Bounds bounds) {
		// Not sure we want to do anything here...
	}

	private void visit (MarkedElement element) {
		// Not sure we want to do anything here...
	}

	private double scaleX(double xCoord) {
		return xCoord*transform.getScaleX();
	}

	private double transformX(double xCoord) {
		return transform(xCoord, 1)[0];
	}

	private double transformY(double yCoord) {
		return transform(1, yCoord)[1];
	}

	private double[] transform(double xCoord, double yCoord) {
		double[] result = new double[2];
		transform.transform( new double[] {xCoord, yCoord}, 0, result, 0, 1);
		return result;
	}

	private void createTextCustomGraphics(Shape textShape, Color textColor, 
	                                      double textStartX, double textStartY, boolean paintBackground) {
		double textWidth = textShape.getBounds2D().getWidth();
		double textHeight = textShape.getBounds2D().getHeight();
		AffineTransform trans = new AffineTransform();
		trans.translate(textStartX, textStartY);
		Shape transShape = trans.createTransformedShape(textShape);
		Shape scaledShape = createTransformedShape(transShape);

		if (paintBackground && !textColor.equals(backgroundColor)) {
			// Paint the background
			Rectangle2D bbox = transShape.getBounds2D();
			Rectangle2D bg = new Rectangle2D.Double(bbox.getX()-.5, bbox.getY()-.5, bbox.getWidth()+1, bbox.getHeight()+1);
			Paint pb = (Color)backgroundColor;
			PaintedShape cb = new MyPaintedShape(createTransformedShape(bg), pb, null, null);
			cgList.add(cb);
		}

		PaintedShape c = new MyPaintedShape(createTransformedShape(scaledShape), textColor, 
		                                    null, null);
		cgList.add(c);
	}

	private static PathIterator getPathIterator(final GeneralPath path,final AffineTransform transform) {
		return new PathIterator() {

			int index;

			private int type(Type type) {
				switch ( type ) {
					case MoveTo: return SEG_MOVETO;
					case LineTo: return SEG_LINETO;
					case QuadTo: return SEG_QUADTO;
					case CubicTo: return SEG_CUBICTO;
					case Close: return SEG_CLOSE;
					default: return SEG_CLOSE;
				}
			}
			public void next() { index++; }

			public boolean isDone() {
				return index>= path.elements.size();
			}

			public int getWindingRule() { return WIND_EVEN_ODD; }

			public int currentSegment( double[] coords ) {
				float[] src = new float[6];
				int type = currentSegment( src );
				double[] srcD = coords;
				for(int i=0;i<src.length;i++){
					srcD[i] = (double) src[i];
				}
				return type;
			}

			public int currentSegment( float[] coords ) {
				float[] src = path.elements.get( index ).points();
				transform.transform( src, 0, coords, 0, src.length/2 );
				return type(path.elements.get( index ).type());
			}
		};
	}

	private void drawFilledWedge(Point2d vertexA, Point2d vertexB, Point2d vertexC, Color clr) {
		double[] pointB = transform(vertexB.x, vertexB.y);
		double[] pointC = transform(vertexC.x, vertexC.y);
		double[] pointA = transform(vertexA.x, vertexA.y);

		int[] xCoords = new int[] { (int)pointB[0], (int)pointC[0], (int)pointA[0] };
		int[] yCoords = new int[] { (int)pointB[1], (int)pointC[1], (int)pointA[1] };
		Shape wedge = new Polygon(xCoords, yCoords, 3);
		PaintedShape c = new MyPaintedShape(createTransformedShape(wedge), clr, null, null);
		cgList.add(c);
	}

	private void drawDashedWedge(Point2d vertexA, Point2d vertexB, Point2d vertexC, Color clr) {
		// store the current stroke
		BasicStroke stroke = new BasicStroke(1.0f/4.0f);
		Path2D path = new Path2D.Double();

		// calculate the distances between lines
		double distance = vertexB.distance(vertexA);
		double gapFactor = 0.1;
		double gap = distance * gapFactor;
		double numberOfDashes = distance / gap;
		double displacement = 0;

		// draw by interpolating along the edges of the triangle
		for (int i = 0; i < numberOfDashes; i++) {
			Point2d point1 = new Point2d();
			point1.interpolate(vertexA, vertexB, displacement);
			Point2d point2 = new Point2d();
			point2.interpolate(vertexA, vertexC, displacement);
			double[] p1T = transform(point1.x, point1.y);
			double[] p2T = transform(point2.x, point2.y);
			path.moveTo(p1T[0], p1T[1]);
			path.lineTo(p2T[0], p2T[1]);
			if (distance * (displacement + gapFactor) >= distance) {
				break;
			} else {
				displacement += gapFactor;
			}
		}

		// PaintedShape c = new MyPaintedShape(createTransformedShape(path), clr, stroke, clr);
		PaintedShape c = new MyPaintedShape(createTransformedShape(path), clr, null, null);
		cgList.add(c);
	}

	private Shape createTransformedShape(Shape s) {
		Shape t = scaleTransform.createTransformedShape(s);
		return offsetTransform.createTransformedShape(t);
	}

}
