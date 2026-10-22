package com.vocr.web;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Exact nested wire allowlist. New fields require an explicit visibility review. */
final class GuestWireContract
{
    private static final String EVENT_FIELDS = "eventname ch1 ch2 ch3";
    private static final Map<String, Set<String>> OBJECTS = Map.ofEntries(
            shape("", "revision phase view selections legalVoteActions claimInquiry "
                    + "legalTargetSelections voteHistory revealedRoles eventCursor nextEventCursor "
                    + "events presentedEvents publicInformation"),
            shape("/publicInformation", "skillReports suspicion deathHistory"),
            shape("/publicInformation/skillReports/*", "playerNumber claimedRole claimedRoleOrder exposed results"),
            shape("/publicInformation/skillReports/*/results/*", "skillDay targetPlayerNumber color"),
            shape("/publicInformation/suspicion", "available rows"),
            shape("/publicInformation/suspicion/rows/*", "playerNumber targets comparisons"),
            shape("/publicInformation/deathHistory/*", "day executions nightDeaths"),
            shape("/publicInformation/deathHistory/*/executions/*", "playerNumber cause prefixPlayers"),
            shape("/view", "configuration gameDay aliveCount deathCount result players"),
            shape("/view/players/*", "playerNumber character alive deathReason deathDay "
                    + "claimedRole claimedRoleOrder comingOutDay"),
            shape("/selections", "avoidanceEnabled voteTargets divination guard"),
            shape("/selections/divination", "hiddenTargets claimedActors"),
            shape("/selections/guard", "hiddenTargets claimedActors"),
            shape("/selections/divination/claimedActors/*", "playerNumber claimedRoleOrder targets"),
            shape("/selections/guard/claimedActors/*", "playerNumber claimedRoleOrder targets"),
            shape("/legalVoteActions/*", "method candidates"),
            shape("/claimInquiry", "roles players"),
            shape("/legalTargetSelections/*", "type targets claimedActors"),
            shape("/legalTargetSelections/*/claimedActors/*", "playerNumber claimedRoleOrder"),
            shape("/voteHistory/*", "day method candidates rounds"),
            shape("/voteHistory/*/rounds/*", "round lines maximumVotes leaders"),
            shape("/voteHistory/*/rounds/*/lines/*", "playerNumber receivedVotes targetPlayerNumber"),
            shape("/revealedRoles/*", "playerNumber actualRole"),
            shape("/events/*", EVENT_FIELDS),
            shape("/presentedEvents/*", "event stage"),
            shape("/presentedEvents/*/event", EVENT_FIELDS));

    private static final Set<String> ARRAYS = Set.of(
            "/view/players", "/selections/voteTargets",
            "/publicInformation/skillReports", "/publicInformation/skillReports/*/results",
            "/publicInformation/suspicion/rows", "/publicInformation/suspicion/rows/*/targets",
            "/publicInformation/suspicion/rows/*/comparisons", "/publicInformation/deathHistory",
            "/publicInformation/deathHistory/*/executions", "/publicInformation/deathHistory/*/nightDeaths",
            "/publicInformation/deathHistory/*/executions/*/prefixPlayers",
            "/selections/divination/hiddenTargets", "/selections/divination/claimedActors",
            "/selections/divination/claimedActors/*/targets",
            "/selections/guard/hiddenTargets", "/selections/guard/claimedActors",
            "/selections/guard/claimedActors/*/targets",
            "/legalVoteActions", "/legalVoteActions/*/candidates",
            "/claimInquiry/roles", "/claimInquiry/players",
            "/legalTargetSelections", "/legalTargetSelections/*/targets",
            "/legalTargetSelections/*/claimedActors", "/voteHistory",
            "/voteHistory/*/candidates", "/voteHistory/*/rounds",
            "/voteHistory/*/rounds/*/lines", "/voteHistory/*/rounds/*/leaders",
            "/revealedRoles", "/events", "/presentedEvents");

    private GuestWireContract() { }

