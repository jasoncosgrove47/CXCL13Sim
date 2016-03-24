package sim3d.collisiondetection;

import java.util.List;
import java.util.ArrayList;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Double3D;
import sim.util.Int3D;
import sim3d.util.Vector3DHelper;

/**
 * Class to handle collisions in the system. Has a discretised grid, and for
 * each time step, agents register the grid spaces they will be interacting
 * with. The class keeps track of each grid space, and any cells in a grid
 * spaces with two or more interactions are prompted to handleCollisions(). This
 * process is repeated until no collisions occur.
 * 
 * @author Simon Jarrett - {@link simonjjarrett@gmail.com}
 */
public class CollisionGrid implements Steppable {

	private static final long serialVersionUID = 1L;

	/**
	 * A 3D array of lists of Collidable objects containing the objects that
	 * will interact with that grid space for that time step
	 */
	private List<Collidable>[][][] m_clGridSpaces;

	/**
	 * Keeps track of when each grid space last was updated. Allows us to only
	 * update the grid spaces that have cells in them at each time step.
	 */
	private int[][][] m_ia3GridUpdateStep;

	/**
	 * List of coordinates for grid spaces which have had collisions in this
	 * time step. Allows us to avoid having to loop though the whole grid each
	 * time step.
	 */
	private List<Int3D> m_i3lCollisionPoints = new ArrayList<Int3D>();

	/**
	 * The size of the collision grid. Larger means less memory, but more
	 * calculations.
	 */
	private double m_dDiscretisation;

	/**
	 * Width, Height and Depth of the collision grid
	 */
	private int m_iWidth, m_iHeight, m_iDepth;

	/**
	 * Keeps track of the current step (used in conjunction with
	 * m_ia3GridUpdateStep)
	 */
	private int m_iCurrentStep = 0;

	/**
	 * Adds a Collidable object to a grid location. Performs checks as to
	 * whether potential collisions exist, and notifies the relevant agents if
	 * so. Note: we are using a ArrayList without type because it is of the
	 * Collidable abstract type
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @param cObject
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void addCollisionPotential(int x, int y, int z, Collidable cObject) {
		// Check if we need to initialise the grid space
		if (m_clGridSpaces[x][y][z] == null) {
			m_clGridSpaces[x][y][z] = new ArrayList();
		}

		// If we haven't seen this grid space on the current time step
		if (m_ia3GridUpdateStep[x][y][z] < m_iCurrentStep) {
			m_ia3GridUpdateStep[x][y][z] = m_iCurrentStep;

			int i = 0;

			// Loop through each element in the grid space and remove non-static
			// elements; Stroma is static and therefore the only thing we can
			// collide with
			// therefore don't need to worry about non-static objects...
			while (i < m_clGridSpaces[x][y][z].size()) {
				if (!m_clGridSpaces[x][y][z].get(i).isStatic()) {
					m_clGridSpaces[x][y][z].remove(i);
				} else {
					i++;
				}
			}
		}

		// add to the list of colliding coordinates
		if (!getM_i3lCollisionPoints().contains(new Int3D(x, y, z))) {
			getM_i3lCollisionPoints().add(new Int3D(x, y, z));
		}

		// check if it's already collided
		if (m_clGridSpaces[x][y][z].contains(cObject)) {
			return;
		}

		// Add the new object to the grid space
		m_clGridSpaces[x][y][z].add(cObject);

		// If there is now exactly two Collidables in this grid space
		if (m_clGridSpaces[x][y][z].size() == 2) {

			// There's a potential collision so tell the other cell, too
			m_clGridSpaces[x][y][z].get(0)
					.addCollisionPoint(new Int3D(x, y, z));
			cObject.addCollisionPoint(new Int3D(x, y, z));
		}

		else if (m_clGridSpaces[x][y][z].size() > 2) {

			// There's a potential collision so tell the cells, too
			for (Collidable cCollidable : m_clGridSpaces[x][y][z]) {
				cCollidable.addCollisionPoint(new Int3D(x, y, z));
			}
		}
	}

	/**
	 * @param iWidth
	 *            Width of the grid
	 * @param iHeight
	 *            Height of the grid
	 * @param iDepth
	 *            Depth of the grid
	 * @param dDiscretisation
	 *            Size of each grid space
	 */
	@SuppressWarnings("unchecked")
	public CollisionGrid(int iWidth, int iHeight, int iDepth,
			double dDiscretisation) {
		m_dDiscretisation = dDiscretisation;// if set to 4 then the collision
											// grid for a cell would be 4*4*4
		m_iWidth = (int) Math.ceil(iWidth / dDiscretisation);
		m_iHeight = (int) Math.ceil(iHeight / dDiscretisation);
		m_iDepth = (int) Math.ceil(iDepth / dDiscretisation);

		m_clGridSpaces = new ArrayList[m_iWidth][m_iHeight][m_iDepth];
		m_ia3GridUpdateStep = new int[m_iWidth][m_iHeight][m_iDepth];
	}

