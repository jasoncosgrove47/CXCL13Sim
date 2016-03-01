package sim3d.cell;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import javax.media.j3d.TransformGroup;
import dataLogger.Controller;
import sim.engine.SimState;
import sim.portrayal3d.simple.SpherePortrayal3D;
import sim.util.Int3D;
import sim3d.Settings;
import sim3d.SimulationEnvironment;
import sim3d.collisiondetection.Collidable;
import sim3d.collisiondetection.CollisionGrid;


public class cognateBC extends BC {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// stores position of cBC in each dimension for each timestep
	private ArrayList<Double> positionX = new ArrayList<Double>();
	private ArrayList<Double> positionY = new ArrayList<Double>();
	private ArrayList<Double> positionZ = new ArrayList<Double>();

	private Integer index = 0; // unique identifier of each cBC
	public boolean displayAntigenGraph = false;
	private int antigenCaptured = 0; // number of antigen acquired by each cBC
	private int dendritesVisited = 0; //these kind of things should just be a member variable to keep style consistent with simon

	//counter for migration data collection
	int counter = 0;
	
	/**
	 * Constructor
	 * 
	 * @param seed
	 *            Used by MASON for the random seed
	 */
	public cognateBC(int index) {
		this.type = TYPE.NAIVE;
		this.setIndex(index);
		
		//need to set these to zero for the CXCR5 KO experiment
		//as we need a functional follicle to form for consistency
		 //this.m_iL_r = 0;
		 //this.m_iR_free = 0;
		 //this.m_iR_i = 0;
	}

	@Override
	public void step(final SimState state) {
		
		
		
		super.step(state);

		// once the system has reached steady state the BC can start to record
		// it's position
		if (SimulationEnvironment.steadyStateReached == true) {
			
			
			//the experiment runs for 6 hours but only 
			// need to record migration data for 30 mins
			if(counter < 30){
				updatePosition(state);
				
			}
			
			counter ++;
			
		}
	}

	

	
	
	/**
	 * Updates the cells X,Y and Z coordinates in the XY and Z arraylists and
	 * the controllers coordinate MAPs so they can be accessed by viewers 
	 */
	private void updatePosition(SimState state) {
		positionX.add(this.x);
		positionY.add(this.y);
		positionZ.add(this.z);
		
		

		Controller.getInstance().getX_Coordinates()
				.put(this.getIndex(), this.getPositionX());
		Controller.getInstance().getY_Coordinates()
				.put(this.getIndex(), this.getPositionY());
		Controller.getInstance().getZ_Coordinates()
				.put(this.getIndex(), this.getPositionZ());
	}

	/**
	 * ENUM for the activation status
	 */
	public enum TYPE {
		NAIVE, PRIMED, ACTIVATED
	}

	public TYPE type;

