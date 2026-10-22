package com.vocr.ui;

import com.vocr.application.PublicInformationFixture;
import com.vocr.application.PublicInformation;
import com.vocr.history.GameRecordManager;
import com.vocr.replay.ReplayManager;
import com.vocr.resources.ResourcesInterface;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class PublicInformationDesktopParityTest
{
    @TempDir Path directory;

    @Test
    void structuredReportsAndSuspicion_shouldMatchActualDesktopText() throws Exception
    {
        var fixture = new PublicInformationFixture();
        var constructor = ReplayManager.class.getDeclaredConstructor(String.class);
        constructor.setAccessible(true);
        var replay = constructor.newInstance(directory.resolve("isolated-replay.json").toString());
        var icon = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB));
        var resources = (ResourcesInterface) Proxy.newProxyInstance(ResourcesInterface.class.getClassLoader(),
                new Class<?>[]{ResourcesInterface.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getImage")) return icon;
                    throw new UnsupportedOperationException(method.getName());
                });
        var ui = new UI(resources, replay, GameRecordManager.inMemory());
        ui.ctx = fixture.context(); ui.uiComponentFactory = new UIComponentFactory(); ui.jPanel = new JPanel(null);
        var information = fixture.update().publicInformation();
        SwingUtilities.invokeAndWait(() -> {
            var renderer = new VoteInfoRenderer(); renderer.render(ui);
            String text = renderer.infoText.getText();
            for (var report : information.skillReports())
            {
                StringBuilder expected = new StringBuilder(ui.getJobText(report.playerNumber()) + " : ");
                for (var result : report.results())
                {
                    expected.append(ui.getJobText(result.targetPlayerNumber()));
                    if (result.color() == PublicInformation.ReportColor.BLACK) expected.append('●');
                    if (result.color() == PublicInformation.ReportColor.WHITE) expected.append('○');
                    expected.append('→');
                }
                if (report.exposed()) expected.append(GameStrings.MARKER_EXPOSED).append('→');
                expected.setLength(expected.length() - 1);
                assertTrue(text.contains(expected + "\n"), expected.toString());
            }
            assertFalse(text.contains(ui.getJobText(4) + " :"));
            try {
                var handler = new GameSceneVoteHandler();
                var field = GameSceneVoteHandler.class.getDeclaredField("ui"); field.setAccessible(true); field.set(handler, ui);
                var method = GameSceneVoteHandler.class.getDeclaredMethod("createDoubt"); method.setAccessible(true); method.invoke(handler);
                String doubt = ui.piaoText.getText() + ui.piaoText1.getText();
                var row = information.suspicion().rows().stream().filter(value -> value.playerNumber() == 4).findFirst().orElseThrow();
                assertEquals(java.util.List.of(PublicInformation.Comparison.SLIGHTLY_GREATER), row.comparisons());
                assertTrue(doubt.contains(ui.getJobText(4) + "：" + ui.getJobText(row.targets().get(0)) + "≧" + ui.getJobText(row.targets().get(1))));
                assertFalse(doubt.contains("100"));
            } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
        });
    }
}
