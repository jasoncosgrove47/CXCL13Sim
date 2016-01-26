package sim3d.cell;

import sim.util.*;
import sim.engine.*;
import sim.field.continuous.Continuous2D;
import sim.field.continuous.Continuous3D;

import java.awt.Color;
//import java.awt.*;
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

import dataLogger.Grapher;
import sim.portrayal3d.simple.Shape3DPortrayal3D;
import sim.portrayal3d.simple.SpherePortrayal3D;
import sim3d.Settings;
import sim3d.SimulationEnvironment;
import sim3d.collisiondetection.Collidable;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.diffusion.ParticleMoles;
import sim3d.util.Vector3DHelper;

/**
 * A B-cell agent. Performs chemotaxis/random movement based on the presence of
 * surrounding chemokine and the amount of receptors the cell is expressing. The
 * receptors are controlled by an ODE. The calculated movement is checked to see
 * whether it collides with the edges or other elements before being realised.
 * 
 * @author Jason Cosgrove  - {@link jc1571@york.ac.uk}
 * @author Simon Jarrett - {@link simonjjarrett@gmail.com}
 */
public class BC extends DrawableCell3D implements Steppable, Collidable
{
	/**
	 * The drawing environment that houses this cell; used by
	 * DrawableCell3D.setObjectLocation
	 */
	public static Continuous3D	drawEnvironment;
								
	/**
	 * The collision grid that contains this element; used to register
	 * collisions
	 */
	public static CollisionGrid	m_cgGrid;
								
	/**
	 * The squared distance between a BC and a stroma edge at the point of
	 * collision; precomputed to speed up calculations
	 */
	private static final double	BC_SE_COLLIDE_DIST_SQ	= (Settings.BC.COLLISION_RADIUS + Settings.FDC.STROMA_EDGE_RADIUS)
			* (Settings.BC.COLLISION_RADIUS + Settings.FDC.STROMA_EDGE_RADIUS);
			
	/**
	 * Required to prevent a warning
	 */
	private static final long	serialVersionUID		= 1;
														
	/**
	 * Boolean to tell the cell whether or not it should be sending graph data;
	 * used to make sure we only have one reporting!
	 */
	public boolean				displayODEGraph			= false;

					
	/**
	 * (ODE) Ligand-Receptor Complexes
	 * m_ signifies it's a member variable
	 */
	public int					m_iL_r					= Settings.BC.ODE.LR();

	/**
	 * (ODE) Free Receptors on cell surface
	 */
	public int					m_iR_free				= Settings.BC.ODE.Rf();
														
	/**
	 * (ODE) Internalised Receptor
	 */
	public int					m_iR_i					= Settings.BC.ODE.Ri();
														
	/**
	 * The direction the cell is facing; used for movement
	 */
	private Double3D			m_d3Face				= Vector3DHelper.getRandomDirection();
														
	/**
	 * DEBUG used to display collision points
	 */
	ArrayList<Double3D>			m_d3aCollisions			= new ArrayList<Double3D>();
														
	/**
	 * The movements of the cell; each collision or bounce means a new movement
	 */
	List<Double3D>				m_d3aMovements			= new ArrayList<Double3D>();
														
	/**
	 * Points on the collision grid we're intersecting with something
	 */
	HashSet <Int3D>					m_i3lCollisionPoints	= new HashSet<Int3D>();

	/*
	 * Determines the position of a BC on a stromal edge
	 */
	private double positionAlongStroma = 0;
	
	
	/*
	 * Determines how many collisions a BC has had this timestep
	 * necessary to prevent infinite collisions
	 */
	int collisionCounter = 0;

	
	@Override
	public boolean isStatic(){ return false; }
	
	/**
	 * Adds a collision point to the list
	 * @param i3Point Coordinates for the collision
	 */
	@Override
	public void addCollisionPoint( Int3D i3Point ){ m_i3lCollisionPoints.add( i3Point ); }
	
	/**
	 * @return the ENUM representing the type of Collidable this cell is (BC)
	 */
	@Override
	public CLASS getCollisionClass(){ return CLASS.BC; }
	
	@Override
	public Continuous3D getDrawEnvironment(){ return drawEnvironment; }
	
