package sim3d.collisiondetection;

import sim.util.Int3D;
/**
 * Interface implemented by objects that want to register on the CollisionGrid
 * 
 * @author Simon Jarrett - {@link simonjjarrett@gmail.com}
 */
public interface Collidable
{
	/**
	 * @return True if the Collidable does not move
	 */
	public abstract boolean isStatic();
	
	/**
	 * Notify the Collidable of a potential collision point
	 * @param i3Point Coordinates of the collision point
	 */
	public abstract void addCollisionPoint(Int3D i3Point);
	
	/**
	 * Prompt the Collidable to register its potential collisions with the CollisionGrid
	 * @param cgGrid The CollisionGrid to register with
	 */
	public abstract void registerCollisions(CollisionGrid cgGrid);
	
	/**
	 * Prompt the Collidable to check for and handle any collisions
	 * @param cgGrid The CollisionGrid containing the potential collidables
	 */
	public abstract void handleCollisions(CollisionGrid cgGrid);
	
	/**
	 * ENUM for the types of collidable objects
	 */
	public static enum CLASS
	{
		BC,
		STROMA,
		STROMA_EDGE
	}
	/**
	 * Accessor for the CLASS ENUM
	 * @return The type of Collidable for this object
	 */
	public abstract CLASS getCollisionClass();
}
