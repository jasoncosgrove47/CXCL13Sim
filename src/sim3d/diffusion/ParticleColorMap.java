package sim3d.diffusion;
import java.awt.Color;

import sim.util.gui.ColorMap;

public class ParticleColorMap implements ColorMap {

	@Override
	public double defaultValue() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getAlpha(double arg0) {
		// TODO Auto-generated method stub
		return (int)(arg0 * 255);
	}

	@Override
	public Color getColor(double arg0) {
		// TODO Auto-generated method stub
		return new Color(255,0,0,(int)(arg0*255));
	}

	public static int hsvToRgb(double hue, double saturation, double value) {

	    int h = (int)(hue * 6) % 6;
	    double f = hue * 6 - h;
	    double p = value * (1 - saturation);
	    double q = value * (1 - f * saturation);
	    double t = value * (1 - (1 - f) * saturation);

	    switch (h) {
	      case 0: return (int)(255*value) << 16 | (int)(255*t) << 8 | (int)(255*p);
	      case 1: return (int)(255*q) << 16 | (int)(255*value) << 8 | (int)(255*p);
	      case 2: return (int)(255*p) << 16 | (int)(255*value) << 8 | (int)(255*t);
	      case 3: return (int)(255*p) << 16 | (int)(255*q) << 8 | (int)(255*value);
	      case 4: return (int)(255*t) << 16 | (int)(255*p) << 8 | (int)(255*value);
	      case 5: return (int)(255*value) << 16 | (int)(255*p) << 8 | (int)(255*q);
	      default: throw new RuntimeException("Something went wrong when converting from HSV to RGB. Input was " + hue + ", " + saturation + ", " + value);
	    }
	}
	
	@Override
	public int getRGB(double arg0) {
		// TODO Auto-generated method stub
		arg0 = Math.log(arg0*20+1)/2;
		if ( arg0 > 2 )
		{
			return ( Color.HSBtoRGB((float)((arg0-2)/50 + 0.66666), 1, 1) ^ 255 << 24 ) | 255 << 24;
		}
		else
		{
			return 0 << 16 | 0 << 8 | 255 | (int)(arg0*127.5) << 24;
		}
	}

	@Override
	public boolean validLevel(double arg0) {
		// TODO Auto-generated method stub
		return true;
	}

}
