package sim3d;

import sim.portrayal.continuous.*;
import sim.portrayal.grid.FastValueGridPortrayal2D;
import sim.portrayal3d.continuous.ContinuousPortrayal3D;
import sim.portrayal3d.simple.SpherePortrayal3D;
import sim.portrayal3d.simple.ValuePortrayal3D;
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

    public Display2D display;
    public Display3D display3D;
    public JFrame displayFrame;
    public JFrame d3DisplayFrame;

    public JFrame htmlFrame;
    JEditorPane htmlPane;
    
    ContinuousPortrayal3D frcPortrayal = new ContinuousPortrayal3D();
    ContinuousPortrayal3D bcPortrayal = new ContinuousPortrayal3D();
    FastValueGridPortrayal2D particlePortrayal = new FastValueGridPortrayal2D();
    ValuePortrayal3D p3dParticles = new ValuePortrayal3D();
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

        //bcPortrayal.setPortrayalForAll(new SpherePortrayal3D(Options.BC.DRAW_COLOR(), 1, 10));
        //frcPortrayal.setPortrayalForAll(new SpherePortrayal3D(Options.FDC.DRAW_COLOR(), 0.4, 10));
        
        particlePortrayal.setField(Particle.getInstance(Particle.TYPE.CXCL13).m_ig2Display);
        particlePortrayal.setMap(new ParticleColorMap());
                
        // reschedule the displayer
        //display.reset();
        //display.setBackdrop(Color.black);
                
        // redraw the display
        //display.repaint();
        
        display3D.createSceneGraph();
        display3D.reset();
    }

    public void init(Controller c)
    {
        super.init(c);
    
        // make the displayer
        //display = new Display2D(600,600,this);
        display3D = new Display3D(600,600,this);
        Demo.display3D = display3D;
        
        display3D.translate(-Options.WIDTH / 2.0, -Options.WIDTH / 2.0, 0);
        display3D.scale(2.0 / Options.WIDTH);

        
        //displayFrame = display.createFrame();
        d3DisplayFrame = display3D.createFrame();

        //displayFrame.setTitle("Demo");
        d3DisplayFrame.setTitle("Demo3D");
        //c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        c.registerFrame(d3DisplayFrame);   // register the frame so it appears in the "Display" list
        //displayFrame.setVisible(true);
        d3DisplayFrame.setVisible(true);
        
        display3D.attach( frcPortrayal, "FRC" );
        
        //display.attach( particlePortrayal, "Particles" );
        //display.attach( frcPortrayal, "FRC" );
        display3D.attach( bcPortrayal, "BC" );
        
        
        /*htmlFrame = new JFrame();
        try
        {
        	String html = "<img src='http://chart.apis.google.com/chart?cht=lc&chs=600x450&chxt=y,x,x,x&chts=FFFFFF,14&chls=3,1,0|3,1,0&chg=100.0,6.78,5,0&chco=FFFF00,32CD32&chdl=S+%26+P+500|Inflation&chf=bg,s,000000|c,s,708090&chxp=1,5,18,39,55,92|3,50.0&chtt=S+%26+P+500%7C1962+-+2008&chxr=0,0.0,1475.25|1,0.0,100.0|2,1962.0,2008.0|3,0.0,100.0&chxs=0,FFFFFF,12,0|1,FFFFFF,12,0|2,FFFFFF,12,0|3,FFFFFF,14,0&chxl=1:|Fed+Chiefs%3A|Burns|Volcker|Greenspan|Bernanke|3:|Year&chm=o,FFFF00,0,-1,10,0|o,000000,0,-1,7,0|v,0000FF,0,8,3,0|v,0000FF,0,17,3,0|v,0000FF,0,24,3,0|v,0000FF,0,40,3,0|B,FFFFE0,0,0,0|o,00FF00,1,-1,10,0|o,000000,1,-1,7,0|B,90EE90,1,0,0&chd=e:CsDMDmD8EGEbD6D8EIE.EKC3D1EkEDEHEmFxFOF.HCHGI.KYKlL0O9OBRtSuT.TqaNgKpa0j-e46xilhvVzx1r8x-7,CsCyC4C.DFDMDTDbDiDqDyD7EEENEWEgEqE0E.FKFWFiFuF7GIGWGkGzHCHSHiHzIFIXIqI9JRJmJ7KSKpLALZLzMN'>";

            htmlPane = new JEditorPane();
            htmlPane.setContentType("text/html");
            
            htmlFrame.add(htmlPane);
            c.registerFrame(htmlFrame);
            htmlFrame.setVisible(true);
            htmlFrame.setSize(600, 450);
            
            Grapher.htmlPane = htmlPane;
            
            Grapher.updateGraph();
        }
        catch (Exception e)
        {
        }*/
    }
        
    public void quit()
        {
        super.quit();
        
        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;
        display = null;
        if (d3DisplayFrame!=null) d3DisplayFrame.dispose();
        d3DisplayFrame = null;
        display3D = null;
        }
    
    public Object getSimulationInspectedObject()
    {
    return state;
    }

    }
