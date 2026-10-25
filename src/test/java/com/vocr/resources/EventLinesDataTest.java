package com.vocr.resources;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 台词资源的数据契约：占位符必须以合法形态出现，否则运行时会原样显示给玩家。
 */
class EventLinesDataTest {
    private static final Set<String> SUPPORTED_PARAMS =
            Set.of("talkerName", "targetName", "partnerName");
    private static final Pattern PLACEHOLDER = Pattern.compile("<param=(\\w+)>");

    private static String loadRawLinesJson() {
        try (InputStream is = EventLinesDataTest.class.getResourceAsStream("/lines/event_lines.json")) {
            assertNotNull(is, "缺少台词资源文件: /lines/event_lines.json");
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("读取台词资源失败", e);
        }
    }

    @Test
    void everyPlaceholderMustUseWellFormedParamTag() {
        String raw = loadRawLinesJson();
        int index = 0;
        while (true) {
            int found = raw.indexOf("param=", index);
            if (found < 0) break;
            assertTrue(found > 0 && raw.charAt(found - 1) == '<', () -> describe(raw, found));
            Matcher matcher = PLACEHOLDER.matcher(raw).region(found - 1, Math.min(raw.length(), found + 64));
            assertTrue(matcher.lookingAt(), () -> describe(raw, found));
            String name = matcher.group(1);
            assertTrue(SUPPORTED_PARAMS.contains(name),
                    () -> "不支持的占位符参数名 <param=" + name + ">，当前仅支持 " + SUPPORTED_PARAMS);
            index = found + "param=".length();
        }
    }

    @Test
    void wellFormedPlaceholderCountShouldMatchRawOccurrences() {
        String raw = loadRawLinesJson();
        int rawOccurrences = 0;
        int index = 0;
        while ((index = raw.indexOf("param=", index)) >= 0) {
            rawOccurrences++;
            index += "param=".length();
        }
        Matcher matcher = PLACEHOLDER.matcher(raw);
        int wellFormed = 0;
        while (matcher.find()) wellFormed++;
        assertEquals(rawOccurrences, wellFormed, "存在未按 <param=xxx> 形态书写的占位符");
    }

    private static String describe(String raw, int found) {
        String context = raw.substring(Math.max(0, found - 20),
                Math.min(raw.length(), found + 40)).replace("\n", "\\n");
        return "占位符必须写成 <param=talkerName|targetName|partnerName>，实际: " + context;
    }
}
