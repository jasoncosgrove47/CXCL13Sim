package sim3d.util;

public class ODESolver {

	/**
	 * This class employs a 4th order runga kutta
	 */
	

	
	/*
	 * d[LR] = Ka[L][Rf] - Ki[LR]
	 * 
	 * d[Rf] = Kr[Ri] - Ka[L][Rf]
	 * 
	 * d[Ri] = Ki[LR] - Kr[Ri]
	 * 
	 */
	//Solve system of ODEs using
	// a fourth order Runge Kutta
	
	
	/**
	 * Approach is taken from:
	 * http://mathworld.wolfram.com/Runge-KuttaMethod.html
	 * 
	 * ODE system: f(Rf), f(LR), F(Ri)
	 * 
	 * h = timestep
	 * 
	 * K1 = hF(Rf, LR, Ri)
	 * K2 = hF(Rf + (K1Rf/2), LR + (K1Rf/2), Ri + (K1Rf/2)
	 * K3 = hF(Rf + (K2Rf/2), LR + (K2Rf/2), Ri + (K2Rf/2)
	 * K4 = hF(Rf + K3Rf, LR + K3LR, Ri + K3Ri)
	 * 
	 * Rf(t+1) = Rt + 1/6RfK1 + 1/3RfK2 + 1/3RfK3 + 1/6RfK4
	 * LR(t+1) = LRt + 1/6LRK1 + 1/3LRK2 + 1/3LRK3 + 1/6LRK4
	 * Ri(t+1) = Rit + 1/6RiK1 + 1/3RiK2 + 1/3RiK3 + 1/6RiK4
	 * 
	 */
	
	public void solveODE(double Ka, double Kr, double Ki, int Rf, int LR, int Ri, double L){
		double h = 0.01666; // timestep, 0.01666 = 1 second
		
		double RfK1 = h * ((Ka * L * Rf) -  (Ki*LR))  ;
		double RfK2 = h * ( ((Ka * L * Rf) - (Ki*LR)) + RfK1/2) ;
		double RfK3 = h * ( ((Ka * L * Rf) - (Ki*LR)) + RfK2/2) ;
		double RfK4 = h * ( ((Ka * L * Rf) - (Ki*LR)) + RfK3) ;
		
		double RiK1 = h * ((Ki * LR) -  (Kr * Ri));
		double RiK2 = h * (((Ki * LR) - (Kr * Ri)) + RiK1/2);
		double RiK3 = h * (((Ki * LR) - (Kr * Ri)) + RiK2/2);
		double RiK4 = h * (((Ki * LR) - (Kr * Ri)) + RiK3);
		
		double LRK1 = h * ((Kr*Ri) -  (Ka * L * Rf)) ;
		double LRK2 = h * (((Kr*Ri) - (Ka * L * Rf)) + LRK1/2);
		double LRK3 = h * (((Kr*Ri) - (Ka * L * Rf)) + LRK2/2);
		double LRK4 = h * (((Kr*Ri) - (Ka * L * Rf)) + LRK3);
		
		double Rf_t1 = (Rf + (RfK1/6) + (RfK2/3)  + (RfK3/3) + (RfK4/6)) ;
		double LR_t1 = (LR + (LRK1/6) + (LRK2/3)  + (LRK3/3) + (LRK4/6)) ;
		double Ri_t1 = (Ri + (RiK1/6) + (RiK2/3)  + (RiK3/3) + (RiK4/6)) ;
		
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
