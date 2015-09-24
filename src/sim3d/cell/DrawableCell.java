package sim3d.cell;
import java.awt.Color;
import java.awt.Graphics2D;

import sim.field.continuous.Continuous2D;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.SimplePortrayal2D;
import sim.util.Double2D;
import sim.util.Double3D;
import sim3d.Options;

public abstract class DrawableCell extends SimplePortrayal2D
{
	private static final long serialVersionUID = 1;
	
	public double x;
	public double y;
	public double z;

	public abstract Continuous2D getDrawEnvironment();
	
    public final void setObjectLocation( Double3D location )
    {
	    x = (((location.x) + Options.WIDTH) % Options.WIDTH);
	    y = (((location.y) + Options.HEIGHT) % Options.HEIGHT);
	    z = (((location.z) + Options.DEPTH) % Options.DEPTH);
	
	    getDrawEnvironment().setObjectLocation( this, new Double2D( x, y ) );
    }
    
    public final DrawInfo2D get3DDrawInfo(DrawInfo2D info, Graphics2D graphics, Color color)
    {
    	info.draw.x = info.draw.x + z*(Options.DEPTH/10);
    	info.draw.y = info.draw.y - z*(Options.DEPTH/10);
    	info.draw.height *= (1+z/Options.DEPTH);
    	info.draw.width *= (1+z/Options.DEPTH);
    	
    	graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(255-z/Options.DEPTH*255)));
    	return info;
    }
}
