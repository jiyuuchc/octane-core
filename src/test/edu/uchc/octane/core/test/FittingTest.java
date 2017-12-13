package edu.uchc.octane.core.test;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Ignore;
import org.junit.Test;

import edu.uchc.octane.core.datasource.RectangularImage;
import edu.uchc.octane.core.fitting.AsymmetricGaussianPSF;
import edu.uchc.octane.core.fitting.DAOFitting;
import edu.uchc.octane.core.fitting.GaussianPSF;
import edu.uchc.octane.core.fitting.IntegratedGaussianPSF;
import edu.uchc.octane.core.fitting.IterativeShrinkageAndThreshold;
import edu.uchc.octane.core.fitting.LeastSquare;
import edu.uchc.octane.core.fitting.MultiPSF;
import edu.uchc.octane.core.fitting.RadialSymmetryFitting;

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
	public void testAsymmetricGaussian() {
		System.out.println("Least Square Asymmetric Guassian");
		LeastSquare lsq = new LeastSquare(new AsymmetricGaussianPSF());
		final double [] start = {5.5, 5.5, 1, 1.5, 1.5, 0.8};
		double[] result = lsq.fit(new RectangularImage(TEST_VALUES_WITH_BACKGROUND, IMAGE_SIZE), start);
		double [] expected = {6, 5, 1, 1.53, 1.53, 1};
		assertArrayEquals(expected, result, 0.01);
	}

	@Test
	public void testLeastSquareGaussin() {

		System.out.println("Least Square Guassian");
		LeastSquare lsq = new LeastSquare(new GaussianPSF());
		double[] result = lsq.fit(new RectangularImage(TEST_VALUES_WITH_BACKGROUND, IMAGE_SIZE), START_PARAMS);
		double [] expected = {6, 5, 1, 1.53, 1};
		assertArrayEquals(expected, result, 0.01);
	}

	@Test
	public void testLeastSquareGaussinFixedSigma() {

		System.out.println("Least Square Guassian fixed sigma");
		LeastSquare lsq = new LeastSquare(new GaussianPSF(true, false));
		double[] result = lsq.fit(new RectangularImage(TEST_VALUES_WITH_BACKGROUND, IMAGE_SIZE), START_PARAMS);
		double [] expected = {6, 5, 0.976, 1.5, 1};
		assertArrayEquals(expected, result, 0.01);
	}

	@Test
	public void testLeastSquareIntegratedGaussin() {

		System.out.println("Least Square integrated Guassian");
		LeastSquare lsq = new LeastSquare(new IntegratedGaussianPSF());
		double[] result = lsq.fit(new RectangularImage(TEST_VALUES_WITH_BACKGROUND, IMAGE_SIZE), START_PARAMS);
		double [] expected = {6, 5, 1, 1.5, 1};
		assertArrayEquals(expected, result, 0.01);
	}

	@Test
	public void testRadialSymmetricFitting () {

		System.out.println("Radial symmetry fitting test");
		RadialSymmetryFitting fitting = new RadialSymmetryFitting();
		double [] result = fitting.fit(new RectangularImage(TEST_VALUES_WITH_BACKGROUND, IMAGE_SIZE));
		double [] expected = {6, 5};
		assertArrayEquals(expected, result, 0.01);
	}

	@Test
	public void testMultiPSFOnSinglePeak () {
		System.out.println("MultiPSF fitting on single peak");
		MultiPSF multiPsf = new MultiPSF(1, new IntegratedGaussianPSF());
		LeastSquare lsq = new LeastSquare(multiPsf);
		double [] result = lsq.fit(new RectangularImage(TEST_VALUES_WITH_BACKGROUND, IMAGE_SIZE), START_PARAMS);
		double [] expected = {6, 5, 1, 1.5, 1};
		assertArrayEquals(expected, result, 0.01);
	}

	@Test
	public void testMultiPSF () {
		System.out.println("MultiPSF fitting two peaks");
		double [] newValue = {0.0012971,0.0039449,0.0077054,0.0096891,0.0078874,0.0042106,0.0015151,0.00038603,7.4045e-05,7.8144e-06,1.0576e-06,
				0.0039449,0.012018,0.023568,0.029922,0.024913,0.013981,0.0055556,0.001662,0.00038603,5.7741e-05,7.8144e-06,
				0.0077054,0.023568,0.046667,0.060599,0.05304,0.032871,0.015337,0.0055556,0.0015151,0.00027356,3.7022e-05,
				0.0096891,0.029922,0.060599,0.082722,0.079956,0.058182,0.032871,0.013981,0.0042106,0.00083101,0.00011246,
				0.0078874,0.024913,0.05304,0.079956,0.090743,0.079956,0.05304,0.024913,0.0078874,0.0016186,0.00021905,
				0.0042106,0.013981,0.032871,0.058182,0.079956,0.082722,0.060599,0.029922,0.0096891,0.0020214,0.00027356,
				0.0015151,0.0055556,0.015337,0.032871,0.05304,0.060599,0.046667,0.023568,0.0077054,0.0016186,0.00021905,
				0.00038603,0.001662,0.0055556,0.013981,0.024913,0.029922,0.023568,0.012018,0.0039449,0.00083101,0.00011246,
				7.4045e-05,0.00038603,0.0015151,0.0042106,0.0078874,0.0096891,0.0077054,0.0039449,0.0012971,0.00027356,3.7022e-05,
				7.8144e-06,5.7741e-05,0.00027356,0.00083101,0.0016186,0.0020214,0.0016186,0.00083101,0.00027356,5.7741e-05,7.8144e-06,
				1.0576e-06,7.8144e-06,3.7022e-05,0.00011246,0.00021905,0.00027356,0.00021905,0.00011246,3.7022e-05,7.8144e-06,1.0576e-06};
		double [] start = {5, 5, 1, 1.5, 0, 3, 2, 1, 1.5, 0};
		MultiPSF multiPsf = new MultiPSF(2, new IntegratedGaussianPSF());
		LeastSquare lsq = new LeastSquare(multiPsf);
		double[] result = lsq.fit(new RectangularImage(newValue, IMAGE_SIZE), start);
		double [] expected = {5, 5, 1, 1.47, 0, 3, 3, 1, 1.47, 0};
		assertArrayEquals(expected, result, 0.01);
	}

	@Test
	public void testDAOFitting () {
		System.out.println("Daofitting two peaks");
		double [] newValue = {0.0012971,0.0039449,0.0077054,0.0096891,0.0078874,0.0042106,0.0015151,0.00038603,7.4045e-05,7.8144e-06,1.0576e-06,
				0.0039449,0.012018,0.023568,0.029922,0.024913,0.013981,0.0055556,0.001662,0.00038603,5.7741e-05,7.8144e-06,
				0.0077054,0.023568,0.046667,0.060599,0.05304,0.032871,0.015337,0.0055556,0.0015151,0.00027356,3.7022e-05,
				0.0096891,0.029922,0.060599,0.082722,0.079956,0.058182,0.032871,0.013981,0.0042106,0.00083101,0.00011246,
				0.0078874,0.024913,0.05304,0.079956,0.090743,0.079956,0.05304,0.024913,0.0078874,0.0016186,0.00021905,
				0.0042106,0.013981,0.032871,0.058182,0.079956,0.082722,0.060599,0.029922,0.0096891,0.0020214,0.00027356,
				0.0015151,0.0055556,0.015337,0.032871,0.05304,0.060599,0.046667,0.023568,0.0077054,0.0016186,0.00021905,
				0.00038603,0.001662,0.0055556,0.013981,0.024913,0.029922,0.023568,0.012018,0.0039449,0.00083101,0.00011246,
				7.4045e-05,0.00038603,0.0015151,0.0042106,0.0078874,0.0096891,0.0077054,0.0039449,0.0012971,0.00027356,3.7022e-05,
				7.8144e-06,5.7741e-05,0.00027356,0.00083101,0.0016186,0.0020214,0.0016186,0.00083101,0.00027356,5.7741e-05,7.8144e-06,
				1.0576e-06,7.8144e-06,3.7022e-05,0.00011246,0.00021905,0.00027356,0.00021905,0.00011246,3.7022e-05,7.8144e-06,1.0576e-06};
		double [] start = {5, 5, 1, 1.5, 0};
		IntegratedGaussianPSF psf = new IntegratedGaussianPSF();
		DAOFitting dao = new DAOFitting(psf, 4, 1e-6);
		double[][] result = dao.fit(new RectangularImage(newValue, IMAGE_SIZE), start);
		assert(result.length == 2);
		double [][] expected = { {3, 3, 1, 1.47, 0}, {5, 5, 1, 1.47, 0}};
		assertArrayEquals(expected[0], result[0], 0.01);
		assertArrayEquals(expected[1], result[1], 0.01);
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
