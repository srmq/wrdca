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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

import wrdca.algo.WTDHMClustering;
import wrdca.util.Cluster;
import wrdca.util.ConfusionMatrix;
import wrdca.util.DissimMatrix;


public class WTDHMTestImageDatabase {
	public static final int NELEM = 1026;
	private int[] clusterNum = null; 
	public static final int APRIORICLASSES = 19;
	
	public static void main(String[] args)  throws IOException, IloException {
		WTDHMTestImageDatabase test = new WTDHMTestImageDatabase();
		DissimMatrix tab1 = test.parseFile(DataFileNames.getString("ImageDataset.DESC"));
		DissimMatrix tab2 = test.parseFile(DataFileNames.getString("ImageDataset.COL_HST"));
		DissimMatrix tab3 = test.parseFile(DataFileNames.getString("ImageDataset.COL_POS"));
		DissimMatrix tab4 = test.parseFile(DataFileNames.getString("ImageDataset.GABOR_HST"));
		DissimMatrix tab5 = test.parseFile(DataFileNames.getString("ImageDataset.GABOR_POS"));
		List<DissimMatrix> dissimMatrices = new ArrayList<DissimMatrix>(5);
		dissimMatrices.add(tab1);
		dissimMatrices.add(tab2);
		dissimMatrices.add(tab3);
		dissimMatrices.add(tab4);
		dissimMatrices.add(tab5);
		
		double bestJ = Double.MAX_VALUE;
		List<Cluster> bestClusters = null;
		final int NUMBER_OF_RUNS = 100;
		int k = APRIORICLASSES;
		
		for (int i = 0; i < NUMBER_OF_RUNS; i++) {
			System.out.println("Run number: " + (i+1));
			WTDHMClustering clust = new WTDHMClustering(dissimMatrices);
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
				final int classlabel = test.clusterNum[element];
				confusionMatrix.putObject(element, i, classlabel);
			}
		}
		
		
		System.out.println(">>>>>>>>>>>> The F-Measure is: "+ confusionMatrix.fMeasureGlobal());
		System.out.println(">>>>>>>>>>>> The CR-Index  is: "+ confusionMatrix.CRIndex());
		System.out.println(">>>>>>>>>>>> OERC Index    is: " + confusionMatrix.OERCIndex());
		System.out.println(">>>>>>>>>>>> NMI  Index    is: " + confusionMatrix.nMIIndex());;
	}
	
	private DissimMatrix parseFile(String string) throws IOException {
		final File file = new File(string);
		final BufferedReader bufw = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));
		String line;
		final boolean readClustersId = (clusterNum == null);
		if (readClustersId) {
			this.clusterNum = new int[NELEM];
		}
		for (int i = 0; i < NELEM; i++) {
			line = bufw.readLine();
			if (readClustersId) {
				line = line.substring(0, line.indexOf(','));
				this.clusterNum[i] = Integer.parseInt(line);
			}
		}
		DissimMatrix result = new DissimMatrix(NELEM);
		for (int i = 0; i < NELEM; i++) {
			line = bufw.readLine();
			StringTokenizer tokenizer = new StringTokenizer(line, ", \t\n\r\f");
			for (int j = 0; j <= i; j++) {
				result.putDissim(i, j, Double.parseDouble(tokenizer.nextToken()));
			}
		}
		bufw.close();
		return result;
	}
}
/*
Resultados iniciais:
>>>>>>>>>>>> The F-Measure is: 0.4008033503561878
>>>>>>>>>>>> The CR-Index  is: 0.15053070111718514
>>>>>>>>>>>> OERC Index    is: 0.5584795321637427
>>>>>>>>>>>> NMI  Index    is: 0.3514668481557376
*/