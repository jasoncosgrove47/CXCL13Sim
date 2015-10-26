package sim3d.cell;
import sim.util.*;
import sim.engine.*;
import sim.field.continuous.Continuous2D;

import java.awt.Color;
import java.awt.Graphics2D;
//import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import sim.portrayal.*;
import sim3d.Grapher;
import sim3d.Options;
import sim3d.collisiondetection.Collidable;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.diffusion.Particle;
import sim3d.util.Vector3DHelper;

public class BC extends DrawableCell implements Steppable, Collidable
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
	public static Continuous2D drawEnvironment;
	@Override public Continuous2D getDrawEnvironment(){
		return drawEnvironment;
	}
	
    public BC() 
    {
    }
    
    public void step( final SimState state )
    {
    	if ( m_d3aMovements != null )
    	{
    		for ( Double3D d3Movement : m_d3aMovements )
    		{
    			x += d3Movement.x;
    			y += d3Movement.y;
    			z += d3Movement.z;

    	    	// Remember which way we're now facing
    			m_d3Face = d3Movement;
    		}

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
    	
    	m_d3aMovements.clear();
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

    /**
     * Determine whether or not a cell can move
     * @return
     */
    private boolean canMove()
    {
    	// Find neighbours within move distance
    	// TODO this doesn't take into account the z dimension, but it is close enough for now I think
    	// Perhaps it would be better to check if neighbouring cells or cells intersecting movement are full?
    	// ie. get a direction, keep moving until you finish or hit another cell
    	// but this would be that order matters... hmm
    	//Bag bagNeighbours = drawEnvironment.getNeighborsWithinDistance( new Double2D(x,y), Options.BC.TRAVEL_DISTANCE() );
    	
    	// TODO my brain hurts and I haven't really thought much about this
    	//m_bCantMove = Options.RNG.nextDouble() > Math.exp(-Math.pow(Math.max(0, bagNeighbours.size()), 2)/400);
    	return true;
    }
    
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
    		Grapher.dataSets.get(0).add(m_iR_free);
    		Grapher.dataSets.get(1).add(m_iR_i);
    		Grapher.dataSets.get(2).add(m_iR_d);
    		Grapher.dataSets.get(3).add(m_iL_r);
    	}
    	if ( displayGraph && Grapher.dataSets.get(0).size() % 50 == 49 )
    	{
    		Grapher.updateGraph();
    	}
    }

    List<Double3D> m_d3aMovements = new ArrayList<Double3D>();
    
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
	 	 * TODO zero division argh!
		 */
		
		double dPosX = x;
		double dPosY = y;
		double dPosZ = z;

		// We should in theory only have to check the last step for bounces
		int iMovementIndex = m_d3aMovements.size()-1;

		// add all movement vectors before the last one
		for ( int i = 0; i < iMovementIndex; i++ )
		{
			dPosX += m_d3aMovements.get(i).x;
			dPosY += m_d3aMovements.get(i).y;
			dPosZ += m_d3aMovements.get(i).z;
		}

		Double3D d3Movement = m_d3aMovements.get(iMovementIndex);
		
		double dNewPosX = dPosX + d3Movement.x;
		double dNewPosY = dPosY + d3Movement.y;
		double dNewPosZ = dPosZ + d3Movement.z;
		
		// If we go out of bounds on either side
		if ( dNewPosX > Options.WIDTH - 1 || dNewPosX < 1 )
    	{
			// figure out how far down we go before we hit the edge
			double dCutOff = 1;
			if ( dNewPosX < 1 )
			{
				dCutOff = (dPosX - 1)/d3Movement.x;
			}
			else
			{
				dCutOff = ((Options.WIDTH-1) - dPosX)/d3Movement.x;
			}
			
			// Create 2 new vectors split at the cutoff point, the latter mirrored along the x axis
			Double3D d3TruncMovement = d3Movement.multiply(dCutOff);
			Double3D d3Remainder = new Double3D(-d3Movement.x + d3TruncMovement.x,
												 d3Movement.y - d3TruncMovement.y,
												 d3Movement.z - d3TruncMovement.z
												);
    		
			// Replace the current one, then add the new one after it
    		m_d3aMovements.set(iMovementIndex, d3TruncMovement);
    		m_d3aMovements.add(d3Remainder);
    	}
    	
		// If we go out of bounds at the top or bottom in the overall movement
    	if ( dNewPosY > Options.HEIGHT - 1 || dNewPosY < 1 )
    	{
    		// There might be multiple vectors now, so we need to keep track of position, and whether we've hit the wall yet or not
    		double dTempPosY = dPosY;
    		boolean bFlipped = false;
    		
    		for ( int i = iMovementIndex; i < m_d3aMovements.size(); i++ )
    		{
    			d3Movement = m_d3aMovements.get(i);
    			
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
    					dCutOff = (dTempPosY - 1)/d3Movement.y;
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
    			d3Movement = m_d3aMovements.get(i);
    			

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
    					dCutOff = (dTempPosZ - 1)/d3Movement.z;
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
    			}
    			
    			dTempPosZ += d3Movement.z;
    		}
    	}
	}

	List<Int3D> m_i3lCollisionPoints = new ArrayList<Int3D>();
	
	@Override
	public void addCollisionPoint(Int3D i3Point)
	{
		m_i3lCollisionPoints.add(i3Point);
		m_bCollisionsHandled = false;
	}

	private boolean m_bCollisionsHandled = false;
	
	@Override
	public void handleCollisions(CollisionGrid cgGrid)
	{
		if ( m_bCollisionsHandled )
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
		
		double dCurrentCollisionPoint = 0;
		
		forloop:
		for ( Collidable cCell : csCollidables )
		{
			switch ( cCell.getCollisionClass() )
			{
				// These first two are probable hits as they won't be moving
				case STROMA_EDGE:
					dCurrentCollisionPoint = collideStromaEdge((StromaEdge) cCell, dCurrentCollisionPoint);
					break;
				case STROMA:
					break;
				case BC:
					break;
			}
		}
		
		m_bCollisionsHandled = true;
		m_i3lCollisionPoints.clear();
	}

	@Override
	public CLASS getCollisionClass()
	{
		return CLASS.BC;
	}
	
	private double collideStromaEdge(StromaEdge seEdge, double dCurrentCollisionPoint)
	{
		Double3D d3Point1 = seEdge.getPoint1();
		Double3D d3Point2 = seEdge.getPoint2();
		
		Double3D d3CurPos = new Double3D(x, y, z);
		
		for ( Double3D d3Movement: m_d3aMovements )
		{
			double s = 0;
			double t = 0;
			//https://q3k.org/gentoomen/Game%20Development/Programming/Real-Time%20Collision%20Detection.pdf p146
			// Check if we are actually going towards either point
			Double3D d1 = d3Movement;
			Double3D d2 = d3Point2.subtract(d3Point1);
			Double3D r = d3CurPos.subtract(d3Point1);

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
		}
		
		return dCurrentCollisionPoint;
	}
}
