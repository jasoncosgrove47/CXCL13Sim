package sim3d;
/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

//package sim.app.woims;


import sim.engine.*;
import sim.util.*;
import sim3d.cell.BC;
import sim3d.cell.FDC;
import sim3d.cell.Ova;
import sim3d.diffusion.Particle;
import sim.field.continuous.*;

public class Demo extends SimState
{
    private static final long serialVersionUID = 1;

    public Continuous2D ovaEnvironment;
    public Continuous2D frcEnvironment;
    public Continuous2D bcEnvironment;
    
    public int getDisplayLevel() {
		return Particle.getDisplayLevel()+1;
	}

	public void setDisplayLevel(int m_iDisplayLevel) {
		Particle.setDisplayLevel(m_iDisplayLevel-1);
	}
	
	public Object domDisplayLevel() { return new sim.util.Interval(1, Options.DEPTH); }

	public Demo(long seed)
    {
        super(seed);
        
        Options.RNG = random;
    }
        
    public void start()
    {
        super.start();  // clear out the schedule

        ovaEnvironment = new Continuous2D( Options.OVA.DISCRETISATION, Options.WIDTH, Options.HEIGHT );
        Ova.drawEnvironment = ovaEnvironment;

        frcEnvironment = new Continuous2D( Options.FDC.DISCRETISATION, Options.WIDTH, Options.HEIGHT );
        FDC.drawEnvironment = frcEnvironment;

        bcEnvironment = new Continuous2D( Options.BC.DISCRETISATION, Options.WIDTH, Options.HEIGHT );
        BC.drawEnvironment = bcEnvironment;
        
        /*for(int x=0;x<Options.OVA.COUNT;x++)
        {
            Double2D loc = new Double2D(random.nextInt(Options.WIDTH), random.nextInt(Options.HEIGHT));
            Ova ova = new Ova();
            
            ova.setObjectLocation(loc);
            
            schedule.scheduleRepeating(ova);
        }*/
        double xPos = 1, yPos = 1, zPos = 1;
        outerloop:
        for(int x=0;x<Options.FDC.COUNT;x++)
        {
        	xPos = (Options.WIDTH-2+xPos+Options.RNG.nextInt(3)-1)%(Options.WIDTH-2)+1;
        	yPos = (Options.HEIGHT-2+yPos+Options.RNG.nextInt(3)-1)%(Options.HEIGHT-2)+1;
        	zPos = (Options.DEPTH-2+zPos+Options.RNG.nextInt(3)-1)%(Options.DEPTH-2)+1;
        	
        	for( Object t : FDC.drawEnvironment.allObjects)
        	{
        		FDC a = (FDC)t;
        		if ( a.x == xPos && a.y == yPos && a.z == zPos)
        		{
        			xPos = a.x;
        			yPos = a.y;
        			zPos = a.z;
        			x--;
        			continue outerloop;
        		}
        	}
        	
            Double3D loc = new Double3D(xPos, yPos, zPos);
            FDC fdc = new FDC();
            
            fdc.setObjectLocation(loc);
            
            schedule.scheduleRepeating(fdc);
        }
        for(int x=0;x<Options.BC.COUNT;x++)
        {
            Double3D loc = new Double3D(random.nextInt(Options.WIDTH-2)+1, random.nextInt(Options.HEIGHT-2)+1, random.nextInt(Options.DEPTH-2)+1);
            BC bc = new BC();
            
            bc.setObjectLocation(loc);
            
            schedule.scheduleRepeating(bc);
        }

        // add particles
        new Particle(schedule, Particle.TYPE.CCL19, Options.WIDTH, Options.HEIGHT, Options.DEPTH);
        new Particle(schedule, Particle.TYPE.CCL21, Options.WIDTH, Options.HEIGHT, Options.DEPTH);
        new Particle(schedule, Particle.TYPE.CXCL13, Options.WIDTH, Options.HEIGHT, Options.DEPTH);
        new Particle(schedule, Particle.TYPE.EBI2L, Options.WIDTH, Options.HEIGHT, Options.DEPTH);
    }
    
    public void finish()
    {
    	Particle.reset();
    }

    public static void main2(String[] args)
    {
        doLoop(Demo.class, args);
        System.exit(0);
    }    
}