	/**
	 * Accessor for the collidables at a specified point Note: we do not need to
	 * check whether this point has been updated in the last time step as this
	 * should only be called by Collidables that are registered in the location
	 * given.
	 * 
	 * @param i3Loc
	 *            Point in grid to query
	 * @return The Collidables registered at that point
	 */
	public List<Collidable> getPoints(Int3D i3Loc) {
		return m_clGridSpaces[i3Loc.x][i3Loc.y][i3Loc.z];
	}

	/**
	 * Performs a collision detection test between a grid space and a sphere
	 * 
	 * calculate the euclidean distance between the center of the box and the
	 * sphere squared in each axis The sphere intersects the gridspace if the
	 * summed distance is less than the radius of the square
	 * 
	 * 
	 * @param dSphereX
	 *            X coordinate of the sphere's origin
	 * @param dSphereY
	 *            Y coordinate of the sphere's origin
	 * @param dSphereZ
	 *            Z coordinate of the sphere's origin
	 * @param dRadiusSquare
	 *            Squared radius of the sphere
	 * @param iBoxX
	 *            X Coordinate of the grid space
	 * @param iBoxY
	 *            Y Coordinate of the grid space
	 * @param iBoxZ
	 *            Z Coordinate of the grid space
	 * @return True if the sphere intersects with the grid space
	 */
	public boolean BoxSphereIntersect(double dSphereX, double dSphereY,
			double dSphereZ, double dRadiusSquare, int iBoxX, int iBoxY,
			int iBoxZ) {
		double dSum = 0;

		// Basically, we do Pythagoras in 3D, but only if for each dimension,
		// we're outside the box (otherwise distance will be 0)
		if (dSphereX < iBoxX)// if box further along the x-axis than sphere
		{
			dSum += (dSphereX - iBoxX) * (dSphereX - iBoxX); // add squared
																// distance to
																// dSum
		}

		// if sphere further along the x-axis than the width of box (add 1
		// because the width of the box is 1
		else if (dSphereX > iBoxX + 1) {
			dSum += (dSphereX - iBoxX + 1) * (dSphereX - iBoxX + 1);
		}

		if (dSphereY < iBoxY) {
			dSum += (dSphereY - iBoxY) * (dSphereY - iBoxY);
		} else if (dSphereY > iBoxY + 1) {
			dSum += (dSphereY - iBoxY + 1) * (dSphereY - iBoxY + 1);
		}

		if (dSphereZ < iBoxZ) {
			dSum += (dSphereZ - iBoxZ) * (dSphereZ - iBoxZ);
		} else if (dSphereZ > iBoxZ + 1) {
			dSum += (dSphereZ - iBoxZ + 1) * (dSphereZ - iBoxZ + 1);
		}

		return dSum * dSum < dRadiusSquare;
	}

