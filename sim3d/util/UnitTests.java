package sim3d.util;

import java.util.ArrayList;

import org.jzy3d.analysis.AbstractAnalysis;
import org.jzy3d.analysis.AnalysisLauncher;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.BoundingBox3d;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.Scatter;
import org.jzy3d.plot3d.rendering.canvas.Quality;

import ec.util.MersenneTwisterFast;
import sim.engine.Schedule;
import sim.util.Double3D;
import sim.util.Int3D;
import sim3d.Options;
import sim3d.cell.StromaEdge;
import sim3d.diffusion.Particle;

public class UnitTests extends AbstractAnalysis {

	public static void main(String[] args) throws Exception {
		AnalysisLauncher.open(new UnitTests());
	}

	public void testFRCStromaGenerator()
	{
		int size = Options.FDC.COUNT;

        ArrayList<StromaEdge> sealEdges = new ArrayList<StromaEdge>(); 
		boolean[][][] ba3Stroma = FRCStromaGenerator.generateStroma3D(Options.WIDTH-2,  Options.HEIGHT-2, Options.DEPTH-2, size, sealEdges);
		ArrayList<Coord3d> c3dalPoints = new ArrayList<Coord3d>();
		ArrayList<Coord3d> c3dalLines = new ArrayList<Coord3d>();
		
        for ( int x = 0; x < Options.WIDTH-2; x++ )
        {
        	for ( int y = 0; y < Options.HEIGHT-2; y++ )
            {
        		for ( int z = 0; z < Options.DEPTH-2; z++ )
                {
                	if ( ba3Stroma[x][y][z] )
                	{
                		ArrayList<Int3D> cells = FRCStromaGenerator.getAdjacentCells(Options.WIDTH-2,  Options.HEIGHT-2, Options.DEPTH-2, ba3Stroma, new Int3D(x, y, z), Options.FRCGenerator.MAX_EDGE_LENGTH());
                		c3dalPoints.add( new Coord3d(x, y, z) );
                	}
                }
            }
        }
        
        for ( StromaEdge seEdge : sealEdges )
        {
        	Double3D d3Point1 = seEdge.getPoint1();
        	Double3D d3Point2 = seEdge.getPoint2();
        	drawLine(c3dalLines, new Coord3d(d3Point1.x, d3Point1.y, d3Point1.z), new Coord3d(d3Point2.x, d3Point2.y, d3Point2.z));
        }
        
        Coord3d[] points = new Coord3d[c3dalPoints.size()];
        Color[]   colors = new Color[c3dalPoints.size()];
        
        for ( int i = 0; i < c3dalPoints.size(); i++ )
        {
        	points[i] = c3dalPoints.get(i);
        	colors[i] = new Color(0,0,0,200);
        }

        Coord3d[] lines = new Coord3d[c3dalLines.size()];
        Color[]   linecolors = new Color[c3dalLines.size()];
        for ( int i = 0; i < c3dalLines.size(); i++ )
        {
        	lines[i] = c3dalLines.get(i);
        	linecolors[i] = new Color(255,0,0,200);
        }

        Scatter scatter = new Scatter(points, colors);
        scatter.width = 3;
        Scatter scatter2 = new Scatter(lines, linecolors);
        scatter2.width = 1;
        chart = AWTChartComponentFactory.chart(Quality.Advanced, "awt");
        chart.getScene().add(scatter);
        chart.getScene().add(scatter2);
	}
	
	public void drawLine(ArrayList<Coord3d> points, Coord3d p1, Coord3d p2)
	{
		Coord3d diff = p2.sub(p1).mul(0.05f);
		for (int i = 0; i < 19; i++)
		{
			points.add(p1.add(diff.mul(i)));
		}
	}
	
	public void drawLine(ArrayList<Coord3d> points, Double3D d1, Double3D d2)
	{
		Coord3d p1 = new Coord3d(d1.x, d1.y, d1.z);
		Coord3d p2 = new Coord3d(d2.x, d2.y, d2.z);
		
		Coord3d diff = p2.sub(p1).mul(0.02f);
		for (int i = 0; i < 50; i++)
		{
			points.add(p1.add(diff.mul(i)));
		}
	}
	
