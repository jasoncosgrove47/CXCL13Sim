package sim3d.cell;

import sim.util.*;
import sim.engine.*;
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

import sim.portrayal3d.simple.Shape3DPortrayal3D;
import sim.portrayal3d.simple.SpherePortrayal3D;
import sim3d.Grapher;
import sim3d.Options;
import sim3d.collisiondetection.Collidable;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.diffusion.Particle;
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
	private static final double	BC_SE_COLLIDE_DIST_SQ	= (Options.BC.COLLISION_RADIUS + Options.FDC.STROMA_EDGE_RADIUS)
			* (Options.BC.COLLISION_RADIUS + Options.FDC.STROMA_EDGE_RADIUS);
			
	/**
	 * Required to prevent a warning!
	 */
	private static final long	serialVersionUID		= 1;
														
	/**
	 * Boolean to tell the cell whether or not it should be sending graph data;
	 * used to make sure we only have one reporting!
	 */
	public boolean				displayGraph			= false;
														
	/**
	 * (ODE) Ligand-Receptor Complexes
	 */
	public int					m_iL_r					= 500;
	/**
	 * (ODE) Desensitised Receptor
	 */
	public int					m_iR_d					= 500;
	/**
	 * (ODE) Free Receptors on cell surface
	 */
	public int					m_iR_free				= 10000;
														
	/**
	 * (ODE) Internalised Receptor
	 */
	public int					m_iR_i					= 1000;
														
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
	List<Int3D>					m_i3lCollisionPoints	= new ArrayList<Int3D>();
		
	
	
	
	
	
	@Override
	public void step( final SimState state )//why is this final here
	{
		//each Bcell registers its intended path on the collision grid, 
		// once all B cells register the collision grid handles the movement
		// at the next iteration the B cells are moved. B cells only collide 
		// with stroma
		m_i3lCollisionPoints.clear();
		// If we have a stored movement, execute it
		if ( m_d3aMovements != null && m_d3aMovements.size() > 0 )
		{
			for ( Double3D d3Movement : m_d3aMovements )
			{
				x += d3Movement.x;
				y += d3Movement.y;
				z += d3Movement.z;
			}
			
			// Remember which way we're now facing
			m_d3Face = m_d3aMovements.get( m_d3aMovements.size() - 1 ).normalize();
			
			setObjectLocation( new Double3D( x, y, z ) );
		}
		
		// Calculate chemotaxis direction if we're above the receptor threshold
		Double3D vMovement;
		if ( m_iR_free > Options.BC.MIN_RECEPTORS )
		{
			vMovement = getMoveDirection();
			
			if ( vMovement.lengthSq() > 0 )
			{
				// Add some noise to the direction and take the average of our
				// current direction and the new direction
				vMovement = m_d3Face.add( Vector3DHelper.getBiasedRandomDirectionInCone( vMovement.normalize(),
						Options.BC.DIRECTION_ERROR() ) );
				if ( vMovement.lengthSq() > 0 )
				{
					vMovement = vMovement.normalize();
				}
			}
		}
		else
		{
			vMovement = null;
		}
		
		if ( vMovement == null || vMovement.lengthSq() == 0 )
		{
			// no data! so do a random turn
			vMovement = Vector3DHelper.getBiasedRandomDirectionInCone( m_d3Face, Options.BC.RANDOM_TURN_ANGLE() );
		}
		
		// Reset all the movement/collision data
		m_d3aCollisions.clear();
		m_d3aMovements = new ArrayList<Double3D>();
		m_d3aMovements.add( vMovement.multiply( Options.BC.TRAVEL_DISTANCE() ) );
		
		// Check for bounces
		handleBounce();
		
		// Step forward the receptor ODE
		receptorStep();
		
		// Register the new movement with the grid
		registerCollisions( m_cgGrid );
	}
	
	
	
	
	
	
	
	
	
	
	/**
	 * Adds a collision point to the list
	 * 
	 * @param i3Point
	 *            Coordinates for the collision
	 */
	@Override
	public void addCollisionPoint( Int3D i3Point )
	{
		m_i3lCollisionPoints.add( i3Point );
	}
	
	/**
	 * @return the ENUM representing the type of Collidable this cell is (BC)
	 */
	@Override
	public CLASS getCollisionClass()
	{
		return CLASS.BC;
	}
	
	@Override
	public Continuous3D getDrawEnvironment()
	{
		return drawEnvironment;
	}
	
	@Override
	public TransformGroup getModel( Object obj, TransformGroup transf )
	{
		// We choose to always recalculate this model because the movement
		// changes in each time step.
		// Removing the movement indicators and removing this true will make the
		// 3d display a lot faster
		if ( transf == null || true )
		{
			transf = new TransformGroup();//TODO what is a transform group
			
			// Draw the BC itself
			SpherePortrayal3D s = new SpherePortrayal3D( Options.BC.DRAW_COLOR(), Options.BC.COLLISION_RADIUS * 2, 6 );
			s.setCurrentFieldPortrayal( getCurrentFieldPortrayal() );
			TransformGroup localTG = s.getModel( obj, null );
			
			localTG.setCapability( TransformGroup.ALLOW_TRANSFORM_WRITE );
			transf.addChild( localTG );
			
			// If we have had any collisions, draw them as red circles
			// TODO encapsulate as visualise collisions
			if ( m_d3aCollisions.size() > 0 )
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
			
			// If we have any movement, then draw it
			// TODO are these the white lines that we see?
			if ( m_d3aMovements.size() > 0 )
			{
				LineArray lineArr = new LineArray( m_d3aMovements.size() * 2, GeometryArray.COORDINATES );
				lineArr.setCoordinate( 0, new Point3d( 0, 0, 0 ) );
				
				int i = 1;
				double xPos = 0, yPos = 0, zPos = 0;
				
				for ( int iIndex = 0; iIndex < m_d3aMovements.size(); iIndex++ )
				{
					Double3D d3Movement = m_d3aMovements.get( iIndex );
					
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
		return transf;
	}
	
	@Override
	public void registerCollisions( CollisionGrid cgGrid )

	{
		if ( cgGrid == null )
		{
			return;
		}
		
		double dPosX = x;
		double dPosY = y;
		double dPosZ = z;
		
		//TODO need to get this bit of code explained
		for ( Double3D d3Movement : m_d3aMovements )
		{
			cgGrid.addLineToGrid( this, new Double3D( dPosX, dPosY, dPosZ ),
					new Double3D( dPosX + d3Movement.x, dPosY + d3Movement.y, dPosZ + d3Movement.z ),
					Options.BC.COLLISION_RADIUS );
					
			dPosX += d3Movement.x;
			dPosY += d3Movement.y;
			dPosZ += d3Movement.z;
		}
	}
	
	
	
	
	@Override
	public void handleCollisions( CollisionGrid cgGrid )
	{
		if ( m_i3lCollisionPoints.size() == 0 )
		{
			return;
		}
		
		// We're using a set because it stores values uniquely!
		HashSet<Collidable> csCollidables = new HashSet<Collidable>();
		
		// Add all the cells to the set
		for ( Int3D i3Point : m_i3lCollisionPoints )
		{
			for ( Collidable cCollidable : cgGrid.getPoints( i3Point ) )
			{
				csCollidables.add( cCollidable );
			}
		}
		
		boolean bCollision = false;
		
		// To keep a track of where we collided - we are only interested in the
		// first collision so we can ignore
		// anything after this
		int iCollisionMovement = m_d3aMovements.size(); // TODO what is this line doing
		
		for ( Collidable cCell : csCollidables )
		{
			switch ( cCell.getCollisionClass() )
			{
				// These first two are the more likely hits as they won't be
				// moving
				case STROMA_EDGE:
					if ( collideStromaEdge( (StromaEdge) cCell, iCollisionMovement ) )
					{
						iCollisionMovement = m_d3aMovements.size() - 1;
						bCollision = true;
					}
					break;
				case STROMA:
					break;
				case BC:
					break;
			}
		}
		
		if ( bCollision )
		{
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
	}
	
	@Override
	public boolean isStatic()
	{
		return false;
	}
	


	/**
	 * Performs collision detection and handling with Stroma Edges (cylinders)
	 * 
	 * @param seEdge
	 *            The edge to check collision with
	 * @param iCollisionMovement
	 *            The movement to check collisions up to (this is to prevent
	 *            colliding before bounces are handled)
	 * @return true if a collision occurs
	 */
	private boolean collideStromaEdge( StromaEdge seEdge, int iCollisionMovement )
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
			// section 5.1.8
			// p146
			
			Double3D r = p1.subtract( p2 );
			
			double a = Vector3DHelper.dotProduct( d1, d1 );
			double b = Vector3DHelper.dotProduct( d1, d2 );
			double c = Vector3DHelper.dotProduct( d1, r );
			double e = Vector3DHelper.dotProduct( d2, d2 );
			double f = Vector3DHelper.dotProduct( d2, r );
			
			// differing from the link, we assume that neither are points (zero
			// length)
			
			double denom = a * e - b * b; // >= 0
			
			// not parallel, so compute closest point and clamp to segment 1
			if ( denom != 0 )
			{
				s = Math.min( 1.0, Math.max( 0.0, (b * f - c * e) / denom ) );
			}
			
			t = b * s + f;
			
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
			
			// So c1 and c2 are the points on the two lines which are closest to
			// one another
			Double3D c1 = p1.add( d1.multiply( s ) );
			Double3D c2 = p2.add( d2.multiply( t ) );
			
			double length = Vector3DHelper.dotProduct( c1.subtract( c2 ), c1.subtract( c2 ) );
			
			boolean bCollide = false;
			
			// If the length is within a range (note: we use length*1.05 to
			// improve computation time. It is a good approximation
			if (  length < BC_SE_COLLIDE_DIST_SQ )
			{
				bCollide = true;
				double sNew = s;
				
				// We want to find the actual point we collide so let's
				// backtrack a bit. We use 1.02 so this process doesn't go
				// on forever. We don't get the exact point, but this does add a
				// little elasticity
				// Basically repeat the process until we go over
				while (length < BC_SE_COLLIDE_DIST_SQ && s > 0 && s < 1)
				{
					s = sNew;
					// (Options.BC.COLLISION_RADIUS +
					// Options.FDC.STROMA_EDGE_RADIUS-Math.sqrt(length)) is the
					// length we're missing
					// So we just add that on. TODO If lines are basically
					// parallel, this might take a while
					double dSinTheta = Math.sqrt( Vector3DHelper.crossProduct( d2, d1 ).lengthSq()/(d2.lengthSq()*d1.lengthSq()) ); // sin th
					double dActualLength = Math.sqrt( length );
					
					sNew = Math.max( 0, s-(0.02+Options.BC.COLLISION_RADIUS + Options.FDC.STROMA_EDGE_RADIUS - dActualLength)/(dSinTheta*d1.length()) );/**/
					
					/*sNew = Math.max( 0,
							s - (0.1 + Options.BC.COLLISION_RADIUS + Options.FDC.STROMA_EDGE_RADIUS - Math.sqrt( length ))
									/ d1.length() );/**/

					//TODO this section isn't very efficient which is unfortunate given how often it will be run...
					// Firstly, if t==0 or t==1 then the above will be an awful approximation. I think the math was ok, but I'm not 100%...
					
					t = b * sNew + f;
					
					if ( t < 0 )
					{
						t = 0;
					}
					else if ( t > e )
					{
						t = 1;
					}
					else
					{
						t /= e;
					}
					
					c1 = p1.add( d1.multiply( sNew ) );
					c2 = p2.add( d2.multiply( t ) );
					length = Vector3DHelper.dotProduct( c1.subtract( c2 ), c1.subtract( c2 ) );
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
			
			// if we collide, check if the nearest point isn't at the end. If it
			// is, then we've already collided with something in
			// a previous step so don't bother
			if ( bCollide )
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
					// Calculate the direction from the stroma collision point
					// to the BC collision point
					Double3D d3BounceNormal = p1.add( d1.multiply( s ) ).subtract( p2.add( d2.multiply( t ) ) )
							.normalize();
							
					// reflect the movement normal about this point (rotate to
					// it, and apply the same rotation again)
					d3NewDir = Vector3DHelper.rotateVectorToVector( d3MovementNormal, d3MovementNormal,
							d3BounceNormal );
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
	 * Samples CXCL13 in the vicinity of the cell, and calculates a new movement
	 * direction. Also removes some CXCL13 from the simulation.
	 * 
	 * Should just remove it at the gridpoint where you actually are
	 * 
	 * @return The new direction for the cell to move
	 */
	private Double3D getMoveDirection()
	{
		// Get the surrounding concentrations
		int[][][] ia3Concs = Particle.get( Particle.TYPE.CXCL13, (int) x, (int) y, (int) z );
		
		// Assume the receptors are spread evenly around the cell
		int iReceptors = m_iR_free / 6;
		
		// {x+, x-, y+, y-, z+, z-}
		int[] iaConcs = { ia3Concs[2][1][1], ia3Concs[0][1][1], ia3Concs[1][2][1], ia3Concs[1][0][1], ia3Concs[1][1][2],
				ia3Concs[1][1][0] };
				
		int[] iaBoundReceptors = new int[6];
		// TODO this is just 1s!!
		
		
		//TODO why is this here, we update the receptors elsewhere
		for ( int i = 0; i < 6; i++ )
		{
			iaBoundReceptors[i] = (int) (Options.BC.ODE.K_a() * Math.sqrt( iReceptors * iaConcs[i] ));
			m_iR_free -= iaBoundReceptors[i];
			m_iL_r += iaBoundReceptors[i];
		}
		
		// Remove chemokine from the grid
		Particle.add( Particle.TYPE.CXCL13, (int) x + 1, (int) y, (int) z, -iaBoundReceptors[0] );
		Particle.add( Particle.TYPE.CXCL13, (int) x - 1, (int) y, (int) z, -iaBoundReceptors[1] );
		Particle.add( Particle.TYPE.CXCL13, (int) x, (int) y + 1, (int) z, -iaBoundReceptors[2] );
		Particle.add( Particle.TYPE.CXCL13, (int) x, (int) y - 1, (int) z, -iaBoundReceptors[3] );
		Particle.add( Particle.TYPE.CXCL13, (int) x, (int) y, (int) z + 1, -iaBoundReceptors[4] );
		Particle.add( Particle.TYPE.CXCL13, (int) x, (int) y, (int) z - 1, -iaBoundReceptors[5] );
		
		Double3D vMovement = new Double3D();
		
		// X
		vMovement = vMovement.add( new Double3D( 1, 0, 0 ).multiply( iaBoundReceptors[0] - iaBoundReceptors[1] ) );
		// Y
		vMovement = vMovement.add( new Double3D( 0, 1, 0 ).multiply( iaBoundReceptors[2] - iaBoundReceptors[3] ) );
		// Z
		vMovement = vMovement.add( new Double3D( 0, 0, 1 ).multiply( iaBoundReceptors[4] - iaBoundReceptors[5] ) );
		
		return vMovement;
	}
	
	/**
	 * Bounces the cell back inside the boundaries Very long method! There's a
	 * lot of repeated code, but it's hard to efficiently abstract it out into
	 * more methods.
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
			
			// add all movement vectors after the index
			for ( int i = iMovementIndex; i < m_d3aMovements.size(); i++ )
			{
				Double3D d3Movement = m_d3aMovements.get( i );
				dNewPosX += d3Movement.x;
				dNewPosY += d3Movement.y;
				dNewPosZ += d3Movement.z;
			}
			
			// If we go out of bounds on either side
			if ( dNewPosX > Options.WIDTH - 1 || dNewPosX < 1 )
			{
				// There might be multiple vectors, so we need to keep track
				// of position, and whether we've hit the wall yet or not
				double dTempPosX = dPosX;
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
					if ( dTempPosX + d3Movement.x < 1 || dTempPosX + d3Movement.x > Options.WIDTH - 1 )
					{
						// Figure out at which point it goes out
						double dCutOff = 1;
						if ( dTempPosX + d3Movement.x < 1 )
						{
							dCutOff = (1 - dTempPosX) / d3Movement.x;
						}
						else
						{
							dCutOff = ((Options.WIDTH - 1) - dTempPosX) / d3Movement.x;
						}
						
						// Create 2 new vectors split at the cutoff point, the
						// latter mirrored along the y axis
						Double3D d3TruncMovement = d3Movement.multiply( dCutOff );
						Double3D d3Remainder = new Double3D( -d3Movement.x + d3TruncMovement.x,
								d3Movement.y - d3TruncMovement.y, d3Movement.z - d3TruncMovement.z );
								
						// Replace the current one, then add the new one after
						// it
						if ( d3TruncMovement.lengthSq() > 0 )
						{
							m_d3aMovements.set( i, d3TruncMovement );
							m_d3aMovements.add( i + 1, d3Remainder );
							
							// if we don't increment i, it will get flipped
							// again!
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
			}
			
			// If we go out of bounds at the top or bottom in the overall
			// movement
			if ( dNewPosY > Options.HEIGHT - 1 || dNewPosY < 1 )
			{
				// There might be multiple vectors now, so we need to keep track
				// of position, and whether we've hit the wall yet or not
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
					if ( dTempPosY + d3Movement.y < 1 || dTempPosY + d3Movement.y > Options.HEIGHT - 1 )
					{
						// Figure out at which point it goes out
						double dCutOff = 1;
						if ( dTempPosY + d3Movement.y < 1 )
						{
							dCutOff = (1 - dTempPosY) / d3Movement.y;
						}
						else
						{
							dCutOff = ((Options.HEIGHT - 1) - dTempPosY) / d3Movement.y;
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
							
							// if we don't increment i, it will get flipped
							// again!
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
			}
			
			// If we go out of bounds at the front or back in the overall
			// movement
			if ( dNewPosZ > Options.DEPTH - 1 || dNewPosZ < 1 )
			{
				// There might be multiple vectors now, so we need to keep track
				// of position, and whether we've hit the wall yet or not
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
					if ( dTempPosZ + d3Movement.z < 1 || dTempPosZ + d3Movement.z > Options.DEPTH - 1 )
					{
						// Figure out at which point it goes out
						double dCutOff = 1;
						if ( dTempPosZ + d3Movement.z < 1 )
						{
							dCutOff = (1 - dTempPosZ) / d3Movement.z;
						}
						else
						{
							dCutOff = ((Options.DEPTH - 1) - dTempPosZ) / d3Movement.z;
						}
						
						// Create 2 new vectors split at the cutoff point, the
						// latter mirrored along the y axis
						Double3D d3TruncMovement = d3Movement.multiply( dCutOff );
						Double3D d3Remainder = new Double3D( d3Movement.x - d3TruncMovement.x,
								d3Movement.y - d3TruncMovement.y, -d3Movement.z + d3TruncMovement.z );
								
						// Replace the current one, then add the new one after
						// it
						if ( d3TruncMovement.lengthSq() > 0 )
						{
							m_d3aMovements.set( i, d3TruncMovement );
							m_d3aMovements.add( i + 1, d3Remainder );
							
							// if we don't increment i, it will get flipped
							// again!
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
			}
		}
	}
	
	/**
	 * Perform a step for the receptor TODO: this assumes a timestep of 1
	 * second!
	 */
	private void receptorStep()
	{
		// Euler method with step size 0.1
		// TODO better methods exist, but this was quick to implement
		int iTimesteps = 10;
		int iR_i, iR_d, iL_r;
		for ( int i = 0; i < iTimesteps; i++ )
		{
			iR_i = m_iR_i;
			iR_d = m_iR_d;
			iL_r = m_iL_r;
			
			m_iR_d += (int) ((1.0 / iTimesteps) * iL_r * iL_r
					/ (Options.BC.ODE.gamma() * (1 + Math.pow( iL_r / Options.BC.ODE.delta(), 2 ) + iL_r)))
					- (int) ((1.0 / iTimesteps) * Options.BC.ODE.K_i() * iR_d);
			m_iR_i += (int) ((1.0 / iTimesteps) * Options.BC.ODE.K_i() * iR_d)
					- (int) ((1.0 / iTimesteps) * Options.BC.ODE.K_r() * iR_i);
			m_iR_free += (int) ((1.0 / iTimesteps) * Options.BC.ODE.K_r() * iR_i);
			m_iL_r -= (int) ((1.0 / iTimesteps) * iL_r * iL_r
					/ (Options.BC.ODE.gamma() * (1 + Math.pow( iL_r / Options.BC.ODE.delta(), 2 ) + iL_r)));
		}
		
		if ( displayGraph )
		{
			Grapher.addPoint( m_iR_free ); //this gives an error when run on console
		}
	}
}
