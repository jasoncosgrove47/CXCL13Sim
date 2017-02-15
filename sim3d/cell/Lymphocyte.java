package sim3d.cell;


import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.LineArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3f;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.engine.Stoppable;
import sim.field.continuous.Continuous3D;
import sim.portrayal3d.simple.Shape3DPortrayal3D;
import sim.portrayal3d.simple.SpherePortrayal3D;
import sim.util.Double3D;
import sim.util.Int3D;
import sim3d.Settings;
import sim3d.SimulationEnvironment;
import sim3d.collisiondetection.Collidable;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.migration.Algorithm1;
import sim3d.migration.MigrationAlgorithm;
import sim3d.migration.MigratoryCell;
import sim3d.stroma.Stroma;
import sim3d.stroma.StromaEdge;
import sim3d.util.Vector3DHelper;

public abstract class Lymphocyte extends DrawableCell3D implements Steppable, Collidable, MigratoryCell {
	
	public int numOfReceptors = 1;
	
	/**
	 * The drawing environment that houses this cell; used by
	 * DrawableCell3D.setObjectLocation
	 */
	public static Continuous3D drawEnvironment;

	/**
	 * Constructor for lymphocytes, responsible for setting up the receptor maps
	 * could probably be encapsulated into another method	 
	 */
	public Lymphocyte(){
		
		initialiseReceptors();
	}
	
	
	
	
	public void initialiseReceptors(){
		


		this.getM_receptorMap().put(Receptor.CXCR5, new ArrayList<Integer>(4));
		this.getM_receptorMap().get(Receptor.CXCR5).add(0,Settings.BC.ODE.LR());
		this.getM_receptorMap().get(Receptor.CXCR5).add(1,Settings.BC.ODE.Rf());
		this.getM_receptorMap().get(Receptor.CXCR5).add(2,Settings.BC.ODE.Ri());
		this.getM_receptorMap().get(Receptor.CXCR5).add(3,0);//this is for desensitised receptors
		
		
		this.getM_receptorMap().put(Receptor.CCR7, new ArrayList<Integer>(3));
		this.getM_receptorMap().get(Receptor.CCR7).add(0,0);
		this.getM_receptorMap().get(Receptor.CCR7).add(1,0);
		this.getM_receptorMap().get(Receptor.CCR7).add(2,0);
		this.getM_receptorMap().get(Receptor.CCR7).add(3,0);
		
		
		this.getM_receptorMap().put(Receptor.EBI2, new ArrayList<Integer>(4));
		this.getM_receptorMap().get(Receptor.EBI2).add(0,0);
		this.getM_receptorMap().get(Receptor.EBI2).add(1,0);
		this.getM_receptorMap().get(Receptor.EBI2).add(2,0);
		this.getM_receptorMap().get(Receptor.EBI2).add(3,0);
			
	}
	
	
	
	
	
	public static enum Receptor {
		CXCR5, CCR7, EBI2
	}
	
	/**
	 * Gives each ENUM an array index
	 */
	protected EnumMap<Receptor, List<Integer>> m_receptorMap = new EnumMap<Receptor, List<Integer>>(Receptor.class);
	
	/*
	 * 3D grid where B cells and cBs exist
	 */
	public static Continuous3D bcEnvironment;

	/**
	 * The collision grid that contains this element; used to register
	 * collisions
	 */
	public static CollisionGrid m_cgGrid;

	/**
	 * The squared distance between a BC and a stroma edge at the point of
	 * collision; precomputed to speed up calculations
	 */
	static final double BC_SE_COLLIDE_DIST_SQ = (Settings.BC.COLLISION_RADIUS + 
			Settings.FDC.STROMA_EDGE_RADIUS)
			* (Settings.BC.COLLISION_RADIUS + Settings.FDC.STROMA_EDGE_RADIUS);

	/**
	 * The squared distance between a BC and a stromal cell at the point of
	 * collision; precomputed to speed up calculations
	 */
	static final double BC_SC_COLLIDE_DIST_SQ = (Settings.BC.COLLISION_RADIUS + 
			Settings.FDC.STROMA_NODE_RADIUS)
			* (Settings.BC.COLLISION_RADIUS + Settings.FDC.STROMA_NODE_RADIUS);
	
	
	/**
	 * Required to prevent a warning
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Boolean to tell the cell whether or not it should be sending graph data;
	 * used to make sure we only have one reporting!
	 */
	public boolean displayODEGraph = false;

	/**
	 * 
	 * The direction the cell is facing; used for movement
	 */
	private Double3D m_d3Face = Vector3DHelper.getRandomDirection();

	/**
	 * DEBUG used to display collision points
	 */
	private ArrayList<Double3D> m_d3aCollisions = new ArrayList<Double3D>();

	/**
	 * The movements of the cell; each collision or bounce means a new movement
	 * each movement is relative to the current position
	 */
	private List<Double3D> m_d3aMovements = new ArrayList<Double3D>();

	/**
	 * Points on the collision grid we're intersecting with something
	 */
	private HashSet<Int3D> m_i3lCollisionPoints = new HashSet<Int3D>();

	/*
	 * Determines the position of a BC on a stromal edge
	 */
	private double positionAlongStroma = 0;
	
	/*
	 * Determines the position of a BC on a stromal edge
	 */
	private Algorithm1 a2 = new Algorithm1();
	
	/*
	 * Determines how many collisions a BC has had this timestep necessary to
	 * prevent infinite collisions
	 */
	private int collisionCounter = 0;

	@Override
	public boolean isStatic() {
		return false;
	}

	/**
	 * Adds a collision point to the list
	 * 
	 * @param i3Point
	 *            Coordinates for the collision
	 */
	@Override
	public void addCollisionPoint(Int3D i3Point) {
		getM_i3lCollisionPoints().add(i3Point);
	}