	public void testRotateVectorToVector()
	{
		int lines = 4;
		Coord3d[] points = new Coord3d[lines*50];
        Color[]   colors = new Color[lines*50];
        
        ArrayList<Coord3d> c3dalPoints = new ArrayList<Coord3d>();

        Double3D d3Point1 = Vector3DHelper.getRandomDirection();
        Double3D d3Point2 = Vector3DHelper.getRandomDirection();
        
        Double3D zero = new Double3D(0,0,0);

        drawLine(c3dalPoints, zero, d3Point1);
        drawLine(c3dalPoints, zero, d3Point2);
        
        Double3D curPoint = new Double3D(d3Point1.x, d3Point1.y, d3Point1.z);
        
        for ( int i = 2; i < lines; i++ )
        {
        	curPoint = Vector3DHelper.rotateVectorToVector(curPoint, d3Point1, d3Point2);
        	drawLine(c3dalPoints, zero, curPoint);
        }
        
        for ( int i = 0; i < 100; i++ )
        {
        	points[i] = c3dalPoints.get(i);
        	colors[i] = new Color(0,(i<50)?255:0,(i<50)?0:255,255);
        }
        
        for ( int i = 100; i < c3dalPoints.size(); i++ )
        {
        	points[i] = c3dalPoints.get(i);
        	colors[i] = new Color((int)(200+(i-100.0)/(lines*50.0) * 55),0,0,255);
        }
        
		Scatter scatter = new Scatter(points, colors);
        scatter.width = 6;
        chart = AWTChartComponentFactory.chart(Quality.Advanced, "awt");
        chart.getScene().add(scatter);
	}
	
	public void testVector3DHelperRandomDirection()
	{
		int size = 10000;
		Coord3d[] points = new Coord3d[size];
        Color[]   colors = new Color[size];
		
        for(int i=0; i<size; i++){
        	Double3D rand = Vector3DHelper.getRandomDirectionInCone(new Double3D(0,0,1), Math.PI/3);
            points[i] = new Coord3d(rand.x, rand.y, rand.z);
            colors[i] = new Color(0, 0, 0, 100);
        }
        
        Scatter scatter = new Scatter(points, colors);
        chart = AWTChartComponentFactory.chart(Quality.Advanced, "awt");
        BoundingBox3d bounds = new BoundingBox3d();
        bounds.setXmin(-1f);
        bounds.setXmax(1f);
        bounds.setYmin(-1f);
        bounds.setYmax(1f);
        bounds.setZmin(-1f);
        bounds.setZmax(1f);
        
        chart.getView().setBoundManual(bounds);
        
        chart.getScene().add(scatter);
	}


