package sim3d.cell;
import java.awt.Color;

import sim.field.continuous.Continuous2D;
import sim.portrayal.SimplePortrayal2D;
import sim.util.Double2D;
import sim.util.Double3D;
import sim3d.diffusion.Particle;

public abstract class DrawableCell extends SimplePortrayal2D
{
	private static final long serialVersionUID = 1;
	
	public double x;
	public double y;
	public double z;

	public abstract Continuous2D getDrawEnvironment();
	
	public static int m_iDisplayLevel = 1;
	
	public static void setDisplayLevel(int i)
	{
		m_iDisplayLevel = i;
	}
	
	public final Color getColorWithDepth(Color color)
	{
		if ( z < m_iDisplayLevel || z >= m_iDisplayLevel + 1 )
		{
			return new Color(0,0,0,0);
		}
		
		return color;
	}
	
    public final void setObjectLocation( Double3D location )
    {
	    x = location.x;
	    y = location.y;
	    z = location.z;
	
	    getDrawEnvironment().setObjectLocation( this, new Double2D( x, y ) );
	    
	    Particle.setDisplayLevel((int)z);
		DrawableCell.setDisplayLevel((int)z);
    }
}
