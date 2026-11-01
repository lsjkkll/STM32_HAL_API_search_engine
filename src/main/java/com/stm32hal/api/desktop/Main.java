package com.stm32hal.api.desktop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** 桌面端入口。 */
public final class Main {

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

    /** 无界面自检：验证数据完整性、系列过滤与搜索。 */
    static int selftest() {
        try {
            Model.Data data = HalFrame.loadData();
            List<String> errors = new ArrayList<>();

            check(errors, data.apis.size() == 186, "接口总数应为 186，实际 " + data.apis.size());
            int diffs = 0;
            int f1Examples = 0;
            int f1Only = 0;
            for (Model.Api a : data.apis) {
                if (!a.diff.isEmpty()) {
                    diffs++;
                }
                if (!a.exampleF1.isBlank()) {
                    f1Examples++;
                }
                if ("f1".equals(a.series)) {
                    f1Only++;
                }
            }
            check(errors, diffs == 16, "系列差异应为 16 条，实际 " + diffs);
            check(errors, f1Examples == 10, "F1 专属示例应为 10 条，实际 " + f1Examples);
            check(errors, f1Only == 4, "F1 专属接口应为 4 个，实际 " + f1Only);

            int f1Count = data.visible(Model.Series.F1).size();
            int f4Count = data.visible(Model.Series.F4).size();
            check(errors, f1Count > f4Count, "F1 可见接口数应大于 F4");

            check(errors, data.search("afio", Model.Series.F4).isEmpty(), "F4 不应搜到 AFIO 专属接口");
            check(errors, data.search("afio", Model.Series.F1).size() == 4, "F1 应搜到 4 个 AFIO 接口");
            check(errors, !data.search("gpio", Model.Series.F4).isEmpty(), "F4 应能搜到 GPIO 接口");

            int chapters = 0;
            for (Model.Category c : data.categories) {
                if (c.cubemx.isEmpty() || c.guide.isEmpty() || c.demo.isBlank()) {
                    errors.add("章节缺少 CubeMX 配置、使用步骤或示例：" + c.id);
                    continue;
                }
                chapters++;
            }
            check(errors, chapters == 15, "应有 15 个章节示例，实际 " + chapters);
            Model.Category i2c = data.categoryOf("i2c");
            if (i2c != null) {
                check(errors, i2c.cubemx.size() >= 3, "I2C 缺少 CubeMX 配置步骤");
                check(errors, i2c.demo.contains("USER CODE") && i2c.demo.contains("MX_I2C1_Init"),
                        "I2C 示例应基于 CubeMX 生成代码");
            }

            Model.Api gpio = data.byId.get("HAL_GPIO_Init");
            check(errors, gpio != null, "缺少 HAL_GPIO_Init");
            if (gpio != null) {
                check(errors, gpio.headerFor(Model.Series.F1).equals("stm32f1xx_hal_gpio.h"),
                        "F1 头文件名替换错误：" + gpio.headerFor(Model.Series.F1));
                check(errors, gpio.headerFor(Model.Series.F4).equals("stm32f4xx_hal_gpio.h"),
                        "F4 头文件名替换错误：" + gpio.headerFor(Model.Series.F4));
            }

            System.out.println("版本        : " + data.version + " (" + data.updated + ")");
            System.out.println("分类        : " + data.categories.size());
            System.out.println("接口总数    : " + data.apis.size());
            System.out.println("章节示例    : " + chapters + " 章（含 CubeMX 配置）");
            System.out.println("可见接口    : F1=" + f1Count + " / F4=" + f4Count);
            System.out.println("系列差异    : " + diffs + " 条");
            System.out.println("F1 示例     : " + f1Examples + " 条");
            System.out.println("F1 专属接口 : " + f1Only + " 个");

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
