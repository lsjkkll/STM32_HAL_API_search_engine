package com.stm32hal.api.desktop;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 数据模型与查询逻辑。 */
public final class Model {

    private Model() {
    }

    public enum Series {
        F1("f1", "F1", "stm32f1xx"),
        F4("f4", "F4", "stm32f4xx");

        public final String code;
        public final String label;
        public final String headerPrefix;

        Series(String code, String label, String headerPrefix) {
            this.code = code;
            this.label = label;
            this.headerPrefix = headerPrefix;
        }

        public static Series fromCode(String code) {
            for (Series s : values()) {
                if (s.code.equals(code)) {
                    return s;
                }
            }
            return F4;
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

        Category(String id, String name, String code, String desc, int count,
                List<String> guide, List<String> cubemx, String demoTitle, String demo, String note) {
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
        }

        public String demoFor(Series s) {
            String text = demo == null ? "" : demo;
            return text.replace("stm32xx", s.headerPrefix);
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
        public final String exampleF1;
        public final String series;
        public final List<Param> params;
        public final List<String> notes;
        public final List<String> keywords;
        public final List<String> related;
        public final Map<String, String> diff;
        public final boolean common;

        Api(String id, String name, String cat, String header, String sig, String brief, String ret,
                String example, String exampleF1, String series, List<Param> params, List<String> notes,
                List<String> keywords, List<String> related, Map<String, String> diff, boolean common) {
            this.id = id;
            this.name = name;
            this.cat = cat;
            this.header = header;
            this.sig = sig;
            this.brief = brief;
            this.ret = ret;
            this.example = example;
            this.exampleF1 = exampleF1;
            this.series = series;
            this.params = params;
            this.notes = notes;
            this.keywords = keywords;
            this.related = related;
            this.diff = diff;
            this.common = common;
        }

        public boolean inSeries(Series s) {
            return series == null || series.isEmpty() || series.equals(s.code);
        }

        public String headerFor(Series s) {
            return header.replace("stm32xx", s.headerPrefix);
        }

        public String exampleFor(Series s) {
            return s == Series.F1 && exampleF1 != null && !exampleF1.isBlank() ? exampleF1 : example;
        }

        public String diffFor(Series s) {
            String d = diff.get(s.code);
            return d == null ? "" : d;
        }
    }

    public static final class Data {
        public final String version;
        public final String updated;
        public final String note;
        public final List<Category> categories;
        public final List<Api> apis;
        public final Map<String, Api> byId;
        public final Map<String, List<Api>> byCat;

        Data(String version, String updated, String note, List<Category> categories, List<Api> apis) {
            this.version = version;
            this.updated = updated;
            this.note = note;
            this.categories = categories;
            this.apis = apis;
            Map<String, Api> ids = new LinkedHashMap<>();
            Map<String, List<Api>> cats = new LinkedHashMap<>();
            for (Api api : apis) {
                ids.put(api.id, api);
                cats.computeIfAbsent(api.cat, k -> new ArrayList<>()).add(api);
            }
            this.byId = Collections.unmodifiableMap(ids);
            this.byCat = Collections.unmodifiableMap(cats);
        }

        public Category categoryOf(String id) {
            for (Category c : categories) {
                if (c.id.equals(id)) {
                    return c;
                }
            }
            return null;
        }

        public List<Api> visible(Series series) {
            List<Api> out = new ArrayList<>();
            for (Api api : apis) {
                if (api.inSeries(series)) {
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
                if (!api.inSeries(series)) {
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

            List<Category> categories = new ArrayList<>();
            for (Object item : asList(root.get("categories"))) {
                Map<String, Object> o = asObject(item);
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
                        str(o, "note")));
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
                apis.add(new Api(
                        str(o, "id"),
                        str(o, "name", str(o, "id")),
                        str(o, "cat"),
                        str(o, "header"),
                        str(o, "sig"),
                        str(o, "brief"),
                        str(o, "ret"),
                        str(o, "example"),
                        str(o, "example_f1"),
                        str(o, "series"),
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
                    categories,
                    apis);
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
