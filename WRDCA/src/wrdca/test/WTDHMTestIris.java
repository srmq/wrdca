/**
 * @author Sergio Queiroz <srmq@cin.ufpe.br>
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.

 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */
package wrdca.test;

import ilog.concert.IloException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import wrdca.algo.WTDHMClustering;
import wrdca.util.Cluster;
import wrdca.util.ConfusionMatrix;
import wrdca.util.DissimMatrix;



public class WTDHMTestIris {
	public static void main(String[] args) throws IOException, IloException {
		DissimMatrix l1 = parseFile(DataFileNames.getString("WTDHMTestIris.L1_DATAFILE")); //$NON-NLS-1$
		DissimMatrix l2 = parseFile(DataFileNames.getString("WTDHMTestIris.L2_DATAFILE")); //$NON-NLS-1$
		DissimMatrix l3 = parseFile(DataFileNames.getString("WTDHMTestIris.L3_DATAFILE")); //$NON-NLS-1$
		DissimMatrix l4 = parseFile(DataFileNames.getString("WTDHMTestIris.L4_DATAFILE")); //$NON-NLS-1$
		List<DissimMatrix> dissimMatrices = new ArrayList<DissimMatrix>(4);
		dissimMatrices.add(l1);
		dissimMatrices.add(l2);
		dissimMatrices.add(l3);
		dissimMatrices.add(l4);
		final int NUMBER_OF_RUNS = 100;
		int k = 3;
		double bestJ = Double.MAX_VALUE;
		double bestCR = 0;
		List<Cluster> bestClusters = null;
		int classlabels[] = new int[150];
		for (int i = 0; i < classlabels.length; i++) {
			classlabels[i] = i/50; 
		}
		final int iterationCount[] = new int[NUMBER_OF_RUNS];
		long timeInMilis = System.currentTimeMillis();
		for (int i = 0; i < NUMBER_OF_RUNS; i++) {
			//System.out.println("Run number " + i);
			WTDHMClustering clust = new WTDHMClustering(dissimMatrices);
			//clust.setSeedType(WTDHMClustering.SeedingType.PLUSPLUS_SEED);
			clust.cluster(k);
			iterationCount[i] = clust.getIterationsToConverge();
			final List<Cluster> myClusters = clust.getClusters();
			final double myJ = clust.calcJ(myClusters);
			if (myJ < bestJ) {
				bestJ = myJ;
				bestClusters = myClusters;
				bestCR = clust.calcCR(classlabels);
			}
		}
		timeInMilis = System.currentTimeMillis() - timeInMilis;
		
		String classNames[] = {"Iris setosa", "Iris versicolour", "Iris virginica"};
		ConfusionMatrix confusionMatrix = new ConfusionMatrix(k, 3, classNames);


		for (int i = 0; i < bestClusters.size(); i++) {
			Cluster cluster = bestClusters.get(i);
			for (Integer element : cluster.getElements()) {
				final int classlabel = (element/50);
				confusionMatrix.putObject(element, i, classlabel);
			}
		}
		System.out.println("------CONFUSION MATRIX-------");
		confusionMatrix.printMatrix(System.out);
		System.out.println("-----------------------------");
		System.out.println("Cluster weights: ");
		for (int i = 0; i < bestClusters.size(); i++) {
			System.out.println(i + ": " + Arrays.toString(bestClusters.get(i).getWeights())); 
		}
		System.out.println(">>>>>>>>>>>> The F-Measure is: "+ confusionMatrix.fMeasureGlobal());
		System.out.println(">>>>>>>>>>>> The CR-Index  is: "+ bestCR);
		System.out.println(">>>>>>>>>>>> OERC Index    is: " + confusionMatrix.OERCIndex());
		System.out.println(">>>>>>>>>>>> NMI  Index    is: " + confusionMatrix.nMIIndex());;
		System.out.println("Iterations to converge: " + Arrays.toString(iterationCount));
		double iteravg = 0.0;
		for (int i = 0; i < iterationCount.length; i++) {
			iteravg += iterationCount[i];
		}
		iteravg /= iterationCount.length;
		System.out.println("Mean iterations: " + iteravg);
		System.out.println("Total time in seconds: " + timeInMilis/1000.0);

	}

	private static DissimMatrix parseFile(String string) throws IOException {
		File file = new File(string);
		BufferedReader bufw = new BufferedReader(new FileReader(file));
		DissimMatrix result = new DissimMatrix(150);
		for (int i = 0; i < 150; i++) {
			String line = bufw.readLine();
			StringTokenizer sttok = new StringTokenizer(line, ",", false); //$NON-NLS-1$
			for (int j = 0; j <= i; j++) {
				result.putDissim(i, j, Float.parseFloat(sttok.nextToken()));
			}
		}
		bufw.close();
		return result;
	}
}
