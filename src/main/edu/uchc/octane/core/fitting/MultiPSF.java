package edu.uchc.octane.core.fitting;

import java.util.Arrays;

import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;

import edu.uchc.octane.core.pixelimage.AbstractDoubleImage;

/**
 * Representation of multi-molecule model.
 */
public class MultiPSF implements PSFFittingFunction {

    PSFFittingFunction singlePSF;
    int numOfPSFs;
    MultivariateVectorFunction valueFunction;
    MultivariateMatrixFunction jacobianFunction;
    AbstractDoubleImage data;
    int[] unified;

    public MultiPSF(int n, PSFFittingFunction single) {
        this(n, single, null);
    }

    public MultiPSF(int n, PSFFittingFunction single, int[] unified) {
        this.singlePSF = single;
        this.numOfPSFs = n;
        this.unified = unified;
    }

    private void unifyParams(double[] point) {
        if (unified == null || numOfPSFs <= 1) {
            return;
        }

        int subParaLen = point.length / numOfPSFs;

        for (int i = 0; i < unified.length; i++) {
            assert (unified[i] < subParaLen);
            for (int j = 1; j < numOfPSFs; j++) {
                point[j * subParaLen + unified[i]] = point[unified[i]];
            }
        }
    }

    @Override
    public MultivariateVectorFunction getValueFunction() {
        return new MultivariateVectorFunction() {
            @Override
            public double[] value(double[] point) throws IllegalArgumentException {
                int subParaLen = point.length / numOfPSFs;
                double[] retVal = new double[data.getLength()];

                unifyParams(point);

                for (int i = 0; i < numOfPSFs; i++) {
                    double[] tmp = Arrays.copyOfRange(point, i * subParaLen, (i + 1) * subParaLen);
                    double[] tmpRet = valueFunction.value(tmp);
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

                for (int i = 0; i < numOfPSFs; i++) {
                    double[] tmp = Arrays.copyOfRange(point, i * subParaLen, (i + 1) * subParaLen);
                    double[][] tmpVal = jacobianFunction.value(tmp);
                    for (int j = 0; j < tmpVal.length; j++) {
                        System.arraycopy(tmpVal[j], 0, retVal[j], i * subParaLen, subParaLen);
                    }
                }

                if (unified == null) {
                    return retVal;
                }

                for (int i = 0; i < unified.length; i++) {
                    int paraIndex = unified[i];
                    for (int j = 1; j < numOfPSFs; j++) {
                        for (int k = 0; k < retVal.length; k++) {
                            retVal[k][j * subParaLen + paraIndex] = 0;
                        }
                    }
                }

                return retVal;
            }

        };
    }

    @Override
    public double[] convertParametersInternalToExternal(double[] internalParameters) {

        unifyParams(internalParameters);

        int subParaLen = internalParameters.length / numOfPSFs;
        double[] retVal = new double[internalParameters.length];
        for (int i = 0; i < numOfPSFs; i++) {
            double[] tmp = Arrays.copyOfRange(internalParameters, i * subParaLen, (i + 1) * subParaLen);
            double[] tmpVal = singlePSF.convertParametersInternalToExternal(tmp);

            if (tmpVal == null) {
                return null;
            }

            System.arraycopy(tmpVal, 0, retVal, i * subParaLen, subParaLen);
        }
        return retVal;
    }

    @Override
    public double[] convertParametersExternalToInternal(double[] externalParameters) {
        int subParaLen = externalParameters.length / numOfPSFs;
        double[] retVal = new double[externalParameters.length];
        for (int i = 0; i < numOfPSFs; i++) {
            double[] tmp = Arrays.copyOfRange(externalParameters, i * subParaLen, (i + 1) * subParaLen);
            double[] tmpVal = singlePSF.convertParametersExternalToInternal(tmp);
            System.arraycopy(tmpVal, 0, retVal, i * subParaLen, subParaLen);
        }
        return retVal;
    }

    @Override
    public void setFittingData(AbstractDoubleImage data) {
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
