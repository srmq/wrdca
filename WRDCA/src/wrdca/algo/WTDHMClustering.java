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
package wrdca.algo;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import wrdca.util.Cluster;
import wrdca.util.DissimMatrix;


public class WTDHMClustering implements ClusterAlgorithm {
	private List<DissimMatrix> dissimMatrices;
	public static final boolean DEBUG = false;
	private static final Random rnd = new Random(1);
	
	public static final double EPSILON = 0.000001;
	
	public static final int TIMELIMIT = 60;
	
	public static final long TOTALTIMELIMITSECONDS = 300;

	public boolean timeLimitAchieved;

	private long initTimeMilis;
	
	private double maxWeightAbsoluteDifferenceGlobal = 1.0;
	
	private int iteracoes;
	
	public static enum SeedingType {
		RANDOM_SEED, PLUSPLUS_SEED
	}
	
	//private int[] protIndices;
	
	private final int nElems;
	private final int nCriteria;
	
	private int[] indexInClustersForElem;
	
	private SeedingType seedType = SeedingType.RANDOM_SEED;
	
	private static IloCplex cplex; 
	
	static {
		try {
			cplex = new IloCplex();
			if (!DEBUG) {
				cplex.setOut(null);
			}
			cplex.setParam(IloCplex.DoubleParam.TiLim, TIMELIMIT);
		} catch (IloException ex) {
			ex.printStackTrace();
			throw new IllegalStateException("ERROR INITIALIZING CPLEX: " + ex.getMessage());
		}
	}
	
	// pesos para cada cluster k
	//private float[][] relevanceMatrix;
	private List<Cluster> clusters;

	public static final int BIG_CONSTANT = Integer.MAX_VALUE/100;
	
	public WTDHMClustering(List<DissimMatrix> dissimMatrices) {
		this.dissimMatrices = dissimMatrices;
		this.nElems = dissimMatrices.get(0).length();
		this.nCriteria = dissimMatrices.size();
	}
	
	public void cluster(int k) throws IloException {
		initialize(k);
		double currJ = BIG_CONSTANT;
		if (DEBUG) {
			currJ = calcJ(this.clusters);
			System.out.println("--iterando k = " + k + " >>>>>>>>>> J: " + currJ);
		}
		boolean changed;
		List<Cluster> _oldClusters;
		this.iteracoes = 0;
		do {
			++iteracoes;
			double _J;
			if (DEBUG) {
				System.out.println("Iteracao " + iteracoes);
				_oldClusters = cloneClusters();
				_J = calcJ(_oldClusters);
			}
			List<Cluster> clusters = bestPrototypes();
			if (DEBUG) {
				final double myJ = calcJ(clusters);
				assert ((myJ <= _J) || (Math.abs(myJ - _J) < EPSILON));
			}
			for (int i = 0; i < clusters.size(); i++) {
				if (DEBUG) {
					System.out.println("Regret of Cluster " + i + " before update weights: " + calcJ(clusters.get(i)));
				}
				final double regret;
				final double maxValue = calcJ(clusters.get(i));
				regret = updateWeights(clusters.get(i), maxValue);
				if (regret == -1) {
					System.out.println("WARNING: Could not optimize weights for cluster " + i);
				}
				if (DEBUG) {
					System.out.println("Regret of Cluster " + i + " AFTER update weights: " + regret);
					System.out.println("And with calcJ this means: " + calcJ(clusters.get(i)));
				}
			}
			if (DEBUG) {
				final double myJ = calcJ(clusters);
				assert ((myJ <= _J) || (Math.abs(myJ - _J) < EPSILON));
			}			
			changed = clusterAssign(clusters);
			if (DEBUG) {
				List<Cluster> newClusters = cloneClusters();
				final double J = calcJ(newClusters);
				
				assert(J <= currJ || (Math.abs(currJ - J) < EPSILON));
				//if (!(J <= currJ || (Math.abs(currJ - J) < EPSILON))) {
				//	System.out.println("boom");
				//}
				currJ = J;
				System.out.println(">>>>>>>>>> J: " + J);
			}
			if (timeIsUp()) {
				this.timeLimitAchieved = true;
				System.err.print("WARNING: Returning because time limit of " + TOTALTIMELIMITSECONDS + " seconds was achieved");
				if (DEBUG) {
					System.out.print(" after " + iteracoes + " iterations!");
				}
				System.out.println("");
			}

		} while (changed && !this.timeLimitAchieved);
		if (DEBUG) {
			System.out.println("------ FIM DE CALCULO -----");

		}
	}

