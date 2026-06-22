package service;

import model.Alternative;
import model.Criterion;
import model.function.GaussianFunction;
import model.function.UsualFunction;
import model.function.VShapeIndifferences;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Benchmark class to measure the algorithmic performance of the PrometheeEngine.
 * Can be run via: mvn test -Dtest=PrometheeBenchmark
 */
public class PrometheeBenchmark {

    @Test
    public void runAlgorithmicBenchmark() {
        System.out.println("==================================================");
        System.out.println("       PROMETHEE ENGINE BENCHMARK RESULTS       ");
        System.out.println("==================================================");
        System.out.printf("%-15s | %-15s | %-15s%n", "Alternatives", "Criteria", "Time (ms)");
        System.out.println("--------------------------------------------------");

        int[] alternativeSizes = {10, 50, 100, 500, 1000, 2000, 5000};
        int criteriaCount = 10;
        
        PrometheeEngine engine = new PrometheeEngine();
        Random random = new Random(42); // fixed seed for reproducibility

        for (int altCount : alternativeSizes) {
            List<Alternative> alternatives = new ArrayList<>();
            List<Criterion> criteria = new ArrayList<>();

            // Generate Criteria
            for (int c = 0; c < criteriaCount; c++) {
                criteria.add(new Criterion("C" + c, "Criterion " + c, 1.0, true, new VShapeIndifferences(15,30)));
            }
            
            // Generate Alternatives with random values
            for (int a = 0; a < altCount; a++) {
                Alternative alt = new Alternative("A" + a, "Alt " + a);
                for (Criterion crit : criteria) {
                    alt.addValue(crit, random.nextDouble() * 100);
                }
                alternatives.add(alt);
            }

            // Warmup (to let JVM JIT kick in)
            if (altCount == 10) {
                for (int i = 0; i < 1000; i++) {
                    engine.calculate(alternatives, criteria);
                }
            }

            // Actual Benchmark
            long startTime = System.nanoTime();
            engine.calculate(alternatives, criteria);
            long endTime = System.nanoTime();

            double durationMs = (endTime - startTime) / 1_000_000.0;
            System.out.printf("%-15d | %-15d | %-15.2f%n", altCount, criteriaCount, durationMs);
        }
        System.out.println("==================================================");
    }
}
