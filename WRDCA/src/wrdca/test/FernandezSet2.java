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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import wrdca.algo.WTDHMGlobalClustering;
import wrdca.util.Cluster;
import wrdca.util.ConfusionMatrix;
import wrdca.util.DissimMatrixDouble;
import wrdca.util.MathUtil;


public class FernandezSet2 {
	public static final int NUMBER_OF_CRITERIA = 4;
	public static final int NUMBER_OF_RUNS = 100;
	

	/**
	 * @param args
	 */
	public static void main(String[] args)  throws IOException, IloException {
		List<ClusterItem> itens = parseFile(DataFileNames.getString("FernandezSet2Test.DATAFILE")); //$NON-NLS-1$);
		int[] classLabels = new int[itens.size()];
		{
			int i = 0;
			for (Iterator<ClusterItem> iterator = itens.iterator(); iterator.hasNext(); i++) {
				ClusterItem clusterItem = iterator.next();
				classLabels[i] = clusterItem.getClassifIndex();
			}
		}
		final String classNames[] = {"Very Low","Low","Low or Below Average","Below Average","Average","Average or Above Average","Above Average","Above Average or High","High","High or Very High","Very High","Exceptional"};
		// TODO Auto-generated method stub
		List<DissimMatrixDouble> dissimMatrices = computeDissims(itens);
		final double iterationCount[] = new double[NUMBER_OF_RUNS];
		for(int k = 1; k <= 25; k++) { /* inicialmente fiz de 1 ate 25 */
			double bestJ = Double.MAX_VALUE;

			List<Cluster> bestClusters = null;
			long timeInMilis = System.currentTimeMillis();
			for (int i = 0; i < NUMBER_OF_RUNS; i++) {
				WTDHMGlobalClustering clust = new WTDHMGlobalClustering(dissimMatrices);
				//WTDHMClustering clust = new WTDHMClustering(dissimMatrices);
				clust.setMaxWeightAbsoluteDifferenceGlobal(0.1);
				//clust.setSeedType(WTDHMGlobalClustering.SeedingType.PLUSPLUS_SEED);
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
			
			System.out.println("---------K = " + k + " ----------------------"); //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println("Best J is: " + bestJ); //$NON-NLS-1$
			for (int i = 0; i < k; i++) {
				Cluster c = bestClusters.get(i);
				System.out.print("Cluster " + i + ", center (" + itens.get(c.getCenter()).getDescription() + ", " + itens.get(c.getCenter()).getClassif() + "):"); //$NON-NLS-1$ //$NON-NLS-2$
				
				for (Integer el : c.getElements()) {
					System.out.print("|" + itens.get(el).getDescription() + ", " + itens.get(el).getClassif());  //$NON-NLS-1$
				}
				System.out.println(""); //$NON-NLS-1$
				System.out.print("  * Weights:"); //$NON-NLS-1$
				for (int j = 0; j < c.getWeights().length; j++) {
					System.out.print(" (" + j + ", " + c.getWeights()[j] + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				System.out.println(""); //$NON-NLS-1$
			}
			ConfusionMatrix confusionMatrix = new ConfusionMatrix(k, 12, classNames);
			for (int i = 0; i < bestClusters.size(); i++) {
				Cluster cluster = bestClusters.get(i);
				for (Integer element : cluster.getElements()) {
					final int classlabel = classLabels[element];
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
			System.out.println(">>>>>>>>>>>> The CR-Index  is: "+ confusionMatrix.CRIndex());
			System.out.println(">>>>>>>>>>>> OERC Index    is: " + confusionMatrix.OERCIndex());
			System.out.println(">>>>>>>>>>>> NMI Index     is: " + confusionMatrix.nMIIndex());
			System.out.println("Iterations to converge: " + Arrays.toString(iterationCount));
			System.out.println("Mean iterations: " + MathUtil.mean(iterationCount));
			System.out.println("Stddev iterations: " + MathUtil.stddev(iterationCount));
			
			
			System.out.println("Total time in seconds: " + timeInMilis/1000.0);
			
			System.out.println("COMPARING TO FERNANDEZ");
			ConfusionMatrix fernandezMatriz = parseTable4Article(itens, ClusterItem.getNumberAprioriClasses());
			System.out.println(">>>>>>>>>>>> Fernandez F-Measure: "+ fernandezMatriz.fMeasureGlobal());
			System.out.println(">>>>>>>>>>>> Fernandez CR-Index : "+ fernandezMatriz.CRIndex());
			System.out.println(">>>>>>>>>>>> Fernandez OERC     : "+ fernandezMatriz.OERCIndex());
			System.out.println(">>>>>>>>>>>> NMI Index     is: " + fernandezMatriz.nMIIndex());			


		}

	}
	
	private static class ClusterItem {
		private String description;
		private int[] c;
		private String classif;
		private int classifIndex;
		private static Map<String, Integer> classifToClass;
		private static int numberAprioriClasses;
		
		
		static {
			classifToClass = new HashMap<String, Integer>(12);
			classifToClass.put("Very Low", 0);
			classifToClass.put("Low", 1);
			classifToClass.put("Low or Below Average", 2);
			classifToClass.put("Below Average", 3);
			classifToClass.put("Average", 4);
			classifToClass.put("Average or Above Average", 5);
			classifToClass.put("Above Average", 6);
			classifToClass.put("Above Average or High", 7);
			classifToClass.put("High", 8);
			classifToClass.put("High or Very High", 9);
			classifToClass.put("Very High", 10);
			classifToClass.put("Exceptional", 11);
			numberAprioriClasses = classifToClass.size();
		}


		public ClusterItem(String description, int c0, int c1, int c2, int c3, String classif) {
			this.description = description;
			this.c = new int[4];
			this.c[0] = c0;
			this.c[1] = c1;
			this.c[2] = c2;
			this.c[3] = c3;
			this.classif = classif;
			this.classifIndex = classifToClass.get(classif).intValue();
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

		public String getClassif() {
			return classif;
		}

		public int getClassifIndex() {
			return classifIndex;
		}

		public static int getNumberAprioriClasses() {
			return numberAprioriClasses;
		}
	}

	private static List<ClusterItem> parseFile(String fileString) throws IOException {
		List<ClusterItem> result = new LinkedList<ClusterItem>();
		File file = new File(fileString);
		BufferedReader bufw = new BufferedReader(new FileReader(file));

		String line = bufw.readLine();
		while ((line = bufw.readLine()) != null) {
			StringTokenizer sttok = new StringTokenizer(line, ";", false); //$NON-NLS-1$
			String name = sttok.nextToken();
			int c0 = Integer.parseInt(sttok.nextToken());
			int c1 = Integer.parseInt(sttok.nextToken());
			int c2 = Integer.parseInt(sttok.nextToken());
			int c3 = Integer.parseInt(sttok.nextToken());
			String classif = sttok.nextToken();
			ClusterItem item = new ClusterItem(name, c0, c1, c2, c3, classif);
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
		double result = (itens.get(el1).getCByIndex(criterionIndex) - itens.get(el2).getCByIndex(criterionIndex))/ClusterItem.getNormFactor(criterionIndex);
		return (float)Math.abs(result);
	}

	private static float calcQuadDissim(int el1, int el2, int criterionIndex,
			List<ClusterItem> itens) {
		double result = ((itens.get(el1).getCByIndex(criterionIndex) - itens.get(el2).getCByIndex(criterionIndex))*(itens.get(el1).getCByIndex(criterionIndex) - itens.get(el2).getCByIndex(criterionIndex)));
		return (float)result;
	}
	
	public static ConfusionMatrix parseTable4Article(List<ClusterItem> itens, int numbAprioriClasses) throws IOException {
		//considerando 22 clusters: os 21 dele e os objetos que nao ficaram em lugar nenhum num 22o.
		File tableFile = new File("C:/Users/Sergio/Dropbox/CIn/research/inria/dados/FernandezSet2-Table4-article.txt");
		int k = 22;
		BufferedReader bufReader = new BufferedReader(new FileReader(tableFile));
		String line;
		int cluster = 0;
		ConfusionMatrix result = new ConfusionMatrix(k, numbAprioriClasses);
		Set<Integer> inputElements = new HashSet<Integer>(81);
		for (int i = 1; i <= 81; i++) {
			inputElements.add(i);
		}
		try {
			while((line = bufReader.readLine()) != null) {
				StringTokenizer sttok = new StringTokenizer(line, ",", false); //$NON-NLS-1$
				while (sttok.hasMoreTokens()) {
					int element = Integer.parseInt(sttok.nextToken());
					boolean hasRemoved = inputElements.remove(element);
					if(!hasRemoved) throw new IllegalStateException("Element " + element + " duplicated?");
					//System.out.println("Elemento(-1): " + (element-1) + "vai para cluster " + cluster);
					assert (element == Integer.parseInt(itens.get(element-1).getDescription()));
					result.putObject(element-1, cluster, itens.get(element-1).getClassifIndex());
				}
				cluster++;
			}

		} finally {
			bufReader.close();
		}
		for (Integer element : inputElements) {
			result.putObject(element-1, 21, itens.get(element-1).getClassifIndex());
		}
		return result;
	}

}
