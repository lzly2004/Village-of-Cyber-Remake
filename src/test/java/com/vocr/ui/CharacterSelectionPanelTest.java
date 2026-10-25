package com.vocr.ui;

import com.vocr.application.ClaimedActorTargets;
import com.vocr.application.SelectionStateSnapshot;
import com.vocr.application.SkillTargetSelections;
import com.vocr.domain.SelectionType;

import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterSelectionPanelTest {

    @Test
    void voteState_shouldDriveEveryVoteIndicator() {
        SelectionStateSnapshot selections = new SelectionStateSnapshot(
                true,
                List.of(2),
                emptySkillSelections(),
                emptySkillSelections());
        List<JLabel> frames = hiddenLabels(3);
        List<JLabel> results = hiddenLabels(3);
        List<JLabel> votes = hiddenLabels(3);

        CharacterSelectionPanel.applyTargetState(
                selections, SelectionType.VOTE, 1, List.of(),
                frames, results, votes, List.of());

        assertOnlyVisible(frames, 1);
        assertOnlyVisible(results, 1);
        assertOnlyVisible(votes, 1);
    }

    @Test
    void hiddenSkillState_shouldShowAggregateResultWithoutClaimedActorResult() {
        SelectionStateSnapshot selections = skillSelections();
        List<JLabel> frames = hiddenLabels(3);
        List<JLabel> results = hiddenLabels(3);
        List<JLabel> actorResults = hiddenLabels(6);

        CharacterSelectionPanel.applyTargetState(
                selections, SelectionType.DIVINATION, 0, List.of(2, 3),
                frames, results, List.of(), actorResults);

        assertOnlyVisible(frames, 0);
        assertOnlyVisible(results, 0);
        assertNoneVisible(actorResults);
    }

    @Test
    void claimedActorSkillState_shouldShowOnlyMatchingActorResult() {
        SelectionStateSnapshot selections = skillSelections();
        List<JLabel> frames = hiddenLabels(3);
        List<JLabel> results = hiddenLabels(3);
        List<JLabel> actorResults = hiddenLabels(6);

        CharacterSelectionPanel.applyTargetState(
                selections, SelectionType.DIVINATION, 1, List.of(2, 3),
                frames, results, List.of(), actorResults);

        assertOnlyVisible(frames, 1);
        assertNoneVisible(results);
        assertOnlyVisible(actorResults, 2);
    }

    @Test
    void unselectedSkillTarget_shouldHideAllIndicators() {
        SelectionStateSnapshot selections = skillSelections();
        List<JLabel> frames = hiddenLabels(3);
        List<JLabel> results = hiddenLabels(3);
        List<JLabel> actorResults = hiddenLabels(6);

        CharacterSelectionPanel.applyTargetState(
                selections, SelectionType.GUARD, 2, List.of(2, 4),
                frames, results, List.of(), actorResults);

        assertNoneVisible(frames);
        assertNoneVisible(results);
        assertNoneVisible(actorResults);
    }

    private static SelectionStateSnapshot skillSelections() {
        return new SelectionStateSnapshot(
                true,
                List.of(),
                new SkillTargetSelections(
                        List.of(1),
                        List.of(
                                new ClaimedActorTargets(2, 1, List.of(2)),
                                new ClaimedActorTargets(3, 2, List.of(3)))),
                emptySkillSelections());
    }

    private static SkillTargetSelections emptySkillSelections() {
        return new SkillTargetSelections(List.of(), List.of());
    }

    private static List<JLabel> hiddenLabels(int count) {
        List<JLabel> labels = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            JLabel label = new JLabel();
            label.setVisible(false);
            labels.add(label);
        }
        return labels;
    }

    private static void assertOnlyVisible(List<JLabel> labels, int visibleIndex) {
        for (int i = 0; i < labels.size(); i++) {
            if (i == visibleIndex) {
                assertTrue(labels.get(i).isVisible(), "标签应显示: " + i);
            } else {
                assertFalse(labels.get(i).isVisible(), "标签不应显示: " + i);
            }
        }
    }

    private static void assertNoneVisible(List<JLabel> labels) {
        for (int i = 0; i < labels.size(); i++) {
            assertFalse(labels.get(i).isVisible(), "标签不应显示: " + i);
        }
    }
}
