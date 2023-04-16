package Controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.DoubleSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import Master.MVC;
import Model.PairPoint;
import Model.Point;
import Model.Solution;
import Request.Body;
import Request.BodyCode;
import Request.Notify;
import Request.Request;
import Request.RequestCode;
import Request.RequestType;

import java.awt.Dimension;
import java.time.Duration;
import java.time.Instant;

public class Controller implements Notify {

	private MVC hub;
	private Random rng;
	private Point[] data;

	public Controller(MVC mvc) {
		this.hub = mvc;
		this.rng = new Random();
	}

	public Controller(MVC mvc, int seed) {
		this.hub = mvc;
		this.rng = new Random(seed);
	}

	public void setSeed(int seed) {
		this.rng.setSeed(seed);
	}

	private Point[] generateData(DoubleSupplier function, Dimension dimension, int amount) {
		Point[] points = new Point[amount];

		for (int i = 0; i < points.length; i++) {
			points[i] = new Point(
					function.getAsDouble() * dimension.width,
					function.getAsDouble() * dimension.height);
		}

		return points;
	}

	private double nextBoundedGaussian() {
		double result = Math.abs(rng.nextGaussian() * 0.2 + 0.5);
		while (!bounded(result)) {
			result = Math.abs(rng.nextGaussian() * 0.2 + 0.5);
		}
		return result;
	}

	// Distribucion de poisson que devuelva valores entre 0 y 1
	private double getPoisson() {
		return 0;
	}

	private double getExponential() {
		double lambda = 0.5;
		double result = Math.log(1 - rng.nextDouble()) / (-lambda);
		while (!bounded(result)) {
			result = getExponential();
		}
		return result;
	}

	private boolean bounded(double point) {
		return point < 1.0 && point > 0.0;
	}

	private void resetRNG() {
		this.rng = new Random(this.hub.getModel().getSeed());
	}

	@Override
	public void notifyRequest(Request<?> request) {
		switch (request.code) {
			case GENERATE_UNIFORM_DATA -> {
				resetRNG();
				Point[] points = generateData(
						rng::nextDouble,
						this.hub.getModel().getFrameDimension(),
						this.hub.getModel().getPointAmount());
				Body<Point[]> body = new Body<>(RequestType.PUT, BodyCode.DATA, points);
				this.hub.notifyRequest(new Request<>(RequestCode.NEW_DATA, this, body));
			}
			case GENERATE_GAUSSIAN_DATA -> {
				resetRNG();
				Point[] points = generateData(
						this::nextBoundedGaussian,
						this.hub.getModel().getFrameDimension(),
						this.hub.getModel().getPointAmount());
				Body<Point[]> body = new Body<>(RequestType.PUT, BodyCode.DATA, points);
				this.hub.notifyRequest(new Request<>(RequestCode.NEW_DATA, this, body));
			}
			case GENERATE_POISSON_DATA -> {
				resetRNG();
				Point[] points = generateData(
						this::getPoisson,
						this.hub.getModel().getFrameDimension(),
						this.hub.getModel().getPointAmount());
				Body<Point[]> body = new Body<>(RequestType.PUT, BodyCode.DATA, points);
				this.hub.notifyRequest(new Request<>(RequestCode.NEW_DATA, this, body));
			}

			case GENERATE_EXPONENTIAL_DATA -> {
				resetRNG();
				Point[] points = generateData(
						this::getExponential,
						this.hub.getModel().getFrameDimension(),
						this.hub.getModel().getPointAmount());
				Body<Point[]> body = new Body<>(RequestType.PUT, BodyCode.DATA, points);
				this.hub.notifyRequest(new Request<>(RequestCode.NEW_DATA, this, body));
			}

			case SEND_DATA -> {
				this.data = (Point[]) request.body.get(BodyCode.DATA);
			}
			case CALC_MIN_DIS -> {
				this.hub.notifyRequest(new Request<>(RequestCode.GET_DATA, this));
				Thread.startVirtualThread(this::calculateMinDistanceNN);
			}
			case CALC_MAX_DIS -> {
				this.hub.notifyRequest(new Request<>(RequestCode.GET_DATA, this));
				Thread.startVirtualThread(this::calculateMaxDistanceNN);
			}
			case CALC_STATS -> {
				// TODO: Change this to the request body system
				// this.hub.notifyRequest(new Request<>(RequestCode.GET_STATS_DATA, this));
				Thread.startVirtualThread(this::calculateStats);
			}
			default -> {
				Logger.getLogger(this.getClass().getSimpleName())
						.log(Level.SEVERE, "{0} is not implemented.", request);
			}
		}
	}

