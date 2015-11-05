package sim3d.cell;
import sim.util.*;
import sim.engine.*;
import sim.field.continuous.Continuous2D;
import sim.field.continuous.Continuous3D;

import java.awt.Color;
import java.awt.Graphics2D;
//import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.LineArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3f;

import sim.portrayal.*;
import sim.portrayal3d.simple.Shape3DPortrayal3D;
import sim.portrayal3d.simple.SpherePortrayal3D;
import sim3d.Grapher;
import sim3d.Options;
import sim3d.collisiondetection.Collidable;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.diffusion.Particle;
import sim3d.util.Vector3DHelper;

public class BC extends DrawableCell3D implements Steppable, Collidable
{
	private static final long serialVersionUID = 1;

	/**
	 * (Display) Whether the last movement was random or directed.
	 */
    private boolean m_bRandom = true;
    /**
     * (Display) Whether the cell could move or not in the last time step.
     */
    private boolean m_bCantMove = false;
    /**
     * The direction the cell is facing
     */
    private Double3D m_d3Face = Vector3DHelper.getRandomDirection();

    /**
     * (ODE) The number of free receptors on the cell surface
     */
    public int m_iR_free = 1000;
    /**
     * (ODE)
     */
    public int m_iR_i = 1000;
    /**
     * (ODE)
     */
    public int m_iR_d = 500;
    /**
     * (ODE)
     */
    public int m_iL_r = 500;
    
    public static CollisionGrid m_cgGrid;
    
	/* Draw Environment accessor */
	public static Continuous3D drawEnvironment;
	@Override public Continuous3D getDrawEnvironment(){
		return drawEnvironment;
	}
	
    public BC() 
    {
    }
    
    public void step( final SimState state )
    {
    	if ( m_d3aMovements != null && m_d3aMovements.size() > 0 )
    	{
    		m_d3aMovementsOld = m_d3aMovements;
    		for ( Double3D d3Movement : m_d3aMovements )
    		{
    			x += d3Movement.x;
    			y += d3Movement.y;
    			z += d3Movement.z;
    		}

	    	// Remember which way we're now facing
			m_d3Face = m_d3aMovements.get(m_d3aMovements.size()-1).normalize();

	    	setObjectLocation( new Double3D(x, y, z) );
    	}
		m_bRandom = true;
		
		// Have to be careful it actually gets set!
		// needs to be null because there's no else for the next if statement
		Double3D vMovement = null;
		if ( m_iR_free > Options.BC.MIN_RECEPTORS())
		{
    		vMovement = getMoveDirection();
    		if ( vMovement.length() > 0 )
    		{
    			m_bRandom = false;
    			vMovement = m_d3Face.add(Vector3DHelper.getBiasedRandomDirectionInCone(vMovement.normalize(), Options.BC.DIRECTION_ERROR()));
    			if ( vMovement.length() > 0 )
    			{
    				vMovement = vMovement.normalize();
    			}
    		}
    	}
		
    	if (m_bRandom)
    	{
    		// no data! so do a random turn
    		vMovement = Vector3DHelper.getBiasedRandomDirectionInCone(m_d3Face, Options.BC.RANDOM_TURN_ANGLE());
    	}
    	
    	m_d3aCollisions.clear();
    	m_d3aMovements = new ArrayList<Double3D>();
    	m_d3aMovements.add(vMovement.multiply(Options.BC.TRAVEL_DISTANCE()));
    	
    	handleBounce();

    	receptorStep();
    	
    	registerCollisions(m_cgGrid);
    }
    
    public final void draw(Object object,  final Graphics2D graphics, final DrawInfo2D info)
    {
    	if ( m_bCantMove )
    	{
    		graphics.setColor(getColorWithDepth(Color.red));
    	}
    	else if ( m_bRandom )
    	{
    		graphics.setColor(getColorWithDepth(Options.BC.RANDOM_COLOR()));
    	}
    	else
    	{
    		graphics.setColor(getColorWithDepth(Options.BC.DRAW_COLOR()));
    	}
    	graphics.fillOval((int)Math.round(info.draw.x-info.draw.width/2), (int)Math.round(info.draw.y-info.draw.height/2), (int)Math.round(info.draw.width), (int)Math.round(info.draw.height));
    }
    
