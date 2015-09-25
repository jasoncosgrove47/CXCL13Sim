package sim3d.util;

import org.jzy3d.analysis.AbstractAnalysis;
import org.jzy3d.analysis.AnalysisLauncher;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.BoundingBox3d;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.maths.Scale;
import org.jzy3d.plot3d.primitives.Scatter;
import org.jzy3d.plot3d.rendering.canvas.Quality;

import ec.util.MersenneTwisterFast;
import sim.util.Double3D;
import sim3d.Options;

public class UnitTests extends AbstractAnalysis {

	public static void main(String[] args) throws Exception {
		AnalysisLauncher.open(new UnitTests());
	}


	public void testVector3DHelperRandomDirection()
	{
		int size = 50000;
		Coord3d[] points = new Coord3d[size];
        Color[]   colors = new Color[size];
		
        for(int i=0; i<size; i++){
        	Double3D rand = Vector3DHelper.getRandomDirection();
            points[i] = new Coord3d(rand.x, rand.y, rand.z);
            colors[i] = new Color(0, 0, 0, 100);
        }
        
        Scatter scatter = new Scatter(points, colors);
        chart = AWTChartComponentFactory.chart(Quality.Advanced, "awt");
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

	@Override
	public void init() throws Exception
	{
		Options.RNG = new MersenneTwisterFast();

		//testVector3DHelperRandomDirection();
		testVector3DHelperRandomDirectionInCone();
		
	}
}
