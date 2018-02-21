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
package wrdca.algo;

import java.util.List;

import wrdca.util.Cluster;


public interface ClusterAlgorithm {
	public void cluster(int k) throws Exception;
	public double calcJ(List<Cluster> clusters);
	public List<Cluster> getClusters();
	public int getIterationsToConverge();
	public void setMaxIterations(int maxIterations);
	double[] regretsForElement(int i, List<Cluster> clusters);
}
