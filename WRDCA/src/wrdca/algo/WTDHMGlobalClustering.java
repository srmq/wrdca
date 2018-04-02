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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import wrdca.util.Cluster;
import wrdca.util.DissimMatrix;


public class WTDHMGlobalClustering implements ClusterAlgorithm {
	private List<? extends DissimMatrix> dissimMatrices;
	private final int nElems;
	private final int nCriteria;
	
	private int iteracoes = 0;
	
	private double maxWeightAbsoluteDifferenceGlobal = 1.0;
	
	public static enum SeedingType {
		RANDOM_SEED, PLUSPLUS_SEED
	}

	private SeedingType seedType = SeedingType.RANDOM_SEED;
	
	private static final Random rnd = new Random(1);
	
	public static final boolean DEBUG = false;
	
	
	public static final double EPSILON = 0.000001;

	
	public static final int BIG_CONSTANT = Integer.MAX_VALUE/100;
	
	private int maxIterations = Integer.MAX_VALUE;


	private List<Cluster> clusters;
	private int[] indexInClustersForElem;

	public WTDHMGlobalClustering(List<? extends DissimMatrix> dissimMatrices) {
		this.dissimMatrices = dissimMatrices;
		this.nElems = dissimMatrices.get(0).length();
		this.nCriteria = dissimMatrices.size();
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

	private float calcRegret(int candidateCenter, Cluster c, float currentRegret) {
		float sumRegret = 0.0f;
		for (Iterator<Integer> iterator = c.getElements().iterator(); iterator.hasNext();) {
			Integer el = iterator.next();
			sumRegret += maxRegret(el, candidateCenter, c);
			if (sumRegret > currentRegret) return BIG_CONSTANT;
		}
		return sumRegret;
	}

	private List<Cluster> cloneClusters() {
		List<Cluster> clusterClone = new ArrayList<Cluster>(this.clusters.size());
		for (Cluster cluster : this.clusters) {
			clusterClone.add((Cluster) cluster.clone());
		}
		//global weights
		float[] weights = clusterClone.get(0).getWeights();
		for (int i = 1; i < clusterClone.size(); i++) {
			clusterClone.get(i).setWeights(weights);
		}
		return clusterClone;
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
			double _J;
			++iteracoes;
			if (DEBUG) {
				_oldClusters = cloneClusters();
				_J = calcJ(_oldClusters);
			}
			List<Cluster> clusters = bestPrototypes();
			final double maxValue = calcJ(clusters);
			if (this.dissimMatrices.size() > 1) {
				if (DEBUG) {
					assert (maxValue <= _J)|| (Math.abs(maxValue - _J) < EPSILON);
					System.out.println("J before updating weights is: " + maxValue);
				}
	
				final double regret = updateWeights(clusters, maxValue);
				if (DEBUG) {
					System.out.println("Regret of Clusters AFTER update weights: " + regret);
					System.out.println("And wit calcJ this means: " + calcJ(clusters));
				}
	
				if (DEBUG) {
					final double myJ = calcJ(clusters);
					assert ((myJ <= _J) || (Math.abs(myJ - _J) < EPSILON));
				}
			}
			changed = clusterAssign(clusters);
			if (DEBUG) {
				List<Cluster> newClusters = cloneClusters();
				final double J = calcJ(newClusters);
				
				// assert(J <= currJ || (Math.abs(currJ - J) < EPSILON));
				if (!(J <= currJ || (Math.abs(currJ - J) < EPSILON))) {
					System.out.println("boom");
				}
				currJ = J;
				System.out.println(">>>>>>>>>> J: " + J);
			}
		} while (changed && (iteracoes <= this.maxIterations));
		if (DEBUG) {
			System.out.println("------ FIM DE CALCULO -----");

		}
	}

