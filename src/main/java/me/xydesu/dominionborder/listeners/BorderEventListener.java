package me.xydesu.dominionborder.listeners;

import cn.lunadeer.dominion.api.DominionAPI;
import cn.lunadeer.dominion.api.dtos.DominionDTO;
import cn.lunadeer.dominion.events.PlayerMoveInDominionEvent;
import cn.lunadeer.dominion.events.dominion.DominionCreateEvent;
import me.xydesu.dominionborder.DominionBorderPlugin;
import me.xydesu.dominionborder.visualizer.BorderVisualizer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.Location;
import cn.lunadeer.dominion.api.dtos.CuboidDTO;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

public class BorderEventListener implements Listener {

    private final DominionBorderPlugin plugin;
    private final BorderVisualizer visualizer;

    public BorderEventListener(DominionBorderPlugin plugin) {
        this.plugin = plugin;
        this.visualizer = new BorderVisualizer(plugin);
    }

    public BorderVisualizer getVisualizer() {
        return visualizer;
    }

    @EventHandler
    public void onDominionCreate(DominionCreateEvent event) {
        if (!(event.getOperator() instanceof Player player)) {
            return;
        }

        event.afterCreated(dom -> {
            if (dom != null && dom.getCuboid() != null) {
                plugin.debug("偵測到玩家 " + player.getName() + " 建立領地 " + dom.getName() + "，準備顯示邊界...");
                visualizer.showBorder(player, dom.getCuboid(), "create");
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMoveInDominion(PlayerMoveInDominionEvent event) {
        Player player = event.getPlayer();
        DominionDTO dom = event.getDominion();
        if (dom != null && dom.getCuboid() != null) {
            cn.lunadeer.dominion.api.DominionAPI api = cn.lunadeer.dominion.api.DominionAPI.getInstance();
            if (api != null && !api.checkEnvironmentFlag(dom, cn.lunadeer.dominion.api.dtos.flag.Flags.SHOW_BORDER)) {
                return; // 如果該領地的 show_border flag 被設為 false，則不顯示
            }
            plugin.debug("偵測到玩家 " + player.getName() + " 進入領地 " + dom.getName() + "，準備顯示邊界...");
            visualizer.showBorder(player, dom.getCuboid(), "enter");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK &&
            event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();

        // 檢查是否是使用 checkItem (查看既有領地)
        String checkItemName = plugin.getConfig().getString("check-item", "STRING");
        Material checkItem;
        try {
            checkItem = Material.valueOf(checkItemName.toUpperCase());
        } catch (IllegalArgumentException e) {
            checkItem = Material.STRING;
        }

        if (event.getItem() != null && event.getItem().getType() == checkItem &&
            (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)) {
            
            plugin.debug(player.getName() + " 左鍵使用了檢查物品: " + checkItem.name());

            DominionAPI api = DominionAPI.getInstance();
            if (api == null) {
                plugin.debug("發生錯誤：無法取得 DominionAPI 實例！");
                return;
            }

            DominionDTO dom = null;
            
            // 如果是點擊實體方塊，優先以方塊的座標來查詢領地
            if (event.getClickedBlock() != null) {
                dom = api.getDominion(event.getClickedBlock().getLocation());
            }
            
            // 如果方塊不在領地內，或玩家是對空氣左鍵，則改用玩家當前座標查詢
            if (dom == null) {
                dom = api.getDominion(player.getLocation());
            }

            if (dom != null && dom.getCuboid() != null) {
                plugin.debug("成功取得領地資料 (" + dom.getName() + ")，正在呼叫 visualizer.showBorder() ...");
                visualizer.showBorder(player, dom.getCuboid(), "view");
            } else {
                plugin.debug("查詢不到領地，玩家與點擊位置都不在領地內。");
            }
            return;
        }
    }

    // 監聽創建工具的使用 (Monitor 優先級，確保 Dominion 已經處理完選擇)
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerSelectPoint(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (event.getItem() == null) return;

        try {
            // 透過反射獲取 Dominion 的 selectTool
            Class<?> configClass = Class.forName("cn.lunadeer.dominion.configuration.Configuration");
            Field selectToolField = configClass.getField("selectTool");
            String selectToolStr = (String) selectToolField.get(null);
            Material selectTool = Material.matchMaterial(selectToolStr != null ? selectToolStr : "BONE");

            if (event.getItem().getType() != selectTool) {
                return;
            }

            // 透過反射獲取 Dominion.pointsSelect
            Class<?> dominionClass = Class.forName("cn.lunadeer.dominion.Dominion");
            Field pointsSelectField = dominionClass.getField("pointsSelect");
            @SuppressWarnings("unchecked")
            Map<UUID, Map<Integer, Location>> pointsSelect = (Map<UUID, Map<Integer, Location>>) pointsSelectField.get(null);

            if (pointsSelect != null && pointsSelect.containsKey(player.getUniqueId())) {
                Map<Integer, Location> points = pointsSelect.get(player.getUniqueId());
                if (points != null && points.size() == 2 && points.containsKey(0) && points.containsKey(1)) {
                    Location loc0 = points.get(0);
                    Location loc1 = points.get(1);

                    if (loc0 != null && loc1 != null && loc0.getWorld() != null && loc0.getWorld().equals(loc1.getWorld())) {
                        CuboidDTO cuboid = new CuboidDTO(loc0, loc1);
                        plugin.debug("偵測到 " + player.getName() + " 使用創建工具圈地，準備顯示預覽邊界...");
                        visualizer.showBorder(player, cuboid, "select");
                    } else {
                        // 如果兩點在不同世界，默默清除舊的邊界預覽
                        visualizer.clearBorder(player);
                    }
                } else {
                    // 如果點位不完整 (例如只點了第一點)，默默清除舊的邊界預覽
                    visualizer.clearBorder(player);
                }
            }

        } catch (Exception e) {
            // 反射失敗，可能是 Dominion 版本變更，靜默失敗或只紀錄一次警告
        }
    }
}
