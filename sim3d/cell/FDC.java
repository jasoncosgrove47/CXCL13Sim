package sim3d.cell;
/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

//package sim.app.woims;

import sim.engine.*;
import sim.field.continuous.Continuous2D;

import java.awt.*;
import sim.portrayal.*;
import sim.util.Double3D;
import sim3d.Options;
import sim3d.collisiondetection.Collidable;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.diffusion.Particle;

public class FDC extends DrawableCell implements Steppable, Collidable
{
	private static final long serialVersionUID = 1;
	
	/* Draw Environment accessor */
	public static Continuous2D drawEnvironment;
	@Override public Continuous2D getDrawEnvironment(){
		return drawEnvironment;
	}

    public void step( final SimState state )
    {
    	Particle.add(Particle.TYPE.CXCL13, (int)x, (int)y, (int)z, Options.FDC.CXCL13_EMITTED() );
    }

    public final void draw(Object object,  final Graphics2D graphics, final DrawInfo2D info)
    {
        /*double radius[] = {4,5,7,9,10,9,7,6,8,9,8,6,5};
        int nPoints = 26;
        int[] X = new int[nPoints];
        int[] Y = new int[nPoints];
        
        for (double current=0.0; current<nPoints; current++)
        {
            int i = (int) current;
            double offsetX = Options.FRC.DRAW_SCALE() * info.draw.width * Math.cos(current*((2*Math.PI)/nPoints))*radius[i % 13];
            double offsetY = Options.FRC.DRAW_SCALE() * info.draw.height * Math.sin(current*((2*Math.PI)/nPoints))*radius[i % 13];

            X[i] = (int)(info.draw.x + offsetX);
            Y[i] = (int)(info.draw.y + offsetY);
        }

        graphics.fillPolygon(X, Y, nPoints);*/

    	graphics.setColor(getColorWithDepth(Options.FDC.DRAW_COLOR()));
    	graphics.fillOval((int)Math.round(info.draw.x-info.draw.width/2), (int)Math.round(info.draw.y-info.draw.height/2), (int)Math.round(info.draw.width), (int)Math.round(info.draw.height));
    }


	@Override
	public boolean isStatic() 
	{
		return true;
	}

	@Override
	public void addCollisions(CollisionGrid cgGrid)
	{
		cgGrid.addSphereToGrid(this, new Double3D(x, y, z), Options.FDC.STROMA_NODE_RADIUS);
	}
}