	/**
	 * Controls what a B cell agent does for each time step
	 * Each Bcell registers its intended path on the collision grid, once all B cells register 
	 * the collision grid handles the movement at the next iteration the B cells are moved. 
	 * B cells only collide with stroma
	 */
	@Override
	public void step( final SimState state )//why is this final here
	{
		
	
		collisionCounter = 0; 	//reset the collision counter for this timestep
		m_i3lCollisionPoints.clear();
		
		if ( m_d3aMovements != null && m_d3aMovements.size() > 0 ) // If we have a stored movement, execute it
		{
			for ( Double3D d3Movement : m_d3aMovements )
			{
				
				//TODO this should be in an ifSpaceAvailable where we determine
				// if there are cells near where we want to go max = 2
				// if this is the case then don't move or maybe have a look at close grids?
				x += d3Movement.x;
				y += d3Movement.y;
				z += d3Movement.z;
			}
			
			// Remember which way we're now facing
			m_d3Face = m_d3aMovements.get( m_d3aMovements.size() - 1 ).normalize();
			setObjectLocation( new Double3D( x, y, z ) );
		}
		

		
		//TODO migration needs to be encapsulated as it's own method
		
		//TODO test case to make sure that random walk leads to a low displacement
		
		Double3D vMovement;
		vMovement = getMoveDirection();		
		double vectorMagnitude = Math.sqrt(Math.pow(vMovement.x, 2) + Math.pow(vMovement.y, 2) + Math.pow(vMovement.z, 2));

		//vectormagnitude makes things go loco but no idea why....
		if ( vMovement.lengthSq() > 0)
		{
			if(vectorMagnitude > Settings.BC.SIGNAL_THRESHOLD)
			{
					// Add some noise to the direction and take the average of our
					// current direction and the new direction
				
				// the multiply is to scale the new vector, when we multiply by 2 we are favouring the new signal more than the old
					vMovement = m_d3Face.add( Vector3DHelper.getBiasedRandomDirectionInCone( vMovement.normalize(),Settings.BC.DIRECTION_ERROR() ).multiply(0.33) );
					//vMovement = m_d3Face.add( Vector3DHelper.getRandomDirectionInCone( vMovement.normalize(),Settings.BC.DIRECTION_ERROR() ) );
					if ( vMovement.lengthSq() > 0 )
					{
						vMovement = vMovement.normalize();//TODO what is this section of code doing
					}
			}
			
			else{ vMovement = null; }
		}
		else{ vMovement = null; }
		
		if ( vMovement == null || vMovement.lengthSq() == 0 )//TODO: 0don't understand this line, need to sort it out
		{
			// no data! so do a random turn
			vMovement = Vector3DHelper.getRandomDirectionInCone( m_d3Face, Settings.BC.RANDOM_TURN_ANGLE() );
			//vMovement = Vector3DHelper.getRandomDirectionInCone( m_d3Face, Settings.BC.RANDOM_TURN_ANGLE() );
		}
		
		// Reset all the movement/collision data
		m_d3aCollisions.clear();
		m_d3aMovements = new ArrayList<Double3D>();
		
		//if there is some signalling then the cell increases it's instantaneous velocity
		if(vectorMagnitude > Settings.BC.SIGNAL_THRESHOLD)
		{
			m_d3aMovements.add( vMovement.multiply( Settings.BC.TRAVEL_DISTANCE() +0.23 )); // was + 0.3
		}
		else if(vectorMagnitude < Settings.BC.SIGNAL_THRESHOLD && m_iL_r > 0) // this is also the case if receptors are saturated or equally biased in each direction, still signalling going on
		{
			//no signalling therefore no increase in instantaneous velocity
			m_d3aMovements.add( vMovement.multiply( Settings.BC.TRAVEL_DISTANCE() +0.23) ); // was + 0.3
		}
		else 
		{
			//no signalling therefore no increase in instantaneous velocity
			m_d3aMovements.add( vMovement.multiply( Settings.BC.TRAVEL_DISTANCE()) );
		}
		handleBounce();                 // Check for bounces
		receptorStep();                 // Step forward the receptor ODE
		registerCollisions( m_cgGrid ); // Register the new movement with the grid
	}
	
	
	
	
	/**
	 * How to remove a BC from the schedule:
	 * 
	 * when you schedule the BC it will return a stoppable object
	 * we store this object so we can access it when we need to 
	 * stop the object.
	 * 
	 * then to remove the object we simply call stop() on the 
	 * stopper object
	 * 
	 * the BC can then be removed by garbage collection
	 */
	public void removeDeadCell( Continuous3D randomSpace)
	{
		
		this.stop();
		randomSpace.remove(this);
	}
	
	  /**
	   * Flag to show if this class has been stopped (when no longer needed)
	   */
	private Stoppable stopper = null;

	/**
	 * Method to change the value of the stopper
	 * this is the stoppabkle object so we can access its stop method
	 * stoppable can acess BC but not the other way round
	 * @param stopper    Whether the class should be stopped or not
	 */
	public void setStopper(Stoppable stopper)   {this.stopper = stopper;}

	/**
	 * Method to stop the class where necessary
	 */
	public void stop(){stopper.stop();}
	
	
	
