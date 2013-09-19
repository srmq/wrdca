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
package wrdca.util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ConfusionMatrix {
	private int[][] clusterCounts;
	private int k;
	private int nClasses;
	double bestJ;
	private Map<Integer, ObjectClassif> objectClassifs;
	private List<Set<Integer>> aprioriClasses;
	private List<Set<Integer>> clusters;
	private String[] classNames;
	
	private static class ObjectClassif {
		public int indexCluster;
		public int indexClassApriori;
		public ObjectClassif(int indexCluster, int indexClassApriori) {
			this.indexCluster = indexCluster;
			this.indexClassApriori = indexClassApriori;
		}
	}
	
	public ConfusionMatrix(int k, int aprioriClassNumber, String[] classNames) {
		this(k, aprioriClassNumber);
		this.classNames = classNames;
		if (classNames.length != aprioriClassNumber) {
			throw new IllegalArgumentException("Number of class names must be the same as the aprioriClassNumber");
		}
	}
	
	public ConfusionMatrix(int k, int aprioriClassesNumber) {
		clusterCounts = new int[k][aprioriClassesNumber];
		this.k = k;
		this.nClasses = aprioriClassesNumber;
		this.objectClassifs = new HashMap<Integer, ObjectClassif>();
		this.aprioriClasses = new ArrayList<Set<Integer>>(aprioriClassesNumber);
		for (int i = 0; i < this.nClasses; i++) {
			this.aprioriClasses.add(new HashSet<Integer>());
		}
		this.clusters = new ArrayList<Set<Integer>>(k);
		for (int i = 0; i < this.k; i++) {
			this.clusters.add(new HashSet<Integer>());
		}
		assert(this.aprioriClasses.size() == aprioriClassesNumber && this.clusters.size() == k);

	}
	
	public void putObject(int indexObject, int indexCluster, int classIndexObject) {
		if (indexCluster < 0 || indexCluster >= this.k) {
			throw new IllegalArgumentException("indexCluster out of range: " + indexCluster + ". Should be between 0 and " + (this.k-1));
		}
		if (classIndexObject < 0 || classIndexObject >= this.nClasses) {
			throw new IllegalArgumentException("classIndexObject out of range: " + classIndexObject + ". Should be between 0 and " + (this.nClasses-1));
		}
		clusterCounts[indexCluster][classIndexObject]++;
		this.objectClassifs.put(indexObject, new ObjectClassif(indexCluster, classIndexObject));
		this.aprioriClasses.get(classIndexObject).add(indexObject);
		this.clusters.get(indexCluster).add(indexObject);
	}

	public int computeTotalOfClass(int classIndex) {
		int total = 0;
		for (int i = 0; i < k; i++) {
			total += clusterCounts[i][classIndex];
		}
		return total;
	}
	
	public int computeTotalOfCluster(int clusterIndex) {
		int total = 0;
		for (int j = 0; j < this.nClasses; j++) {
			total += clusterCounts[clusterIndex][j];
		}
		return total;
	}
	
	public int computeClassIClusterJ(int i, int j) {
		return clusterCounts[j][i];
	}
	
	public int getK() {
		return k;
	}
	
	public int computeGrandTotal() {
		int total = 0;
		for (int i = 0; i < k; i++) {
			total += computeTotalOfCluster(i);
		}
		assert ((total == totalByLine()) && (total == this.objectClassifs.size()));
		return total;
	}
	
	private int totalByLine() {
		int total = 0;
		for (int j = 0; j < this.nClasses; j++) {
			total += computeTotalOfClass(j);
		}
		return total;
	}

	public double getBestJ() {
		return bestJ;
	}

	public void setBestJ(double bestJ) {
		this.bestJ = bestJ;
	}
	
	public double calcPrecision(int partitionI, int clusterJ) {
		double result = ((double)this.computeClassIClusterJ(partitionI, clusterJ))/this.computeTotalOfCluster(clusterJ);
		return result;
	}
	
	public double calcRecall(int partitionI, int clusterJ) {
		double result = ((double)this.computeClassIClusterJ(partitionI, clusterJ))/this.computeTotalOfClass(partitionI);
		return result;
	}
	
	public double fMeasure(int partitionI, int clusterJ) {
		double result = 2.0*(this.calcPrecision(partitionI, clusterJ)*this.calcRecall(partitionI, clusterJ))/(this.calcPrecision(partitionI, clusterJ)+this.calcRecall(partitionI, clusterJ));
		return result;
	}
	
	public double fMeasureGlobal() {
		double n = this.computeGrandTotal();
		double sum = 0;
		for (int i = 0; i < this.nClasses; i++) {
			sum += this.computeTotalOfClass(i)*this.maxFmeasure(i);
		}
		double result = sum / n;
		return result;
	}
	
	private double maxFmeasure(int partitionI) {
		double result = -1;
		double currMeasure;
		for (int j = 0; j < this.k; j++) {
			currMeasure = this.fMeasure(partitionI, j);
			if (currMeasure > result) result = currMeasure;
		}
		return result;
	}
	
	private int maxElemsClassInClusterJ(int j) {
		int max = 0;
		for (int i = 0; i < this.nClasses; i++) {
			final int classIInClusterJ;
			if ((classIInClusterJ = this.computeClassIClusterJ(i, j)) > max) {
				max = classIInClusterJ;
			}
		}
		return max;
	}
	
	public double OERCIndex() {
		double result = 0;
		for (int j = 0; j < this.k; j++) {
			result += this.maxElemsClassInClusterJ(j);
		}
		result /= this.totalByLine();
		result = 1.0 - result;
		return result;
	}
	
	public double CRIndex() {
		double a, b, c, d;
		a = b = c = d = 0.0;
		byte agreeCluster, agreeClass;
		final int nElems = this.computeGrandTotal();
		for (int i = 0; i < nElems; i++) {
			final ObjectClassif classifI = this.objectClassifs.get(i);
			final int clusterI = classifI.indexCluster;
			final int classLabelI = classifI.indexClassApriori;
			for (int j = i + 1; j < nElems; j++) {
				final ObjectClassif classifJ = this.objectClassifs.get(j);
				final int clusterJ = classifJ.indexCluster;
				final int classLabelJ = classifJ.indexClassApriori;
				agreeCluster = (clusterI == clusterJ) ? (byte)1 : (byte)0;
				agreeClass = (classLabelI == classLabelJ) ? (byte)1 : (byte)0;
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
	
	public double nMIIndex() {
		final double n = this.computeGrandTotal();
		double numerador = 0;
		double den1 = 0;
		double den2 = 0;
		for (int partitionH = 0; partitionH < this.nClasses; partitionH++) {
			final double nha = this.aprioriClasses.get(partitionH).size();
			den1 += nha/n * Math.log(nha/n);
			for (int clusterL = 0; clusterL < this.k; clusterL++) {
				final double nlb = this.clusters.get(clusterL).size();
				Set<Integer> nhlSet = new HashSet<Integer>(this.aprioriClasses.get(partitionH));
				nhlSet.retainAll(this.clusters.get(clusterL));
				final double nhl = nhlSet.size();
				if(nhl != 0) {
					if (nha * nlb != 0) { 
						final double addToNumerador = nhl/n * Math.log((n * nhl)/(nha * nlb)); 
						numerador += addToNumerador;
					}
				}
				if (partitionH == 0) {
					if (nlb != 0) {
						den2 += nlb/n * Math.log(nlb/n);
					}
				}
			}
		}
		den1 *= -1.0;
		den2 *= -1.0;
		final double denominador = (den1+den2)/2.0;
		return numerador/denominador;
	}

	public void printMatrix(PrintStream out) {
		out.print("Clusters\tClasses\n");
		if (this.classNames != null) {
			assert this.classNames.length == this.nClasses;
			for (String name : classNames) {
				out.print("\t" + name);
			}
		} else {
			for (int i = 0; i < this.nClasses; i++) {
				out.print("\t" + i);
			}
		}
		out.print('\n');
		for (int i = 0; i < this.k; i++) {
			out.print(i);
			for (int c = 0; c < this.nClasses; c++) {
				out.print("\t" + this.clusterCounts[i][c]);
			}
			out.print('\n');
		}
	}

}
