package sim3d.cell;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Group;
import javax.media.j3d.LineArray;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;

import sim.field.continuous.Continuous2D;
import sim.field.continuous.Continuous3D;
import sim.portrayal.DrawInfo2D;
import sim.portrayal3d.simple.CylinderPortrayal3D;
import sim.portrayal3d.simple.Shape3DPortrayal3D;
import sim.portrayal3d.simple.SpherePortrayal3D;
import sim.util.Double3D;
import sim.util.Int3D;
import sim3d.Options;
import sim3d.collisiondetection.Collidable;
import sim3d.collisiondetection.CollisionGrid;

public class StromaEdge extends DrawableCell3D implements Collidable
{
	private static final long serialVersionUID = 1L;
	
	public static Continuous3D drawEnvironment;
	@Override public Continuous3D getDrawEnvironment(){
		return drawEnvironment;
	}

	public StromaEdge(Double3D d3Point1, Double3D d3Point2)
	{
		if ( d3Point1.z > d3Point2.z )
		{
			Double3D temp = d3Point1;
			d3Point1 = d3Point2;
			d3Point2 = temp;
		}
		
		x = d3Point1.x;
		y = d3Point1.y;
		z = d3Point1.z;

		m_d3Edge = d3Point2.subtract(d3Point1);
	}
	
	private Double3D m_d3Edge;
	
	public Double3D getPoint1()
	{
		return new Double3D(x, y, z);
	}
	
	public Double3D getPoint2()
	{
		return  new Double3D(x + m_d3Edge.x, y + m_d3Edge.y, z + m_d3Edge.z);
	}
	
    public final void draw(Object object,  final Graphics2D graphics, final DrawInfo2D info)
    {
    	Color transp = getColorWithDepth(Options.FDC.DRAW_COLOR());
    	//graphics.setColor(getColorWithDepth(Options.FDC.DRAW_COLOR()));
    	graphics.setPaint(new GradientPaint((float)Math.round(info.draw.x),
    										(float)Math.round(info.draw.y),
    										getColorWithDepth(Options.FDC.DRAW_COLOR()),
    										(float)Math.round(info.draw.x+info.draw.width*m_d3Edge.x/m_d3Edge.z),
    										(float)Math.round(info.draw.y+info.draw.height*m_d3Edge.y/m_d3Edge.z),
    										new Color(transp.getRed(), transp.getGreen(), transp.getBlue(), Math.max(0, (int)(transp.getAlpha()-255*m_d3Edge.z)))));
    	graphics.drawLine((int)Math.round(info.draw.x),
						  (int)Math.round(info.draw.y),
						  (int)Math.round(info.draw.x+info.draw.width*m_d3Edge.x),
						  (int)Math.round(info.draw.y+info.draw.height*m_d3Edge.y));
    }

	@Override
	public boolean isStatic() 
	{
		return true;
	}

	@Override
	public void registerCollisions(CollisionGrid cgGrid)
	{
		cgGrid.addLineToGrid(this, new Double3D(x, y, z), new Double3D(x+m_d3Edge.x, y+m_d3Edge.y, z+m_d3Edge.z), Options.FDC.STROMA_EDGE_RADIUS);
	}

	@Override
	public void addCollisionPoint(Int3D i3Point)
	{
		// We're not interested in collisions as we're static
		return;	
	}
	@Override
	public void handleCollisions(CollisionGrid cgGrid)
	{
		return;
	}

	@Override
	public CLASS getCollisionClass()
	{
		return CLASS.STROMA_EDGE;
	}
	
    public TransformGroup getModel(Object obj, TransformGroup transf)
    {
	    if(transf==null)
	    {
	    	transf = new TransformGroup();
	    	
        	LineArray lineArr = new LineArray(2, LineArray.COORDINATES);
        	lineArr.setCoordinate(0, new Point3d(0, 0, 0));
        	lineArr.setCoordinate(1, new Point3d(m_d3Edge.x, m_d3Edge.y, m_d3Edge.z));
        	
        	Appearance aAppearance = new Appearance();
        	Color col = Options.FDC.DRAW_COLOR();
        	aAppearance.setColoringAttributes(new ColoringAttributes(col.getRed()/255f, col.getGreen()/255f, col.getBlue()/255f, ColoringAttributes.FASTEST));
        	aAppearance.setTransparencyAttributes(new TransparencyAttributes(TransparencyAttributes.FASTEST, 0.4f));
        	/*lineArr.setColor(0, new Color3f(col.getRed()/255f, col.getGreen()/255f, col.getBlue()/255f));
        	lineArr.setColor(1, new Color3f(col.getRed()/255f, col.getGreen()/255f, col.getBlue()/255f));*/
        	LineAttributes la = new LineAttributes();
        	la.setLineWidth((float)Options.FDC.STROMA_EDGE_RADIUS*20);
        	aAppearance.setLineAttributes(la);
        	
        	Shape3D s3Shape = new Shape3D(lineArr, aAppearance);
        	
	        Shape3DPortrayal3D s = new Shape3DPortrayal3D(s3Shape, aAppearance);
	        s.setCurrentFieldPortrayal(getCurrentFieldPortrayal());
	        TransformGroup localTG = s.getModel(obj, null);
	        
	        localTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
	        transf.addChild(localTG);
	    }
	    return transf;
    }
}


















