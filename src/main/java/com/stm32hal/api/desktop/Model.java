package com.stm32hal.api.desktop;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 数据模型与查询逻辑。 */
public final class Model {

    /** 数据缺失或无记录时的默认系列。 */
    public static final String DEFAULT_SERIES_CODE = "f4";

    private Model() {
    }

    /** 一个 STM32 产品系列（F1/F4/C0/G0/G4/H5/H7/L0/L1/L4/L5/U0/U5 …）。 */
    public static final class Series {
        public final String code;
        public final String label;
        public final String family;
        public final String headerPrefix;
        public final String core;
        public final int maxFreq;
        public final String flash;
        public final String ram;
        public final String typical;
        public final String brief;
        public final String note;
        public final Map<String, String> features;

        Series(String code, String label, String family, String headerPrefix, String core, int maxFreq,
                String flash, String ram, String typical, String brief, String note, Map<String, String> features) {
            this.code = code;
            this.label = label;
            this.family = family;
            this.headerPrefix = headerPrefix;
            this.core = core;
            this.maxFreq = maxFreq;
            this.flash = flash;
            this.ram = ram;
            this.typical = typical;
            this.brief = brief;
            this.note = note;
            this.features = Collections.unmodifiableMap(new LinkedHashMap<>(features));
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Series && ((Series) o).code.equals(code);
        }

