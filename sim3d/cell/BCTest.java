/**
 * 
 */
package sim3d.cell;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ec.util.MersenneTwisterFast;
import sim.engine.Schedule;
import sim.field.continuous.Continuous3D;
import sim.util.Double3D;
import sim.util.MutableDouble3D;
import sim3d.Options;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.diffusion.Particle;
import sim3d.util.Vector3DHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
/**
 * @author sjj509
 * 		
 */
public class BCTest
{
	private Schedule schedule = new Schedule();
	private Particle m_pParticle;
	
	@BeforeClass
    public static void oneTimeSetUp()
	{
		Options.RNG = new MersenneTwisterFast();
		Options.WIDTH = 31;
		Options.HEIGHT = 31;
		Options.DEPTH = 31;
    }


	@Before
	public void setUp() throws Exception
	{
		m_pParticle = new Particle(schedule, Particle.TYPE.CXCL13, 31, 31, 31);
		
		BC.drawEnvironment = new Continuous3D( Options.BC.DISCRETISATION, 31, 31, 31 );
	}

    @After
    public void tearDown() {
    	m_pParticle.field = null;
    	m_pParticle = null;
    	Particle.reset();
    	BC.drawEnvironment = null;
    }
	
    
    /*
     * Where are the comments for this
     */
	@Test
	public void testShouldMigrateTowardsChemokine()
	{
		m_pParticle.field[15][15][15] = 10000;
		
		m_pParticle.m_dDecayRateInv = 1;
		
		Options.BC.MIN_RECEPTORS = 0;
		
		// Let's diffuse a little
		Options.DIFFUSION_STEPS = 2;
		m_pParticle.step( null );
		m_pParticle.step( null );
		m_pParticle.step( null );
		m_pParticle.step( null );
		m_pParticle.step( null );
		m_pParticle.step( null );
		m_pParticle.step( null );
		m_pParticle.step( null );
		m_pParticle.step( null );
		m_pParticle.step( null );
		m_pParticle.step( null );
		m_pParticle.step( null );
		
		// Randomly place 100 BCs
		BC[] bcCells = new BC[100];
		for (int i = 0; i < 100; i++)
		{
			bcCells[i] = new BC();
			
			bcCells[i].setObjectLocation( new Double3D(Options.RNG.nextInt(14)+8,Options.RNG.nextInt(14)+8,Options.RNG.nextInt(14)+8) );
		}
		// Let them move a bit
		for ( int i = 0; i < 400; i++ )
		{
			for (int j = 0; j < 100; j++)
			{
				bcCells[j].step( null );//why are you passing in null
			}
			m_pParticle.field[15][15][15] = 10000;
			m_pParticle.step( null );
		}
		
		double avDistance = 0;
		double maxDist = 0;
		
		//not quite sure what this bit is doing
		for (int i = 0; i < 100; i++)
		{
			Double3D bcLoc = new Double3D(bcCells[i].x-15, bcCells[i].y-15, bcCells[i].z-15);//why take 15 away
			
			avDistance += bcLoc.length();//add this vector? see how far they are from origin?
			
			
			//why do we need maxDist, doesn't seem to be doing anything
			// do we need a maxDist criteria?
			if ( maxDist < bcLoc.length() )
			{
				maxDist = bcLoc.length();
			}
		}

		assertThat(avDistance/100, lessThan(4.0));//why is this condition here?
	}
	
	// TODO This doesn't pass given enough time...
	@Test
	public void testShouldCollideWithStroma()
	{
		CollisionGrid cgGrid = new CollisionGrid(31, 31, 31, 1);
		BC.m_cgGrid = cgGrid;
		
		int iEdges = 1000;
		
		Double3D[] points = Vector3DHelper.getEqDistPointsOnSphere( iEdges );
		
		Double3D d3Centre = new Double3D(15,15,15);
		
		points[0] = points[0].multiply( 3 ).add( d3Centre ); //what is this line doing
		
		iEdges--;  // what is this line doing
		for ( int i = 0; i < iEdges; i++ )
		{
			points[i+1] = points[i+1].multiply( 3 ).add( d3Centre );
			StromaEdge seEdge = new StromaEdge(points[i], points[i+1]);
			seEdge.registerCollisions( cgGrid );
		}
		
		// place 100 BCs in centre
		BC[] bcCells = new BC[100];
		for (int i = 0; i < 100; i++)
		{
			bcCells[i] = new BC();
			
			bcCells[i].setObjectLocation( d3Centre );
		}
		
		// Let them move a bit
		for ( int i = 0; i < 100; i++ )
		{
			for (int j = 0; j < 100; j++)
			{
				bcCells[j].step( null );
			}
			cgGrid.step( null );
		}
		
		
		//again not fully sure what this bit is doing
		double avDistance = 0;
		double maxDist = 0;
		for (int i = 0; i < 100; i++)
		{
			Double3D bcLoc = new Double3D(bcCells[i].x-15, bcCells[i].y-15, bcCells[i].z-15);
			
			avDistance += bcLoc.length();
			
			if ( maxDist < bcLoc.length() )
			{
				maxDist = bcLoc.length();
			}
		}
		
		
		//again not sure what these are doing
		assertThat(avDistance/100, lessThan(3.0));
		assertThat(maxDist, lessThan(3.1));
		
		// so we don't break other tests!
		BC.m_cgGrid = null;
	}
	
	/**
	 * We want to test that the cell doesn't perfect go towards the chemokine
	 * gradient, but, for example, moves freely in a large area of medium-high
	 * concentration of chemokine, i.e. the stromal network
	 */
	@Test
	public void testShouldMoveRandomly()
	{
		for ( int i = 0; i < 31; i++ )
		{
			m_pParticle.field[15][15][i] = 4000;
		}
	
		m_pParticle.m_dDecayRateInv = 1;
		
		// Let's diffuse a little
		Options.DIFFUSION_STEPS = 2;
		m_pParticle.step( null );
		m_pParticle.step( null );
		m_pParticle.step( null );
		m_pParticle.step( null );
		m_pParticle.step( null );
		m_pParticle.step( null );
		m_pParticle.step( null );
		m_pParticle.step( null );
		m_pParticle.step( null );
		m_pParticle.step( null );
		m_pParticle.step( null );
		m_pParticle.step( null );
		
		// Randomly place 100 BCs
		BC[] bcCells = new BC[250];
		for (int i = 0; i < 250; i++)
		{
			bcCells[i] = new BC();
			
			bcCells[i].setObjectLocation( new Double3D(15, 15, 15) );
		}
		
		// Let them move a bit
		for ( int i = 0; i < 400; i++ )
		{
			for (int j = 0; j < 250; j++)
			{
				bcCells[j].step( null );
			}
			
			for ( int k = 0; k < 31; k++ )
			{
				m_pParticle.field[15][15][k] = 4000;
			}
			m_pParticle.step( null );
		}
		
		
		//not fully sure what this bit down does....
		int[] iaResults = new int[5];

		for (int i = 0; i < 250; i++)
		{
			iaResults[(int)(5*(bcCells[i].z-1)/29.0)]++;
		}

		assertEquals("0-6", 50, iaResults[0], 15.0);
		assertEquals("6-12", 50, iaResults[1], 15.0);
		assertEquals("12-18", 50, iaResults[2], 15.0);
		assertEquals("18-24", 50, iaResults[3], 15.0);
		assertEquals("24-30", 50, iaResults[4], 15.0);
	}
}