    static void assertCreated(JsonNode created)
    {
        assertEnvelope(created, Set.of("sessionId", "update"));
        assertTrue(created.path("sessionId").isTextual());
        assertUpdate(created.get("update"));
    }

    static void assertVoteResult(JsonNode result)
    {
        assertEnvelope(result, Set.of("voteCompleted", "update"));
        assertTrue(result.path("voteCompleted").isBoolean());
        assertUpdate(result.get("update"));
    }

    private static void assertEnvelope(JsonNode node, Set<String> expected)
    {
        assertTrue(node.isObject());
        Set<String> actual = new HashSet<>();
        node.fieldNames().forEachRemaining(actual::add);
        assertEquals(expected, actual, "response envelope fields");
    }

    static void assertUpdate(JsonNode update)
    {
        check(update, "");
        String phase = update.path("phase").asText();
        assertTrue(Set.of("DAY_DECISION", "FINISHED").contains(phase), "unknown phase");
        assertEquals(phase.equals("FINISHED"), !update.path("view").path("result").asText().equals("NONE"));
        JsonNode revealed = update.path("revealedRoles");
        if (phase.equals("DAY_DECISION"))
        {
            assertTrue(revealed.isEmpty(), "running game must not reveal actual roles");
            assertNoPreciseNightCause(update);
            for (JsonNode player : update.path("view").path("players"))
            {
                assertTrue(Set.of("NONE", "EXECUTION", "DAY_CAT_CURSE", "DAY_PURSUED", "NIGHT_DEATH")
                        .contains(player.path("deathReason").asText()));
                assertEquals(player.path("alive").asBoolean(), player.path("deathReason").asText().equals("NONE"));
            }
        }
        else
        {
            JsonNode players = update.path("view").path("players");
            assertEquals(players.size(), revealed.size(), "complete settlement reveal");
            for (int i = 0; i < players.size(); i++)
            {
                assertEquals(players.get(i).get("playerNumber"), revealed.get(i).get("playerNumber"));
                assertTrue(revealed.get(i).path("actualRole").isTextual());
                assertNotEquals("NONE", revealed.get(i).path("actualRole").asText());
                assertTrue(Set.of("NONE", "chuxing", "daymaozhou", "dayhouzhui", "beiyao", "nightmaozhou", "nighthouzhui", "zhousha")
                        .contains(players.get(i).path("deathReason").asText()));
            }
        }
        long start = update.path("eventCursor").asLong();
        JsonNode information = update.path("publicInformation");
        if (phase.equals("FINISHED")) assertTrue(information.path("skillReports").isEmpty());
        JsonNode suspicion = information.path("suspicion");
        if (phase.equals("FINISHED") || update.path("view").path("gameDay").asInt() <= 2)
        {
            assertFalse(suspicion.path("available").asBoolean());
            assertTrue(suspicion.path("rows").isEmpty());
        }
        for (JsonNode report : information.path("skillReports"))
        {
            int actor = report.path("playerNumber").asInt();
            assertTrue(actor >= 1 && actor <= update.path("view").path("players").size());
            JsonNode player = update.path("view").path("players").get(actor - 1);
            assertTrue(Set.of("zhan", "ling", "lie").contains(report.path("claimedRole").asText()));
            assertEquals(player.path("claimedRole"), report.path("claimedRole"));
            assertEquals(player.path("claimedRoleOrder"), report.path("claimedRoleOrder"));
            for (JsonNode result : report.path("results"))
            {
                int day = result.path("skillDay").asInt();
                assertTrue(day >= (report.path("claimedRole").asText().equals("zhan") ? 1 : 2));
                assertTrue(day < update.path("view").path("gameDay").asInt());
                assertTrue(player.path("deathDay").asInt() == 0 || day < player.path("deathDay").asInt());
                assertTrue(result.path("targetPlayerNumber").asInt() >= 1
                        && result.path("targetPlayerNumber").asInt() <= update.path("view").path("players").size());
                assertTrue(Set.of("WHITE", "BLACK", "NOT_APPLICABLE").contains(result.path("color").asText()));
                assertEquals(report.path("claimedRole").asText().equals("lie"), result.path("color").asText().equals("NOT_APPLICABLE"));
            }
        }
        for (JsonNode row : suspicion.path("rows"))
        {
            assertTrue(row.path("playerNumber").asInt() >= 1 && row.path("playerNumber").asInt() <= update.path("view").path("players").size());
            assertTrue(update.path("view").path("players").get(row.path("playerNumber").asInt() - 1).path("alive").asBoolean());
            assertTrue(row.path("targets").size() <= 3);
            assertEquals(Math.max(0, row.path("targets").size() - 1), row.path("comparisons").size());
            row.path("targets").forEach(value -> assertTrue(value.asInt() >= 1
                    && value.asInt() <= update.path("view").path("players").size()));
            row.path("comparisons").forEach(value -> assertTrue(Set.of("CLOSE", "SLIGHTLY_GREATER", "GREATER", "MUCH_GREATER").contains(value.asText())));
        }
        int previousDay = 0;
        int count = update.path("view").path("players").size();
        for (JsonNode day : information.path("deathHistory"))
        {
            assertTrue(day.path("day").asInt() > previousDay
                    && day.path("day").asInt() < update.path("view").path("gameDay").asInt());
            previousDay = day.path("day").asInt();
            day.path("nightDeaths").forEach(value -> assertTrue(value.asInt() >= 1 && value.asInt() <= count));
            for (JsonNode death : day.path("executions"))
            {
                assertTrue(death.path("playerNumber").asInt() >= 1 && death.path("playerNumber").asInt() <= count);
                assertTrue(Set.of("EXECUTION", "DAY_CAT_CURSE", "DAY_PURSUED").contains(death.path("cause").asText()));
                assertTrue(death.path("prefixPlayers").size() <= 1);
                death.path("prefixPlayers").forEach(value -> assertTrue(value.asInt() >= 1 && value.asInt() <= count));
            }
        }
        long end = update.path("nextEventCursor").asLong();
        assertTrue(start >= 0 && end >= start);
        assertEquals(end - start, (long) update.path("events").size());
        assertEquals(update.path("events").size(), update.path("presentedEvents").size());
        for (int i = 0; i < update.path("events").size(); i++)
            assertEquals(update.path("events").get(i),
                    update.path("presentedEvents").get(i).get("event"));
    }

