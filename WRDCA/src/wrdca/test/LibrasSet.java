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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import wrdca.algo.ClusterAlgorithm;
import wrdca.algo.WTDHMClustering;
import wrdca.util.Cluster;
import wrdca.util.DissimMatrixDouble;



public class LibrasSet {
	public static final int NUMBER_OF_CRITERIA = 90;
	public static final int NUMBER_OF_RUNS = 30;

	public static void main(String[] args)  throws Exception {
		List<ClusterItem> itens = parseFile(DataFileNames.getString("LibrasSet.DATAFILE")); //$NON-NLS-1$);
		List<DissimMatrixDouble> dissimMatrices = computeDissims(itens);
		
		int k= 15;
		
		double bestJ = Double.MAX_VALUE;
		List<Cluster> bestClusters = null;
		for (int i = 0; i < NUMBER_OF_RUNS; i++) {
			System.out.println("Starting run " + (i+1));
			ClusterAlgorithm clust = new WTDHMClustering(dissimMatrices);
			clust.cluster(k);
			final List<Cluster> myClusters = clust.getClusters();
			final double myJ = clust.calcJ(myClusters);
			if (myJ < bestJ) {
				bestJ = myJ;
				bestClusters = myClusters;
			}
		}
		System.out.println("---------K = " + k + " ----------------------"); //$NON-NLS-1$ //$NON-NLS-2$
		System.out.println("Best J is: " + bestJ); //$NON-NLS-1$
		for (int i = 0; i < k; i++) {
			Cluster c = bestClusters.get(i);
			System.out.print("Cluster " + i + ", center (" + c.getCenter() + ", " + itens.get(c.getCenter()).getClassName() + "):"); //$NON-NLS-1$ //$NON-NLS-2$
			
			for (Integer el : c.getElements()) {
				System.out.print("|" + el + ", " + itens.get(el).getClassName());  //$NON-NLS-1$
			}
			System.out.println(""); //$NON-NLS-1$
			System.out.print("  * Weights:"); //$NON-NLS-1$
			for (int j = 0; j < c.getWeights().length; j++) {
				System.out.print(" (" + j + ", " + c.getWeights()[j] + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			System.out.println(""); //$NON-NLS-1$
		}
		
		
		System.exit(0);
	}
	
	private static class ClusterItem {
		private float[] features;
		private int className;
		
		public ClusterItem(float[] f, int classif) {
			this.features = f;
			this.className = classif;
		}

		public float[] getFeatures() {
			return features;
		}

		public int getClassName() {
			return className;
		}
	}
	
	private static List<ClusterItem> parseFile(String fileString) throws IOException {
		List<ClusterItem> result = new LinkedList<ClusterItem>();
		File file = new File(fileString);
		BufferedReader bufw = new BufferedReader(new FileReader(file));

		String line;
		while ((line = bufw.readLine()) != null) {
			StringTokenizer sttok = new StringTokenizer(line, ",", false); //$NON-NLS-1$
			float[] features = new float[NUMBER_OF_CRITERIA];
			for (int i = 0; i < NUMBER_OF_CRITERIA; i++) {
				features[i] = Float.parseFloat(sttok.nextToken());
			}
			int classif = Integer.parseInt(sttok.nextToken());
			ClusterItem item = new ClusterItem(features, classif);
			result.add(item);
		}
		bufw.close();
		return result;
		
	}
	
	private static List<DissimMatrixDouble> computeDissims(List<ClusterItem> itens) {
		List<DissimMatrixDouble> result = new ArrayList<DissimMatrixDouble>(NUMBER_OF_CRITERIA);
		for (int i = 0; i < NUMBER_OF_CRITERIA; i++) {
			DissimMatrixDouble dissimM = new DissimMatrixDouble(itens.size());
			for (int el = 0; el < itens.size(); el++) {
				for (int j = 0; j <= el; j++) {
					dissimM.putDissim(el, j, calcDissim(el, j, i, itens));
				}
				
			}
			result.add(dissimM);
		}
		
		return result;
	}
	
	private static float calcDissim(int el1, int el2, int criterionIndex,
			List<ClusterItem> itens) {
		double result = itens.get(el1).getFeatures()[criterionIndex] - itens.get(el2).getFeatures()[criterionIndex];
		return (float)Math.abs(result);
	}
	
	
}
