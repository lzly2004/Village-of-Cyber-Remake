import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProbabilityCalculatorTest {
    private static final int EXPECTED_RULE_COUNT = 5 * 7 * 4 * 4;

    @TempDir
    Path tempDir;

    @Test
    void canonicalConfig_shouldContainEveryLegalCombinationExactlyOnce() throws Exception {
        Path config = Path.of(PathConfig.PROBABILITY_CONFIG);
        JsonNode entries = new ObjectMapper().readTree(config.toFile()).path("probabilities");
        Set<String> keys = new HashSet<>();

        assertTrue(entries.isArray(), "probabilities 必须是数组");
        assertEquals(EXPECTED_RULE_COUNT, entries.size());
        for (JsonNode entry : entries) {
            int zhi = entry.path("zhi").asInt(-1);
            int situation = entry.path("situation").asInt(-1);
            int p1 = entry.path("p1").asInt(-1);
            int p2 = entry.path("p2").asInt(-1);
            int probability = entry.path("probability").asInt(-1);
            assertTrue(zhi >= 0 && zhi <= 4);
            assertTrue(situation >= 0 && situation <= 6);
            assertTrue(p1 >= 0 && p1 <= 3);
            assertTrue(p2 >= 0 && p2 <= 3);
            assertTrue(probability >= 0 && probability <= 100);
            assertTrue(keys.add(zhi + " " + situation + " " + p1 + " " + p2),
                    "配置中存在重复组合");
        }

        ProbabilityCalculator calculator = new ProbabilityCalculator(config.toString());
        assertEquals(EXPECTED_RULE_COUNT, calculator.getRuleCount());
    }

    @Test
    void missingConfig_shouldCreateUsableCompleteDefaultTable() {
        Path config = tempDir.resolve("nested/probability.json");
        ProbabilityCalculator calculator = new ProbabilityCalculator(config.toString());

        assertTrue(Files.isRegularFile(config));
        assertEquals(EXPECTED_RULE_COUNT, calculator.getRuleCount());
        assertEquals(GameConstants.DEFAULT_PROBABILITY, calculator.maolieco(4, 6, 3, 3));
    }

    @Test
    void malformedConfig_shouldFallBackToUsableCompleteDefaultTable() throws Exception {
        Path config = tempDir.resolve("malformed.json");
        Files.writeString(config, "{not-json");

        ProbabilityCalculator calculator = new ProbabilityCalculator(config.toString());

        assertEquals(EXPECTED_RULE_COUNT, calculator.getRuleCount());
        assertEquals(GameConstants.DEFAULT_PROBABILITY, calculator.maolieco(0, 0, 0, 0));
    }
}
