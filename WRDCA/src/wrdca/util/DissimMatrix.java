package wrdca.util;

import java.io.IOException;
import java.io.Writer;

public interface DissimMatrix {
	/**
	 * Retorna a dissimilaridade entre dois elementos <code>x</code> e <code>y</code> de acordo com
	 * essa matriz.
	 * @param x
	 * @param y
	 * @return
	 */
	public double getDissim(int i, int j);
	
    public void putDissim(int i, int j, double dissim);

	
	public int length();
	
	public void printMatrix(Writer out) throws IOException;

}
