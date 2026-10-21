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
                    + "events presentedEvents"),
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
        JsonNode revealed = update.path("revealedRoles");
        if (phase.equals("DAY_DECISION"))
            assertTrue(revealed.isEmpty(), "running game must not reveal actual roles");
        else
        {
            JsonNode players = update.path("view").path("players");
            assertEquals(players.size(), revealed.size(), "complete settlement reveal");
            for (int i = 0; i < players.size(); i++)
            {
                assertEquals(players.get(i).get("playerNumber"), revealed.get(i).get("playerNumber"));
                assertTrue(revealed.get(i).path("actualRole").isTextual());
                assertNotEquals("NONE", revealed.get(i).path("actualRole").asText());
            }
        }
        long start = update.path("eventCursor").asLong();
        long end = update.path("nextEventCursor").asLong();
        assertTrue(start >= 0 && end >= start);
        assertEquals(end - start, (long) update.path("events").size());
        assertEquals(update.path("events").size(), update.path("presentedEvents").size());
        for (int i = 0; i < update.path("events").size(); i++)
            assertEquals(update.path("events").get(i),
                    update.path("presentedEvents").get(i).get("event"));
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
