package Controller;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import Model.KeyPair;
import Model.PrivateKey;
import Model.PublicKey;
import Model.Result;
import Services.Service;
import Services.Comunication.Content.Body;
import Services.Comunication.Request.Request;
import Services.Comunication.Response.Response;
import Services.Comunication.Response.ResponseCode;
import mesurament.Mesurament;
import utils.Config;

public class Controller implements Service {

	private final Logger logger = Logger.getLogger(this.getClass().getSimpleName());

	private static final int BASE_ITERATIONS = 10;
	private static final int BASE_SEED = 27;

	public Controller() {
		// Initialize controller things here
	}

	@Override
	public void start() {
		logger.log(Level.INFO, "Controller started.");
	}

	@Override
	public void stop() {
		logger.log(Level.INFO, "Controller stopped.");
	}

	// https://planetcalc.com/9023/?xy=5%2010%0A7%2023%0A8%2061%0A10%20165%0A11%20428%0A14%2016596%0A15%2037864&interpolate=10%2020%2030%2040
	private Duration getEstimatedTime(int length) {
		final BigInteger x = BigInteger.valueOf(length);
		BigInteger numerator1 = new BigInteger("5939");
		BigInteger denominator1 = new BigInteger("907200");
		BigInteger term1 = numerator1.multiply(x.pow(6)).divide(denominator1);

		BigInteger numerator2 = new BigInteger("409711");
		BigInteger denominator2 = new BigInteger("181440");
		BigInteger term2 = numerator2.multiply(x.pow(5)).divide(denominator2);

		BigInteger numerator3 = new BigInteger("-17477987");
		BigInteger denominator3 = new BigInteger("181440");
		BigInteger term3 = numerator3.multiply(x.pow(4)).divide(denominator3);

		BigInteger numerator4 = new BigInteger("40090411");
		BigInteger denominator4 = new BigInteger("25920");
		BigInteger term4 = numerator4.multiply(x.pow(3)).divide(denominator4);

		BigInteger numerator5 = new BigInteger("-2739635491");
		BigInteger denominator5 = new BigInteger("226800");
		BigInteger term5 = numerator5.multiply(x.pow(2)).divide(denominator5);

		BigInteger numerator6 = new BigInteger("2092430983");
		BigInteger denominator6 = new BigInteger("45360");
		BigInteger term6 = numerator6.multiply(x).divide(denominator6);

		BigInteger numerator7 = new BigInteger("-3722729");
		BigInteger denominator7 = new BigInteger("54");
		BigInteger term7 = numerator7.divide(denominator7);

		BigInteger result = term1.add(term2).add(term3).add(term4)
				.add(term5).add(term6).add(term7);

		return Duration.ofMillis(result.longValue());
	}

	private Map<BigInteger, BigInteger> getFactors(String number) {
		Map<BigInteger, BigInteger> primeFactors = new HashMap<>();
		BigInteger num = new BigInteger(number);
		BigInteger divisor = BigInteger.valueOf(2);

		while (num.compareTo(BigInteger.ONE) > 0) {

			if (num.isProbablePrime(100)) {
				primeFactors.put(num, BigInteger.ONE);
				break;
			}

			if (!num.remainder(divisor).equals(BigInteger.ZERO)) {
				divisor = divisor.nextProbablePrime();
				continue;
			}

			primeFactors.put(divisor, primeFactors.getOrDefault(divisor, BigInteger.ZERO).add(BigInteger.ONE));
			num = num.divide(divisor);
		}

		return primeFactors;
	}

	private String getMesurament() {
		// Crear un stream de salida en memoria para capturar la salida de System.out
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		PrintStream printStream = new PrintStream(outputStream);

		// Guardar la salida estándar actual
		PrintStream originalPrintStream = System.out;

		// Redirigir la salida a nuestro stream de salida en memoria
		System.setOut(printStream);

		// Llamar al método mesura()
		Mesurament.mesura();

		// Restaurar la salida estándar original
		System.setOut(originalPrintStream);

		// Obtener el resultado del stream de salida en memoria
		String output = outputStream.toString();

		// Procesar el resultado para extraer el valor del ratio
		String ratioString = output.split(":")[1].trim();
		ratioString = ratioString.replace("*", "").trim();

		logger.info(ratioString);

		return ratioString;
	}

	private long[] populateDB() {
		final int TOP_NUM_OF_DIGITS = 3200;
		long[] numbers = new long[TOP_NUM_OF_DIGITS];

		for (int i = 1; i <= TOP_NUM_OF_DIGITS; i++) {
			numbers[i - 1] = getEstimatedTime(i).toHours();
		}

		return numbers;
	}

	private void writeToFile(String filename, String content) {
		try (BufferedWriter br = new BufferedWriter(new FileWriter(filename))) {
			br.write(content);
		} catch (Exception e) {
			logger.warning(e.getMessage());
		}
	}

