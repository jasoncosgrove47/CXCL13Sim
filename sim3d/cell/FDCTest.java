/**
 * 
 */
package sim3d.cell;


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
import sim3d.diffusion.ParticleMoles;
import sim3d.util.FRCStromaGenerator;
import sim3d.util.IO;
import sim3d.util.FRCStromaGenerator.FRCCell;


public class FDCTest {
	
	@Before
	public void setUp() throws Exception {
		Settings.RNG = new MersenneTwisterFast();
		// Initialise the stromal grid
		Continuous3D fdcEnvironment = new Continuous3D(Settings.FDC.DISCRETISATION,
				60, 60, 10);
		
	}

	/*
	 * Make sure that the FDC can display antigen
	 */
	@Test
	public void canDisplayAntigen() {
		
		ArrayList<FRCCell> d3lCellLocations = new ArrayList<FRCCell>();
		ArrayList<StromaEdge> selEdges = new ArrayList<StromaEdge>();
		FRCStromaGenerator.generateStroma3D(50, 50, 5, 350, d3lCellLocations,
				selEdges);
		
		Settings.FDC.STARTINGANTIGENLEVEL = 100;
		for (StromaEdge seEdge : selEdges) {
			seEdge.getAntigen();
			assertThat(seEdge.getAntigen(), greaterThan(0));
		}
	}

	/**
	 * Make sure that the FDC can lose antigen
	 */
	@Test
	public void canLoseAntigen(){
		
		
		ArrayList<FRCCell> d3lCellLocations2 = new ArrayList<FRCCell>();
		ArrayList<StromaEdge> selEdges2 = new ArrayList<StromaEdge>();
		
		Settings.FDC.STARTINGANTIGENLEVEL = 100;
		
		FRCStromaGenerator.generateStroma3D(5, 5, 5, 5, d3lCellLocations2,
				selEdges2);
		
		for (StromaEdge seEdge : selEdges2) {
			
			//int antigenLevel = seEdge.getAntigen();
			seEdge.setAntigenLevelLowerHalf(seEdge.getAntigenLevelLowerEdge() - 1);
			assertThat(seEdge.getAntigen(), lessThan(100));
		}
		
		
	}

	/**
	 * Test that getModel returns an object of type 
	 * TransformGroup
	 */
	@Test
	public void testGetModel() {
		FDC c = new FDC();
		TransformGroup localTG = c.getModel(c,null);
		assertTrue(localTG instanceof TransformGroup);
	}
	
	/**
	 * Test that registerCollisions updates
	 * m_i3lCollisionPoints
	 */
	@Test
	public void testRegisterCollisions() {
		CollisionGrid cgGrid = new CollisionGrid(31, 31, 31, 1);
		Settings.FDC.STROMA_NODE_RADIUS = 1;
	
		FDC fdc = new FDC();
		fdc.registerCollisions(cgGrid);
		assertEquals(true, cgGrid.getM_i3lCollisionPoints().size() > 0);
	}
	
	/**
	 * test that the FDC can secrete Antigen
	 */
	@Test
	public void testCXCL13SECRETING() {
		
		//initialise the system
		Schedule schedule = new Schedule();
		Continuous3D fdcEnvironment = new Continuous3D(Settings.FDC.DISCRETISATION,
				60, 60, 10);
		FDC.drawEnvironment = fdcEnvironment;
		ParticleMoles m_pParticle = new ParticleMoles(schedule, ParticleMoles.TYPE.CXCL13,
				60, 60, 10);
		Settings.FDC.CXCL13_EMITTED = 100;
		FDC fdc = new FDC();
		fdc.setObjectLocation( new Double3D(15,15, 5 ));

		
		//assert that there is currently no chemokine on the grid
		double[][][] chemokinebefore = ParticleMoles.get(ParticleMoles.TYPE.CXCL13,
				15, 15, 5);
		assertThat(chemokinebefore[1][1][1], equalTo(0.0));
		
		//step the FDC
		fdc.step(null);
		fdc.step(null);
		fdc.step(null);
		fdc.step(null);
		fdc.step(null);
	
		//assert that there is now chemokine on the grid. 
		double[][][] chemokine = ParticleMoles.get(ParticleMoles.TYPE.CXCL13,
				15, 15, 5);
		assertThat(chemokine[1][1][1], greaterThan(0.0));
		
		ParticleMoles.reset();
		FDC.drawEnvironment = null;

	}

	/**
	 * Make sure that getCollsionClass
	 * returns the correct enum type
	 */
	@Test
	public void testGetCollisionClass(){	
		FDC fdc = new FDC();
		assertEquals(fdc.getCollisionClass() ,CLASS.STROMA); 
	}
	
	

}