    private static void assertNoPreciseNightCause(JsonNode node)
    {
        // Scan the entire response, including object keys and any nested scalar, not only player fields.
        if (node.isObject()) node.fields().forEachRemaining(field -> {
            assertFalse(Set.of("beiyao", "nightmaozhou", "nighthouzhui", "zhousha").contains(field.getKey()));
            assertNoPreciseNightCause(field.getValue());
        });
        else if (node.isArray()) node.forEach(GuestWireContract::assertNoPreciseNightCause);
        else if (node.isTextual())
            assertFalse(node.asText().matches("(?s).*(beiyao|nightmaozhou|nighthouzhui|zhousha).*"), "running response hides exact night cause");
    }

    private static void check(JsonNode node, String path)
    {
        if (OBJECTS.containsKey(path))
        {
            assertTrue(node.isObject(), path + " must be object");
            Set<String> actual = new HashSet<>();
            node.fieldNames().forEachRemaining(actual::add);
            assertEquals(OBJECTS.get(path), actual, path + " fields");
            node.fields().forEachRemaining(field -> check(field.getValue(), path + "/" + field.getKey()));
        }
        else if (ARRAYS.contains(path))
        {
            assertTrue(node.isArray(), path + " must be array");
            node.forEach(child -> check(child, path + "/*"));
        }
        else
        {
            assertTrue(node.isValueNode(), path + " must not hide a nested object/array");
            if (node.isNull())
                assertTrue(path.matches("/(events/\\*|presentedEvents/\\*/event)/ch[123]"),
                        path + " unexpected null");
        }
    }

    private static Map.Entry<String, Set<String>> shape(String path, String fields)
    {
        return Map.entry(path, Set.of(fields.split(" ")));
    }
}
