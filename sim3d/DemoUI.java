package sim3d;

import sim.portrayal.continuous.*;
import sim.portrayal.grid.FastValueGridPortrayal2D;
import sim.portrayal3d.continuous.ContinuousPortrayal3D;
import sim.portrayal3d.grid.ValueGridPortrayal3D;
import sim.portrayal3d.simple.SpherePortrayal3D;
import sim.portrayal3d.simple.ValuePortrayal3D;
import sim.util.media.chart.TimeSeriesChartGenerator;
import sim.engine.*;
import sim.display.*;
import sim.display3d.Display3D;
import sim3d.cell.BC;
import sim3d.cell.FDC;
import sim3d.diffusion.Particle;
import sim3d.diffusion.ParticleColorMap;

import javax.swing.*;

import java.awt.Color;
import java.net.MalformedURLException;
import java.net.URL;

public class DemoUI extends GUIState
{

    public Display3D display3D;
    public JFrame d3DisplayFrame;
    public JFrame chartFrame;
    public JFrame chartFrame2;
    public JFrame chartFrame3;
    
    ContinuousPortrayal3D frcPortrayal = new ContinuousPortrayal3D();
    ContinuousPortrayal3D bcPortrayal = new ContinuousPortrayal3D();
    FastValueGridPortrayal2D particlePortrayal = new FastValueGridPortrayal2D();
    ValueGridPortrayal3D p3dParticles = new ValueGridPortrayal3D();
    ContinuousPortrayal3D p3dPortrayal = new ContinuousPortrayal3D();
    
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
        
        Grapher.start();
    }
    
    public void finish()
    {
    	super.finish();
    }

    public void load(SimState state)
        {
        super.load(state);
        setupPortrayals();
        }
        
    public void setupPortrayals()
    {
        // tell the portrayals what to portray and how to portray them
        frcPortrayal.setField(FDC.drawEnvironment);
        bcPortrayal.setField(BC.drawEnvironment);
        
        p3dParticles.setField(Particle.getInstance(Particle.TYPE.CXCL13));
        //p3dParticles.setMap(new ParticleColorMap());
        p3dParticles.setMap(new sim.util.gui.SimpleColorMap(0.0, 3000, new Color(0,0,0,0), Color.WHITE));
        
        display3D.createSceneGraph();
        display3D.reset();
    }

    public void init(Controller c)
    {
        super.init(c);
    
        // make the displayer
        display3D = new Display3D(600,600,this);
        Demo.display3D = display3D;
        
        display3D.translate(-Options.WIDTH / 2.0, -Options.WIDTH / 2.0, 0);
        display3D.scale(2.0 / Options.WIDTH);
        
        d3DisplayFrame = display3D.createFrame();

        d3DisplayFrame.setTitle("Demo3D");
        c.registerFrame(d3DisplayFrame);   // register the frame so it appears in the "Display" list
        d3DisplayFrame.setVisible(true);
        
        display3D.attach( frcPortrayal, "FRC" );
        display3D.attach( bcPortrayal, "BC" );
        //display3D.attach( p3dParticles, "Particles" );
        
        Grapher.init();
        Grapher.schedule = state.schedule;

        chartFrame = Grapher.chart.createFrame();
        // perhaps you might move the chart to where you like.
        chartFrame.setVisible(true);
        chartFrame.pack();
        chartFrame.setLocation(0, 700);
        c.registerFrame(chartFrame);

        chartFrame2 = Grapher.bcFRCEdgeSizeChart.createFrame();
        // perhaps you might move the chart to where you like.
        chartFrame2.setVisible(true);
        chartFrame2.pack();
        chartFrame2.setLocation(0, 700);
        c.registerFrame(chartFrame2);

        chartFrame3 = Grapher.bcFRCEdgeNumberChart.createFrame();
        // perhaps you might move the chart to where you like.
        chartFrame3.setVisible(true);
        chartFrame3.pack();
        chartFrame3.setLocation(0, 700);
        c.registerFrame(chartFrame3);
    }
        
    public void quit()
        {
        super.quit();
        
        if (d3DisplayFrame!=null) d3DisplayFrame.dispose();
        d3DisplayFrame = null;
        display3D = null;
        
        Grapher.finish();
        if (chartFrame != null)	chartFrame.dispose();
        chartFrame = null;
        }
    
    public Object getSimulationInspectedObject()
    {
    return state;
    }

    }
