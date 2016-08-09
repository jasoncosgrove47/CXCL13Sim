package sim3d.stroma;

import javax.media.j3d.TransformGroup;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.continuous.Continuous3D;
import sim.portrayal3d.simple.SpherePortrayal3D;
import sim.util.Int3D;
import sim3d.Settings;
import sim3d.cell.DrawableCell3D;
import sim3d.collisiondetection.Collidable;
import sim3d.collisiondetection.CollisionGrid;

public class MRC extends FRC {


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

}