	private List<Cluster> cloneClusters() {
		List<Cluster> clusterClone = new ArrayList<Cluster>(this.clusters.size());
		for (Cluster cluster : this.clusters) {
			clusterClone.add((Cluster) cluster.clone());
		}
		return clusterClone;
	}

	public double calcJ(List<Cluster> clusters) {
		double J = 0.0;
		for (Iterator<Cluster> iterator = clusters.iterator(); iterator.hasNext();) {
			Cluster cluster = (Cluster) iterator.next();
			J += calcJ(cluster);
		}
		return J;
	}
	
	private double calcJ(Cluster cluster) {
		return calcRegret(cluster.getCenter(), cluster, BIG_CONSTANT);
	}

	private double updateWeights(Cluster cluster, double maxValue) throws IloException {
		double regret = -1;
		
		IloNumVar[] pVars = new IloNumVar[cluster.getElements().size()];
		for (int i = 0; i < pVars.length; i++) {
			pVars[i] = cplex.numVar(0.0, BIG_CONSTANT);
		}
		IloNumVar[] weights = new IloNumVar[nCriteria];
		for (int i = 0; i < weights.length; i++) {
			weights[i] = cplex.numVar(0.0, 1.0);
		}

		{
			int i = 0;
			for (Iterator<Integer> iterator = cluster.getElements().iterator(); iterator.hasNext();) {
				Integer element = iterator.next();
				addEquations(element.intValue(), cluster.getCenter(), cplex, pVars[i], weights);
				i++;
			}
		}
		
		cplex.addEq(cplex.sum(weights), 1.0);
		
		if (this.maxWeightAbsoluteDifferenceGlobal != 1.0) {
			for (int i = 0; i < (weights.length - 1); i++) {
				for (int j = i+1; j < weights.length; j++) {
					cplex.addLe(cplex.abs(cplex.diff(weights[i], weights[j])), this.maxWeightAbsoluteDifferenceGlobal);
				}
			}
		}
		

		IloNumExpr expressionObjective = cplex.sum(pVars);
		cplex.addLe(expressionObjective, maxValue);
		cplex.addMinimize(expressionObjective);
		
		if (cplex.solve()) {
			if (cplex.getStatus() == IloCplex.Status.Optimal) {
				regret = cplex.getObjValue();
				double[] newWeights = cplex.getValues(weights);
				float[] weightVector = cluster.getWeights();
				if (weightVector == null) {
					weightVector = new float[newWeights.length];
					cluster.setWeights(weightVector);
				}
				for (int i = 0; i < newWeights.length; i++) {
					weightVector[i] = (float)newWeights[i];
				}
			}
		}
		cplex.clearModel();
		
		return regret;
	}

	private void addEquations(int el, int ck, IloCplex cplex, IloNumVar myVar,
			IloNumVar[] weights) throws IloException {

		int j = nCriteria - 1;
		int i = j - 1;
		IloNumVar lmax = cplex.numVar(0, BIG_CONSTANT);
		final IloNumExpr prod1 = cplex.prod(dissimMatrices.get(j).getDissim(el, ck) , weights[j]);
		final IloNumExpr prod2 = cplex.prod(dissimMatrices.get(i).getDissim(el, ck), weights[i]);
		cplex.addGe(lmax, prod1);
		cplex.addGe(lmax, prod2);
		
		IloIntVar c = cplex.boolVar();
		cplex.addLe(cplex.diff(lmax, prod1), cplex.prod(c, BIG_CONSTANT));
		cplex.addLe(cplex.diff(lmax, prod2), cplex.prod(cplex.diff(1, c), BIG_CONSTANT));

		i--;
		while (i >= 0) {
			IloNumVar newmax = cplex.numVar(0, BIG_CONSTANT);
			cplex.addGe(newmax, lmax);
			final IloNumExpr prod = cplex.prod(dissimMatrices.get(i).getDissim(el, ck), weights[i]);
			cplex.addGe(newmax, prod);
			IloIntVar d = cplex.boolVar();
			cplex.addLe(cplex.diff(newmax, lmax), cplex.prod(d, BIG_CONSTANT));
			cplex.addLe(cplex.diff(newmax, prod), cplex.prod(cplex.diff(1, d), BIG_CONSTANT));

			lmax = newmax;
			i--;
		}
		cplex.addEq(myVar, lmax);
	}

