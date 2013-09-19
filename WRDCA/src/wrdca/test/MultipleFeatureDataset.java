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
import java.util.List;
import java.util.StringTokenizer;

import wrdca.algo.WTDHMClustering;
import wrdca.util.Cluster;
import wrdca.util.ConfusionMatrix;
import wrdca.util.DissimMatrix;


public class MultipleFeatureDataset {
	public static final int NELEM = 2000;
	public static final int NELEM_BY_CLASS = 200;
	public static final int APRIORICLASSES = 10;
	/**
	 * @param args
	 * @throws IloException 
	 */
	public static void main(String[] args) throws IOException, IloException {
		DissimMatrix dissimFac = parseFile(DataFileNames.getString("MultipleFeatureDataset.FACMATRIX"));
		DissimMatrix dissimFou = parseFile(DataFileNames.getString("MultipleFeatureDataset.FOUMATRIX"));
		DissimMatrix dissimKar = parseFile(DataFileNames.getString("MultipleFeatureDataset.KARMATRIX"));
		DissimMatrix dissimMor = parseFile(DataFileNames.getString("MultipleFeatureDataset.MORMATRIX"));
		DissimMatrix dissimPix = parseFile(DataFileNames.getString("MultipleFeatureDataset.PIXMATRIX"));
		DissimMatrix dissimZer = parseFile(DataFileNames.getString("MultipleFeatureDataset.ZERMATRIX"));
		List<DissimMatrix> dissimMatrices = new ArrayList<DissimMatrix>(6);
		dissimMatrices.add(dissimFac);
		dissimMatrices.add(dissimFou);
		dissimMatrices.add(dissimKar);
		dissimMatrices.add(dissimMor);
		dissimMatrices.add(dissimPix);
		dissimMatrices.add(dissimZer);
		final int NUMBER_OF_RUNS = 100;
		int k = APRIORICLASSES;
		double bestJ = Double.MAX_VALUE;
		List<Cluster> bestClusters = null;

		for (int i = 0; i < NUMBER_OF_RUNS; i++) {
			System.out.println("Run number: " + (i+1));
			WTDHMClustering clust = new WTDHMClustering(dissimMatrices);
			//clust.setSeedType(WTDHMClustering.SeedingType.PLUSPLUS_SEED);
			clust.cluster(k);
			final List<Cluster> myClusters = clust.getClusters();
			final double myJ = clust.calcJ(myClusters);
			if (myJ < bestJ) {
				bestJ = myJ;
				bestClusters = myClusters;
			}
		}			

		ConfusionMatrix confusionMatrix = new ConfusionMatrix(k, APRIORICLASSES);
		
		for (int i = 0; i < bestClusters.size(); i++) {
			Cluster cluster = bestClusters.get(i);
			for (Integer element : cluster.getElements()) {
				assert(element >= 0 && element < NELEM);
				final int classlabel = (element/NELEM_BY_CLASS);
				confusionMatrix.putObject(element, i, classlabel);
			}
		}
		
		System.out.println(">>>>>>>>>>>> The F-Measure is: "+ confusionMatrix.fMeasureGlobal());
		System.out.println(">>>>>>>>>>>> The CR-Index  is: "+ confusionMatrix.CRIndex());
		System.out.println(">>>>>>>>>>>> OERC Index    is: " + confusionMatrix.OERCIndex());
		System.out.println(">>>>>>>>>>>> NMI  Index    is: " + confusionMatrix.nMIIndex());;
		

	}

	private static DissimMatrix parseFile(String string) throws IOException {
		File file = new File(string);
		BufferedReader bufw = new BufferedReader(new FileReader(file));
		DissimMatrix result = new DissimMatrix(NELEM);
		String line;
		while((line = bufw.readLine()).indexOf("DIST_MATRIX") == -1)
			;
		for (int i = 0; i < NELEM; i++) {
			line = bufw.readLine();
			StringTokenizer sttok = new StringTokenizer(line, ", ()", false); //$NON-NLS-1$
			for (int j = 0; j < i; j++) {
				String dissimji = sttok.nextToken();
			 result.putDissim(i, j, Float.parseFloat(dissimji));
			}
			assert(Float.parseFloat(sttok.nextToken()) == 0f);
		}
		bufw.close();
		return result;
	}

}