	/**
	 * Need more comments
	 */
	@Override
	public void handleCollisions(CollisionGrid cgGrid) {
		
		//initialise the dataMap
		if(this.dendritesVisited == 0 & Controller.getInstance().getDendritesVisited().containsKey(this.index) == false){
			
			Controller.getInstance().getDendritesVisited().put(this.index, this.dendritesVisited);
		
		}
		
		
		// don't let a b cell collide more than collisionThreshold times
		int collisionThreshold = 10;
		if (m_i3lCollisionPoints.size() == 0
				|| collisionCounter > collisionThreshold) {
			return;
		}

		//stores values uniquely in a hashset
		HashSet<Collidable> csCollidables = new HashSet<Collidable>(); 

		// Add all the cells to the set
		for (Int3D i3Point : m_i3lCollisionPoints) {
			for (Collidable cCollidable : cgGrid.getPoints(i3Point)) {
				csCollidables.add(cCollidable);
			}
		}

		int iCollisionMovement = m_d3aMovements.size();
		boolean bCollision = false;

		// To keep a track of where we collided - we are only interested in the
		// first collision so we can ignore anything after this
		for (Collidable cCell : csCollidables) {
			switch (cCell.getCollisionClass()) {
			case STROMA_EDGE: // These first two are the more likely hits as
								// they won't be moving
				if (collideStromaEdge((StromaEdge) cCell, iCollisionMovement))
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
		if (bCollision) // if the cell has collided
		{
			performCollision(cgGrid, iCollisionMovement); // deal with the
															// collision
		}
	}

	/**
	 * Acquire antigen from a B cell TODO update to account for heterogeneity at
	 * some point
	 * 
	 * @param cCell
	 */
	private void acquireAntigen(Collidable cCell) {
		StromaEdge sEdge = (StromaEdge) cCell;

		
		//determine if the cell has already grabbed antigen from this dendrite
		boolean lowerCollision = sEdge.cellsCollidedWithLowerHalf.contains(this.index);
		boolean upperCollision = sEdge.cellsCollidedWithUpperHalf.contains(this.index);
		
		
		
		
		
	
		// we divide the stroma in two as make it more accurate
		if (this.getPositionAlongStroma() > 0.5 & upperCollision == false) // if on the upper half of the
													// stromal edge
		{
	
			if (sEdge.getAntigenLevelUpperEdge() > 0) // if the stroma has
														// antigen to present
			{
				//remove antigen from the stromal edge
				sEdge.setAntigenLevelUpperEdge(sEdge.getAntigenLevelUpperEdge() - 1); 
				sEdge.cellsCollidedWithUpperHalf.add(this.index);
				
				//TODO not the prettiest code so should refactor
				this.dendritesVisited += 1;
				Controller.getInstance().getDendritesVisited().put(this.index, this.dendritesVisited);
			
			
			}
		} else if(lowerCollision == false){
			if (sEdge.getAntigenLevelLowerEdge() > 0) // if on the lower half of
														// the stromal edge
			{
				// remove antigen from stromal edge
				sEdge.setAntigenLevelLowerHalf(sEdge.getAntigenLevelLowerEdge() - 1); 
				sEdge.cellsCollidedWithLowerHalf.add(this.index);
				this.dendritesVisited +=1;
				Controller.getInstance().getDendritesVisited().put(this.index, this.dendritesVisited);
			}
		}

		//increment the cBC antigen captured counter
		this.setAntigenCaptured(this.getAntigenCaptured() + 1); 

		// if the cell is naive then we need to update its status to primed
		if (this.type == TYPE.NAIVE) {
			this.type = TYPE.PRIMED;

			// update the number of primed cells in the simulation
	
			//TODO why the hell is this happening
			Controller.getInstance().setPrimedCells(Controller.getInstance().getPrimedCells() + 1);
			
		}

		if (displayAntigenGraph) {
			// Grapher.updateAntigenGraph( this.getAntigenCaptured()); //this
			// gives an error when run on console
		}

	}

	@Override
	public TransformGroup getModel(Object obj, TransformGroup transf) {
		// We choose to always recalculate this model because the movement
		// changes in each time step.
		// Removing the movement indicators and removing this true will make the
		// 3d display a lot faster
		if (transf == null || true) {
			transf = new TransformGroup();

			// Draw the BC itself
			java.awt.Color blue = new Color(30, 200, 255, 255);

			SpherePortrayal3D s = new SpherePortrayal3D(blue,
					Settings.BC.COLLISION_RADIUS * 2, 6);
			s.setCurrentFieldPortrayal(getCurrentFieldPortrayal());
			TransformGroup localTG = s.getModel(obj, null);

			localTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
			transf.addChild(localTG);

			// if we have had any collisions, draw them as red circles
			modelCollisions(m_d3aCollisions, obj, transf);

			// If we have any movement, then draw it as white lines telling us
			// where the cell is orientated
			modelMovements(m_d3aMovements, obj, transf);
		}
		return transf;
	}


	// getters and setters, more efficient to have cBC update the MAP than
	// controller
	public int getAntigenCaptured() {
		return antigenCaptured;
	}

	public ArrayList<Double> getPositionX() {
		return positionX;
	}

	public ArrayList<Double> getPositionY() {
		return positionY;
	}

	public ArrayList<Double> getPositionZ() {
		return positionZ;
	}

	public Integer getIndex() {
		return index;
	}

	public void setAntigenCaptured(int antigenCaptured) {
		this.antigenCaptured = antigenCaptured;
	}

	public void setPositionX(ArrayList<Double> positionX) {
		this.positionX = positionX;
	}

	public void setPositionY(ArrayList<Double> positionY) {
		this.positionY = positionY;
	}

	public void setPositionZ(ArrayList<Double> positionZ) {
		this.positionZ = positionZ;
	}

	public void setIndex(Integer index) {
		this.index = index;
	}

}
