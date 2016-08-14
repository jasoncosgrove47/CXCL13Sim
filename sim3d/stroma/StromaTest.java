/**
 * 
 */
package sim3d.stroma;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;

import javax.media.j3d.TransformGroup;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import ec.util.MersenneTwisterFast;
import sim.engine.Schedule;
import sim.field.continuous.Continuous3D;
import sim.util.Double3D;
import sim.util.Int3D;
import sim3d.Settings;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.collisiondetection.Collidable.CLASS;
import sim3d.diffusion.Chemokine;
import sim3d.util.StromaGenerator;
import sim3d.util.IO;
import sim3d.util.StromaGenerator.StromalCell;

public class StromaTest {

	@Before
	public void setUp() throws Exception {
		Settings.RNG = new MersenneTwisterFast();
		// Initialise the stromal grid
		Continuous3D fdcEnvironment = new Continuous3D(
				Settings.FDC.DISCRETISATION, 60, 60, 10);

	}

	
	/*
	 * Make sure that the FDC can display antigen
	 * TODO make sure that MRCs and the rest of them
	 * don't display antigen
	 */
	@Test
	public void canDisplayAntigen() {

		Settings.FDC.STARTINGANTIGENLEVEL = 100;
		
		StromaEdge rc = new StromaEdge(new Double3D(0,0,0),new Double3D(0,0,0), StromaEdge.TYPE.RC_edge);
		assertEquals(rc.getAntigenLevel(),0);
		
		StromaEdge fdcbranch = new StromaEdge(new Double3D(0,0,0),new Double3D(0,0,0), StromaEdge.TYPE.FDC_branch);
		assertThat(fdcbranch.getAntigenLevel(), greaterThan(0));
		
		ArrayList<StromalCell> d3lCellLocations = new ArrayList<StromalCell>();
		ArrayList<StromaEdge> selEdges = new ArrayList<StromaEdge>();
		StromaGenerator.generateStroma3D(50, 50, 5, 350, d3lCellLocations,
				selEdges);
		

		
		for (StromaEdge seEdge : selEdges) {
			seEdge.getAntigenLevel();
			assertThat(seEdge.getAntigenLevel(), greaterThan(0));
		}
	}

	/*
	 * Assert that the FDC is a static object
	 */
	@Test
	public void isStatic() {

		Stroma frc = new Stroma(Stroma.TYPE.FRC);
		assertTrue(frc.isStatic());
		Stroma mrc = new Stroma(Stroma.TYPE.MRC);
		assertTrue(mrc.isStatic());
		Stroma lec = new Stroma(Stroma.TYPE.LEC);
		assertTrue(lec.isStatic());
		
		
		Stroma fdc = new Stroma(Stroma.TYPE.FDC);
		assertTrue(fdc.isStatic());

	}

	/**
	 * Make sure that the FDC can lose antigen
	 */
	@Test
	public void canLoseAntigen() {

		ArrayList<StromalCell> d3lCellLocations2 = new ArrayList<StromalCell>();
		ArrayList<StromaEdge> selEdges2 = new ArrayList<StromaEdge>();

		Settings.FDC.STARTINGANTIGENLEVEL = 100;

		StromaGenerator.generateStroma3D(5, 5, 5, 5, d3lCellLocations2,
				selEdges2);

		for (StromaEdge seEdge : selEdges2) {

			// int antigenLevel = seEdge.getAntigen();
			seEdge.setAntigenLevel(seEdge.getAntigenLevel() - 1);
			assertThat(seEdge.getAntigenLevel(), lessThan(100));
		}

	}

	/**
	 * Test that getModel returns an object of type TransformGroup
	 */
	@Test
	public void testGetModel() {
		Stroma c = new Stroma(Stroma.TYPE.FDC);
		TransformGroup localTG = c.getModel(c, null);
		assertTrue(localTG instanceof TransformGroup);
	}