	/**
	 * Adds a sphere to the collision grid get the discretised collision area
	 * around the object
	 * 
	 * 
	 * @param cObject
	 *            The Collidable to add to the grid
	 * @param d3Centre
	 *            The centre of the sphere
	 * @param dRadius
	 *            The radius of the sphere
	 */
	public void addSphereToGrid(Collidable cObject, Double3D d3Centre,
			double dRadius) {
		// Convert the coordinates to our discretised coordinates
		Double3D d3DiscretisedCentre = new Double3D(d3Centre.x
				/ m_dDiscretisation, d3Centre.y / m_dDiscretisation, d3Centre.z
				/ m_dDiscretisation);

		double dDiscretisedRadius = dRadius / m_dDiscretisation;

		// this will be used a lot - pre compute for speed
		double dRadiusSquare = dDiscretisedRadius * dDiscretisedRadius;

		// Calculate the grid space coordinate bounds for each dimension
		int iXLow = (int) Math.max(0,
				(d3DiscretisedCentre.x - dDiscretisedRadius));
		int iXHigh = (int) Math.min(m_iWidth - 1,
				(d3DiscretisedCentre.x + dDiscretisedRadius));

		int iYLow = (int) Math.max(0,
				(d3DiscretisedCentre.y - dDiscretisedRadius));
		int iYHigh = (int) Math.min(m_iHeight - 1,
				(d3DiscretisedCentre.y + dDiscretisedRadius));

		int iZLow = (int) Math.max(0,
				(d3DiscretisedCentre.z - dDiscretisedRadius));
		int iZHigh = (int) Math.min(m_iDepth - 1,
				(d3DiscretisedCentre.z + dDiscretisedRadius));

		// might be room for optimisation here
		for (int x = iXLow; x <= iXHigh; x++) {
			for (int y = iYLow; y <= iYHigh; y++) {
				for (int z = iZLow; z <= iZHigh; z++) {
					if (BoxSphereIntersect(d3DiscretisedCentre.x,
							d3DiscretisedCentre.y, d3DiscretisedCentre.z,
							dRadiusSquare, x, y, z)) {
						addCollisionPotential(x, y, z, cObject);
					}
				}
			}
		}
	}

