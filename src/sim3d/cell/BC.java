package sim3d.cell;
import sim.util.*;
import sim3d.Options;
import sim3d.diffusion.Particle;
import sim3d.util.Vector3DHelper;
import sim.engine.*;
import sim.field.continuous.Continuous2D;

import java.awt.*;
import sim.portrayal.*;

public class BC extends DrawableCell implements Steppable
{
	private static final long serialVersionUID = 1;

	/* Draw Environment accessor */
	public static Continuous2D drawEnvironment;
	@Override public Continuous2D getDrawEnvironment(){
		return drawEnvironment;
	}
	
    public BC() 
    {
    }

    private boolean m_bRandom = true;
    
    private Double3D m_d3Face = Vector3DHelper.getRandomDirection();
    
    private double applyCarrying(double dValue)
    {
    	return dValue * Options.BC.CONC_CARRYING() / ( Options.BC.CONC_CARRYING() + dValue );
    }
    
    public void step( final SimState state )
    {
    	if ( canMove() )
    	{
	    	double[][][] aadConcs = Particle.get(Particle.TYPE.CXCL13, (int)x, (int)y, (int)z);
	    	
	    	
	    	// TODO consider how to best add noise, this or the cone thing
	    	/*for ( int x = 0; x < 3; x++ )
	    	{
	    		for ( int y = 0; y < 3; y++ )
	    		{
		    		for ( int z = 0; z < 3; z++ )
		    		{
		    			aadConcs[x][y][z] *= 1 - Options.RNG.nextDouble()*Options.BC.RECEPTOR_NOISE();
		    			aadConcs[x][y][z] = Math.min(aadConcs[x][y][z], Options.BC.RECEPTOR_MAX());
		    		}
	    		}
	    	}/**/
	    	
	    	Double3D vMovement = new Double3D();
	
	    	// X
	    	vMovement = vMovement.add(new Double3D(1, 0, 0).multiply(applyCarrying(aadConcs[2][1][1])-applyCarrying(aadConcs[0][1][1]))).multiply(1-Options.RNG.nextDouble()*Options.BC.VECTOR_NOISE());
	    	// Y
	    	vMovement = vMovement.add(new Double3D(0, 1, 0).multiply(applyCarrying(aadConcs[1][2][1])-applyCarrying(aadConcs[1][0][1]))).multiply(1-Options.RNG.nextDouble()*Options.BC.VECTOR_NOISE());
	    	// Z
	    	vMovement = vMovement.add(new Double3D(0, 0, 1).multiply(applyCarrying(aadConcs[1][1][2])-applyCarrying(aadConcs[1][1][0]))).multiply(1-Options.RNG.nextDouble()*Options.BC.VECTOR_NOISE());
	    	
	    	if ( vMovement.length() > Options.BC.VECTOR_MIN() )
	    	{
	    		m_bRandom = false;
	    		vMovement = Vector3DHelper.getBiasedRandomDirectionInCone(vMovement.normalize(), Options.BC.DIRECTION_ERROR());
	    	}
	    	else
	    	{
	    		m_bRandom = true;
	    		// no data! so do a random turn
	    		vMovement = Vector3DHelper.getBiasedRandomDirectionInCone(m_d3Face, Options.BC.RANDOM_TURN_ANGLE());
	    	}
	    	
	    	// Remember which way we're now facing
	    	m_d3Face = vMovement;
	    	
	    	x = Math.min(Options.WIDTH-2, Math.max(1, x + vMovement.x*Options.BC.TRAVEL_DISTANCE()));
	    	y = Math.min(Options.HEIGHT-2, Math.max(1, y + vMovement.y*Options.BC.TRAVEL_DISTANCE()));
	    	z = Math.min(Options.DEPTH-2, Math.max(1, z + vMovement.z*Options.BC.TRAVEL_DISTANCE()));
	    	
	    	// TODO better handling of edges
	    	setObjectLocation( new Double3D(x, y, z) );
    	}

    	//Particle.add(Particle.TYPE.CCL19, (int)x, (int)y, (int)z, -1 );
    }

    private boolean m_bCantMove = false;
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
}
