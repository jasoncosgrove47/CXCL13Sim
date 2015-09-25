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

    // bleh
    private Double3D d3Face = Vector3DHelper.getRandomDirection();
    
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
	    	vMovement = vMovement.add(new Double3D(1, 0, 0).multiply(aadConcs[2][1][1]-aadConcs[0][1][1]));
	    	// Y
	    	vMovement = vMovement.add(new Double3D(0, 1, 0).multiply(aadConcs[1][2][1]-aadConcs[1][0][1]));
	    	// Z
	    	vMovement = vMovement.add(new Double3D(0, 0, 1).multiply(aadConcs[1][1][2]-aadConcs[1][1][0]));
	    	
	    	if ( vMovement.length() > Options.BC.VECTOR_MIN() )
	    	{
	    		vMovement = vMovement.normalize();
	    	}
	    	else
	    	{
	    		// no data! so do a random turn
	    		vMovement = Vector3DHelper.getBiasedRandomDirectionInCone(d3Face, Math.PI);
	    	}
	    	
	    	// Remember which way we're now facing
	    	d3Face = vMovement;
	    	
	    	x = Math.min(Options.WIDTH-1, Math.max(1, x + vMovement.x*Options.BC.TRAVEL_DISTANCE()));
	    	y = Math.min(Options.HEIGHT-1, Math.max(1, y + vMovement.y*Options.BC.TRAVEL_DISTANCE()));
	    	z = Math.min(Options.DEPTH-1, Math.max(1, z + vMovement.z*Options.BC.TRAVEL_DISTANCE()));
	    	
	    	// TODO better handling of edges
	    	setObjectLocation( new Double3D(x, y, z) );
    	}

    	Particle.add(Particle.TYPE.CCL19, (int)x, (int)y, (int)z, -2 );
    }

    private boolean canMove()
    {
    	// Find neighbours within move distance
    	// TODO this doesn't take into account the z dimension, but it is close enough for now I think
    	// Perhaps it would be better to check if neighbouring cells or cells intersecting movement are full?
    	// ie. get a direction, keep moving until you finish or hit another cell
    	// but this would be that order matters... hmm
    	Bag bagNeighbours = drawEnvironment.getNeighborsWithinDistance( new Double2D(x,y), Options.BC.TRAVEL_DISTANCE() );
    	
    	// TODO my brain hurts and I haven't really thought much about this
    	return Options.RNG.nextDouble() < Math.exp(-Math.pow(Math.max(0, bagNeighbours.size()), 2)/400);
    }
    
    public final void draw(Object object,  final Graphics2D graphics, final DrawInfo2D info)
    {
    	graphics.setColor(getColorWithDepth(Options.BC.DRAW_COLOR()));
    	graphics.fillOval((int)info.draw.x, (int)info.draw.y, (int)info.draw.width, (int)info.draw.height);
    }
}
