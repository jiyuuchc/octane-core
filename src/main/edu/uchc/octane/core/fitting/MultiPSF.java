package edu.uchc.octane.core.fitting;

import java.util.Arrays;

import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;

import edu.uchc.octane.core.datasource.ImageData;

/**
 * Representation of multi-molecule model.
 */
public class MultiPSF implements PSFFittingFunction {

	PSFFittingFunction singlePSF;
	int numOfPSFs;
	MultivariateVectorFunction valueFunction;
	MultivariateMatrixFunction jacobianFunction;
	ImageData data;
	int [] unified;

	public MultiPSF(int n, PSFFittingFunction single) {
		this(n, single, null);
	}

	public MultiPSF(int n, PSFFittingFunction single, int [] unified) {
		this.singlePSF = single;
		this.numOfPSFs = n;
		this.unified = unified;
	}

    private void unifyParams(double [] point) {
    	if (unified == null || numOfPSFs <= 1) {return;}

		int subParaLen = point.length / numOfPSFs;

		for (int i = 0; i < unified.length; i++) {
			assert(unified[i] < subParaLen);
			for (int j = 1; j < numOfPSFs; j++) {
				point[j*subParaLen + unified[i]] = point[unified[i]];
			}
		}
    }

//    /**
//     * Value function overriden for speed. When calculating for the whole
//     * subimage, some values can be reused. But can only be used for a square
//     * grid where xgrid values are same in each column and ygrid values are the
//     * same in each row.
//     *
//     * @param xgrid
//     * @param ygrid
//     * @return
//     */
//    @Override
//    public MultivariateVectorFunction getValueFunction(final double[] xgrid, final double[] ygrid) {
//        return new MultivariateVectorFunction() {
//            @Override
//            public double[] value(double[] point) throws IllegalArgumentException {
//                fixParams(point);
//                //
//                double[] retVal = new double[xgrid.length];
//                Arrays.fill(retVal, 0.0);
//                for(int i = 0; i < nmol; i++) {
//                    double [] tmp = Arrays.copyOfRange(point, i*Params.PARAMS_LENGTH, (i+1)*Params.PARAMS_LENGTH);
//                    double [] values = psf.getValueFunction(xgrid, ygrid).value(tmp);
//                    for(int j = 0; j < values.length; j++) {
//                        retVal[j] += values[j];
//                    }
//                }
//                return retVal;
//            }
//        };
//    }
//
//    @Override
//    public double[] getInitialSimplex() {
//        double[] steps = new double[nmol*Params.PARAMS_LENGTH];
//        double[] init = psf.getInitialSimplex();
//        for(int i = 0; i < nmol; i++) {
//            for(int j = 0, k = i*Params.PARAMS_LENGTH; j < Params.PARAMS_LENGTH; j++, k++) {
//                steps[k] = init[j];
//            }
//        }
//        return steps;
//    }
//
//    @Override
//    public double[] getInitialParams(SubImage subImage) {
//        if(n_1_params == null) {
//            assert(nmol == 1);
//            return psf.getInitialParams(subImage);
//        } else {
//            assert(nmol > 1);
//            //
//            double[] guess = new double[nmol*Params.PARAMS_LENGTH];
//            Arrays.fill(guess, 0);
//            // copy parameters of N-1 molecules from previous model
//            System.arraycopy(n_1_params, 0, guess, 0, n_1_params.length);
//            // subtract fitted model from the subimage
//            nmol -= 1;  // change size to get the values of simpler model
//            double [] residual = subImage.subtract(getValueFunction(subImage.xgrid, subImage.ygrid).value(n_1_params));
//            nmol += 1;  // change it back
//            // find maximum
//            int max_i = 0;
//            for(int i = 1; i < residual.length; i++) {
//                if(residual[i] > residual[max_i]) {
//                    max_i = i;
//                }
//            }
//            SubImage img = new SubImage(subImage.size_x, subImage.size_y, subImage.xgrid, subImage.ygrid, residual, subImage.xgrid[max_i], subImage.ygrid[max_i]);
//            // get the initial guess for Nth molecule
//            System.arraycopy(psf.getInitialParams(img), 0, guess, (nmol-1)*Params.PARAMS_LENGTH, Params.PARAMS_LENGTH);
//            // perform push&pull adjustment -- to close to the boundary? push out; else pull in;
//            double x, y, sig_2 = defaultSigma / 2.0;
//            for(int i = 0, base = 0; i < nmol; i++, base += Params.PARAMS_LENGTH) {
//                x = guess[base+Params.X];
//                y = guess[base+Params.Y];
//                if((subImage.size_x/2 - abs(x)) < defaultSigma) {
//                    guess[base+Params.X] += (x > 0 ? sig_2 : -sig_2);
//                } else {
//                    guess[base+Params.X] -= (x > 0 ? sig_2 : -sig_2);
//                }
//                if((subImage.size_y/2 - abs(y)) < defaultSigma) {
//                    guess[base+Params.Y] += (y > 0 ? sig_2 : -sig_2);
//                } else {
//                    guess[base+Params.Y] -= (y > 0 ? sig_2 : -sig_2);
//                }
//            }
//            return guess;
//        }
//    }
//
//    public void setFixedIntensities(boolean sameI) {
//        this.sameI = sameI;
//    }

