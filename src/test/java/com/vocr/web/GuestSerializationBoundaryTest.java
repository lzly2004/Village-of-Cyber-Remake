package com.vocr.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vocr.application.*;
import com.vocr.core.SeededGuestSession;
import com.vocr.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GuestSerializationBoundaryTest
{
    private final ObjectMapper json = new ObjectMapper();
    private static final Set<Class<?>> RECORDS = Set.of(GameUpdate.class,
            PlayerViewSnapshot.class, VisiblePlayer.class, SelectionStateSnapshot.class,
            SkillTargetSelections.class, ClaimedActorTargets.class, LegalVoteAction.class,
            ClaimInquiryOptions.class, LegalTargetSelection.class, ClaimedRoleActor.class,
            VoteResultSnapshot.class, VoteRoundSnapshot.class, VoteLineSnapshot.class,
            RevealedPlayerRole.class, Event.class, PresentedEvent.class,
            GuestGameServer.CreatedGame.class, VoteExecutionResult.class);
    private static final Set<Class<?>> SCALARS = Set.of(int.class, long.class, boolean.class, Integer.class,
            String.class, peiyi.class, Role.class, GameResult.class, whyDie.class,
            CharacterEnglishName.class, SelectionType.class, VotingMethod.class,
            BusinessPhase.class, EventPresentationStage.class, EventName.class);

    @Test
    void declaredUpdateGraph_shouldContainOnlyReviewedRecordsAndScalarTypes()
    {
        Set<Class<?>> visited = new HashSet<>();
        checkType(GameUpdate.class, visited);
        checkType(GuestGameServer.CreatedGame.class, visited);
        checkType(VoteExecutionResult.class, visited);
        assertEquals(RECORDS, visited, "all allowlisted records must be reachable");
    }

    private static void checkType(Type type, Set<Class<?>> visited)
    {
        if (type instanceof ParameterizedType parameterized)
        {
            assertEquals(List.class, parameterized.getRawType(), "only typed lists are allowed");
            for (Type element : parameterized.getActualTypeArguments()) checkType(element, visited);
            return;
        }
        assertInstanceOf(Class.class, type);
        Class<?> concrete = (Class<?>) type;
        if (SCALARS.contains(concrete)) return;
        assertTrue(RECORDS.contains(concrete), "unreviewed wire type: " + type);
        assertTrue(concrete.isRecord());
        if (visited.add(concrete))
            for (var component : concrete.getRecordComponents()) checkType(component.getGenericType(), visited);
    }

    @Test
    void declaredGraph_shouldRejectCoreContextAndUnboundedTypes()
    {
        for (Class<?> type : List.of(GameContextView.class, com.vocr.core.GameRecorder.class,
                Object.class, java.util.Random.class, List.class))
            assertThrows(AssertionError.class, () -> checkType(type, new HashSet<>()), type.getName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"actualRole", "suspicion", "nonHumanPlan", "gameContext",
            "recorder", "random", "innocentLookingExtraField"})
    void allowlist_shouldRejectExtraFieldsAtRootAndInsidePlayers(String field) throws Exception
    {
        ObjectNode root = initialUpdate();
        GuestWireContract.assertUpdate(root);
        root.put(field, "secret");
        assertThrows(AssertionError.class, () -> GuestWireContract.assertUpdate(root));
        ObjectNode nested = initialUpdate();
        ((ObjectNode) nested.path("view").path("players").get(0)).put(field, "secret");
        assertThrows(AssertionError.class, () -> GuestWireContract.assertUpdate(nested));
    }

    @Test
    void allowlist_shouldRejectRevealBeforeSettlementAndContainersInsideScalarFields() throws Exception
    {
        ObjectNode running = initialUpdate();
        running.withArray("revealedRoles").addObject().put("playerNumber", 1).put("actualRole", "lang");
        assertThrows(AssertionError.class, () -> GuestWireContract.assertUpdate(running));
        ObjectNode hidden = initialUpdate();
        ((ObjectNode) hidden.path("view").path("players").get(0))
                .putObject("character").put("actualRole", "lang");
        assertThrows(AssertionError.class, () -> GuestWireContract.assertUpdate(hidden));
        ObjectNode fakeFinished = initialUpdate();
        fakeFinished.put("phase", "FINISHED");
        assertThrows(AssertionError.class, () -> GuestWireContract.assertUpdate(fakeFinished));
    }

    @Test
    void responseEnvelopes_shouldRejectAdditionalSecretFields() throws Exception
    {
        ObjectNode created = json.createObjectNode().put("sessionId", "boundary");
        created.set("update", initialUpdate());
        GuestWireContract.assertCreated(created);
        created.putObject("context").put("secret", 1);
        assertThrows(AssertionError.class, () -> GuestWireContract.assertCreated(created));
        ObjectNode vote = json.createObjectNode().put("voteCompleted", false);
        vote.set("update", initialUpdate());
        GuestWireContract.assertVoteResult(vote);
        vote.put("actualRole", "lang");
        assertThrows(AssertionError.class, () -> GuestWireContract.assertVoteResult(vote));
    }

    @Test
    void scheduledSkillsAndClaims_shouldSerializeOnlyReviewedState() throws Exception
    {
        GameSession session = SeededGuestSession.create("boundary-skills", 4202);
        session.startGame(new StartGameCommand(0, peiyi.tongchang));
        GameUpdate update = session.requestClaims(new AskClaimByRoleCommand(1, Role.zhan));
        GuestWireContract.assertUpdate(wire(update));
        for (SelectionType type : List.of(SelectionType.DIVINATION, SelectionType.GUARD))
        {
            int target = update.legalTargetSelection(type).orElseThrow().targets().getFirst();
            update = session.setTargetSelection(new SetAllTargetSelectionCommand(
                    update.revision(), type, target, true));
            GuestWireContract.assertUpdate(wire(update));
            assertFalse(update.selections().skillSelections(type).hiddenTargets().isEmpty());
        }
    }

    @Test
    void eventCharacters_shouldPreserveNullAndNoneAsDistinctWireValues() throws Exception
    {
        JsonNode event = wire(new Event(EventName.values()[0], null, CharacterEnglishName.NONE));
        assertTrue(event.path("ch1").isNull());
        assertEquals("NONE", event.path("ch2").asText());
        assertTrue(event.path("ch3").isNull());
    }

    private ObjectNode initialUpdate() throws Exception
    {
        GameSession session = SeededGuestSession.create("boundary", 4201);
        session.startGame(new StartGameCommand(0, peiyi.jianyi));
        return (ObjectNode) wire(session.currentUpdate(0));
    }

    private JsonNode wire(Object value) throws Exception
    {
        return json.readTree(json.writeValueAsString(value));
    }
}