	private void initialize(int k) {
		//this.protIndices = new int[k];
		//Arrays.fill(protIndices, -1);
//		{
//			int i = 0;
//			while (i < this.protIndices.length) {
//				final int index = rnd.nextInt(this.nElems);
//				if (!isCenter(index)) {
//					this.protIndices[i] = index;
//					i++;
//				}
//			}
//		}

		this.initTimeMilis = System.currentTimeMillis();
		this.indexInClustersForElem = new int[nElems];
		Arrays.fill(this.indexInClustersForElem, -1);
		List<Cluster> clusters = firstGenClusters(k);
		for (Cluster cluster : clusters) {
			float[] weightVector = new float[nCriteria];
			Arrays.fill(weightVector, 1.0f/nCriteria);
			cluster.setWeights(weightVector);
		}
		switch(this.seedType) {
		case RANDOM_SEED:
			for (Cluster cluster : clusters) {
				cluster.setCenter(rnd.nextInt(this.nElems));
			}
			break;
		case PLUSPLUS_SEED:
			this.KMeansPlusPlusSeeding(clusters);
			break;
		default:
			throw new IllegalStateException("Initialization for seed type " + this.seedType + " not implemented");
		}
		this.clusters = clusters;
		clusterAssign(clusters);
	}
	
	private void KMeansPlusPlusSeeding(List<Cluster> clusters) {
		int firstCenter = rnd.nextInt(this.nElems);
		Iterator<Cluster> iter = clusters.iterator();
		Cluster firstCluster = iter.next(); 
		firstCluster.setCenter(firstCenter);
		
		double distances[] = new double[this.nElems];
		double sum = 0;
		for (int i = 0; i < distances.length; i++) {
			distances[i] = this.maxRegret(i, firstCenter, firstCluster);
			distances[i] *= distances[i];
			sum += distances[i];
		}
		while(iter.hasNext()) {
			Cluster c = iter.next();
			double rndNum = rnd.nextDouble();
			int index = 0;
			double currSum = 0;
			for (int i = 0; i < distances.length; i++) {
				currSum += (distances[i]/sum);
				if (currSum >= rndNum) {
					index = i;
					
					for (int j = 0; j < distances.length; j++) {
						double newDist = this.maxRegret(j, index, firstCluster);
						newDist *= newDist;
						if (newDist < distances[j]) {
							sum -= distances[j];
							sum += newDist;
							distances[j] = newDist;
						}
					}
					
					break;
				}
			}
			c.setCenter(index);
		}
	}

	/**
	 * Return the index of the first cluster in which the index passed as parameter is the center
	 * @param index
	 * @return
	 */
	private final int isCenterOf(int index) {
		for (int i = 0; i < this.clusters.size(); i++) {
			if (this.clusters.get(i).getCenter() == index) {
				return i;
			}
		}
		return -1;
	}

	private boolean clusterAssign(List<Cluster> clusters) {
		boolean hasChanged = false;
		for (int i = 0; i < nElems; i++) {
			int centerOf;
			if ((centerOf = isCenterOf(i)) == -1) {
				int clusterIndexMinDist;
				double minDist;
				if (this.indexInClustersForElem[i] >= 0 && this.indexInClustersForElem[i] < clusters.size()) {
					clusterIndexMinDist = this.indexInClustersForElem[i];
					minDist = maxRegret(i, clusters.get(this.indexInClustersForElem[i]).getCenter(), clusters.get(this.indexInClustersForElem[i]));
				} else {
					clusterIndexMinDist = -1;
					minDist = BIG_CONSTANT;
				}

				for (int k = 0; k < clusters.size(); k++) {
					final double myDist;
					if (((myDist = maxRegret(i, clusters.get(k).getCenter(), clusters.get(k))) + EPSILON) < minDist) {
						minDist = myDist;
						clusterIndexMinDist = k;
					}
				}
				if (this.indexInClustersForElem[i] != clusterIndexMinDist) {
					if (this.indexInClustersForElem[i] >= 0 && this.indexInClustersForElem[i] < clusters.size()) {
						clusters.get(this.indexInClustersForElem[i]).remove(i);
					}
					this.indexInClustersForElem[i] = clusterIndexMinDist;
					clusters.get(clusterIndexMinDist).add(i);
					hasChanged = true;
				}
			} else {
				if (this.indexInClustersForElem[i] != centerOf) {
					if (this.indexInClustersForElem[i] >= 0 && this.indexInClustersForElem[i] < clusters.size()) {
						clusters.get(this.indexInClustersForElem[i]).remove(i);
					}

					this.indexInClustersForElem[i] = centerOf;
					clusters.get(centerOf).add(i);

					hasChanged = true;
				}
			}
		}
		return hasChanged;
	}

