package com.stm32hal.api.desktop;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.stm32hal.api.desktop.Model.Api;
import com.stm32hal.api.desktop.Model.Category;
import com.stm32hal.api.desktop.Model.Series;
import com.stm32hal.api.desktop.Ui.PillButton;
import com.stm32hal.api.desktop.Ui.RoundPanel;
import com.stm32hal.api.desktop.Ui.WrapArea;

/** 桌面端主窗口。 */
public final class HalFrame extends JFrame {

    private final Model.Data data;
    private final FavStore store;
    private final Set<String> favorites;
    private Series series;

    private String query = "";
    private String categoryId;
    private boolean commonMode;
    private boolean favMode;
    private boolean updating;
    private Api current;

    private final Ui.SearchField search = new Ui.SearchField("搜索函数 / 外设 / 关键词");
    private final PillButton clearSearch = new PillButton("✕", true);
    private final JLabel headerCount = Ui.label("", Ui.sans(11f, Font.PLAIN), Ui.MUTED);
    private final PillButton f1Button = new PillButton("F1", true);
    private final PillButton f4Button = new PillButton("F4", true);
    private final PillButton allTab = new PillButton("全部接口", false);
    private final PillButton favTab = new PillButton("我的收藏", false);
    private final JPanel chips = new JPanel();
    private final List<PillButton> chipButtons = new ArrayList<>();

    private final DefaultListModel<Api> listModel = new DefaultListModel<>();
    private final JList<Api> list = new JList<>(listModel);
    private final JPanel detail = new JPanel(new GridBagLayout());
    private final JScrollPane detailScroll = Ui.scroll(detail);
    private final List<WrapArea> wrapAreas = new ArrayList<>();
    private boolean categoryMode = false;
    private int detailRow;

    public HalFrame(Model.Data data) {
        super("HAL 速查 · STM32 HAL API 离线手册");
        this.data = data;
        this.store = new FavStore();
        this.favorites = new LinkedHashSet<>(store.loadFavorites());
        this.series = Series.fromCode(store.loadSeries());

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension((int) (940 * Ui.SCALE), (int) (620 * Ui.SCALE)));
        setSize((int) (1180 * Ui.SCALE), (int) (780 * Ui.SCALE));
        setLocationRelativeTo(null);
        setIconImage(buildIcon());
        getContentPane().setBackground(Ui.INK);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        installShortcuts();