	/**
	 * Perform a step for the receptor 
	 * Euler method with step size 0.1
	 * TODO  assumes a timestep of 1 second!
	 * TODO better methods exist, but this was quick to implement

	 */
	private void receptorStep()
	{
		double[] iaBoundReceptors = calculateLigandBindingMoles();
		
		// Remove chemokine from the grid TODO: Just remove from where you are!!
		
		double avogadro = 6.0221409e+23;
		
		//this is in moles, not receptors so need to scale it before i remove, 
		// eg if i took away 10,000 that would be 10,000 moles which is not what we want!!!
		ParticleMoles.add( ParticleMoles.TYPE.CXCL13, (int) x + 1, (int) y, (int) z, -(iaBoundReceptors[0] / avogadro) );
		ParticleMoles.add( ParticleMoles.TYPE.CXCL13, (int) x - 1, (int) y, (int) z, -(iaBoundReceptors[1] / avogadro) );
		ParticleMoles.add( ParticleMoles.TYPE.CXCL13, (int) x, (int) y + 1, (int) z, -(iaBoundReceptors[2] / avogadro) );
		ParticleMoles.add( ParticleMoles.TYPE.CXCL13, (int) x, (int) y - 1, (int) z, -(iaBoundReceptors[3] / avogadro) );
		ParticleMoles.add( ParticleMoles.TYPE.CXCL13, (int) x, (int) y, (int) z + 1, -(iaBoundReceptors[4] / avogadro) );
		ParticleMoles.add( ParticleMoles.TYPE.CXCL13, (int) x, (int) y, (int) z - 1, -(iaBoundReceptors[5] / avogadro) );
		
		// update the amount of free and bound receptors
		for ( int i = 0; i < 6; i++ )
		{
			m_iR_free -= iaBoundReceptors[i];
			m_iL_r += iaBoundReceptors[i];	
		}

		int iTimesteps = 10;
		int iR_i, iL_r;
	
		for ( int i = 0; i < iTimesteps; i++ )
		{
			iR_i = m_iR_i;
			iL_r = m_iL_r;

			m_iR_free += (int) ((1.0 / iTimesteps) * Settings.BC.ODE.K_r() * iR_i);
			m_iR_i += (int) ((1.0 / iTimesteps) * Settings.BC.ODE.K_i() * iL_r) - (int) ((1.0 / iTimesteps) * Settings.BC.ODE.K_r() * iR_i);
			m_iL_r -= (int) ((1.0 / iTimesteps)  * Settings.BC.ODE.K_i() * iL_r);
		}
		
		if ( displayODEGraph && SimulationEnvironment.steadyStateReached ==  true )
		{
			//Grapher.updateODEGraph( m_iL_r ); //this gives an error when run on console
		}
	}
	
	/**  
	 * 
	 * Samples CXCL13 in the vicinity of the cell, and calculates a new movement
	 * direction. Also removes some CXCL13 from the simulation.
	 * 
	 * @return The new direction for the cell to move
	 */
	private Double3D getMoveDirection()
	{	
		double[] iaBoundReceptors = calculateLigandBindingMoles();
		
		//calculateMovementVector
		Double3D vMovement = new Double3D(); //the new direction for the cell to move
		
		// X
		vMovement = vMovement.add( new Double3D( 1, 0, 0 ).multiply( iaBoundReceptors[0] - iaBoundReceptors[1] ) );
		// Y
		vMovement = vMovement.add( new Double3D( 0, 1, 0 ).multiply( iaBoundReceptors[2] - iaBoundReceptors[3] ) );
		// Z
		vMovement = vMovement.add( new Double3D( 0, 0, 1 ).multiply( iaBoundReceptors[4] - iaBoundReceptors[5] ) );
		
		return vMovement;
	}
		
	

	
	
	/*
	 * Helper method to calculate the amount of ligand bound to receptor
	 * returns an int array with the number of bound receptors at each psuedopod
	 */
	private double[] calculateLigandBindingMoles()
	{
		
		//need to figure out what is sensible to secrete per timestep, might as well do that in moles
		// Get the surrounding concentrations
		double[][][] ia3Concs = ParticleMoles.get( ParticleMoles.TYPE.CXCL13, (int) x, (int) y, (int) z );
		
		// Assume the receptors are spread evenly around the cell
		int iReceptors = m_iR_free / 6;
		
		// get CXCL13 concentrations at each psuedopod
		// {x+, x-, y+, y-, z+, z-}
		double[] iaConcs = { ia3Concs[2][1][1], ia3Concs[0][1][1], ia3Concs[1][2][1], ia3Concs[1][0][1], ia3Concs[1][1][2],
				ia3Concs[1][1][0] };
		
		double[] iaBoundReceptors = new double[6]; //stores how many receptors are bound at each psuedopod
		
		for ( int i = 0; i < 6; i++ ) // for each pseudopod
		{
			double proportionToBind = (Settings.BC.ODE.K_a() *  iaConcs[i]);
			
			//cap the amount of receptors that can be bound
			if(proportionToBind > 1){proportionToBind = 1;}
			if(proportionToBind < 0){proportionToBind = 0;}
			
			iaBoundReceptors[i] = (int) (proportionToBind * iReceptors);
		}
		return iaBoundReceptors;
	}
	
	
	
	@Override
	public void registerCollisions( CollisionGrid cgGrid )
	{
		if ( cgGrid == null ){ return; }
		
		double dPosX = x;
		double dPosY = y;
		double dPosZ = z;
		
		for ( Double3D d3Movement : m_d3aMovements )
		{
			cgGrid.addLineToGrid( this, new Double3D( dPosX, dPosY, dPosZ ),
					new Double3D( dPosX + d3Movement.x, dPosY + d3Movement.y, dPosZ + d3Movement.z ),
					Settings.BC.COLLISION_RADIUS );
					
			dPosX += d3Movement.x;
			dPosY += d3Movement.y;
			dPosZ += d3Movement.z;
		}
	}
	
	
	/**
	 * Handles collisions between b cells and stroma
	 */
	@Override
	public void handleCollisions( CollisionGrid cgGrid )
	{
		// don't let a b cell collide more than collisionThreshold times
		// otherwise you get in an infinite loop where a B cell continues bouncing
		// indefinitely
		int collisionThreshold = 50;
		if ( m_i3lCollisionPoints.size() == 0 || collisionCounter > collisionThreshold)
		{
			return;
		}
				
		HashSet<Collidable> csCollidables = new HashSet<Collidable>(); // stores values uniquely in a hashset
		
		// Add all the cells to the set
		for ( Int3D i3Point : m_i3lCollisionPoints )
		{
			for ( Collidable cCollidable : cgGrid.getPoints( i3Point ) )
			{
				csCollidables.add( cCollidable );
			}
		}
		
		int iCollisionMovement = m_d3aMovements.size(); 
		boolean bCollision = false;
		
		// To keep a track of where we collided - we are only interested in the
		// first collision so we can ignore anything after this
		for ( Collidable cCell : csCollidables )
		{
			switch ( cCell.getCollisionClass() )
			{
				case STROMA_EDGE: // These first two are the more likely hits as they won't be moving
					if ( collideStromaEdge( (StromaEdge) cCell, iCollisionMovement ) )//TODO we can get T from this method
					{		
						iCollisionMovement = m_d3aMovements.size() - 1;
						bCollision = true;							
						//acquireAntigen(cCell);	
					}
					break;
				case STROMA:
					break;
				case BC:
					break;
			}
		}
		if ( bCollision ) //if the cell has collided
		{
			performCollision(cgGrid, iCollisionMovement); //deal with the collision
		}
	}
	
	