        @Override
        public int hashCode() {
            return code.hashCode();
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public static final class Param {
        public final String name;
        public final String type;
        public final String desc;

        Param(String name, String type, String desc) {
            this.name = name;
            this.type = type;
            this.desc = desc;
        }
    }

    /** 某个章节在某个系列下的文本覆盖（字段为 null 表示沿用通用文本）。 */
    private static final class Variant {
        final List<String> guide;
        final List<String> cubemx;
        final String demoTitle;
        final String demo;
        final String note;

        Variant(List<String> guide, List<String> cubemx, String demoTitle, String demo, String note) {
            this.guide = guide;
            this.cubemx = cubemx;
            this.demoTitle = demoTitle;
            this.demo = demo;
            this.note = note;
        }
    }

    public static final class Category {
        public final String id;
        public final String name;
        public final String code;
        public final String desc;
        public final int count;
        public final List<String> guide;
        public final List<String> cubemx;
        public final String demoTitle;
        public final String demo;
        public final String note;
        /** 各系列的文本覆盖，key = 系列 code。 */
        public final Map<String, Variant> variants;
        /** 章节可用系列白名单；空表示所有系列可用。 */
        public final List<String> series;

        Category(String id, String name, String code, String desc, int count,
                List<String> guide, List<String> cubemx, String demoTitle, String demo, String note,
                Map<String, Variant> variants, List<String> series) {
            this.id = id;
            this.name = name;
            this.code = code;
            this.desc = desc;
            this.count = count;
            this.guide = guide;
            this.cubemx = cubemx;
            this.demoTitle = demoTitle;
            this.demo = demo;
            this.note = note;
            this.variants = Collections.unmodifiableMap(new LinkedHashMap<>(variants));
            this.series = series;
        }

        private Variant variant(Series s) {
            return s == null ? null : variants.get(s.code);
        }

        public boolean inSeries(Series s) {
            return series.isEmpty() || (s != null && series.contains(s.code));
        }

        public List<String> guideFor(Series s) {
            Variant v = variant(s);
            return v != null && v.guide != null && !v.guide.isEmpty() ? v.guide : guide;
        }

        public List<String> cubemxFor(Series s) {
            Variant v = variant(s);
            return v != null && v.cubemx != null && !v.cubemx.isEmpty() ? v.cubemx : cubemx;
        }

        public String demoTitleFor(Series s) {
            Variant v = variant(s);
            return v != null && v.demoTitle != null && !v.demoTitle.isBlank() ? v.demoTitle : demoTitle;
        }

        public String demoFor(Series s) {
            Variant v = variant(s);
            String text = v != null && v.demo != null && !v.demo.isBlank() ? v.demo : demo;
            text = text == null ? "" : text;
            return s == null ? text : text.replace("stm32xx", s.headerPrefix);
        }

        public String noteFor(Series s) {
            Variant v = variant(s);
            return v != null && v.note != null && !v.note.isBlank() ? v.note : note;
        }
    }

    public static final class Api {
        public final String id;
        public final String name;
        public final String cat;
        public final String header;
        public final String sig;
        public final String brief;
        public final String ret;
        public final String example;
        /** 各系列专属示例，key = 系列 code。 */
        public final Map<String, String> examples;
        /** 可用系列白名单；空表示所有系列可用。 */
        public final List<String> series;
        public final List<Param> params;
        public final List<String> notes;
        public final List<String> keywords;
        public final List<String> related;
        public final Map<String, String> diff;
        public final boolean common;

        Api(String id, String name, String cat, String header, String sig, String brief, String ret,
                String example, Map<String, String> examples, List<String> series, List<Param> params,
                List<String> notes, List<String> keywords, List<String> related, Map<String, String> diff,
                boolean common) {
            this.id = id;
            this.name = name;
            this.cat = cat;
            this.header = header;
            this.sig = sig;
            this.brief = brief;
            this.ret = ret;
            this.example = example;
            this.examples = Collections.unmodifiableMap(new LinkedHashMap<>(examples));
            this.series = series;
            this.params = params;
            this.notes = notes;
            this.keywords = keywords;
            this.related = related;
            this.diff = Collections.unmodifiableMap(new LinkedHashMap<>(diff));
            this.common = common;
        }

        public boolean inSeries(Series s) {
            return series.isEmpty() || (s != null && series.contains(s.code));
        }

        public String headerFor(Series s) {
            String h = header == null ? "" : header;
            return s == null ? h : h.replace("stm32xx", s.headerPrefix);
        }

        public String exampleFor(Series s) {
            String e = s == null ? null : examples.get(s.code);
            return e != null && !e.isBlank() ? e : example;
        }

        public String diffFor(Series s) {
            String d = s == null ? null : diff.get(s.code);
            return d == null ? "" : d;
        }
    }

    public static final class Data {
        public final String version;
        public final String updated;
        public final String note;
        public final List<Series> series;
        public final List<Category> categories;
        public final List<Api> apis;
        public final Map<String, Series> seriesByCode;
        public final Map<String, Api> byId;
        public final Map<String, List<Api>> byCat;

        Data(String version, String updated, String note, List<Series> series,
                List<Category> categories, List<Api> apis) {
            this.version = version;
            this.updated = updated;
            this.note = note;
            this.series = series;
            this.categories = categories;
            this.apis = apis;
            Map<String, Series> seriesMap = new LinkedHashMap<>();
            for (Series s : series) {
                seriesMap.put(s.code, s);
            }
            this.seriesByCode = Collections.unmodifiableMap(seriesMap);
            Map<String, Api> ids = new LinkedHashMap<>();
            Map<String, List<Api>> cats = new LinkedHashMap<>();
            for (Api api : apis) {
                ids.put(api.id, api);
                cats.computeIfAbsent(api.cat, k -> new ArrayList<>()).add(api);
            }
            this.byId = Collections.unmodifiableMap(ids);
            this.byCat = Collections.unmodifiableMap(cats);
        }

        public List<Series> series() {
            return series;
        }

        public Series series(String code) {
            return seriesByCode.get(code);
        }

        public Series defaultSeries() {
            Series s = seriesByCode.get(DEFAULT_SERIES_CODE);
            if (s == null && !series.isEmpty()) {
                s = series.get(0);
            }
            return s;
        }

        public Category categoryOf(String id) {
            for (Category c : categories) {
                if (c.id.equals(id)) {
                    return c;
                }
            }
            return null;
        }

        /** 该系列可用的章节（大章），保持数据里的顺序。 */
        public List<Category> categories(Series series) {
            List<Category> out = new ArrayList<>();
            for (Category c : categories) {
                if (c.inSeries(series)) {
                    out.add(c);
                }
            }
            return out;
        }

        public boolean categoryAvailable(String categoryId, Series series) {
            Category c = categoryOf(categoryId);
            return c == null || c.inSeries(series);
        }

        public List<Api> visible(Series series) {
            List<Api> out = new ArrayList<>();
            for (Api api : apis) {
                if (api.inSeries(series) && categoryAvailable(api.cat, series)) {
                    out.add(api);
                }
            }
            return out;
        }

        public int countIn(Series series, String categoryId) {
            int n = 0;
            for (Api api : apis) {
                if (api.inSeries(series) && api.cat.equals(categoryId)) {
                    n++;
                }
            }
            return n;
        }

        public int countIn(Series series) {
            return visible(series).size();
        }

        public List<Api> search(String raw, Series series) {
            String q = raw == null ? "" : raw.trim().toLowerCase();
            if (q.isEmpty()) {
                return Collections.emptyList();
            }
            Map<String, String> catNames = new LinkedHashMap<>();
            for (Category c : categories) {
                catNames.put(c.id, (c.name + " " + c.desc).toLowerCase());
            }
            List<Map.Entry<Api, Integer>> hits = new ArrayList<>();
            for (Api api : apis) {
                if (!api.inSeries(series) || !categoryAvailable(api.cat, series)) {
                    continue;
                }
                int score = 0;
                String name = api.name.toLowerCase();
                if (name.equals(q)) {
                    score = 120;
                } else if (name.startsWith(q)) {
                    score = 100;
                } else if (name.contains(q)) {
                    score = 85;
                }
                for (String kw : api.keywords) {
                    if (kw.toLowerCase().contains(q)) {
                        score = Math.max(score, 60);
                        break;
                    }
                }
                if (api.brief.toLowerCase().contains(q)) {
                    score = Math.max(score, 45);
                }
                if (api.sig.toLowerCase().contains(q)) {
                    score = Math.max(score, 40);
                }
                if (api.header.toLowerCase().contains(q)) {
                    score = Math.max(score, 30);
                }
                if (!api.id.equals(api.name) && api.id.toLowerCase().contains(q)) {
                    score = Math.max(score, 75);
                }
                String cat = catNames.get(api.cat);
                if (cat != null && cat.contains(q)) {
                    score = Math.max(score, 25);
                }
                if (score > 0) {
                    hits.add(Map.entry(api, score));
                }
            }
            hits.sort((a, b) -> {
                int byScore = Integer.compare(b.getValue(), a.getValue());
                return byScore != 0 ? byScore : a.getKey().name.compareTo(b.getKey().name);
            });
            List<Api> out = new ArrayList<>();
            for (Map.Entry<Api, Integer> e : hits) {
                out.add(e.getKey());
            }
            return out;
        }

        public static Data load(InputStream in) throws IOException {
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> root = asObject(Json.parse(text));
            Map<String, Object> meta = asObject(root.get("meta"));

            List<Series> seriesList = new ArrayList<>();
            for (Object item : asList(root.get("series"))) {
                Map<String, Object> o = asObject(item);
                Map<String, String> features = new LinkedHashMap<>();
                Object fo = o.get("features");
                if (fo instanceof Map) {
                    for (Map.Entry<String, Object> e : asObject(fo).entrySet()) {
                        features.put(e.getKey(), String.valueOf(e.getValue()));
                    }
                }
                seriesList.add(new Series(
                        str(o, "code"),
                        str(o, "label", str(o, "code").toUpperCase()),
                        str(o, "family"),
                        str(o, "header", str(o, "header_prefix")),
                        str(o, "core"),
                        integer(o.get("max_freq")),
                        str(o, "flash"),
                        str(o, "ram"),
                        str(o, "typical"),
                        str(o, "brief"),
                        str(o, "note"),
                        features));
            }
            if (seriesList.isEmpty()) {
                seriesList = legacySeries();
            }

            List<Category> categories = new ArrayList<>();
            for (Object item : asList(root.get("categories"))) {
                Map<String, Object> o = asObject(item);
                Map<String, Variant> variants = new LinkedHashMap<>();
                Object vo = o.get("variants");
                if (vo instanceof Map) {
                    for (Map.Entry<String, Object> e : asObject(vo).entrySet()) {
                        Map<String, Object> v = asObject(e.getValue());
                        variants.put(e.getKey(), new Variant(
                                strings(v.get("guide")),
                                strings(v.get("cubemx")),
                                optionalStr(v, "demo_title"),
                                optionalStr(v, "demo"),
                                optionalStr(v, "note")));
                    }
                }
                categories.add(new Category(
                        str(o, "id"),
                        str(o, "name"),
                        str(o, "code", str(o, "id").toUpperCase()),
                        str(o, "desc"),
                        integer(o.get("count")),
                        strings(o.get("guide")),
                        strings(o.get("cubemx")),
                        str(o, "demo_title"),
                        str(o, "demo"),
                        str(o, "note"),
                        variants,
                        strings(o.get("series"))));
            }

            List<Api> apis = new ArrayList<>();
            for (Object item : asList(root.get("apis"))) {
                Map<String, Object> o = asObject(item);
                List<Param> params = new ArrayList<>();
                for (Object p : asList(o.get("params"))) {
                    Map<String, Object> po = asObject(p);
                    params.add(new Param(str(po, "n"), str(po, "t"), str(po, "d")));
                }
                Map<String, String> diff = new LinkedHashMap<>();
                Object diffObj = o.get("diff");
                if (diffObj instanceof Map) {
                    for (Map.Entry<String, Object> e : asObject(diffObj).entrySet()) {
                        diff.put(e.getKey(), String.valueOf(e.getValue()));
                    }
                }
                Map<String, String> examples = new LinkedHashMap<>();
                Object examplesObj = o.get("examples");
                if (examplesObj instanceof Map) {
                    for (Map.Entry<String, Object> e : asObject(examplesObj).entrySet()) {
                        String value = String.valueOf(e.getValue());
                        if (!value.isBlank()) {
                            examples.put(e.getKey(), value);
                        }
                    }
                }
                String legacyF1 = str(o, "example_f1");
                if (!legacyF1.isBlank() && !examples.containsKey("f1")) {
                    examples.put("f1", legacyF1);
                }
                List<String> apiSeries = new ArrayList<>();
                Object seriesObj = o.get("series");
                if (seriesObj instanceof String) {
                    String value = String.valueOf(seriesObj).trim();
                    if (!value.isEmpty()) {
                        apiSeries.add(value);
                    }
                } else {
                    apiSeries = strings(seriesObj);
                }
                apis.add(new Api(
                        str(o, "id"),
                        str(o, "name", str(o, "id")),
                        str(o, "cat"),
                        str(o, "header"),
                        str(o, "sig"),
                        str(o, "brief"),
                        str(o, "ret"),
                        str(o, "example"),
                        examples,
                        apiSeries,
                        params,
                        strings(o.get("notes")),
                        strings(o.get("keywords")),
                        strings(o.get("related")),
                        diff,
                        bool(o.get("common"))));
            }

            return new Data(
                    meta == null ? "1.0.0" : str(meta, "version", "1.0.0"),
                    meta == null ? "" : str(meta, "updated"),
                    meta == null ? "" : str(meta, "note"),
                    seriesList,
                    categories,
                    apis);
        }

        /** 兼容 v1 数据（只有 F1/F4、没有 series 数组）时的内置系列。 */
        private static List<Series> legacySeries() {
            List<Series> out = new ArrayList<>();
            Map<String, String> f1 = new LinkedHashMap<>();
            f1.put("PLL", "PLLMUL，最高 72MHz");
            f1.put("DMA", "固定通道");
            f1.put("FLASH", "按页擦除");
            f1.put("ADC", "12-bit 固定");
            f1.put("CAN", "bxCAN");
            f1.put("GPIO", "AFIO 重映射");
            out.add(new Series("f1", "F1", "STM32F1", "stm32f1xx", "ARM Cortex-M3", 72,
                    "16–768 KB", "6–96 KB", "STM32F103C8T6",
                    "经典入门系列，外设简单、资料最多。",
                    "72MHz Cortex-M3；AFIO 重映射引脚；无 GPIO Alternate 字段；ADC 12-bit 无校准函数。", f1));
            Map<String, String> f4 = new LinkedHashMap<>();
            f4.put("PLL", "PLLM/PLLN/PLLP/PLLQ");
            f4.put("DMA", "Stream + Channel");
            f4.put("FLASH", "按扇区擦除");
            f4.put("ADC", "6/8/10/12-bit");
            f4.put("CAN", "bxCAN");
            f4.put("GPIO", "GPIO Alternate");
            out.add(new Series("f4", "F4", "STM32F4", "stm32f4xx", "ARM Cortex-M4F", 168,
                    "64 KB–2 MB", "64–320 KB", "STM32F407ZGT6",
                    "带 FPU 与 DSP 指令的主流高性能系列。",
                    "168MHz Cortex-M4F；PLL M/N/P/Q；DMA Stream+Channel；Flash 按扇区擦除；ADC 支持多种分辨率。", f4));
            return out;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asObject(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object o) {
        return o instanceof List ? (List<Object>) o : Collections.emptyList();
    }

    private static String str(Map<String, Object> o, String key) {
        Object v = o.get(key);
        return v == null ? "" : String.valueOf(v);
    }

    private static String optionalStr(Map<String, Object> o, String key) {
        Object v = o.get(key);
        if (v == null) {
            return null;
        }
        String s = String.valueOf(v);
        return s.isBlank() ? null : s;
    }

    private static String str(Map<String, Object> o, String key, String def) {
        Object v = o.get(key);
        return v == null || String.valueOf(v).isEmpty() ? def : String.valueOf(v);
    }

    private static List<String> strings(Object o) {
        List<String> out = new ArrayList<>();
        for (Object v : asList(o)) {
            if (v != null) {
                out.add(String.valueOf(v));
            }
        }
        return out;
    }

    private static int integer(Object o) {
        return o instanceof Number ? ((Number) o).intValue() : 0;
    }

    private static boolean bool(Object o) {
        return o instanceof Boolean && (Boolean) o;
    }
}
