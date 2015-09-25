package sim3d;
/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

//package sim.app.woims;


import sim.portrayal.continuous.*;
import sim.portrayal.grid.FastValueGridPortrayal2D;
import sim3d.cell.BC;
import sim3d.cell.FDC;
import sim3d.cell.Ova;
import sim3d.diffusion.Particle;
import sim3d.diffusion.ParticleColorMap;
import sim.engine.*;
import sim.display.*;
import javax.swing.*;
import java.awt.Color;

public class DemoUI extends GUIState
{

    public Display2D display;
    public JFrame displayFrame;

    ContinuousPortrayal2D ovaPortrayal = new ContinuousPortrayal2D();
    ContinuousPortrayal2D frcPortrayal = new ContinuousPortrayal2D();
    ContinuousPortrayal2D bcPortrayal = new ContinuousPortrayal2D();
    FastValueGridPortrayal2D particlePortrayal = new FastValueGridPortrayal2D();
    
    public static void main(String[] args)
    {
        new DemoUI().createController();
    }

    public DemoUI() { super(new Demo( System.currentTimeMillis())); }
    public DemoUI(SimState state) { super(state); }

    public static String getName() { return "Demo"; }

    public void start()
        {
        super.start();
        setupPortrayals();
        }

    public void load(SimState state)
        {
        super.load(state);
        setupPortrayals();
        }
        
    public void setupPortrayals()
        {
        // tell the portrayals what to portray and how to portray them
        ovaPortrayal.setField(Ova.drawEnvironment);
        frcPortrayal.setField(FDC.drawEnvironment);
        bcPortrayal.setField(BC.drawEnvironment);
        //particlePortrayal.setField(Particle.getInstance(Particle.TYPE.CXCL13));
        //particlePortrayal.setMap(new ParticleColorMap());
                
        // reschedule the displayer
        display.reset();
        display.setBackdrop(Color.black);
                
        // redraw the display
        display.repaint();
        }

    public void init(Controller c)
    {
        super.init(c);

        // make the displayer
        display = new Display2D(600,600,this);

        displayFrame = display.createFrame();
        displayFrame.setTitle("Demo");
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);
        //display.attach( particlePortrayal, "Particles" );
        display.attach( ovaPortrayal, "Ova" );
        display.attach( frcPortrayal, "FRC" );
        display.attach( bcPortrayal, "BC" );
    }
        
    public void quit()
        {
        super.quit();
        
        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;
        display = null;
        }

    }
