package sim3d.cell;

import sim.util.*;
import sim.engine.*;
import sim.field.continuous.Continuous3D;

import java.awt.Color;
import java.util.ArrayList;
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

import sim.portrayal3d.simple.Shape3DPortrayal3D;
import sim.portrayal3d.simple.SpherePortrayal3D;
import sim3d.Settings;
import sim3d.SimulationEnvironment;
import sim3d.collisiondetection.Collidable;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.diffusion.Chemokine;
import sim3d.migration.Algorithm1;
import sim3d.migration.MigrationAlgorithm;
import sim3d.migration.MigratoryCell;
import sim3d.stroma.StromaEdge;
import sim3d.util.ODESolver;
import sim3d.util.Vector3DHelper;

/**
 * A B-cell agent. Performs chemotaxis/random movement based on the presence of
 * surrounding chemokine and the amount of receptors the cell is expressing. The
 * receptors are controlled by an ODE. The calculated movement is checked to see
 * whether it collides with the edges or other elements before being realised.
 * 
 * @author Jason Cosgrove - {@link jc1571@york.ac.uk}
 * @author Simon Jarrett - {@link simonjjarrett@gmail.com}
 */
public class TC extends Lymphocyte{
	

	/**
	 * Controls what a B cell agent does for each time step Each Bcell registers
	 * its intended path on the collision grid, once all B cells register the
	 * collision grid handles the movement at the next iteration the B cells are
	 * moved. B cells only collide with stroma
	 */
	@Override
	public void step(final SimState state)// why is this final here
	{
		super.step(state);		
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

			
			java.awt.Color red = new Color(255, 0, 0, 200);
			
			// Draw the BC itself
			SpherePortrayal3D s = new SpherePortrayal3D(
					red, Settings.BC.COLLISION_RADIUS * 2,
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
	
	
	
}
