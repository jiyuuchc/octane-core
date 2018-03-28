package edu.uchc.octane.core.fitting;

import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.util.FastMath;

public class IntegratedGaussianPSF extends GaussianPSF {

//    public static class Params {
//        public static final int X = 0;
//        public static final int Y = 1;
//        public static final int INTENSITY = 2;
//        public static final int SIGMA = 3;
//        public static final int OFFSET = 4;
//        public static final int PARAMS_LENGTH = 5;
//    }

    public IntegratedGaussianPSF() {
        super();
    }

    public IntegratedGaussianPSF(boolean fixSigma, boolean fixOffset) {
        super(fixSigma, fixOffset);
    }

    @Override
    public MultivariateVectorFunction getValueFunction() {
        return new MultivariateVectorFunction() {

            @Override
            public double[] value(double[] point) throws IllegalArgumentException {

                double sigma = point[Params.SIGMA];
                double sqrt2s = FastMath.sqrt(2.0) * sigma;
                double intensity = point[Params.INTENSITY] * point[Params.INTENSITY];
                double offset = point[Params.OFFSET] * point[Params.OFFSET];

                double[] retVal = new double[data.getLength()];

                // FIXME cache values
                for (int i = 0; i < data.getLength(); i++) {

                    double dx = data.getXCordinate(i) - point[Params.X];
                    double dy = data.getYCordinate(i) - point[Params.Y];
                    double errX = erf((dx + 0.5) / sqrt2s) - erf((dx - 0.5) / sqrt2s);
                    double errY = erf((dy + 0.5) / sqrt2s) - erf((dy - 0.5) / sqrt2s);
                    retVal[i] = offset + 0.25 * intensity * errX * errY;
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

                // double[] params = pointToParameters(point);
                double sigma = point[Params.SIGMA];
                double sigmaSquared = sigma * sigma;
                double sqrt2s = FastMath.sqrt(2) * sigma;
                double intensity = point[Params.INTENSITY] * point[Params.INTENSITY];

                double[][] retVal = new double[data.getLength()][Params.PARAMS_LENGTH];

                // FIXME cache values
                for (int i = 0; i < data.getLength(); i++) {

                    double dy = data.getYCordinate(i) - point[Params.Y];
                    double dx = data.getXCordinate(i) - point[Params.X];
                    double errY = erf((dy + 0.5) / sqrt2s) - erf((dy - 0.5) / sqrt2s);
                    double errX = erf((dx + 0.5) / sqrt2s) - erf((dx - 0.5) / sqrt2s);

                    double yp = FastMath.sqrt(2 / FastMath.PI)
                            * FastMath.exp(-(dy + .5) * (dy + .5) / (2 * sigmaSquared));
                    double ym = FastMath.sqrt(2 / FastMath.PI)
                            * FastMath.exp(-(dy - .5) * (dy - .5) / (2 * sigmaSquared));
                    double expDeltaY = (ym - yp) / sigma;

                    double xp = FastMath.sqrt(2 / FastMath.PI)
                            * FastMath.exp(-(dx + .5) * (dx + .5) / (2 * sigmaSquared));
                    double xm = FastMath.sqrt(2 / FastMath.PI)
                            * FastMath.exp(-(dx - .5) * (dx - .5) / (2 * sigmaSquared));
                    double expDeltaX = (xm - xp) / sigma;

                    retVal[i][Params.INTENSITY] = 0.5 * point[Params.INTENSITY] * errX * errY;

                    if (fixSigma) {
                        retVal[i][Params.SIGMA] = 0;
                    } else {
                        double expDeltaSY = (ym * (dy - 0.5) - yp * (dy + 0.5)) / sigmaSquared;
                        double expDeltaSX = (xm * (dx - 0.5) - xp * (dx + 0.5)) / sigmaSquared;
                        retVal[i][Params.SIGMA] = 0.25 * intensity * (errX * expDeltaSY + errY * expDeltaSX);
                    }

                    retVal[i][Params.X] = 0.25 * intensity * errY * expDeltaX;
                    retVal[i][Params.Y] = 0.25 * intensity * errX * expDeltaY;

                    if (fixOffset) {
                        retVal[i][Params.OFFSET] = 0;
                    } else {
                        retVal[i][Params.OFFSET] = 2 * point[Params.OFFSET];
                    }
                }

                return retVal;
            }
        };
    }

    @Override
    public double[] pointToParameters(double[] point) {

        if (!data.isCoordinateValid((int) FastMath.floor(point[Params.X]), (int) FastMath.floor(point[Params.Y]))) {
            return null;
        }
        
        if (point[Params.SIGMA] <=0) {
            return null;
        }

        double[] transformed = point.clone();

        transformed[Params.INTENSITY] = point[Params.INTENSITY] * point[Params.INTENSITY];
        transformed[Params.OFFSET] = point[Params.OFFSET] * point[Params.OFFSET];

        return transformed;
    }

    @Override
    public double[] parametersToPoint(double[] parameters) {
        double[] transformed = parameters.clone();

        transformed[Params.INTENSITY] = FastMath.sqrt(parameters[Params.INTENSITY]);
        transformed[Params.OFFSET] = FastMath.sqrt(parameters[Params.OFFSET]);

        return transformed;
    }

    private double erf(double z) {
        double t = 1.0 / (1.0 + 0.5 * Math.abs(z));

        // use Horner's method
        double ans = 1 - t * FastMath.exp(-z * z - 1.26551223
                + t * (1.00002368 + t * (0.37409196 + t * (0.09678418 + t * (-0.18628806 + t * (0.27886807
                        + t * (-1.13520398 + t * (1.48851587 + t * (-0.82215223 + t * (0.17087277))))))))));
        if (z >= 0) {
            return ans;
        } else {
            return -ans;
        }
    }

}
