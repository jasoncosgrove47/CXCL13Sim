package sim3d.cell;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;

import javax.media.j3d.TransformGroup;

import dataLogger.Controller;
import sim.engine.SimState;
import sim.portrayal3d.simple.SpherePortrayal3D;
import sim.util.Double3D;
import sim.util.Int3D;
import sim3d.Settings;
import sim3d.SimulationEnvironment;
import sim3d.collisiondetection.Collidable;
import sim3d.collisiondetection.CollisionGrid;

public class cognateBC extends BC {

	/**
	 * A cognateBC is a type of B-cell which can bind to antigen. Within an in
	 * silico experiment we track only this cell type
	 * 
	 * @author: Jason Cosgrove
	 */
	private static final long serialVersionUID = 1L;

	

	/**
	 * Used to store the x,y and z coordinates of a BC during a cell tracking
	 * experiment
	 */
	//private ArrayList<Double> positionX = new ArrayList<Double>();
	//private ArrayList<Double> positionY = new ArrayList<Double>();
	//private ArrayList<Double> positionZ = new ArrayList<Double>();
	private ArrayList<Double3D> coordinates = new ArrayList<Double3D>();
	private ArrayList<Integer> receptors = new ArrayList<Integer>();

	
	/**
	 * Unique identifier of each cBC
	 */
	private Integer index = 0;

	/**
	 * Graphs the number of primed cells within the system if running the GUI
	 */
	public boolean displayAntigenGraph = false;

	/**
	 * The number of antigen captured by a cBC
	 */
	private int antigenCaptured = 0;

	/**
	 * The number of unique dendrites visited by a cBC
	 */
	int dendritesVisited = 0;

	/**
	 * Counter used to increment time in a cell migration experiment
	 */
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

		// need to set these to zero for the CXCR5 KO experiment
		// as we need a functional follicle to form for consistency

		 //this.m_iL_r = 0;
		 //this.m_iR_free = 0;
		// this.m_iR_i = 0;
	}

	@Override
	public void step(final SimState state) {

		
		super.step(state);

		// once the system has reached steady state the BC can start to record
		// it's position
		if (SimulationEnvironment.steadyStateReached == true) {

			updateReceptors();
			// the experiment runs for 12 hours but only
			// need to record migration data for 30 mins
			if (counter < 30) {
				updatePosition();
			}

			counter++;

		}
	}

	
	/**
	 * Updates the cells surface receptor levels in Controller
	 */
	void updateReceptors() {
		receptors.add(this.m_iR_free);

		Controller.getInstance().getReceptors()
				.put(this.getIndex(), this.getReceptors());

	}

	
	/**
	 * Updates the cells X,Y and Z coordinates in the XY and Z arraylists and
	 * the controllers coordinate MAPs so they can be accessed by viewers
	 */
	void updatePosition() {

		coordinates.add(new Double3D(this.x,this.y,this.z));


		Controller.getInstance().getCoordinates().put(this.getIndex(), this.getCoordinates());
		
	}

	/**
	 * ENUM for the activation status
	 */
	public enum TYPE {
		NAIVE, PRIMED, ACTIVATED
	}

	public TYPE type;

	/**
	 * Handles collisions between stroma and cognate B cells Might be able to
	 * refactor TODO Need more comments
	 */
	@Override
	public void handleCollisions(CollisionGrid cgGrid) {

		// initialise the dataMap
		if (this.dendritesVisited == 0
				& Controller.getInstance().getDendritesVisited()
						.containsKey(this.index) == false) {

			Controller.getInstance().getDendritesVisited()
					.put(this.index, this.dendritesVisited);

		}

		// don't let a b cell collide more than collisionThreshold times
		// required to avoid getting in an infinite loop
		int collisionThreshold = 10;
		if (getM_i3lCollisionPoints().size() == 0
				|| getCollisionCounter() > collisionThreshold) {
			return;
		}

		// stores values uniquely in a hashset
		HashSet<Collidable> csCollidables = new HashSet<Collidable>();

		// Add all the cells to the set
		for (Int3D i3Point : getM_i3lCollisionPoints()) {
			for (Collidable cCollidable : cgGrid.getPoints(i3Point)) {
				csCollidables.add(cCollidable);
			}
		}

		int iCollisionMovement = getM_d3aMovements().size();
		boolean bCollision = false;

		// To keep a track of where we collided - we are only interested in the
		// first collision so we can ignore anything after this
		for (Collidable cCell : csCollidables) {
			switch (cCell.getCollisionClass()) {
			case STROMA_EDGE: // These first two are the more likely hits as
								// they won't be moving
				if (collideStromaEdge((StromaEdge) cCell, iCollisionMovement)) {
					iCollisionMovement = getM_d3aMovements().size() - 1;
					bCollision = true;

					// this guard is here as we don't want the agents to acquire
					// antigen until controller starts recording
					if (SimulationEnvironment.steadyStateReached == true) {
						acquireAntigen(cCell);
					}

				}
				break;

			case BRANCH:

				if (collideStromaEdge((branch) cCell, iCollisionMovement)) {

					iCollisionMovement = getM_d3aMovements().size() - 1;
					bCollision = true;

					// this guard is here as we don't want the agents to acquire
					// antigen until controller starts recording
					if (SimulationEnvironment.steadyStateReached == true) {
						acquireAntigen(cCell);
					}
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
	 * Acquire antigen from a stroma edge or branch
	 * 
	 * 
	 * TODO think this is what's making it sensitive to calibration
	 * @param cCell
	 * 
	 */
	public void acquireAntigen(Collidable cCell) {
		StromaEdge sEdge = (StromaEdge) cCell;

		// determine if the cell has already grabbed antigen from this dendrite
		boolean collision = sEdge.cellsCollidedWith
				.contains(this.index);


		// we divide the stroma in two as make it more accurate
		//if on the upper half of the stroma
		if (collision == false) 
		{

			if (sEdge.getAntigenLevel() > 0) // if the stroma has antigen to present
			{
				// remove antigen from the stromal edge
				sEdge.setAntigenLevel(sEdge.getAntigenLevel() - 1);
				sEdge.cellsCollidedWith.add(this.index);

				// TODO not the prettiest code so should refactor
				this.dendritesVisited += 1;
				Controller.getInstance().getDendritesVisited()
						.put(this.index, this.dendritesVisited);

				// increment the cBC antigen captured counter
				this.setAntigenCaptured(this.getAntigenCaptured() + 1);

			}
		} 

		// if the cell is naive then we need to update its status to primed
		if (this.type == TYPE.NAIVE) {
			this.type = TYPE.PRIMED;

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
			modelCollisions(getM_d3aCollisions(), obj, transf);

			// If we have any movement, then draw it as white lines telling us
			// where the cell is orientated
			modelMovements(getM_d3aMovements(), obj, transf);
		}
		return transf;
	}

	/**
	 * Getters and setters for key parameters TODO probably more consistent to
	 * have them as member variables, need to ask Simon
	 * 
	 * @return
	 */
	public int getAntigenCaptured() {
		return antigenCaptured;
	}



	public ArrayList<Double3D> getCoordinates() {
		return coordinates;
	}
	
	public ArrayList<Integer> getReceptors() {
		return receptors;
	}

	public Integer getIndex() {
		return index;
	}

	public void setAntigenCaptured(int antigenCaptured) {
		this.antigenCaptured = antigenCaptured;
	}
	
	public void setIndex(Integer index) {
		this.index = index;
	}

}
