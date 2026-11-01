# STM32 HAL API 速查（桌面版）

面向 STM32F1 / STM32F4 的 HAL 库 API 速查工具，纯 Java Swing 实现，零第三方依赖。

## 下载

最新版本见 [Releases](https://github.com/lsjkkll/STM32_HAL_API_search_engine/releases/latest)：

| 文件 | 说明 |
|---|---|
| `HAL-Speed-Lookup-Windows-v*.zip` | Windows 免安装包（内置精简 JRE），解压后双击 `HAL-Speed-Lookup.exe`，无需另装 Java |
| `HAL-Speed-Lookup-Desktop-*.jar` | 可执行 JAR，需 JDK 17+，`java -jar HAL-Speed-Lookup-Desktop-1.0.0.jar` |

发布包附带 `SHA256SUMS.txt` 供校验；可用 `HAL-Speed-Lookup.exe --selftest` 确认数据完整（输出 `SELFTEST OK`）。

## 功能

- 186 个常用 HAL 接口，覆盖 15 个外设章节（CORE、RCC、GPIO、EXTI、UART、SPI、I2C、TIM、ADC、DMA、FLASH、IWDG、RTC、PWR、CAN）
- F1 / F4 系列切换：头文件名、计数器、示例与系列差异随之变化
- 每个章节包含：CubeMX 配置步骤、代码步骤、综合示例程序（按 CubeMX 的 USER CODE 区组织）
- 函数搜索、分类浏览、收藏、代码一键复制、相关接口跳转
- 深色主题，按 1080p / 2K / 4K 自动缩放字号

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
java -jar dist/HAL-Speed-Lookup-Desktop-1.0.0.jar
```

IDE（IDEA）打开：把 `src/main/java` 标记为 Sources，`src/main/resources` 标记为 Resources，主类 `com.stm32hal.api.desktop.Main`。

数据自检：

```bash
java -jar dist/HAL-Speed-Lookup-Desktop-1.0.0.jar --selftest
```

## 目录结构

```
src/main/java/com/stm32hal/api/desktop/
  Main.java        程序入口与自检
  HalFrame.java    主界面、章节总览、详情页
  Ui.java          主题、颜色与自定义控件
  Model.java       数据模型与系列枚举
  Json.java        轻量 JSON 解析
  FavStore.java    收藏与系列持久化
src/main/resources/assets/hal_api.json   接口与章节数据（v1.3.0）
build.sh           命令行构建脚本
```

## 数据说明

接口数据版本 v1.3.0，包含 186 条记录、16 条 F1/F4 系列差异、10 条 F1 专属示例与 4 个 F1 专属 AFIO 接口。

收藏与系列选择保存在 `~/.stm32hal-api/prefs.properties`。
