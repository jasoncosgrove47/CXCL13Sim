package sim3d.cell;

import java.awt.Color;
import java.util.HashSet;

import javax.media.j3d.TransformGroup;

import org.w3c.dom.Document;

import sim.engine.SimState;
import sim.portrayal3d.simple.SpherePortrayal3D;
import sim.util.Int3D;
import sim3d.Grapher;
import sim3d.Settings;
import sim3d.collisiondetection.Collidable;
import sim3d.collisiondetection.CollisionGrid;

public class cognateBC extends BC
{
	
	
	public boolean				displayAntigenGraph			= false;
	
	private int antigenCaptured = 0;
	/**
	 * Constructor
	 * @param seed  Used by MASON for the random seed
	 */
	public cognateBC( )
	{
		this.type = TYPE.NAIVE;
	}
	
	@Override
	public void step( final SimState state )
	{
		super.step(state);
	}
	
	/**
	 * ENUM for the chemokine types
	 */
	public enum TYPE
	{
		NAIVE, PRIMED, ACTIVATED
	}
	
	public TYPE type;
	
	
	/**
	 * Need more comments
	 */
	@Override
	public void handleCollisions( CollisionGrid cgGrid )
	{
		// don't let a b cell collide more than collisionThreshold times
		int collisionThreshold = 10;
		if ( m_i3lCollisionPoints.size() == 0 || collisionCounter > collisionThreshold)
		{
			return;
		}
				
		HashSet<Collidable> csCollidables = new HashSet<Collidable>(); // stores values uniquely in a hashset
		
		// Add all the cells to the set
		for ( Int3D i3Point : m_i3lCollisionPoints )
		{
			for ( Collidable cCollidable : cgGrid.getPoints( i3Point ) )
			{
				csCollidables.add( cCollidable );
			}
		}
		
		int iCollisionMovement = m_d3aMovements.size(); 
		boolean bCollision = false;
		
		// To keep a track of where we collided - we are only interested in the
		// first collision so we can ignore anything after this
		for ( Collidable cCell : csCollidables )
		{
			switch ( cCell.getCollisionClass() )
			{
				case STROMA_EDGE: // These first two are the more likely hits as they won't be moving
					if ( collideStromaEdge( (StromaEdge) cCell, iCollisionMovement ) )//TODO we can get T from this method
					{
						iCollisionMovement = m_d3aMovements.size() - 1;
						acquireAntigen(cCell);
						bCollision = true;
					}
					break;
				case STROMA:
					break;
				case BC:
					break;
			}
		}
		if ( bCollision ) //if the cell has collided
		{
			performCollision(cgGrid, iCollisionMovement); //deal with the collision
		}
	}
	
	
	/**
	 * Acquire antigen from a B cell
	 * TODO update to account for heterogeneity at some point
	 * @param cCell
	 */
	private void acquireAntigen(Collidable cCell){
		StromaEdge sEdge = (StromaEdge) cCell; 

		//we divide the stroma in two as make it more accurate
		if(this.getPositionAlongStroma()> 0.5)
		{
			if (sEdge.getAntigenLevelUpperEdge() > 0)
			{
				sEdge.setAntigenLevelUpperEdge(sEdge.getAntigenLevelUpperEdge() - 1);			
			}
		}
		else
		{
			if (sEdge.getAntigenLevelLowerHalf() > 0)
			{
				sEdge.setAntigenLevelLowerHalf(sEdge.getAntigenLevelLowerHalf() - 1);			
			}
		}
		this.setAntigenCaptured(this.getAntigenCaptured() + 1);
		this.type = TYPE.PRIMED;
		
		if ( displayAntigenGraph )
		{
		  Grapher.updateAntigenGraph( this.getAntigenCaptured()); //this gives an error when run on console
		}
		
	}
	
	
	@Override
	public TransformGroup getModel( Object obj, TransformGroup transf )
	{
		// We choose to always recalculate this model because the movement
		// changes in each time step.
		// Removing the movement indicators and removing this true will make the
		// 3d display a lot faster
		if ( transf == null || true )
		{
			transf = new TransformGroup();
			
			// Draw the BC itself
		 	java.awt.Color blue 	= new Color(30,200,255,255);
			
			SpherePortrayal3D s = new SpherePortrayal3D( blue, Settings.BC.COLLISION_RADIUS * 2, 6 );
			s.setCurrentFieldPortrayal( getCurrentFieldPortrayal() );
			TransformGroup localTG = s.getModel( obj, null );
			
			localTG.setCapability( TransformGroup.ALLOW_TRANSFORM_WRITE );
			transf.addChild( localTG );
			
			//if we have had any collisions, draw them as red circles
			modelCollisions(m_d3aCollisions,obj, transf);
			
			// If we have any movement, then draw it as white lines telling us where the cell is orientated
			modelMovements(m_d3aMovements,obj, transf);
		}
		return transf;
	}

	public int getAntigenCaptured() {
		return antigenCaptured;
	}

	public void setAntigenCaptured(int antigenCaptured) {
		this.antigenCaptured = antigenCaptured;
	}
	
	

}
