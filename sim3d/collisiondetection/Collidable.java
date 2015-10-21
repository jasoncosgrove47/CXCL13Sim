package sim3d.collisiondetection;

public interface Collidable
{
	public abstract boolean isStatic();
	
	public abstract void addCollisions(CollisionGrid cgGrid);
}
