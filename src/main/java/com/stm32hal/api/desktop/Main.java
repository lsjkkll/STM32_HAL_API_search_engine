package com.stm32hal.api.desktop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** 桌面端入口。 */
public final class Main {

    /** 期望的系列清单（顺序与数据一致）。 */
    private static final List<String> EXPECTED_SERIES = List.of(
            "f1", "f4", "c0", "g0", "g4", "h5", "h7", "l0", "l1", "l4", "l5", "u0", "u5");

    /** 15 个通用章节 + FDCAN。 */
    private static final int EXPECTED_CHAPTERS = 16;

    /** 期望接口总数：v1 的 186 个 + FDCAN 新章节 12 个。 */
    private static final int EXPECTED_APIS = 198;

    /** 需要有系列差异说明的接口（15 个通用 + bxCAN）。 */
    private static final List<String> DIFF_APIS = List.of(
            "HAL_RCC_OscConfig", "HAL_RCC_ClockConfig", "HAL_RCCEx_PeriphCLKConfig",
            "HAL_GPIO_Init", "HAL_UART_MspInit", "HAL_SPI_MspInit", "HAL_I2C_MspInit",
            "HAL_TIM_Base_Start_DMA", "HAL_ADC_Init", "HAL_ADC_Start_DMA",
            "HAL_ADCEx_Calibration_Start", "HAL_DMA_Init", "__HAL_DMA_GET_FLAG",
            "HAL_FLASH_Program", "HAL_FLASHEx_Erase", "HAL_CAN_Init");

    /** 需要有系列专属示例的接口。 */
    private static final List<String> EXAMPLE_APIS = List.of(
            "HAL_RCC_OscConfig", "HAL_RCC_ClockConfig", "HAL_GPIO_Init",
            "HAL_UART_MspInit", "HAL_SPI_MspInit", "HAL_I2C_MspInit",
            "HAL_ADC_Init", "HAL_DMA_Init", "HAL_FLASH_Program", "HAL_FLASHEx_Erase");

    /** 支持 FDCAN 的系列（C0 仅 C092 型号有 FDCAN1，按系列粒度统一显示）。 */
    private static final List<String> FDCAN_SERIES = List.of("c0", "g0", "g4", "h5", "h7", "l5", "u5");

    /** 支持 bxCAN 的系列。 */
    private static final List<String> CAN_SERIES = List.of("f1", "f4", "l4");

    private Main() {
    }

