package Controller;

import Services.Comunication.Content.Body;
import Services.Comunication.Request.Request;
import Services.Comunication.Request.RequestCode;
import Services.Comunication.Response.Response;
import Services.Comunication.Response.ResponseCode;
import utils.Config;
import utils.Helpers;
import Services.Service;

import java.io.BufferedReader;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import Model.ExecResultData;
import Model.ExecResultDataTreeNode;

import java.util.List;

public class Controller implements Service {

	private Map<String, Double> lang;
	private String[] words;
	private NaiveBayesClassifier model;
	private String sentence;

	public Controller() {
		this.lang = new HashMap<>();
	}

	@Override
	public void start() {
		Logger
				.getLogger(this.getClass().getSimpleName())
				.log(Level.INFO, "Controller started.");
	}

	@Override
	public void stop() {
		Logger
				.getLogger(this.getClass().getSimpleName())
				.log(Level.INFO, "Controller stopped.");
	}

	private int levenshtein(String source, String target) {
		int m = source.length();
		int n = target.length();

		int[][] memo = new int[m + 1][n + 1];

		for (int i = 0; i <= m; i++) {
			memo[i][0] = i;
		}

		for (int j = 0; j <= n; j++) {
			memo[0][j] = j;
		}

		for (int i = 1; i <= m; i++) {
			for (int j = 1; j <= n; j++) {
				if (source.charAt(i - 1) == target.charAt(j - 1)) {
					memo[i][j] = memo[i - 1][j - 1]; // No operation needed
					continue;
				}

				int deletion = memo[i - 1][j] + 1;
				int insertion = memo[i][j - 1] + 1;
				int substitution = memo[i - 1][j - 1] + 1;
				memo[i][j] = Math.min(Math.min(deletion, insertion), substitution);
			}
		}

		return memo[m][n];
	}

	private double levenshtein(String[] source, String[] target) {
		double score = 0;
		for (String sourceWord : source) {
			int tmpScore = Integer.MAX_VALUE;
			for (String targetWord : target) {
				tmpScore = Math.min(tmpScore, levenshtein(sourceWord, targetWord));
			}
			score += (double) tmpScore / sourceWord.length();
		}
		return score / source.length;
	}

	private Map<String, Double> levenshtein(String[] languages, boolean isParallel, int batchSize) {
		for (int i = 0; i < languages.length; i++) {
			for (int j = 0; j < languages.length; j++) {
				if (i == j) {
					continue;
				}

				// Increment the counter by 1
				Helpers.syncCount.inc();

				lang.put(languages[i] + "-" + languages[j], 0.0);

				// Create a request to fetch those two languages words, then the calculation
				// will be done within the request
				Body body = new Body(new Object[] { languages[i], languages[j], batchSize });
				Request request = new Request(RequestCode.FETCH_LANGS, this, body);
				this.sendRequest(request);

				// Barrera polling
				if (!isParallel) {
					while (Helpers.syncCount.get() != 0) {
						Helpers.await();
					}
				}
			}
		}

		// Barrera polling
		if (isParallel) {
			while (Helpers.syncCount.get() != 0) {
				Helpers.await();
			}
		}

		// All langs were calculated. Get pairs and calculate the euclidean distance
		// from their scores
		return mergeLangScores(this.lang);
	}

	private double euclideanDistance(double x, double y) {
		return Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
	}

	@SuppressWarnings("unchecked")
	private NaiveBayesClassifier createAndSaveModel(String pathToData) {
		Object[] data = this.createDataset(pathToData);
		List<String> trainingData = (List<String>) data[0];
		List<String> trainingLabels = (List<String>) data[1];

		NaiveBayesClassifier m = new NaiveBayesClassifier();
		m.train(trainingData, trainingLabels);
		// Save the model to disk
		m.saveModel(Config.NAIVE_BAYES_MODEL_PATH);
		return m;
	}

	private Object[] createDataset(String path) {
		List<String> trainingData = new ArrayList<>();
		List<String> trainingLabels = new ArrayList<>();

		File folder = new File(path);
		File[] listOfFiles = folder.listFiles();

		for (File file : listOfFiles) {
			try (BufferedReader br = new BufferedReader(new java.io.FileReader(file))) {
				String line;
				while ((line = br.readLine()) != null) {
					trainingData.add(line);
					trainingLabels.add(file.getName().replace(".dic", ""));
				}
			} catch (Exception e) {
				Logger.getLogger(this.getClass().getSimpleName()).log(Level.SEVERE, e.getMessage(), e);
			}
		}

		return new Object[] { trainingData, trainingLabels };
	}

