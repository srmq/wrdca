package wrdca.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import wrdca.algo.ClusterAlgorithm;
import wrdca.algo.WTDHMClustering;
import wrdca.algo.WTDHMGlobalClustering;
import wrdca.util.Cluster;
import wrdca.util.ConfusionMatrix;
import wrdca.util.DissimMatrix;
import wrdca.util.DissimMatrixDouble;
import wrdca.util.DissimMatrixFloat;
import wrdca.util.Pair;

public class WTDHMSimpleFormatRunner {
	
	private int k;
	private int numInicializacao;
	private int numIteracoes;
	private List<File> inputFiles;
	private File outputFile;
	private int n;
	private int numPrioriClusters;
	private List<DissimMatrix> dissimMatrices;
	private boolean useFloats;
	
	
	private WTDHMSimpleFormatRunner(String[] args) throws FileNotFoundException, IOException {
		this.useFloats = false;
		this.readConfigFile(args);
		this.parseDissimMatrices(this.useFloats);
	}
	
	public ClusterAlgorithm createLocalClusterAlgorithm(List<? extends DissimMatrix> dissimMatrices) {
		return new WTDHMClustering(dissimMatrices);
	}
	
	public ClusterAlgorithm createGlobalClusterAlgorithm(List<DissimMatrix> dissimMatrices) {
		return new WTDHMGlobalClustering(dissimMatrices);
	}
	

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.out.println("Should give name of config file as argument");
		} //args = 2 significa global
		WTDHMSimpleFormatRunner runner = new WTDHMSimpleFormatRunner(args);

		final Pair<int[], String[]> classLabelsObjectNames = runner.classLabelsForObjects();
		final int classlabels[] = classLabelsObjectNames.getFirst();
		final String[] objectNames = classLabelsObjectNames.getSecond();
		double bestJ = Double.MAX_VALUE;
		List<Cluster> bestClusters = null;
		final int iterationCount[] = new int[runner.numInicializacao];
		long timeInMilis = System.currentTimeMillis();
		PrintStream outStream;
		if (runner.outputFile == null) {
			outStream = System.out;
		} else {
			outStream = new PrintStream(runner.outputFile, "UTF-8");
		}

		ClusterAlgorithm clust=null;
		for (int i = 0; i < runner.numInicializacao; i++) {
			if (args.length == 1) {
				clust = runner.createLocalClusterAlgorithm(runner.dissimMatrices);
			} else {
				clust = runner.createGlobalClusterAlgorithm(runner.dissimMatrices);
				outStream.println("RUNNING GLOBAL");
			}
			if (runner.numIteracoes > 0) clust.setMaxIterations(runner.numIteracoes);
			//clust.setMaxWeightAbsoluteDifferenceGlobal(0.2);
			clust.cluster(runner.k);
			iterationCount[i] = clust.getIterationsToConverge();
			final List<Cluster> myClusters = clust.getClusters();
			final double myJ = clust.calcJ(myClusters);
			if (myJ < bestJ) {
				bestJ = myJ;
				bestClusters = myClusters;
			}
		}
		timeInMilis = System.currentTimeMillis() - timeInMilis;
		ConfusionMatrix confusionMatrix = new ConfusionMatrix(runner.k, runner.numPrioriClusters);
		outStream.println("------CLUSTER CONTENTS-------");
		for (int i = 0; i < bestClusters.size(); i++) {
			Cluster cluster = bestClusters.get(i);
			outStream.println("Cluster " + i);
			outStream.println("Center: " + objectNames[cluster.getCenter()]);
			outStream.println("-- BEGIN MEMBERS --");
			for (Integer element : cluster.getElements()) {
				final int classlabel = classlabels[element];
				confusionMatrix.putObject(element, i, classlabel);
				outStream.print(objectNames[element]);
				final int align = 40 - objectNames[element].length();
				for (int c = 0; c < Math.max(align, 1); c++)
					outStream.print(' ');
				double[] regrets = clust.regretsForElement(element, bestClusters);
				outStream.println(Arrays.toString(regrets));
			}
			outStream.println("-- END MEMBERS --");			
		}
		
		outStream.println("------CONFUSION MATRIX-------");
		confusionMatrix.printMatrix(outStream);
		outStream.println("-----------------------------");
		outStream.println("Cluster weights: ");
		for (int i = 0; i < bestClusters.size(); i++) {
			outStream.println(i + ": " + Arrays.toString(bestClusters.get(i).getWeights())); 
		}
		outStream.println(">>>>>>>>>>>> Best J is: "+ bestJ);
		outStream.println(">>>>>>>>>>>> The F-Measure is: "+ confusionMatrix.fMeasureGlobal());
		outStream.println(">>>>>>>>>>>> The CR-Index  is: "+ confusionMatrix.CRIndex());
		outStream.println(">>>>>>>>>>>> OERC Index    is: " + confusionMatrix.OERCIndex());
		outStream.println(">>>>>>>>>>>> NMI  Index    is: " + confusionMatrix.nMIIndex());;
		outStream.println("Iterations to converge: " + Arrays.toString(iterationCount));
		double iteravg = 0.0;
		for (int i = 0; i < iterationCount.length; i++) {
			iteravg += iterationCount[i];
		}
		iteravg /= iterationCount.length;
		outStream.println("Mean iterations: " + iteravg);
		outStream.println("Total time in seconds: " + timeInMilis/1000.0);
		outStream.flush();
		System.exit(0);
	}

	private Pair<int[], String[]> classLabelsForObjects() throws IOException{
		BufferedReader buf = new BufferedReader(new FileReader(this.inputFiles.get(0)));
		int[] classLabels = new int[this.n];
		String[] names = new String[this.n];
		for (int i = 0; i < n; i++) {
			final String line = buf.readLine();
			final StringTokenizer strtok = new StringTokenizer(line, ",");
			classLabels[i] = Integer.parseInt(strtok.nextToken());
			names[i] = strtok.nextToken();
		}

		buf.close();
		return new Pair<int[], String[]>(classLabels, names);
	}

	private void parseDissimMatrices(boolean useFloats) throws IOException{
		 this.dissimMatrices = new ArrayList<DissimMatrix>(this.inputFiles.size());
		 boolean firstFile = true;
		 Map<String, Integer> refOrder = null;
		 List<String> refNames = null;
		 for (File file : inputFiles) {
			Pair<DissimMatrix, List<String>> parseResult = parseDissimMatrix(file, useFloats);
			DissimMatrix dissim = parseResult.getFirst();
			if (firstFile) {
				refNames = parseResult.getSecond();
				refOrder = new HashMap<String, Integer>(refNames.size());
				int i = 0;
				for (String refName : refNames) {
					refOrder.put(refName, i);
					i++;
				}
				firstFile = false;
			} else {
				assert(refOrder != null);
				assert(refNames != null);
				if (!parseResult.getSecond().equals(refNames)) {
					dissim = reorderedDissim(refOrder, parseResult.getSecond(), dissim);
				}
			}
			this.dissimMatrices.add(dissim);
		}
	}


	private DissimMatrixDouble reorderedDissim(final Map<String, Integer> refOrder, List<String> second, DissimMatrix dissim) {
		List<Pair<String, Integer>> names = new ArrayList<Pair<String, Integer>>(second.size());
		{
			int i = 0;
			for (String name : second) {
				names.add(new Pair<String, Integer>(name, i));
				i++;
			}
		}
		names.sort(new Comparator<Pair<String, Integer>>() {

			public int compare(Pair<String, Integer> o1, Pair<String, Integer> o2) {
				assert(refOrder.containsKey(o1.getFirst()));
				assert(refOrder.containsKey(o2.getFirst()));
				return refOrder.get(o1.getFirst()) - refOrder.get(o2.getFirst());
			}
			
		});
		DissimMatrixDouble dissimMatrix = new DissimMatrixDouble(n);
		for (int i = 0; i < n; i++) {
			int trI = names.get(i).getSecond();
			for (int j = 0; j <= i; j++) {
				int trJ = names.get(j).getSecond();
				dissimMatrix.putDissim(i, j, dissim.getDissim(trI, trJ));
			}
		}
		
		return dissimMatrix;
	}
	
	private Pair<DissimMatrix, List<String>> parseDissimMatrix(File file, boolean useFloats) throws IOException {
		BufferedReader buf = new BufferedReader(new FileReader(file));
		List<String> objNames = new ArrayList<String>(n);
		for (int i = 0; i < n; i++) {
			String line = buf.readLine();
			line = line.substring(line.indexOf(',')+1);
			objNames.add(line);
		}
		DissimMatrix dissimMatrix = useFloats ? new DissimMatrixFloat(n) : new DissimMatrixDouble(n);
		for (int i = 0; i < n; i++) {
			final String line = buf.readLine();
			final StringTokenizer strtok = new StringTokenizer(line, ",");
			for(int j = 0; j <= i; j++) {
				final double dissim = Double.parseDouble(strtok.nextToken());
				dissimMatrix.putDissim(i, j, dissim);
			}
		}
		buf.close();
		return new Pair<DissimMatrix, List<String>>(dissimMatrix, objNames);
	}

	private void readConfigFile(String[] args)
			throws FileNotFoundException, IOException {
		File configFile = new File(args[0]);
		BufferedReader bufw = new BufferedReader(new FileReader(configFile));
		String line;
		while ((line = bufw.readLine()) != null) {
			if (line.contains("(numCluster)")) {
				k = Integer.parseInt(bufw.readLine());
			} else if (line.contains("(numInicializacao)")) {
				numInicializacao = Integer.parseInt(bufw.readLine());
			} else if (line.contains("(numIteracoes)")) {
				numIteracoes = Integer.parseInt(bufw.readLine());
			} else if (line.contains("(input)")) {
				inputFiles = new LinkedList<File>();
				while ((line = bufw.readLine()).length() > 0) {
					inputFiles.add(new File(line));
				}
			} else if (line.contains("(output)")) {
				outputFile = new File(bufw.readLine());
			} else if (line.contains("(numIndividuos)")) {
				n = Integer.parseInt(bufw.readLine());
			} else if (line.contains("(numPrioriClusters)")) {
				numPrioriClusters = Integer.parseInt(bufw.readLine());
			} else if (line.contains("(useDissimFloats)")) {
				this.useFloats = Integer.parseInt(bufw.readLine()) != 0;
			}
		}
		
		bufw.close();
	}

	
}
