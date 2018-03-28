package edu.uchc.octane.core.fitting;

import org.apache.commons.math3.distribution.FDistribution;

import edu.uchc.octane.core.pixelimage.AbstractDoubleImage;

public class DAOFitting {

	MultiPSF multiPsf;
	PSFFittingFunction psf;
	int maxNumPeaks;
	double pValue;

	public DAOFitting(PSFFittingFunction psf) {
		this(psf, 4, 1e-6);
	}

	public DAOFitting(PSFFittingFunction psf, int max, double pValue) {
		this.psf = psf;
		this.maxNumPeaks = max;
		this.pValue = pValue;
	}

    public double [][] fit(AbstractDoubleImage data, double [] start) {

    	double [] curStart = start;

		MultiPSF multiPsf = new MultiPSF(1, psf);
		LeastSquare multiLsq = new LeastSquare(multiPsf);
		LeastSquare lsq = new LeastSquare(psf);
		if (multiLsq.fit(data, curStart) == null) {
			return null;
		}
		MultiPSF bestPsf = multiPsf;
		LeastSquare bestLsq = multiLsq;

    	for(int n = 2; n <= maxNumPeaks; n++) {

    		double [] oldValues = data.getValueVector();
    		data.setValueVector(multiLsq.optimum.getResiduals().toArray());
    		double [] lastPeak = lsq.fit(data, start);
    		data.setValueVector(oldValues);

    		multiPsf = new MultiPSF(n, psf);
    		multiLsq = new LeastSquare(multiPsf);

    		double [] oldStart = curStart;
    		curStart = new double[n * start.length];
    		System.arraycopy(oldStart, 0, curStart, 0, oldStart.length);
    		if (lastPeak != null ) {
    			System.arraycopy(lastPeak, 0, curStart, oldStart.length, lastPeak.length);
    		} else {
    			System.arraycopy(start, 0, curStart, oldStart.length, start.length);
    		}

    		if (multiLsq.fit(data, curStart) != null) {

    			double pValue = 1.0 - new FDistribution(
    					multiPsf.getDoF() - bestPsf.getDoF(),
    					data.getLength() - multiPsf.getDoF())
    					.cumulativeProbability(
    							( (bestLsq.optimum.getCost() - multiLsq.optimum.getCost()) / (multiPsf.getDoF() - bestPsf.getDoF()))
    							/ (multiLsq.optimum.getCost() / (data.getLength() - multiPsf.getDoF()) )
    							);

    			if(!Double.isNaN(pValue) && (pValue < this.pValue) ) {
    				bestPsf = multiPsf;
    				bestLsq = multiLsq;
    			}
    		}
    	}

    	double [] tmpResult = bestLsq.getResult();
    	int subParaLen = tmpResult.length / bestPsf.numOfPSFs;
    	double [][] results = new double[bestPsf.numOfPSFs][subParaLen];

    	for (int i = 0; i < results.length; i++) {
    		System.arraycopy(tmpResult, i * subParaLen, results[i], 0, subParaLen);
    	}

    	return results;
    }
}