	private ExecResultData[] resultToGraphData(Map<String, Double> result) {
		List<ExecResultData> data = new ArrayList<>();

		Set<String> langs = this.getIdLangs(result);

		for (String lang1 : langs) {
			final List<ExecResultData.Connection> connections = new ArrayList<>();
			for (String lang2 : langs) {
				if (lang1.equals(lang2)) {
					continue;
				}

				final String key = String.format("%s-%s", lang1, lang2);
				final String key2 = String.format("%s-%s", lang2, lang1);

				ExecResultData.Connection connection = result.containsKey(key)
						? new ExecResultData.Connection(lang2, result.get(key))
						: new ExecResultData.Connection(lang2, result.get(key2));

				connections.add(connection);
			}
			data.add(new ExecResultData(lang1, connections.toArray(ExecResultData.Connection[]::new)));
		}

		return data.toArray(ExecResultData[]::new);
	}

	private ExecResultDataTreeNode resultToTreeData(Map<String, Double> result, ExecResultData[] graph) {
		List<ExecResultData.Edge> mst = new ArrayList<>();
		List<ExecResultData.Edge> edges = new ArrayList<>();
		for (ExecResultData data : graph) {
			for (ExecResultData.Connection connection : data.connections()) {
				edges.add(new ExecResultData.Edge(data.id(), connection.id(), connection.value()));
			}
		}

		// Sort edges by value in ascending order
		Collections.sort(edges);

		// Create a parent array to track the subset of each node
		Map<String, Integer> parent = new HashMap<>();
		int[] auxParent = new int[graph.length];
		for (int i = 0; i < graph.length; i++) {
			parent.put(graph[i].id(), i);
			auxParent[i] = i;
		}

		int edgeCount = 0;
		int index = 0;

		while (edgeCount < graph.length - 1) {
			ExecResultData.Edge actualEdge = edges.get(index);

			// Find the subset of the source and destination
			int srcParent = findParent(auxParent, parent.get(actualEdge.src()));
			int dstParent = findParent(auxParent, parent.get(actualEdge.dst()));

			// Check if including this edge forms a cycle or not
			if (srcParent != dstParent) {
				mst.add(actualEdge);
				edgeCount++;
				auxParent[srcParent] = dstParent;
			}
			index++;
		}

		// Convert to ExecResultDataTreeNode
		Set<String> idSets = this.getIdLangs(result);
		ArrayList<ExecResultDataTreeNode> treeNodes = new ArrayList<>();

		for (String id : idSets) {
			treeNodes.add(new ExecResultDataTreeNode(id, new ExecResultDataTreeNode[0]));
		}

		for (ExecResultData.Edge edge : mst) {
			String src = getContainedId(idSets, edge.src());
			idSets.remove(src);

			String dst = getContainedId(idSets, edge.dst());
			idSets.remove(dst);

			String mergedId = src + "-" + dst;
			ExecResultDataTreeNode mergedNode = new ExecResultDataTreeNode(mergedId,
					new ExecResultDataTreeNode[] {
							getTreeNodeById(src, treeNodes),
							getTreeNodeById(dst, treeNodes) });
			treeNodes.add(mergedNode);
			idSets.add(mergedId);
		}

		return treeNodes.get(treeNodes.size() - 1);
	}

	private String getContainedId(Set<String> set, String id) {
		for (String possibleId : set) {
			if (possibleId.contains(id)) {
				return possibleId;
			}
		}

		return "";
	}

	private ExecResultDataTreeNode getTreeNodeById(String id, List<ExecResultDataTreeNode> nodes) {
		for (ExecResultDataTreeNode execResultDataTreeNode : nodes) {
			if (execResultDataTreeNode.id().equals(id)) {
				return execResultDataTreeNode;
			}
		}

		return null;
	}

	private int findParent(int[] parent, int vertex) {
		if (parent[vertex] != vertex) {
			parent[vertex] = findParent(parent, parent[vertex]);
		}
		return parent[vertex];
	}

