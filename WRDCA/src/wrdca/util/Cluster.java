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

import java.util.HashSet;
import java.util.Set;

public class Cluster implements Cloneable {
	private int center;
	
	private float[] weights;
	
	private Set<Integer> elements;
	public int getCenter() {
		return center;
	}
	public void setCenter(int center) {
		this.center = center;
	}
	public Set<Integer> getElements() {
		return elements;
	}
	public void setElements(Set<Integer> elements) {
		this.elements = elements;
	}
	
	public synchronized void remove(int element) {
		this.elements.remove(element);
	}
	
	public synchronized void add(int element) {
		this.elements.add(element);
	}
	public float[] getWeights() {
		return weights;
	}
	public void setWeights(float[] weights) {
		this.weights = weights;
	}
	
	@Override
	public Object clone() {
		Cluster result = new Cluster();
		result.center = this.center;
		result.weights = this.weights.clone();
		result.elements = new HashSet<Integer>(this.elements);
		return result;
	}
}
