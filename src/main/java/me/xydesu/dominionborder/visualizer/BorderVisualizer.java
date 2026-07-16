package me.xydesu.dominionborder.visualizer;

import cn.lunadeer.dominion.api.dtos.CuboidDTO;
import me.xydesu.dominionborder.DominionBorderPlugin;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.scheduler.BukkitTask;

public class BorderVisualizer {

    private final DominionBorderPlugin plugin;

    private static class BorderData {
        List<BlockDisplay> displays;
        BukkitTask task;
        BorderData(List<BlockDisplay> displays, BukkitTask task) {
            this.displays = displays;
            this.task = task;
        }
    }

    private final Map<UUID, BorderData> activeBorders = new ConcurrentHashMap<>();

    public BorderVisualizer(DominionBorderPlugin plugin) {
        this.plugin = plugin;
    }

    public void clearBorder(Player player) {
        BorderData data = activeBorders.remove(player.getUniqueId());
        if (data != null) {
            if (data.task != null) {
                data.task.cancel();
            }
            if (data.displays != null) {
                for (BlockDisplay display : data.displays) {
                    if (display.isValid()) {
                        display.remove();
                    }
                }
            }
        }
    }

    public void showBorder(Player player, CuboidDTO cuboid, String eventType) {
        // 先清除該玩家之前顯示的邊界，避免連點疊加
        clearBorder(player);

        String mode = plugin.getConfig().getString("display-mode", "BLOCK_DISPLAY").toUpperCase();
        int durationSeconds = plugin.getConfig().getInt("duration-seconds", 5);

        plugin.debug(player.getName() + " 觸發了邊界顯示。模式: " + mode + "，持續時間: " + durationSeconds + " 秒。");

        if ("PARTICLE".equals(mode)) {
            showParticleBorder(player, cuboid, durationSeconds);
        } else {
            showBlockDisplayBorder(player, cuboid, durationSeconds, eventType);
        }
    }

