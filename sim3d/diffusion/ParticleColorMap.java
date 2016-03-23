package sim3d.diffusion;

import java.awt.Color;

import sim.util.gui.ColorMap;

/**
 * A colour map that will go from black to blue, and then start to change hue.
 * Looks pretty nice
 * 
 * TODO get rid of this class
 * 
 * @author Simon Jarrett - {@link simonjjarrett@gmail.com}
 */
public class ParticleColorMap implements ColorMap
{
	
	@Override
	public double defaultValue()
	{
		return 0;
	}
	
	@Override
	public int getAlpha( double arg0 )
	{
		return (int) (arg0 * 255);
	}
	
	@Override
	public Color getColor( double arg0 )
	{
		return new Color( getRGB( arg0 ) );
	}
	
	/**
	 * Helper function to convert HSV to RGB
	 */
	public static int hsvToRgb( double hue, double saturation, double value )
	{
		
		int h = (int) (hue * 6) % 6;
		double f = hue * 6 - h;
		double p = value * (1 - saturation);
		double q = value * (1 - f * saturation);
		double t = value * (1 - (1 - f) * saturation);
		
		switch ( h )
		{
			case 0:
				return (int) (255 * value) << 16 | (int) (255 * t) << 8 | (int) (255 * p);
			case 1:
				return (int) (255 * q) << 16 | (int) (255 * value) << 8 | (int) (255 * p);
			case 2:
				return (int) (255 * p) << 16 | (int) (255 * value) << 8 | (int) (255 * t);
			case 3:
				return (int) (255 * p) << 16 | (int) (255 * q) << 8 | (int) (255 * value);
			case 4:
				return (int) (255 * t) << 16 | (int) (255 * p) << 8 | (int) (255 * value);
			case 5:
				return (int) (255 * value) << 16 | (int) (255 * p) << 8 | (int) (255 * q);
			default:
				throw new RuntimeException( "Something went wrong when converting from HSV to RGB. Input was " + hue
						+ ", " + saturation + ", " + value );
		}
	}
	
	@Override
	public int getRGB( double arg0 )
	{
		// arg0 = Math.log(arg0*2000)/Math.log(2);
		if ( arg0 > 400 )
		{
			return (Color.HSBtoRGB( (float) ((arg0 - 400) / 400 + 0.66666), 1, 1 ) ^ 255 << 24) | 255 << 24;
		}
		else
		{
			return 0 << 16 | 0 << 8 | (int) (arg0 / 400 * 255.0) | 255 << 24;
		}
	}
	
	@Override
	public boolean validLevel( double arg0 )
	{
		return true;
	}
	
}
