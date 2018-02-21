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
import wrdca.util.DissimMatrixDouble;



public class WTDHMTestPhoneme {
	public static void main(String[] args) throws IOException, IloException {
		DissimMatrixDouble l1 = parseFile(DataFileNames.getString("PhonemeDataset.ACC"));
		DissimMatrixDouble l2 = parseFile(DataFileNames.getString("PhonemeDataset.PHO"));
		DissimMatrixDouble l3 = parseFile(DataFileNames.getString("PhonemeDataset.VEL"));
		List<DissimMatrixDouble> dissimMatrices = new ArrayList<DissimMatrixDouble>(3);
		dissimMatrices.add(l1);
		dissimMatrices.add(l2);
		dissimMatrices.add(l3);
		
		final int NUMBER_OF_RUNS = 100;
		int k = 5;
		double bestJ = Double.MAX_VALUE;
		
		int aprioriClasses[] = new int[2000];
		for (int i = 0; i < aprioriClasses.length; i++) {
			aprioriClasses[i] = i/400;
		}
		List<Cluster> bestClusters = null;
		final int iterationCount[] = new int[NUMBER_OF_RUNS];
		long timeInMilis = System.currentTimeMillis();
		for (int i = 0; i < NUMBER_OF_RUNS; i++) {
			WTDHMClustering clust = new WTDHMClustering(dissimMatrices);
			//clust.setSeedType(WTDHMClustering.SeedingType.PLUSPLUS_SEED);
			clust.cluster(k);
			iterationCount[i] = clust.getIterationsToConverge();
			final List<Cluster> myClusters = clust.getClusters();
			final double myJ = clust.calcJ(myClusters);
			if (myJ < bestJ) {
				bestJ = myJ;
				bestClusters = myClusters;
			}
		}
		timeInMilis = System.currentTimeMillis() - timeInMilis;
		
		final String classNames[] = {"1-sh", "2-iy", "3-dcl", "4-aa", "5-ao"};
		ConfusionMatrix confusionMatrix = new ConfusionMatrix(k, 5, classNames);

		for (int i = 0; i < bestClusters.size(); i++) {
			Cluster cluster = bestClusters.get(i);
			for (Integer element : cluster.getElements()) {
				confusionMatrix.putObject(element, i, (element/400));
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
		System.out.println(">>>>>>>>>>>> The CR-Index  is: "+ confusionMatrix.CRIndex());
		System.out.println(">>>>>>>>>>>> OERC Index    is: " + confusionMatrix.OERCIndex());
		System.out.println(">>>>>>>>>>>> NMI  Index    is: " + confusionMatrix.nMIIndex());
		System.out.println("Iterations to converge: " + Arrays.toString(iterationCount));
		double iteravg = 0.0;
		for (int i = 0; i < iterationCount.length; i++) {
			iteravg += iterationCount[i];
		}
		iteravg /= iterationCount.length;
		System.out.println("Mean iterations: " + iteravg);
		System.out.println("Total time in seconds: " + timeInMilis/1000.0);
		
		
	}

	private static DissimMatrixDouble parseFile(String string) throws IOException {
		File file = new File(string);
		BufferedReader bufw = new BufferedReader(new FileReader(file));
		DissimMatrixDouble result = new DissimMatrixDouble(2000);
		for (int i = 0; i < 2000; i++) {
			String line = bufw.readLine();
			StringTokenizer sttok = new StringTokenizer(line, ",", false);
			for (int j = 0; j <= i; j++) {
				result.putDissim(i, j, Float.parseFloat(sttok.nextToken()));
			}
		}
		bufw.close();
		return result;
	}
}