    private void showBlockDisplayBorder(Player player, CuboidDTO cuboid, int durationSeconds, String eventType) {
        World world = player.getWorld();
        
        // 嘗試從 config 的 colors 節點獲取此事件的自訂顏色
        String colorPath = "colors." + eventType;
        String blockTypeName = plugin.getConfig().getString(colorPath);
        
        // 如果沒有設定此事件的顏色，或者設定無效，退回使用預設的 block-type
        if (blockTypeName == null || blockTypeName.isEmpty()) {
            blockTypeName = plugin.getConfig().getString("block-type", "YELLOW_STAINED_GLASS");
        }
        
        Material material = Material.getMaterial(blockTypeName.toUpperCase());
        if (material == null || !material.isBlock()) {
            material = Material.getMaterial(plugin.getConfig().getString("block-type", "YELLOW_STAINED_GLASS").toUpperCase());
            if (material == null || !material.isBlock()) {
                material = Material.YELLOW_STAINED_GLASS;
            }
        }

        BlockData blockData = material.createBlockData();
        List<BlockDisplay> displays = new ArrayList<>();

        double minX = cuboid.x1();
        double minZ = cuboid.z1();
        double maxX = cuboid.x2();
        double maxZ = cuboid.z2();
        double maxY = cuboid.y2();

        if(eventType.equals("select")) {
            maxX += 1.0;
            maxZ += 1.0;
        }
        

        // 計算 Y 軸上下限
        double playerY = player.getLocation().getY();
        //int displayHeight = plugin.getConfig().getInt("display-height", 15);
        
        // 底部：取領地原本底部與玩家腳下的最小值 (不低於領地，也不低於玩家腳下)
        double top = player.getWorld().getHighestBlockAt(player.getLocation()).getY();
        double minY;
        if(playerY > top) {
            minY = Math.max(cuboid.y1(), top - 1.0);
        } else {
            minY = Math.max(cuboid.y1(), playerY - 1.0);
        }
        // 頂部：取領地原本頂部與 minY+displayHeight 之中的最小值 (不高於領地，也不超過設定顯示高度)
        //double maxY = Math.min(cuboid.y2() + 1.0, minY + displayHeight);

        // 如果計算後的上限低於下限，可能是因為玩家在遠低於/遠高於領地的地方
        // 為了避免報錯，我們強制 maxY 至少等於 minY
        if (maxY < minY) {
            maxY = minY;
        }

        float sizeX = (float) (maxX - minX);
        float sizeY = (float) (maxY - minY);
        float sizeZ = (float) (maxZ - minZ);
        float thick = (float) plugin.getConfig().getDouble("border-thickness", 0.15); // 線框厚度
        if (thick <= 0.0f) thick = 0.15f; // 防止厚度為 0 導致完全隱形

        // 6 個面的起點偏移量與大小 (Scale)
        // {offsetX, offsetY, offsetZ, scaleX, scaleY, scaleZ}
        float[][] faces = {
                // 北面 (Z = z1)
                {0, 0, 0, sizeX, sizeY, thick},
                // 南面 (Z = z2)
                {0, 0, sizeZ - thick, sizeX, sizeY, thick},
                // 西面 (X = x1)
                {0, 0, 0, thick, sizeY, sizeZ},
                // 東面 (X = x2)
                {sizeX - thick, 0, 0, thick, sizeY, sizeZ}
        };

        Location anchor = new Location(world, minX, minY, minZ);

        for (float[] face : faces) {
            // 計算面的起始點 (Corner)
            double startX = anchor.getX() + face[0];
            double startY = anchor.getY() + face[1];
            double startZ = anchor.getZ() + face[2];
            Location loc = new Location(world, startX, startY, startZ);

            BlockDisplay display = (BlockDisplay) world.spawnEntity(loc, EntityType.BLOCK_DISPLAY);
            display.setBlock(blockData);
            display.setViewRange(100.0f); // 增加客戶端渲染距離

            // 隱藏對其他玩家的顯示
            for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                if (!p.equals(player)) {
                    p.hideEntity(plugin, display);
                }
            }

            // 直接以角落為原點，設定大小，不需要 Translation
            display.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new org.joml.Quaternionf(),
                    new Vector3f(face[3], face[4], face[5]),
                    new org.joml.Quaternionf()
            ));

            display.setGlowing(true); // 改成一整片的話，發光可能會太亮，所以關閉外框發光

            displays.add(display);
        }
        
        plugin.debug("已成功為玩家 " + player.getName() + " 生成 " + displays.size() + " 個 BlockDisplay 面，使用材質: " + material.name());

        // 排程清除
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                clearBorder(player);
            }
        }.runTaskLater(plugin, durationSeconds * 20L);

        activeBorders.put(player.getUniqueId(), new BorderData(displays, task));
    }

    private void showParticleBorder(Player player, CuboidDTO cuboid, int durationSeconds) {
        String particleName = plugin.getConfig().getString("particle-type", "HAPPY_VILLAGER");
        Particle particle;
        try {
            particle = Particle.valueOf(particleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            particle = Particle.HAPPY_VILLAGER;
        }

        double pillarVerticalStep = plugin.getConfig().getDouble("particle-pillar-step", 0.5);
        double outlineStep = plugin.getConfig().getDouble("particle-outline-step", 0.5);

        double x1 = Math.min(cuboid.x1(), cuboid.x2());
        double x2 = Math.max(cuboid.x1(), cuboid.x2());
        double y1 = Math.min(cuboid.y1(), cuboid.y2());
        double y2 = Math.max(cuboid.y1(), cuboid.y2());
        double z1 = Math.min(cuboid.z1(), cuboid.z2());
        double z2 = Math.max(cuboid.z1(), cuboid.z2());

        final List<double[]> pillarPoints = new ArrayList<>();
        for (double y = y1; y <= y2; y += pillarVerticalStep) {
            pillarPoints.add(new double[]{x1, y, z1});
            pillarPoints.add(new double[]{x2, y, z1});
            pillarPoints.add(new double[]{x1, y, z2});
            pillarPoints.add(new double[]{x2, y, z2});
        }

        final List<double[]> staticOutline = new ArrayList<>();
        for (double y : new double[]{y1, y2}) {
            for (double x = x1 + outlineStep; x < x2; x += outlineStep) {
                staticOutline.add(new double[]{x, y, z1});
                staticOutline.add(new double[]{x, y, z2});
            }
            for (double z = z1 + outlineStep; z < z2; z += outlineStep) {
                staticOutline.add(new double[]{x1, y, z});
                staticOutline.add(new double[]{x2, y, z});
            }
        }

        final Particle finalParticle = particle;
        int intervalTicks = 10;
        int durationTicks = durationSeconds * 20;

        BukkitTask task = new BukkitRunnable() {
            int elapsed = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    activeBorders.remove(player.getUniqueId());
                    return;
                }

                // 四條垂直角柱
                for (double[] p : pillarPoints) {
                    player.spawnParticle(finalParticle, p[0], p[1], p[2], 1, 0, 0, 0, 0);
                }

                // 頂/底兩圈靜態邊框
                for (double[] p : staticOutline) {
                    player.spawnParticle(finalParticle, p[0], p[1], p[2], 1, 0, 0, 0, 0);
                }

                elapsed += intervalTicks;
                if (elapsed >= durationTicks) {
                    cancel();
                    activeBorders.remove(player.getUniqueId());
                }
            }
        }.runTaskTimer(plugin, 0L, intervalTicks);

        activeBorders.put(player.getUniqueId(), new BorderData(null, task));
    }
}
