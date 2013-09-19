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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import wrdca.algo.ClusterAlgorithm;
import wrdca.algo.WTDHMClustering;
import wrdca.util.Cluster;
import wrdca.util.DissimMatrix;


public class PollMarriageTestCrossValidation1x22 {
	
	private static int NUMBER_OF_OBJECTS = 22;
	private static int NUMBER_OF_QUESTIONS = 10;
	public static final int NUMBER_OF_RUNS = 100;
	public static final int CROSS_VALIDATION_PARTITION_CARDINALITY = 1;
	public static final int CROSS_VALIDATION_PARTITIONS = 22;

	
	
	private static class GroupObject {
		private List<Question> questions;
		private String name;
		public GroupObject(String name) {
			this.questions = new ArrayList<Question>(NUMBER_OF_QUESTIONS);
			this.name = name;
		}
		public void addCategory(int questionIndex, String catName, int value) {
			this.questions.get(questionIndex).add(catName, value);
		}
		public void addQuestion(String name) {
			this.questions.add(new Question(name));
		}
		public int numberOfQuestions() {
			return this.questions.size();
		}
		public Question getQuestion(int index) {
			return this.questions.get(index);
		}
		
		public String getName() {
			return name;
		}
	}
	
	private static class Question {
		private String name;
		private List<String> categories;
		private List<Integer> values;
		public Question(String name) {
			this.name = name;
			this.categories = new ArrayList<String>();
			this.values = new ArrayList<Integer>();
		}
		public void add(String catName, int value) {
			this.categories.add(catName);
			this.values.add(value);
		}
		public String getName() {
			return name;
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		GroupObject[] objects = parseFile(DataFileNames
				.getString("PollMarriageTest.DATAFILE")); //$NON-NLS-1$
		Collections.shuffle(Arrays.asList(objects));
		GroupObject[] objectsToCluster = new GroupObject[objects.length
				- CROSS_VALIDATION_PARTITION_CARDINALITY];

		int objectsToClusterObjectMapping[] = new int[objectsToCluster.length];
		List<DissimMatrix> dissimMatricesAllObjects = computeDissims(objects);

		System.out.println("K,THETA_V_FOLDS");
		for (int k = 1; k <= 10; k++) {
			System.out.print(k + ",");
			// cross-validation folds
			double thetaVFolds = 0;
			assert (CROSS_VALIDATION_PARTITION_CARDINALITY * CROSS_VALIDATION_PARTITIONS == NUMBER_OF_OBJECTS);
			for (int testSetIndex = 0; testSetIndex < objects.length; testSetIndex += CROSS_VALIDATION_PARTITION_CARDINALITY) {
				int destIndex;
				for (destIndex = 0; destIndex < testSetIndex; destIndex++) {
					objectsToCluster[destIndex] = objects[destIndex];
					objectsToClusterObjectMapping[destIndex] = destIndex;
				}
				int sourceIndex = testSetIndex + CROSS_VALIDATION_PARTITION_CARDINALITY;
				while (sourceIndex < objects.length) {
					objectsToClusterObjectMapping[destIndex] = sourceIndex;
					objectsToCluster[destIndex++] = objects[sourceIndex++];
				}

				GroupObject[] testSet = new GroupObject[CROSS_VALIDATION_PARTITION_CARDINALITY];
				int testSetObjectMapping[] = new int[CROSS_VALIDATION_PARTITION_CARDINALITY];
				destIndex = 0;
				sourceIndex = testSetIndex;

				while (destIndex < testSet.length) {
					testSetObjectMapping[destIndex] = sourceIndex;
					testSet[destIndex++] = objects[sourceIndex++];
				}

				List<DissimMatrix> dissimMatricesToCluster = computeDissims(objectsToCluster);
				final int iterationCount[] = new int[NUMBER_OF_RUNS];

				double bestJ = Double.MAX_VALUE;
				List<Cluster> bestClusters = null;
				long timeInMilis = System.currentTimeMillis();
				for (int i = 0; i < NUMBER_OF_RUNS; i++) {
					ClusterAlgorithm clust = new WTDHMClustering(
							dissimMatricesToCluster);
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
								dissimMatricesAllObjects);
						if (thisRegret < minClusterRegret) {
							minClusterRegret = thisRegret;
						}
					}
					testSetJ += minClusterRegret;
				}
				thetaVFolds += testSetJ;

			} // FIM CROSS VALIDATION
			System.out.println(thetaVFolds);
		} // fim for (k)

	}
	
	private final static double maxRegret(int elTestSet, int clusterCenterAllObjects, Cluster cluster, List<DissimMatrix> dissimMatricesAllObjects) {
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

	
	private static List<DissimMatrix> computeDissims(GroupObject[] objects) {
		final int numberOfQuestions = objects[0].numberOfQuestions();
		List<DissimMatrix> result = new ArrayList<DissimMatrix>(numberOfQuestions);
		for (int i = 0; i < numberOfQuestions; i++) {
			DissimMatrix dissimM = new DissimMatrix(objects.length);
			for (int el = 0; el < objects.length; el++) {
				for (int j = 0; j <= el; j++) {
				 dissimM.putDissim(el, j, calcDissim(el, j, i, objects));
				}
				
			}
			result.add(dissimM);
		}
		return result;
	}

	private static float calcDissim(int index1, int index2, int questionIndex, GroupObject[] objects) {
		final GroupObject obj1 = objects[index1];
		final GroupObject obj2 = objects[index2];
		final int numberOfCategories = obj1.getQuestion(questionIndex).categories.size();
		double numObj1 = 0;
		double numObj2 = 0;
		for (int i = 0; i < numberOfCategories; i++) {
			numObj1 += obj1.getQuestion(questionIndex).values.get(i);
			numObj2 += obj2.getQuestion(questionIndex).values.get(i);
		}
		double sum = 0;
		for (int i = 0; i < numberOfCategories; i++) {
			final double nij1 = obj1.getQuestion(questionIndex).values.get(i);
			final double nij2 = obj2.getQuestion(questionIndex).values.get(i);
			sum += Math.sqrt((nij1/numObj1)*(nij2/numObj2));
		}

		if (sum > 1.0) sum = 1.0;
		if (sum < 0.0) sum = 0.0;
		
		return (float)(1.0 - sum);
	}

	private static GroupObject[] parseFile(String string) throws IOException {
		String currentQuestion = " "; //$NON-NLS-1$
		File file = new File(string);
		BufferedReader bufw = new BufferedReader(new FileReader(file));

		String line = bufw.readLine();
		String[] clusterNames = parseNames(line);
		
		int questionIndex = -1;
		GroupObject[] objects = new GroupObject[NUMBER_OF_OBJECTS];
		for (int i = 0; i < objects.length; i++) {
			objects[i] = new GroupObject(clusterNames[i]);
		}
		while ((line = bufw.readLine()) != null) {
			StringTokenizer sttok = new StringTokenizer(line, ",", false); //$NON-NLS-1$
			String question = sttok.nextToken();
			if (!question.equals(currentQuestion)) {
				questionIndex++;
				currentQuestion = question;
				for (int i = 0; i < objects.length; i++) {
					objects[i].addQuestion(question);
				}
			}				
			final String categoryName = sttok.nextToken();
			for (int i = 0; i < NUMBER_OF_OBJECTS; i++) {
				objects[i].addCategory(questionIndex, categoryName, Integer.parseInt(sttok.nextToken()));
			}
		}
		bufw.close();
		return objects;
	}

	private static String[] parseNames(String line) {
		StringTokenizer sttok = new StringTokenizer(line, ",", false); //$NON-NLS-1$
		sttok.nextToken();
		sttok.nextElement();
		String[] returnValue = new String[NUMBER_OF_OBJECTS];
		for (int i = 0; i < returnValue.length; i++) {
			returnValue[i] = sttok.nextToken();
		}
		return returnValue;
	}

}
