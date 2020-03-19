/**
 * 
 */
package sim3d.stroma;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.awt.Color;
import java.util.ArrayList;
import javax.media.j3d.TransformGroup;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ec.util.MersenneTwisterFast;
import sim.engine.Schedule;
import sim.field.continuous.Continuous3D;
import sim.util.Double3D;
import sim3d.Settings;
import sim3d.SimulationEnvironment;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.collisiondetection.Collidable.CLASS;
import sim3d.diffusion.Chemokine;
import sim3d.util.StromaGenerator;
import sim3d.util.StromaGenerator.StromalCelltemp;

public class StromaTest {

	@Before
	public void setUp() throws Exception {
		Settings.RNG = new MersenneTwisterFast();
	}

	@After
	public void tearDown() throws Exception {
		if(SimulationEnvironment.simulation != null){
			SimulationEnvironment.simulation.finish();
		}
	}
	
	/*
	 * Make sure that the FDC/MRC can display antigen
	 * and make sure that BRCs and the rest of them
	 * don't display antigen
	 */
	@Test
	public void canDisplayAntigen() {

		Settings.FDC.STARTINGANTIGENLEVEL = 100;
		
		StromaEdge rc = new StromaEdge(new Double3D(0,0,0),new Double3D(0,0,0), StromaEdge.TYPE.RC_edge);
		assertEquals(rc.getAntigenLevel(),0);
		
		StromaEdge fdcbranch = new StromaEdge(new Double3D(0,0,0),new Double3D(0,0,0), StromaEdge.TYPE.FDC_edge);
		assertThat(fdcbranch.getAntigenLevel(), greaterThan(0));
		
		StromaEdge mrcbranch = new StromaEdge(new Double3D(0,0,0),new Double3D(0,0,0), StromaEdge.TYPE.MRC_edge);
		assertThat(mrcbranch.getAntigenLevel(), greaterThan(0));
		
		ArrayList<StromalCelltemp> d3lCellLocations = new ArrayList<StromalCelltemp>();
		ArrayList<StromaEdge> selEdges = new ArrayList<StromaEdge>();
		
		
		StromaGenerator.generateStroma3D_Updated(50, 50, 5, 350, d3lCellLocations,
				selEdges);
		
		for (StromaEdge seEdge : selEdges) {
			seEdge.getAntigenLevel();
			if(seEdge.getStromaedgetype() == StromaEdge.TYPE.FDC_edge 
					|| seEdge.getStromaedgetype() == StromaEdge.TYPE.MRC_edge ){
				assertThat(seEdge.getAntigenLevel(), greaterThan(0));
			}
		}
	}

	/*
	 * Assert that the FDC is a static object
	 */
	@Test
	public void isStatic() {
		
		Double3D loc = new Double3D(0,0,0);

		Stroma frc = new Stroma(Stroma.TYPE.bRC,loc);
		assertTrue(frc.isStatic());
		Stroma mrc = new Stroma(Stroma.TYPE.MRC,loc);
		assertTrue(mrc.isStatic());
		Stroma lec = new Stroma(Stroma.TYPE.LEC,loc);
		assertTrue(lec.isStatic());
		
		Stroma fdc = new Stroma(Stroma.TYPE.FDC,loc);
		assertTrue(fdc.isStatic());

	}

	/**
	 * Make sure that the FDC can lose antigen
	 */
	@Test
	public void canLoseAntigen() {

		ArrayList<StromalCelltemp> d3lCellLocations2 = new ArrayList<StromalCelltemp>();
		ArrayList<StromaEdge> selEdges2 = new ArrayList<StromaEdge>();

		Settings.FDC.STARTINGANTIGENLEVEL = 100;

		StromaGenerator.generateStroma3D_Updated(5, 5, 5, 5, d3lCellLocations2,
				selEdges2);

		for (StromaEdge seEdge : selEdges2) {

			seEdge.setAntigenLevel(seEdge.getAntigenLevel() - 1);
			assertThat(seEdge.getAntigenLevel(), lessThan(100));
		}

	}

