package osmgpxtool.osmgpxpreprocessor;

import java.math.BigDecimal;
import java.util.stream.IntStream;

import org.apache.commons.math3.stat.descriptive.moment.Mean;

import com.vividsolutions.jts.math.MathUtil;

public class WeightedMovingAverageTask {
	private double[] weights;
	private double[] measurements;

	public WeightedMovingAverageTask(double[] measurements, double[] weights) {
		super();

		this.weights = weights;

		this.measurements = measurements;

		checkWeights();
	}

	private void checkWeights() {
		// must be an odd number
		if (weights.length % 2 == 0) {
			throw new IllegalArgumentException("Number of given weights is not odd.");
		}
		// sum must be equal to 1
		BigDecimal sum = BigDecimal.valueOf(0);

		for (double weight : weights) {
			sum = sum.add(BigDecimal.valueOf(weight));

		}

		if (sum.compareTo(new BigDecimal(1d)) != 0) {
			throw new IllegalArgumentException("Sum of given weights is not equal to one.");
		}

	}

	public Double[] smoothMeasurements() {
		Double[] smoothed = new Double[measurements.length];
		for (int i = 0; i < measurements.length; i++) {
			// calculate mean of measurement[i] and the weights.length / 2
			// number of measurements before and after measurement[i]
			int n = -weights.length / 2;
			double[] m = new double[weights.length];
			if (i >= Math.abs(n) && i<measurements.length-Math.abs(n)) {
				for (int a = 0; a < m.length; a++) {
					m[a] = measurements[i + n];
					n++;
				}
				Mean mean = new Mean();
				smoothed[i] = Double.valueOf(mean.evaluate(m, weights));
			} else {
				smoothed[i] = Double.NaN;
			}
		}
		return smoothed;
	}

}