	private final double maxRegret(int i, int j, Cluster cluster) {
		double maxRegret = Double.MIN_VALUE;
		double myRegret;
		for (int c = 0; c < nCriteria; c++) {
			myRegret = this.dissimMatrices.get(c).getDissim(i, j) * cluster.getWeights()[c];
			if (myRegret > maxRegret) {
				maxRegret = myRegret;
			}
		}
		return maxRegret;
	}
	
	private List<Cluster> bestPrototypes() {
		//List<Cluster> clusterList = genClusters();
		float[] minSumRegrets = new float[this.clusters.size()];
		for (int k = 0; k < minSumRegrets.length; k++) {
			if (clusters.get(k).getCenter() >= 0) {
				minSumRegrets[k] = calcRegret(clusters.get(k).getCenter(), clusters.get(k), BIG_CONSTANT);
			} else {
				minSumRegrets[k] = BIG_CONSTANT;
			}
		}
		
		for (int n = 0; n < nElems; n++) {
			// para cada elemento ver se ele e melhor do que o que temos agora
			for (int k = 0; k < this.clusters.size(); k++) {
				final float regret = calcRegret(n, clusters.get(k), minSumRegrets[k]);
				if ((regret + EPSILON) < minSumRegrets[k]) {
					clusters.get(k).setCenter(n);
					minSumRegrets[k] = regret;
				}
			}
		}
//		for (int i = 0; i < protIndices.length; i++) {
//			protIndices[i] = clusters.get(i).getCenter();
//		}
		return clusters;
	}

	private float calcRegret(int candidateCenter, Cluster c, float currentRegret) {
		float sumRegret = 0.0f;
		for (Iterator<Integer> iterator = c.getElements().iterator(); iterator.hasNext();) {
			Integer el = iterator.next();
			sumRegret += maxRegret(el, candidateCenter, c);
			if (sumRegret > currentRegret) return BIG_CONSTANT;
		}
		return sumRegret;
	}

	private List<Cluster> firstGenClusters(int k) {
		List<Cluster> result = new ArrayList<Cluster>(k);
		//Map<Integer,Cluster> centerMap = new HashMap<Integer,Cluster>(this.protIndices.length);
		for (int i = 0; i < k; i++) {
				Cluster c = new Cluster();
				c.setElements(new HashSet<Integer>());
				result.add(c);
		}
//		for (int i = 0; i < elemCluster.length; i++) {
//			if (elemCluster[i] != -1) {
//				Cluster c = centerMap.get(elemCluster[i]);
//				c.add(i);		
//			}
//		}
		return result;
	}
	
	public double calcCR(int[] classLabels) {
		double a, b, c, d;
		a = b = c = d = 0.0;
		assert(classLabels.length == this.nElems);
		byte agreeCluster, agreeClass;
		for (int i = 0; i < classLabels.length - 1; i++) {
			for (int j = i + 1; j < classLabels.length; j++) {
				agreeCluster = (this.indexInClustersForElem[i] == this.indexInClustersForElem[j]) ? (byte)1 : (byte)0;
				agreeClass = (classLabels[i] == classLabels[j]) ? (byte)1 : (byte)0;
				a += agreeCluster * agreeClass;
				b += (1 - agreeCluster) * agreeClass;
				c += agreeCluster * (1 - agreeClass);
				d += (1 - agreeCluster) * (1 - agreeClass);
			}
		}
		final double p = a + b + c + d;
		double CR = (a + d) - ((a + b)*(a + c) + (c + d)*(b + d))/p;
		CR /= p - ((a + b)*(a + c) + (c + d)*(b + d))/p;
		return CR;
	}

	public List<Cluster> getClusters() {
		return clusters;
	}

	public SeedingType getSeedType() {
		return seedType;
	}

	public void setSeedType(SeedingType seedType) {
		this.seedType = seedType;
	}

	public double getMaxWeightAbsoluteDifferenceGlobal() {
		return maxWeightAbsoluteDifferenceGlobal;
	}

	public void setMaxWeightAbsoluteDifferenceGlobal(
			double maxWeightAbsoluteDifferenceGlobal) {
		if (maxWeightAbsoluteDifferenceGlobal >= 0.0 && maxWeightAbsoluteDifferenceGlobal <= 1.0) {
			this.maxWeightAbsoluteDifferenceGlobal = maxWeightAbsoluteDifferenceGlobal;
		} else {
			throw new IllegalArgumentException("Parameter should be between 0.0 and 1.0");
		}

	}
	
	private boolean timeIsUp() {
		return ((System.currentTimeMillis() - this.initTimeMilis)/1000L) > TOTALTIMELIMITSECONDS;
	}

	@Override
	public int getIterationsToConverge() {
		return this.iteracoes;
	}

}
