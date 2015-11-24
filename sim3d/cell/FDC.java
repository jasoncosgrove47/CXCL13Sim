package sim3d.cell;
/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

//package sim.app.woims;

import sim.engine.*;
import sim.field.continuous.Continuous3D;

import javax.media.j3d.TransformGroup;

import sim.portrayal3d.simple.SpherePortrayal3D;
import sim.util.Double3D;
import sim.util.Int3D;
import sim3d.Options;
import sim3d.collisiondetection.Collidable;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.diffusion.Particle;

/**
 * An FDC agent. Represents the nucleus of the FDC, and handles the secretion of
 * chemokine to the particle grid.
 * 
 * @author Simon Jarrett - {@link simonjjarrett@gmail.com}
 */
public class FDC extends DrawableCell3D implements Steppable, Collidable
{
	/**
	 * The drawing environment that houses this cell; used by
	 * DrawableCell3D.setObjectLocation
	 */
	public static Continuous3D	drawEnvironment;
								
	private static final long	serialVersionUID	= 1;
													
	@Override
	public void addCollisionPoint( Int3D i3Point )
	{
		// We're not interested in collisions as we're static
		return;
	}
	
	@Override
	public CLASS getCollisionClass()
	{
		return CLASS.STROMA;
	}
	
	@Override
	public Continuous3D getDrawEnvironment()
	{
		return drawEnvironment;
	}
	
	public TransformGroup getModel( Object obj, TransformGroup transf )
	{
		if ( transf == null )
		{
			transf = new TransformGroup();
			
			SpherePortrayal3D s = new SpherePortrayal3D( Options.FDC.DRAW_COLOR(), Options.FDC.STROMA_NODE_RADIUS * 2,
					6 );
			s.setCurrentFieldPortrayal( getCurrentFieldPortrayal() );
			TransformGroup localTG = s.getModel( obj, null );
			
			localTG.setCapability( TransformGroup.ALLOW_TRANSFORM_WRITE );
			transf.addChild( localTG );
		}
		return transf;
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
		cgGrid.addSphereToGrid( this, new Double3D( x, y, z ), Options.FDC.STROMA_NODE_RADIUS );
	}
	
	@Override
	public void step( final SimState state )
	{
		Particle.add( Particle.TYPE.CXCL13, (int) x, (int) y, (int) z, Options.FDC.CXCL13_EMITTED() );
	}
}
