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
import java.util.Collections;
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


public class FernandezSet2CrossValidation9x9 {
	public static final int NUMBER_OF_CRITERIA = 4;
	public static final int NUMBER_OF_RUNS = 100;
	public static final int CROSS_VALIDATION_PARTITION_CARDINALITY = 9;
	public static final int CROSS_VALIDATION_PARTITIONS = 9;
	public static final int NUMBER_OF_OBJECTS = 81;
	public static final int B_CROSSVALIDATION = 30;
	public static final int K_MAX = 25;
	


	/**
	 * @param args
	 */
	public static void main(String[] args)  throws IOException, IloException {
		List<ClusterItem> allItens = parseFile(DataFileNames.getString("FernandezSet2Test.DATAFILE")); //$NON-NLS-1$);
		assert(allItens.size() == NUMBER_OF_OBJECTS);
		
		System.out.println("K,B_CROSSVALIDATION,THETA_V_FOLDS");
		double[] averagesForK = new double[K_MAX];
		double[] stdDevsForK = new double[K_MAX];
		double valuesForThisK[] = new double[B_CROSSVALIDATION];
		for (int k = 1; k <= K_MAX; k++) {
			Arrays.fill(valuesForThisK, 0);
			for(int b = 0; b < B_CROSSVALIDATION; b++) { /* inicialmente fiz de 1 ate 25 */
				Collections.shuffle(allItens);
				int[] classLabels = new int[allItens.size()];
				{
					int i = 0;
					for (Iterator<ClusterItem> iterator = allItens.iterator(); iterator.hasNext(); i++) {
						ClusterItem clusterItem = iterator.next();
						classLabels[i] = clusterItem.getClassifIndex();
					}
				}
				//final String classNames[] = {"Very Low","Low","Low or Below Average","Below Average","Average","Average or Above Average","Above Average","Above Average or High","High","High or Very High","Very High","Exceptional"};
				// TODO Auto-generated method stub
				List<DissimMatrixDouble> allObjectsDissimMatrices = computeDissims(allItens);
				
				List<ClusterItem> objectsToCluster = new ArrayList<ClusterItem>(NUMBER_OF_OBJECTS - CROSS_VALIDATION_PARTITION_CARDINALITY);
				assert (CROSS_VALIDATION_PARTITION_CARDINALITY * CROSS_VALIDATION_PARTITIONS == NUMBER_OF_OBJECTS);
				
				int objectsToClusterObjectMapping[] = new int[NUMBER_OF_OBJECTS - CROSS_VALIDATION_PARTITION_CARDINALITY];

				final int iterationCount[] = new int[NUMBER_OF_RUNS];
				
				System.out.print(k + "," + b + ",");
				// cross-validation folds
				double thetaVFolds = 0;

				for (int testSetIndex = 0; testSetIndex < allItens.size(); testSetIndex += CROSS_VALIDATION_PARTITION_CARDINALITY) {
					int destIndex;
					objectsToCluster.clear();
					for (destIndex = 0; destIndex < testSetIndex; destIndex++) {
						objectsToCluster.add(allItens.get(destIndex));
						objectsToClusterObjectMapping[destIndex] = destIndex;
					}
					int sourceIndex = testSetIndex + CROSS_VALIDATION_PARTITION_CARDINALITY;
					while (sourceIndex < allItens.size()) {
						objectsToClusterObjectMapping[destIndex++] = sourceIndex;
						objectsToCluster.add(allItens.get(sourceIndex++));
					}
					
					List<ClusterItem> testSet = new ArrayList<ClusterItem>(CROSS_VALIDATION_PARTITION_CARDINALITY);
					int testSetObjectMapping[] = new int[CROSS_VALIDATION_PARTITION_CARDINALITY];
					destIndex = 0;
					sourceIndex = testSetIndex;

					while (testSet.size() < CROSS_VALIDATION_PARTITION_CARDINALITY) {
						testSetObjectMapping[destIndex++] = sourceIndex;
						testSet.add(allItens.get(sourceIndex++));
					}

					
					double bestJ = Double.MAX_VALUE;

					List<Cluster> bestClusters = null;
					long timeInMilis = System.currentTimeMillis();
					List<DissimMatrixDouble> clusterObjectsDissimMatrices = computeDissims(objectsToCluster);
					for (int i = 0; i < NUMBER_OF_RUNS; i++) {
						WTDHMGlobalClustering clust = new WTDHMGlobalClustering(clusterObjectsDissimMatrices);
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
					double testSetJ = 0;
					for (int testObjectOriginalIndex : testSetObjectMapping) {
						double minClusterRegret = Double.MAX_VALUE;
						for (Cluster cluster : bestClusters) {
							double thisRegret = maxRegret(testObjectOriginalIndex,
									objectsToClusterObjectMapping[cluster
											.getCenter()], cluster,
											allObjectsDissimMatrices);
							if (thisRegret < minClusterRegret) {
								minClusterRegret = thisRegret; 
							}
						}
						testSetJ += minClusterRegret;
					}
					thetaVFolds += testSetJ;
				} // END V-Folds
				valuesForThisK[b] = thetaVFolds;
				System.out.println(thetaVFolds);
			}  // END for b
			averagesForK[k-1] = MathUtil.mean(valuesForThisK);
			stdDevsForK[k-1] = MathUtil.stddev(valuesForThisK);
		} // END for k
		System.out.println("------------MEANS AND STDDEVS--------");
		System.out.println("K,AVGTheta,STDdevTheta");
		for (int i = 0; i < averagesForK.length; i++) {
			System.out.println((i+1) + "," + averagesForK[i] + "," + stdDevsForK[i]);
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
	
	private final static double maxRegret(int elTestSet, int clusterCenterAllObjects, Cluster cluster, List<DissimMatrixDouble> dissimMatricesAllObjects) {
		double maxRegret = Double.MIN_VALUE;
		double myRegret;
		int dimensions = cluster.getWeights().length;
		for (int c = 0; c < dimensions; c++) {
			myRegret = dissimMatricesAllObjects.get(c).getDissim(elTestSet, clusterCenterAllObjects) * cluster.getWeights()[c];
			if (myRegret > maxRegret) {
				maxRegret = myRegret;
			}
		}
		return maxRegret;
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
