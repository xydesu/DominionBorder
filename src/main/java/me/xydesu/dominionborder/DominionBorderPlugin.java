package me.xydesu.dominionborder;

import me.xydesu.dominionborder.listeners.BorderEventListener;
import me.xydesu.dominionborder.commands.BorderCommand;
import me.xydesu.dominionborder.visualizer.BorderVisualizer;
import org.bukkit.plugin.java.JavaPlugin;

public class DominionBorderPlugin extends JavaPlugin {

    private static DominionBorderPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        
        // 儲存預設設定檔
        saveDefaultConfig();
        
        // 註冊事件監聽器
        BorderEventListener listener = new BorderEventListener(this);
        getServer().getPluginManager().registerEvents(listener, this);

        // 註冊指令
        this.visualizer = listener.getVisualizer();
        BorderCommand command = new BorderCommand(this, visualizer);
        if (getCommand("dominionborder") != null) {
            getCommand("dominionborder").setExecutor(command);
            getCommand("dominionborder").setTabCompleter(command);
        }
        
        getLogger().info("DominionBorder 已啟用！");
    }

    private BorderVisualizer visualizer;

    @Override
    public void onDisable() {
        if (visualizer != null) {
            visualizer.clearAllBorders();
        }
        getLogger().info("DominionBorder 已停用！");
    }

    public static DominionBorderPlugin getInstance() {
        return instance;
    }

    public void debug(String message) {
        if (getConfig().getBoolean("debug", false)) {
            getLogger().info("[DEBUG] " + message);
        }
    }
}
