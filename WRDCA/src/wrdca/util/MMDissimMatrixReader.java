package wrdca.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;
import java.util.Scanner;

public class MMDissimMatrixReader {
	
	public static DissimMatrix readMM(File mtxFile) throws IOException {
		return readMM(mtxFile, false);
	}

	public static DissimMatrix readMM(File mtxFile, boolean useFloats) throws IOException {
		Scanner scan = null;
		BufferedReader bufw = null;
		try {
			bufw = new BufferedReader(new FileReader(mtxFile));
			String line = bufw.readLine();
			if (!line.startsWith("%%MatrixMarket matrix coordinate real symmetric")) {
				throw new IllegalArgumentException("Invalid file format (should be MatrixMarket matrix coordinate real symmetric)");
			}
			scan = new Scanner(bufw);
			scan.useLocale(Locale.US);
			int lines, cols;
			lines = scan.nextInt();
			cols = scan.nextInt();
			if (lines != cols) {
				throw new IllegalStateException("Invalid dissimilarity matrix: #lines != #columns");
			}
			int nonZeros = scan.nextInt();
			int readN = 0;
			final DissimMatrix result = (useFloats) ? new DissimMatrixFloat(lines) : new DissimMatrixDouble(lines);
			while(scan.hasNextInt()) {
				int i = scan.nextInt(); i--;
				int j = scan.nextInt(); j--;
				final double val = scan.nextDouble();
				result.putDissim(i, j, val);
				readN++;
			}
			if (readN != nonZeros) {
				System.err.println("WARNING: Header says there are " + nonZeros + " values but we read " + readN + " instead");
			}
			return result;
		} finally {
			if (scan != null) scan.close();
			if (bufw != null) bufw.close();
		}
	}
}
