package sim3d.collisiondetection;

import sim.util.Int3D;

public interface Collidable
{
	public abstract boolean isStatic();
	
	public abstract void addCollisionPoint(Int3D i3Point);
	public abstract void registerCollisions(CollisionGrid cgGrid);
	public abstract void handleCollisions(CollisionGrid cgGrid);
	
	public static enum CLASS
	{
		BC,
		STROMA,
		STROMA_EDGE
	}
	public abstract CLASS getCollisionClass();
}
