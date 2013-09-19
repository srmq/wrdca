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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import wrdca.algo.WTDHMClustering;
import wrdca.util.Cluster;
import wrdca.util.ConfusionMatrix;
import wrdca.util.DissimMatrix;



public class WTDHMTestCar {
	public static final int NUMBER_LINES = 1728;
	public static final int NUMBER_COLUMNS = 7;
	public static final int NUMBER_OF_RUNS = 30;

	public static void main(String[] args) throws IOException, IloException {
		byte[][] objectDescription = objectDescription();
		int[] classLabels = getClassLabels(objectDescription);
		List<DissimMatrix> dissimMatrices = computeDissims(objectDescription);
		
		for(int k = 4; k <= 4; k++) { //fixme k = 1
			double bestJ = Double.MAX_VALUE;
			double bestCR = -1;
			List<Cluster> bestClusters = null;
			for (int i = 0; i < NUMBER_OF_RUNS; i++) {
				WTDHMClustering clust = new WTDHMClustering(dissimMatrices);
				clust.setSeedType(WTDHMClustering.SeedingType.PLUSPLUS_SEED);
				clust.cluster(k);
				final List<Cluster> myClusters = clust.getClusters();
				final double myJ = clust.calcJ(myClusters);
				if (myJ < bestJ) {
					bestJ = myJ;
					bestCR = clust.calcCR(classLabels);
					bestClusters = myClusters;
				}
			}

			System.out.println("---------K = " + k + " ----------------------"); //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println("Best J is: " + bestJ); //$NON-NLS-1$
			
			System.out.println("-------------------------------"); //$NON-NLS-1$
			System.out.println("Best J is: " + bestJ); //$NON-NLS-1$
			System.out.println("CR is: " + bestCR); //$NON-NLS-1$
			String[] classes = {"unacc", "acc", "good", "vgood"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			int[][] clusterMatrix = new int[k][classes.length];
			
			ConfusionMatrix confusionMatrix = new ConfusionMatrix(k, 4);
			for (int i = 0; i < k; i++) {
				Cluster c = bestClusters.get(i);
				for (Integer el : c.getElements()) {
					clusterMatrix[i][objectDescription[el][6]]++;
					confusionMatrix.putObject(el, i, classLabels[el]);
				}
			}
			System.out.println(" ,unacc,acc,good,vgood"); //$NON-NLS-1$
			for (int i = 0; i < k; i++) {
				System.out.print(i);
				for (int j = 0; j < classes.length; j++) {
					System.out.print("," + clusterMatrix[i][j]); //$NON-NLS-1$
				}
				System.out.println(""); //$NON-NLS-1$
			}
			System.out.println(">>>>>>>>>>>> The F-Measure is: "+ confusionMatrix.fMeasureGlobal());
			System.out.println("F-measure for R baseline is:   "+ parseLogRKMeans(k).fMeasureGlobal());
			
		}
	}
	
	private static int[] getClassLabels(byte[][] objectDescription) {
		final int[] result = new int[objectDescription.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = objectDescription[i][6];
		}
		return result;
	}

	private static List<DissimMatrix> computeDissims(byte[][] objectDescription) {
		List<DissimMatrix> result = new ArrayList<DissimMatrix>(NUMBER_COLUMNS - 1);
		for (int i = 0; i < NUMBER_COLUMNS - 1; i++) {
			DissimMatrix dissimM = new DissimMatrix(objectDescription.length);
			for (int el = 0; el < objectDescription.length; el++) {
				for (int j = 0; j <= el; j++) {
					dissimM.putDissim(el, j, calcDissim(el, j, i, objectDescription));
				}
				
			}
			result.add(dissimM);
		}
		return result;
	}

	private static float calcDissim(int el1, int el2, int criterionIndex,
			byte[][] objectDescription) {
		final double[] normQuocients = {3.0, 3.0, 3.0, 2.0, 2.0, 2.0};
		double result = (objectDescription[el1][criterionIndex] - objectDescription[el2][criterionIndex])/normQuocients[criterionIndex];
		return (float)Math.abs(result);
	}

	@SuppressWarnings("unchecked")
	private static byte[][] objectDescription() throws IOException {
		byte[][] result;
		File file = new File(DataFileNames.getString("WTDHMTestCar.TEST_CAR_DATAFILE")); //$NON-NLS-1$
		BufferedReader bufw = new BufferedReader(new FileReader(file));
		
		result = new byte[NUMBER_LINES][NUMBER_COLUMNS];
		Map<String,Byte>[] map = new Map[NUMBER_COLUMNS];
		
		map[0] = new HashMap<String,Byte>(4);
		map[0].put("vhigh", (byte)0); //$NON-NLS-1$
		map[0].put("high", (byte)1); //$NON-NLS-1$
		map[0].put("med", (byte)2); //$NON-NLS-1$
		map[0].put("low", (byte)3); //$NON-NLS-1$
		
		map[1] = map[0];
		
		map[2] = new HashMap<String,Byte>(4);
		map[2].put("2", (byte)0); //$NON-NLS-1$
		map[2].put("3", (byte)1); //$NON-NLS-1$
		map[2].put("4", (byte)2); //$NON-NLS-1$
		map[2].put("5more", (byte)3); //$NON-NLS-1$

		map[3] = new HashMap<String,Byte>(3);
		map[3].put("2", (byte)0); //$NON-NLS-1$
		map[3].put("4", (byte)1); //$NON-NLS-1$
		map[3].put("more", (byte)2); //$NON-NLS-1$

		map[4] = new HashMap<String,Byte>(3);
		map[4].put("small", (byte)0); //$NON-NLS-1$
		map[4].put("med", (byte)1); //$NON-NLS-1$
		map[4].put("big", (byte)2); //$NON-NLS-1$
		
		map[5] = new HashMap<String,Byte>(3);
		map[5].put("low", (byte)0); //$NON-NLS-1$
		map[5].put("med", (byte)1); //$NON-NLS-1$
		map[5].put("high", (byte)2); //$NON-NLS-1$
		
		map[6] = new HashMap<String,Byte>(4);
		map[6].put("unacc", (byte)0); //$NON-NLS-1$
		map[6].put("acc", (byte)1); //$NON-NLS-1$
		map[6].put("good", (byte)2); //$NON-NLS-1$
		map[6].put("vgood", (byte)3); //$NON-NLS-1$
		
		
		for (int i = 0; i < NUMBER_LINES; i++) {
			String line = bufw.readLine();
			StringTokenizer sttok = new StringTokenizer(line, ",", false); //$NON-NLS-1$
			for (int j = 0; j < NUMBER_COLUMNS; j++) {
				result[i][j] = map[j].get(sttok.nextToken());
			}
		}
		bufw.close();
		return result;
	}
	
	public static ConfusionMatrix parseLogRKMeans(int k) throws IOException {
		File logFile = new File("/home/srmq/Dropbox/CIn/research/inria/dados/car-data/car-num.data-RKmeans-4classes");
		int numbAprioriClasses = 4;
		BufferedReader bufReader = new BufferedReader(new FileReader(logFile));
		String line;
		bufReader.readLine();
		int ord = -1;
		int aprioriclass = 0;
		ConfusionMatrix result = new ConfusionMatrix(k, numbAprioriClasses);
		while((line = bufReader.readLine()) != null) {
			ord++;
			if (ord == 384) aprioriclass++;
			if (ord == 453) aprioriclass++;
			if (ord == 1663) aprioriclass++;
			int clusterNumber = Integer.parseInt(line.substring(line.indexOf(',')+1));
			result.putObject(ord+1, clusterNumber-1, aprioriclass);
			
		}
		bufReader.close();
		return result;
	}
	
}
