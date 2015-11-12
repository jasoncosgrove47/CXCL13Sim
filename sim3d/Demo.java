package sim3d;

import java.util.ArrayList;

import javax.media.j3d.LineArray;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;

import javafx.geometry.Point3D;
import sim.display3d.Display3D;
import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;
import sim3d.cell.*;
import sim3d.collisiondetection.CollisionGrid;
import sim3d.diffusion.Particle;
import sim3d.util.FRCStromaGenerator;


//code needs much more comments!

public class Demo extends SimState
{
    private static final long serialVersionUID = 1;

    public Continuous3D frcEnvironment;
    public Continuous3D bcEnvironment;
    
    public static Display3D display3D;
    
    public int getDisplayLevel() {
		return Particle.getDisplayLevel()+1;
	}

	public void setDisplayLevel(int m_iDisplayLevel) {
		Particle.setDisplayLevel(m_iDisplayLevel-1);
		DrawableCell.setDisplayLevel(m_iDisplayLevel-1);
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

        frcEnvironment = new Continuous3D( Options.FDC.DISCRETISATION, Options.WIDTH, Options.HEIGHT, Options.DEPTH );
        FDC.drawEnvironment = frcEnvironment;
        StromaEdge.drawEnvironment = frcEnvironment;

        bcEnvironment = new Continuous3D( Options.BC.DISCRETISATION, Options.WIDTH, Options.HEIGHT, Options.DEPTH );
        BC.drawEnvironment = bcEnvironment;
        
        CollisionGrid cgGrid = new CollisionGrid(Options.WIDTH, Options.HEIGHT, Options.DEPTH, 1);
        schedule.scheduleRepeating(cgGrid, 3, 1);
        
        ArrayList<StromaEdge> sealEdges = new ArrayList<StromaEdge>(); 
        boolean[][][] ba3CellLocs = FRCStromaGenerator.generateStroma3D(Options.WIDTH-2, Options.HEIGHT-2, Options.DEPTH-2, Options.FDC.COUNT, sealEdges);
        for(int x = 0; x < Options.WIDTH-2; x++)
        {
        	for(int y = 0; y < Options.HEIGHT-2; y++)
            {
        		for(int z = 0; z < Options.DEPTH-2; z++)
                {
        			if ( ba3CellLocs[x][y][z] )
        			{
	                    FDC fdc = new FDC();
	                    
	                    fdc.setObjectLocation( new Double3D(x+1.5, y+1.5, z+1.5) );
	                    
	                    schedule.scheduleRepeating(fdc, 2, 1);
	                    
	                    fdc.registerCollisions(cgGrid);
        			}
                }
            }
        }
        for ( StromaEdge seEdge : sealEdges )
        {
        	seEdge.setObjectLocation(new Double3D(seEdge.x+1.5, seEdge.y+1.5, seEdge.z+1.5));
        	seEdge.registerCollisions(cgGrid);
        }
        
        // All the static cells are in, now reset the collision data
        cgGrid.step(null);
        
        BC.m_cgGrid = cgGrid;
        
        for(int x=0;x<Options.BC.COUNT;x++)
        {
            Double3D loc = new Double3D(random.nextInt(Options.WIDTH-2)+1, random.nextInt(Options.HEIGHT-2)+1, random.nextInt(Options.DEPTH-2)+1);
            BC bc = new BC();
            
            bc.setObjectLocation(loc);
            
            schedule.scheduleRepeating(bc, 0, 1);
            
            if ( x == 0 )
            {
            	bc.displayGraph = true;
            }
        }

        // add particles
        new Particle(schedule, Particle.TYPE.CXCL13, Options.WIDTH, Options.HEIGHT, Options.DEPTH);
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
