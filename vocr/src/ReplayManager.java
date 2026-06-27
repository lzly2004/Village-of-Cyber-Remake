// C:\Users\Lenovo\Desktop\电脑村\电脑村重制相关文件\Village of Cyber Remake\vocr\src\ReplayManager.java
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;

public class ReplayManager {
    public static final int MAX_SLOTS = 9;
    private static final String SAVE_FILE = "data/replay_saves.json";

    private ReplaySave[] slots = new ReplaySave[MAX_SLOTS];

    public ReplayManager() { load(); }

    public void load() {
        File file = new File(SAVE_FILE);
        if (file.exists() && file.length() > 50) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                slots = mapper.readValue(file, ReplaySave[].class);
                
                int validCount = 0;
                for (int i = 0; i < slots.length; i++) {
                    if (slots[i] != null && slots[i].rawRecords != null && !slots[i].rawRecords.isEmpty()) {
                        validCount++;
                    } else {
                        slots[i] = null; // 清理无效数据
                    }
                }
                
                DebugLogger.info("[ReplayManager] 已加载存档数据: " + SAVE_FILE + ", 有效存档=" + validCount + "/" + MAX_SLOTS);
            } catch (Exception e) {
                DebugLogger.error("[ReplayManager] 加载失败（可能格式不兼容，将重建）: " + e.getMessage());
                e.printStackTrace();
                
                // 备份损坏的文件
                if (file.exists()) {
                    File backup = new File(SAVE_FILE + ".corrupted." + System.currentTimeMillis());
                    file.renameTo(backup);
                    DebugLogger.warn("[ReplayManager] 已备份损坏文件到: " + backup.getName());
                }
                
                slots = new ReplaySave[MAX_SLOTS];
                save(); // 创建新的空存档文件
            }
        } else {
            if (file.exists() && file.length() <= 50) {
                file.delete();
                DebugLogger.warn("[ReplayManager] 检测到空/过小存档，已删除");
            }
            save();
        }
    }

    public void save() {
        try {
            File file = new File(SAVE_FILE);
            File dir = file.getParentFile();
            if (dir != null && !dir.exists()) dir.mkdirs();

            // 验证数据有效性
            for (int i = 0; i < slots.length; i++) {
                if (slots[i] != null) {
                    if (slots[i].rawRecords == null) slots[i].rawRecords = new java.util.ArrayList<>();
                    if (slots[i].daySnapshots == null) slots[i].daySnapshots = new java.util.ArrayList<>();
                    if (slots[i].saveTime == null) slots[i].saveTime = "unknown";
                    if (slots[i].peiyiName == null) slots[i].peiyiName = "unknown";
                }
            }
            
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT, true);
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, slots);
            DebugLogger.info("[ReplayManager] 已保存存档数据: " + SAVE_FILE);
        } catch (Exception e) {
            DebugLogger.error("[ReplayManager] 保存失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean isSlotEmpty(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= MAX_SLOTS) return true;
        return slots[slotIndex] == null;
    }

    public ReplaySave getSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= MAX_SLOTS) return null;
        return slots[slotIndex];
    }

    public void saveToSlot(int slotIndex, ReplaySave save) {
        if (slotIndex < 0 || slotIndex >= MAX_SLOTS) return;
        slots[slotIndex] = save;
        slots[slotIndex].slotIndex = slotIndex;
        save();
        DebugLogger.info("[ReplayManager] 已保存到槽位 " + slotIndex);
    }

    public void deleteSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= MAX_SLOTS) return;
        slots[slotIndex] = null;
        save();
        DebugLogger.info("[ReplayManager] 已删除槽位 " + slotIndex);
    }

    public int getUsedSlotCount() {
        int count = 0;
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (slots[i] != null) count++;
        }
        return count;
    }
}