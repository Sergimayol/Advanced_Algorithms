package Controller;

import java.util.Random;
import java.util.function.DoubleSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import Master.MVC;
import Model.Point;
import Request.Body;
import Request.BodyCode;
import Request.Notify;
import Request.Request;
import Request.RequestCode;
import Request.RequestType;

import java.awt.Dimension;

public class Controller implements Notify {

	private MVC hub;
	private Random rng;

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

	private boolean bounded(double point) {
		return point < 1.0 && point > 0.0;
	}

	private void resetRNG() {
		this.rng = new Random(this.hub.getModel().getSeed());
	}

	@Override
	public void notifyRequest(Request<?> request) {
		resetRNG();
		Point[] data;
		switch (request.code) {
			case GENERATE_UNIFORM_DATA -> {
				data = generateData(
						rng::nextDouble,
						this.hub.getModel().getFrameDimension(),
						this.hub.getModel().getPointAmount());
			}
			case GENERATE_GAUSSIAN_DATA -> {
				data = generateData(
						this::nextBoundedGaussian,
						this.hub.getModel().getFrameDimension(),
						this.hub.getModel().getPointAmount());
			}
			default -> {
				Logger.getLogger(this.getClass().getSimpleName())
						.log(Level.SEVERE, "{0} is not implemented.", request);
				return;
			}
		}
		Body<Point[]> body = new Body<>(RequestType.PUT, BodyCode.DATA, data);
		this.hub.notifyRequest(new Request<>(RequestCode.NEW_DATA, this, body));
	}

}
