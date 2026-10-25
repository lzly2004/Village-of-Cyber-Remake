package com.vocr.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 事件立绘目录的数据契约：JSON 是数据驱动立绘名称的唯一来源，所有名称必须准确指向图片资源。
 */
class EventImageMappingDataTest {
    private static final String MAPPING_RESOURCE = "/config/event_image_mapping.json";
    private static final Path IMAGE_DIRECTORY = Path.of("src/main/resources/images");

    @Test
    void everyConfiguredImageShouldExistWithExactFileName() throws Exception {
        JsonNode mapping = loadMapping();
        assertTrue(mapping.isObject(), "事件立绘目录的根节点必须是对象");
        assertFalse(mapping.isEmpty(), "事件立绘目录不能为空");

        Set<String> configuredImages = new HashSet<>();
        collectImageNames(mapping, "$", configuredImages);
        assertFalse(configuredImages.isEmpty(), "事件立绘目录必须至少引用一张图片");

        Set<String> actualFileNames = new HashSet<>();
        try (var files = Files.list(IMAGE_DIRECTORY)) {
            files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .forEach(actualFileNames::add);
        }

        Set<String> missingFiles = new TreeSet<>(configuredImages);
        missingFiles.removeAll(actualFileNames);
        assertTrue(missingFiles.isEmpty(),
                () -> "事件立绘不存在或文件名大小写不一致: " + missingFiles);

        Set<String> missingClasspathResources = new TreeSet<>();
        for (String imageName : configuredImages) {
            if (EventImageMappingDataTest.class.getResource("/images/" + imageName) == null) {
                missingClasspathResources.add(imageName);
            }
        }
        assertTrue(missingClasspathResources.isEmpty(),
                () -> "事件立绘未进入 classpath: " + missingClasspathResources);
    }

    private static JsonNode loadMapping() throws Exception {
        try (InputStream input = EventImageMappingDataTest.class.getResourceAsStream(MAPPING_RESOURCE)) {
            assertNotNull(input, "缺少事件立绘目录: " + MAPPING_RESOURCE);
            return new ObjectMapper().readTree(input);
        }
    }

    private static void collectImageNames(JsonNode node, String location, Set<String> result) {
        if (node.isTextual()) {
            String imageName = node.textValue();
            assertTrue(imageName.matches("[^/\\\\]+\\.png"),
                    () -> "事件立绘名称必须是 PNG 文件名，不能包含目录: " + location + " = " + imageName);
            result.add(imageName);
            return;
        }
        if (node.isArray()) {
            for (int index = 0; index < node.size(); index++) {
                collectImageNames(node.get(index), location + "[" + index + "]", result);
            }
            return;
        }
        assertTrue(node.isObject(), () -> "事件立绘目录包含不支持的数据类型: " + location);
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            collectImageNames(field.getValue(), location + "." + field.getKey(), result);
        }
    }
}
