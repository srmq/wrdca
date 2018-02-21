package wrdca.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.AbstractMap.SimpleImmutableEntry;

public class SimpleInputToNewChicoFormatTransformer {
	
	public static void main(String[] args) throws IOException {
		if (args.length != 4) {
			System.out.println("Should give the input file to transform, the number of objects in the file, the outputDescriptionFile and the outputDissimFile as arguments");
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
		
		File outputDescFile = new File(args[2]);
		File outputDissimFile = new File(args[3]);
		
		if (outputDescFile.isDirectory()) {
			System.out.println("Given output description file " + outputDescFile.toString() + " is a directory");
			System.exit(-4);
			
		}
		if (outputDissimFile.isDirectory()) {
			System.out.println("Given output dissim file " + outputDissimFile.toString() + " is a directory");
			System.exit(-5);
			
		}
		
		if (outputDescFile.exists() && !outputDescFile.canWrite()) {
			System.out.println("Given output description file " + outputDescFile.toString() + " cannot be written");
			System.exit(-6);
			
		}
		
		if (outputDissimFile.exists() && !outputDissimFile.canWrite()) {
			System.out.println("Given output dissim file " + outputDissimFile.toString() + " cannot be written");
			System.exit(-7);
			
		}
		

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
		DissimMatrixDouble dissimMatrix = new DissimMatrixDouble(n);
		for (int i = 0; i < n; i++) {
			final String line = buf.readLine();
			final StringTokenizer strtok = new StringTokenizer(line, ",");
			for(int j = 0; j <= i; j++) {
				final double dissim = Double.parseDouble(strtok.nextToken());
				dissimMatrix.putDissim(i, j, dissim);
			}
		}
		buf.close();
		{
			PrintStream descStream = new PrintStream(outputDescFile);
			printDescriptionFile(objects, classSet, descStream);
			descStream.flush();
			descStream.close();
		}
		{
			PrintStream dissimStream = new PrintStream(outputDissimFile);
			printDissimMatrix(dissimMatrix, dissimStream);
			dissimStream.flush();
			dissimStream.close();
		}

	}

	private static void printDissimMatrix(DissimMatrixDouble dissimMatrix,
			PrintStream stream) {
		for (int i = 0; i < dissimMatrix.length(); i++) {
			stream.print(String.format("%6.6e",dissimMatrix.getDissim(i, 0)));
			for (int j = 1; j < dissimMatrix.length(); j++) {
				stream.print(" " + String.format("%6.6e",dissimMatrix.getDissim(i, j)));
			}
			stream.println("");
		}
	}

	private static void printDescriptionFile(
			List<SimpleImmutableEntry<String, Integer>> objects,
			SortedSet<Integer> classSet, PrintStream stream) {

		stream.println(objects.size());
		Iterator<SimpleImmutableEntry<String, Integer>> it = objects.iterator();
		stream.print(it.next().getValue() + 1);
		while (it.hasNext()) {
			stream.print(" " + (it.next().getValue() + 1));
		}
		stream.println("");
		stream.println("X (number of input files)");
		stream.println("X (input files follow)");
		stream.println("(outputFileName)");
		stream.println(classSet.size());
		stream.println("1");
		stream.println("100");
		stream.println("0.0");
	}
	

}
