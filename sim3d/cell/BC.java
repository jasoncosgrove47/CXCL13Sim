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
import sim3d.cell.Lymphocyte.Receptor;
import sim3d.collisiondetection.Collidable;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.diffusion.Chemokine;
import sim3d.migration.Algorithm1;
import sim3d.migration.Algorithm2;
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
public class BC extends Lymphocyte{
	

	private static boolean multipleChemokines = true;
	
	/*
	 * This is the algorithm which controls BC migration
	 */
	private Algorithm2 a2 = new Algorithm2();
	private Algorithm1 a1 = new Algorithm1();
	
	/**
	 * Controls what a B cell agent does for each time step Each Bcell registers
	 * its intended path on the collision grid, once all B cells register the
	 * collision grid handles the movement at the next iteration the B cells are
	 * moved. B cells only collide with stroma
	 */
	
	@Override
	public void step(final SimState state)// why is this final here
	{
		if(isMultipleChemokines()){migrate(a2);}
		else{
			migrate(a1);
		}
	}

	public static boolean isMultipleChemokines() {
		return multipleChemokines;
	}

	public static void setMultipleChemokines(boolean multipleChemokines) {
		BC.multipleChemokines = multipleChemokines;
	}

	

	
}
