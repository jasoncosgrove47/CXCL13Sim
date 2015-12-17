package sim3d.cell;

import java.awt.Color;
import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.LineArray;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Point3d;

import sim.field.continuous.Continuous3D;
import sim.portrayal3d.simple.Shape3DPortrayal3D;
import sim.util.Double3D;
import sim.util.Int3D;
import sim3d.Options;
import sim3d.collisiondetection.Collidable;
import sim3d.collisiondetection.CollisionGrid;

/**
 * An agent representing the edge of stroma. Used to represent the dendrites of
 * the FDCs for the purposes of display and collision.
 * 
 * @author Simon Jarrett - {@link simonjjarrett@gmail.com}
 */
public class StromaEdge extends DrawableCell3D implements Collidable
{
	/**
	 * The drawing environment that houses this cell; used by
	 * DrawableCell3D.setObjectLocation
	 */
	public static Continuous3D	drawEnvironment;
								
	/**
	 * A vector representing the movement from point 1 to point 2
	 */
	private Double3D			m_d3Edge;
								
	/**
	 * Constructor
	 * 
	 * @param d3Point1 Absolute value of the start point
	 * @param d3Point2 Absolute value of the end point
	 */
	public StromaEdge( Double3D d3Point1, Double3D d3Point2 )
	{
		
		// makes sure that first point is always lower on the z axis
		// Make sure it's ordered on the z index
		// TODO I can't remember why I did this... I think it might be a relic
		// from 2D projection
		if ( d3Point1.z > d3Point2.z )
		{
			Double3D temp = d3Point1;
			d3Point1 = d3Point2;
			d3Point2 = temp;
		}
		
		//location of stroma is static so easiest to specify it's location in the constructor

		x = d3Point1.x;
		y = d3Point1.y;
		z = d3Point1.z;
		
		m_d3Edge = d3Point2.subtract( d3Point1 );
	}
	
	
	
	
	@Override
	public void addCollisionPoint( Int3D i3Point )
	{
		// We're not interested in collisions as we're static
		return;
	}
	
	@Override
	public CLASS getCollisionClass()
	{
		return CLASS.STROMA_EDGE;
	}
	
	@Override
	public Continuous3D getDrawEnvironment()
	{
		return drawEnvironment;
	}
	
	/*
	 * This method creates a 3d model of stromal edge for visualisation
	 * (non-Javadoc)
	 * @see sim.portrayal3d.SimplePortrayal3D#getModel(java.lang.Object, javax.media.j3d.TransformGroup)
	 */
	@Override
	public TransformGroup getModel( Object obj, TransformGroup transf )
	{
		if ( transf == null )
		{
			transf = new TransformGroup();
			
			LineArray lineArr = new LineArray( 2, LineArray.COORDINATES );
			lineArr.setCoordinate( 0, new Point3d( 0, 0, 0 ) );
			lineArr.setCoordinate( 1, new Point3d( m_d3Edge.x, m_d3Edge.y, m_d3Edge.z ) );
			
			Appearance aAppearance = new Appearance();
			Color col = Options.FDC.DRAW_COLOR();
			aAppearance.setColoringAttributes( new ColoringAttributes( col.getRed() / 255f, col.getGreen() / 255f,
					col.getBlue() / 255f, ColoringAttributes.FASTEST ) );
			aAppearance.setTransparencyAttributes( new TransparencyAttributes( TransparencyAttributes.FASTEST, 0.4f ) );
			
			LineAttributes la = new LineAttributes();
			la.setLineWidth( (float) Options.FDC.STROMA_EDGE_RADIUS * 20 );
			aAppearance.setLineAttributes( la );
			
			Shape3D s3Shape = new Shape3D( lineArr, aAppearance );
			
			Shape3DPortrayal3D s = new Shape3DPortrayal3D( s3Shape, aAppearance );
			s.setCurrentFieldPortrayal( getCurrentFieldPortrayal() );
			TransformGroup localTG = s.getModel( obj, null );
			
			localTG.setCapability( TransformGroup.ALLOW_TRANSFORM_WRITE );
			transf.addChild( localTG );
		}
		return transf;
	}
	
	/**
	 * Accessor for point 1
	 */
	public Double3D getPoint1()
	{
		return new Double3D( x, y, z );
	}
	
	/**
	 * Accessor for point 2
	 */
	public Double3D getPoint2()
	{
		return new Double3D( x + m_d3Edge.x, y + m_d3Edge.y, z + m_d3Edge.z );
	}
	
	@Override
	public void handleCollisions( CollisionGrid cgGrid )
	{
		return;
	}
	
	@Override
	public boolean isStatic()
	{
		return true;
	}
	
	@Override
	public void registerCollisions( CollisionGrid cgGrid )
	{
		cgGrid.addLineToGrid( this, new Double3D( x, y, z ),
				new Double3D( x + m_d3Edge.x, y + m_d3Edge.y, z + m_d3Edge.z ), Options.FDC.STROMA_EDGE_RADIUS );
	}
}
