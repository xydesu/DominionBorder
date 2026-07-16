<div align="center">
  <h1>🛡️ DominionBorder</h1>
  <p><b>專為 Dominion 領地插件設計的 3D 邊界視覺化擴充套件</b></p>
  <p>
    <img src="https://img.shields.io/badge/Minecraft-1.21+-success.svg" alt="Minecraft Version">
    <img src="https://img.shields.io/badge/API-Spigot-orange.svg" alt="API">
    <img src="https://img.shields.io/badge/Dependency-Dominion-blue.svg" alt="Dependency">
  </p>
</div>

---

## 🌟 簡介
**DominionBorder** 是一個為 [Dominion](https://github.com/xydesu/Dominion) 領地插件設計的附屬套件。它能夠在玩家進入領地、查看領地或使用圈地工具時，以直觀且效能極佳的方式在世界上繪製出領地的立體邊界。

這款插件利用了 Minecraft 1.21 全新的 `BlockDisplay` 實體技術，不僅畫面美觀，還能自動根據您的地形高低起伏，創造出完美貼合地表的「虛擬城牆」！

## ✨ 核心特色
- 🧱 **全新 BlockDisplay 繪製**：摒棄傳統且消耗資源的方塊替換方式，使用純客戶端渲染的 3D 面板，保證極致滑順。
- ⛰️ **智慧地形貼合**：邊界會自動掃描地形最高點，順著山谷與高山起伏，不會出現懸空或深埋地下的狀況。
- 🎨 **自訂情境顏色**：可以為不同的動作（進入領地、查看領地、圈地預覽）自訂不同的方塊材質或玻璃顏色。
- ⚡ **極致效能優化**：
  - 使用大網格 (16x16) 合併渲染，將實體數量降至最低。
  - 啟動時快取反射 (Reflection) 運算，確保玩家頻繁圈地時伺服器依然流暢。
  - 嚴格的生命週期管理，玩家登出或重啟時自動清理，杜絕幽靈方塊 (Ghost Entities)。
- 🎇 **雙重顯示模式**：除了方塊顯示外，還支援傳統的粒子 (Particle) 模式，滿足不同伺服器的需求。

---

## 📸 運作機制
本插件會在以下幾個時機自動為玩家觸發邊界顯示：
1. **進入領地時**：跨越領地邊界時自動顯示數秒鐘。
2. **使用工具查詢時**：手持指定的檢查物品（預設為 `字串 / String`）左鍵點擊領地內的空氣或方塊。
3. **圈地預覽**：當玩家使用 Dominion 的圈地工具點選了兩個點時，自動為其顯示即將圈選的立體邊界範圍。
4. **建立領地後**：成功創建領地後給予視覺回饋。

---

## ⚙️ 安裝說明
1. 確保您的伺服器為 **Spigot / Paper 1.21 或更高版本**。
2. 確保伺服器已安裝前置插件：**Dominion**。
3. 下載 `DominionBorder-1.0.0.jar` 並放入伺服器的 `plugins/` 資料夾。
4. 啟動或重啟伺服器。
5. 前往 `plugins/DominionBorder/config.yml` 進行自訂設定。

---

## 🛠️ 設定檔說明 (`config.yml`)
本插件具有極高的自訂性，以下為重要的設定項目：

```yaml
# ==========================================
# DominionBorder 核心設定
# ==========================================

# 顯示模式 (可選: BLOCK_DISPLAY 或 PARTICLE)
# 推薦使用 BLOCK_DISPLAY 視覺效果最佳且效能好
display-mode: "BLOCK_DISPLAY"

# 邊界預設顯示秒數
duration-seconds: 5

# 預設使用的方塊材質 (如果下方沒有特別定義動作顏色)
block-type: "YELLOW_STAINED_GLASS"

# 線框厚度 (0.01 ~ 1.0)
border-thickness: 0.15

# 邊界高度 (自地形往上延伸的高度，預設15格)
display-height: 15

# 最大分段大小 (預設 16.0，建議不要更改以保持最佳效能)
max-segment-size: 16.0

# 用來手動查詢領地的物品 (左鍵空氣或方塊)
check-item: "STRING"

# ==========================================
# 情境顏色設定 (BlockDisplay 專用)
# ==========================================
colors:
  enter: "YELLOW_STAINED_GLASS"  # 進入領地
  view: "LIGHT_BLUE_STAINED_GLASS" # 查詢領地
  create: "LIME_STAINED_GLASS"   # 創建成功
  select: "ORANGE_STAINED_GLASS" # 圈地預覽
```

---

## 🧑‍💻 指令與權限
- **`/dominionborder reload`** 或 **`/db reload`**
  - **功能**：重新載入 `config.yml` 的設定。
  - **權限**：`dominionborder.admin`

## 🤝 參與貢獻
如果您有任何問題、建議或發現 Bug，歡迎透過 [Issues] 提交回報。如果您想要直接貢獻程式碼，非常歡迎發起 Pull Request！