	private double updateWeights(List<Cluster> clusters, double maxValue) throws IloException {
		double regret = -1;
		IloCplex cplex = new IloCplex();
		if (!DEBUG) {
			cplex.setOut(null);
		}
		IloNumVar[] pVars = new IloNumVar[this.nElems];
		if(DEBUG) {
			int numberOfElems = 0;
			for (Cluster cluster : clusters) {
				numberOfElems += cluster.getElements().size();
			}
			assert(numberOfElems == pVars.length);
		}
		for (int i = 0; i < pVars.length; i++) {
			pVars[i] = cplex.numVar(0.0, BIG_CONSTANT);
		}
		IloNumVar[] weights = new IloNumVar[nCriteria];
		for (int i = 0; i < weights.length; i++) {
			weights[i] = cplex.numVar(0.0, 1.0);
		}
		{
			int i = 0;
			for (Cluster c : clusters) {
				for (Iterator<Integer> iterator = c.getElements().iterator(); iterator.hasNext();) {
					Integer element = iterator.next();
					addEquations(element.intValue(), c.getCenter(), cplex, pVars[i], weights);
					i++;
				}
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
			if (cplex.getStatus() != IloCplex.Status.Optimal) {
				System.out.println("booomm");
			}
			regret = cplex.getObjValue();
			double[] newWeights = cplex.getValues(weights);
			float[] weightVector = clusters.get(0).getWeights();
			if (weightVector == null) {
				weightVector = new float[newWeights.length];
			}
			for (int i = 0; i < newWeights.length; i++) {
				weightVector[i] = (float)newWeights[i];
			}
			for (Cluster c : clusters) {
				c.setWeights(weightVector);
			}
		}
		cplex.end(); 
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
	
	
	private List<Cluster> bestPrototypes() {
		Float[] minSumRegrets = new Float[this.clusters.size()];
		Arrays.fill(minSumRegrets, new Float(BIG_CONSTANT));
		
		IntStream.range(0, nElems).parallel().forEach(n -> {
			// para cada elemento ver se ele e melhor do que o que temos agora
			for (int k = 0; k < this.clusters.size(); k++) {
				final float regret = calcRegret(n, clusters.get(k), minSumRegrets[k]);
				synchronized(minSumRegrets[k]) {
					if ((regret + EPSILON) < minSumRegrets[k]) {
						clusters.get(k).setCenter(n);
						minSumRegrets[k] = regret;
					}
				}
			}
		});
		return clusters;
	}
	
	
	private void initialize(int k) {
		this.indexInClustersForElem = new int[nElems];
		Arrays.fill(this.indexInClustersForElem, -1);
		List<Cluster> clusters = firstGenClusters(k);
		//Global weights
		float[] weightVector = new float[nCriteria];
		Arrays.fill(weightVector, 1.0f/nCriteria);
		for (Cluster cluster : clusters) {
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


	private final int isCenterOf(int index) {
		for (int i = 0; i < this.clusters.size(); i++) {
			if (this.clusters.get(i).getCenter() == index) {
				return i;
			}
		}
		return -1;
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
	
	
	private boolean clusterAssign(List<Cluster> clusters) {
		boolean[] hasChanged = {false};
		IntStream.range(0, nElems).parallel().forEach(i -> {
			int centerOf;
			if ((centerOf = isCenterOf(i)) == -1) {
				int clusterIndexMinDist = -1;
				double minDist = BIG_CONSTANT;

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
					synchronized(hasChanged) {
						hasChanged[0] = true;
					}
				}
			} else {
				if (this.indexInClustersForElem[i] != centerOf) {
					if (this.indexInClustersForElem[i] >= 0 && this.indexInClustersForElem[i] < clusters.size()) {
						clusters.get(this.indexInClustersForElem[i]).remove(i);
					}

					this.indexInClustersForElem[i] = centerOf;
					clusters.get(centerOf).add(i);
					synchronized (hasChanged) {
						hasChanged[0] = true;						
					}
				}
			}
		});
		return hasChanged[0];
	}
	

	private List<Cluster> firstGenClusters(int k) {
		List<Cluster> result = new ArrayList<Cluster>(k);
		for (int i = 0; i < k; i++) {
				Cluster c = new Cluster();
				c.setElements(new HashSet<Integer>());
				result.add(c);
		}
		return result;
	}

	public List<Cluster> getClusters() {
		return clusters;
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

	public int getIterationsToConverge() {
		return this.iteracoes;
	}
	
	public void setMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
	}

	public double[] regretsForElement(int i, List<Cluster> clusters) {
		double[] res = new double[clusters.size()];
		{
			int ci=0;
			for (Iterator<Cluster> it = clusters.iterator(); it.hasNext(); ci++) {
				final Cluster clust = it.next();
				res[ci] = maxRegret(i, clust.getCenter(), clust);
			}
		}
		return res;
	}
	
	
}
