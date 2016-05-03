package sim3d.cell;

import sim.engine.*;
import sim.field.continuous.Continuous3D;

import javax.media.j3d.TransformGroup;

import sim.portrayal3d.simple.SpherePortrayal3D;
import sim.util.Double3D;
import sim.util.Int3D;
import sim3d.Settings;
import sim3d.SimulationEnvironment;
import sim3d.collisiondetection.Collidable;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.diffusion.ParticleMoles;

/**
 * An FDC agent. Represents the nucleus of the FDC, and handles the secretion of
 * chemokine to the particle grid.
 * 
 * @author Jason Cosgrove, Simon Jarrett
 */
public class FDC extends DrawableCell3D implements Steppable, Collidable {

	/**
	 * The drawing environment that houses this cell; used by
	 * DrawableCell3D.setObjectLocation
	 */
	public static Continuous3D drawEnvironment;

	private static final long serialVersionUID = 1L;

	@Override
	public void addCollisionPoint(Int3D i3Point) {
		// We're not interested in collisions as we're static
		return;
	}

	@Override
	public CLASS getCollisionClass() {
		return CLASS.STROMA;
	}

	@Override
	public Continuous3D getDrawEnvironment() {
		return drawEnvironment;
	}

	// Create a model to visualising the stroma node in 3D
	public TransformGroup getModel(Object obj, TransformGroup transf) {
		if (transf == null) {
			transf = new TransformGroup();

			SpherePortrayal3D s = new SpherePortrayal3D(
					Settings.FDC.DRAW_COLOR(),
					Settings.FDC.STROMA_NODE_RADIUS * 2, 6);
			s.setCurrentFieldPortrayal(getCurrentFieldPortrayal());
			TransformGroup localTG = s.getModel(obj, null);

			localTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
			transf.addChild(localTG);
		}
		return transf;
	}

	@Override
	public void handleCollisions(CollisionGrid cgGrid) {
		return;
	}

	@Override
	public boolean isStatic() {
		return true;
	}

	@Override
	public void registerCollisions(CollisionGrid cgGrid) {
		cgGrid.addSphereToGrid(this, new Double3D(x, y, z),
				Settings.FDC.STROMA_NODE_RADIUS);
	}

	@Override
	public void step(final SimState state) {

		// secrete chemokine
		ParticleMoles.add(ParticleMoles.TYPE.CXCL13, (int) x, (int) y, (int) z,
				Settings.FDC.CXCL13_EMITTED());
		
	

	}
}