	private Set<String> getIdLangs(Map<String, Double> result) {
		Set<String> langs = new HashSet<>();

		for (Map.Entry<String, Double> entry : result.entrySet()) {
			String[] pair = entry.getKey().split("-");
			langs.add(pair[0]);
			langs.add(pair[1]);
		}

		return langs;
	}

	private Map<String, Double> mergeLangScores(Map<String, Double> langScores) {

		Map<String, Double> scores = new HashMap<>(langScores);
		Map<String, Double> langScore = new HashMap<>();
		while (!scores.entrySet().isEmpty()) {
			final Map.Entry<String, Double> entry1 = scores.entrySet().iterator().next();
			final Double score1 = entry1.getValue();
			scores.remove(entry1.getKey());

			final String[] pair1 = entry1.getKey().split("-");
			final String key2 = String.format("%s-%s", pair1[1], pair1[0]);
			final Double score2 = scores.get(key2);
			scores.remove(key2);

			langScore.put(key2, euclideanDistance(score1, score2));
		}

		return langScore;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void notifyRequest(Request request) {
		switch (request.code) {
			case FETCH_LANGS -> {
				final Object[] paramenters = (Object[]) request.body.content;
				final String langName = (String) paramenters[0];
				final String[] sourceWords = (String[]) paramenters[1];
				final String[] targetWords = (String[]) paramenters[2];
				lang.put(langName, levenshtein(sourceWords, targetWords));
				Helpers.syncCount.dec();
			}
			case LEVENSHTEIN -> {
				final Object[] parameters = (Object[]) request.body.content;
				final String[] langNames = (String[]) parameters[0];
				final Map<String, Integer> options = (HashMap<String, Integer>) parameters[1];

				this.lang.clear();

				final Instant start = Instant.now();
				final Map<String, Double> results = this.levenshtein(langNames, options.get("parallel") == 1,
						options.get("batchSize"));
				final Duration duration = Duration.between(start, Instant.now());

				final ExecResultData[] graphData = resultToGraphData(results);
				final ExecResultDataTreeNode treeData = resultToTreeData(results, graphData);

				final Body body = new Body(new Object[] { duration, graphData, treeData });
				this.sendRequest(new Request(RequestCode.ADD_RESULT, this, body));
			}
			case GET_ALL_LANGS -> {
				final Object[] parameters = (Object[]) request.body.content;
				final String[][] langWords = (String[][]) parameters[0];
				final String[] langNames = (String[]) parameters[1];

				final Instant start = Instant.now();
				Map<String, Double> result = new HashMap<>();

				// We create a division of the data, as we need different words for each
				// iteration.
				// As to not add more complexity to the software, we get double the words and
				// slice it by half.
				for (int i = 0; i < langWords.length; i++) {
					result.put("CUSTOM-" + langNames[i],
							levenshtein(this.words, Arrays.copyOfRange(langWords[i], 0, langWords[i].length / 2)));
					result.put(langNames[i] + "-CUSTOM",
							levenshtein(Arrays.copyOfRange(langWords[i], langWords[i].length / 2, langWords[i].length),
									this.words));
				}

				final Map<String, Double> mergedResult = mergeLangScores(result);
				final Object[] bayesRes = this.model.classify(this.sentence);
				final Duration duration = Duration.between(start, Instant.now());

				Body body = new Body(new Object[] { duration, mergedResult, bayesRes });
				this.sendResponse(new Response(ResponseCode.GUESS_LANG, this, body));
			}
			case GUESS_LANG -> {
				if (Objects.isNull(this.model)) {
					this.model = NaiveBayesClassifier.loadModel(Config.NAIVE_BAYES_MODEL_PATH);
				}
				this.sentence = (String) request.body.content;
				this.words = ((String) request.body.content).split(" ");
				this.sendRequest(new Request(RequestCode.GET_ALL_LANGS, this));
			}
			case TRAIN_NAIVE_MODEL -> {
				this.model = this.createAndSaveModel(Config.PATH_TO_RAW_DATA);
			}
			case LOAD_MODEL_FROM_DB -> {
				this.model = NaiveBayesClassifier.loadModel(Config.NAIVE_BAYES_MODEL_PATH);
				Logger.getLogger(this.getClass().getSimpleName()).log(Level.INFO, "Model loaded from DB.");
			}
			default -> {
				Logger.getLogger(this.getClass().getSimpleName())
						.log(Level.SEVERE, "{0} is not implemented.", request);
			}
		}
	}
}
