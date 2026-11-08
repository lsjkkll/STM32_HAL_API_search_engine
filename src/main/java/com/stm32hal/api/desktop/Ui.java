package com.stm32hal.api.desktop;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/** 主题色与常用控件。 */
public final class Ui {

    public static final Color INK = new Color(0x0B1220);
    public static final Color SURFACE1 = new Color(0x121A29);
    public static final Color SURFACE2 = new Color(0x182437);
    public static final Color STROKE = new Color(0x22304A);
    public static final Color TEXT = new Color(0xE8EEF9);
    public static final Color MUTED = new Color(0x8DA0BC);
    public static final Color ACCENT = new Color(0x2F81F7);
    public static final Color CYAN = new Color(0x22D3EE);
    public static final Color GOOD = new Color(0x34D399);
    public static final Color WARN = new Color(0xFBBF24);

    public static final Font SANS = resolveUiFont();
    public static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    /** 按屏幕逻辑分辨率放大字号，2K（2560）屏幕约 1.4 倍。 */
    public static final float SCALE = detectScale();

    private Ui() {
    }

    private static float detectScale() {
        try {
            int width = Toolkit.getDefaultToolkit().getScreenSize().width;
            if (width >= 3000) {
                return 1.6f;
            }
            if (width >= 2300) {
                return 1.4f;
            }
            if (width >= 1800) {
                return 1.15f;
            }
        } catch (HeadlessException ignored) {
            // 无界面环境（自检）使用默认比例
        }
        return 1.0f;
    }

