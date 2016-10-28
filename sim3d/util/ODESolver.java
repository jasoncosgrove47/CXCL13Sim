package sim3d.util;

import sim3d.Settings;
import sim3d.cell.Lymphocyte;
import sim3d.diffusion.Chemokine;

/**
 * 4th order Runge Kutta Solver as described from:
 * http://mathworld.wolfram.com/Runge-KuttaMethod.html
 * 
 * ODE system: f(Rf), f(LR), F(Ri)
 * 
 * d[LR] = Ka[L][Rf] - Ki[LR]
 * 
 * d[Rf] = Kr[Ri] - Ka[L][Rf]
 * 
 * d[Ri] = Ki[LR] - Kr[Ri]
 * 
 * h = timestep
 * 
 * K1 = hF(Rf, LR, Ri) K2 = hF(Rf + (K1Rf/2), LR + (K1Rf/2), Ri + (K1Rf/2) K3 =
 * hF(Rf + (K2Rf/2), LR + (K2Rf/2), Ri + (K2Rf/2) K4 = hF(Rf + K3Rf, LR + K3LR,
 * Ri + K3Ri)
 * 
 * Rf(t+1) = Rt + 1/6RfK1 + 1/3RfK2 + 1/3RfK3 + 1/6RfK4 LR(t+1) = LRt + 1/6LRK1
 * + 1/3LRK2 + 1/3LRK3 + 1/6LRK4 Ri(t+1) = Rit + 1/6RiK1 + 1/3RiK2 + 1/3RiK3 +
 * 1/6RiK4
 * 
 * @return
 * 
 * 
 * @author Jason Cosgrove
 * 
 */
public class ODESolver {

	/*
	 * This portion of the ODE interfaces with the ABM so is solved seperately.
	 * 4th order Runge Kutta
	 */
	public double solveLR(double Ka, double Ki, int Rf, int LR, double L) {
		double h = 0.01666;

		double RfK1 = h * ((Ka * L * Rf) - (Ki * LR));
		double RfK2 = h * (((Ka * L * Rf) - (Ki * LR)) + RfK1 / 2);
		double RfK3 = h * (((Ka * L * Rf) - (Ki * LR)) + RfK2 / 2);
		double RfK4 = h * (((Ka * L * Rf) - (Ki * LR)) + RfK3);

		double Rf_t1 = (Rf + (RfK1 / 6) + (RfK2 / 3) + (RfK3 / 3) + (RfK4 / 6));

		return Rf_t1;
	}