	/**
	 * @return the ENUM representing the type of Collidable this cell is (BC)
	 * 
	 *TODO this method shouldnt be necessary here should just overwrite in BC
	 */
	@Override
	public CLASS getCollisionClass() {
		return CLASS.LYMPHOCYTE;
	}

	/**
	 * TODO this method shouldnt be necessary here should just overwrite in BC
	 * (non-Javadoc)
	 * @see sim3d.cell.DrawableCell3D#getDrawEnvironment()
	 */
	@Override
	public Continuous3D getDrawEnvironment() {
		return drawEnvironment;
	}

	/**
	 * Controls what a B cell agent does for each time step Each Bcell registers
	 * its intended path on the collision grid, once all B cells register the
	 * collision grid handles the movement at the next iteration the B cells are
	 * moved. B cells only collide with stroma
	 */
	@Override
	public void step(final SimState state)// why is this final here
	{
		migrate(a2);
	}

	/**
	 * DO NOT DELETE THIS METHOD
	 */
	@Override
	public void migrate(MigrationAlgorithm algorithm) {
		// TODO Auto-generated method stub
		algorithm.performMigration(this);
	}
	
	
	public Int3D getDiscretizedLocation(Continuous3D grid) {
		Double3D me = grid.getObjectLocation(this);// obtain coordinates of the
													// tcell
		Int3D meDiscrete = grid.discretize(me);
		return meDiscrete;
	}

	/**
	 * How to remove a BC from the schedule:
	 * 
	 * when you schedule the BC it will return a stoppable object we store this
	 * object so we can access it when we need to stop the object. Then to
	 * remove the object we simply call stop() on the stopper object the BC can
	 * then be removed by garbage collection
	 */
	public void removeDeadCell(Continuous3D randomSpace) {
		this.stop();
		randomSpace.remove(this);
	}

	/**
	 * Flag to show if this class has been stopped (when no longer needed)
	 */
	private Stoppable stopper = null;

	/**
	 * Method to change the value of the stopper this is the stoppabkle object
	 * so we can access its stop method stoppable can acess BC but not the other
	 * way round
	 * 
	 * @param stopper
	 *            Whether the class should be stopped or not
	 */
	public void setStopper(Stoppable stopper) {
		this.stopper = stopper;
	}

	/**
	 * Method to stop the class where necessary
	 */
	public void stop() {
		stopper.stop();
	}

	@Override
	public void registerCollisions(CollisionGrid cgGrid) {
		if (cgGrid == null) {
			return;
		}

		double dPosX = x;
		double dPosY = y;
		double dPosZ = z;

		for (Double3D d3Movement : getM_d3aMovements()) {
			cgGrid.addLineToGrid(this, new Double3D(dPosX, dPosY, dPosZ),
					new Double3D(dPosX + d3Movement.x, dPosY + d3Movement.y,
							dPosZ + d3Movement.z), Settings.BC.COLLISION_RADIUS);

			dPosX += d3Movement.x;
			dPosY += d3Movement.y;
			dPosZ += d3Movement.z;
		}
	}

	/**
	 * Handles collisions between b cells and stroma
	 */