    /**
     * Private Methods
     */


    
    private Double3D getMoveDirection()
    {
    	// Get the surrounding concentrations
    	int[][][] ia3Concs = Particle.get(Particle.TYPE.CXCL13, (int)x, (int)y, (int)z);
    	
    	// {x+, x-, y+, y-, z+, z-}
    	int iReceptors = (int)(m_iR_free/6);

    	// {x+, x-, y+, y-, z+, z-}
    	int[] iaConcs = {ia3Concs[2][1][1],
    					 ia3Concs[0][1][1],
    					 ia3Concs[1][2][1],
    					 ia3Concs[1][0][1],
    					 ia3Concs[1][1][2],
    					 ia3Concs[1][1][0]};
    	
    	int[] iaBoundReceptors = new int[6];
    	for ( int i = 0; i < 6; i++ )
    	{
    		iaBoundReceptors[i] = (int)(Options.BC.ODE.K_a()*Math.sqrt(iReceptors*iaConcs[i]));
			m_iR_free -= iaBoundReceptors[i];
			m_iL_r    += iaBoundReceptors[i];
    	}

		Particle.add(Particle.TYPE.CXCL13, (int)x+1, (int)y,   (int)z,   -iaBoundReceptors[0]);
		Particle.add(Particle.TYPE.CXCL13, (int)x-1, (int)y,   (int)z,   -iaBoundReceptors[1]);
		Particle.add(Particle.TYPE.CXCL13, (int)x,   (int)y+1, (int)z,   -iaBoundReceptors[2]);
		Particle.add(Particle.TYPE.CXCL13, (int)x,   (int)y-1, (int)z,   -iaBoundReceptors[3]);
		Particle.add(Particle.TYPE.CXCL13, (int)x,   (int)y,   (int)z+1, -iaBoundReceptors[4]);
		Particle.add(Particle.TYPE.CXCL13, (int)x,   (int)y,   (int)z-1, -iaBoundReceptors[5]);
    	
    	Double3D vMovement = new Double3D();

    	// X
    	vMovement = vMovement.add(new Double3D(1, 0, 0).multiply(iaBoundReceptors[0]-iaBoundReceptors[1]));
    	// Y
    	vMovement = vMovement.add(new Double3D(0, 1, 0).multiply(iaBoundReceptors[2]-iaBoundReceptors[3]));
    	// Z
    	vMovement = vMovement.add(new Double3D(0, 0, 1).multiply(iaBoundReceptors[4]-iaBoundReceptors[5]));
    	
    	return vMovement;
    }
    
    public boolean displayGraph = false;
    
    private void receptorStep()
    {
    	int iTimesteps = 10;
    	int iR_i, iR_d, iL_r;
    	for ( int i = 0; i < iTimesteps; i++ )
    	{
    		iR_i = m_iR_i;
    		iR_d = m_iR_d;
    		iL_r = m_iL_r;

    		m_iR_d		+= (int)((1.0/iTimesteps)*iL_r*iL_r/(Options.BC.ODE.gamma()*(1 + Math.pow(iL_r/Options.BC.ODE.delta(), 2) + iL_r)))
    				     - (int)((1.0/iTimesteps)*Options.BC.ODE.K_i() * iR_d);
    		m_iR_i		+= (int)((1.0/iTimesteps)*Options.BC.ODE.K_i() * iR_d)
					 	 - (int)((1.0/iTimesteps)*Options.BC.ODE.K_r() * iR_i);
    		m_iR_free	+= (int)((1.0/iTimesteps)*Options.BC.ODE.K_r() * iR_i);
    		m_iL_r		-= (int)((1.0/iTimesteps)*iL_r*iL_r/(Options.BC.ODE.gamma()*(1 + Math.pow(iL_r/Options.BC.ODE.delta(), 2) + iL_r)));
    	}
		
    	if ( displayGraph )
    	{
    		Grapher.addPoint(m_iR_free);
    		//Grapher.dataSets.get(1).add(m_iR_i);
    		//Grapher.dataSets.get(2).add(m_iR_d);
    		//Grapher.dataSets.get(3).add(m_iL_r);
    	}
    }

    List<Double3D> m_d3aMovements = new ArrayList<Double3D>();
    List<Double3D> m_d3aMovementsOld = new ArrayList<Double3D>();
    
