package com.vocr.history;

import com.vocr.config.PathConfig;
import com.vocr.core.DebugLogger;

// C:\Users\Lenovo\Desktop\电脑村\电脑村重制相关文件\Village of Cyber Remake\src\main\java\com\vocr\history\GameRecordManager.java
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;

public class GameRecordManager {
    private GameRecord record;
    private final String savePath;

    public GameRecordManager() { this(PathConfig.GAME_RECORD); }

    /** 允许测试或便携运行环境指定独立存档位置。 */
    public GameRecordManager(String savePath) {
        if (savePath == null || savePath.isBlank()) {
            throw new IllegalArgumentException("存档路径不能为空");
        }
        this.savePath = savePath;
        load();
    }

    public void load() {
        File file = new File(savePath);
        if (file.exists() && file.length() > 10) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                record = mapper.readValue(file, GameRecord.class);
                DebugLogger.info("[GameRecordManager] 已加载: " + savePath);
            } catch (IOException e) {
                DebugLogger.error("[GameRecordManager] 加载失败（文件可能损坏，将重建）: " + e.getMessage());
                record = new GameRecord();
                save();
            }
        } else {
            if (file.exists() && file.length() <= 10) {
                file.delete();
                DebugLogger.warn("[GameRecordManager] 检测到空/过小存档，已删除");
            }
            record = new GameRecord();
            save();
        }
    }

    public void save() {
        try {
            File file = new File(savePath);
            File dir = file.getParentFile();
            if (dir != null && !dir.exists()) dir.mkdirs();

            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, record);
            DebugLogger.info("[GameRecordManager] 已保存: " + savePath);
        } catch (IOException e) {
            DebugLogger.error("[GameRecordManager] 保存失败: " + e.getMessage());
        }
    }

    public GameRecord getRecord() { return record; }

    public void updateRecord(int peiyiIndex, int result) {
        DebugLogger.info("[GameRecordManager] 调用update: peiyi=" + peiyiIndex + ", result=" + result);
        record.update(peiyiIndex, result);
        save();
    }
}