	/**
	 * This portion of the ODE does not interface directly with the ABM so we
	 * calculate this separately 4th order Runge Kutta
	 */
	public double[] solveRestofODE(double Ka, double Kr, double Ki, int Rf,
			int LR, int Ri, double L) {
		double h = 0.01666;

		double[] results = new double[2];
		double RiK1 = h * ((Ki * LR) - (Kr * Ri));
		double RiK2 = h * (((Ki * LR) - (Kr * Ri)) + RiK1 / 2);
		double RiK3 = h * (((Ki * LR) - (Kr * Ri)) + RiK2 / 2);
		double RiK4 = h * (((Ki * LR) - (Kr * Ri)) + RiK3);

		double LRK1 = h * ((Kr * Ri) - (Ka * L * Rf));
		double LRK2 = h * (((Kr * Ri) - (Ka * L * Rf)) + LRK1 / 2);
		double LRK3 = h * (((Kr * Ri) - (Ka * L * Rf)) + LRK2 / 2);
		double LRK4 = h * (((Kr * Ri) - (Ka * L * Rf)) + LRK3);

		results[0] = (LR + (LRK1 / 6) + (LRK2 / 3) + (LRK3 / 3) + (LRK4 / 6));
		results[1] = (Ri + (RiK1 / 6) + (RiK2 / 3) + (RiK3 / 3) + (RiK4 / 6));

		return results;

	}
	
	
	/**
	 * This portion of the ODE does not interface directly with the ABM so we
	 * calculate this separately 4th order Runge Kutta
	 */
	public static void solveODE(double Ka, double Kr, double Ki,double Koff,double Kdes,
			Chemokine.TYPE chemokine,Lymphocyte lymphocyte) {
		
		
		//figure out which receptor you need
		Lymphocyte.Receptor receptor = null;
		
		switch (chemokine) {
		case CXCL13: 
			receptor = Lymphocyte.Receptor.CXCR5;
			break;
		case CCL19:
			receptor = Lymphocyte.Receptor.CCR7;	
			break;
			
		case EBI2L:
			receptor = Lymphocyte.Receptor.EBI2;	
			break;
		default:
			break;
		}

		
	
		double h = 1;
		int iR_i, iL_r, iR_d, iR_f;


		
		//every second do the following
		//TODO this should be 60
		for (int i = 0; i < 1; i++) {
			
			
			//calculate the total amount of ligand available

			double[][][] ia3Concs = Chemokine.get(chemokine, (int) lymphocyte.x,
					(int) lymphocyte.y, (int) lymphocyte.z);

			
			
			double vol = 1e-12;// volume of one gridspace in litres
			
			// get CXCL13 concentrations at each psuedopod
			// {x+, x-, y+, y-, z+, z-}
			double[] iaConcs = { ia3Concs[2][1][1]/ vol, ia3Concs[0][1][1]/ vol,
					ia3Concs[1][2][1]/ vol, ia3Concs[1][0][1]/ vol, ia3Concs[1][1][2]/ vol,
					ia3Concs[1][1][0] / vol};
			
			
			double totalLigand = 0;
			
			
			
			for (int j = 0; j < 6; j++) // for each pseudopod
			{
				totalLigand += iaConcs[j];
			}
			
			//System.out.println("total ligand" + totalLigand);
			
			iR_i = lymphocyte.getM_Ri(receptor);
			//System.out.println("iR_i: " + iR_i);
			
			iL_r = lymphocyte.getM_LR(receptor);
			//System.out.println("iL_r: " + iL_r);
			
			iR_d = lymphocyte.getM_Rd(receptor);
			//System.out.println("iR_d: " + iR_d);
			iR_f = lymphocyte.getM_Rf(receptor);
			//System.out.println("iR_f_ODE: " + iR_f);
			
			
			double ligandBinding1 = h * (Ka * totalLigand);
			double ligandBinding2 = h * ((Ka * totalLigand) + ligandBinding1 / 2);
			double ligandBinding3 = h * ((Ka * totalLigand) + ligandBinding2 / 2);
			double ligandBinding4 = h * ((Ka * totalLigand) + ligandBinding3);
			
			// receptors desensitised from surface
			double LRdK1 = h * (Kdes * iL_r);
			double LRdK2 = h * ((Kdes * iL_r) + LRdK1 / 2);
			double LRdK3 = h * ((Kdes * iL_r) + LRdK2 / 2);
			double LRdK4 = h * ((Kdes * iL_r) + LRdK3);
			
			// receptors internalised from surface
			double LRK1 = h * (Ki * iR_d);
			double LRK2 = h * ((Ki * iR_d) + LRK1 / 2);
			double LRK3 = h * ((Ki * iR_d) + LRK2 / 2);
			double LRK4 = h * ((Ki * iR_d) + LRK3);
			
			// receptors that are recycled from internal pool
			double RfK1 = h * (Kr * iR_i);
			double RfK2 = h * ((Kr * iR_i) + RfK1 / 2);
			double RfK3 = h * ((Kr * iR_i) + RfK2 / 2);
			double RfK4 = h * ((Kr * iR_i) + RfK3);

			// ligand dissociation from receptor
			double RdisK1 = h * (Koff * iL_r);
			double RdisK2 = h * ((Koff * iL_r) + RdisK1 / 2);
			double RdisK3 = h * ((Koff * iL_r) + RdisK2 / 2);
			double RdisK4 = h * ((Koff * iL_r) + RdisK3);
			
			
			// the total change in bound receptor for this time increment is
						// given b this equation
			double proportionToBind = ((ligandBinding1 / 6) + (ligandBinding2 / 3)
									+ (ligandBinding3 / 3) + (ligandBinding4 / 6));
					
			
			//System.out.println("proportionToBind" + proportionToBind);
			// cap the amount of receptors that can be bound
			if (proportionToBind > 1) {proportionToBind = 1;}
			else if (proportionToBind < 0) {proportionToBind = 0;}

			
		
			
			
			//determine the number of receptors that have bound
			double receptorsBound = (int) (proportionToBind * iR_f);
			//System.out.println("receptorsBound" + receptorsBound);
			
			double receptorsRecycled = (int) ((RfK1 / 6) + (RfK2 / 3) + (RfK3 / 3) + (RfK4 / 6));
			double receptorsInternalised = (int) ((LRK1 / 6) + (LRK2 / 3) + (LRK3 / 3) + (LRK4 / 6));
			double ligandDissociation = (int) ((RdisK1 / 6) + (RdisK2 / 3) + (RdisK3 / 3) + (RdisK4 / 6));
			double receptorsDesensitised = (int) ((LRdK1 / 6) + (LRdK2 / 3) + (LRdK3 / 3) + (LRdK4 / 6));
			
						
			int d_rf = lymphocyte.getM_Rf(receptor);
			int d_ri = lymphocyte.getM_Ri(receptor);
			//System.out.println("d_ri: " + d_ri);
			
			int d_lr = lymphocyte.getM_LR(receptor);
			
			int d_rd = lymphocyte.getM_Rd(receptor);
			
			
			d_rf = (int) (d_rf - receptorsBound + ligandDissociation + receptorsRecycled);
			
			d_lr = (int) (d_lr + receptorsBound - ligandDissociation - receptorsDesensitised);
			
			d_rd = (int) (d_rd + receptorsDesensitised - receptorsInternalised);
			
			d_ri = (int) (d_ri + receptorsInternalised - receptorsRecycled);
			
	
			lymphocyte.setM_Rf(receptor, d_rf);
			lymphocyte.setM_Ri(receptor, d_ri);
			lymphocyte.setM_LR(receptor, d_lr);
			lymphocyte.setM_Rd(receptor, d_rd);	
						
		
		}
		
	}
	

}
