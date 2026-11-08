# STM32 HAL API 速查（桌面版）

面向 STM32 全家族主流系列的 HAL 库 API 速查工具，纯 Java Swing 实现，零第三方依赖。

## 下载

最新版本见 [Releases](https://github.com/lsjkkll/STM32_HAL_API_search_engine/releases/latest)：

| 文件 | 说明 |
|---|---|
| `HAL-Speed-Lookup-Windows-v*.zip` | Windows 免安装包（内置精简 JRE），解压后双击 `HAL-Speed-Lookup.exe`，无需另装 Java |
| `HAL-Speed-Lookup-Desktop-*.jar` | 可执行 JAR，需 JDK 17+，`java -jar HAL-Speed-Lookup-Desktop-2.0.1.jar` |

发布包附带 `SHA256SUMS.txt` 供校验；可用 `HAL-Speed-Lookup.exe --selftest` 确认数据完整（输出 `SELFTEST OK`）。

## 功能

- 198 个常用 HAL 接口，覆盖 16 个外设章节（CORE、RCC、GPIO、EXTI、UART、SPI、I2C、TIM、ADC、DMA、FLASH、IWDG、RTC、PWR、CAN、FDCAN）
- 13 个系列切换：F1 / F4 / C0 / G0 / G4 / H5 / H7 / L0 / L1 / L4 / L5 / U0 / U5，头文件名、示例与系列差异随之变化
- 每个章节包含：CubeMX 配置步骤、代码步骤、综合示例程序（按 CubeMX 的 USER CODE 区组织）
- 函数搜索、分类浏览、收藏、代码一键复制、相关接口跳转
- 深色主题，按 1080p / 2K / 4K 自动缩放字号
- 界面字体优先黑体，按 黑体 / SimHei → 等线 → 微软雅黑 → 苹方 / 思源黑体 的顺序探测（含中文系统本地化字体名），并直接加载系统黑体文件兜底，正文统一加粗
- 代码示例为 CLion 风格编辑器面板：左侧行号栏 + JetBrains Darcula 配色语法高亮（关键字、函数、注释、字符串、数字、预处理指令），等宽字体 Consolas → Cascadia Mono → JetBrains Mono → …，中文字符自动切到黑体、逐字挑字形，不会出现方块

## 系列说明

| 系列 | 内核 | 最高主频 | 典型型号 | 备注 |
|---|---|---|---|---|
| F1 | Cortex-M3 | 72 MHz | STM32F103C8T6 | AFIO 重映射、PLLMUL、ADC 无校准函数 |
| F4 | Cortex-M4F | 168 MHz | STM32F407ZGT6 | PLL M/N/P/Q、DMA Stream+Channel、扇区擦除 |
| C0 | Cortex-M0+ | 48 MHz | STM32C071RB | 无 PLL（HSI48/HSE 直连）；C092 带 FDCAN1 |
| G0 | Cortex-M0+ | 64 MHz | STM32G071RB | 带 PLL、DMAMUX；FDCAN 仅 G0B1/G0C1 |
| G4 | Cortex-M4F | 170 MHz | STM32G431RB | PLLPDIV、DIFSEL、FDCAN1-3 |
| H5 | Cortex-M33 | 250 MHz | STM32H563ZI | PLL1/2/3、GPDMA、扇区擦除、FDCAN |
| H7 | Cortex-M7 | 480 MHz（H743/H750） | STM32H743ZI | PLL1/2/3、Stream+BDMA+DMAMUX、16 位 ADC |
| L0 | Cortex-M0+ | 32 MHz | STM32L073RZ | PLLMUL/PLLDIV、固定通道 DMA、ADC 校准 |
| L1 | Cortex-M3 | 32 MHz | STM32L151C8 | ADC 类 F1、GPIO 有复用、RTC 日历 |
| L4 | Cortex-M4F | 80 MHz | STM32L476RG | PLL+PLLSAI、DMAMUX、部分型号 bxCAN |
| L5 | Cortex-M33 | 110 MHz | STM32L552ZE | TrustZone、DMAMUX、FDCAN1 |
| U0 | Cortex-M0+ | 56 MHz | STM32U073RB | 带 PLL、DMAMUX、低功耗 |
| U5 | Cortex-M33 | 160 MHz | STM32U575ZI | PLL1/2/3、GPDMA、14 位 ADC、FDCAN1 |

> 同一系列不同型号的外设实例数、Flash 页/扇区大小、是否带 CAN/FDCAN 可能不同；页面内容按系列粒度给出，具体以所选芯片的参考手册与 CubeMX 配置为准。

## 环境要求

- JDK 17 或更高版本

## 运行

```bash
javac -encoding UTF-8 -d build/classes $(find src/main/java -name "*.java")
cp -r src/main/resources/* build/classes/
java -cp build/classes com.stm32hal.api.desktop.Main
```

或直接使用 `build.sh`：

```bash
bash build.sh
java -jar dist/HAL-Speed-Lookup-Desktop-2.0.1.jar
```

IDE（IDEA）打开：把 `src/main/java` 标记为 Sources，`src/main/resources` 标记为 Resources，主类 `com.stm32hal.api.desktop.Main`。

数据自检：

```bash
java -jar dist/HAL-Speed-Lookup-Desktop-2.0.1.jar --selftest
```

## 目录结构

```
src/main/java/com/stm32hal/api/desktop/
  Main.java        程序入口与自检
  HalFrame.java    主界面、系列切换、章节总览、详情页
  Ui.java          主题、颜色与自定义控件
  Model.java       数据模型与系列元数据
  Json.java        轻量 JSON 解析
  FavStore.java    收藏与系列持久化
src/main/resources/assets/hal_api.json   接口与章节数据（v2.0.1）
build.sh           命令行构建脚本
```

## 数据说明

接口数据版本 v2.0.1，包含 198 条记录、13 个系列、16 个章节：
- 15 个通用章节 + 新增 FDCAN 章节（对 C0/G0/G4/H5/H7/L5/U5 显示，C0 仅 C092 型号有 FDCAN1）
- 每个系列对 16 个关键接口（RCC 时钟、GPIO、DMA、ADC、FLASH、CAN 等）提供「系列差异」说明
- 10 个关键接口（RCC/GPIO/MspInit/ADC/DMA/FLASH）为每个系列提供专属示例代码
- 4 个 F1 专属 AFIO 接口只在 F1 下显示；CAN（bxCAN）章节只对 F1/F4/L4 显示

收藏与系列选择保存在 `~/.stm32hal-api/prefs.properties`。