	/**
	 * Test that getModel returns an object of type TransformGroup
	 */
	@Test
	public void testGetModel() {
		Double3D loc = new Double3D(0,0,0);
		
		Stroma c = new Stroma(Stroma.TYPE.FDC,loc);
		TransformGroup localTG = c.getModel(c, null);
		assertTrue(localTG instanceof TransformGroup);
		
		Stroma c2 = new Stroma(Stroma.TYPE.bRC,loc);
		TransformGroup localTG2 = c2.getModel(c2, null);
		assertTrue(localTG2 instanceof TransformGroup);
		
		Stroma c3 = new Stroma(Stroma.TYPE.MRC,loc);
		TransformGroup localTG3 = c3.getModel(c3, null);
		assertTrue(localTG3 instanceof TransformGroup);
		
		Stroma c4 = new Stroma(Stroma.TYPE.LEC,loc);
		TransformGroup localTG4 = c4.getModel(c4, null);
		assertTrue(localTG4 instanceof TransformGroup);
	}

	/**
	 * Test that registerCollisions updates m_i3lCollisionPoints
	 */
	@Test
	public void testRegisterCollisions() {

		Double3D loc = new Double3D(0,0,0);
		
		Settings.FDC.STROMA_NODE_RADIUS = 1;

		CollisionGrid cgGrid_fdc = new CollisionGrid(31, 31, 31, 1);
		Stroma fdc = new Stroma(Stroma.TYPE.FDC,loc);
		fdc.registerCollisions(cgGrid_fdc);
		assertEquals(true, cgGrid_fdc.getM_i3lCollisionPoints().size() > 0);
		
		CollisionGrid cgGrid_frc = new CollisionGrid(31, 31, 31, 1);
		Stroma frc = new Stroma(Stroma.TYPE.bRC,loc);
		frc.registerCollisions(cgGrid_frc);
		assertEquals(true, cgGrid_frc.getM_i3lCollisionPoints().size() > 0);
		
		CollisionGrid cgGrid_mrc = new CollisionGrid(31, 31, 31, 1);
		Stroma mrc = new Stroma(Stroma.TYPE.FDC,loc);
		mrc.registerCollisions(cgGrid_mrc);
		assertEquals(true, cgGrid_mrc.getM_i3lCollisionPoints().size() > 0);
		
		CollisionGrid cgGrid_lec = new CollisionGrid(31, 31, 31, 1);
		Stroma lec = new Stroma(Stroma.TYPE.FDC,loc);
		lec.registerCollisions(cgGrid_lec);
		assertEquals(true, cgGrid_lec.getM_i3lCollisionPoints().size() > 0);
	}

	/**
	 * test that the MRC can secrete chemokine
	 */
	@Test
	public void testFDCCXCL13SECRETING() {

		Double3D loc = new Double3D(0,0,0);
		
		// initialise the system
		Schedule schedule = new Schedule();
		Continuous3D fdcEnvironment = new Continuous3D(
				Settings.FDC.DISCRETISATION, 60, 60, 10);
		new Chemokine(schedule,
				Chemokine.TYPE.CXCL13, 60, 60, 10);
		Settings.FDC.CXCL13_EMITTED = 100;
		Stroma fdc = new Stroma(Stroma.TYPE.FDC,loc);
		fdc.m_drawEnvironment = fdcEnvironment;
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

	}