	@Override
	public boolean isStatic()
	{
		return false;
	}

	@Override
	public void registerCollisions(CollisionGrid cgGrid)
	{
		double dPosX = x;
		double dPosY = y;
		double dPosZ = z;
		
		for ( Double3D d3Movement : m_d3aMovements )
		{
			cgGrid.addLineToGrid(this, new Double3D(dPosX, dPosY, dPosZ), new Double3D(dPosX+d3Movement.x, dPosY+d3Movement.y, dPosZ+d3Movement.z), Options.BC.COLLISION_RADIUS);
			
			dPosX += d3Movement.x;
			dPosY += d3Movement.y;
			dPosZ += d3Movement.z;
		}
	}
	
	/**
	 * Bounces the cell back inside the boundaries
	 */
	private void handleBounce()
	{
		/*
		 * Very long method! There's a lot of repeated code, but it's hard to efficiently
	 	 * abstract that out into more methods.
		 */
		
		boolean bBounce = true;

		// We should in theory only have to check the last step for bounces
		int iMovementIndex = m_d3aMovements.size()-1;
		
		double dPosX = x;
		double dPosY = y;
		double dPosZ = z;

		// add all movement vectors before the last one
		for ( int i = 0; i < iMovementIndex; i++ )
		{
			dPosX += m_d3aMovements.get(i).x;
			dPosY += m_d3aMovements.get(i).y;
			dPosZ += m_d3aMovements.get(i).z;
		}
		
		while ( bBounce )
		{
			bBounce = false;

			
			double dNewPosX = dPosX;
			double dNewPosY = dPosY;
			double dNewPosZ = dPosZ;
			
			// add all movement vectors after the index
			for ( int i = iMovementIndex; i < m_d3aMovements.size(); i++ )
			{
				Double3D d3Movement = m_d3aMovements.get(i);
				dNewPosX += d3Movement.x;
				dNewPosY += d3Movement.y;
				dNewPosZ += d3Movement.z;
			}
			
			// If we go out of bounds on either side
	    	if ( dNewPosX > Options.WIDTH - 1 || dNewPosX < 1 )
	    	{
	    		// There might be multiple vectors now, so we need to keep track of position, and whether we've hit the wall yet or not
	    		double dTempPosX = dPosX;
	    		boolean bFlipped = false;
	    		
	    		for ( int i = iMovementIndex; i < m_d3aMovements.size(); i++ )
	    		{
	    			Double3D d3Movement = m_d3aMovements.get(i);
	    			
	    			// if we have already hit the wall, we just flip the y axis of all the remaining movements
	    			if ( bFlipped )
	    			{
	    				m_d3aMovements.set(i, new Double3D(-d3Movement.x, d3Movement.y, d3Movement.z));
	    				continue;
	    			}
	    			
	    			// does this sub movement go out of bounds
	    			if ( dTempPosX + d3Movement.x < 1 || dTempPosX + d3Movement.x > Options.WIDTH - 1 )
	    			{
	    				// Figure out at which point it goes out
	    				double dCutOff = 1;
	    				if ( dTempPosX + d3Movement.x < 1 )
	    				{
	    					dCutOff = (1 - dTempPosX)/d3Movement.x;
	    				}
	    				else
	    				{	
	    					dCutOff = ((Options.WIDTH-1) - dTempPosX)/d3Movement.x;
	    				}
	    				
	    				// Create 2 new vectors split at the cutoff point, the latter mirrored along the y axis
	    				Double3D d3TruncMovement = d3Movement.multiply(dCutOff);
	    				Double3D d3Remainder = new Double3D(-d3Movement.x + d3TruncMovement.x,
	    													 d3Movement.y - d3TruncMovement.y,
	    													 d3Movement.z - d3TruncMovement.z
	    													);
	    	    		
	    				// Replace the current one, then add the new one after it
	    	    		m_d3aMovements.set(i, d3TruncMovement);
	    	    		m_d3aMovements.add(i+1, d3Remainder);
	    	    		
	    	    		// if we don't increment i, it will get flipped again!
	    	    		i++;
	    	    		
	    				bFlipped = true;
	    				bBounce = true;
	    			}
	    			
	    			dTempPosX += d3Movement.x;
	    		}
	    	}
	    	
			// If we go out of bounds at the top or bottom in the overall movement
	    	if ( dNewPosY > Options.HEIGHT - 1 || dNewPosY < 1 )
	    	{
	    		// There might be multiple vectors now, so we need to keep track of position, and whether we've hit the wall yet or not
	    		double dTempPosY = dPosY;
	    		boolean bFlipped = false;
	    		
	    		for ( int i = iMovementIndex; i < m_d3aMovements.size(); i++ )
	    		{
	    			Double3D d3Movement = m_d3aMovements.get(i);
	    			
	    			// if we have already hit the wall, we just flip the y axis of all the remaining movements
	    			if ( bFlipped )
	    			{
	    				m_d3aMovements.set(i, new Double3D(d3Movement.x, -d3Movement.y, d3Movement.z));
	    				continue;
	    			}
	    			
	    			// does this sub movement go out of bounds
	    			if ( dTempPosY + d3Movement.y < 1 || dTempPosY + d3Movement.y > Options.HEIGHT - 1 )
	    			{
	    				// Figure out at which point it goes out
	    				double dCutOff = 1;
	    				if ( dTempPosY + d3Movement.y < 1 )
	    				{
	    					dCutOff = (1 - dTempPosY)/d3Movement.y;
	    				}
	    				else
	    				{	
	    					dCutOff = ((Options.HEIGHT-1) - dTempPosY)/d3Movement.y;
	    				}
	    				
	    				// Create 2 new vectors split at the cutoff point, the latter mirrored along the y axis
	    				Double3D d3TruncMovement = d3Movement.multiply(dCutOff);
	    				Double3D d3Remainder = new Double3D( d3Movement.x - d3TruncMovement.x,
	    													-d3Movement.y + d3TruncMovement.y,
	    													 d3Movement.z - d3TruncMovement.z
	    													);
	    	    		
	    				// Replace the current one, then add the new one after it
	    	    		m_d3aMovements.set(i, d3TruncMovement);
	    	    		m_d3aMovements.add(i+1, d3Remainder);
	    	    		
	    	    		// if we don't increment i, it will get flipped again!
	    	    		i++;
	    	    		
	    				bFlipped = true;
	    				bBounce = true;
	    			}
	    			
	    			dTempPosY += d3Movement.y;
	    		}
	    	}
	
			// If we go out of bounds at the front or back in the overall movement
	    	if ( dNewPosZ > Options.DEPTH - 1 || dNewPosZ < 1 )
	    	{
	    		// There might be multiple vectors now, so we need to keep track of position, and whether we've hit the wall yet or not
	    		double dTempPosZ = dPosZ;
	    		boolean bFlipped = false;
	    		
	    		for ( int i = iMovementIndex; i < m_d3aMovements.size(); i++ )
	    		{
	    			Double3D d3Movement = m_d3aMovements.get(i);
	    			
	
	    			// if we have already hit the wall, we just flip the y axis of all the movements
	    			if ( bFlipped )
	    			{
	    				m_d3aMovements.set(i, new Double3D(d3Movement.x, d3Movement.y, -d3Movement.z));
	    				continue;
	    			}
	    			
	    			// does this sub movement go out of bounds
	    			if ( dTempPosZ + d3Movement.z < 1 || dTempPosZ + d3Movement.z > Options.DEPTH - 1 )
	    			{
	    				// Figure out at which point it goes out
	    				double dCutOff = 1;
	    				if ( dTempPosZ + d3Movement.z < 1 )
	    				{
	    					dCutOff = (1 - dTempPosZ)/d3Movement.z;
	    				}
	    				else
	    				{	
	    					dCutOff = ((Options.DEPTH-1) - dTempPosZ)/d3Movement.z;
	    				}
	    				
	    				// Create 2 new vectors split at the cutoff point, the latter mirrored along the y axis
	    				Double3D d3TruncMovement = d3Movement.multiply(dCutOff);
	    				Double3D d3Remainder = new Double3D( d3Movement.x - d3TruncMovement.x,
	    													 d3Movement.y - d3TruncMovement.y,
	    													-d3Movement.z + d3TruncMovement.z
	    													);
	    	    		
	    				// Replace the current one, then add the new one after it
	    	    		m_d3aMovements.set(i, d3TruncMovement);
	    	    		m_d3aMovements.add(i+1, d3Remainder);
	    	    		
	    	    		// if we don't increment i, it will get flipped again!
	    	    		i++;
	    	    		
	    				bFlipped = true;
	    				bBounce = true;
	    			}
	    			
	    			dTempPosZ += d3Movement.z;
	    		}
	    	}
		}
	}

