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

import java.io.IOException;
import java.io.Writer;

public class DissimMatrixDouble implements DissimMatrix {
	private double[][] matrix;
	private int nElems;
	
	public DissimMatrixDouble(int nElements) {
        this.matrix = new double[nElements][];
        for (int i = 0; i < nElements; i++) {
            this.matrix[i] = new double[i + 1];
        }
        this.nElems = nElements;
	}
	
	/**
	 * Retorna a dissimilaridade entre dois elementos <code>x</code> e <code>y</code> de acordo com
	 * essa matriz.
	 * @param x
	 * @param y
	 * @return
	 */
	public final double getDissim(int i, int j) {
        if (j > i) {
            return this.matrix[j][i];
        }
        return this.matrix[i][j];
	}
	
    public void putDissim(int i, int j, double dissim) {
        if (j > i) {
            this.matrix[j][i] = dissim;
        } else {
            this.matrix[i][j] = dissim;
        }
    }

	
	/**
	 * Numero de elementos da matriz de dissimilaridade
	 * @return
	 */
	public final int length() {
		return this.nElems;
	}
	
	public void printMatrix(Writer out) throws IOException {
		for(int i = 0; i < nElems; i++) {
			for(int j = 0; j < i; j++) {
				out.write(Double.toString(this.matrix[i][j]));
				out.write(',');
			}
			out.write(Double.toString(this.matrix[i][i]));
			out.write(System.lineSeparator());
		}
		out.flush();
	}
}
