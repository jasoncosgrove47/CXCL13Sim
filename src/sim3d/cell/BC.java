package sim3d.cell;
import sim.util.*;
import sim3d.Options;
import sim3d.diffusion.Particle;
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
	
	private double posX;
	private double posY;
	
    public BC() 
    {
    }

    // bleh
    private Double3D d2Face = new Double3D(1,0,0).rotate(Options.RNG.nextDouble()*Math.PI*2);
    
    public void step( final SimState state )
    {
    	if ( canMove() )
    	{
	    	double[][][] aadConcs = Particle.get(Particle.TYPE.CXCL13, (int)x, (int)y, (int)z);
	    	
	    	//add some noise
	    	for ( int x = 0; x < 3; x++ )
	    	{
	    		for ( int y = 0; y < 3; y++ )
	    		{
		    		for ( int z = 0; z < 3; z++ )
		    		{
		    			aadConcs[x][y][z] *= 1 - Options.RNG.nextDouble()*Options.BC.RECEPTOR_NOISE();
		    			aadConcs[x][y][z] = Math.min(aadConcs[x][y][z], Options.BC.RECEPTOR_MAX());
		    		}
	    		}
	    	}
	    	
	    	Double3D vMovement = new Double3D();
	
	    	// calculate direction vector
	    	//vMovement = vMovement.add(new Double2D(-1,-1).multiply(aadConcs[0][0]-aadConcs[2][2]));
	    	vMovement = vMovement.add(new Double3D(0,-Math.sqrt(2)).multiply(aadConcs[1][0][1]-aadConcs[1][2][1]));
	    	//vMovement = vMovement.add(new Double2D(1,-1).multiply(aadConcs[2][0]-aadConcs[0][2]));
	    	vMovement = vMovement.add(new Double3D(-Math.sqrt(2),0).multiply(aadConcs[0][1][1]-aadConcs[2][1][1]));
	    	
	    	// sqrt2 because the magnitude of the directional vectors is sqrt2
	    	if ( vMovement.length() > Math.sqrt(2) * Options.BC.VECTOR_MIN() )
	    	{
	    		vMovement = vMovement.normalize();
	    	}
	    	else
	    	{
	    		// no data! so do a random turn
	    		vMovement = d2Face.rotate(Options.RNG.nextDouble()*Math.PI - Math.PI/2);
	    	}
	    	
	    	d2Face = vMovement;
	    	
	    	posX = x + vMovement.x*Options.BC.TRAVEL_DISTANCE();
	    	posY = y + vMovement.y*Options.BC.TRAVEL_DISTANCE();
	    	posZ = z + vMovement.z*Options.BC.TRAVEL_DISTANCE();
	    	
	    	setObjectLocation( new Double2D(posX, posY, posZ));
    	}

    	Particle.add(Particle.TYPE.CCL19, (int)x, (int)y, -2 );
    }

    private boolean canMove()
    {
    	// Find neighbours within move distance
    	Bag bagNeighbours = drawEnvironment.getNeighborsWithinDistance( new Double2D(x,y), Options.BC.TRAVEL_DISTANCE() );
    	
    	// Separate the neighbours from the ones on the same spot
    	// TODO is there any point in doing this since I'm not using discrete space?
    	// Perhaps a better way would be to somehow limit movement in specific
    	int iNeighbours = bagNeighbours.size();
    	int iSameLoc = 0;
    	for ( int i = 0; i < iNeighbours; i++ )
    	{
    		BC bcNeighbour = (BC)bagNeighbours.get(i);
    		if ( bcNeighbour.x == x && bcNeighbour.y == y )
    		{
    			iSameLoc++;
    		}
    	}
    	iNeighbours -= iSameLoc;
    	
    	return Options.RNG.nextDouble() < Math.exp(-Math.pow(2, Math.max(0, iNeighbours/64-iSameLoc/8)));
    }
    
    public final void draw(Object object,  final Graphics2D graphics, final DrawInfo2D info)
    {
        get3DDrawInfo(info, graphics, Options.BC.DRAW_COLOR());
    	graphics.fillOval((int)info.draw.x, (int)info.draw.y, (int)info.draw.width, (int)info.draw.height);
    }
}
