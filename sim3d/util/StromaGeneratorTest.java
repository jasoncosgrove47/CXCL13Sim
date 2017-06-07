/**
 * 
 */
package sim3d.util;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ec.util.MersenneTwisterFast;
import sim.util.Double3D;
import sim3d.Settings;
import sim3d.SimulationEnvironment;
import sim3d.stroma.StromaEdge;
import sim3d.util.StromaGenerator.StromalCelltemp;

/**
 * @author sjj509
 * 
 */
public class StromaGeneratorTest {

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		SimulationEnvironment.simulation = null;
		Settings.RNG = new MersenneTwisterFast();
	}

	@After
	public void tearDown() throws Exception {
		if(SimulationEnvironment.simulation != null){
			SimulationEnvironment.simulation.finish();
		}
	}
	
	/**
	 * Test method for
	 * {@link sim3d.util.StromaGenerator#generateStroma3D(int, int, int, int, java.util.List)}
	 * 
	 * what specifically is this testing - testing to make sure that the edge
	 * length is as we expect
	 */
	@Test
	public void testEdgeLength() {
		ArrayList<StromalCelltemp> d3lCellLocations = new ArrayList<StromalCelltemp>();
		ArrayList<StromaEdge> selEdges = new ArrayList<StromaEdge>();
		
		StromaGenerator.generateStroma3D_Updated(30, 30, 30, 2000,
				d3lCellLocations, selEdges);

		double dTotal = 0;
		for (StromaEdge seEdge : selEdges) {
			Double3D d3Point = seEdge.getPoint2();// get the second point of the
													// edge

			// if the point is within the boundaries
			if (d3Point.x <= 0 || d3Point.x >= 100 || d3Point.y <= 0
					|| d3Point.y >= 100 || d3Point.z <= 0 || d3Point.z >= 100) {
				continue;
			}

			dTotal += seEdge.getPoint2().subtract(seEdge.getPoint1()).length();
		}
		dTotal /= selEdges.size();

		// Value taken from literature + radius of cell*2
		assertEquals(3.0, dTotal, 1.5); // assert that dTotal must equal 2.163
											// with 0.2 error for margin
	}

	
	/**
	 * Test method for
	 * {@link sim3d.util.StromaGenerator#generateStroma3D(int, int, int, int, java.util.List)}
	 * 
	 * Test that the number of edges is as we expect
	 */
	@Test
	public void testEdgeCount() {
		ArrayList<StromalCelltemp> d3lCellLocations = new ArrayList<StromalCelltemp>();
		ArrayList<StromaEdge> selEdges = new ArrayList<StromaEdge>();
		StromaGenerator.generateStroma3D_Updated(20, 20,20, 700,
				d3lCellLocations, selEdges);

		
		int iCellCount = 0;
		int iEdgeCount = 0;

		for (StromalCelltemp frcCell : d3lCellLocations) {
			iCellCount++;
			iEdgeCount += frcCell.m_iEdges;
			
		}

		double dEdgesPerFDC = (double) iEdgeCount / (double) iCellCount;

		// Value taken from literature
		assertEquals(5, dEdgesPerFDC, 2); // 4 edges per FDC is determined
											// from exptl data
	}

	/**
	 * Test method for
	 * {@link sim3d.util.StromaGenerator#generateStroma3D(int, int, int, int, java.util.List)}
	 * .
	 * 
	 * Test that the cell count is as we expect
	 */
	@Test
	public void testCellCount() {
		
		ArrayList<StromalCelltemp> d3lCellLocations = new ArrayList<StromalCelltemp>();
		ArrayList<StromaEdge> selEdges = new ArrayList<StromaEdge>();
		StromaGenerator.generateStroma3D_Updated(50, 50, 5, 350, d3lCellLocations,
				selEdges);

		int iCellCount = d3lCellLocations.size();

		// Value taken from literature
		assertEquals(350, iCellCount, 100);
	}

	/**
	 * Tests that the stromal network is generated even in 2D
	 */
	@Test
	public void testStroma2D() {

		ArrayList<StromalCelltemp> d3lCellLocations = new ArrayList<StromalCelltemp>();
		ArrayList<StromaEdge> selEdges = new ArrayList<StromaEdge>();
		StromaGenerator.generateStroma3D_Updated(50, 50, 1, 10, d3lCellLocations,
				selEdges);

		int iCellCount = d3lCellLocations.size();

		// Value taken from literature
		assertEquals(10, iCellCount, 10);

	}

}
