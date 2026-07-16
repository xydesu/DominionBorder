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

    public void clearAllBorders() {
        for (UUID uuid : activeBorders.keySet()) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                clearBorder(player);
            } else {
                // 如果玩家已經離線，直接清空 task 與 displays
                BorderData data = activeBorders.remove(uuid);
                if (data != null) {
                    if (data.task != null) data.task.cancel();
                    if (data.displays != null) {
                        for (BlockDisplay display : data.displays) {
                            if (display.isValid()) display.remove();
                        }
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

        if(eventType.equals("select")) {
            maxX += 1.0;
            maxZ += 1.0;
        }

        int displayHeight = plugin.getConfig().getInt("display-height", 15);
        float thick = (float) plugin.getConfig().getDouble("border-thickness", 0.15); // 線框厚度
        if (thick <= 0.0f) thick = 0.15f; // 防止厚度為 0 導致完全隱形

        // 4 個垂直面的定義
        // 為了讓高度隨地形變化，我們不再全域計算 Y，而是用 {offsetX, offsetZ, sizeX, sizeZ} 來定義 2D 的邊緣線
        float sizeX = (float) (maxX - minX);
        float sizeZ = (float) (maxZ - minZ);
        
        float[][] faces2D = {
                // 北面 (Z = minZ)
                {0, 0, sizeX, thick},
                // 南面 (Z = maxZ)
                {0, sizeZ - thick, sizeX, thick},
                // 西面 (X = minX)
                {0, 0, thick, sizeZ},
                // 東面 (X = maxX)
                {sizeX - thick, 0, thick, sizeZ}
        };

        Location anchor = new Location(world, minX, 0, minZ); // anchorY 將被動態取代
        
        double maxSeg = plugin.getConfig().getDouble("max-segment-size", 16.0);
        if (maxSeg <= 0.1) maxSeg = 16.0;
        float viewRange = 100.0f * (float) plugin.getConfig().getDouble("view-range-multiplier", 1.0);

        for (float[] face : faces2D) {
            float startOffsetX = face[0];
            float startOffsetZ = face[1];
            float totalSizeX = face[2];
            float totalSizeZ = face[3];
            
            boolean xIsThick = Math.abs(totalSizeX - thick) < 0.001f;
            boolean zIsThick = Math.abs(totalSizeZ - thick) < 0.001f;
            
            // 沿著邊緣水平切割
            int countX = xIsThick ? 1 : (int) Math.ceil(totalSizeX / maxSeg);
            int countZ = zIsThick ? 1 : (int) Math.ceil(totalSizeZ / maxSeg);
            
            for (int ix = 0; ix < countX; ix++) {
                for (int iz = 0; iz < countZ; iz++) {
                    
                    float segStartX = xIsThick ? 0 : (float) (ix * (totalSizeX / countX));
                    float segStartZ = zIsThick ? 0 : (float) (iz * (totalSizeZ / countZ));
                    
                    float segSizeX = xIsThick ? thick : (float) (totalSizeX / countX);
                    float segSizeZ = zIsThick ? thick : (float) (totalSizeZ / countZ);
                    
                    // 取得這個分段水平範圍內的最低地形高度
                    double lowestTerrainY = 320; // 世界最高度
                    double stepX = xIsThick ? 0 : 1.0;
                    double stepZ = zIsThick ? 0 : 1.0;
                    int loopCount = (int) Math.max(segSizeX, segSizeZ);
                    if (loopCount <= 0) loopCount = 1;

                    for (int step = 0; step <= loopCount; step++) {
                        double checkX = anchor.getX() + startOffsetX + segStartX + step * stepX;
                        double checkZ = anchor.getZ() + startOffsetZ + segStartZ + step * stepZ;
                        
                        // 確保不超過區段邊界
                        if (!xIsThick) checkX = Math.min(checkX, anchor.getX() + startOffsetX + segStartX + segSizeX);
                        if (!zIsThick) checkZ = Math.min(checkZ, anchor.getZ() + startOffsetZ + segStartZ + segSizeZ);

                        double ty = world.getHighestBlockYAt((int) checkX, (int) checkZ);
                        if (ty < lowestTerrainY) {
                            lowestTerrainY = ty;
                        }
                    }
                    
                    double terrainY = lowestTerrainY;
                    
                    // 根據地形計算該垂直柱的 Y 範圍 (保證一定會向上延伸 displayHeight)
                    double segMinY = Math.max(cuboid.y1(), terrainY);
                    double segMaxY = segMinY + displayHeight;
                    
                    if (segMaxY < segMinY) segMaxY = segMinY;
                    float totalSizeY = (float) (segMaxY - segMinY);
                    
                    // 在 Y 軸上進行垂直切割
                    int countY = (totalSizeY <= 0) ? 1 : (int) Math.ceil(totalSizeY / maxSeg);
                    
                    for (int iy = 0; iy < countY; iy++) {
                        float segStartY = (float) (iy * (totalSizeY / countY));
                        float segSizeY = (float) (totalSizeY / countY);
                        
                        double spawnX = anchor.getX() + startOffsetX + segStartX;
                        double spawnY = segMinY + segStartY;
                        double spawnZ = anchor.getZ() + startOffsetZ + segStartZ;
                        Location loc = new Location(world, spawnX, spawnY, spawnZ);
                        
                        BlockDisplay display = (BlockDisplay) world.spawnEntity(loc, EntityType.BLOCK_DISPLAY);
                        display.setBlock(blockData);
                        display.setViewRange(viewRange);
                        
                        for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                            if (!p.equals(player)) {
                                p.hideEntity(plugin, display);
                            }
                        }
                        
                        display.setTransformation(new Transformation(
                                new Vector3f(0f, 0f, 0f),
                                new org.joml.Quaternionf(),
                                new Vector3f(segSizeX, segSizeY, segSizeZ),
                                new org.joml.Quaternionf()
                        ));
                        
                        display.setGlowing(false);
                        displays.add(display);
                    }
                }
            }
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
        double z1 = Math.min(cuboid.z1(), cuboid.z2());
        double z2 = Math.max(cuboid.z1(), cuboid.z2());
        
        int displayHeight = plugin.getConfig().getInt("display-height", 15);
        World world = player.getWorld();

        final List<double[]> pillarPoints = new ArrayList<>();
        // 北西角
        double tyNW = world.getHighestBlockYAt((int) x1, (int) z1);
        double y1NW = Math.max(cuboid.y1(), tyNW);
        for (double y = y1NW; y <= y1NW + displayHeight; y += pillarVerticalStep) pillarPoints.add(new double[]{x1, y, z1});
        // 北東角
        double tyNE = world.getHighestBlockYAt((int) x2, (int) z1);
        double y1NE = Math.max(cuboid.y1(), tyNE);
        for (double y = y1NE; y <= y1NE + displayHeight; y += pillarVerticalStep) pillarPoints.add(new double[]{x2, y, z1});
        // 南西角
        double tySW = world.getHighestBlockYAt((int) x1, (int) z2);
        double y1SW = Math.max(cuboid.y1(), tySW);
        for (double y = y1SW; y <= y1SW + displayHeight; y += pillarVerticalStep) pillarPoints.add(new double[]{x1, y, z2});
        // 南東角
        double tySE = world.getHighestBlockYAt((int) x2, (int) z2);
        double y1SE = Math.max(cuboid.y1(), tySE);
        for (double y = y1SE; y <= y1SE + displayHeight; y += pillarVerticalStep) pillarPoints.add(new double[]{x2, y, z2});

        final List<double[]> staticOutline = new ArrayList<>();
        for (double x = x1 + outlineStep; x < x2; x += outlineStep) {
            double ty = world.getHighestBlockYAt((int) x, (int) z1);
            double base = Math.max(cuboid.y1(), ty);
            staticOutline.add(new double[]{x, base, z1});
            staticOutline.add(new double[]{x, base + displayHeight, z1});
            
            ty = world.getHighestBlockYAt((int) x, (int) z2);
            base = Math.max(cuboid.y1(), ty);
            staticOutline.add(new double[]{x, base, z2});
            staticOutline.add(new double[]{x, base + displayHeight, z2});
        }
        for (double z = z1 + outlineStep; z < z2; z += outlineStep) {
            double ty = world.getHighestBlockYAt((int) x1, (int) z);
            double base = Math.max(cuboid.y1(), ty);
            staticOutline.add(new double[]{x1, base, z});
            staticOutline.add(new double[]{x1, base + displayHeight, z});
            
            ty = world.getHighestBlockYAt((int) x2, (int) z);
            base = Math.max(cuboid.y1(), ty);
            staticOutline.add(new double[]{x2, base, z});
            staticOutline.add(new double[]{x2, base + displayHeight, z});
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
