package wrdca.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

public class SimpleInputToSodasFileTransformer {

	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.out.println("Should give the input file to transform and the number of objects in the file as argument");
			System.exit(-1);
		}
		File inputFile = new File(args[0]);
		int n = Integer.parseInt(args[1]);
		if (!inputFile.isFile()) {
			System.out.println("Argument: " + inputFile.toString() + " is not a file");
			System.exit(-2);
		}
		if (!inputFile.canRead()) {
			System.out.println("Input file " + inputFile.toString() + " is not readable");
			System.exit(-3);
		}
		BufferedReader buf = new BufferedReader(new FileReader(inputFile));

		List<AbstractMap.SimpleImmutableEntry<String, Integer>> objects = new ArrayList<AbstractMap.SimpleImmutableEntry<String, Integer>>(n);
		SortedSet<Integer> classSet = new TreeSet<Integer>();
		for (int i = 0; i < n; i++) {
			final String line = buf.readLine();
			int classVar = Integer.parseInt(line.substring(0, line.indexOf(',')));
			classSet.add(classVar);
			String objectId = line.substring(line.indexOf('"')+1, line.length()-1);
			AbstractMap.SimpleImmutableEntry<String, Integer> entry = new AbstractMap.SimpleImmutableEntry<String, Integer>(objectId, classVar);
			objects.add(entry);
		}
		DissimMatrix dissimMatrix = new DissimMatrix(n);
		for (int i = 0; i < n; i++) {
			final String line = buf.readLine();
			final StringTokenizer strtok = new StringTokenizer(line, ",");
			for(int j = 0; j <= i; j++) {
				final double dissim = Double.parseDouble(strtok.nextToken());
				dissimMatrix.putDissim(i, j, dissim);
			}
		}
		buf.close();
		printSodas(objects, dissimMatrix, classSet, System.out);

	}

	private static void printSodas(
			List<SimpleImmutableEntry<String, Integer>> objects,
			DissimMatrix dissimMatrix, SortedSet<Integer> classSet, PrintStream stream) {

		stream.println("SODAS = (");
		stream.println("		CONTAINS = (");
		stream.println("		   FILES, HEADER, INDIVIDUALS, VARIABLES, RECTANGLE_MATRIX");
		stream.println("		), ");
		stream.println("		FILE =  ("); 
		stream.println("		   procedure_name = \"db2so\" ,");
		stream.println("		   version = \"sans\" ,");
		stream.println("		   create_date = \"\"");
		stream.println("		),");
		stream.println("		HEADER =  ("); 
		stream.println("		   title = \"dissimFile\" ,");
		stream.println("		   sub_title = \"Generated by SimpleInputToSodas\" ,");
		stream.println("		   indiv_nb = " + objects.size() + ",");
		stream.println("		   var_nb = 2 ,");
		stream.println("		   rules_nb = 0 ,");
		stream.println("		   nb_var_set = 0 ,");
		stream.println("		   nb_indiv_set = 0 ,");
		stream.println("		   nb_var_nom = 0 ,");
		stream.println("		   nb_var_cont = 0 ,");
		stream.println("		   nb_var_text = 0 ,");
		stream.println("		   nb_var_cont_symb = 1 ,");
		stream.println("		   nb_var_nom_symb = 1 ,");
		stream.println("		   nb_var_nom_mod = 0 ,");
		stream.println("		   nb_na = 0 ,");
		stream.println("		   nb_null = 0 ,");
		stream.println("		   nb_hierarchies = 0 ,");
		stream.println("		   nb_nu = 0");
		stream.println("		),");
		stream.println("");
		stream.println("INDIVIDUALS = (");
		stream.print("(0, \"AA00\", \"I_1\" )");
		for (int i = 1; i < objects.size(); i++) {
			stream.println(",");
			stream.print("  (" + i + ", \"AA" + i + "\", \"I_" + i + "\" )");
		}
		stream.println("");
		stream.println(" ),");
		stream.println("VARIABLES =  (");
		stream.println(" (1 ,nominal ,\"\" ,\"AF00\" ,\"Apriori Class\" , 0, 0 ," + classSet.size() + ", (");
		{
			Iterator<Integer> it = classSet.iterator();
			int i = it.next() + 1; // categories are one based in sodas
			stream.print("	(" + i +" ,\"CL" + i + "\" ,\"Class" + i + "\" ,0)");
			while(it.hasNext()) {
				i = it.next() + 1;
				stream.println(",");
				stream.print("	(" + i +" ,\"CL" + i + "\" ,\"Class" + i + "\" ,0)");
			}
			stream.println(" )");
			stream.println("  )");
			stream.println("),");
			stream.println(" (2 ,inter_cont ,\"\" ,\"FV00\" ,\"fake variable\" ,0, 0, 0.0, 0.0)");
			stream.println("),");
		}
		stream.println("RECTANGLE_MATRIX = (");
		Iterator<SimpleImmutableEntry<String, Integer>> it = objects.iterator();
		SimpleImmutableEntry<String, Integer> element = it.next();
		stream.print("(" + (element.getValue()+1) + ",( 0.0 : 0.0 ))"); //one-based in SODAS
		while (it.hasNext()) {
			element = it.next();
			stream.println(",");
			stream.print("(" + (element.getValue()+1) + ",( 0.0 : 0.0 ))");
		}
		stream.println("");
		stream.println("),");
		stream.println("DIST_MATRIX= (");
		stream.print("(" + String.format("%6.6e",dissimMatrix.getDissim(0, 0)) + ")");
		for (int i = 1; i < dissimMatrix.length(); i++) {
			stream.println(",");
			stream.print("(");
			stream.print(String.format("%6.6e",dissimMatrix.getDissim(i, 0)));
			for (int j = 1; j <= i; j++) {
				stream.print(", " + String.format("%6.6e",dissimMatrix.getDissim(i, j)));
			}
			stream.print(")");
		}
		stream.println("");
		stream.println("))");
		stream.println("END");
	}

}