	List<Int3D> m_i3lCollisionPoints = new ArrayList<Int3D>();
	
	@Override
	public void addCollisionPoint(Int3D i3Point)
	{
		m_i3lCollisionPoints.add(i3Point);
	}

	private boolean m_bCollisionsHandled = false;
	
	@Override
	public void handleCollisions(CollisionGrid cgGrid)
	{
		if ( m_i3lCollisionPoints.size() == 0 )
		{
			return;
		}
		
		// We're using a set because it stores values uniquely!
		HashSet<Collidable> csCollidables = new HashSet<Collidable>();
		
		for ( Int3D i3Point : m_i3lCollisionPoints )
		{
			for ( Collidable cCollidable : cgGrid.getPoints(i3Point) )
			{
				csCollidables.add(cCollidable);
			}
		}
		
		System.out.println(csCollidables.size());
		
		boolean bCollision = false;
		
		for ( Collidable cCell : csCollidables )
		{
			switch ( cCell.getCollisionClass() )
			{
				// These first two are probable hits as they won't be moving
				case STROMA_EDGE:
					if ( collideStromaEdge((StromaEdge) cCell) )
					{
						System.out.println("HIT!");
						handleBounce();
						bCollision = true;
					}
					break;
				case STROMA:
					break;
				case BC:
					break;
			}
		}

		m_i3lCollisionPoints.clear();
		if ( bCollision )
		{
			System.out.println("!TIH");
			registerCollisions(cgGrid);
		}
	}