	@Override
	public void handleCollisions(CollisionGrid cgGrid) {
		// don't let a b cell collide more than collisionThreshold times
		// otherwise you get in an infinite loop where a B cell continues
		// bouncing indefinitely
		int collisionThreshold = 50;
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
			case STROMA_EDGE:
				// These first two are the more likely hits as
				// they won't be moving

				if (collideStromaEdge((StromaEdge) cCell, iCollisionMovement)) {
					iCollisionMovement = getM_d3aMovements().size() - 1;
					bCollision = true;
				}
				break;
			case STROMA:
			
				//is it safe to assume that the cells have actually collided at this point
				// or do we still need to do the collision check as per collideStromaEdge to determine 
				// where they have collided. 
				if (collideStromaNode((Stroma) cCell, iCollisionMovement)) {
					
					iCollisionMovement = getM_d3aMovements().size() - 1;
					bCollision = true;
				
				}
				break;

			case LYMPHOCYTE:
				break;
			}
		}
		if (bCollision) // if the cell has collided
		{
			// deal with the collision
			performCollision(cgGrid, iCollisionMovement);
		}
	}

	
	/**
	 * helper method to perform the actual collision
	 */
	protected void performCollision(CollisionGrid cgGrid, int iCollisionMovement) {
		// increment the number of times a B cell has collided this time step
		setCollisionCounter(getCollisionCounter() + 1);
		// Add the collision point for visualisation
		double xPos = 0;
		double yPos = 0;
		double zPos = 0;
		for (int i = 0; i < iCollisionMovement; i++) {
			Double3D d3Movement = getM_d3aMovements().get(i);
			xPos += d3Movement.x;
			yPos += d3Movement.y;
			zPos += d3Movement.z;
		}

		getM_d3aCollisions().add(new Double3D(xPos, yPos, zPos));
		// Recheck for bounces and reregister with the grid
		handleBounce();
		registerCollisions(cgGrid);
	}

	
	
	
	/**
	 * Treat the cells movement vector as a line and the static stromal cell as a point
	 * we then figure out what the closest point between the two objects is,
	 * if this distance is less than the width of the two cells then we mark it as a collision
	 * 
	 * 
	 * 
	 * @param cell
	 * @param iCollisionMovement
	 * @return
	 */
	protected boolean collideStromaNode(Stroma cell, int iCollisionMovement){

	
		boolean hasCollided = false;
		
		//the cells current location
		Double3D p1 = this.getDrawEnvironment().getObjectLocation(this);

		
		//determine the last coordinates of the movement, need to figure out what these various
		//arraylists contain collisionMovement md3amovement etc
		for (int i = 0; i < iCollisionMovement; i++) {
			
			//the movements to make relative to the current position
			Double3D d1 = getM_d3aMovements().get(i);	
			Double3D stromaLoc = cell.getDrawEnvironment().getObjectLocation(cell);
			
			//determine the point on the line which is the shortest distance between
			// the stromal cell and the movement vector
			Double3D shortestPoint = closestPointToStroma(p1,d1,stromaLoc);
			double length = shortestDistanceToSegment(shortestPoint, stromaLoc);
			
			if(length < BC_SC_COLLIDE_DIST_SQ){
				iCollisionMovement = getM_d3aMovements().size() - 1;
				hasCollided = true;
			}
			
			
			if (hasCollided) {
				
				updateMovementToAccountForCollisionWithNode(length,p1,d1,shortestPoint,stromaLoc,i);
				
			} else {
				// Move the BC location according to full the movement.
				p1 = p1.add(d1);
			}
		
		}
		
	
		return hasCollided;
	}
	
	
	/*
    * Returns the distance of p3 to the segment defined by p1,p2;
    * 
    * @param p1
    *                First point of the segment
    * @param p2
    *                Second point of the segment
    * @param p3
    *                Point to which we want to know the distance of the segment
    *                defined by p1,p2
    * @return The distance of p3 to the segment defined by p1,p2
    * 
    * Works on the fact that shortest point between the point and the line
    * is where they are orthogonal, ie where the dot product equals 0
    * 
    * the distance is thus the distance between the poiint and where
    * the orthogonal tangent meets the line. 
    * 
    * http://paulbourke.net/geometry/pointlineplane/
    * http://paulbourke.net/geometry/pointlineplane/DistancePoint.java
    * 
    */
    public static double shortestDistanceToSegment(Double3D closestPoint, Double3D p3) {

    	return closestPoint.distance(p3);
    }
	
    
    /**
     * Given a point p1 on a line q1-q2 return the proportion of the distance from
     * q1-p1 to the distance from q1-12
     */
    private static double proportionAlongVector(Double3D p1, Double3D q1, Double3D q2){
    	double lengthOfLine = SimulationEnvironment.calcDistance(q1,q2);
    	double distToClosestPoint = SimulationEnvironment.calcDistance(q1,p1);
    	return distToClosestPoint/lengthOfLine;
    	
    }
    
	/*
     * Returns the distance of p3 to the segment defined by p1,p2;
     * 
     * @param p1
     *                First point of the segment
     * @param p2
     *                Second point of the segment
     * @param p3
     *                Point to which we want to know the distance of the segment
     *                defined by p1,p2
     * @return The distance of p3 to the segment defined by p1,p2
     * 
     * Works on the fact that shortest point between the point and the line
     * is where they are orthogonal, ie where the dot product equals 0
     * 
     * the distance is thus the distance between the poiint and where
     * the orthogonal tangent meets the line. 
     * 
     * http://paulbourke.net/geometry/pointlineplane/
     * http://paulbourke.net/geometry/pointlineplane/DistancePoint.java
     * 
     */
    public static Double3D closestPointToStroma(Double3D p1, Double3D p2, Double3D p3){
    	
    	final double xDelta = p2.getX() - p1.getX();
    	final double yDelta = p2.getY() - p1.getY();
    	final double zDelta = p2.getZ() - p1.getZ();
    	

    	if ((xDelta == 0) && (yDelta == 0)) {
    	    throw new IllegalArgumentException("p1 and p2 cannot be the same point");
    	}

    	
    	final double u = ((p3.getX() - p1.getX()) * xDelta + (p3.getY() - p1.getY()) * yDelta + (p3.getZ() - p1.getZ()) * zDelta) 
    			/ (xDelta * xDelta + yDelta * yDelta + zDelta * zDelta);

    	final Double3D closestPoint;
    	if (u < 0) {
    	    closestPoint = p1;
    	} else if (u > 1) {
    	    closestPoint = p2;
    	} else {
    	    closestPoint = new Double3D(p1.getX() + u * xDelta, p1.getY() + u * yDelta, p1.getZ() + u * yDelta);
    	}

    	return closestPoint;
    	
    }
   
	
	/**
	 * Performs collision detection and handling with Stroma Edges (cylinders).
	 * 
	 * Find the closest point between two vectors (cell movement, stromal edge)
	 * backtrack to find where the vectors first meet eachother. The exact point
	 * that a collision first occurs.
	 * 
	 * update the B cells movement list m_d3aMovements
	 * 
	 * @param seEdge
	 *            The edge to check collision with
	 * @param iCollisionMovement
	 *            The movement to check collisions up to (this is to prevent
	 *            colliding before bounces are handled)
	 * @return true if a collision occurs
	 * 
	 */
	protected boolean collideStromaEdge(StromaEdge seEdge,
			int iCollisionMovement) {
		
		//we have two lines characterised by the following start and end points
		// P1-Q1
		// P2-Q2
		
		// the closest point between the two lines are where the line between them is orthogonal to both lines
		// or in other words where the dot product between the two vectors is equal to zero. 
		// https://q3k.org/gentoomen/Game%20Development/Programming/Real-Time%20Collision%20Detection.pdf
		
		
		//take a point on the segment and then draw a vector from the
		// point to the start point P1 of the line
		// the dot product is just the projection of that vector onto
		// the line (remember cos gives you the x-axis
		// we are just making the line the x-axis).

		
		Double3D p1 = new Double3D(x, y, z);//the start point of the cells movement
		Double3D p2 = seEdge.getPoint1(); //the start point of the stroma edge
		Double3D d2 = seEdge.getPoint2().subtract(p2); //Q2 - P2

		for (int i = 0; i < iCollisionMovement; i++) {
			Double3D d1 = getM_d3aMovements().get(i); //where to move relative to the current location

			// make sure that d1 has a length
			if (d1.length() > 0) {

			
				// For some pair of values for s  and t , L1 (s ) and L2 (t ) 
				// correspond to the closest points on the lines, 
				//  v (s , t ) describes a vector between them 

				double s = 0;
				double t = 0;


				Double3D r = p1.subtract(p2); // p1 - p2
				double a = Vector3DHelper.dotProduct(d1, d1); // squared length line 1																
				double b = Vector3DHelper.dotProduct(d1, d2);
				double c = Vector3DHelper.dotProduct(d1, r);
				double e = Vector3DHelper.dotProduct(d2, d2); // squared length of line 2
				double f = Vector3DHelper.dotProduct(d2, r);

				// differing from the link, dealing with lines so dont need to
				// account for points
				// we therefore assume that neither are points (zero length)

				// (d1.d1)(d2.d2) - (d1.d2)(d1.d2)
				double denom = a * e - b * b; // >= 0

				// find the points on each line where the vectors are closest
				List<Double> closestPoints = findClosestPointsBetween(i, p1,
						p2, d1, d2, denom, s, t, a, b, c, e, f);

	
				s = closestPoints.get(0);
				t = closestPoints.get(1);

				// update the B cells T-variable as this tells us how far along
				// the stroma the B cell is
				this.setPositionAlongStroma(closestPoints.get(1));

				// So c1 and c2 are the points on the two lines which are
				// closest to
				// one another
				// c1 = P1 + s.d1
				// c2 = P2 + t.d2
				Double3D c1 = p1.add(d1.multiply(s));
				Double3D c2 = p2.add(d2.multiply(t));

				// remember that the dot product of a vector times a vector
				// equals its length squared
				double length = Vector3DHelper.dotProduct(c1.subtract(c2),
						c1.subtract(c2));

				boolean bCollide = false;

				if (length < BC_SE_COLLIDE_DIST_SQ) // if the distance between
													// the B
													// cell and the stroma is
													// lower
													// than a threshold
				{
					bCollide = true; // set the collision flag for the B cell to
										// true
					double sNew = s;

					// We want to find the actual point we collide so let's
					// backtrack a bit. We don't get the exact point, but this
					// does add a little elasticity. Basically repeat the process 
					// until we go over this value can affect the speed of the 
					// simulation so really need to be careful

					// we add on the collidedist/10 term because if the BC and
					// stroma are the exact distance apart then the BC won't know 
					// that it has collided so we bring it back just slightly to 
					// make sure that it collides in the next time step
					while (length < BC_SE_COLLIDE_DIST_SQ
							+ (BC_SE_COLLIDE_DIST_SQ / 10)
							&& s > 0 && s < 1) {
						sNew = calculateSNew(s, length, d1, d2);

						// Collision Detection p. 130
						// ab = d2, ac = point - p2, bc = point -
						// seEdge.getPoint2()
						Double3D ac = p1.add(d1.multiply(sNew)).subtract(p2);
						Double3D bc = p1.add(d1.multiply(sNew)).subtract(
								seEdge.getPoint2());
						e = Vector3DHelper.dotProduct(ac, d2);

						length = updateLength(length, ac, bc, e, f, d2);
						s = sNew;
					}
				}

				if (s == 0) {
					Double3D d3Vec = c1.subtract(c2);

					// we're already moving away!
					if (Vector3DHelper.dotProduct(d3Vec, d1) > 0) {
						bCollide = false;
					}
				} else if (s == 1) {
					bCollide = false;
				}

				if (bCollide) {
					updateMovementToAccountForCollision(length, d1, d2, p1, p2,
							s, t, i);
					return true;
				} else {
					// Move the BC location according to full the movement.
					p1 = p1.add(d1);
				}
			}

		}

		return false;
	}

	
	private void updateMovementToAccountForCollisionWithNode(double length,
			Double3D p1,Double3D d1,  Double3D closestPoint, 
			 Double3D stromaLoc,int i) {

		Double3D d3NewDir;

		// Get the approach direction normalised, and in reverse

		Double3D d3MovementNormal = d1.multiply(-1).normalize();

		// We hit bang in the middle so just bounce - unlikely!
		if (length == 0) {

			// The cell bounces back to it's original position so
			// no need to updated its coordinates.
			d3NewDir = d3MovementNormal;

		} else {

			// Calculate the direction from the stroma collision point to the BC
			// collision point
			Double3D d3BounceNormal = closestPoint
					.subtract(stromaLoc).normalize();

			
			// reflect the movement normal about this point (rotate to it, and
			// apply the same rotation again)
			d3NewDir = Vector3DHelper.rotateVectorToVector(d3MovementNormal,
					d3MovementNormal, d3BounceNormal);
			d3NewDir = Vector3DHelper.rotateVectorToVector(d3NewDir,
					d3MovementNormal, d3BounceNormal);

		}

		double s = proportionAlongVector(closestPoint,p1,d1);
		
		// Set the new movement 
		d1 = d1.multiply(s); //what does multiplying by s give us??

		if (d1.lengthSq() > 0) {
			getM_d3aMovements().set(i, d1);
			i++;
		}

		// We need to add up all vectors after this one
		// so we can add a new vector of this length
		double dNewLength = 0;
		while (getM_d3aMovements().size() > i) {
			dNewLength += getM_d3aMovements().get(i).length();
			getM_d3aMovements().remove(i);
		}

		// add the remaining length of the current movement
		dNewLength += d1.length() * (1 - s);

		// slow down based on how fast we changed direction
		dNewLength *= (2 + Vector3DHelper
				.dotProduct(d3NewDir, d3MovementNormal)) / 3;
		d3NewDir = d3NewDir.multiply(dNewLength);

		if (d3NewDir.lengthSq() > 0) {
			getM_d3aMovements().add(d3NewDir);
		}

	}
	
	
	
	/**
	 * Helper Method to update movement after a collision occurs update the B
	 * cells m_d3aMovements
	 * 
	 * TODO Need to say what the different inputs actually are....
	 * 
	 * d1 = the values to add to the current location to move it
	 * d2 = Q2 - P2
	 * 
	 * p1 = cells current location
	 * p2 = start point of line 2
	 * 
	 * s and t equal values where 
	 * L1 (s) and L2 (t) correspond to the closest points on the lines, 
	 * 
	 * i is the iteration through the movements array
	 * 
	 * 
	 */
	private void updateMovementToAccountForCollision(double length,
			Double3D d1, Double3D d2, Double3D p1, Double3D p2, double s,
			double t, int i) {

		Double3D d3NewDir;

		// Get the approach direction normalised, and in reverse

		Double3D d3MovementNormal = d1.multiply(-1).normalize();

		// We hit bang in the middle so just bounce - unlikely!
		if (length == 0) {

			// The cell bounces back to it's original position so
			// no need to updated its coordinates.
			d3NewDir = d3MovementNormal;

		} else {

			// Calculate the direction from the stroma collision point to the BC
			// collision point
			Double3D d3BounceNormal = p1.add(d1.multiply(s))
					.subtract(p2.add(d2.multiply(t))).normalize();

			// reflect the movement normal about this point (rotate to it, and
			// apply the same rotation again)
			d3NewDir = Vector3DHelper.rotateVectorToVector(d3MovementNormal,
					d3MovementNormal, d3BounceNormal);
			d3NewDir = Vector3DHelper.rotateVectorToVector(d3NewDir,
					d3MovementNormal, d3BounceNormal);

		}

		// Set the new movement
		d1 = d1.multiply(s);//why multiply by s?

		if (d1.lengthSq() > 0) {
			getM_d3aMovements().set(i, d1);
			i++;
		}

		// We need to add up all vectors after this one
		// so we can add a new vector of this length
		double dNewLength = 0;
		while (getM_d3aMovements().size() > i) {
			dNewLength += getM_d3aMovements().get(i).length();
			getM_d3aMovements().remove(i);
		}

		// add the remaining length of the current movement
		dNewLength += d1.length() * (1 - s);

		// slow down based on how fast we changed direction
		dNewLength *= (2 + Vector3DHelper
				.dotProduct(d3NewDir, d3MovementNormal)) / 3;
		d3NewDir = d3NewDir.multiply(dNewLength);

		if (d3NewDir.lengthSq() > 0) {
			getM_d3aMovements().add(d3NewDir);
		}

	}

	/**
	 * Helper method for collide stroma edge
	 * 
	 * returns a double, length
	 */
	double updateLength(double length, Double3D ac, Double3D bc, double e,
			double f, Double3D d2) {
		if (e <= 0) {
			length = Vector3DHelper.dotProduct(ac, ac);
		} else {
			f = Vector3DHelper.dotProduct(d2, d2);

			if (e >= f) {
				length = Vector3DHelper.dotProduct(bc, bc);
			} else {
				length = Vector3DHelper.dotProduct(ac, ac) - e * e / f;
			}
		}
		return length;
	}

	/**
	 * Helper method that determines the new value of S in collideStromaEdge
	 */
	double calculateSNew(double s, double length, Double3D d1, Double3D d2) {
		double sNew = s;

		// (Options.BC.COLLISION_RADIUS +
		// Options.FDC.STROMA_EDGE_RADIUS-Math.sqrt(length)) is the
		// length we're missing so we just add that on.
		double dSinTheta = Math.sqrt(Vector3DHelper.crossProduct(d2, d1)
				.lengthSq() / (d2.lengthSq() * d1.lengthSq())); // sin th
		double dActualLength = Math.sqrt(length);

		sNew = Math.max(0, s
				- (0.04 + Settings.BC.COLLISION_RADIUS
						+ Settings.FDC.STROMA_EDGE_RADIUS - dActualLength)
				/ (dSinTheta * d1.length()));
		return sNew;
	}

    /**
	 * Helper function which calculates the closest point between two lines
	 * called by collideStromaEdge, returns the closest points between two lines
	 * 
	 * Has lots of parameter inputs but this was the only way to encapsulate
	 * collision methods
	 */
	List<Double> findClosestPointsBetween(int i, Double3D p1, Double3D p2,
			Double3D d1, Double3D d2, double denom, double s, double t,
			double a, double b, double c, double e, double f) {

		// if segments not parallel, compute closest point on L1 to L2
		// and clamp to segment S1. Else pick arbritrary closest point S
		// so compute closest point and clamp to segment 1
		if (denom != 0) {
			s = Math.min(1.0, Math.max(0.0, (b * f - c * e) / denom));
		}

		// compute point on L2 closest to S1(s) using
		// t = Dot((P1+D1*s)-P1,D1)/Dot(D2,D2) = (b*s + f/e)
		t = b * s + f;// divide by e at the end to optimise p.151

		// if t in [0,1] done. Else, clamp t, recompute s for the new value
		// of t using s = Dot((P2 + D2*s) - P1,D1) / Dot(D1,D1) = (t*b - c) / a
		if (t < 0) {
			t = 0;
			s = Math.max(0, Math.min(1, -c / a));
		} else if (t > e) {
			t = 1;
			s = Math.max(0, Math.min(1, (b - c) / a));
		} else {
			t /= e;
		}

		List<Double> closestPoints = new ArrayList<Double>();
		closestPoints.add(s);
		closestPoints.add(t);

		return closestPoints;
	}

	/**
	 * Bounces the cell back inside the boundaries Very long method! There's a
	 * lot of repeated code, but it's hard to efficiently abstract it out into
	 * more methods.
	 * 
	 * 
	 * if the B cell gets to the border of the simulation it has to bounce back
	 * as space is non-toroidal, much trickier in 3D than 2D
	 */
	public void handleBounce() {
		boolean bBounce = true;

		// We should in theory only have to check the last step for bounces
		int iMovementIndex = getM_d3aMovements().size() - 1;

		double dPosX = x;
		double dPosY = y;
		double dPosZ = z;

		// add all movement vectors before the last one
		for (int i = 0; i < iMovementIndex; i++) {
			dPosX += getM_d3aMovements().get(i).x;
			dPosY += getM_d3aMovements().get(i).y;
			dPosZ += getM_d3aMovements().get(i).z;
		}

		// multiple bounces may occur, especially with long travel distances
		while (bBounce) {
			bBounce = false;

			double dNewPosX = dPosX;
			double dNewPosY = dPosY;
			double dNewPosZ = dPosZ;

			// add all movement vectors after the index,
			for (int i = iMovementIndex; i < getM_d3aMovements().size(); i++) {
				Double3D d3Movement = getM_d3aMovements().get(i);
				dNewPosX += d3Movement.x;
				dNewPosY += d3Movement.y;
				dNewPosZ += d3Movement.z;
			}
			// out of bounds on X axis
			if (dNewPosX > Settings.WIDTH - 1 || dNewPosX < 1) {
				bBounce = handleBounceXaxis(dPosX, iMovementIndex);
			}
			// out of bounds on Y axis, CHANGE SO DOESNT GO PAST LECS
			if (dNewPosY > Settings.HEIGHT - 2 || dNewPosY < 1) {
				bBounce = handleBounceYaxis(dPosY, iMovementIndex);
			}
			// out of bounds on Z axis
			if (dNewPosZ > Settings.DEPTH - 1 || dNewPosZ < 1) {
				bBounce = handleBounceZaxis(dPosZ, iMovementIndex);
			}
		}
	}

	/**
	 * Helper method which handles collisions between a B cell and the
	 * simulation borders along the X-axis
	 */
	private boolean handleBounceXaxis(double dPosX, int iMovementIndex) {
		// There might be multiple vectors, so we need to keep track
		// of position, and whether we've hit the wall yet or not
		boolean bBounce = false;

		double dTempPosX = dPosX; // what are these variables keeping track of

		boolean bFlipped = false;

		for (int i = iMovementIndex; i < getM_d3aMovements().size(); i++) {
			Double3D d3Movement = getM_d3aMovements().get(i);

			// if we have already hit the wall, we just flip the x axis
			// of all the remaining movements
			if (bFlipped) {
				getM_d3aMovements().set(i, new Double3D(-d3Movement.x, d3Movement.y,
						d3Movement.z));
				continue;
			}

			// does this sub movement go out of bounds
			if (dTempPosX + d3Movement.x < 1
					|| dTempPosX + d3Movement.x > Settings.WIDTH - 1) {
				// Figure out at which point it goes out
				double dCutOff = 1;
				if (dTempPosX + d3Movement.x < 1) {
					dCutOff = (1 - dTempPosX) / d3Movement.x;
				} else {
					dCutOff = ((Settings.WIDTH - 1) - dTempPosX) / d3Movement.x;
				}

				// Create 2 new vectors split at the cutoff point, the
				// latter mirrored along the y axis
				Double3D d3TruncMovement = d3Movement.multiply(dCutOff);
				Double3D d3Remainder = new Double3D(-d3Movement.x
						+ d3TruncMovement.x, d3Movement.y - d3TruncMovement.y,
						d3Movement.z - d3TruncMovement.z);

				// Replace the current one, then add the new one after it
				if (d3TruncMovement.lengthSq() > 0) {
					getM_d3aMovements().set(i, d3TruncMovement);
					getM_d3aMovements().add(i + 1, d3Remainder);

					// if we don't increment i, it will get flipped again!
					i++;
				} else {
					getM_d3aMovements().set(i, d3Remainder);
				}

				bFlipped = true;
				bBounce = true;
			}

			dTempPosX += d3Movement.x;
		}
		return bBounce;
	}

	/*
	 * Helper method which handles collisions between a B cell and the
	 * simulation borders along the Y-axis. TODO THIS SHOULD ACCOUNT FOR THE 
	 * LECS ALSO needs to be a parameter
	 */
	
	private boolean handleBounceYaxis(double dPosY, int iMovementIndex) {
		// There might be multiple vectors now, so we need to keep track
		// of position, and whether we've hit the wall yet or not
		boolean bBounce = false;

		double dTempPosY = dPosY;
		boolean bFlipped = false;

		for (int i = iMovementIndex; i < getM_d3aMovements().size(); i++) {
			Double3D d3Movement = getM_d3aMovements().get(i);

			// if we have already hit the wall, we just flip the y axis
			// of all the remaining movements
			if (bFlipped) {
				getM_d3aMovements().set(i, new Double3D(d3Movement.x, -d3Movement.y,
						d3Movement.z));
				continue;
			}

			// does this sub movement go out of bounds
			if (dTempPosY + d3Movement.y < 1
					//TODO this needs to be set to the SCS height , wherever you see a 3
					|| dTempPosY + d3Movement.y > Settings.HEIGHT- (Settings.bRC.SCSDEPTH)) {
				// Figure out at which point it goes out
				double dCutOff = 1;
				if (dTempPosY + d3Movement.y < 1) {
					dCutOff = (1 - dTempPosY) / d3Movement.y;
				} else {
					dCutOff = ((Settings.HEIGHT- (Settings.bRC.SCSDEPTH) ) - dTempPosY) //
							/ d3Movement.y;
				}

				// Create 2 new vectors split at the cutoff point, the
				// latter mirrored along the y axis
				Double3D d3TruncMovement = d3Movement.multiply(dCutOff);
				Double3D d3Remainder = new Double3D(d3Movement.x
						- d3TruncMovement.x, -d3Movement.y + d3TruncMovement.y,
						d3Movement.z - d3TruncMovement.z);

				// Replace the current one, then add the new one after
				// it
				if (d3TruncMovement.lengthSq() > 0) {
					getM_d3aMovements().set(i, d3TruncMovement);
					getM_d3aMovements().add(i + 1, d3Remainder);
					// if we don't increment i, it will get flipped again!
					i++;
				} else {
					getM_d3aMovements().set(i, d3Remainder);
				}

				bFlipped = true;
				bBounce = true;
			}

			dTempPosY += d3Movement.y;
		}
		return bBounce;
	}

	/*
	 * Helper method which handles collisions between a B cell and the
	 * simulation borders along the Z-axis
	 */
	private boolean handleBounceZaxis(double dPosZ, int iMovementIndex) {
		// There might be multiple vectors now, so we need to keep track
		// of position, and whether we've hit the wall yet or not
		boolean bBounce = false;

		double dTempPosZ = dPosZ;
		boolean bFlipped = false;

		for (int i = iMovementIndex; i < getM_d3aMovements().size(); i++) {
			Double3D d3Movement = getM_d3aMovements().get(i);

			// if we have already hit the wall, we just flip the y axis
			// of all the movements
			if (bFlipped) {
				getM_d3aMovements().set(i, new Double3D(d3Movement.x, d3Movement.y,
						-d3Movement.z));
				continue;
			}

			// does this sub movement go out of bounds
			if (dTempPosZ + d3Movement.z < 1
					|| dTempPosZ + d3Movement.z > Settings.DEPTH - 1) {
				// Figure out at which point it goes out
				double dCutOff = 1;
				if (dTempPosZ + d3Movement.z < 1) {
					dCutOff = (1 - dTempPosZ) / d3Movement.z;
				} else {
					dCutOff = ((Settings.DEPTH - 1) - dTempPosZ) / d3Movement.z;
				}

				// Create 2 new vectors split at the cutoff point, the latter
				// mirrored along the y axis
				Double3D d3TruncMovement = d3Movement.multiply(dCutOff);
				Double3D d3Remainder = new Double3D(d3Movement.x
						- d3TruncMovement.x, d3Movement.y - d3TruncMovement.y,
						-d3Movement.z + d3TruncMovement.z);

				// Replace the current one, then add the new one after it
				if (d3TruncMovement.lengthSq() > 0) {
					getM_d3aMovements().set(i, d3TruncMovement);
					getM_d3aMovements().add(i + 1, d3Remainder);
					i++; // if we don't increment i, it will get flipped again!
				} else {
					getM_d3aMovements().set(i, d3Remainder);
				}
				bFlipped = true;
				bBounce = true;
			}
			dTempPosZ += d3Movement.z;
		}
		return bBounce;
	}

	/*
	 * This is the 3D model of the B cell. Overrides JAVA 3D so we never
	 * actually call it anywhere in the simulation ourselves (non-Javadoc)
	 * 
	 * @see sim.portrayal3d.SimplePortrayal3D#getModel(java.lang.Object,
	 * javax.media.j3d.TransformGroup)
	 */
	@Override
	public TransformGroup getModel(Object obj, TransformGroup transf) {
		// We choose to always recalculate this model because the movement
		// changes in each time step.
		// Removing the movement indicators and removing this true will make the
		// 3d display a lot faster
		if (transf == null || true) {
			transf = new TransformGroup();

			// Draw the BC itself
			SpherePortrayal3D s = new SpherePortrayal3D(
					Settings.BC.DRAW_COLOR(), Settings.BC.COLLISION_RADIUS * 2,
					6);
			s.setCurrentFieldPortrayal(getCurrentFieldPortrayal());
			TransformGroup localTG = s.getModel(obj, null);

			localTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
			transf.addChild(localTG);

			// if we have had any collisions, draw them as red circles
			// modelCollisions(m_d3aCollisions,obj, transf);

			// If we have any movement, then draw it as white lines telling us
			// where the cell is orientated
			// modelMovements(m_d3aMovements,obj, transf);
		}
		return transf;
	}

	/*
	 * Helper method Model movement of movement and add a white line indicating
	 * the orientation of the B cell
	 */
	protected void modelMovements(List<Double3D> m_d3aMovements2, Object obj,
			TransformGroup transf) {
		// If we have any movement, then draw it
		if (m_d3aMovements2.size() > 0) {
			LineArray lineArr = new LineArray(m_d3aMovements2.size() * 2,
					GeometryArray.COORDINATES);
			lineArr.setCoordinate(0, new Point3d(0, 0, 0));

			int i = 1;
			double xPos = 0, yPos = 0, zPos = 0;

			for (int iIndex = 0; iIndex < m_d3aMovements2.size(); iIndex++) {
				Double3D d3Movement = m_d3aMovements2.get(iIndex);

				if (i > 1) {
					lineArr.setCoordinate(i, new Point3d(xPos, yPos, zPos));
					i++;
				}
				xPos += d3Movement.x;
				yPos += d3Movement.y;
				zPos += d3Movement.z;
				lineArr.setCoordinate(i, new Point3d(xPos, yPos, zPos));
				i++;
			}
			Appearance aAppearance = new Appearance();
			Color col = Color.white;
			aAppearance.setColoringAttributes(new ColoringAttributes(col
					.getRed() / 255f, col.getGreen() / 255f,
					col.getBlue() / 255f, ColoringAttributes.FASTEST));

			Shape3D s3Shape = new Shape3D(lineArr, aAppearance);
			Shape3DPortrayal3D s2 = new Shape3DPortrayal3D(s3Shape, aAppearance);
			s2.setCurrentFieldPortrayal(getCurrentFieldPortrayal());
			TransformGroup localTG2 = s2.getModel(obj, null);

			localTG2.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
			transf.addChild(localTG2);
		}
	}

	/*
	 * Helper method Modelling collisions as red dots, called by getModel()
	 */
	protected void modelCollisions(ArrayList<Double3D> m_d3aCollisions,
			Object obj, TransformGroup transf) {

		if (m_d3aCollisions.size() > 0) // is this a global variable? should
										// pass it into the method!!
		{
			for (Double3D d3Point : m_d3aCollisions) {
				SpherePortrayal3D s2 = new SpherePortrayal3D(Color.RED, 0.25, 6);
				s2.setCurrentFieldPortrayal(getCurrentFieldPortrayal());
				TransformGroup localTG2 = s2.getModel(obj, null);

				Transform3D tTransform = new Transform3D();
				tTransform.setTranslation(new Vector3f((float) d3Point.x,
						(float) d3Point.y, (float) d3Point.z));

				localTG2.setTransform(tTransform);

				localTG2.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
				transf.addChild(localTG2);
			}
		}
	}

	public double getPositionAlongStroma() {
		return positionAlongStroma;
	}

	public void setPositionAlongStroma(double positionAlongStroma) {
		this.positionAlongStroma = positionAlongStroma;
	}

	public int getCollisionCounter() {
		return collisionCounter;
	}

	public void setCollisionCounter(int collisionCounter) {
		this.collisionCounter = collisionCounter;
	}

	public HashSet<Int3D> getM_i3lCollisionPoints() {
		return m_i3lCollisionPoints;
	}

	public void setM_i3lCollisionPoints(HashSet<Int3D> m_i3lCollisionPoints) {
		this.m_i3lCollisionPoints = m_i3lCollisionPoints;
	}

	public List<Double3D> getM_d3aMovements() {
		return m_d3aMovements;
	}

	public void setM_d3aMovements(List<Double3D> m_d3aMovements) {
		this.m_d3aMovements = m_d3aMovements;
	}

	public Double3D getM_d3Face() {
		return m_d3Face;
	}

	public void setM_d3Face(Double3D m_d3Face) {
		this.m_d3Face = m_d3Face;
	}

	public ArrayList<Double3D> getM_d3aCollisions() {
		return m_d3aCollisions;
	}

	public void setM_d3aCollisions(ArrayList<Double3D> m_d3aCollisions) {
		this.m_d3aCollisions = m_d3aCollisions;
	}

	public EnumMap<Receptor, List<Integer>> getM_receptorMap() {
		return m_receptorMap;
	}

	public void setM_receptorMap(EnumMap<Receptor, List<Integer>> m_receptorMap) {
		this.m_receptorMap = m_receptorMap;
	}
	
	public void setM_LR(Lymphocyte.Receptor receptor, Integer value){
		switch (receptor) {
		case CXCR5: 
			this.getM_receptorMap().get(Receptor.CXCR5).set(0, value);
			break;
		case CCR7:
			this.getM_receptorMap().get(Receptor.CCR7).set(0, value);
			break;
		case EBI2:
			this.getM_receptorMap().get(Receptor.EBI2).set(0, value);
			break;
		}
	}
	
	public void setM_Rf(Lymphocyte.Receptor receptor, Integer value){
		switch (receptor) {
		case CXCR5: 
			this.getM_receptorMap().get(Receptor.CXCR5).set(1, value);
			break;
		case CCR7:
			this.getM_receptorMap().get(Receptor.CCR7).set(1, value);
			break;
		case EBI2:
			this.getM_receptorMap().get(Receptor.EBI2).set(1, value);
			break;
		}
	}
	
	public void setM_Ri(Lymphocyte.Receptor receptor, Integer value){
		switch (receptor) {
		case CXCR5: 
			this.getM_receptorMap().get(Receptor.CXCR5).set(2, value);
			break;
		case CCR7:
			this.getM_receptorMap().get(Receptor.CCR7).set(2, value);
			break;
		case EBI2:
			this.getM_receptorMap().get(Receptor.EBI2).set(2, value);
			break;
		}
	}
	
	public void setM_Rd(Lymphocyte.Receptor receptor, Integer value){
		switch (receptor) {
		case CXCR5: 
			this.getM_receptorMap().get(Receptor.CXCR5).set(3, value);
			break;
		case CCR7:
			this.getM_receptorMap().get(Receptor.CCR7).set(3, value);
			break;
		case EBI2:
			this.getM_receptorMap().get(Receptor.EBI2).set(3, value);
			break;
		}
	}
	
	public Integer getM_LR(Lymphocyte.Receptor receptor){
		int output;
		switch (receptor) {
		case CXCR5: 
			output = m_receptorMap.get(Receptor.CXCR5).get(0);
			break;
		case CCR7:
			output = m_receptorMap.get(Receptor.CCR7).get(0);
			break;
		case EBI2:
			output = m_receptorMap.get(Receptor.EBI2).get(0);
			break;
		default:
			output = 0;
			break;
		}	
		return output;
	}
	
	public Integer getM_Rf(Lymphocyte.Receptor receptor){
		int output;
		switch (receptor) {
		case CXCR5: 
			output = m_receptorMap.get(Receptor.CXCR5).get(1);
			break;
		case CCR7:
			output = m_receptorMap.get(Receptor.CCR7).get(1);
			break;
		case EBI2:
			output = m_receptorMap.get(Receptor.EBI2).get(1);
			break;
		default:
			output = 0;
			break;
		}	
		return output;
	}
	
	public Integer getM_Ri(Lymphocyte.Receptor receptor){
		int output;
		switch (receptor) {
		case CXCR5: 
			output = m_receptorMap.get(Receptor.CXCR5).get(2);
			break;
		case CCR7:
			output = m_receptorMap.get(Receptor.CCR7).get(2);
			break;
		case EBI2:
			output = m_receptorMap.get(Receptor.EBI2).get(2);
			break;
			
		default:
			output = 0;
		}	
		return output;
	}
	
	
	public Integer getM_Rd(Lymphocyte.Receptor receptor){
		int output;
		switch (receptor) {
		case CXCR5: 
			output = m_receptorMap.get(Receptor.CXCR5).get(3);
			break;
		case CCR7:
			output = m_receptorMap.get(Receptor.CCR7).get(3);
			break;
		case EBI2:
			output = m_receptorMap.get(Receptor.EBI2).get(3);
			break;
			
		default:
			output = 0;
		}	
		return output;
	}
	
}