	@Override
	public MultivariateVectorFunction getValueFunction() {
		return new MultivariateVectorFunction() {
			@Override
			public double[] value(double[] point) throws IllegalArgumentException {
				int subParaLen = point.length / numOfPSFs;
				double[] retVal = new double[data.getLength()];

				unifyParams(point);

				for (int i = 0; i < numOfPSFs; i ++ ) {
					double [] tmp = Arrays.copyOfRange(point, i * subParaLen, (i+1) * subParaLen);
					double [] tmpRet = valueFunction.value(tmp);
					for (int j = 0; j < retVal.length; j++) {
						retVal[j] += tmpRet[j];
					}
				}

				return retVal;
			}
		};
	}

	@Override
	public MultivariateMatrixFunction getJacobian() {
		return new MultivariateMatrixFunction() {

			@Override
			public double[][] value(double[] point) throws IllegalArgumentException {
				int subParaLen = point.length / numOfPSFs;
				double[][] retVal = new double[data.getLength()][point.length];
				// fixParams(point);

				for(int i = 0; i < numOfPSFs; i++) {
					double [] tmp = Arrays.copyOfRange(point, i * subParaLen, (i+1) * subParaLen);
					double [][] tmpVal = jacobianFunction.value(tmp);
					for (int j = 0; j < tmpVal.length; j++) {
						System.arraycopy(tmpVal[j], 0, retVal[j], i * subParaLen, subParaLen);
					}
				}

				if (unified == null) {
					return retVal;
				}

				for (int i = 0; i < unified.length; i++) {
					int paraIndex = unified[i];
					for (int j = 1; j < numOfPSFs; j ++) {
						for (int k = 0; k < retVal.length; k++) {
							retVal[k][j*subParaLen + paraIndex] = 0;
						}
					}
				}

				return retVal;
			}

		};
	}

	@Override
	public double[] pointToParameters(double[] point) {

		unifyParams(point);

		int subParaLen = point.length / numOfPSFs;
		double [] retVal = new double[point.length];
		for(int i = 0; i < numOfPSFs; i++) {
			double [] tmp = Arrays.copyOfRange(point, i * subParaLen, (i+1) * subParaLen);
			double [] tmpVal = singlePSF.pointToParameters(tmp);
			System.arraycopy(tmpVal, 0, retVal, i * subParaLen, subParaLen);
		}
		return retVal;
	}

	@Override
	public double[] parametersToPoint(double[] parameters) {
		int subParaLen = parameters.length / numOfPSFs;
		double [] retVal = new double[parameters.length];
		for(int i = 0; i < numOfPSFs; i++) {
			double [] tmp = Arrays.copyOfRange(parameters, i * subParaLen, (i+1) * subParaLen);
			double [] tmpVal = singlePSF.parametersToPoint(tmp);
			System.arraycopy(tmpVal, 0, retVal, i * subParaLen, subParaLen);
		}
		return retVal;
	}

	@Override
	public void setFittingData(ImageData data) {
		this.data = data;
		singlePSF.setFittingData(data);
		valueFunction = singlePSF.getValueFunction();
		jacobianFunction = singlePSF.getJacobian();

	}

	@Override
	public int getDoF() {
		int dof = singlePSF.getDoF() * numOfPSFs;
		if (unified != null) {
			dof -= unified.length * (numOfPSFs - 1);
		}
		return dof;
	}
}
