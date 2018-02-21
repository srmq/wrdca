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
import java.util.List;
import java.util.StringTokenizer;

import wrdca.algo.ClusterAlgorithm;
import wrdca.algo.WTDHMClustering;
import wrdca.util.Cluster;
import wrdca.util.DissimMatrixDouble;


public class PollMarriageTest {
	
	private static int NUMBER_OF_OBJECTS = 22;
	private static int NUMBER_OF_QUESTIONS = 10;
	public static final int NUMBER_OF_RUNS = 100;

	
	
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
	public static void main(String[] args)  throws Exception {
		GroupObject[] objects = parseFile(DataFileNames.getString("PollMarriageTest.DATAFILE")); //$NON-NLS-1$
		List<DissimMatrixDouble> dissimMatrices = computeDissims(objects);
		final int iterationCount[] = new int[NUMBER_OF_RUNS];

		for(int k = 1; k <= 10; k++) {
			double bestJ = Double.MAX_VALUE;
			List<Cluster> bestClusters = null;
			long timeInMilis = System.currentTimeMillis();
			for (int i = 0; i < NUMBER_OF_RUNS; i++) {
				ClusterAlgorithm clust = new WTDHMClustering(dissimMatrices);
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
				System.out.print("Cluster " + i + ":"); //$NON-NLS-1$ //$NON-NLS-2$
				Cluster c = bestClusters.get(i);
				for (Integer el : c.getElements()) {
					System.out.print(" " + objects[el].getName());  //$NON-NLS-1$
				}
				System.out.println(""); //$NON-NLS-1$
				System.out.print("  * Prototype: ");
				System.out.print(objects[c.getCenter()].getName());
				System.out.println("");
				System.out.print("  * Weights:"); //$NON-NLS-1$
				for (int j = 0; j < c.getWeights().length; j++) {
					System.out.print(" (" + objects[0].getQuestion(j).getName() + ", " + c.getWeights()[j] + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				System.out.println(""); //$NON-NLS-1$
			}
			System.out.println("Iterations to converge: " + Arrays.toString(iterationCount));
			double iteravg = 0.0;
			for (int i = 0; i < iterationCount.length; i++) {
				iteravg += iterationCount[i];
			}
			iteravg /= iterationCount.length;
			System.out.println("Mean iterations: " + iteravg);
			System.out.println("Total time in seconds: " + timeInMilis/1000.0);

		}
		
	}
	
	private static List<DissimMatrixDouble> computeDissims(GroupObject[] objects) {
		final int numberOfQuestions = objects[0].numberOfQuestions();
		List<DissimMatrixDouble> result = new ArrayList<DissimMatrixDouble>(numberOfQuestions);
		for (int i = 0; i < numberOfQuestions; i++) {
			DissimMatrixDouble dissimM = new DissimMatrixDouble(objects.length);
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
