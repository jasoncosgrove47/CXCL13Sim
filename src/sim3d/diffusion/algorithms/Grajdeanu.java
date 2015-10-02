package sim3d.diffusion.algorithms;

/**
 * Diffusion method proposed by Adrian Grajdeanu in "Modelling diffusion in a discrete environment",
 * Technical Report GMU-CS-TR-2007-1
 * 
 * @author Simon Jarrett
 * @see https://cs.gmu.edu/~tr-admin/papers/GMU-CS-TR-2007-1.pdf
 */

public class Grajdeanu extends DiffusionAlgorithm
{
	// Public
	
	public Grajdeanu(double dDiffusionCoefficient, int iWidth, int iHeight, int iDepth)
	{
		super(iWidth, iHeight, iDepth);
		
		setDiffusionCoefficients(dDiffusionCoefficient);
	}
	
	
	// Private
	
	private void setDiffusionCoefficients(double dDiffuseCoeff)
	{
		// initialise member variable
		m_adDiffusionCoefficients = new double[3][3][3];
		
		// used to calculate amount dispersed to the corresponding squares
		double[][][] adDistances = new double[3][3][3];
		
		// normalise so that the outer boxes sum to dDispersePercent
		double dTotalDistance = 0;
		for (int x = -1; x < 2; x++)
		{
			for (int y = -1; y< 2; y++)
			{
				for (int z = -1; z < 2; z++)
				{
					// Not concerned with the middle square
					if ( x == 0 && y == 0 && z == 0 )
					{
						continue;
					}
					
					adDistances[x+1][y+1][z+1] = Math.exp(-(x*x + y*y + z*z)/dDiffuseCoeff);
					dTotalDistance += adDistances[x+1][y+1][z+1];
				}
			}
		}
		double dNormalisingCoefficient = 1 / dTotalDistance;
		
		// set the coefficients using distances and normalising constants
		for (int x = 0; x < 3; x++)
		{
			for (int y = 0; y< 3; y++)
			{
				for (int z = 0; z < 3; z++)
				{
					m_adDiffusionCoefficients[x][y][z] = dNormalisingCoefficient * adDistances[x][y][z];
				}
			}
		}
	}
}
