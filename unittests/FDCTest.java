/**
 * 
 */
package unittests;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.*;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import ec.util.MersenneTwisterFast;
import sim3d.Settings;
import sim3d.cell.StromaEdge;
import sim3d.util.FRCStromaGenerator;
import sim3d.util.FRCStromaGenerator.FRCCell;


/**
 * Test some basic functionality of the FDC network
 */
public class FDCTest {
	


	@Before
	public void setUp() throws Exception {
		Settings.RNG = new MersenneTwisterFast();
		
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
		
		ArrayList<FRCCell> d3lCellLocations = new ArrayList<FRCCell>();
		ArrayList<StromaEdge> selEdges = new ArrayList<StromaEdge>();
		FRCStromaGenerator.generateStroma3D(50, 50, 5, 350, d3lCellLocations,
				selEdges);
		
		Settings.FDC.STARTINGANTIGENLEVEL = 100;
		for (StromaEdge seEdge : selEdges) {
			seEdge.setAntigenLevelLowerHalf(seEdge.getAntigen());
			assertThat(seEdge.getAntigen(), lessThan(100));
		}
		
		
	}


	// TODO Integration test check that the stroma can secrete chemokine and that it secretes
	// before it diffuses


}
