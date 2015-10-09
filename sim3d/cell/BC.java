package sim3d.cell;
import sim.util.*;
import sim.engine.*;
import sim.field.continuous.Continuous2D;

import java.awt.*;

import sim.portrayal.*;
import sim3d.Options;
import sim3d.diffusion.Particle;
import sim3d.util.Vector3DHelper;

public class BC extends DrawableCell implements Steppable
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
    	if ( canMove() )
    	{
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
	    	
	    	// Remember which way we're now facing
	    	m_d3Face = vMovement;
	    	

	    	x += vMovement.x*Options.BC.TRAVEL_DISTANCE();
	    	y += vMovement.y*Options.BC.TRAVEL_DISTANCE();
	    	z += vMovement.z*Options.BC.TRAVEL_DISTANCE();
	    	
	    	// Handle bounces
	    	if ( x > Options.WIDTH - 1 )
	    	{
	    		x = Math.max(1, 2*(Options.WIDTH - 1) - x);
	    		m_d3Face = new Double3D(-m_d3Face.x, m_d3Face.y, m_d3Face.z);
	    	}
	    	else if ( x < 1 )
	    	{
	    		x = Math.min(Options.WIDTH-2, 2 - x);
	    		m_d3Face = new Double3D(-m_d3Face.x, m_d3Face.y, m_d3Face.z);
	    	}
	    	
	    	if ( y > Options.HEIGHT - 1 )
	    	{
	    		y = Math.max(1, 2*(Options.HEIGHT - 1) - y);
	    		m_d3Face = new Double3D(m_d3Face.x, -m_d3Face.y, m_d3Face.z);
	    	}
	    	else if ( y < 1 )
	    	{
	    		y = Math.min(Options.HEIGHT-2, 2 - y);
	    		m_d3Face = new Double3D(m_d3Face.x, -m_d3Face.y, m_d3Face.z);
	    	}
	    	
	    	if ( z > Options.DEPTH - 1 )
	    	{
	    		z = Math.max(1, 2*(Options.DEPTH - 1) - z);
	    		m_d3Face = new Double3D(m_d3Face.x, m_d3Face.y, -m_d3Face.z);
	    	}
	    	else if ( z < 1 )
	    	{
	    		z = Math.min(Options.DEPTH-2, 2 - z);
	    		m_d3Face = new Double3D(m_d3Face.x, m_d3Face.y, -m_d3Face.z);
	    	}
	    	
	    	setObjectLocation( new Double3D(x, y, z) );
    	}

    	receptorStep();
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
    	graphics.fillOval((int)Math.round(info.draw.x), (int)Math.round(info.draw.y), (int)Math.round(info.draw.width), (int)Math.round(info.draw.height));
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
    		iaBoundReceptors[i] = (int)Options.BC.ODE.K_a()*iReceptors*iaConcs[i];
			m_iR_free -= iaBoundReceptors[i];
			m_iL_r  += iaBoundReceptors[i];
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
    
    private void receptorStep()
    {
    	int iTimesteps = 10;
    	int iR_i, iR_d, iL_r;
    	for ( int i = 0; i < iTimesteps; i++ )
    	{
    		iR_i = m_iR_i;
    		iR_d = m_iR_d;
    		iL_r = m_iL_r;
    		
    		m_iR_free	+= (1/iTimesteps)*Options.BC.ODE.K_r() * iR_i;
    		m_iR_i		+= (1/iTimesteps)*Options.BC.ODE.K_r() * iR_d
    					 - (1/iTimesteps)*Options.BC.ODE.K_i() * iR_i;
    		m_iR_d		+= (1/iTimesteps)*iL_r*iL_r/(Options.BC.ODE.gamma()*(1 + Math.pow(iL_r/Options.BC.ODE.delta(), 2) + iL_r))
    				     - (1/iTimesteps)*Options.BC.ODE.K_r() * iR_d;
    		m_iL_r		-= (1/iTimesteps)*iL_r*iL_r/(Options.BC.ODE.gamma()*(1 + Math.pow(iL_r/Options.BC.ODE.delta(), 2) + iL_r));
    	}
    }
}