	/*
	 * helper method to perform the actual collision
	 */
	protected void performCollision(CollisionGrid cgGrid , int iCollisionMovement)
	{
		//increment the number of times a B cell has collided this time step
		collisionCounter++;
		// Add the collision point for visualisation
		double xPos = 0;
		double yPos = 0;
		double zPos = 0;
		for ( int i = 0; i < iCollisionMovement; i++ )
		{
			Double3D d3Movement = m_d3aMovements.get( i );
			xPos += d3Movement.x;
			yPos += d3Movement.y;
			zPos += d3Movement.z;
		}
		
		m_d3aCollisions.add( new Double3D( xPos, yPos, zPos ) );
		// Recheck for bounces and reregister with the grid
		handleBounce();
		registerCollisions( cgGrid );
	}
	
	
	/**
	 * Performs collision detection and handling with Stroma Edges (cylinders).
	 * 
	 * Find the closest point between two vectors (cell movement, stromal edge)
	 * backtrack to find where the vectors first meet eachother. The exact point
	 * that a collision first occurs.
	 * 
	 * update the B cells movement list m_d3aMovements
	 * 
	 * @param seEdge The edge to check collision with
	 * @param iCollisionMovement The movement to check collisions up to (this is to prevent colliding before bounces are handled)
	 * @return true if a collision occurs
	 * 
	 */
	protected boolean collideStromaEdge( StromaEdge seEdge, int iCollisionMovement )
	{
		Double3D p1 = new Double3D( x, y, z );
		Double3D p2 = seEdge.getPoint1();
		Double3D d2 = seEdge.getPoint2().subtract( p2 );
	
		for ( int i = 0; i < iCollisionMovement; i++ )
		{
			Double3D d1 = m_d3aMovements.get( i );
			
			// The two lines are p1 + s*d1 and p2 + t*d2
			// We are essentially trying to find the closest point between the
			// lines because that's an easy problem
			// using the fact that the line between them would be orthogonal to
			// both lines
			
			double s = 0;
			double t = 0;
			
			// This is all vector math explained in the following link. We are
			// essentially solving a system of linear
			// equations, and all the details are there.
			// https://q3k.org/gentoomen/Game%20Development/Programming/Real-Time%20Collision%20Detection.pdf
			// section 5.1.8, 5.1.9 psuedocode p146
			// see betterexplained trig and also dot product for intuitive understanding of how formulae are derived
			// 
			//Given a point S2(t) = P2 +td2 on a line segment, the closest point on another line is given by
			// closest point = S2(t) - P1.d1/d1.d1
			//
			//take a point on the segment and then draw a vector from the point to the start point P1 of the line
			// the dot product is just the projection of that vector onto the line (remember cos gives you the x-axis 
			// we are just making the line the x-axis). 
			//
			
			Double3D r = p1.subtract( p2 ); // p1 - p2
			double a = Vector3DHelper.dotProduct( d1, d1 ); //squared length of segment s1, always positive
			double b = Vector3DHelper.dotProduct( d1, d2 ); 
			double c = Vector3DHelper.dotProduct( d1, r );
			double e = Vector3DHelper.dotProduct( d2, d2 ); // squared length of segment s2, always positive
			double f = Vector3DHelper.dotProduct( d2, r );
			
			// differing from the link, dealing with lines so dont need to account for points
			//we therefore assume that neither are points (zero length)
			
			//(d1.d1)(d2.d2) - (d1.d2)(d1.d2)
			double denom = a * e - b * b; // >= 0
			
			//find the points on each line where the vectors are closest
			List<Double>closestPoints= findClosestPointsBetween(i,p1,p2,d1,d2,denom,s,t,a,b,c,e,f);
			
			s = closestPoints.get(0);
			t = closestPoints.get(1);
			
			
			//update the B cells T variable as this tells us how far along the stroma the B cell is
			
			this.setPositionAlongStroma(closestPoints.get(1));
			
			// So c1 and c2 are the points on the two lines which are closest to
			// one another
			// c1 = P1 + s.d1
			// c2 = P2 + t.d2
			Double3D c1 = p1.add( d1.multiply( s ) );
			Double3D c2 = p2.add( d2.multiply( t ) );
			
			//remember that the dot product of a vector times a vector equals its length squared
			double length = Vector3DHelper.dotProduct( c1.subtract( c2 ), c1.subtract( c2 ) );
			
			boolean bCollide = false; //what is this variable doing
			
			if (  length < BC_SE_COLLIDE_DIST_SQ ) //if the distance between the B cell and the stroma is lower than a threshold
			{
				bCollide = true;  // set the collision flag for the B cell to true
				double sNew = s; 
				
				// We want to find the actual point we collide so let's
				// backtrack a bit. . We don't get the exact point, but this does add a
				// little elasticity
				// Basically repeat the process until we go over
				while (length < BC_SE_COLLIDE_DIST_SQ+ 0.05 && s > 0 && s < 1)
				{
					sNew = calculateSNew(s, length, d1, d2);
					
					// Collision Detection p. 130
					// ab = d2, ac = point - p2, bc = point - seEdge.getPoint2()
					Double3D ac = p1.add( d1.multiply( sNew ) ).subtract(p2);
					Double3D bc = p1.add( d1.multiply( sNew ) ).subtract(seEdge.getPoint2());
					e = Vector3DHelper.dotProduct( ac, d2 );
					
					length = updateLength(length, ac, bc, e, f, d2);
					s = sNew; 
				}
			}
			
			if ( s == 0 )
			{
				Double3D d3Vec = c1.subtract( c2 );
				
				// we're already moving away!
				if ( Vector3DHelper.dotProduct( d3Vec, d1 ) > 0 )
				{
					bCollide = false;
				}
			}
			else if ( s == 1 )
			{
				bCollide = false;
			}

			if ( bCollide )
			{
				updateMovementToAccountForCollision( length,  d1,  d2,  p1,  p2, s,  t, i);
				return true;
			}
			else
			{
				// Move the BC location according to full the movement.
				p1 = p1.add( d1 );
			}
		}
		return false;
	}
	
	
	/**
	 * Helper Method to update movement after a collision occurs
	 * update the B cells m_d3aMovements
	 */
	private void updateMovementToAccountForCollision(double length, Double3D d1, Double3D d2, Double3D p1, Double3D p2, 
			double s, double t, int i)
	{
			Double3D d3NewDir;
			
			// Get the approach direction normalised, and in reverse
			Double3D d3MovementNormal = d1.multiply( -1 ).normalize();
			
			// We hit bang in the middle so just bounce - unlikely!
			if ( length == 0 )
			{
				d3NewDir = d3MovementNormal;
			}
			else
			{
				// Calculate the direction from the stroma collision point to the BC collision point
				Double3D d3BounceNormal = p1.add( d1.multiply( s ) ).subtract( p2.add( d2.multiply( t ) ) ).normalize();
						
				// reflect the movement normal about this point (rotate to it, and apply the same rotation again)
				d3NewDir = Vector3DHelper.rotateVectorToVector( d3MovementNormal, d3MovementNormal,d3BounceNormal );
				d3NewDir = Vector3DHelper.rotateVectorToVector( d3NewDir, d3MovementNormal, d3BounceNormal );
			}
			
			// Set the new movement
			d1 = d1.multiply( s );
			
			if ( d1.lengthSq() > 0 )
			{
				m_d3aMovements.set( i, d1 );
				i++;
			}
			
			// We need to add up all vectors after this one so we can add a
			// new vector of this length
			double dNewLength = 0;
			while (m_d3aMovements.size() > i)
			{
				dNewLength += m_d3aMovements.get( i ).length();
				m_d3aMovements.remove( i );
			}
			
			// add the remaining length of the current movement
			dNewLength += d1.length() * (1 - s);
			
			// slow down based on how fast we changed direction
			dNewLength *= (2+Vector3DHelper.dotProduct( d3NewDir, d3MovementNormal ))/3;
			d3NewDir = d3NewDir.multiply( dNewLength );
			
			if ( d3NewDir.lengthSq() > 0 )
			{
				m_d3aMovements.add( d3NewDir );
			}	
	}
	

	
	/**
	 * Helper method for collide stroma edge
	 * 
	 * returns a double, length
	 */
	private double updateLength(double length, Double3D ac,Double3D bc, double e, double f, Double3D d2)
	{
		if ( e <= 0 )
		{
			length = Vector3DHelper.dotProduct( ac, ac );
		}
		else
		{
			f = Vector3DHelper.dotProduct( d2, d2 );
			
			if ( e >= f )
			{
				length = Vector3DHelper.dotProduct( bc, bc );
			}
			else
			{
				length = Vector3DHelper.dotProduct( ac, ac ) - e*e/f;
			}
		}
		return length;
	}
	
	
	/**
	 * Helper method that determines the new value of S in collideStromaEdge
	 * where S is TODO what is s again
	 */
	private double calculateSNew(double s, double length, Double3D d1, Double3D d2)
	{
			double sNew = s; 
	
			// (Options.BC.COLLISION_RADIUS +
			// Options.FDC.STROMA_EDGE_RADIUS-Math.sqrt(length)) is the
			// length we're missing
			// So we just add that on.
			double dSinTheta = Math.sqrt( Vector3DHelper.crossProduct( d2, d1 ).lengthSq()/(d2.lengthSq()*d1.lengthSq()) ); // sin th
			double dActualLength = Math.sqrt( length );
			
			sNew = Math.max( 0, s-(0.04+Settings.BC.COLLISION_RADIUS + Settings.FDC.STROMA_EDGE_RADIUS - dActualLength)/(dSinTheta*d1.length()) );			
			return sNew;
	}
			

	
	/*
	 * Helper function which calculates the closest point between two lines
	 * called by collideStromaEdge, returns the closest points between two lines
	 * 
	 * Has lots of parameter inputs but this was the only way to encapsulate collision methods
	 */
	private List<Double> findClosestPointsBetween(int i, Double3D p1, Double3D p2,Double3D d1, Double3D d2,double denom,double s, double t,double a
			,double b, double c, double e, double f)
	{
			
			// if segments not parallel, compute closest point on L1 to L2
			// and clamp to segment S1. Else pick arbritrary closest point S
			// so compute closest point and clamp to segment 1
			if ( denom != 0 )
				{
					s = Math.min( 1.0, Math.max( 0.0, (b * f - c * e) / denom ) );
				}
					
			//compute point on L2 closest to S1(s) using
			// t = Dot((P1+D1*s)-P1,D1)/Dot(D2,D2) = (b*s + f/e)	
			t = b * s + f;//divide by e at the end to optimise p.151
					
			// if t in [0,1] done. Else, clamp t, recompute s for the new value
			// of t using s = Dot((P2 + D2*s) - P1,D1) / Dot(D1,D1) = (t*b - c) / a
			if ( t < 0 )
			{
				t = 0;
				s = Math.max( 0, Math.min( 1, -c / a ) );
			}
			else if ( t > e )
			{
				t = 1;
				s = Math.max( 0, Math.min( 1, (b - c) / a ) );
			}
			else
			{
				t /= e;
			}
				
			List<Double> closestPoints= new ArrayList<Double>();
			closestPoints.add(s);
			closestPoints.add(t);
	
			return closestPoints;
	}
	
	
	/**
	 * Bounces the cell back inside the boundaries Very long method! There's a
	 * lot of repeated code, but it's hard to efficiently abstract it out into
	 * more methods.
	 * 
	 * TODO a high level overview of algorithm in the description
	 * 
	 * if the B cell gets to the border of the simulation it has to bounce back
	 * as space is non-toroidal, much trickier in 3D than 2D
	 */
	private void handleBounce()
	{
		boolean bBounce = true;
		
		// We should in theory only have to check the last step for bounces
		int iMovementIndex = m_d3aMovements.size() - 1;
		
		double dPosX = x;
		double dPosY = y;
		double dPosZ = z;
		
		// add all movement vectors before the last one
		for ( int i = 0; i < iMovementIndex; i++ )
		{
			dPosX += m_d3aMovements.get( i ).x;
			dPosY += m_d3aMovements.get( i ).y;
			dPosZ += m_d3aMovements.get( i ).z;
		}
		
		// multiple bounces may occur, especially with long travel distances
		while (bBounce)
		{
			bBounce = false;
			
			double dNewPosX = dPosX;
			double dNewPosY = dPosY;
			double dNewPosZ = dPosZ;
			
			// add all movement vectors after the index, 
			// TODO why do we break up all of the movements like this, what do these variables represent
			for ( int i = iMovementIndex; i < m_d3aMovements.size(); i++ )
			{
				Double3D d3Movement = m_d3aMovements.get( i );
				dNewPosX += d3Movement.x;
				dNewPosY += d3Movement.y;
				dNewPosZ += d3Movement.z;
			}
			
			if ( dNewPosX > Settings.WIDTH - 1 || dNewPosX < 1 ) // Out of bounds on X axis
			{
				bBounce = handleBounceXaxis(dPosX, iMovementIndex);
			}
			
			if ( dNewPosY > Settings.HEIGHT - 1 || dNewPosY < 1 ) // out of bounds on Y axis
			{	
				bBounce = handleBounceYaxis(dPosY, iMovementIndex);
			}
			
			if ( dNewPosZ > Settings.DEPTH - 1 || dNewPosZ < 1 ) // out of bounds on Z axis
			{
				bBounce = handleBounceZaxis(dPosZ, iMovementIndex);
			}
		}
	}
	
	
	/*
	 * Helper method which handles collisions between a B cell and 
	 * the simulation borders along the X-axis
	 */
	private boolean handleBounceXaxis(double dPosX, int iMovementIndex){
		// There might be multiple vectors, so we need to keep track
		// of position, and whether we've hit the wall yet or not
		boolean bBounce = false;
		
		double dTempPosX = dPosX; //what are these variables keeping track of...
		boolean bFlipped = false;
		
		for ( int i = iMovementIndex; i < m_d3aMovements.size(); i++ )
		{
			Double3D d3Movement = m_d3aMovements.get( i );
			
			// if we have already hit the wall, we just flip the x axis
			// of all the remaining movements 
			if ( bFlipped )
			{
				m_d3aMovements.set( i, new Double3D( -d3Movement.x, d3Movement.y, d3Movement.z ) );
				continue;
			}
			
			// does this sub movement go out of bounds
			if ( dTempPosX + d3Movement.x < 1 || dTempPosX + d3Movement.x > Settings.WIDTH - 1 )
			{
				// Figure out at which point it goes out
				double dCutOff = 1;
				if ( dTempPosX + d3Movement.x < 1 )
				{
					dCutOff = (1 - dTempPosX) / d3Movement.x;
				}
				else
				{
					dCutOff = ((Settings.WIDTH - 1) - dTempPosX) / d3Movement.x;
				}
				
				// Create 2 new vectors split at the cutoff point, the
				// latter mirrored along the y axis
				Double3D d3TruncMovement = d3Movement.multiply( dCutOff );
				Double3D d3Remainder = new Double3D( -d3Movement.x + d3TruncMovement.x,
						d3Movement.y - d3TruncMovement.y, d3Movement.z - d3TruncMovement.z );
						
				// Replace the current one, then add the new one after it
				if ( d3TruncMovement.lengthSq() > 0 )
				{
					m_d3aMovements.set( i, d3TruncMovement );
					m_d3aMovements.add( i + 1, d3Remainder );
					
					// if we don't increment i, it will get flipped again!
					i++;
				}
				else
				{
					m_d3aMovements.set( i, d3Remainder );
				}
				
				bFlipped = true;
				bBounce = true;
			}
			
			dTempPosX += d3Movement.x;
		}
		return bBounce;
	}
	
	
	/*
	 * Helper method which handles collisions between a B cell and the 
	 * simulation borders along the Y-axis
	 */
	private boolean handleBounceYaxis(double dPosY, int iMovementIndex){
		// There might be multiple vectors now, so we need to keep track
		// of position, and whether we've hit the wall yet or not
		boolean bBounce = false;
		
		double dTempPosY = dPosY;
		boolean bFlipped = false;
		
		for ( int i = iMovementIndex; i < m_d3aMovements.size(); i++ )
		{
			Double3D d3Movement = m_d3aMovements.get( i );
			
			// if we have already hit the wall, we just flip the y axis
			// of all the remaining movements
			if ( bFlipped )
			{
				m_d3aMovements.set( i, new Double3D( d3Movement.x, -d3Movement.y, d3Movement.z ) );
				continue;
			}
			
			// does this sub movement go out of bounds
			if ( dTempPosY + d3Movement.y < 1 || dTempPosY + d3Movement.y > Settings.HEIGHT - 1 )
			{
				// Figure out at which point it goes out
				double dCutOff = 1;
				if ( dTempPosY + d3Movement.y < 1 )
				{
					dCutOff = (1 - dTempPosY) / d3Movement.y;
				}
				else
				{
					dCutOff = ((Settings.HEIGHT - 1) - dTempPosY) / d3Movement.y;
				}
				
				// Create 2 new vectors split at the cutoff point, the
				// latter mirrored along the y axis
				Double3D d3TruncMovement = d3Movement.multiply( dCutOff );
				Double3D d3Remainder = new Double3D( d3Movement.x - d3TruncMovement.x,
						-d3Movement.y + d3TruncMovement.y, d3Movement.z - d3TruncMovement.z );
						
				// Replace the current one, then add the new one after
				// it
				if ( d3TruncMovement.lengthSq() > 0 )
				{
					m_d3aMovements.set( i, d3TruncMovement );
					m_d3aMovements.add( i + 1, d3Remainder );
					// if we don't increment i, it will get flipped again!
					i++;
				}
				else
				{
					m_d3aMovements.set( i, d3Remainder );
				}
				
				bFlipped = true;
				bBounce = true;
			}
			
			dTempPosY += d3Movement.y;
		}
		return bBounce;
	}
	
	
    /*
     *  Helper method which handles collisions between a B cell and 
     *  the simulation borders along the Z-axis
     */
	private boolean handleBounceZaxis(double dPosZ, int iMovementIndex){
		// There might be multiple vectors now, so we need to keep track
		// of position, and whether we've hit the wall yet or not
		boolean bBounce = false;
		
		double dTempPosZ = dPosZ;
		boolean bFlipped = false;
		
		for ( int i = iMovementIndex; i < m_d3aMovements.size(); i++ )
		{
			Double3D d3Movement = m_d3aMovements.get( i );
			
			// if we have already hit the wall, we just flip the y axis
			// of all the movements
			if ( bFlipped )
			{
				m_d3aMovements.set( i, new Double3D( d3Movement.x, d3Movement.y, -d3Movement.z ) );
				continue;
			}
			
			// does this sub movement go out of bounds
			if ( dTempPosZ + d3Movement.z < 1 || dTempPosZ + d3Movement.z > Settings.DEPTH - 1 )
			{
				// Figure out at which point it goes out
				double dCutOff = 1;
				if ( dTempPosZ + d3Movement.z < 1 )
				{
					dCutOff = (1 - dTempPosZ) / d3Movement.z;
				}
				else
				{
					dCutOff = ((Settings.DEPTH - 1) - dTempPosZ) / d3Movement.z;
				}
				
				// Create 2 new vectors split at the cutoff point, the latter mirrored along the y axis
				Double3D d3TruncMovement = d3Movement.multiply( dCutOff );
				Double3D d3Remainder = new Double3D( d3Movement.x - d3TruncMovement.x,
						d3Movement.y - d3TruncMovement.y, -d3Movement.z + d3TruncMovement.z );
						
				// Replace the current one, then add the new one after it
				if ( d3TruncMovement.lengthSq() > 0 )
				{
					m_d3aMovements.set( i, d3TruncMovement );
					m_d3aMovements.add( i + 1, d3Remainder );
					
					// if we don't increment i, it will get flipped again!
					i++;
				}
				else
				{
					m_d3aMovements.set( i, d3Remainder );
				}
				
				bFlipped = true;
				bBounce = true;
			}
			
			dTempPosZ += d3Movement.z;
		}
		
		return bBounce;
	}
	
