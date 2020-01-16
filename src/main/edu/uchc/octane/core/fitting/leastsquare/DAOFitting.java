package edu.uchc.octane.core.fitting.leastsquare;

import org.apache.commons.math3.distribution.FDistribution;

import edu.uchc.octane.core.fitting.Fitter;
import edu.uchc.octane.core.pixelimage.PixelImageBase;

public class DAOFitting implements Fitter{

	MultiPSF multiPsf;
	PSFFittingFunction psf;
	int maxNumPeaks;
	double pValue;
	double [][] results;
	int resultCnt;

	public DAOFitting(PSFFittingFunction psf) {
		this(psf, 4, 1e-6);
	}

	public DAOFitting(PSFFittingFunction psf, int max, double pValue) {
		this.psf = psf;
		this.maxNumPeaks = max;
		this.pValue = pValue;
		this.results = null;
	}

    public double [] fit(PixelImageBase data, double [] start) {

    	if (start == null) {
    		start = psf.setFittingData(data);
    	}
    	double [] curStart = start;

		MultiPSF multiPsf = new MultiPSF(1, psf);
		LeastSquare fitter = new LeastSquare(multiPsf);
		// LeastSquare lsq = new LeastSquare(psf);
		if (fitter.fit(data, curStart) == null) {
			return null;
		}
		MultiPSF bestPsf = multiPsf;
		LeastSquare bestLsq = fitter;

    	for(int n = 2; n <= maxNumPeaks; n++) {

//    		double [] oldValues = data.getValueVector();
//    		data.setValueVector(multiLsq.optimum.getResiduals().toArray());
//    		double [] lastPeak = lsq.fit(data, start);
//    		data.setValueVector(oldValues);

    		multiPsf = new MultiPSF(n, psf);
    		fitter = new LeastSquare(multiPsf);

    		double [] oldStart = curStart;
    		curStart = new double[n * start.length];
    		System.arraycopy(oldStart, 0, curStart, 0, oldStart.length);
//    		if (lastPeak != null ) {
//    			System.arraycopy(lastPeak, 0, curStart, oldStart.length, lastPeak.length);
//    		} else {
    			System.arraycopy(start, 0, curStart, oldStart.length, start.length);
//    		}

    		if (fitter.fit(data, curStart) != null) {

    			double pValue = 1.0 - new FDistribution(
    					multiPsf.getDoF() - bestPsf.getDoF(),
    					data.getLength() - multiPsf.getDoF())
    					.cumulativeProbability(
    							( (bestLsq.optimum.getCost() - fitter.optimum.getCost()) / (multiPsf.getDoF() - bestPsf.getDoF()))
    							/ (fitter.optimum.getCost() / (data.getLength() - multiPsf.getDoF()) )
    							);

    			if(!Double.isNaN(pValue) && (pValue < this.pValue) ) {
    				bestPsf = multiPsf;
    				bestLsq = fitter;
    			}
    		} else {
    		    break;
    		}
    	}

    	double [] tmpResult = bestLsq.getResult();
    	int subParaLen = tmpResult.length / bestPsf.numOfPSFs;
    	double [][] results = new double[bestPsf.numOfPSFs][subParaLen];

    	for (int i = 0; i < results.length; i++) {
    		System.arraycopy(tmpResult, i * subParaLen, results[i], 0, subParaLen);
    	}

    	this.results = results;
    	this.resultCnt = 1;

    	return results[0];
    }

	public double[] getNextResult() {
		if (resultCnt < results.length) {
			return results[resultCnt ++];
		} else {
			return null;
		}
	}

	@Override
	public String[] getHeaders() {
		return psf.getHeaders();
	}
	
	public int getNumParticles() {
		if (results != null) {
			return results.length;
		} else {
			return 0;
		}
	}
}
