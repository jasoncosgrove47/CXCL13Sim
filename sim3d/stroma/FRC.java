package sim3d.stroma;


import javax.media.j3d.TransformGroup;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.continuous.Continuous3D;
import sim.portrayal3d.simple.SpherePortrayal3D;
import sim.util.Double3D;
import sim.util.Int3D;
import sim3d.Settings;
import sim3d.cell.DrawableCell3D;
import sim3d.collisiondetection.Collidable;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.diffusion.Chemokine;

public class FRC extends DrawableCell3D implements Steppable, Collidable  {


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
					Settings.FRC.DRAW_COLOR(),
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
		Chemokine.add(Chemokine.TYPE.CCL19, (int) x, (int) y, (int) z,
				Settings.FDC.CXCL13_EMITTED());
		
	

	}

}
