package edu.ucsf.rbvi.chemViz2.internal.view;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;


import org.cytoscape.view.presentation.customgraphics.CustomGraphicLayer;
import org.cytoscape.view.presentation.customgraphics.PaintedShape;

public class MyPaintedShape implements PaintedShape {
	Shape shape;
	Paint shapePaint;
	Stroke stroke;
	Paint strokePaint;
	protected Rectangle2D bounds;

	public MyPaintedShape(Shape shape, Paint shapePaint, Stroke stroke, Paint strokePaint) {
		this.shape = shape;
		this.shapePaint = shapePaint;
		if (stroke != null)
			this.shape = stroke.createStrokedShape(shape);
		else
			this.shape = shape;
		this.bounds = new Rectangle2D.Double(0.0,0.0,100.0,100.0);
	}

	public Paint getPaint() { return shapePaint; }
	public Paint getPaint(Rectangle2D bounds) { return shapePaint; }
	public Shape getShape() { return shape; }
	public Stroke getStroke() { return null; }
	public Paint getStrokePaint() { return null; }
	public Rectangle2D getBounds2D() { return bounds; }

	public CustomGraphicLayer transform(AffineTransform xform) {
		// System.out.println("Shape: "+toString()+" Got transform: "+xform);
		Shape newBounds = xform.createTransformedShape(bounds);

		// In general, it's a bad idea to allow our structures to stretch in strange ways...
		// Make sure the transformation is isoscale
		double[] matrix = new double[6];
		xform.getMatrix(matrix);

		double scale = matrix[0];
		if (matrix[0] != matrix[3]) {
			scale = Math.min(matrix[0], matrix[3]);
		}

		matrix[0] = scale;
		matrix[3] = scale;

		AffineTransform newXform = new AffineTransform(matrix);

		MyPaintedShape mps = new MyPaintedShape(newXform.createTransformedShape(shape), shapePaint, stroke, strokePaint);
		mps.bounds = newBounds.getBounds2D();
		return mps;
	}

	public String toString() {
		return "shape="+shape+" shapeColor="+shapePaint+" stroke="+stroke+" strokePaint="+strokePaint;
	}
}