	private void calculateStats() {
		Object[] statsData = new Object[2];
		ArrayList<Solution> max = this.hub.getModel().getSolutionsForMax();
		ArrayList<Solution> min = this.hub.getModel().getSolutionsForMin();
		// TODO
		statsData[0] = max.stream().mapToDouble(Solution::distance).average().orElse(0);
		statsData[1] = min.stream().mapToDouble(Solution::distance).average().orElse(0);

		Body<Object[]> body = new Body<>(RequestType.PUT, BodyCode.DATA, statsData);
		this.hub.notifyRequest(new Request<>(RequestCode.STATS_DATA, this, body));
	}

	private boolean isNotInPairList(Point point1, Point point2, boolean isMin) {
		PairPoint[] pairPointsList = isMin ? this.hub.getModel().getMinPairPointsList()
				: this.hub.getModel().getMaxPairPointsList();
		for (PairPoint pairPoint : pairPointsList) {
			if (pairPoint.p1().equals(point1) && pairPoint.p2().equals(point2)) {
				return false;
			}
		}
		return true;
	}

	private Solution[] initSolutions(int n, boolean isMin) {
		Solution[] solutions = new Solution[5];
		for (int i = 0; i < solutions.length; i++) {
			solutions[i] = new Solution(null, isMin ? Double.MAX_VALUE : Double.MIN_VALUE, 0);
		}
		return solutions;
	}

	private void calculateMinDistanceNN() {
		// TODO: Create a button to set the number of solutions
		Solution[] solutions = initSolutions(5, true);
		Instant start = Instant.now();
		for (int i = 0; i < data.length; i++) {
			for (int j = i + 1; j < data.length; j++) {
				double tempDistance = data[i].euclideanDistanceTo(data[j]);
				if (tempDistance < solutions[0].distance() && isNotInPairList(data[i], data[j], true)) {
					solutions[0] = new Solution(new PairPoint(data[i], data[j]),
							tempDistance,
							Duration.between(start, Instant.now()).toMillis());
					Arrays.sort(solutions,
							(solution1, solution2) -> Double.compare(solution2.distance(), solution1.distance()));
				}
			}
		}

		for (Solution solution : solutions) {
			Logger.getLogger(this.getClass().getSimpleName())
					.log(Level.INFO,
							"Minimum distance found is {0} between points {1} and {2} under {3} milliseconds.",
							new Object[] { solution.distance(), solution.pair().p1(), solution.pair().p2(),
									solution.time() });

			Body<Solution> body1 = new Body<>(RequestType.PUT, BodyCode.PAIR_POINTS, solution);
			this.hub.notifyRequest(new Request<>(RequestCode.NEW_PAIR_DATA_MIN, this, body1));
		}

		Object[] objects = new Object[2];
		// TODO: Change this to the request body system
		objects[0] = this.hub.getModel().getMinPairPointsList();
		objects[1] = this.hub.getModel().getData();
		Body<Object[]> body = new Body<>(RequestType.PUT, BodyCode.PAIR_POINTS, objects);
		this.hub.notifyRequest(new Request<>(RequestCode.RESULT_MIN_DIS, this, body));
	}

	private void calculateMaxDistanceNN() {
		// TODO: Create a button to set the number of solutions
		Solution[] solutions = initSolutions(5, false);
		Instant start = Instant.now();
		for (int i = 0; i < data.length; i++) {
			for (int j = i + 1; j < data.length; j++) {
				double tempDistance = data[i].euclideanDistanceTo(data[j]);
				if (tempDistance > solutions[0].distance() && isNotInPairList(data[i], data[j], false)) {
					solutions[0] = new Solution(new PairPoint(data[i], data[j]),
							tempDistance,
							Duration.between(start, Instant.now()).toMillis());
					Arrays.sort(solutions,
							(solution1, solution2) -> Double.compare(solution1.distance(), solution2.distance()));
				}
			}
		}

		for (Solution solution : solutions) {
			Logger.getLogger(this.getClass().getSimpleName())
					.log(Level.INFO,
							"Maximum distance found is {0} between points {1} and {2} under {3} milliseconds.",
							new Object[] { solution.distance(), solution.pair().p1(), solution.pair().p2(),
									solution.time() });

			Body<Solution> body1 = new Body<>(RequestType.PUT, BodyCode.PAIR_POINTS, solution);
			this.hub.notifyRequest(new Request<>(RequestCode.NEW_PAIR_DATA_MAX, this, body1));
		}

		Object[] objects = new Object[2];
		// TODO: Change this to the request body system
		objects[0] = this.hub.getModel().getMaxPairPointsList();
		objects[1] = this.hub.getModel().getData();
		Body<Object[]> body = new Body<>(RequestType.PUT, BodyCode.PAIR_POINTS, objects);
		this.hub.notifyRequest(new Request<>(RequestCode.RESULT_MAX_DIS, this, body));
	}

}