	/**
	 * Test that registerCollisions updates m_i3lCollisionPoints
	 */
	@Test
	public void testRegisterCollisions() {

		Settings.FDC.STROMA_NODE_RADIUS = 1;

		CollisionGrid cgGrid_fdc = new CollisionGrid(31, 31, 31, 1);
		Stroma fdc = new Stroma(Stroma.TYPE.FDC);
		fdc.registerCollisions(cgGrid_fdc);
		assertEquals(true, cgGrid_fdc.getM_i3lCollisionPoints().size() > 0);
		
		CollisionGrid cgGrid_frc = new CollisionGrid(31, 31, 31, 1);
		Stroma frc = new Stroma(Stroma.TYPE.FRC);
		frc.registerCollisions(cgGrid_frc);
		assertEquals(true, cgGrid_frc.getM_i3lCollisionPoints().size() > 0);
		
		CollisionGrid cgGrid_mrc = new CollisionGrid(31, 31, 31, 1);
		Stroma mrc = new Stroma(Stroma.TYPE.FDC);
		mrc.registerCollisions(cgGrid_mrc);
		assertEquals(true, cgGrid_mrc.getM_i3lCollisionPoints().size() > 0);
		
		CollisionGrid cgGrid_lec = new CollisionGrid(31, 31, 31, 1);
		Stroma lec = new Stroma(Stroma.TYPE.FDC);
		lec.registerCollisions(cgGrid_lec);
		assertEquals(true, cgGrid_lec.getM_i3lCollisionPoints().size() > 0);
	}

	/**
	 * test that the FDC can secrete chemokine
	 * 
	 * TODO tests that ensure that each cell type
	 * secretes the correct type of chemokine.
	 */
	@Test
	public void testCXCL13SECRETING() {

		// initialise the system
		Schedule schedule = new Schedule();
		Continuous3D fdcEnvironment = new Continuous3D(
				Settings.FDC.DISCRETISATION, 60, 60, 10);
		Stroma.drawEnvironment = fdcEnvironment;
		Chemokine m_pParticle = new Chemokine(schedule,
				Chemokine.TYPE.CXCL13, 60, 60, 10);
		Settings.FDC.CXCL13_EMITTED = 100;
		Stroma fdc = new Stroma(Stroma.TYPE.FDC);
		fdc.setObjectLocation(new Double3D(15, 15, 5));

		// assert that there is currently no chemokine on the grid
		double[][][] chemokinebefore = Chemokine.get(
				Chemokine.TYPE.CXCL13, 15, 15, 5);
		assertThat(chemokinebefore[1][1][1], equalTo(0.0));

		// step the FDC
		fdc.step(null);
		fdc.step(null);
		fdc.step(null);
		fdc.step(null);
		fdc.step(null);

		// assert that there is now chemokine on the grid.
		double[][][] chemokine = Chemokine.get(Chemokine.TYPE.CXCL13,
				15, 15, 5);
		assertThat(chemokine[1][1][1], greaterThan(0.0));

		Chemokine.reset();
		Stroma.drawEnvironment = null;

	}

	/**
	 * Make sure that getCollsionClass returns the correct enum type
	 */
	@Test
	public void testGetCollisionClass() {
		Stroma fdc = new Stroma(Stroma.TYPE.FDC);
		assertEquals(fdc.getCollisionClass(), CLASS.STROMA);
		
		Stroma frc = new Stroma(Stroma.TYPE.FRC);
		assertEquals(frc.getCollisionClass(), CLASS.STROMA);
		
		Stroma mrc = new Stroma(Stroma.TYPE.MRC);
		assertEquals(mrc.getCollisionClass(), CLASS.STROMA);
		
		Double3D d1 = new Double3D(0,0,0);
		Double3D d2 = new Double3D(1,1,1);
		
		StromaEdge fdcedge = new StromaEdge(d1,d2,StromaEdge.TYPE.FDC_edge);
		StromaEdge frcedge = new StromaEdge(d1,d2,StromaEdge.TYPE.RC_edge);
		StromaEdge fdcbranch = new StromaEdge(d1,d2,StromaEdge.TYPE.FDC_branch);
		
		assertEquals(fdcedge.getCollisionClass(), CLASS.STROMA_EDGE);
		assertEquals(frcedge.getCollisionClass(), CLASS.STROMA_EDGE);
		assertEquals(fdcbranch.getCollisionClass(), CLASS.STROMA_EDGE);
	}

}
