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
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import wrdca.algo.WTDHMClustering;
import wrdca.util.Cluster;
import wrdca.util.DissimMatrix;


public class FernandezSet1Test {
	public static final int NUMBER_OF_CRITERIA = 4;
	public static final int NUMBER_OF_RUNS = 100;
	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException, IloException {
		List<ClusterItem> itens = parseFile(DataFileNames.getString("FernandezSet1Test.DATAFILE")); //$NON-NLS-1$);
		List<DissimMatrix> dissimMatrices = computeDissims(itens);
		
		for(int k = 1; k <= 10; k++) {
			double bestJ = Double.MAX_VALUE;
			List<Cluster> bestClusters = null;
			for (int i = 0; i < NUMBER_OF_RUNS; i++) {
				WTDHMClustering clust = new WTDHMClustering(dissimMatrices);
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
				System.out.print("Cluster " + i + ", center (" + itens.get(c.getCenter()).getDescription() + "):"); //$NON-NLS-1$ //$NON-NLS-2$
				
				for (Integer el : c.getElements()) {
					System.out.print("|" + itens.get(el).getDescription());  //$NON-NLS-1$
				}
				System.out.println(""); //$NON-NLS-1$
				System.out.print("  * Weights:"); //$NON-NLS-1$
				for (int j = 0; j < c.getWeights().length; j++) {
					System.out.print(" (" + j + ", " + c.getWeights()[j] + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				System.out.println(""); //$NON-NLS-1$
			}
		}
		
	}
	
	private static List<DissimMatrix> computeDissims(List<ClusterItem> itens) {
		List<DissimMatrix> result = new ArrayList<DissimMatrix>(NUMBER_OF_CRITERIA);
		for (int i = 0; i < NUMBER_OF_CRITERIA; i++) {
			DissimMatrix dissimM = new DissimMatrix(itens.size());

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
		double result = (itens.get(el1).getCByIndex(criterionIndex) - itens.get(el2).getCByIndex(criterionIndex))/ClusterItem.getNormFactor(criterionIndex);
		return (float)Math.abs(result);
	}

	private static List<ClusterItem> parseFile(String fileString) throws IOException {
		List<ClusterItem> result = new LinkedList<ClusterItem>();
		File file = new File(fileString);
		BufferedReader bufw = new BufferedReader(new FileReader(file));

		String line;
		while ((line = bufw.readLine()) != null) {
			StringTokenizer sttok = new StringTokenizer(line, ";", false); //$NON-NLS-1$
			String name = sttok.nextToken();
			int c0 = Integer.parseInt(sttok.nextToken());
			int c1 = Integer.parseInt(sttok.nextToken());
			int c2 = Integer.parseInt(sttok.nextToken());
			int c3 = Integer.parseInt(sttok.nextToken());
			ClusterItem item = new ClusterItem(name, c0, c1, c2, c3);
			result.add(item);
		}
		bufw.close();
		return result;
	}
	
	private static class ClusterItem {
		private String description;
		private int[] c;

		public ClusterItem(String description, int c0, int c1, int c2, int c3) {
			this.description = description;
			this.c = new int[4];
			this.c[0] = c0;
			this.c[1] = c1;
			this.c[2] = c2;
			this.c[3] = c3;
		}
		
		public static double getNormFactor(int i) {
			assert (i >= 0 && i < 4);
			return 6.0;
		}
		public String getDescription() {
			return description;
		}
		public int getC1() {
			return c[1];
		}
		public int getC2() {
			return c[2];
		}
		public int getC3() {
			return c[3];
		}
		public int getC0() {
			return c[0];
		}
		
		public int getCByIndex(int i) {
			assert(i >= 0 && i <= 3);
			return c[i];
		}
	}

}
