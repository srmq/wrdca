package wrdca.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import wrdca.algo.ClusterAlgorithm;
import wrdca.algo.WTDHMClustering;
import wrdca.algo.WTDHMGlobalClustering;
import wrdca.util.Cluster;
import wrdca.util.ConfusionMatrix;
import wrdca.util.DissimMatrix;

public class WTDHMSimpleFormatRunner {
	
	private int k;
	private int numInicializacao;
	private int numIteracoes;
	private List<File> inputFiles;
	private File outputFile;
	private int n;
	private int numPrioriClusters;
	private List<DissimMatrix> dissimMatrices;
	
	
	private WTDHMSimpleFormatRunner(String[] args) throws FileNotFoundException, IOException {
		this.readConfigFile(args);
		this.parseDissimMatrices();
	}
	
	public ClusterAlgorithm createLocalClusterAlgorithm(List<DissimMatrix> dissimMatrices) {
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

		int classlabels[] = runner.classLabelsForObjects();		
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

		for (int i = 0; i < runner.numInicializacao; i++) {
			ClusterAlgorithm clust;
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
		for (int i = 0; i < bestClusters.size(); i++) {
			Cluster cluster = bestClusters.get(i);
			for (Integer element : cluster.getElements()) {
				final int classlabel = classlabels[element];
				confusionMatrix.putObject(element, i, classlabel);
			}
		}
		
		outStream.println("------CONFUSION MATRIX-------");
		confusionMatrix.printMatrix(outStream);
		outStream.println("-----------------------------");
		outStream.println("Cluster weights: ");
		for (int i = 0; i < bestClusters.size(); i++) {
			outStream.println(i + ": " + Arrays.toString(bestClusters.get(i).getWeights())); 
		}
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

	private int[] classLabelsForObjects() throws IOException{
		BufferedReader buf = new BufferedReader(new FileReader(this.inputFiles.get(0)));
		int[] classLabels = new int[this.n];
		for (int i = 0; i < n; i++) {
			final String line = buf.readLine();
			final StringTokenizer strtok = new StringTokenizer(line, ",");
			classLabels[i] = Integer.parseInt(strtok.nextToken());
		}

		buf.close();
		return classLabels;
	}

	private void parseDissimMatrices() throws IOException{
		 this.dissimMatrices = new ArrayList<DissimMatrix>(this.inputFiles.size());
		 for (File file : inputFiles) {
			DissimMatrix dissim = parseDissimMatrix(file);
			this.dissimMatrices.add(dissim);
		}
	}


	private DissimMatrix parseDissimMatrix(File file) throws IOException {
		BufferedReader buf = new BufferedReader(new FileReader(file));
		for (int i = 0; i < n; i++) {
			buf.readLine();
		}
		DissimMatrix dissimMatrix = new DissimMatrix(n);
		for (int i = 0; i < n; i++) {
			final String line = buf.readLine();
			final StringTokenizer strtok = new StringTokenizer(line, ",");
			for(int j = 0; j <= i; j++) {
				final double dissim = Double.parseDouble(strtok.nextToken());
				dissimMatrix.putDissim(i, j, dissim);
			}
		}
		buf.close();
		return dissimMatrix;
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
			}
		}
		
		bufw.close();
	}

	
}
