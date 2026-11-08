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

    /** 代码面板配色对齐 CLion（新版 Dark / Darcula 主题）。 */
    public static final Color CODE_BG = new Color(0x1E1F22);
    public static final Color CODE_LINE = new Color(0x2B2D30);
    public static final Color CODE_TEXT = new Color(0xBFCADB);
    public static final Color CODE_COMMENT = new Color(0x7A7E85);
    public static final Color CODE_KEYWORD = new Color(0xCF8E6D);
    public static final Color CODE_STRING = new Color(0x6AAB73);
    public static final Color CODE_NUMBER = new Color(0x2AACB8);
    public static final Color CODE_FUNC = new Color(0x56A8F5);
    public static final Color CODE_PREPROC = new Color(0xC77DBB);
    public static final Color CODE_GUTTER = new Color(0x606366);

    public static final Font SANS = resolveUiFont();
    public static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    /** 代码用等宽字体：优先系统里的编程字体（Consolas / Cascadia Mono 等）。 */
    public static final Font CODE = resolveCodeFont();

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
        Font loaded = loadFontFile(13f, new String[]{"simhei.ttf", "Deng.ttf"},
                "/System/Library/Fonts/PingFang.ttc");
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

    /** 名称探测全部失败时，直接读系统里的字体文件兜底。 */
    private static Font loadFontFile(float size, String[] windowsNames, String... otherPaths) {
        String windir = System.getenv("WINDIR");
        java.util.List<String> paths = new java.util.ArrayList<>();
        if (windir != null) {
            for (String name : windowsNames) {
                paths.add(windir + "\\Fonts\\" + name);
            }
        }
        paths.addAll(java.util.Arrays.asList(otherPaths));
        for (String path : paths) {
            try {
                java.io.File file = new java.io.File(path);
                if (!file.isFile()) {
                    continue;
                }
                return Font.createFont(Font.TRUETYPE_FONT, file).deriveFont(Font.PLAIN, size);
            } catch (Exception ignored) {
                // 当前文件不可用时继续尝试下一个
            }
        }
        return null;
    }

    /** 代码字体：优先 Consolas / Cascadia Mono 这类编程等宽字体，缺中文字形时由 CodeArea 交给黑体。 */
    private static Font resolveCodeFont() {
        String[] candidates = {
                "Consolas", "Cascadia Mono", "Cascadia Code", "JetBrains Mono",
                "Fira Code", "Source Code Pro", "DejaVu Sans Mono", "Menlo"
        };
        java.util.Set<String> available = availableFamilies();
        for (String name : candidates) {
            if (available.contains(name)) {
                return new Font(name, Font.PLAIN, 12);
            }
            Font probe = new Font(name, Font.PLAIN, 12);
            if (!Font.DIALOG.equals(probe.getFamily())) {
                return probe;
            }
        }
        Font loaded = loadFontFile(12f, new String[]{"consola.ttf", "CascadiaMono.ttf"},
                "/System/Library/Fonts/Menlo.ttc");
        return loaded != null ? loaded : new Font(Font.MONOSPACED, Font.PLAIN, 12);
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

    /** 由父容器按可用宽度布局的文本块。 */
    public interface Sizable {
        void setFixedWidth(int width);
    }

    /** 可随可用宽度自动换行的多行文本。 */
    public static class WrapArea extends JTextArea implements Sizable {
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

    /**
     * 代码块：CLion 风格的深色编辑器面板——左侧行号栏 + CLion Dark 语法高亮，
     * 英文/符号用编程等宽字体（Consolas 等），中文自动切黑体，逐字挑字形，避免方块。
     */
    public static class CodeArea extends javax.swing.JTextPane implements Sizable {
        private static final float TEXT_SIZE = 12.5f;
        private static final int T_PLAIN = 0;
        private static final int T_COMMENT = 1;
        private static final int T_KEYWORD = 2;
        private static final int T_STRING = 3;
        private static final int T_NUMBER = 4;
        private static final int T_FUNC = 5;
        private static final int T_PREPROC = 6;
        private static final java.util.Set<String> KEYWORDS = java.util.Set.of(
                "auto", "break", "case", "char", "const", "continue", "default", "do", "double", "else",
                "enum", "extern", "float", "for", "goto", "if", "inline", "int", "long", "register",
                "restrict", "return", "short", "signed", "sizeof", "static", "struct", "switch",
                "typedef", "union", "unsigned", "void", "volatile", "while", "bool", "true", "false");
        private final java.util.Map<String, javax.swing.text.SimpleAttributeSet> attrCache =
                new java.util.HashMap<>();
        private int fixedWidth = 320;
        private int gutterWidth;
        private Font gutterFont;
        private Font codeFont;

        public CodeArea(String text) {
            setOpaque(true);
            setBackground(CODE_BG);
            setEditable(false);
            setFocusable(false);
            setBorder(BorderFactory.createEmptyBorder(8, 0, 10, 10));
            setSelectionColor(alpha(ACCENT, 0.40f));
            setSelectedTextColor(TEXT);
            Font latinFont = CODE.deriveFont(Font.PLAIN, TEXT_SIZE * SCALE);
            Font cjkFont = SANS.deriveFont(Font.PLAIN, TEXT_SIZE * SCALE);
            Font fallbackFont = MONO.deriveFont(Font.PLAIN, TEXT_SIZE * SCALE);
            codeFont = latinFont;
            int[] classes = classify(text);
            javax.swing.text.StyledDocument doc = getStyledDocument();
            StringBuilder run = new StringBuilder();
            javax.swing.text.SimpleAttributeSet runAttrs = null;
            try {
                for (int i = 0; i < text.length(); i++) {
                    char ch = text.charAt(i);
                    Font font = latinFont.canDisplay(ch) ? latinFont
                            : (cjkFont.canDisplay(ch) ? cjkFont : fallbackFont);
                    javax.swing.text.SimpleAttributeSet attrs = attribute(font, colorOf(classes[i]));
                    if (runAttrs != null && attrs != runAttrs) {
                        doc.insertString(doc.getLength(), run.toString(), runAttrs);
                        run.setLength(0);
                    }
                    runAttrs = attrs;
                    run.append(ch);
                }
                if (run.length() > 0) {
                    doc.insertString(doc.getLength(), run.toString(), runAttrs);
                }
            } catch (javax.swing.text.BadLocationException ignored) {
                // 追加到文档末尾不会越界
            }
            int lines = 1;
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) == '\n') {
                    lines++;
                }
            }
            gutterFont = CODE.deriveFont(Font.PLAIN, Math.max(9f, TEXT_SIZE * SCALE - 1f));
            gutterWidth = getFontMetrics(gutterFont).stringWidth(String.valueOf(lines)) + (int) (16 * SCALE);
            // margin 在部分 LAF 下不生效，改用段落左缩进给行号栏留位置
            javax.swing.text.SimpleAttributeSet para = new javax.swing.text.SimpleAttributeSet();
            javax.swing.text.StyleConstants.setLeftIndent(para, gutterWidth);
            doc.setParagraphAttributes(0, doc.getLength(), para, false);
        }

        private javax.swing.text.SimpleAttributeSet attribute(Font font, Color color) {
            String key = font.getFamily() + "|" + font.getSize() + "|" + color.getRGB();
            javax.swing.text.SimpleAttributeSet cached = attrCache.get(key);
            if (cached != null) {
                return cached;
            }
            javax.swing.text.SimpleAttributeSet a = new javax.swing.text.SimpleAttributeSet();
            javax.swing.text.StyleConstants.setFontFamily(a, font.getFamily());
            javax.swing.text.StyleConstants.setFontSize(a, font.getSize());
            javax.swing.text.StyleConstants.setForeground(a, color);
            attrCache.put(key, a);
            return a;
        }

        private static Color colorOf(int token) {
            switch (token) {
                case T_COMMENT:
                    return CODE_COMMENT;
                case T_KEYWORD:
                    return CODE_KEYWORD;
                case T_STRING:
                    return CODE_STRING;
                case T_NUMBER:
                    return CODE_NUMBER;
                case T_FUNC:
                    return CODE_FUNC;
                case T_PREPROC:
                    return CODE_PREPROC;
                default:
                    return CODE_TEXT;
            }
        }

        /** 简单 C 词法扫描：注释、字符串、数字、关键字、函数调用、预处理指令。 */
        private static int[] classify(String text) {
            int n = text.length();
            int[] classes = new int[n];
            int i = 0;
            boolean lineStart = true;
            while (i < n) {
                char c = text.charAt(i);
                if (c == '\n') {
                    lineStart = true;
                    i++;
                    continue;
                }
                if (c == ' ' || c == '\t' || c == '\r') {
                    i++;
                    continue;
                }
                if (lineStart && c == '#') {
                    int j = i + 1;
                    while (j < n && (Character.isLetterOrDigit(text.charAt(j)) || text.charAt(j) == '_')) {
                        j++;
                    }
                    fill(classes, i, j, T_PREPROC);
                    i = j;
                    lineStart = false;
                    continue;
                }
                lineStart = false;
                if (c == '/' && i + 1 < n && text.charAt(i + 1) == '/') {
                    int j = text.indexOf('\n', i);
                    j = j < 0 ? n : j;
                    fill(classes, i, j, T_COMMENT);
                    i = j;
                    continue;
                }
                if (c == '/' && i + 1 < n && text.charAt(i + 1) == '*') {
                    int j = text.indexOf("*/", i + 2);
                    j = j < 0 ? n : j + 2;
                    fill(classes, i, j, T_COMMENT);
                    i = j;
                    continue;
                }
                if (c == '"' || c == '\'') {
                    int j = i + 1;
                    while (j < n) {
                        char d = text.charAt(j);
                        if (d == '\\') {
                            j += 2;
                            continue;
                        }
                        if (d == c || d == '\n') {
                            j++;
                            break;
                        }
                        j++;
                    }
                    fill(classes, i, Math.min(j, n), T_STRING);
                    i = Math.min(j, n);
                    continue;
                }
                if (Character.isDigit(c)) {
                    int j = i + 1;
                    while (j < n) {
                        char d = text.charAt(j);
                        if (Character.isLetterOrDigit(d) || d == '.') {
                            j++;
                        } else {
                            break;
                        }
                    }
                    fill(classes, i, j, T_NUMBER);
                    i = j;
                    continue;
                }
                if (Character.isLetter(c) || c == '_') {
                    int j = i + 1;
                    while (j < n && (Character.isLetterOrDigit(text.charAt(j)) || text.charAt(j) == '_')) {
                        j++;
                    }
                    int cls = KEYWORDS.contains(text.substring(i, j)) ? T_KEYWORD : T_PLAIN;
                    if (cls == T_PLAIN) {
                        int k = j;
                        while (k < n && (text.charAt(k) == ' ' || text.charAt(k) == '\t')) {
                            k++;
                        }
                        if (k < n && text.charAt(k) == '(') {
                            cls = T_FUNC;
                        }
                    }
                    fill(classes, i, j, cls);
                    i = j;
                    continue;
                }
                i++;
            }
            return classes;
        }

        private static void fill(int[] classes, int from, int to, int cls) {
            for (int k = Math.max(0, from); k < Math.min(classes.length, to); k++) {
                classes[k] = cls;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(gutterFont);
            FontMetrics fm = g2.getFontMetrics();
            FontMetrics codeFm = getFontMetrics(codeFont);
            javax.swing.text.Element root = getStyledDocument().getDefaultRootElement();
            g2.setColor(CODE_GUTTER);
            for (int i = 0; i < root.getElementCount(); i++) {
                java.awt.geom.Rectangle2D r;
                try {
                    r = modelToView2D(root.getElement(i).getStartOffset());
                } catch (Exception ignored) {
                    continue;
                }
                if (r == null) {
                    continue;
                }
                String num = String.valueOf(i + 1);
                int x = gutterWidth - (int) (8 * SCALE) - fm.stringWidth(num);
                int y = (int) Math.round(r.getY()) + codeFm.getAscent();
                g2.drawString(num, x, y);
            }
            g2.setColor(CODE_LINE);
            int sep = gutterWidth - (int) (4 * SCALE);
            g2.drawLine(sep, 6, sep, Math.max(6, getHeight() - 8));
            g2.dispose();
        }

        /** 由父容器在宽度变化时调用。 */
        @Override
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
