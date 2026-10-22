package com.vocr.ui;

import com.vocr.domain.GameContextView;
import com.vocr.domain.GameResult;
import com.vocr.domain.peiyi;
import com.vocr.domain.whyDie;
import com.vocr.history.GameRecordManager;
import com.vocr.logging.DebugLogger;
import com.vocr.replay.ReplayManager;
import com.vocr.resources.ImageNames;
import com.vocr.resources.ResourcesInterface;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.nio.file.Files;

/** Standalone audit probe: real Swing text renderers, synthetic context, no game/HTTP changes. */
public final class Web013AuditProbe
{
    public static void main(String[] args) throws Exception
    {
        DebugLogger.getInstance().setEnabled(false);
        var directory = Files.createTempDirectory("vocr-web013-data-");
        var constructor = ReplayManager.class.getDeclaredConstructor(String.class);
        constructor.setAccessible(true);
        var replay = constructor.newInstance(directory.resolve("replay.json").toString());
        var image = new ImageIcon(new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB));
        var resources = (ResourcesInterface) Proxy.newProxyInstance(ResourcesInterface.class.getClassLoader(),
                new Class<?>[]{ResourcesInterface.class}, (proxy, method, parameters) -> {
                    if (method.getName().equals("getImage")) return image;
                    throw new UnsupportedOperationException(method.getName());
                });
        UI ui = new UI(resources, replay, GameRecordManager.inMemory());
        ui.ctx = context();
        ui.uiComponentFactory = new UIComponentFactory();
        ui.jPanel = new JPanel(null);
        SwingUtilities.invokeAndWait(() -> {
            VoteInfoRenderer renderer = new VoteInfoRenderer();
            renderer.render(ui);
            String text = renderer.infoText.getText();
            require(text.contains("青年 : 研究○→傭兵●→教師○→破绽"), "seer retroactive reports and public mark");
            require(text.contains("研究 : 傭兵●→教師○"), "medium starts at skill day 2");
            require(!text.contains("研究 : 青年●"), "medium skill day 1 excluded");
            require(text.contains("傭兵 : 研究→情報"), "hunter targets without balls, from day 2");
            require(!text.contains("教師 :"), "unclaimed skill holder excluded");
            require(text.contains("情報 : 少年○→青年●→破绽"), "dead public actor retains past reports");
            require(!text.contains("情報 : 少年○→青年●→研究"), "actor death day result excluded");
            System.out.println("=== ACTUAL VoteInfoRenderer TEXT / SYNTHETIC CONTEXT ===\n" + text);
            try {
                var handler = new GameSceneVoteHandler();
                var field = GameSceneVoteHandler.class.getDeclaredField("ui");
                field.setAccessible(true);
                field.set(handler, ui);
                var renderDoubt = GameSceneVoteHandler.class.getDeclaredMethod("createDoubt");
                renderDoubt.setAccessible(true);
                renderDoubt.invoke(handler);
                String doubt = ui.piaoText.getText() + ui.piaoText1.getText();
                for (String symbol : new String[]{"=", "≧", ">", "≫"})
                    require(doubt.contains(symbol), "comparison threshold " + symbol);
                require(!doubt.contains("100"), "raw suspicion score excluded from text");
                require(!doubt.contains("情報："), "dead observer excluded");
                System.out.println("=== ACTUAL createDoubt TEXT / SYNTHETIC CONTEXT ===\n" + doubt);
            } catch (ReflectiveOperationException error) {
                throw new AssertionError(error);
            }
        });
        for (var reason : new whyDie[]{whyDie.beiyao, whyDie.nightmaozhou, whyDie.nighthouzhui, whyDie.zhousha})
        {
            require(ImageNames.deathMarker(reason).equals("kami.png"), "night causes share one public marker");
            System.out.println("NIGHT_MARKER " + reason + " -> " + ImageNames.deathMarker(reason));
        }
        var projector = Class.forName("com.vocr.application.PlayerViewProjector").getDeclaredMethod("project", GameContextView.class);
        projector.setAccessible(true);
        var view = (com.vocr.application.PlayerViewSnapshot) projector.invoke(null, ui.ctx);
        require(view.result() == GameResult.NONE, "running view, not settlement");
        require(view.players().get(5).deathReason() == whyDie.zhousha, "existing running projector discloses exact night cause");
        System.out.println("CURRENT_RUNNING_PROJECTOR player=6 deathReason=" + view.players().get(5).deathReason()
                + " / desktopMarker=" + ImageNames.deathMarker(view.players().get(5).deathReason()));
        System.out.println("PASS: actual renderer assertions; no interactive window, real images, or live HTTP exercised");
    }

    private static void require(boolean condition, String message)
    {
        if (!condition) throw new AssertionError(message);
    }

    private static GameContextView context()
    {
        int[][] skills = new int[8][6];
        skills[1] = new int[]{0, 2, 10, 4, 5, 0};
        skills[2] = new int[]{0, 8, 10, 4, 0, 0};
        skills[3] = new int[]{0, 6, 2, 5, 0, 0};
        skills[4] = new int[]{0, 9, 6, 0, 0, 0};
        skills[5] = new int[]{0, 6, 8, 2, 0, 0};
        int[] roles = {0, 1, 2, 3, 0, 1, 6, 4};
        return (GameContextView) Proxy.newProxyInstance(GameContextView.class.getClassLoader(),
                new Class<?>[]{GameContextView.class}, (proxy, method, parameters) -> {
                    int player = parameters == null || parameters.length == 0 ? 0 : (Integer) parameters[0];
                    return switch (method.getName()) {
                        case "getPlayerSum" -> 7;
                        case "getGameDay" -> 4;
                        case "getAliveCounter" -> 5;
                        case "getDeathCounter" -> 2;
                        case "getPeiyi" -> peiyi.tongchang;
                        case "getEndResult" -> GameResult.NONE;
                        case "getCharacterNumber" -> player;
                        case "getClaimedRole" -> roles[player];
                        case "getClaimedRoleOrder" -> roles[player] > 0 && roles[player] < 6 ? 1 : 0;
                        case "getActualRole" -> player == 4 ? 1 : roles[player];
                        case "getComingOutDay" -> player == 1 ? 3 : 1;
                        case "getDeathDay" -> player == 5 ? 3 : player == 6 ? 2 : 0;
                        case "getDeathReason" -> player == 5 ? whyDie.chuxing : player == 6 ? whyDie.zhousha : whyDie.NONE;
                        case "isAlive" -> player != 5 && player != 6;
                        case "getSkillTarget" -> skills[player][(Integer) parameters[1]];
                        case "isNonHumanMarked" -> player == 1 || player == 5;
                        case "getTop3SuspectedPlayer" -> (Integer) parameters[1] + 1;
                        case "getSuspicionValue" -> {
                            int gap = new int[]{0, 2, 5, 10, 11, 2, 2, 2}[player];
                            yield (Integer) parameters[1] == 2 ? 100 : 100 - gap;
                        }
                        default -> throw new UnsupportedOperationException(method.getName());
                    };
                });
    }
}
