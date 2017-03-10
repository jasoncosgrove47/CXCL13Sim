package sim3d;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

import sim.field.continuous.Continuous3D;
import sim.util.Bag;
import sim.util.Double3D;
import sim3d.Settings.FDC;
import sim3d.cell.BC;
import sim3d.stroma.Stroma;
import sim3d.stroma.StromaEdge;
import sim3d.util.IO;

public class FollicleInitialiserTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		String paramFile = "/Users/jc1571/Dropbox/EBI2Sim/Simulation/LymphSimParameters.xml";
		SimulationEnvironment.simulation = new SimulationEnvironment(0,
				IO.openXMLFile(paramFile));
		SimulationEnvironment.simulation.setupSimulationParameters();
		
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testCheckForPointsInTheWay() {
		fail("not yet implemented");
	}
	
	
	
	@Test
	public void testForMultipleGrids() {
		fail("not yet implemented");
	}

	
	@Test
	public void testUpdateNodeConnections() {
		
		SimulationEnvironment.fdcEnvironment = new Continuous3D(Settings.BC.DISCRETISATION, 31, 31,
				31);

	
		Double3D p1 = new Double3D(0,0,0);
		Double3D p2 = new Double3D(1,1,1);
		Double3D p3 = new Double3D(2,2,2);
		
		Stroma n1 = new Stroma(Stroma.TYPE.FDC, p1);
		Stroma n2 = new Stroma(Stroma.TYPE.FDC, p2);
		Stroma n3 = new Stroma(Stroma.TYPE.FDC, p3);
		
		n1.setObjectLocation(p1);
		n2.setObjectLocation(p2);
		n3.setObjectLocation(p3);
		
		StromaEdge se1 = new StromaEdge(p1,p2,StromaEdge.TYPE.FDC_edge);
		StromaEdge se2 = new StromaEdge(p2,p3,StromaEdge.TYPE.FDC_edge);

		se1.setObjectLocation(se1.getPoint1());
		se2.setObjectLocation(se2.getPoint1());
		
		//update the protrusions
		n1.getM_Edges().add(se1);
		n2.getM_Edges().add(se1);
		n2.getM_Edges().add(se2);
		n3.getM_Edges().add(se2);
		
		
		assertFalse(n1.getM_Nodes().contains(n2));
		FollicleInitialiser.updateNodeConnectionForNodeOtherGrids(n1);
		assertTrue(n1.getM_Nodes().contains(n2));
		assertFalse(n1.getM_Nodes().contains(n3));
		
		

		assertFalse(n2.getM_Nodes().contains(n3));
		FollicleInitialiser.updateNodeConnectionForNodeOtherGrids(n2);
		assertTrue(n2.getM_Nodes().contains(n3));
		
	
	}

	
	
	@Test
	public void testUpdateNodeConnections2() {
		
		SimulationEnvironment.fdcEnvironment = new Continuous3D(Settings.BC.DISCRETISATION, 31, 31,
				31);

		Double3D p1 = new Double3D(1,1,1);
		Double3D p2 = new Double3D(2,2,2);
		Double3D p3 = new Double3D(3,3,3);
		
		Stroma n1 = new Stroma(Stroma.TYPE.FDC, p1);
		Stroma n3 = new Stroma(Stroma.TYPE.FDC, p3);
		
		n1.setObjectLocation(p1);
		n3.setObjectLocation(p3);
		
		StromaEdge se1 = new StromaEdge(p1,p2,StromaEdge.TYPE.FDC_edge);
		StromaEdge se2 = new StromaEdge(p2,p3,StromaEdge.TYPE.FDC_edge);

		se1.setObjectLocation(se1.getPoint1());
		se2.setObjectLocation(se2.getPoint1());
		
		//update the protrusions
		n1.getM_Edges().add(se1);
		n3.getM_Edges().add(se2);
		
		assertFalse(n1.getM_Nodes().contains(n3));
		FollicleInitialiser.updateNodeConnectionForNodeOtherGrids(n1);
		assertTrue(n1.getM_Nodes().contains(n3));
		
	}
	
	@Test
	public void testUpdateNodeConnections3() {
		
		SimulationEnvironment.fdcEnvironment = new Continuous3D(Settings.BC.DISCRETISATION, 31, 31,
				31);

		Double3D p1 = new Double3D(1,1,1);
		Double3D p2 = new Double3D(2,2,2);
		Double3D p3 = new Double3D(3,3,3);
		Double3D p4 = new Double3D(4,4,4);
		
		Stroma n1 = new Stroma(Stroma.TYPE.FDC, p1);
		Stroma n3 = new Stroma(Stroma.TYPE.FDC, p4);
		
		n1.setObjectLocation(p1);
		n3.setObjectLocation(p4);
		
		StromaEdge se1 = new StromaEdge(p1,p2,StromaEdge.TYPE.FDC_edge);
		StromaEdge se2 = new StromaEdge(p2,p3,StromaEdge.TYPE.FDC_edge);
		StromaEdge se3 = new StromaEdge(p3,p4,StromaEdge.TYPE.FDC_edge);
		
		se1.setObjectLocation(se1.getPoint1());
		se2.setObjectLocation(se2.getPoint1());
		se3.setObjectLocation(se3.getPoint1());
		
		//update the protrusions
		n1.getM_Edges().add(se1);
		n3.getM_Edges().add(se3);
		
		assertFalse(n1.getM_Nodes().contains(n3));
		FollicleInitialiser.updateNodeConnectionForNodeOtherGrids(n1);
		assertTrue(n1.getM_Nodes().contains(n3));
		
	}
	
	@Test
	public void testUpdateNodeConnections4() {
		
		SimulationEnvironment.fdcEnvironment = new Continuous3D(Settings.BC.DISCRETISATION, 31, 31,
				31);

		Double3D p1 = new Double3D(1,1,1);
		Double3D p2 = new Double3D(2,2,2);
		Double3D p3 = new Double3D(3,3,3);
		Double3D p4 = new Double3D(4,4,4);
		Double3D p5 = new Double3D(2,4,3);
		
		Stroma n1 = new Stroma(Stroma.TYPE.FDC, p1);
		Stroma n3 = new Stroma(Stroma.TYPE.FDC, p4);
		Stroma n4 = new Stroma(Stroma.TYPE.FDC, p5);
		
		n1.setObjectLocation(p1);
		n3.setObjectLocation(p4);
		n4.setObjectLocation(p5);
		
		StromaEdge se1 = new StromaEdge(p1,p2,StromaEdge.TYPE.FDC_edge);
		StromaEdge se2 = new StromaEdge(p2,p3,StromaEdge.TYPE.FDC_edge);
		
		//these three both orginate from p3 :D
		StromaEdge se3 = new StromaEdge(p3,p4,StromaEdge.TYPE.FDC_edge);
		StromaEdge se4 = new StromaEdge(p3,p5,StromaEdge.TYPE.FDC_edge);
		
		se1.setObjectLocation(se1.getPoint1());
		se2.setObjectLocation(se2.getPoint1());
		se3.setObjectLocation(se3.getPoint1());
		se4.setObjectLocation(se4.getPoint1());
		
		//update the protrusions
		n1.getM_Edges().add(se1);
		n3.getM_Edges().add(se3);
		n4.getM_Edges().add(se4);
		
		assertFalse(n1.getM_Nodes().contains(n3));
		assertFalse(n1.getM_Nodes().contains(n4));
		FollicleInitialiser.updateNodeConnectionForNodeOtherGrids(n1);
		assertTrue(n1.getM_Nodes().contains(n3));
		assertTrue(n1.getM_Nodes().contains(n4));
		
	}
	

	@Test
	public void checkForZeroNodeConnections(){
		
		SimulationEnvironment.simulation.start();

		boolean anyNodesHaveZeroNodeConnections = false;
		
		int counter = 0;
		
		
		Bag stroma = SimulationEnvironment.getAllStroma();
		for (int i = 0; i < stroma.size(); i++) {
			if (stroma.get(i) instanceof Stroma) {
				
	
				Stroma sc = (Stroma) stroma.get(i);
				//on occastions the MRCs dont connect to anything so omit them from this
				// test case
				if(sc.getStromatype() != Stroma.TYPE.MRC && sc.getStromatype() != Stroma.TYPE.LEC ){
				
					if(sc.getM_Nodes().size() == 0){
						anyNodesHaveZeroNodeConnections = true;
						counter += 1;
					}
				}
			}	
		}

		System.out.println("counter equals: " + counter);
		assertFalse(anyNodesHaveZeroNodeConnections);
	}

		//cells should not be connected to LECs
	
	@Test
	public void checkForLECConnections(){
		
		SimulationEnvironment.simulation.start();

		boolean lecConnections = false;
		
		Bag stroma = SimulationEnvironment.getAllStroma();
		for (int i = 0; i < stroma.size(); i++) {
			if (stroma.get(i) instanceof Stroma) {
				
	
				Stroma sc = (Stroma) stroma.get(i);
				//on occastions the MRCs dont connect to anything so omit them from this
				// test case
				if(sc.getStromatype() != Stroma.TYPE.LEC ){
					
					for(Stroma sc2 : sc.getM_Nodes()){
						if(sc2.getStromatype() == Stroma.TYPE.LEC){
							lecConnections = true;
							break;
						}	
					}	
				}	
			}	
		}
		assertFalse(lecConnections);
	}
	

	
	
	@Test
	public void checkForNodeConnectionsWithBranches(){

		SimulationEnvironment.fdcEnvironment = new Continuous3D(Settings.BC.DISCRETISATION, 31, 31,
				31);

		Double3D p1 = new Double3D(1,1,1);
		Double3D p2 = new Double3D(2,2,2);
		Double3D p3 = new Double3D(3,3,3);
		Double3D p4 = new Double3D(4,4,4);
		Double3D p5 = new Double3D(2,4,3);
		
		Stroma n1 = new Stroma(Stroma.TYPE.FDC, p1);
		Stroma n3 = new Stroma(Stroma.TYPE.FDC, p4);
		Stroma n4 = new Stroma(Stroma.TYPE.FDC, p5);
		
		n1.setObjectLocation(p1);
		n3.setObjectLocation(p4);
		n4.setObjectLocation(p5);
		
		StromaEdge se1 = new StromaEdge(p1,p2,StromaEdge.TYPE.FDC_edge);
		StromaEdge se2 = new StromaEdge(p2,p3,StromaEdge.TYPE.FDC_edge);
		
		//these three both orginate from p3 :D
		StromaEdge se3 = new StromaEdge(p3,p4,StromaEdge.TYPE.FDC_edge);
		StromaEdge se4 = new StromaEdge(p3,p5,StromaEdge.TYPE.FDC_edge);
		
		se1.setObjectLocation(se1.getPoint1());
		se2.setObjectLocation(se2.getPoint1());
		se3.setObjectLocation(se3.getPoint1());
		se4.setObjectLocation(se4.getPoint1());
		
		//update the protrusions
		n1.getM_Edges().add(se1);
		n3.getM_Edges().add(se3);
		n4.getM_Edges().add(se4);
		
		assertFalse(n1.getM_Nodes().contains(n3));
		assertFalse(n1.getM_Nodes().contains(n4));
		FollicleInitialiser.updateNodeConnectionForNodeOtherGrids(n1);
		assertTrue(n1.getM_Nodes().contains(n3));
		assertTrue(n1.getM_Nodes().contains(n4));
	}
	
	
	@Test
	public void checkForNodeConnectionsWithBranches2(){

		SimulationEnvironment.fdcEnvironment = new Continuous3D(Settings.BC.DISCRETISATION, 31, 31,
				31);

		
		//lets create two parallel lines with 2 points each
		Double3D p1 = new Double3D(1,1,1);
		Double3D p2 = new Double3D(2,2,2);
		Double3D p3 = new Double3D(2,1,1);
		Double3D p4 = new Double3D(3,2,2);
		
		Stroma n1 = new Stroma(Stroma.TYPE.FDC, p1);
		Stroma n2 = new Stroma(Stroma.TYPE.FDC, p2);
		Stroma n3 = new Stroma(Stroma.TYPE.FDC, p3);
		Stroma n4 = new Stroma(Stroma.TYPE.FDC, p4);
		
		n1.setObjectLocation(p1);
		n2.setObjectLocation(p2);
		n3.setObjectLocation(p3);
		n4.setObjectLocation(p4);
		
		
		
		//connect each parallel line with an edge
		StromaEdge se1 = new StromaEdge(p1,p2,StromaEdge.TYPE.FDC_edge);
		StromaEdge se2 = new StromaEdge(p3,p4,StromaEdge.TYPE.FDC_edge);
		//now make a branch between them
		StromaEdge se3 = new StromaEdge(se1.getMidpoint(),se2.getMidpoint(),StromaEdge.TYPE.FDC_branch);
		
		
		//set the edge locations
		se1.setObjectLocation(se1.getPoint1());
		se2.setObjectLocation(se2.getPoint1());
		se3.setObjectLocation(se1.getMidpoint());
		
		//update the protrusions
		n1.getM_Edges().add(se1);
		n2.getM_Edges().add(se1);
		n3.getM_Edges().add(se2);
		n4.getM_Edges().add(se2);
		
		assertFalse(n1.getM_Nodes().contains(n3));
		assertFalse(n1.getM_Nodes().contains(n4));
		assertFalse(n2.getM_Nodes().contains(n3));
		assertFalse(n2.getM_Nodes().contains(n4));
		FollicleInitialiser.updateNodeConnectionForNodeOtherGrids(n1);
		FollicleInitialiser.updateNodeConnectionForNodeOtherGrids(n2);
		assertTrue(n1.getM_Nodes().contains(n3));
		assertTrue(n1.getM_Nodes().contains(n4));
		assertTrue(n2.getM_Nodes().contains(n3));
		assertTrue(n2.getM_Nodes().contains(n4));
		assertTrue(n4.getM_Nodes().contains(n1));
		assertTrue(n4.getM_Nodes().contains(n2));

	}
	
	
	
}