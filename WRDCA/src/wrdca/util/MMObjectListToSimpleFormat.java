package wrdca.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class MMObjectListToSimpleFormat {
	public static void genSimpleFormatFile(File mmFile, File objectList, File outputSimpleFormat) throws IOException {
		DissimMatrix dissim = MMDissimMatrixReader.readMM(mmFile);
		BufferedWriter bufw = new BufferedWriter(new FileWriter(outputSimpleFormat));
		
		BufferedReader read = new BufferedReader(new FileReader(objectList));
		String line;
		while((line = read.readLine()) != null) {
			bufw.write(line);
			bufw.write(System.lineSeparator());
		}
		read.close();
		dissim.printMatrix(bufw);
		bufw.close();
	}
	public static void main(String[] args) throws IOException{
		//e.g. "/home/srmq/git/nlp/srmq-nlp/experiments/20newsgroups-sample10-lsa50-Dissims.mtx"
		File mmFile = new File(args[0]);
		
		//e.g. "/home/srmq/Documents/Research/textmining/devel/data/20_newsgroups-noheaders-sample10-matrixElements.txt"
		File objectList = new File(args[1]);
		
		////e.g. "/home/srmq/git/nlp/srmq-nlp/experiments/20newsgroups-sample10-lsa50Dissim.txt"
		File outputFile = new File(args[2]);
		genSimpleFormatFile(mmFile, objectList, outputFile);
	}
}