	public void testVector3DHelperRandomDirectionInCone()
	{
		int size = 20000;
		Coord3d[] points = new Coord3d[4*size+400];
        Color[]   colors = new Color[4*size+400];

        Double3D vec = Vector3DHelper.getRandomDirection();
        for(int i=0; i<size; i++){
        	Double3D rand = Vector3DHelper.getBiasedRandomDirectionInCone(vec, Math.PI/6);
            points[i] = new Coord3d(rand.x, rand.y, rand.z);
            colors[i] = new Color(200, 0, 0, 100);
        }
        for ( int i = 0; i < 100; i++ )
        {
        	Double3D rand = vec.multiply(((double)i)/100.0);
        	points[4*size+i] = new Coord3d(rand.x, rand.y, rand.z);
            colors[4*size+i] = new Color(255, 0, 0, 255);
        }
        
        vec = Vector3DHelper.getRandomDirection();
        for(int i=0; i<size; i++){
        	Double3D rand = Vector3DHelper.getBiasedRandomDirectionInCone(vec, Math.PI/6);
            points[size+i] = new Coord3d(rand.x, rand.y, rand.z);
            colors[size+i] = new Color(0, 200, 0, 100);
        }
        for ( int i = 0; i < 100; i++ )
        {
        	Double3D rand = vec.multiply(((double)i)/100.0);
        	points[4*size+i+100] = new Coord3d(rand.x, rand.y, rand.z);
            colors[4*size+i+100] = new Color(0, 255, 0, 255);
        }
        
        vec = Vector3DHelper.getRandomDirection();
        for(int i=0; i<size; i++){
        	Double3D rand = Vector3DHelper.getBiasedRandomDirectionInCone(vec, Math.PI/6);
            points[2*size+i] = new Coord3d(rand.x, rand.y, rand.z);
            colors[2*size+i] = new Color(0, 0, 200, 100);
        }/**/
        for ( int i = 0; i < 100; i++ )
        {
        	Double3D rand = vec.multiply(((double)i)/100.0);
        	points[4*size+i+200] = new Coord3d(rand.x, rand.y, rand.z);
            colors[4*size+i+200] = new Color(0, 0, 255, 255);
        }
        
        vec = Vector3DHelper.getRandomDirection();
        for(int i=0; i<size; i++){
        	Double3D rand = Vector3DHelper.getBiasedRandomDirectionInCone(vec, Math.PI/6);
            points[3*size+i] = new Coord3d(rand.x, rand.y, rand.z);
            colors[3*size+i] = new Color(0, 0, 0, 100);
        }/**/
        for ( int i = 0; i < 100; i++ )
        {
        	Double3D rand = vec.multiply(((double)i)/100.0);
        	points[4*size+i+300] = new Coord3d(rand.x, rand.y, rand.z);
            colors[4*size+i+300] = new Color(0, 0, 0, 255);
        }
        
        Scatter scatter = new Scatter(points, colors);
        chart = AWTChartComponentFactory.chart(Quality.Advanced, "awt");
        //chart.setScale(new Scale(-0.5, 0.5));
        BoundingBox3d bounds = new BoundingBox3d();
        bounds.setXmin(-1f);
        bounds.setXmax(1f);
        bounds.setYmin(-1f);
        bounds.setYmax(1f);
        bounds.setZmin(-1f);
        bounds.setZmax(1f);
        
        chart.getView().setBoundManual(bounds);
        chart.getScene().add(scatter);
	}

	public void testVector3DHelperEqDistPointsOnSphere()
	{
		int size = 5000;
		Coord3d[] points = new Coord3d[size];
        Color[]   colors = new Color[size];
		
        Double3D[] ad3Points = Vector3DHelper.getEqDistPointsOnSphere(size);
        
        for(int i=0; i<size; i++){
            points[i] = new Coord3d(ad3Points[i].x, ad3Points[i].y, ad3Points[i].z);
            colors[i] = new Color(0, 0, 0, 100);
        }
        
        Scatter scatter = new Scatter(points, colors);
        scatter.width = 5;
        chart = AWTChartComponentFactory.chart(Quality.Advanced, "awt");
        chart.getScene().add(scatter);
	}
	
	public void testParticleDiffusion(){
		Schedule schedule = new Schedule();
		Particle p = new Particle(schedule, Particle.TYPE.CCL19, 125,125,125);
		
		p.add(62, 62, 62, 1);
		p.m_dDecayRateInv = 1;
		for ( int i = 0; i < 60; i++ )
		{
			p.step(null);
		}

		double distance = 0;
		
		for ( int x = 0; x < 125; x++ )
		{
			for ( int y = 0; y < 125; y++ )
			{
				for ( int z = 0; z < 125; z++ )
				{
					distance += ((62-x)*(62-x) + (62-y)*(62-y) + (62-z)*(62-z))*p.field[x][y][z];
				}
			}
		}
		
		System.out.printf("Mean Square Dist = %f", distance);
	}
	
	@Override
	public void init() throws Exception
	{
		Options.RNG = new MersenneTwisterFast();

		//testVector3DHelperRandomDirection();
		//testVector3DHelperRandomDirectionInCone();
		//testVector3DHelperEqDistPointsOnSphere();
		//testFRCStromaGenerator();
		//testParticleDiffusion();
		testRotateVectorToVector();
	}
}