	/**
	 * Add a line (cylinder) to the collision grid
	 * 
	 * @param cObject
	 *            The Collidable to add to the grid
	 * @param d3Point1
	 *            The start point of the line
	 * @param d3Point2
	 *            The end point of the line
	 * @param dRadius
	 *            The thickness of the line
	 */
	public void addLineToGrid(Collidable cObject, Double3D d3Point1,
			Double3D d3Point2, double dRadius) {
		// Convert the coordinates to our discretised coordinates
		// eg if point was (10,10,5) and discretisation was 3
		// then new values would be (2,2,1) cos we divide and floor
		// so we shrink the grid by a factor of 3
		Double3D d3DiscretisedPoint1 = new Double3D(d3Point1.x
				/ m_dDiscretisation, d3Point1.y / m_dDiscretisation, d3Point1.z
				/ m_dDiscretisation);
		Double3D d3DiscretisedPoint2 = new Double3D(d3Point2.x
				/ m_dDiscretisation, d3Point2.y / m_dDiscretisation, d3Point2.z
				/ m_dDiscretisation);

		double dDiscretisedRadius = dRadius / m_dDiscretisation;

		// this will be used a lot - pre compute for speed
		// Add 0.866 as this is approximately the radius of a cube
		// this won't detect collisions in the corners of the grid spaces,
		// but it's much more efficient!
		double dRadiusSquare = (0.866 + dDiscretisedRadius)
				* (0.5 + dDiscretisedRadius);

		// Calculate the grid space coordinate bounds for each dimension
		int iXLow, iXHigh, iYLow, iYHigh, iZLow, iZHigh;

		// take the smallest x as lower bound, highest x as upper bound in each
		// axis
		// this is the space in which we will iterate to see in which points the
		// cylinder is
		// interacting
		if (d3DiscretisedPoint1.x < d3DiscretisedPoint2.x) {
			iXLow = (int) Math.max(0,
					(d3DiscretisedPoint1.x - dDiscretisedRadius));
			iXHigh = (int) Math.min(m_iWidth - 1,
					(d3DiscretisedPoint2.x + dDiscretisedRadius));
		} else {
			iXLow = (int) Math.max(0,
					(d3DiscretisedPoint2.x - dDiscretisedRadius));
			iXHigh = (int) Math.min(m_iWidth - 1,
					(d3DiscretisedPoint1.x + dDiscretisedRadius));
		}

		if (d3DiscretisedPoint1.y < d3DiscretisedPoint2.y) {
			iYLow = (int) Math.max(0,
					(d3DiscretisedPoint1.y - dDiscretisedRadius));
			iYHigh = (int) Math.min(m_iHeight - 1,
					(d3DiscretisedPoint2.y + dDiscretisedRadius));
		} else {
			iYLow = (int) Math.max(0,
					(d3DiscretisedPoint2.y - dDiscretisedRadius));
			iYHigh = (int) Math.min(m_iHeight - 1,
					(d3DiscretisedPoint1.y + dDiscretisedRadius));
		}

		if (d3DiscretisedPoint1.z < d3DiscretisedPoint2.z) {
			iZLow = (int) Math.max(0,
					(d3DiscretisedPoint1.z - dDiscretisedRadius));
			iZHigh = (int) Math.min(m_iDepth - 1,
					(d3DiscretisedPoint2.z + dDiscretisedRadius));
		} else {
			iZLow = (int) Math.max(0,
					(d3DiscretisedPoint2.z - dDiscretisedRadius));
			iZHigh = (int) Math.min(m_iDepth - 1,
					(d3DiscretisedPoint1.z + dDiscretisedRadius));
		}

		for (int x = iXLow; x <= iXHigh; x++) {
			for (int y = iYLow; y <= iYHigh; y++) {
				for (int z = iZLow; z <= iZHigh; z++) {
					// Real-Time Collision Detection, Christer Ericson
					// https://q3k.org/gentoomen/Game%20Development/Programming/Real-Time%20Collision%20Detection.pdf
					// p130

					// A (point1) to B (point2)
					Double3D ab = d3DiscretisedPoint2
							.subtract(d3DiscretisedPoint1);

					// add 0.5 so we're using the centre of the square
					// A to C (GridSpace)
					Double3D ac = new Double3D(x + 0.5 - d3DiscretisedPoint1.x,
							y + 0.5 - d3DiscretisedPoint1.y, z + 0.5
									- d3DiscretisedPoint1.z);

					double length = 0;
					double e = Vector3DHelper.dotProduct(ac, ab);

					// If the grid space is the opposite direction of the line
					// we just use the distance of AC
					if (e <= 0) {
						length = Vector3DHelper.dotProduct(ac, ac);
					} else {
						double f = Vector3DHelper.dotProduct(ab, ab);

						// the opposite of the previous check - if the grid
						// space is past the line we just use the distance of BC
						if (e >= f) {
							Double3D bc = new Double3D(x + 0.5
									- d3DiscretisedPoint2.x, y + 0.5
									- d3DiscretisedPoint2.y, z + 0.5
									- d3DiscretisedPoint2.z);

							length = Vector3DHelper.dotProduct(bc, bc);
						} else {
							// explained in the linked book!
							length = Vector3DHelper.dotProduct(ac, ac) - e * e
									/ f;
						}
					}

					// If the length is within the radius then add to the grid
					if (length <= dRadiusSquare) {
						addCollisionPotential(x, y, z, cObject);
					}
				}
			}
		}
	}

	/**
	 * Prompts the cells to handle the collisions. Repeats until no more
	 * collisions have been registered.
	 */
	@Override
	public void step(SimState state) {
		// while we still have points to check
		while (getM_i3lCollisionPoints().size() > 0) {
			Int3D i3CollisionPoint = getM_i3lCollisionPoints().get(0);
			getM_i3lCollisionPoints().remove(0);

			// List through all the points at this location
			// Hopefully most will just immediately return!
			List<Collidable> cPoints = getPoints(i3CollisionPoint);

			int iMax = cPoints.size();
			for (int i = 0; i < iMax; i++) {
				// Note: this command can get this location reregistered! (in
				// fact it's likely if a collision occurs)
				cPoints.get(i).handleCollisions(this);
			}
		}

		getM_i3lCollisionPoints().clear();
		m_iCurrentStep++;
	}

	public List<Int3D> getM_i3lCollisionPoints() {
		return m_i3lCollisionPoints;
	}

	public void setM_i3lCollisionPoints(List<Int3D> m_i3lCollisionPoints) {
		this.m_i3lCollisionPoints = m_i3lCollisionPoints;
	}
}