	@Override
	public void notifyRequest(Request request) {
		switch (request.code) {
			case POPULATE_DB -> {
				long[] numbers = populateDB();
				this.sendResponse(new Response(ResponseCode.POPULATE_DB, this, new Body(numbers)));
			}
			case GET_MESURAMENT -> {
				this.sendResponse(new Response(ResponseCode.GET_MESURAMENT, this, new Body(getMesurament())));
			}
			case GET_FACTORS -> {
				final String number = (String) request.body.content;
				final Duration expectedTime = getEstimatedTime(number.length());
				logger.info("expectedTime: " + expectedTime.toMinutes() + "mins");
				if (expectedTime.toMinutes() >= 1 || expectedTime.toMinutes() < 0) {
					this.sendResponse(new Response(ResponseCode.GET_FACTORS, this,
							new Body(new Result(expectedTime, Collections.emptyMap()))));
				}
				final Instant start = Instant.now();
				final Map<BigInteger, BigInteger> primeFactors = getFactors(number);
				final Instant end = Instant.now();

				this.sendResponse(new Response(ResponseCode.GET_FACTORS, this,
						new Body(new Result(Duration.between(start, end), primeFactors))));
			}
			case DECRYPT_FILE -> {
				logger.info("Decrypting file...");
				final Object[] params = (Object[]) request.body.content;
				final File file = (File) params[0];
				final PrivateKey privateKey = (PrivateKey) params[1];

				Result result = null;
				try {
					final Instant start = Instant.now();
					final String decryptedFile = privateKey.decrypt(file);
					final Instant end = Instant.now();
					result = new Result(Duration.between(start, end), decryptedFile);
				} catch (IOException e) {
					e.printStackTrace();
				}

				this.sendResponse(new Response(ResponseCode.DECRYPT_FILE, this, new Body(result)));
			}
			case ENCRYPT_FILE -> {
				logger.info("Encrypting file...");
				final Object[] params = (Object[]) request.body.content;
				final File file = (File) params[0];
				final PublicKey publicKey = (PublicKey) params[1];

				Result result = null;
				try {
					final Instant start = Instant.now();
					final String encryptedFile = publicKey.encrypt(file);
					final Instant end = Instant.now();
					result = new Result(Duration.between(start, end), encryptedFile);
				} catch (IOException e) {
					e.printStackTrace();
				}

				this.sendResponse(new Response(ResponseCode.ENCRYPT_FILE, this, new Body(result)));
			}
			case GENERATE_RSA_KEYS -> {
				logger.info("Generating RSA keys...");
				final Object[] params = (Object[]) request.body.content;
				final int keyLength = (int) params[0];
				final int seed = (int) params[1];
				final KeyPair kp = Cryptography.generateRSAKeyPair(
						Cryptography.generatePrime(keyLength, seed).toString(),
						Cryptography.generatePrime(keyLength, seed + 1).toString(), seed);

				this.sendResponse(new Response(ResponseCode.GENERATE_RSA_KEYS, this, new Body(kp)));
				this.writeToFile(Config.PUBLIC_KEY_FILE_NAME, kp.publicKey().toString());
				this.writeToFile(Config.PRIVATE_KEY_FILE_NAME, kp.privateKey().toString());
			}
			case CHECK_PRIMALITY -> {
				final Object[] params = (Object[]) request.body.content;
				final PrimalityFunction function = (PrimalityFunction) params[0];
				final BigInteger number = new BigInteger((String) params[1]);

				Instant start = Instant.now();
				boolean isPrime = switch (function) {
					case TRIAL_DIVISION -> PrimalityTest.trialDivision(number);
					case FERMAT -> {
						int iterations = Controller.BASE_ITERATIONS;
						try {
							iterations = (Integer) params[2];
						} catch (Exception e) {
							logger.log(Level.INFO, "Set iteratios to fallback value of {0}",
									Controller.BASE_ITERATIONS);
						}

						int seed = Controller.BASE_SEED;
						try {
							seed = (Integer) params[3];
						} catch (Exception e) {
							logger.log(Level.INFO, "Set seed to fallback value of {0}",
									Controller.BASE_SEED);
						}
						yield PrimalityTest.fermat(number, iterations, seed);
					}
					case MILLER_RABIN -> {
						int iterations = Controller.BASE_ITERATIONS;
						try {
							iterations = (Integer) params[2];
						} catch (Exception e) {
							logger.log(Level.INFO, "Set iteratios to fallback value of {0}",
									Controller.BASE_ITERATIONS);
						}

						int seed = Controller.BASE_SEED;
						try {
							seed = (Integer) params[3];
						} catch (Exception e) {
							logger.log(Level.INFO, "Set seed to fallback value of {0}",
									Controller.BASE_SEED);
						}
						yield PrimalityTest.millerRabin(number, iterations, seed);
					}
					case MILLER_RABIN_PARALLEL -> {
						int iterations = Controller.BASE_ITERATIONS;
						try {
							iterations = (Integer) params[2];
						} catch (Exception e) {
							logger.log(Level.INFO, "Set iteratios to fallback value of {0}",
									Controller.BASE_ITERATIONS);
						}

						int seed = Controller.BASE_SEED;
						try {
							seed = (Integer) params[3];
						} catch (Exception e) {
							logger.log(Level.INFO, "Set seed to fallback value of {0}",
									Controller.BASE_SEED);
						}
						yield PrimalityTest.millerRabinParallel(number, iterations, seed);
					}
					case TRIAL_DIVISION_PARALLEL -> PrimalityTest.trialDivisionParallel(number);
				};

				Instant end = Instant.now();

				this.sendResponse(new Response(ResponseCode.CHECK_PRIMALITY, this,
						new Body(new Result(Duration.between(start, end), isPrime))));
			}
			default -> {
				Logger.getLogger(this.getClass().getSimpleName())
						.log(Level.SEVERE, "{0} is not implemented.", request);
			}
		}
	}

}
