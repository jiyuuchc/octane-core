package edu.uchc.octanecore.test;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Ignore;
import org.junit.Test;

import edu.uchc.octanecore.datasource.RectangularImage;
import edu.uchc.octanecore.fitting.IterativeShrinkageAndThreshold;
import edu.uchc.octanecore.fitting.LeastSquareGaussianFitting;
import edu.uchc.octanecore.fitting.LeastSquareIntegratedGaussianFitting;
import edu.uchc.octanecore.fitting.RadialSymmetryFitting;

public class FittingTest {

	static final int IMAGE_SIZE = 11;

	static final double[] TEST_VALUES = {
			1.4175035112951352E-7, 1.5056067251874795E-6, 1.0387350450247402E-5, 4.6596555689039867E-5, 1.3603475690306496E-4, 2.5864120226361595E-4, 3.203992064538993E-4, 2.5864120226361595E-4, 1.3603475690306496E-4, 4.6596555689039867E-5, 1.0387350450247402E-5,
			9.779516450051282E-7, 1.0387350450247402E-5, 7.166350121265529E-5, 3.2147488824231735E-4, 9.385191163983373E-4, 0.0017843947983501222, 0.002210470228208766, 0.0017843947983501222, 9.385191163983373E-4, 3.2147488824231735E-4, 7.166350121265529E-5,
			4.3869876640759965E-6, 4.6596555689039867E-5, 3.2147488824231735E-4, 0.00144210235366173, 0.004210097510616101, 0.008004606371066857, 0.00991593569322976, 0.008004606371066857, 0.004210097510616101, 0.00144210235366173, 3.2147488824231735E-4,
			1.2807444490144886E-5, 1.3603475690306496E-4, 9.385191163983373E-4, 0.004210097510616101, 0.01229102844461037, 0.02336878049655781, 0.02894874699531102, 0.02336878049655781, 0.01229102844461037, 0.004210097510616101, 9.385191163983373E-4,
			2.4350635942371895E-5, 2.5864120226361595E-4, 0.0017843947983501222, 0.008004606371066857, 0.02336878049655781, 0.04443077358068976, 0.05503989493087988, 0.04443077358068976, 0.02336878049655781, 0.008004606371066857, 0.0017843947983501222,
			3.0165048585846606E-5, 3.203992064538993E-4, 0.002210470228208766, 0.00991593569322976, 0.02894874699531102, 0.05503989493087988, 0.06818224824514224, 0.05503989493087988, 0.02894874699531102, 0.00991593569322976, 0.002210470228208766,
			2.4350635942371895E-5, 2.5864120226361595E-4, 0.0017843947983501222, 0.008004606371066857, 0.02336878049655781, 0.04443077358068976, 0.05503989493087988, 0.04443077358068976, 0.02336878049655781, 0.008004606371066857, 0.0017843947983501222,
			1.2807444490144886E-5, 1.3603475690306496E-4, 9.385191163983373E-4, 0.004210097510616101, 0.01229102844461037, 0.02336878049655781, 0.02894874699531102, 0.02336878049655781, 0.01229102844461037, 0.004210097510616101, 9.385191163983373E-4,
			4.3869876640759965E-6, 4.6596555689039867E-5, 3.2147488824231735E-4, 0.00144210235366173, 0.004210097510616101, 0.008004606371066857, 0.00991593569322976, 0.008004606371066857, 0.004210097510616101, 0.00144210235366173, 3.2147488824231735E-4,
			9.779516450051282E-7, 1.0387350450247402E-5, 7.166350121265529E-5, 3.2147488824231735E-4, 9.385191163983373E-4, 0.0017843947983501222, 0.002210470228208766, 0.0017843947983501222, 9.385191163983373E-4, 3.2147488824231735E-4, 7.166350121265529E-5,
			1.4175035112951352E-7, 1.5056067251874795E-6, 1.0387350450247402E-5, 4.6596555689039867E-5, 1.3603475690306496E-4, 2.5864120226361595E-4, 3.203992064538993E-4, 2.5864120226361595E-4, 1.3603475690306496E-4, 4.6596555689039867E-5, 1.0387350450247402E-5};

	static final double[] TEST_VALUES_WITH_BACKGROUND = new double [TEST_VALUES.length];

	static final double [] START_PARAMS = {5.5, 5.5, 1, 1.5, 0.8};

	static {
		for (int i = 0; i < TEST_VALUES_WITH_BACKGROUND.length; i ++) {
			TEST_VALUES_WITH_BACKGROUND[i] = TEST_VALUES[i] + 1.0;
		}
	}

	@Test
	public void testLeastSquareGaussin() {

		//System.out.println("Least Square Guassian");
		LeastSquareGaussianFitting fitting = new LeastSquareGaussianFitting();
		double[] result = fitting.fit(new RectangularImage(TEST_VALUES_WITH_BACKGROUND, IMAGE_SIZE), START_PARAMS);

		double [] expected = {6, 5, 1, 1.53, 1};
		assertArrayEquals(expected, result, 0.01);
	}

	@Test
	public void testLeastSquareGaussinFixedSigma() {

		//System.out.println("Least Square Guassian fixed sigma");
		LeastSquareGaussianFitting fitting = new LeastSquareGaussianFitting(false, true, false, 500);
		double [] result = fitting.fit(new RectangularImage(TEST_VALUES_WITH_BACKGROUND, IMAGE_SIZE), START_PARAMS);

		double [] expected = {6, 5, 0.976, 1.5, 1};
		assertArrayEquals(expected, result, 0.01);
	}

	@Test
	public void testLeastSquareIntegratedGaussin() {

		//System.out.println("Least Square integrated Guassian");
		LeastSquareIntegratedGaussianFitting fitting = new LeastSquareIntegratedGaussianFitting();
		double [] result = fitting.fit(new RectangularImage(TEST_VALUES_WITH_BACKGROUND, IMAGE_SIZE), START_PARAMS);

		double [] expected = {6, 5, 1, 1.5, 1};
		assertArrayEquals(expected, result, 0.01);
	}

	@Test
	public void testRadialSymmetricFitting () {

		RadialSymmetryFitting fitting = new RadialSymmetryFitting();
		double [] result = fitting.fit(new RectangularImage(TEST_VALUES_WITH_BACKGROUND, IMAGE_SIZE));

		double [] expected = {6, 5};
		assertArrayEquals(expected, result, 0.01);
	}

	@Ignore
	@Test
	public void testIsta() {

		int scale = 8;
		IterativeShrinkageAndThreshold ista = new IterativeShrinkageAndThreshold(0.03, 0.15, 2000);
		double[] x = ista.fit(new RectangularImage(TEST_VALUES, IMAGE_SIZE), scale, 1.50);

		for (int i = 0; i < x.length; i++) {
			if (i % (IMAGE_SIZE * scale) == 0) {
				System.out.println();
			}
			System.out.print(x[i]);
			System.out.print(",");
		}
	}
}