	@Override
	public CLASS getCollisionClass()
	{
		return CLASS.BC;
	}
	
	public static final double BC_SE_COLLIDE_DIST_SQ = (Options.BC.COLLISION_RADIUS + Options.FDC.STROMA_EDGE_RADIUS) * (Options.BC.COLLISION_RADIUS + Options.FDC.STROMA_EDGE_RADIUS);
	
	private boolean collideStromaEdge(StromaEdge seEdge)
	{
		Double3D p1 = new Double3D(x, y, z);
		Double3D p2 = seEdge.getPoint1();
		Double3D d2 = seEdge.getPoint2().subtract(p2);
		
		for ( int i = 0; i < m_d3aMovements.size(); i++ )
		{
			Double3D d1 = m_d3aMovements.get(i);
			
			double s = 0;
			double t = 0;
			//https://q3k.org/gentoomen/Game%20Development/Programming/Real-Time%20Collision%20Detection.pdf p146

			Double3D r = p1.subtract(p2);

			double a = Vector3DHelper.dotProduct(d1, d1);
			double b = Vector3DHelper.dotProduct(d1, d2);
			double c = Vector3DHelper.dotProduct(d1, r);
			double e = Vector3DHelper.dotProduct(d2, d2);
			double f = Vector3DHelper.dotProduct(d2, r);
			
			// differing from the link, we assume that neither are points (zero length)
			
			double denom = a*e-b*b; // >= 0
			
			// not parallel, so compute closest point and clamp to segment 1
			if ( denom != 0 )
			{
				s = Math.min(1.0, Math.max(0.0, (b*f - c*e) / denom));
			}
			
			t = b*s + f;
			
			if ( t < 0 )
			{
				t = 0;
				s = Math.max(0,  Math.min(1,  -c/a));
			}
			else if ( t > e )
			{
				t = 1;
				s = Math.max(0,  Math.min(1,  (b-c)/a));
			}
			else
			{
				t /= e;
			}

			Double3D c1 = p1.add(d1.multiply(s));
			Double3D c2 = p2.add(d2.multiply(t));
			
			double length = Vector3DHelper.dotProduct(c1.subtract(c2), c1.subtract(c2));
			
			boolean bCollide = false;
			
			if ( BC_SE_COLLIDE_DIST_SQ > length*1.03 )
			{
				bCollide = true;
				double sNew = s;
				while ( BC_SE_COLLIDE_DIST_SQ > length*1.03 && s > 0 && s < 1 )
				{
					s = sNew;
					sNew = Math.max(0, s - 0.5*(BC_SE_COLLIDE_DIST_SQ-length)/d1.length());

					t = b*sNew + f;
					
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
					
					c1 = p1.add(d1.multiply(sNew));
					c2 = p2.add(d2.multiply(t));
					length = Vector3DHelper.dotProduct(c1.subtract(c2), c1.subtract(c2));
				}
			}
			
			//first check if we actually get close
			//second check if the nearest point isn't at the end. If it is, then we've already collided
			if ( bCollide && s > 0 && s < 1 )
			{

				
				//We need to take s back a bit, this is only an estimate because the actual maths is ridiculous
				//s = Math.max(0, s - (BC_SE_COLLIDE_DIST_SQ-length)/d1.length());
				
				Double3D d3NewDir;
				Double3D d3MovementNormal = d1.multiply(-1).normalize();
				
				if ( length == 0 )
				{
					d3NewDir = d3MovementNormal;
				}
				else
				{
					Double3D d3BounceNormal = p1.add(d1.multiply(s)).subtract(p2.add(d2.multiply(t))).normalize();
					
					d3NewDir = Vector3DHelper.rotateVectorToVector(d3MovementNormal, d3MovementNormal, d3BounceNormal);
					d3NewDir = Vector3DHelper.rotateVectorToVector(d3NewDir, d3MovementNormal, d3BounceNormal);
				}
				
				double dCloseness = length / BC_SE_COLLIDE_DIST_SQ;
				
				//d3NewDir = ( d3MovementNormal.multiply(1 - dCloseness).add(d3NewDir.multiply(dCloseness)) ).normalize();

				m_d3aMovements.set(i, d1.multiply(s));
				i++;
				
				double dNewLength = 0;
				while ( m_d3aMovements.size() > i )
				{
					dNewLength += m_d3aMovements.get(i).length();
					m_d3aMovements.remove(i);
				}
				
				dNewLength += d1.length()*(1-s);
				
				m_d3aMovements.add(d3NewDir.multiply(dNewLength));
				
				m_d3aCollisions.add(p1.add(d1.multiply(s)).subtract(new Double3D(x, y, z)));
				
				return true;
			}
			else
			{
				p1 = p1.add(d1);
			}
		}
		
		return false;
	}
	
