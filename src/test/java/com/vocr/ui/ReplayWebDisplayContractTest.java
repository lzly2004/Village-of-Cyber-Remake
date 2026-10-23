package com.vocr.ui;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocr.replay.DaySnapshot;
import com.vocr.replay.ReplaySave;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import java.util.ArrayList;
import java.util.Collection;
import static org.junit.jupiter.api.Assertions.*;

/** Frozen synthetic cases shared with the Web tests. No Swing, user data or production changes. */
class ReplayWebDisplayContractTest {
    @TestFactory
    Collection<DynamicTest> frozenDisplayCases() throws Exception {
        var mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JsonNode cases;
        try (var input = getClass().getResourceAsStream("/replay/web034-display-cases.json")) {
            assertNotNull(input);
            cases = mapper.readTree(input);
        }
        var tests = new ArrayList<DynamicTest>();
        for (JsonNode c : cases) tests.add(DynamicTest.dynamicTest(c.get("name").asText(), () -> {
            ReplaySave replay = mapper.treeToValue(c.get("replay"), ReplaySave.class);
            int day = c.get("day").asInt(), count = replay.daySnapshots.getFirst().players.length;
            assertEquals(c.get("normalText").asText(), ReplayTextFormatter.buildNormalInfo(replay, day, count));
            assertEquals(c.get("roleText").asText(), ReplayTextFormatter.buildRoleInfo(replay.daySnapshots.getLast(), count));
            if (c.has("vote")) {
                JsonNode expected = c.get("vote");
                DaySnapshot snapshot = replay.daySnapshots.stream().filter(s -> s.dayNumber == day).findFirst().orElseThrow();
                var vote = ReplayTextFormatter.buildVoteInfo(snapshot, day, expected.get("request").asInt(), count);
                assertEquals(expected.get("index").asInt(), vote.roundIndex());
                assertEquals(expected.get("left").asText(), vote.leftColumn());
                assertEquals(expected.get("right").asText(), vote.rightColumn());
            }
        }));
        return tests;
    }
}
