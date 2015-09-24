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
        for(int x=0;x<Options.FDC.COUNT;x++)
        {
            Double2D loc = new Double2D(random.nextInt(Options.WIDTH-2)+1, random.nextInt(Options.HEIGHT-2)+1);
            FDC frc = new FDC();
            
            frc.setObjectLocation(loc);
            
            schedule.scheduleRepeating(frc);
        }
        for(int x=0;x<Options.BC.COUNT;x++)
        {
            Double2D loc = new Double2D(random.nextInt(Options.WIDTH-2)+1, random.nextInt(Options.HEIGHT-2)+1);
            BC bc = new BC();
            
            bc.setObjectLocation(loc);
            
            schedule.scheduleRepeating(bc);
        }

        // add particles
        new Particle(schedule, Particle.TYPE.CCL19, Options.WIDTH, Options.HEIGHT);
        new Particle(schedule, Particle.TYPE.CCL21, Options.WIDTH, Options.HEIGHT);
        new Particle(schedule, Particle.TYPE.CXCL13, Options.WIDTH, Options.HEIGHT);
        new Particle(schedule, Particle.TYPE.EBI2L, Options.WIDTH, Options.HEIGHT);
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
