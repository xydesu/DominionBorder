package me.xydesu.dominionborder.commands;

import cn.lunadeer.dominion.api.DominionAPI;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import me.xydesu.dominionborder.DominionBorderPlugin;
import me.xydesu.dominionborder.visualizer.BorderVisualizer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BorderCommand implements CommandExecutor, TabCompleter {

    private final DominionBorderPlugin plugin;
    private final BorderVisualizer visualizer;

    public BorderCommand(DominionBorderPlugin plugin, BorderVisualizer visualizer) {
        this.plugin = plugin;
        this.visualizer = visualizer;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("dominionborder.admin")) {
            sender.sendMessage("§c你沒有權限使用此指令！");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§e==== DominionBorder 指令 ====");
            sender.sendMessage("§e/dborder reload §7- 重載設定檔");
            sender.sendMessage("§e/dborder show §7- 強制顯示當前所在領地邊界");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage("§a[DominionBorder] 設定檔已重載！");
            return true;
        }

        if (args[0].equalsIgnoreCase("show")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§c只有玩家可以使用此指令！");
                return true;
            }

            DominionAPI api = DominionAPI.getInstance();
            if (api == null) {
                sender.sendMessage("§c無法取得 DominionAPI！");
                return true;
            }

            DominionDTO dom = api.getDominion(player.getLocation());
            if (dom != null && dom.getCuboid() != null) {
                visualizer.showBorder(player, dom.getCuboid(), "view");
                sender.sendMessage("§a[DominionBorder] 已顯示 " + dom.getName() + " 的邊界！");
            } else {
                sender.sendMessage("§c你當前位置不在任何領地內，或該領地無邊界資訊！");
            }
            return true;
        }

        sender.sendMessage("§c未知的指令用法！請輸入 /dborder 查看說明。");
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission("dominionborder.admin")) {
            if ("reload".startsWith(args[0].toLowerCase())) completions.add("reload");
            if ("show".startsWith(args[0].toLowerCase())) completions.add("show");
        }
        return completions;
    }
}
