package wrdca.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class ProcessedOutputToTableWriter {

	private static class ClusterElement {
		private String aPrioriClass;
		private String name;
		public ClusterElement(String aPrioriClass, String name) {
			this.aPrioriClass = aPrioriClass;
			this.name = name;
		}
	}
	
	private static class Cluster {
		private List<ClusterElement> elements;
		public Cluster() {
			this.elements = new LinkedList<ClusterElement>();
		}
	}

	public static void main(String[] args) throws IOException {
		List<Cluster> clusters = new LinkedList<Cluster>();
		if (args.length != 1) {
			System.err.println("Should specify which file to read");
			System.exit(-1);
		}
		File f = new File(args[0]);
		try (BufferedReader bufw = new BufferedReader(new FileReader(f))) {
			String line;
			while((line = bufw.readLine()) != null) {
				if (line.startsWith("Cluster ") && Character.isDigit(line.charAt(8))) {
					do {
						line = bufw.readLine();
					} while (!line.startsWith("-- BEGIN MEMBERS"));
					Cluster clust = new Cluster();
					line = bufw.readLine();
					while (!line.startsWith("-- END MEMBERS")) {
						String elemLine = line;
						if (elemLine.charAt(0) == '\"') {
							elemLine = line.substring(1, line.lastIndexOf('\"'));
						}
						String pClass = elemLine.substring(0, elemLine.lastIndexOf('/'));
						String id = elemLine.substring(elemLine.lastIndexOf('/')+1, elemLine.length());
						ClusterElement clusElem = new ClusterElement(pClass, id);
						clust.elements.add(clusElem);
						
						line = bufw.readLine();
					}
					clusters.add(clust);
				}
			}
		}
		printClusters(clusters);
	}

	private static void printClusters(List<Cluster> clusters) {
		System.out.println("Cluster Real Id");
		int clusterIndex = 0;
		for (Cluster cluster : clusters) {
			for (ClusterElement el : cluster.elements) {
				System.out.println(clusterIndex + " " + el.aPrioriClass + " " + el.name);
			}
			clusterIndex++;
		}
		
	}
}