        refreshAll();
    }

    // ---------------------------------------------------------------- 布局

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(Ui.INK);
        header.setBorder(new EmptyBorder((int) (14 * Ui.SCALE), (int) (18 * Ui.SCALE),
                (int) (10 * Ui.SCALE), (int) (18 * Ui.SCALE)));

        header.add(logo(), BorderLayout.WEST);

        JPanel titles = new JPanel(new BorderLayout());
        titles.setOpaque(false);
        titles.add(Ui.label("HAL 速查", Ui.sans(21f, Font.BOLD), Ui.TEXT), BorderLayout.NORTH);
        JPanel sub = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        sub.setOpaque(false);
        sub.add(headerCount);
        titles.add(sub, BorderLayout.CENTER);
        header.add(titles, BorderLayout.CENTER);

        RoundPanel toggle = new RoundPanel(Ui.SURFACE1, Ui.STROKE, 12);
        toggle.setLayout(new FlowLayout(FlowLayout.LEFT, 3, 3));
        f1Button.setArc(8);
        f4Button.setArc(8);
        f1Button.addActionListener(e -> switchSeries(Series.F1));
        f4Button.addActionListener(e -> switchSeries(Series.F4));
        toggle.add(f1Button);
        toggle.add(f4Button);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(toggle);
        header.add(right, BorderLayout.EAST);
        updateSeriesButtons();
        return header;
    }

    private JComponent logo() {
        JPanel logo = new JPanel() {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new java.awt.GradientPaint(0, 0, Ui.ACCENT, getWidth(), getHeight(), Ui.CYAN));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(Ui.INK);
                g2.setFont(Ui.mono(13f, Font.BOLD));
                java.awt.FontMetrics fm = g2.getFontMetrics();
                String s = "HAL";
                int x = (getWidth() - fm.stringWidth(s)) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(s, x, y);
                g2.dispose();
            }
        };
        logo.setOpaque(false);
        logo.setPreferredSize(new Dimension((int) (46 * Ui.SCALE), (int) (46 * Ui.SCALE)));
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(0, 0, 0, 12));
        wrap.add(logo, BorderLayout.CENTER);
        return wrap;
    }

    private JComponent buildBody() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildLeftPane(), buildDetailPane());
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setDividerSize(1);
        split.setBackground(Ui.STROKE);
        split.setContinuousLayout(true);
        split.setResizeWeight(0);
        split.setDividerLocation((int) (392 * Ui.SCALE));
        return split;
    }

    private JComponent buildLeftPane() {
        JPanel left = new JPanel(new BorderLayout(0, 10));
        left.setBackground(Ui.INK);
        left.setBorder(new EmptyBorder((int) (2 * Ui.SCALE), (int) (18 * Ui.SCALE),
                (int) (12 * Ui.SCALE), (int) (10 * Ui.SCALE)));
        left.setMinimumSize(new Dimension((int) (330 * Ui.SCALE), 0));

        JPanel top = Ui.vbox();

        RoundPanel searchCard = new RoundPanel(Ui.SURFACE1, Ui.STROKE, 12);
        searchCard.setLayout(new BorderLayout(8, 0));
        searchCard.setBorder(new EmptyBorder((int) (7 * Ui.SCALE), (int) (12 * Ui.SCALE),
                (int) (7 * Ui.SCALE), (int) (6 * Ui.SCALE)));
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onQueryChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onQueryChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onQueryChanged();
            }
        });
        searchCard.add(search, BorderLayout.CENTER);
        clearSearch.setForeground(Ui.MUTED);
        clearSearch.setVisible(false);
        clearSearch.addActionListener(e -> {
            search.setText("");
            search.requestFocusInWindow();
        });
        searchCard.add(clearSearch, BorderLayout.EAST);
        Ui.fullWidth(searchCard);
        top.add(searchCard);
        top.add(javax.swing.Box.createVerticalStrut(10));

        RoundPanel tabs = new RoundPanel(Ui.SURFACE1, Ui.STROKE, 12);
        tabs.setLayout(new GridLayout(1, 2, 4, 0));
        tabs.setBorder(new EmptyBorder((int) (4 * Ui.SCALE), (int) (4 * Ui.SCALE),
                (int) (4 * Ui.SCALE), (int) (4 * Ui.SCALE)));
        allTab.setArc(9);
        favTab.setArc(9);
        allTab.addActionListener(e -> {
            favMode = false;
            categoryMode = false;
            refreshList();
        });
        favTab.addActionListener(e -> {
            favMode = true;
            categoryMode = false;
            refreshList();
        });
        tabs.add(allTab);
        tabs.add(favTab);
        Ui.fullWidth(tabs);
        top.add(tabs);
        top.add(javax.swing.Box.createVerticalStrut(10));

        chips.setLayout(new BorderLayout(0, 6));
        chips.setOpaque(false);
        top.add(chips);

        left.add(top, BorderLayout.NORTH);

        list.setBackground(Ui.INK);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight((int) (56 * Ui.SCALE));
        list.setCellRenderer(new ApiCell());
        list.setBorder(new EmptyBorder(4, 0, 0, 0));
        list.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || updating) {
                return;
            }
            Api selected = list.getSelectedValue();
            if (selected != null) {
                categoryMode = false;
                showDetail(selected);
            }
        });
        left.add(Ui.scroll(list), BorderLayout.CENTER);
        return left;
    }

    private JComponent buildDetailPane() {
        detail.setBackground(Ui.INK);
        detail.setBorder(new EmptyBorder((int) (4 * Ui.SCALE), (int) (22 * Ui.SCALE),
                (int) (26 * Ui.SCALE), (int) (22 * Ui.SCALE)));
        detailScroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                relayoutWraps();
            }
        });
        return detailScroll;
    }

    private void installShortcuts() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), "focusSearch");
        getRootPane().getActionMap().put("focusSearch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                search.requestFocusInWindow();
                search.selectAll();
            }
        });
        search.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clear");
        search.getActionMap().put("clear", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                search.setText("");
            }
        });
    }

    // ---------------------------------------------------------------- 状态

    private void onQueryChanged() {
        query = search.getText();
        categoryMode = false;
        clearSearch.setVisible(!query.isEmpty());
        refreshList();
    }

    private void switchSeries(Series next) {
        if (next == series) {
            return;
        }
        series = next;
        categoryId = null;
        commonMode = false;
        categoryMode = false;
        updateSeriesButtons();
        store.save(favorites, series.code);
        refreshAll();
    }

    private void updateSeriesButtons() {
        f1Button.setActive(series == Series.F1);
        f4Button.setActive(series == Series.F4);
    }

    private void refreshAll() {
        rebuildChips();
        refreshList();
        headerCount.setText("STM32 HAL API 离线手册 · " + data.visible(series).size() + " 个接口");
    }

    private void rebuildChips() {
        chips.removeAll();
        chipButtons.clear();
        chips.setAlignmentX(0f);
        chips.add(chipLabel("按外设筛选"), BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 3, 6, 6));
        grid.setOpaque(false);
        addChip(grid, "全部", data.visible(series).size(), () -> {
            categoryId = null;
            commonMode = false;
            categoryMode = false;
            refreshList();
        });
        int common = 0;
        for (Api a : data.visible(series)) {
            if (a.common) {
                common++;
            }
        }
        addChip(grid, "常用", common, () -> {
            commonMode = true;
            categoryId = null;
            categoryMode = false;
            refreshList();
        });
        for (Category c : data.categories) {
            int n = data.countIn(series, c.id);
            if (n > 0) {
                addChip(grid, c.code, n, () -> {
                    categoryId = c.id;
                    commonMode = false;
                    categoryMode = true;
                    refreshList();
                });
            }
        }
        chips.add(grid, BorderLayout.CENTER);
        chips.setMaximumSize(new Dimension(Integer.MAX_VALUE, chips.getPreferredSize().height));
    }

    private JComponent chipLabel(String text) {
        JLabel l = Ui.label(text, Ui.sans(10.5f, Font.BOLD), Ui.MUTED);
        l.setAlignmentX(0f);
        return l;
    }

    private void addChip(JPanel grid, String label, int count, Runnable action) {
        PillButton chip = new PillButton(label + " · " + count, false);
        chip.setArc(9);
        chip.setFont(Ui.sans(10.5f, Font.BOLD));
        chip.addActionListener(e -> action.run());
        chipButtons.add(chip);
        grid.add(chip);
    }

    private void updateChipStates() {
        int i = 0;
        for (PillButton chip : chipButtons) {
            boolean active;
            if (i == 0) {
                active = categoryId == null && !commonMode;
            } else if (i == 1) {
                active = commonMode;
            } else {
                active = categoryId != null && categoryId.equals(chipCategoryAt(i));
            }
            chip.setActive(active);
            i++;
        }
    }

    private String chipCategoryAt(int index) {
        List<String> ids = new ArrayList<>();
        for (Category c : data.categories) {
            if (data.countIn(series, c.id) > 0) {
                ids.add(c.id);
            }
        }
        int pos = index - 2;
        return pos >= 0 && pos < ids.size() ? ids.get(pos) : "";
    }

    private List<Api> computeItems() {
        if (!query.isBlank()) {
            return data.search(query, series);
        }
        List<Api> items = new ArrayList<>();
        for (Api a : data.visible(series)) {
            if (favMode && !favorites.contains(a.id)) {
                continue;
            }
            if (!favMode && commonMode && !a.common) {
                continue;
            }
            if (!favMode && !commonMode && categoryId != null && !a.cat.equals(categoryId)) {
                continue;
            }
            items.add(a);
        }
        return items;
    }

    private void refreshList() {
        List<Api> items = computeItems();
        Api keep = current;
        updating = true;
        listModel.clear();
        for (Api a : items) {
            listModel.addElement(a);
        }
        updating = false;

        allTab.setText("全部接口 " + data.visible(series).size());
        int favCount = 0;
        for (Api a : data.visible(series)) {
            if (favorites.contains(a.id)) {
                favCount++;
            }
        }
        favTab.setText("我的收藏 " + favCount);
        allTab.setActive(!favMode);
        favTab.setActive(favMode);

        boolean showChips = query.isBlank() && !favMode;
        chips.setVisible(showChips);
        updateChipStates();

        if (categoryMode && categoryId != null && query.isBlank() && !favMode) {
            Category c = data.categoryOf(categoryId);
            if (c != null) {
                updating = true;
                list.clearSelection();
                updating = false;
                current = null;
                showCategory(c);
                return;
            }
        }

        if (items.isEmpty()) {
            list.clearSelection();
            current = null;
            showEmpty(!query.isBlank() ? "没有匹配的接口" : favMode ? "还没有收藏" : "该分类下暂无接口",
                    !query.isBlank()
                            ? "试试函数名片段或外设关键词，例如 GPIO_Init、DMA、PWM、空闲中断。"
                            : favMode ? "在接口详情页点右上角星标即可收藏，方便随时回查。" : "切换 F1 / F4 系列或选择其他分类。");
            return;
        }

        int index = 0;
        if (keep != null) {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).id.equals(keep.id)) {
                    index = i;
                    break;
                }
            }
        }
        list.setSelectedIndex(index);
        if (list.getSelectedValue() == null || !list.getSelectedValue().id.equals(items.get(index).id)) {
            list.setSelectedIndex(index);
        }
        showDetail(items.get(index));
        list.ensureIndexIsVisible(index);
    }

    // ---------------------------------------------------------------- 章节总览

    private void showCategory(Category c) {
        detail.removeAll();
        wrapAreas.clear();
        detailRow = 0;
        detailScroll.getVerticalScrollBar().setValue(0);

        int n = data.countIn(series, c.id);

        JPanel head = new JPanel(new BorderLayout(10, 0));
        head.setOpaque(false);
        JPanel titles = new JPanel(new BorderLayout());
        titles.setOpaque(false);
        titles.add(Ui.label(c.name + " 章节总览", Ui.sans(17f, Font.BOLD), Ui.TEXT), BorderLayout.NORTH);
        titles.add(Ui.label("STM32" + series.label + " · " + c.code + " · " + n + " 个接口",
                Ui.sans(10.5f, Font.PLAIN), Ui.MUTED), BorderLayout.CENTER);
        head.add(titles, BorderLayout.CENTER);
        addDetail(head, 0, 6);

        addDetail(wrap(c.desc + "  本节给出 CubeMX 配置步骤、代码步骤与可直接参考的完整示例；左侧列表可查看单个函数。",
                Ui.sans(13f, Font.PLAIN), Ui.TEXT, true, 0), 0, 8);

        JPanel tags = Ui.flow();
        tags.add(tag("STM32" + series.label, Ui.CYAN, true));
        tags.add(tag(c.code, Ui.ACCENT, true));
        tags.add(tag("综合示例", Ui.MUTED, false));
        addDetail(tags, 0, 8);

        if (!c.cubemx.isEmpty()) {
            addDetail(section("CubeMX 配置", c.cubemx.size() + " 步"), 8, 6);
            for (int i = 0; i < c.cubemx.size(); i++) {
                addDetail(guideRow(i + 1, c.cubemx.get(i)), 0, 7);
            }
        }

        if (!c.guide.isEmpty()) {
            addDetail(section("使用步骤", c.guide.size() + " 步"), 8, 6);
            for (int i = 0; i < c.guide.size(); i++) {
                addDetail(guideRow(i + 1, c.guide.get(i)), 0, 7);
            }
        }

        if (!c.demo.isBlank()) {
            addDetail(section("综合示例程序", "DEMO"), 8, 6);
            String title = c.demoTitle.isBlank() ? c.code + " 综合示例" : c.demoTitle;
            addDetail(codeBlock(title, c.demoFor(series)), 0, 8);
        }

        if (!c.note.isBlank()) {
            addDetail(section("接线与系列差异", "NOTE"), 6, 6);
            RoundPanel noteCard = new RoundPanel(Ui.blend(Ui.INK, Ui.ACCENT, 0.12f),
                    Ui.alpha(Ui.ACCENT, 0.35f), 12);
            noteCard.setLayout(new BorderLayout());
            noteCard.setBorder(new EmptyBorder(11, 12, 11, 12));
            noteCard.add(wrap(c.note, Ui.sans(12.5f, Font.PLAIN), Ui.alpha(Ui.TEXT, 0.92f), true, 24));
            addDetail(noteCard, 0, 8);
        }

        List<Api> inCat = new ArrayList<>();
        for (Api a : data.byCat.getOrDefault(c.id, Collections.emptyList())) {
            if (a.inSeries(series)) {
                inCat.add(a);
            }
        }
        if (!inCat.isEmpty()) {
            addDetail(section("本章接口", inCat.size() + " 个"), 6, 6);
            JPanel gridApis = new JPanel(new GridLayout(0, 2, 6, 6));
            gridApis.setOpaque(false);
            for (Api a : inCat) {
                PillButton b = new PillButton(a.name, true);
                b.setArc(8);
                b.setFont(Ui.mono(10.5f, Font.BOLD));
                b.setForeground(Ui.CYAN);
                b.setToolTipText(a.name);
                b.addActionListener(e -> openApiInCategory(a));
                gridApis.add(b);
            }
            addDetail(gridApis, 0, 10);
        }

        detail.revalidate();
        detail.repaint();
        SwingUtilities.invokeLater(() -> {
            relayoutWraps();
            detailScroll.getVerticalScrollBar().setValue(0);
            detailScroll.getHorizontalScrollBar().setValue(0);
        });
    }

    private void openApiInCategory(Api api) {
        categoryMode = false;
        refreshList();
        list.setSelectedValue(api, true);
        showDetail(api);
    }

    private JComponent guideRow(int index, String text) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        JLabel num = Ui.label(String.format("%02d", index), Ui.mono(11f, Font.BOLD), Ui.CYAN);
        num.setVerticalAlignment(JLabel.TOP);
        row.add(num, BorderLayout.WEST);
        row.add(wrap(text, Ui.sans(12.5f, Font.PLAIN), Ui.alpha(Ui.TEXT, 0.88f), true, 30),
                BorderLayout.CENTER);
        return row;
    }

    private void showEmpty(String title, String desc) {
        detail.removeAll();
        wrapAreas.clear();
        detailRow = 0;
        JPanel box = Ui.vbox();
        box.setBorder(new EmptyBorder(70, 0, 0, 0));
        JLabel t = Ui.label(title, Ui.sans(15f, Font.BOLD), Ui.TEXT);
        t.setAlignmentX(0.5f);
        box.add(t);
        JLabel d = Ui.label(desc, Ui.sans(12f, Font.PLAIN), Ui.MUTED);
        d.setAlignmentX(0.5f);
        box.add(javax.swing.Box.createVerticalStrut(8));
        box.add(d);
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        center.setOpaque(false);
        center.add(box);
        addDetail(center, 0, 0);
        detail.revalidate();
        detail.repaint();
    }

    // ---------------------------------------------------------------- 详情

    private void showDetail(Api api) {
        current = api;
        detail.removeAll();
        wrapAreas.clear();
        detailRow = 0;
        detailScroll.getVerticalScrollBar().setValue(0);

        addDetail(buildDetailHeader(api), 0, 6);
        addDetail(codeBlock("函数原型", api.sig), 0, 10);
        WrapArea brief = wrap(api.brief, Ui.sans(13.5f, Font.PLAIN), Ui.TEXT, true, 0);
        addDetail(brief, 0, 10);

        JPanel tags = Ui.flow();
        tags.add(tag("STM32" + series.label, Ui.CYAN, true));
        Category cat = data.categoryOf(api.cat);
        if (cat != null) {
            tags.add(tag(cat.name, Ui.ACCENT, false));
        }
        String header = api.headerFor(series);
        if (!header.isBlank()) {
            tags.add(tag(header, Ui.MUTED, true));
        }
        addDetail(tags, 0, 6);

        addDetail(section("参数", api.params.isEmpty() ? "无" : api.params.size() + " 项"), 6, 6);
        if (api.params.isEmpty()) {
            addDetail(wrap("本函数没有参数。", Ui.sans(12.5f, Font.PLAIN), Ui.MUTED, true, 0), 0, 8);
        } else {
            for (Model.Param p : api.params) {
                addDetail(paramCard(p), 0, 8);
            }
        }

        addDetail(section("返回值", null), 6, 6);
        addDetail(card(wrap(api.ret.isBlank() ? "void" : api.ret, Ui.mono(12f, Font.PLAIN),
                Ui.alpha(Ui.TEXT, 0.9f), true, 24)), 0, 8);

        if (!api.notes.isEmpty()) {
            addDetail(section("注意事项", api.notes.size() + " 条"), 6, 6);
            for (String note : api.notes) {
                addDetail(noteRow(note), 0, 6);
            }
        }

        String example = api.exampleFor(series);
        if (!example.isBlank()) {
            addDetail(section("代码示例", series.label + " · CODE"), 6, 6);
            addDetail(codeBlock(series.label + " 示例", example), 0, 8);
        }

        String diff = api.diffFor(series);
        if (!diff.isBlank()) {
            addDetail(section(series.label + " 系列差异", "DIFF"), 6, 6);
            RoundPanel diffCard = new RoundPanel(Ui.blend(Ui.INK, Ui.ACCENT, 0.12f),
                    Ui.alpha(Ui.ACCENT, 0.35f), 12);
            diffCard.setLayout(new BorderLayout());
            diffCard.setBorder(new EmptyBorder(11, 12, 11, 12));
            diffCard.add(wrap(diff, Ui.sans(12.5f, Font.PLAIN), Ui.alpha(Ui.TEXT, 0.92f), true, 24));
            addDetail(diffCard, 0, 8);
        }

        List<String> related = new ArrayList<>();
        for (String name : api.related) {
            if (findApi(name) != null) {
                related.add(name);
            }
        }
        if (!related.isEmpty()) {
            addDetail(section("相关接口", null), 6, 6);
            JPanel row = Ui.flow();
            for (String name : related) {
                PillButton b = new PillButton(findApi(name).name, true);
                b.setArc(8);
                b.setFont(Ui.mono(11f, Font.BOLD));
                b.setForeground(Ui.CYAN);
                b.addActionListener(e -> openRelated(name));
                row.add(b);
            }
            addDetail(row, 0, 10);
        }

        detail.revalidate();
        detail.repaint();
        SwingUtilities.invokeLater(this::relayoutWraps);
    }

    private JComponent buildDetailHeader(Api api) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);

        JPanel titles = new JPanel(new BorderLayout());
        titles.setOpaque(false);
        JLabel name = Ui.label(api.name, Ui.mono(18f, Font.BOLD), Ui.TEXT);
        name.setToolTipText(api.name);
        titles.add(name, BorderLayout.NORTH);
        Category cat = data.categoryOf(api.cat);
        String sub = (cat == null ? "" : cat.name + " · ") + api.headerFor(series);
        titles.add(Ui.label(sub, Ui.sans(10.5f, Font.PLAIN), Ui.MUTED), BorderLayout.CENTER);
        row.add(titles, BorderLayout.CENTER);

        PillButton star = new PillButton("★ 收藏", false);
        star.setArc(8);
        star.setFont(Ui.sans(11f, Font.BOLD));
        star.setForeground(Ui.WARN);
        star.addActionListener(e -> toggleFavorite(api));
        updateStar(star, api);
        row.add(star, BorderLayout.EAST);
        return row;
    }

    private void updateStar(PillButton star, Api api) {
        boolean fav = favorites.contains(api.id);
        star.setText(fav ? "★ 已收藏" : "☆ 收藏");
        star.setForeground(fav ? Ui.WARN : Ui.MUTED);
    }

    private void toggleFavorite(Api api) {
        if (favorites.contains(api.id)) {
            favorites.remove(api.id);
        } else {
            favorites.add(api.id);
        }
        store.save(favorites, series.code);
        int scroll = detailScroll.getVerticalScrollBar().getValue();
        if (current != null && current.id.equals(api.id)) {
            showDetail(api);
            SwingUtilities.invokeLater(() -> detailScroll.getVerticalScrollBar().setValue(scroll));
        } else {
            refreshList();
        }
        refreshTabCounts();
    }

    private void refreshTabCounts() {
        int favCount = 0;
        for (Api a : data.visible(series)) {
            if (favorites.contains(a.id)) {
                favCount++;
            }
        }
        favTab.setText("我的收藏 " + favCount);
        list.repaint();
    }

    private void openRelated(String name) {
        Api api = findApi(name);
        if (api == null || !api.inSeries(series)) {
            return;
        }
        query = "";
        search.setText("");
        categoryId = null;
        commonMode = false;
        favMode = false;
        categoryMode = false;
        refreshList();
        list.setSelectedValue(api, true);
        if (list.getSelectedValue() != null && list.getSelectedValue().id.equals(api.id)) {
            showDetail(api);
        }
    }

    private Api findApi(String key) {
        Api api = data.byId.get(key);
        if (api != null) {
            return api;
        }
        for (Api a : data.apis) {
            if (a.name.equals(key)) {
                return a;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- 详情组件

    private void addDetail(Component c, int top, int bottom) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = detailRow++;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(top, 0, bottom, 0);
        detail.add(c, gbc);
    }

    private JComponent section(String title, String trailing) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.add(Ui.label(title, Ui.sans(13f, Font.BOLD), Ui.TEXT), BorderLayout.WEST);
        if (trailing != null && !trailing.isEmpty()) {
            row.add(Ui.label(trailing, Ui.mono(10f, Font.BOLD), Ui.MUTED), BorderLayout.EAST);
        }
        return row;
    }

    private JComponent tag(String text, Color tint, boolean mono) {
        RoundPanel p = new RoundPanel(Ui.alpha(tint, 0.12f), Ui.alpha(tint, 0.28f), 10);
        p.setLayout(new BorderLayout());
        p.setBorder(new EmptyBorder(3, 8, 3, 8));
        p.add(Ui.label(text, mono ? Ui.mono(10.5f, Font.BOLD) : Ui.sans(10.5f, Font.BOLD), tint));
        JPanel holder = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        holder.setOpaque(false);
        holder.add(p);
        return holder;
    }

    private JComponent card(JComponent content) {
        RoundPanel card = new RoundPanel(Ui.SURFACE1, Ui.STROKE, 12);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(11, 12, 11, 12));
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JComponent paramCard(Model.Param p) {
        RoundPanel card = new RoundPanel(Ui.SURFACE1, Ui.STROKE, 12);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(10, 12, 10, 12));

        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        head.setOpaque(false);
        head.add(Ui.label(p.name, Ui.mono(12.5f, Font.BOLD), Ui.CYAN));
        head.add(Ui.label(p.type, Ui.mono(10.5f, Font.PLAIN), Ui.MUTED));
        JPanel headWrap = new JPanel(new BorderLayout());
        headWrap.setOpaque(false);
        headWrap.add(head, BorderLayout.WEST);
        card.add(headWrap, BorderLayout.NORTH);

        if (!p.desc.isBlank()) {
            WrapArea desc = wrap(p.desc, Ui.sans(12.5f, Font.PLAIN), Ui.alpha(Ui.TEXT, 0.88f), true, 24);
            desc.setBorder(new EmptyBorder(5, 0, 0, 0));
            card.add(desc, BorderLayout.CENTER);
        }
        return card;
    }

    private JComponent noteRow(String note) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        JLabel bullet = Ui.label("•", Ui.sans(14f, Font.BOLD), Ui.CYAN);
        bullet.setVerticalAlignment(JLabel.TOP);
        row.add(bullet, BorderLayout.WEST);
        row.add(wrap(note, Ui.sans(12.5f, Font.PLAIN), Ui.alpha(Ui.TEXT, 0.88f), true, 18), BorderLayout.CENTER);
        return row;
    }

    private JComponent codeBlock(String title, String code) {
        RoundPanel card = new RoundPanel(Ui.SURFACE1, Ui.STROKE, 12);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(9, 12, 10, 12));

        JPanel head = new JPanel(new BorderLayout(8, 0));
        head.setOpaque(false);
        head.add(Ui.label(title, Ui.mono(10f, Font.BOLD), Ui.MUTED), BorderLayout.WEST);
        Ui.GhostButton copy = new Ui.GhostButton("复制");
        copy.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(code), null);
            copy.setText("已复制");
            Timer timer = new Timer(1400, ev -> copy.setText("复制"));
            timer.setRepeats(false);
            timer.start();
        });
        head.add(copy, BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        WrapArea area = wrap(code, Ui.mono(12f, Font.PLAIN), Ui.alpha(Ui.TEXT, 0.92f), false, 24);
        area.setBorder(new EmptyBorder(6, 0, 0, 0));
        card.add(area, BorderLayout.CENTER);
        return card;
    }

    private WrapArea wrap(String text, Font font, Color color, boolean wrapWords, int extraInset) {
        WrapArea area = new WrapArea(text, font, color, wrapWords);
        area.putClientProperty("inset", extraInset);
        wrapAreas.add(area);
        return area;
    }

    private void relayoutWraps() {
        int width = detailScroll.getViewport().getWidth();
        if (width <= 0) {
            return;
        }
        for (WrapArea area : wrapAreas) {
            Object insetObj = area.getClientProperty("inset");
            int inset = insetObj instanceof Integer ? (Integer) insetObj : 0;
            area.setFixedWidth(Math.max((int) (160 * Ui.SCALE), width - (int) (44 * Ui.SCALE) - inset));
        }
        detail.revalidate();
        detail.repaint();
    }

    // ---------------------------------------------------------------- 列表渲染

    private final class ApiCell extends JPanel implements ListCellRenderer<Api> {
        private final JLabel name = Ui.label("", Ui.mono(12.5f, Font.BOLD), Ui.TEXT);
        private final JLabel brief = Ui.label("", Ui.sans(10.5f, Font.PLAIN), Ui.MUTED);
        private final JLabel cat = Ui.label("", Ui.mono(9.5f, Font.BOLD), Ui.MUTED);

        ApiCell() {
            setLayout(new BorderLayout(10, 0));
            JPanel texts = new JPanel(new GridLayout(2, 1, 0, 2));
            texts.setOpaque(false);
            texts.add(name);
            texts.add(brief);
            add(texts, BorderLayout.CENTER);
            add(cat, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Api> l, Api api, int index,
                boolean selected, boolean focus) {
            setOpaque(true);
            setBackground(selected ? Ui.SURFACE2 : Ui.INK);
            setBorder(new CompoundBorder(
                    new MatteBorder(0, 3, 0, 0, selected ? Ui.ACCENT : Ui.INK),
                    new EmptyBorder(5, 9, 5, 6)));
            name.setText(api.name);
            name.setForeground(selected ? Ui.CYAN : Ui.TEXT);
            brief.setText(shorten(api.brief, 30));
            Category c = data.categoryOf(api.cat);
            cat.setText(c == null ? "" : c.code);
            cat.setPreferredSize(new Dimension((int) (44 * Ui.SCALE), (int) (14 * Ui.SCALE)));
            return this;
        }
    }

    private static String shorten(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    // ---------------------------------------------------------------- 其他

    private BufferedImage buildIcon() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new java.awt.GradientPaint(0, 0, Ui.ACCENT, 64, 64, Ui.CYAN));
        g.fillRoundRect(2, 2, 60, 60, 16, 16);
        g.setColor(Ui.INK);
        g.setFont(Ui.mono(18f, Font.BOLD));
        java.awt.FontMetrics fm = g.getFontMetrics();
        g.drawString("HAL", (64 - fm.stringWidth("HAL")) / 2, (64 + fm.getAscent() - fm.getDescent()) / 2);
        g.dispose();
        return img;
    }

    /** 从类路径读取内置数据。 */
    public static Model.Data loadData() throws IOException {
        InputStream res = HalFrame.class.getResourceAsStream("/assets/hal_api.json");
        if (res == null) {
            // 在 IDEA 中未把 resources 标记为资源目录时，直接读工程源文件
            Path dev = Paths.get("src/main/resources/assets/hal_api.json");
            if (Files.exists(dev)) {
                res = Files.newInputStream(dev);
            }
        }
        if (res == null) {
            throw new IOException("缺少数据文件 assets/hal_api.json，请把 src/main/resources 标记为 Resources Root");
        }
        try (InputStream in = res) {
            return Model.Data.load(in);
        }
    }
}
