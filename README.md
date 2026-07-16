# DominionBorder

**專為 Dominion 領地插件設計的 3D 邊界視覺化擴充套件**

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21+-success.svg)
![API](https://img.shields.io/badge/API-Spigot-orange.svg)
![Dependency](https://img.shields.io/badge/Dependency-Dominion-blue.svg)

---

## 簡介
**DominionBorder** 是一個為 [Dominion](https://github.com/xydesu/Dominion) 領地插件設計的附屬套件。它會在玩家進入領地、查看領地或使用圈地工具時，顯示對應的領地立體邊界。

本插件利用 Minecraft 1.21 引入的 `BlockDisplay` 實體技術來顯示邊界，並會根據世界地形的高度自動調整邊界網格的垂直位置。

## 核心功能
- **BlockDisplay 顯示**：使用純客戶端渲染的 3D 面板來顯示邊界，取代傳統的實體方塊替換方式。
- **地形貼合**：邊界會自動掃描目標位置的地形最高點 (`getHighestBlockYAt`)，讓邊界底端順著地形高低起伏。
- **自訂情境顏色**：支援為不同的動作（進入領地、查詢領地、圈地預覽、創建成功）配置不同的方塊材質或玻璃顏色。
- **顯示優化**：
  - 使用大網格合併渲染（預設 16x16），以降低客戶端的實體渲染數量。
  - 於伺服器關閉或玩家登出時自動清理對應的邊界實體，防止資源未正確釋放。
- **支援粒子模式**：除了方塊顯示外，亦可切換為傳統的粒子 (Particle) 模式來顯示邊界。

---

## 觸發時機
本插件會在以下時機觸發邊界顯示：
1. **進入領地時**：玩家跨越領地邊界時自動顯示。
2. **使用工具查詢時**：手持設定的檢查物品（預設為 `字串 / String`），左鍵點擊領地內的空氣或方塊。
3. **圈地預覽**：當玩家使用 Dominion 圈地工具選定兩點時，自動顯示預覽邊界。
4. **建立領地後**：成功創建領地時顯示。

---

## 安裝說明
1. 確保伺服器核心版本為 **Spigot / Paper 1.21 或更高版本**。
2. 確保伺服器已安裝前置插件：**Dominion**。
3. 下載 `DominionBorder-1.0.0.jar` 並放置於伺服器的 `plugins/` 資料夾。
4. 重新啟動伺服器。
5. 前往 `plugins/DominionBorder/config.yml` 進行後續設定。

---

## 設定檔說明 (`config.yml`)
以下為預設的設定項目說明：

```yaml
# ==========================================
# DominionBorder 核心設定
# ==========================================

# 顯示模式 (可選: BLOCK_DISPLAY 或 PARTICLE)
display-mode: "BLOCK_DISPLAY"

# 邊界預設顯示秒數
duration-seconds: 5

# 預設使用的方塊材質 (當沒有特別定義動作顏色時使用)
block-type: "YELLOW_STAINED_GLASS"

# 線框厚度 (0.01 ~ 1.0)
border-thickness: 0.15

# 邊界高度 (自地形往上延伸的高度)
display-height: 15

# 最大分段大小 (預設 16.0，降低可提升精細度但會增加實體數量)
max-segment-size: 16.0

# 用來手動查詢領地的物品 (左鍵空氣或方塊)
check-item: "STRING"

# ==========================================
# 情境顏色設定 (BlockDisplay 專用)
# ==========================================
colors:
  enter: "YELLOW_STAINED_GLASS"    # 進入領地
  view: "LIGHT_BLUE_STAINED_GLASS" # 查詢領地
  create: "LIME_STAINED_GLASS"     # 創建成功
  select: "ORANGE_STAINED_GLASS"   # 圈地預覽
```

---

## 指令與權限
- **`/dominionborder reload`** 或 **`/db reload`**
  - **功能**：重新載入 `config.yml` 的設定。
  - **權限**：`dominionborder.admin`

## 參與貢獻
如果您發現 Bug 或有改進建議，歡迎提交 Issue 或 Pull Request。