    /**
     * 界面字体：优先黑体（SimHei）。中文 Windows 会把字体家族名本地化成“黑体”“微软雅黑”，
     * 直接按英文名查表会查不到而悄悄回退到偏细的逻辑字体，所以这里既查英文名，
     * 也校验 new Font 实际解析到的家族名，最后才退到逻辑无衬线字体。
     */
    private static Font resolveUiFont() {
        String[] candidates = {
                "SimHei", "黑体",
                "DengXian", "等线",
                "Microsoft YaHei", "微软雅黑",
                "PingFang SC", "Hiragino Sans GB",
                "Noto Sans CJK SC", "Source Han Sans SC", "WenQuanYi Micro Hei"
        };
        java.util.Set<String> available = availableFamilies();
        for (String name : candidates) {
            if (available.contains(name)) {
                return new Font(name, Font.PLAIN, 13);
            }
            Font probe = new Font(name, Font.PLAIN, 13);
            // 名字不存在时 Java 会回退到 Dialog；能解析成别的家族说明系统里确实有这个字体。
            if (!Font.DIALOG.equals(probe.getFamily())) {
                return probe;
            }
        }
        Font loaded = loadUiFontFile();
        if (loaded != null) {
            return loaded;
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    }

    private static java.util.Set<String> availableFamilies() {
        try {
            return new java.util.HashSet<>(java.util.Arrays.asList(
                    java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        } catch (Exception ignored) {
            // 字体枚举失败时返回空集合，交给后续的名称探测与文件加载
            return java.util.Set.of();
        }
    }

    /** 名称探测全部失败时，直接读系统中文字体文件（Windows 自带黑体）。 */
    private static Font loadUiFontFile() {
        String windir = System.getenv("WINDIR");
        String[] files = {
                windir == null ? "" : windir + "\\Fonts\\simhei.ttf",
                windir == null ? "" : windir + "\\Fonts\\Deng.ttf",
                "/System/Library/Fonts/PingFang.ttc"
        };
        for (String path : files) {
            if (path.isEmpty()) {
                continue;
            }
            try {
                java.io.File file = new java.io.File(path);
                if (!file.isFile()) {
                    continue;
                }
                return Font.createFont(Font.TRUETYPE_FONT, file).deriveFont(Font.PLAIN, 13f);
            } catch (Exception ignored) {
                // 当前文件不可用时继续尝试下一个
            }
        }
        return null;
    }

    /** 界面文字统一加粗（黑体无粗体字面，由 Java 合成加粗），并按屏幕比例放大。 */
    public static Font sans(float size, int style) {
        int weight = (style == Font.PLAIN) ? Font.BOLD : style;
        return SANS.deriveFont(weight, size * SCALE);
    }

    /** 等宽字体用于代码与函数名，字号同样按屏幕比例放大。 */
    public static Font mono(float size, int style) {
        return MONO.deriveFont(style, size * SCALE);
    }

    public static Color alpha(Color c, float a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.round(255 * a));
    }

    public static Color blend(Color base, Color overlay, float ratio) {
        int r = Math.round(base.getRed() * (1 - ratio) + overlay.getRed() * ratio);
        int g = Math.round(base.getGreen() * (1 - ratio) + overlay.getGreen() * ratio);
        int b = Math.round(base.getBlue() * (1 - ratio) + overlay.getBlue() * ratio);
        return new Color(r, g, b);
    }

    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        l.setAlignmentX(0f);
        l.setHorizontalAlignment(JLabel.LEFT);
        return l;
    }

    /** 圆角卡片，负责绘制背景与描边。 */
    public static class RoundPanel extends JPanel {
        private Color fill;
        private Color stroke;
        private int arc;

        public RoundPanel(Color fill, Color stroke, int arc) {
            this.fill = fill;
            this.stroke = stroke;
            this.arc = arc;
            setOpaque(false);
        }

        public void setFill(Color fill) {
            this.fill = fill;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (fill != null) {
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            }
            if (stroke != null) {
                g2.setColor(stroke);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** 圆角胶囊按钮，active 时使用渐变强调色。 */
    public static class PillButton extends JButton {
        private boolean active;
        private int arc = 12;
        private boolean small;

        public PillButton(String text, boolean monoFont) {
            super(text);
            setFont(monoFont ? mono(11.5f, Font.BOLD) : sans(11.5f, Font.BOLD));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setForeground(MUTED);
            setMargin(new Insets(4, 8, 4, 8));
        }

        public void setSmall(boolean small) {
            this.small = small;
            setFont(getFont().deriveFont((small ? 10.5f : 11.5f) * SCALE));
        }

        public void setArc(int arc) {
            this.arc = arc;
        }

        public void setActive(boolean active) {
            this.active = active;
            setForeground(active ? INK : MUTED);
            repaint();
        }

        public boolean isActive() {
            return active;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - 1;
            int h = getHeight() - 1;
            if (active) {
                g2.setPaint(new GradientPaint(0, 0, ACCENT, w, 0, CYAN));
                g2.fillRoundRect(0, 0, w, h, arc, arc);
            } else {
                g2.setColor(SURFACE2);
                g2.fillRoundRect(0, 0, w, h, arc, arc);
                g2.setColor(STROKE);
                g2.drawRoundRect(0, 0, w, h, arc, arc);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** 文字强调按钮（用于“复制”）。 */
    public static class GhostButton extends JButton {
        public GhostButton(String text) {
            super(text);
            setFont(mono(10.5f, Font.BOLD));
            setForeground(CYAN);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMargin(new Insets(2, 6, 2, 6));
        }
    }

    /** 可随可用宽度自动换行的多行文本。 */
    public static class WrapArea extends JTextArea {
        private int fixedWidth = 320;

        public WrapArea(String text, Font font, Color color, boolean wrapWords) {
            super(text);
            setFont(font);
            setForeground(color);
            setOpaque(false);
            setEditable(false);
            setFocusable(false);
            setLineWrap(true);
            setWrapStyleWord(wrapWords);
            setBorder(BorderFactory.createEmptyBorder());
        }

        /** 由父容器在宽度变化时调用。 */
        public void setFixedWidth(int width) {
            if (width > 0 && width != fixedWidth) {
                fixedWidth = width;
                revalidate();
                repaint();
            }
        }

        public int getFixedWidth() {
            return fixedWidth;
        }

        @Override
        public Dimension getPreferredSize() {
            setSize(fixedWidth, Integer.MAX_VALUE);
            Dimension d = super.getPreferredSize();
            return new Dimension(fixedWidth, d.height);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }

    /** 自绘占位符的搜索框。 */
    public static class SearchField extends JTextField {
        private final String placeholder;

        public SearchField(String placeholder) {
            this.placeholder = placeholder;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder());
            setFont(sans(13f, Font.PLAIN));
            setForeground(TEXT);
            setCaretColor(CYAN);
            setSelectionColor(alpha(ACCENT, 0.4f));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(MUTED);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(placeholder, 0, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        }
    }

    public static JPanel vbox() {
        JPanel p = new JPanel();
        p.setLayout(new javax.swing.BoxLayout(p, javax.swing.BoxLayout.Y_AXIS));
        p.setOpaque(false);
        return p;
    }

    public static JPanel flow() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        return p;
    }

    public static JPanel hbox() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        return p;
    }

    public static void fullWidth(JComponent c) {
        c.setAlignmentX(0f);
        Dimension pref = c.getPreferredSize();
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));
    }

    public static javax.swing.JScrollPane scroll(java.awt.Component view) {
        javax.swing.JScrollPane sp = new javax.swing.JScrollPane(view);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(INK);
        sp.setBackground(INK);
        sp.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.getVerticalScrollBar().setUnitIncrement(18);
        sp.getHorizontalScrollBar().setUnitIncrement(18);
        return sp;
    }

    /** 让控件在鼠标悬停时给出可点击提示。 */
    public static void hoverBright(JComponent c) {
        c.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        });
    }
}
