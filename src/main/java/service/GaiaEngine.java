package service;

import model.Alternative;
import model.Criterion;

import java.util.*;

/**
 * Computes the GAIA plane projection using Principal Component Analysis (PCA).
 * The GAIA plane provides a 2D visualization of alternatives and criteria
 * based on the unicriterion net flows from the PROMETHEE method.
 *
 * <p>The algorithm works as follows:</p>
 * <ol>
 *   <li>Build the unicriterion net flow matrix (N alternatives × C criteria)</li>
 *   <li>Center the matrix by subtracting column means</li>
 *   <li>Compute the covariance matrix (C × C)</li>
 *   <li>Extract the two principal eigenvectors using Power Iteration</li>
 *   <li>Project alternatives and criteria onto the 2D plane</li>
 *   <li>Compute the PROMETHEE decision axis (pi vector)</li>
 * </ol>
 *
 * @author Developer
 */
public class GaiaEngine {

    private static final int MAX_ITERATIONS = 200;
    private static final double CONVERGENCE_THRESHOLD = 1e-10;

    /**
     * Computes the full GAIA plane data for a set of alternatives and criteria.
     *
     * @param alternatives the list of evaluated alternatives (with flows already computed)
     * @param criteria the list of criteria used in the evaluation
     * @return a Map containing the projected coordinates for alternatives, criteria, and the decision axis
     */
    public Map<String, Object> computeGaia(List<Alternative> alternatives, List<Criterion> criteria) {
        int n = alternatives.size();
        int c = criteria.size();

        if (n < 2 || c < 2) {
            return Collections.emptyMap();
        }

        // Step 1: Build the unicriterion net flow matrix (N x C)
        double[][] flowMatrix = buildUnicriterionFlowMatrix(alternatives, criteria);

        // Step 2: Center the matrix (subtract column means)
        double[] colMeans = new double[c];
        for (int j = 0; j < c; j++) {
            double sum = 0;
            for (int i = 0; i < n; i++) sum += flowMatrix[i][j];
            colMeans[j] = sum / n;
        }
        double[][] centered = new double[n][c];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < c; j++) {
                centered[i][j] = flowMatrix[i][j] - colMeans[j];
            }
        }

        // Step 3: Compute the covariance matrix (C x C)
        double[][] covariance = new double[c][c];
        for (int i = 0; i < c; i++) {
            for (int j = 0; j <= i; j++) {
                double sum = 0;
                for (int k = 0; k < n; k++) {
                    sum += centered[k][i] * centered[k][j];
                }
                covariance[i][j] = sum / (n - 1);
                covariance[j][i] = covariance[i][j]; // Symmetric
            }
        }

        // Step 4: Extract the two principal eigenvectors via Power Iteration
        double[] eigenvalue1 = new double[1];
        double[] eigenvector1 = powerIteration(covariance, c, eigenvalue1);

        // Deflate the covariance matrix: C' = C - λ1 * v1 * v1^T
        double[][] deflated = new double[c][c];
        for (int i = 0; i < c; i++) {
            for (int j = 0; j < c; j++) {
                deflated[i][j] = covariance[i][j] - eigenvalue1[0] * eigenvector1[i] * eigenvector1[j];
            }
        }

        double[] eigenvalue2 = new double[1];
        double[] eigenvector2 = powerIteration(deflated, c, eigenvalue2);

        // Compute total variance and percentage explained
        double totalVariance = 0;
        for (int i = 0; i < c; i++) totalVariance += covariance[i][i];
        double varianceExplained = (totalVariance > 0)
                ? ((eigenvalue1[0] + eigenvalue2[0]) / totalVariance) * 100.0
                : 0;

        // Step 5: Project alternatives onto the 2D plane
        List<Map<String, Object>> altProjections = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double x = 0, y = 0;
            for (int j = 0; j < c; j++) {
                x += centered[i][j] * eigenvector1[j];
                y += centered[i][j] * eigenvector2[j];
            }
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("name", alternatives.get(i).getName());
            point.put("x", round(x));
            point.put("y", round(y));
            altProjections.add(point);
        }

        // Step 6: Project criteria as vectors (eigenvector components scaled by eigenvalues)
        List<Map<String, Object>> critProjections = new ArrayList<>();
        double scale1 = Math.sqrt(eigenvalue1[0]);
        double scale2 = Math.sqrt(eigenvalue2[0]);
        for (int j = 0; j < c; j++) {
            Map<String, Object> vec = new LinkedHashMap<>();
            vec.put("name", criteria.get(j).getName());
            vec.put("x", round(eigenvector1[j] * scale1));
            vec.put("y", round(eigenvector2[j] * scale2));
            critProjections.add(vec);
        }

        // Step 7: Compute the PROMETHEE decision axis (pi vector)
        // pi = weighted sum of criterion vectors projected onto the plane
        double totalWeight = 0;
        for (Criterion crit : criteria) totalWeight += crit.getWeight();

        double piX = 0, piY = 0;
        for (int j = 0; j < c; j++) {
            double w = (totalWeight > 0) ? criteria.get(j).getWeight() / totalWeight : 1.0 / c;
            piX += w * eigenvector1[j] * scale1;
            piY += w * eigenvector2[j] * scale2;
        }

        Map<String, Object> decisionAxis = new LinkedHashMap<>();
        decisionAxis.put("x", round(piX));
        decisionAxis.put("y", round(piY));

        // Build final result
        Map<String, Object> gaia = new LinkedHashMap<>();
        gaia.put("alternatives", altProjections);
        gaia.put("criteria", critProjections);
        gaia.put("decisionAxis", decisionAxis);
        gaia.put("varianceExplained", round(varianceExplained));

        return gaia;
    }

    /**
     * Builds the unicriterion net flow matrix.
     * For each criterion j, computes φ_j(a) = 1/(n-1) * Σ_{x≠a} [P_j(a,x) - P_j(x,a)]
     *
     * @param alternatives the list of alternatives
     * @param criteria the list of criteria
     * @return a 2D array of size [N alternatives][C criteria]
     */
    private double[][] buildUnicriterionFlowMatrix(List<Alternative> alternatives, List<Criterion> criteria) {
        int n = alternatives.size();
        int c = criteria.size();
        double[][] flows = new double[n][c];

        for (int j = 0; j < c; j++) {
            Criterion crit = criteria.get(j);
            for (int i = 0; i < n; i++) {
                double phiJ = 0;
                for (int k = 0; k < n; k++) {
                    if (i == k) continue;
                    double valI = alternatives.get(i).getValue(crit);
                    double valK = alternatives.get(k).getValue(crit);
                    double diff = crit.isMaximize() ? (valI - valK) : (valK - valI);
                    double diffReverse = crit.isMaximize() ? (valK - valI) : (valI - valK);
                    phiJ += crit.getPreference(diff) - crit.getPreference(diffReverse);
                }
                flows[i][j] = phiJ / (n - 1);
            }
        }
        return flows;
    }

    /**
     * Power Iteration algorithm to find the dominant eigenvector of a symmetric matrix.
     *
     * @param matrix the symmetric matrix
     * @param size the dimension of the matrix
     * @param eigenvalueOut single-element array to store the computed eigenvalue
     * @return the dominant eigenvector (normalized)
     */
    private double[] powerIteration(double[][] matrix, int size, double[] eigenvalueOut) {
        // Initialize with a non-zero vector
        double[] v = new double[size];
        Random rng = new Random(42);
        for (int i = 0; i < size; i++) v[i] = rng.nextDouble() - 0.5;
        normalize(v);

        double eigenvalue = 0;

        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            // Multiply: w = matrix * v
            double[] w = new double[size];
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    w[i] += matrix[i][j] * v[j];
                }
            }

            // Compute eigenvalue (Rayleigh quotient approximation = norm of w)
            double newEigenvalue = norm(w);
            if (newEigenvalue < 1e-15) {
                // Matrix is essentially zero in this direction
                eigenvalueOut[0] = 0;
                return new double[size];
            }

            // Normalize
            for (int i = 0; i < size; i++) w[i] /= newEigenvalue;

            // Check convergence
            if (Math.abs(newEigenvalue - eigenvalue) < CONVERGENCE_THRESHOLD) {
                eigenvalueOut[0] = newEigenvalue;
                return w;
            }

            eigenvalue = newEigenvalue;
            v = w;
        }

        eigenvalueOut[0] = eigenvalue;
        return v;
    }

    private void normalize(double[] v) {
        double n = norm(v);
        if (n > 0) for (int i = 0; i < v.length; i++) v[i] /= n;
    }

    private double norm(double[] v) {
        double sum = 0;
        for (double x : v) sum += x * x;
        return Math.sqrt(sum);
    }

    private double round(double val) {
        return Math.round(val * 10000.0) / 10000.0;
    }
}
