package com.vocr.ui;

import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterSelectionPanelTest {

    @Test
    void singleClaimedActor_shouldUpdateSelectedPlayersLabel() {
        List<JLabel> labels = hiddenLabels(3);

        CharacterSelectionPanel.setClaimedActorResultVisible(
                labels, 1, 0, 1, true);

        assertFalse(labels.get(0).isVisible());
        assertTrue(labels.get(1).isVisible());
        assertFalse(labels.get(2).isVisible());
    }

    @Test
    void multipleClaimedActors_shouldUpdateOnlySelectedActorForPlayer() {
        List<JLabel> labels = hiddenLabels(6);

        CharacterSelectionPanel.setClaimedActorResultVisible(
                labels, 1, 1, 2, true);

        assertOnlyVisible(labels, 3);
    }

    @Test
    void claimedActorSelection_shouldSupportCancellingOneIndicator() {
        List<JLabel> labels = hiddenLabels(6);
        CharacterSelectionPanel.setClaimedActorResultVisible(
                labels, 1, 0, 2, true);
        CharacterSelectionPanel.setClaimedActorResultVisible(
                labels, 1, 1, 2, true);

        CharacterSelectionPanel.setClaimedActorResultVisible(
                labels, 1, 0, 2, false);

        assertOnlyVisible(labels, 3);
    }

    @Test
    void deletingTarget_shouldHideEveryClaimedActorIndicatorForPlayer() {
        List<JLabel> labels = hiddenLabels(6);
        CharacterSelectionPanel.setAllClaimedActorResultsVisible(
                labels, 0, 2, true);
        CharacterSelectionPanel.setAllClaimedActorResultsVisible(
                labels, 1, 2, true);

        CharacterSelectionPanel.setAllClaimedActorResultsVisible(
                labels, 1, 2, false);

        assertTrue(labels.get(0).isVisible());
        assertTrue(labels.get(1).isVisible());
        assertFalse(labels.get(2).isVisible());
        assertFalse(labels.get(3).isVisible());
        assertFalse(labels.get(4).isVisible());
        assertFalse(labels.get(5).isVisible());
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
}
