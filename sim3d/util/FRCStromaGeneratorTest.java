/**
 * 
 */
package sim3d.util;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import ec.util.MersenneTwisterFast;
import sim.util.Double3D;
import sim.util.Int3D;
import sim3d.Options;
import sim3d.cell.StromaEdge;

/**
 * @author sjj509
 *
 */
public class FRCStromaGeneratorTest
{

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception
	{
		Options.RNG = new MersenneTwisterFast();
	}
	
	/**
	 * Test method for {@link sim3d.util.FRCStromaGenerator#generateStroma3D(int, int, int, int, java.util.List)}.
	 */
	@Test
	public void testEdgeLength()
	{
        ArrayList<Double3D> d3lCellLocations = new ArrayList<Double3D>();
		ArrayList<StromaEdge> selEdges = new ArrayList<StromaEdge>();
		FRCStromaGenerator.generateStroma3D(100, 100, 100, 10000, d3lCellLocations, selEdges);
		
		double dTotal = 0;
		for ( StromaEdge seEdge : selEdges )
		{
			dTotal += seEdge.getPoint2().subtract( seEdge.getPoint1() ).length();
		}
		dTotal /= selEdges.size();
		
		// Value taken from literature + radius of cell
		assertEquals(2.163, dTotal, 0.1);
	}
	
	/**
	 * Test method for {@link sim3d.util.FRCStromaGenerator#generateStroma3D(int, int, int, int, java.util.List)}.
	 */
	@Test
	public void testEdgeCount()
	{
        ArrayList<Double3D> d3lCellLocations = new ArrayList<Double3D>();
		ArrayList<StromaEdge> selEdges = new ArrayList<StromaEdge>();
		FRCStromaGenerator.generateStroma3D(100, 100, 100, 10000, d3lCellLocations, selEdges);
		
		int iCellCount = d3lCellLocations.size();
		
		// Each edge is connected to 2 cells! *not exactly true, but close enough
		double dEdgesPerFDC = selEdges.size()*2.0 / iCellCount;
		
		// Value taken from literature
		assertEquals(4, dEdgesPerFDC, 0.01);
	}
	
	/**
	 * Test method for {@link sim3d.util.FRCStromaGenerator#generateStroma3D(int, int, int, int, java.util.List)}.
	 * /
	@Test
	public void testGenerateStroma3D()
	{
		fail( "Not yet implemented" );
	}
	
	/**
	 * Test method for {@link sim3d.util.FRCStromaGenerator#createNewCells(int, int, int, java.util.ArrayList, boolean[][][], sim.util.Int3D, sim.util.Double3D[])}.
	 * /
	@Test
	public void testCreateNewCells()
	{
		fail( "Not yet implemented" );
	}
	
	/**
	 * Test method for {@link sim3d.util.FRCStromaGenerator#generateDirections(int, int, int, boolean[][][], sim.util.Int3D, int)}.
	 * /
	@Test
	public void testGenerateDirections()
	{
		fail( "Not yet implemented" );
	}
	
	/**
	 * Test method for {@link sim3d.util.FRCStromaGenerator#pickNextCell(int, int, int, java.util.ArrayList, boolean[][][])}.
	 * /
	@Test
	public void testPickNextCell()
	{
		fail( "Not yet implemented" );
	}
	
	/**
	 * Test method for {@link sim3d.util.FRCStromaGenerator#getAdjacentCells(int, int, int, boolean[][][], sim.util.Int3D, int)}.
	 * /
	@Test
	public void testGetAdjacentCells()
	{
		fail( "Not yet implemented" );
	}
	
	/**
	 * Test method for {@link sim3d.util.FRCStromaGenerator#calcDistance(sim.util.Int3D, sim.util.Int3D)}.
	 */
	@Test
	public void testCalcDistance()
	{
		Double3D d3Point1 = new Double3D(1,0,1);
		Double3D d3Point2 = new Double3D(0,0,0);
		assertEquals(Math.sqrt( 2 ), FRCStromaGenerator.calcDistance( d3Point1, d3Point2 ), 0.001);
	}
}
