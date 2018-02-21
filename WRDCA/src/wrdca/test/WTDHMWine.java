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
import java.util.List;
import java.util.StringTokenizer;

import wrdca.algo.WTDHMClustering;
import wrdca.util.Cluster;
import wrdca.util.ConfusionMatrix;
import wrdca.util.DissimMatrixDouble;


public class WTDHMWine {
	
	public static final int NELEM = 178;
	public static final int APRIORICLASSES = 3;

	public static void main(String[] args) throws IOException, IloException {
		DissimMatrixDouble tab2 = parseFile(DataFileNames.getString("WineDataset.WINE2"));
		DissimMatrixDouble tab3 = parseFile(DataFileNames.getString("WineDataset.WINE3"));
		DissimMatrixDouble tab4 = parseFile(DataFileNames.getString("WineDataset.WINE4"));		
		DissimMatrixDouble tab5 = parseFile(DataFileNames.getString("WineDataset.WINE5"));
		DissimMatrixDouble tab6 = parseFile(DataFileNames.getString("WineDataset.WINE6"));
		DissimMatrixDouble tab7 = parseFile(DataFileNames.getString("WineDataset.WINE7"));
		DissimMatrixDouble tab8 = parseFile(DataFileNames.getString("WineDataset.WINE8"));
		DissimMatrixDouble tab9 = parseFile(DataFileNames.getString("WineDataset.WINE9"));
		DissimMatrixDouble tab10 = parseFile(DataFileNames.getString("WineDataset.WINE10"));
		DissimMatrixDouble tab11 = parseFile(DataFileNames.getString("WineDataset.WINE11"));
		DissimMatrixDouble tab12 = parseFile(DataFileNames.getString("WineDataset.WINE12"));
		DissimMatrixDouble tab13 = parseFile(DataFileNames.getString("WineDataset.WINE13"));
		DissimMatrixDouble tab14 = parseFile(DataFileNames.getString("WineDataset.WINE14"));
		List<DissimMatrixDouble> dissimMatrices = new ArrayList<DissimMatrixDouble>(13);
		dissimMatrices.add(tab2);		
		dissimMatrices.add(tab3);
		dissimMatrices.add(tab4);
		dissimMatrices.add(tab5);
		dissimMatrices.add(tab6);
		dissimMatrices.add(tab7);
		dissimMatrices.add(tab8);
		dissimMatrices.add(tab9);
		dissimMatrices.add(tab10);
		dissimMatrices.add(tab11);
		dissimMatrices.add(tab12);
		dissimMatrices.add(tab13);
		dissimMatrices.add(tab14);
		
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
		final String classNames[] = {"class 1", "class 2", "class 3"};
		ConfusionMatrix confusionMatrix = new ConfusionMatrix(k, APRIORICLASSES, classNames);
		

		
		for (int i = 0; i < bestClusters.size(); i++) {
			Cluster cluster = bestClusters.get(i);
			for (Integer element : cluster.getElements()) {
				assert(element >= 0 && element < NELEM);
				final int classlabel;
				if (element <= 58) classlabel = 0;
				else if (element <= 129) classlabel = 1;
				else classlabel = 2;
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
		System.out.println(">>>>>>>>>>>> NMI  Index    is: " + confusionMatrix.nMIIndex());;

	}
	
	private static DissimMatrixDouble parseFile(String string) throws IOException {
		File file = new File(string);
		BufferedReader bufw = new BufferedReader(new FileReader(file));
		DissimMatrixDouble result = new DissimMatrixDouble(NELEM);
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
