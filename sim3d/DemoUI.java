package sim3d;

import sim.portrayal.continuous.*;
import sim.portrayal.grid.FastValueGridPortrayal2D;
import sim.engine.*;
import sim.display.*;
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
    public JFrame displayFrame;

    public JFrame htmlFrame;
    JEditorPane htmlPane;
    
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
        frcPortrayal.setField(FDC.drawEnvironment);
        bcPortrayal.setField(BC.drawEnvironment);
        particlePortrayal.setField(Particle.getInstance(Particle.TYPE.CXCL13).m_ig2Display);
        particlePortrayal.setMap(new ParticleColorMap());
                
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
        
        display.attach( particlePortrayal, "Particles" );
        display.attach( frcPortrayal, "FRC" );
        display.attach( bcPortrayal, "BC" );
        
        
        htmlFrame = new JFrame();
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
        }
    }
        
    public void quit()
        {
        super.quit();
        
        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;
        display = null;
        }
    
    public Object getSimulationInspectedObject()
    {
    return state;
    }

    }
