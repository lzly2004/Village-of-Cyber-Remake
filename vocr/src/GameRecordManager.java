// C:\Users\Lenovo\Desktop\电脑村\电脑村重制相关文件\Village of Cyber Remake\vocr\src\GameRecordManager.java
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;

public class GameRecordManager {
    private static GameRecordManager instance;
    private GameRecord record;
    private static final String SAVE_PATH = "data/game_record.json";

    private GameRecordManager() { load(); }

    public static synchronized GameRecordManager getInstance() {
        if (instance == null) instance = new GameRecordManager();
        return instance;
    }

    public void load() {
        File file = new File(SAVE_PATH);
        if (file.exists() && file.length() > 10) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                record = mapper.readValue(file, GameRecord.class);
                DebugLogger.info("[GameRecordManager] 已加载: " + SAVE_PATH);
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
            File file = new File(SAVE_PATH);
            File dir = file.getParentFile();
            if (dir != null && !dir.exists()) dir.mkdirs();

            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, record);
            DebugLogger.info("[GameRecordManager] 已保存: " + SAVE_PATH);
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