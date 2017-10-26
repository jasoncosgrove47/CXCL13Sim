package sim3d.diffusion.algorithms;

import java.io.Serializable;

import sim3d.Settings;

/**
 * Diffusion method proposed by Adrian Grajdeanu in
 * "Modelling diffusion in a discrete environment", Technical Report
 * GMU-CS-TR-2007-1
 * 
 * @author Simon Jarrett - {@link simonjjarrett@gmail.com}
 * @see https://cs.gmu.edu/~tr-admin/papers/GMU-CS-TR-2007-1.pdf
 */
public class Grajdeanu extends DiffusionAlgorithmMultiThread implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Initialise the class and set the diffusion coefficients for the
	 * DiffusionAlgorithm parent
	 * 
	 * @param dDiffusionCoefficient
	 *            The parameter for the diffusion
	 * @param iWidth
	 *            Width of the space
	 * @param iHeight
	 *            Height of the space
	 * @param iDepth
	 *            Depth of the space
	 */
	public Grajdeanu(double dDiffusionCoefficient, int iWidth, int iHeight,
			int iDepth,int numThreads, double DIFFUSION_TIMESTEP) {
		super(iWidth, iHeight, iDepth,numThreads);

		setDiffusionCoefficients(dDiffusionCoefficient,DIFFUSION_TIMESTEP);
	}

	/**
	 * Sets the diffusion coefficients of the parent using the diffusion
	 * coefficient.In the gradjeanu implementation the diffusion coefficient
	 * isn't used directly.
	 * 
	 * The coefficient is exp(-d^2/mu) Where: d^2 is Math.pow(
	 * Settings.GRID_SIZE, 2 ) * (x * x + y * y + z * z) mu is 4Dt (which is in
	 * the Grajdeanu paper).
	 * 
	 * @param dDiffuseCoeff
	 *            The diffusion coefficient to use
	 */
	private void setDiffusionCoefficients(double dDiffuseCoeff, double DIFFUSION_TIMESTEP) {
		// initialise the coefficient array
		m_adDiffusionCoefficients = new double[3][3][3];

		// normalise so that the outer boxes sum to dDispersePercent
		double dTotalDistance = 0;

		// Loop through the surrounding squares in 3D space
		// the Moore neighbourhood
		for (int x = -1; x < 2; x++) {
			for (int y = -1; y < 2; y++) {
				for (int z = -1; z < 2; z++) {
					// if ( x*x + y*y + z*z <= 1)
					// Grajdeanu's algorithm has coefficients exp(-d^2/µ) where
					// d is the distance to the neighbouring square, and µ is
					// the diffusion coefficient d^2 is calculated by squaring
					// each of the fundamental vector distances (i.e. pythagoras
					// in 3D!) this stops anisotropy as accounts for fact that diagonal
					// neighbours are further away than lateral ones

					// We can add some value to the center squares diffusion
					// coefficient such that more chemokine stays there if we 
					// want to simulate confined diffusion. Remember 
					// that the algorithm weights each surrounding gridspace and 
					// then normalises it so we can increase the weighting at the 
					// central space by doing this
					m_adDiffusionCoefficients[x + 1][y + 1][z + 1] = Math
							.exp(-(Math.pow(Settings.GRID_SIZE, 2) * (x * x + y
									* y + z * z))
									/ (4 * dDiffuseCoeff * DIFFUSION_TIMESTEP));

					// Add the result to the total distance for normalising
					dTotalDistance += m_adDiffusionCoefficients[x + 1][y + 1][z + 1];
				}
			}
		}

		// need to normalise so that the total amount being diffused is less
		// than or equal to the total amount that exists
		normaliseChemokine(dTotalDistance);
			
	}
	
	/*
	 * Normalise the chemokine so that the
	 * total amount being diffused is less 
	 * than or equal to the total amount
	 * that exists
	 */
	private void normaliseChemokine(double dTotalDistance){
		// need to normalise so that the total amount being diffused is less
		// than or equal to the total amount that exists

		double dNormalisingCoefficient = 1 / dTotalDistance;

		for (int x = 0; x < 3; x++) {
			for (int y = 0; y < 3; y++) {
				for (int z = 0; z < 3; z++) {
					// Normalise so the sum adds up to one
					m_adDiffusionCoefficients[x][y][z] *= dNormalisingCoefficient;
				}
			}
		}
	}

}