	////////////////////////////////////////////  3D Model for GUI  //////////////////////////////
	
	/*
	 * This is the 3D model of the B cell. Overrides JAVA 3D so we never actually 
	 * call it anywhere in the simulation ourselves
	 * (non-Javadoc)
	 * @see sim.portrayal3d.SimplePortrayal3D#getModel(java.lang.Object, javax.media.j3d.TransformGroup)
	 */
	@Override
	public TransformGroup getModel( Object obj, TransformGroup transf )
	{
		// We choose to always recalculate this model because the movement
		// changes in each time step.
		// Removing the movement indicators and removing this true will make the
		// 3d display a lot faster
		if ( transf == null || true )
		{
			transf = new TransformGroup();
			
			// Draw the BC itself
			SpherePortrayal3D s = new SpherePortrayal3D( Settings.BC.DRAW_COLOR(), Settings.BC.COLLISION_RADIUS * 2, 6 );
			s.setCurrentFieldPortrayal( getCurrentFieldPortrayal() );
			TransformGroup localTG = s.getModel( obj, null );
			
			localTG.setCapability( TransformGroup.ALLOW_TRANSFORM_WRITE );
			transf.addChild( localTG );
			
			//if we have had any collisions, draw them as red circles
			//modelCollisions(m_d3aCollisions,obj, transf);
			
			// If we have any movement, then draw it as white lines telling us where the cell is orientated
			//modelMovements(m_d3aMovements,obj, transf);
		}
		return transf;
	}
	
	
	/*
	 * Helper method
	 * Model movement of movement and add a white line indicating the orientation of the B cell 
	 */
	protected void modelMovements(List<Double3D> m_d3aMovements2,Object obj, TransformGroup transf)
	{
		// If we have any movement, then draw it
		if ( m_d3aMovements2.size() > 0 )
		{
			LineArray lineArr = new LineArray( m_d3aMovements2.size() * 2, GeometryArray.COORDINATES );
			lineArr.setCoordinate( 0, new Point3d( 0, 0, 0 ) );
			
			int i = 1;
			double xPos = 0, yPos = 0, zPos = 0;
			
			for ( int iIndex = 0; iIndex < m_d3aMovements2.size(); iIndex++ )
			{
				Double3D d3Movement = m_d3aMovements2.get( iIndex );
				
				if ( i > 1 )
				{
					lineArr.setCoordinate( i, new Point3d( xPos, yPos, zPos ) );
					i++;
				}
				xPos += d3Movement.x;
				yPos += d3Movement.y;
				zPos += d3Movement.z;
				lineArr.setCoordinate( i, new Point3d( xPos, yPos, zPos ) );
				i++;
			}
			Appearance aAppearance = new Appearance();
			Color col = Color.white;
			aAppearance.setColoringAttributes( new ColoringAttributes( col.getRed() / 255f, col.getGreen() / 255f,
					col.getBlue() / 255f, ColoringAttributes.FASTEST ) );
					
			Shape3D s3Shape = new Shape3D( lineArr, aAppearance );
			Shape3DPortrayal3D s2 = new Shape3DPortrayal3D( s3Shape, aAppearance );
			s2.setCurrentFieldPortrayal( getCurrentFieldPortrayal() );
			TransformGroup localTG2 = s2.getModel( obj, null );
			
			localTG2.setCapability( TransformGroup.ALLOW_TRANSFORM_WRITE );
			transf.addChild( localTG2 );
		}
	}
	
	
	/*
	 * Helper method
	 * Modelling collisions as red dots, called by getModel()
	 */
	protected void modelCollisions(ArrayList<Double3D> m_d3aCollisions,Object obj, TransformGroup transf)
	{	
		
	
		if ( m_d3aCollisions.size() > 0 ) //is this a global variable? should pass it into the method!!
		{
			for ( Double3D d3Point : m_d3aCollisions )
			{
				SpherePortrayal3D s2 = new SpherePortrayal3D( Color.RED, 0.25, 6 );
				s2.setCurrentFieldPortrayal( getCurrentFieldPortrayal() );
				TransformGroup localTG2 = s2.getModel( obj, null );
				
				Transform3D tTransform = new Transform3D();
				tTransform
						.setTranslation( new Vector3f( (float) d3Point.x, (float) d3Point.y, (float) d3Point.z ) );
						
				localTG2.setTransform( tTransform );
				
				localTG2.setCapability( TransformGroup.ALLOW_TRANSFORM_WRITE );
				transf.addChild( localTG2 );
			}
		}
	}

	public double getPositionAlongStroma() {
		return positionAlongStroma;
	}

	public void setPositionAlongStroma(double positionAlongStroma) {
		this.positionAlongStroma = positionAlongStroma;
	}











	
}