	ArrayList<Double3D> m_d3aCollisions = new ArrayList<Double3D>();
	
    public TransformGroup getModel(Object obj, TransformGroup transf)
    {
    	// TODO could potentially add lines showing where the cell is about to move to, too!
	    if(transf==null || true)
	    {
	    	transf = new TransformGroup();

	        SpherePortrayal3D s = new SpherePortrayal3D(Options.BC.DRAW_COLOR(), 2, 6);
	        s.setCurrentFieldPortrayal(getCurrentFieldPortrayal());
	        TransformGroup localTG = s.getModel(obj, null);
	        
	        localTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
	        transf.addChild(localTG);
	        
	        if ( m_d3aCollisions.size() > 0 )
	        {
	        	for ( Double3D d3Point : m_d3aCollisions )
	        	{
		        	SpherePortrayal3D s2 = new SpherePortrayal3D(Color.RED, 0.25, 6);
			        s2.setCurrentFieldPortrayal(getCurrentFieldPortrayal());
			        TransformGroup localTG2 = s2.getModel(obj, null);
			        
			        Transform3D tTransform = new Transform3D();
			        tTransform.setTranslation(new Vector3f((float)d3Point.x, (float)d3Point.y, (float)d3Point.z));
			        
			        localTG2.setTransform(tTransform);
			        
			        localTG2.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
			        transf.addChild(localTG2);
	        	}
	        }
	        if ( true )
	        {
		        LineArray lineArr = new LineArray(6, LineArray.COORDINATES);

		        double val = (Options.BC.COLLISION_RADIUS);

	        	lineArr.setCoordinate(0, new Point3d(0, 0, -val));
	        	lineArr.setCoordinate(1, new Point3d(0, 0, val));
	        	lineArr.setCoordinate(2, new Point3d(0, -val, 0));
	        	lineArr.setCoordinate(3, new Point3d(0, val, 0));
	        	lineArr.setCoordinate(4, new Point3d(-val, 0, 0));
	        	lineArr.setCoordinate(5, new Point3d(val, 0, 0));
	        	
	        	Appearance aAppearance = new Appearance();
	        	Color col = Options.BC.DRAW_COLOR();
	        	aAppearance.setColoringAttributes(new ColoringAttributes(col.getRed()/255f, col.getGreen()/255f, col.getBlue()/255f, ColoringAttributes.FASTEST));
	        	
	        	Shape3D s3Shape = new Shape3D(lineArr, aAppearance);
	        	Shape3DPortrayal3D s2 = new Shape3DPortrayal3D(s3Shape, aAppearance);
	        	s2.setCurrentFieldPortrayal(getCurrentFieldPortrayal());
		        TransformGroup localTG2 = s2.getModel(obj, null);
		        
		        localTG2.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		        transf.addChild(localTG2);
		        
	        }
	        
	        /*if ( m_d3aMovementsOld.size() > 0 )
	        {
	        	LineArray lineArr = new LineArray(m_d3aMovementsOld.size()*2, LineArray.COORDINATES);
	        	lineArr.setCoordinate(0, new Point3d(0, 0, 0));
	        	
	        	int i = 1;
	        	double xPos = 0, yPos = 0, zPos = 0;
	        	
	        	for ( int iIndex = m_d3aMovementsOld.size()-1; iIndex >= 0; iIndex-- )
	        	{
	        		Double3D d3Movement = m_d3aMovementsOld.get(iIndex);
	        		
	        		if ( i > 1 )
	        		{
	        			lineArr.setCoordinate(i, new Point3d(xPos, yPos, zPos));
	        			i++;
	        		}
	        		xPos -= d3Movement.x;
	        		yPos -= d3Movement.y;
	        		zPos -= d3Movement.z;
	        		lineArr.setCoordinate(i, new Point3d(xPos, yPos, zPos));
	        		i++;
	        	}
	        	Appearance aAppearance = new Appearance();
	        	Color col = Options.BC.DRAW_COLOR();
	        	aAppearance.setColoringAttributes(new ColoringAttributes(col.getRed()/255f, col.getGreen()/255f, col.getBlue()/255f, ColoringAttributes.FASTEST));
	        	
	        	Shape3D s3Shape = new Shape3D(lineArr, aAppearance);
	        	Shape3DPortrayal3D s2 = new Shape3DPortrayal3D(s3Shape, aAppearance);
	        	s2.setCurrentFieldPortrayal(getCurrentFieldPortrayal());
		        TransformGroup localTG2 = s2.getModel(obj, null);
		        
		        localTG2.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		        transf.addChild(localTG2);
	        }*/
	        
	        if ( m_d3aMovements.size() > 0 )
	        {
	        	LineArray lineArr = new LineArray(m_d3aMovements.size()*2, LineArray.COORDINATES);
	        	lineArr.setCoordinate(0, new Point3d(0, 0, 0));
	        	
	        	int i = 1;
	        	double xPos = 0, yPos = 0, zPos = 0;
	        	
	        	for ( int iIndex = 0; iIndex < m_d3aMovements.size(); iIndex++ )
	        	{
	        		Double3D d3Movement = m_d3aMovements.get(iIndex);
	        		
	        		if ( i > 1 )
	        		{
	        			lineArr.setCoordinate(i, new Point3d(xPos, yPos, zPos));
	        			i++;
	        		}
	        		xPos += d3Movement.x;
	        		yPos += d3Movement.y;
	        		zPos += d3Movement.z;
	        		lineArr.setCoordinate(i, new Point3d(xPos, yPos, zPos));
	        		i++;
	        	}
	        	Appearance aAppearance = new Appearance();
	        	Color col = Color.white;
	        	aAppearance.setColoringAttributes(new ColoringAttributes(col.getRed()/255f, col.getGreen()/255f, col.getBlue()/255f, ColoringAttributes.FASTEST));
	        	
	        	Shape3D s3Shape = new Shape3D(lineArr, aAppearance);
	        	Shape3DPortrayal3D s2 = new Shape3DPortrayal3D(s3Shape, aAppearance);
	        	s2.setCurrentFieldPortrayal(getCurrentFieldPortrayal());
		        TransformGroup localTG2 = s2.getModel(obj, null);
		        
		        localTG2.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		        transf.addChild(localTG2);
	        }
	    }
	    return transf;
    }
}
