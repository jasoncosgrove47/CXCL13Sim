package sim3d.cell;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.media.j3d.TransformGroup;

import org.w3c.dom.Document;

import dataLogger.Grapher;
import dataLogger.Controller;
import sim.engine.SimState;
import sim.portrayal3d.simple.SpherePortrayal3D;
import sim.util.Double3D;
import sim.util.Int3D;
import sim3d.Settings;
import sim3d.SimulationEnvironment;
import sim3d.consoleRun;
import sim3d.collisiondetection.Collidable;
import sim3d.collisiondetection.CollisionGrid;

public class cognateBC extends BC
{
	// stores position of cBC in each dimension for each timestep
	private ArrayList<Double> positionX  = new ArrayList<Double>();
	private ArrayList<Double> positionY  = new ArrayList<Double>();
	private ArrayList<Double> positionZ  = new ArrayList<Double>();
	
	private Integer index = 0; 	//unique identifier of each cBC, used for data output 
	public boolean displayAntigenGraph	= false;
	private int antigenCaptured = 0; //number of antigen acquired by each antigen
	
	
	/**
	 * Constructor
	 * @param seed  Used by MASON for the random seed
	 */
	public cognateBC(int index)
	{
		this.type = TYPE.NAIVE;
		this.setIndex(index);
	}
	
	@Override
	public void step( final SimState state )
	{
		super.step(state);
		
		// record the cells coordinates in arraylists

		

		
		//set the time frame for data collection
		//30 minutes is what was done in vivo
		// we should do the same to keep it comparable
		
		
		//once the system has reached steady state the BC can start to record it's position
		if(SimulationEnvironment.steadyStateReached == true)
		{
			updatePosition(state);
		}
	}
	
	

	/**
	 * Updates the cells X,Y and Z coordinates in the XY and Z arraylists
	 * and the controllers coordinate MAPs so they can be accessed by viewers
	 * TODO could be refactored
	 */
	private void updatePosition(SimState state)
	{
		positionX.add(this.x);
		positionY.add(this.y);
		positionZ.add(this.z);
		
		SimulationEnvironment.getController().getX_Coordinates().put(this.getIndex(), this.getPositionX());
		SimulationEnvironment.getController().getY_Coordinates().put(this.getIndex(), this.getPositionY());
		SimulationEnvironment.getController().getZ_Coordinates().put(this.getIndex(), this.getPositionZ());
	
		
	}
	
	/**
	 * ENUM for the activation status
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
		if(this.getPositionAlongStroma()> 0.5)  //if on the upper half of the stromal edge
		{
			if (sEdge.getAntigenLevelUpperEdge() > 0) // if the stroma has antigen to present
			{
				sEdge.setAntigenLevelUpperEdge(sEdge.getAntigenLevelUpperEdge() - 1);	// remove the antigen from the stromal edge		
			}
		}
		else
		{
			if (sEdge.getAntigenLevelLowerEdge() > 0) // if on the lower half of the stromal edge
			{
				sEdge.setAntigenLevelLowerHalf(sEdge.getAntigenLevelLowerEdge() - 1);	//remove the antigen from the stromal edge		
			}
		}
		
		this.setAntigenCaptured(this.getAntigenCaptured() + 1); //increment the cBC antigen captured counter
		
		//if the cell is naive then we need to update its status to primed
		if(this.type==TYPE.NAIVE)
		{
			this.type = TYPE.PRIMED;
			
			//update the number of primed cells in the simulation
			SimulationEnvironment.getController();
			Controller.setPrimedCells(Controller.getPrimedCells() + 1);;
		}
		
		if ( displayAntigenGraph )
		{
		 // Grapher.updateAntigenGraph( this.getAntigenCaptured()); //this gives an error when run on console
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

	
	
	
	
	//getters and setters, more efficient to have cBC update the MAP than controller
	public int 				 getAntigenCaptured()   {return antigenCaptured;}
	public ArrayList<Double> getPositionX() 		{return positionX;}
	public ArrayList<Double> getPositionY() 		{return positionY;}
	public ArrayList<Double> getPositionZ() 		{return positionZ;}
	public Integer 			 getIndex() 			{return index;}
	
	public void setAntigenCaptured(int antigenCaptured)   {this.antigenCaptured = antigenCaptured;}
	public void setPositionX(ArrayList<Double> positionX) {this.positionX = positionX;}
	public void setPositionY(ArrayList<Double> positionY) {this.positionY = positionY;}
	public void setPositionZ(ArrayList<Double> positionZ) {this.positionZ = positionZ;}
	public void setIndex(Integer index) 				  {this.index = index;}


}