	/**
	 * test that the BRC can secrete chemokine
	 */
	@Test
	public void testBRCCXCL13SECRETING() {

		Double3D loc = new Double3D(0,0,0);
		
		// initialise the system
		Schedule schedule = new Schedule();
		Continuous3D fdcEnvironment = new Continuous3D(
				Settings.FDC.DISCRETISATION, 60, 60, 10);
		new Chemokine(schedule,
				Chemokine.TYPE.CXCL13, 60, 60, 10);
		Settings.bRC.CXCL13_EMITTED = 100;
		Stroma brc = new Stroma(Stroma.TYPE.bRC,loc);
		brc.m_drawEnvironment = fdcEnvironment;
		brc.setObjectLocation(new Double3D(15, 15, 5));

		// assert that there is currently no chemokine on the grid
		double[][][] chemokinebefore = Chemokine.get(
				Chemokine.TYPE.CXCL13, 15, 15, 5);
		assertThat(chemokinebefore[1][1][1], equalTo(0.0));

		// step the FDC
		brc.step(null);
		brc.step(null);
		brc.step(null);
		brc.step(null);
		brc.step(null);

		// assert that there is now chemokine on the grid.
		double[][][] chemokine = Chemokine.get(Chemokine.TYPE.CXCL13,
				15, 15, 5);
		assertThat(chemokine[1][1][1], greaterThan(0.0));

		Chemokine.reset();

	}
	
	/**
	 * test that the MRC can secrete chemokine
	 */
	@Test
	public void testMRCCXCL13SECRETING() {

		Double3D loc = new Double3D(0,0,0);
		
		// initialise the system
		Schedule schedule = new Schedule();
		Continuous3D fdcEnvironment = new Continuous3D(
				Settings.FDC.DISCRETISATION, 60, 60, 10);
		new Chemokine(schedule,
				Chemokine.TYPE.CXCL13, 60, 60, 10);
		Settings.MRC.CXCL13_EMITTED = 100;
		Stroma mrc = new Stroma(Stroma.TYPE.MRC,loc);
		mrc.m_drawEnvironment = fdcEnvironment;
		mrc.setObjectLocation(new Double3D(15, 15, 5));

		// assert that there is currently no chemokine on the grid
		double[][][] chemokinebefore = Chemokine.get(
				Chemokine.TYPE.CXCL13, 15, 15, 5);
		assertThat(chemokinebefore[1][1][1], equalTo(0.0));

		// step the FDC
		mrc.step(null);
		mrc.step(null);
		mrc.step(null);
		mrc.step(null);
		mrc.step(null);

		// assert that there is now chemokine on the grid.
		double[][][] chemokine = Chemokine.get(Chemokine.TYPE.CXCL13,
				15, 15, 5);
		assertThat(chemokine[1][1][1], greaterThan(0.0));

		Chemokine.reset();

	}
	
	
	/**
	 * Make sure that getCollsionClass returns the correct enum type
	 */
	@Test
	public void testGetCollisionClass() {
		
		Double3D loc = new Double3D(0,0,0);
		
		Stroma fdc = new Stroma(Stroma.TYPE.FDC,loc);
		assertEquals(fdc.getCollisionClass(), CLASS.STROMA);
		
		Stroma frc = new Stroma(Stroma.TYPE.bRC,loc);
		assertEquals(frc.getCollisionClass(), CLASS.STROMA);
		
		Stroma mrc = new Stroma(Stroma.TYPE.MRC,loc);
		assertEquals(mrc.getCollisionClass(), CLASS.STROMA);
		
		Double3D d1 = new Double3D(0,0,0);
		Double3D d2 = new Double3D(1,1,1);
		
		StromaEdge fdcedge = new StromaEdge(d1,d2,StromaEdge.TYPE.FDC_edge);
		StromaEdge frcedge = new StromaEdge(d1,d2,StromaEdge.TYPE.RC_edge);
		StromaEdge fdcbranch = new StromaEdge(d1,d2,StromaEdge.TYPE.FDC_edge);
		
		assertEquals(fdcedge.getCollisionClass(), CLASS.STROMA_EDGE);
		assertEquals(frcedge.getCollisionClass(), CLASS.STROMA_EDGE);
		assertEquals(fdcbranch.getCollisionClass(), CLASS.STROMA_EDGE);
	}

	

}