    public static void main(String[] args) {
        if (args.length > 0 && "--selftest".equals(args[0])) {
            System.exit(selftest());
        }
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // 回退到默认外观
        }
        SwingUtilities.invokeLater(() -> {
            try {
                new HalFrame(HalFrame.loadData()).setVisible(true);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "数据加载失败：" + e.getMessage(),
                        "HAL 速查", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }

    /** 无界面自检：验证数据完整性、系列过滤、章节覆盖与搜索。 */
    static int selftest() {
        try {
            Model.Data data = HalFrame.loadData();
            List<String> errors = new ArrayList<>();

            // ---------- 系列 ----------
            check(errors, data.series().size() == EXPECTED_SERIES.size(),
                    "系列数应为 " + EXPECTED_SERIES.size() + "，实际 " + data.series().size());
            for (String code : EXPECTED_SERIES) {
                Model.Series s = data.series(code);
                if (s == null) {
                    errors.add("缺少系列：" + code);
                    continue;
                }
                check(errors, !s.label.isBlank() && !s.family.isBlank() && !s.headerPrefix.isBlank(),
                        "系列元数据不完整：" + code);
                check(errors, !s.core.isBlank() && s.maxFreq > 0, "系列内核/主频缺失：" + code);
                check(errors, !s.brief.isBlank() && !s.note.isBlank(), "系列简介缺失：" + code);
            }
            check(errors, data.defaultSeries() != null, "缺少默认系列");

            // ---------- 章节 ----------
            check(errors, data.categories.size() == EXPECTED_CHAPTERS,
                    "章节数应为 " + EXPECTED_CHAPTERS + "，实际 " + data.categories.size());
            for (Model.Category c : data.categories) {
                check(errors, !c.demoTitle.isBlank() || !c.demo.isBlank(), "章节缺少示例标题/代码：" + c.id);
                check(errors, !c.guide.isEmpty() && !c.cubemx.isEmpty() && !c.demo.isBlank(),
                        "章节缺少使用步骤/CubeMX 配置/示例：" + c.id);
            }

            // ---------- 每个系列 × 可用章节的解析文本 ----------
            for (Model.Series s : data.series()) {
                for (Model.Category c : data.categories(s)) {
                    if (c.guideFor(s).isEmpty() || c.cubemxFor(s).isEmpty()
                            || c.demoFor(s).isBlank() || c.demoTitleFor(s).isBlank()
                            || c.noteFor(s).isBlank()) {
                        errors.add("章节 " + c.id + " 在系列 " + s.code + " 下文本不完整");
                    }
                }
            }

            // ---------- 头文件按系列替换 ----------
            for (Model.Series s : data.series()) {
                for (Model.Api a : data.apis) {
                    String h = a.headerFor(s);
                    if (h.contains("stm32xx")) {
                        errors.add("头文件未按系列替换：" + a.id + " @ " + s.code);
                    }
                    String expectPrefix = "stm32" + s.code + "xx";
                    if (!a.header.isEmpty() && a.header.contains("stm32xx") && !h.startsWith(expectPrefix)) {
                        errors.add("头文件前缀错误：" + a.id + " @ " + s.code + " -> " + h);
                    }
                }
            }

            // ---------- 系列差异与示例覆盖 ----------
            for (String id : DIFF_APIS) {
                Model.Api a = data.byId.get(id);
                if (a == null) {
                    errors.add("缺少接口：" + id);
                    continue;
                }
                for (Model.Series s : data.series()) {
                    if (!a.inSeries(s)) {
                        continue;
                    }
                    if (a.diffFor(s).isBlank()) {
                        errors.add("缺少系列差异：" + id + " @ " + s.code);
                    }
                }
            }
            for (String id : EXAMPLE_APIS) {
                Model.Api a = data.byId.get(id);
                if (a == null) {
                    errors.add("缺少接口：" + id);
                    continue;
                }
                for (Model.Series s : data.series()) {
                    if (a.inSeries(s) && (a.exampleFor(s) == null || a.exampleFor(s).isBlank())) {
                        errors.add("缺少系列示例：" + id + " @ " + s.code);
                    }
                }
            }

            // ---------- AFIO 只属于 F1 ----------
            Model.Series f1 = data.series("f1");
            Model.Series f4 = data.series("f4");
            check(errors, data.search("afio", f4).isEmpty(), "F4 不应搜到 AFIO 专属接口");
            check(errors, f1 != null && data.search("afio", f1).size() == 4, "F1 应搜到 4 个 AFIO 接口");
            check(errors, data.search("afio", data.series("c0")).isEmpty(), "C0 不应搜到 AFIO 接口");

            // ---------- CAN / FDCAN 章节可用性 ----------
            Model.Category can = data.categoryOf("can");
            Model.Category fdcan = data.categoryOf("fdcan");
            check(errors, can != null, "缺少 CAN 章节");
            check(errors, fdcan != null, "缺少 FDCAN 章节");
            for (Model.Series s : data.series()) {
                boolean expectCan = CAN_SERIES.contains(s.code);
                boolean expectFdcan = FDCAN_SERIES.contains(s.code);
                if (can != null && can.inSeries(s) != expectCan) {
                    errors.add("CAN 章节在系列 " + s.code + " 的可用性错误");
                }
                if (fdcan != null && fdcan.inSeries(s) != expectFdcan) {
                    errors.add("FDCAN 章节在系列 " + s.code + " 的可用性错误");
                }
                check(errors, !data.search("gpio", s).isEmpty(), "系列 " + s.code + " 应能搜到 GPIO 接口");
            }
            check(errors, data.search("fdcan", f1).isEmpty(), "F1 不应搜到 FDCAN 接口");
            check(errors, !data.search("fdcan", data.series("g4")).isEmpty(), "G4 应能搜到 FDCAN 接口");
            check(errors, !data.search("can", f1).isEmpty(), "F1 应能搜到 CAN 接口");

            // ---------- 章节示例（CubeMX 风格） ----------
            Model.Category i2c = data.categoryOf("i2c");
            if (i2c != null) {
                for (String code : List.of("f4", "c0", "g4")) {
                    String demo = i2c.demoFor(data.series(code));
                    check(errors, demo.contains("USER CODE") && demo.contains("MX_I2C1_Init"),
                            "I2C 示例应基于 CubeMX 生成代码 @" + code);
                }
            }

            // ---------- 头文件名精确校验 ----------
            Model.Api gpio = data.byId.get("HAL_GPIO_Init");
            check(errors, gpio != null, "缺少 HAL_GPIO_Init");
            if (gpio != null) {
                check(errors, gpio.headerFor(f1).equals("stm32f1xx_hal_gpio.h"),
                        "F1 头文件名替换错误：" + gpio.headerFor(f1));
                check(errors, gpio.headerFor(f4).equals("stm32f4xx_hal_gpio.h"),
                        "F4 头文件名替换错误：" + gpio.headerFor(f4));
                check(errors, gpio.headerFor(data.series("c0")).equals("stm32c0xx_hal_gpio.h"),
                        "C0 头文件名替换错误：" + gpio.headerFor(data.series("c0")));
            }

            // ---------- 总数 ----------
            check(errors, data.apis.size() == EXPECTED_APIS,
                    "接口总数应为 " + EXPECTED_APIS + "，实际 " + data.apis.size());
            check(errors, data.version.startsWith("2."), "数据版本应为 2.x，实际 " + data.version);

            List<String> codes = new ArrayList<>();
            for (Model.Series s : data.series()) {
                codes.add(s.code);
            }
            check(errors, codes.equals(EXPECTED_SERIES), "系列顺序/内容应为 " + EXPECTED_SERIES + "，实际 " + codes);

            System.out.println("版本        : " + data.version + " (" + data.updated + ")");
            System.out.println("系列        : " + data.series().size() + " 个（" + String.join(" / ", codes) + "）");
            System.out.println("章节        : " + data.categories.size());
            System.out.println("接口总数    : " + data.apis.size());
            for (Model.Series s : data.series()) {
                System.out.println("  " + s.label + "\t可见 " + data.countIn(s) + " 个接口，"
                        + data.categories(s).size() + " 个章节");
            }

            if (errors.isEmpty()) {
                System.out.println("SELFTEST OK");
                return 0;
            }
            for (String e : errors) {
                System.out.println("FAIL: " + e);
            }
            return 1;
        } catch (Exception e) {
            System.out.println("SELFTEST ERROR: " + e);
            e.printStackTrace();
            return 1;
        }
    }

    private static void check(List<String> errors, boolean ok, String message) {
        if (!ok) {
            errors.add(message);
        }
    }
}
