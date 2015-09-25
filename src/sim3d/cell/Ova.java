package sim3d.cell;
import sim.util.*;
import sim3d.Options;
import sim3d.Options.OVA;
import sim3d.diffusion.Particle;
import sim3d.util.Vector3DHelper;
import sim.engine.*;
import sim.field.continuous.Continuous2D;

import java.awt.*;
import sim.portrayal.*;

public class Ova extends DrawableCell implements Steppable
{
	private static final long serialVersionUID = 1;

	/* Draw Environment accessor */
	public static Continuous2D drawEnvironment;
	@Override public Continuous2D getDrawEnvironment(){
		return drawEnvironment;
	}
	
    public Ova() 
    {
    }
    
    Double3D d3Face = Vector3DHelper.getRandomDirection();

    public void step( final SimState state )
    {
    	Double3D vMovement = Vector3DHelper.getBiasedRandomDirectionInCone(d3Face, Math.PI);
    	
    	x = Math.min(Options.WIDTH-1, Math.max(1, x + vMovement.x*Options.OVA.TRAVEL_DISTANCE()));
    	y = Math.min(Options.HEIGHT-1, Math.max(1, y + vMovement.y*Options.OVA.TRAVEL_DISTANCE()));
    	z = Math.min(Options.DEPTH-1, Math.max(1, z + vMovement.z*Options.OVA.TRAVEL_DISTANCE()));
    	
    	setObjectLocation(new Double3D(x, y, z));
    	

    	Particle.add(Particle.TYPE.CCL19, (int)x, (int)y, (int)z, -2 );
    }

    public final void draw(Object object,  final Graphics2D graphics, DrawInfo2D info)
    {
        /*double radius[] = {3,3,3,6};
        int nPoints = 16;
        int[] X = new int[nPoints];
        int[] Y = new int[nPoints];
        
        for (double current=0.0; current<nPoints; current++)
        {
            int i = (int) current;
            double offsetX = Options.OVA.DRAW_SCALE() * info.draw.width * Math.cos(current*((2*Math.PI)/nPoints))*radius[i % 4];
            double offsetY = Options.OVA.DRAW_SCALE() * info.draw.height * Math.sin(current*((2*Math.PI)/nPoints))*radius[i % 4];

            X[i] = (int)(info.draw.x + offsetX);
            Y[i] = (int)(info.draw.y + offsetY);
        }
        graphics.setColor(Options.OVA.DRAW_COLOR());
        graphics.fillPolygon(X, Y, nPoints); */
    	//info = get3DDrawInfo(info, graphics, OVA.DRAW_COLOR());
    	graphics.setColor(getColorWithDepth(Options.BC.DRAW_COLOR()));
    	graphics.fillRect((int)info.draw.x, (int)info.draw.y, (int)info.draw.width, (int)info.draw.height);
    }
}
