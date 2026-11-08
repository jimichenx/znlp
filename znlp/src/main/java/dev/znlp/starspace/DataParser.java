package dev.znlp.starspace;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 数据解析接口，用于将原始文本行解析为 ParseResult 对象
 * <p>
 * 该接口定义了数据解析的核心方法，不同的实现类可以支持不同的数据格式和解析策略。
 * 主要用于将输入文本转换为模型可接受的特征和标签格式。
 */
public interface DataParser {
    /**
     * 解析文本行数据
     * @param line 原始文本行
     * @param sep 分隔符模式
     * @return 解析结果对象
     */
    ParseResult parse(String line, Pattern sep);

    /**
     * FastText 格式的数据解析器实现类
     * <p>
     * 该类实现了 DataParser 接口，用于解析 FastText 格式的文本数据。
     * 支持处理带权重的特征、文本归一化、以及不同的训练模式。
     */
    class FastTextDataParser implements DataParser {
        /** 词典对象，用于将词转换为ID */
        protected final Dictionary dictionary;
        /** 训练参数 */
        protected final Args args;
        /** 是否使用权重 */
        protected final boolean useWeight;
        /** 是否归一化文本 */
        protected final boolean normalizeText;
        /** 训练模式 */
        protected final Args.TrainMode trainMode;

        public FastTextDataParser(Dictionary dictionary, Args args) {
            this.dictionary = dictionary;
            this.args = args;
            this.useWeight = args.useWeight;
            this.normalizeText = args.normalizeText;
            this.trainMode = args.trainMode;
        }

        @Override
        public ParseResult parse(String line, Pattern sep) {
            String[] parts = sep.split(line);
            List<String> tokens = new ArrayList<>();
            for (String part : parts) {
                String t = part.trim();
                if (!t.isEmpty()) tokens.add(t);
            }

            List<Utils.XPair> lhsTokens = new ArrayList<>();
            List<Utils.XPair> rhsTokens = new ArrayList<>();
            ParseResult rslts = new ParseResult();
            rslts.lhsTokens = lhsTokens;
            rslts.rhsTokens = rhsTokens;

            for (String token : tokens) {
                if (token.startsWith("__weight__")) {
                    int pos = token.indexOf(":");
                    if (pos != -1) {
                        try {
                            rslts.weight = Float.parseFloat(token.substring(pos + 1));
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    }
                    continue;
                }

                String t = token;
                float weight = 1.0f;

                if (useWeight) {
                    int pos = token.indexOf(":");
                    if (pos != -1) {
                        t = token.substring(0, pos);
                        try {
                            weight = Float.parseFloat(token.substring(pos + 1));
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    }
                }

                if (normalizeText) {
                    t = Utils.NormalizeText.normalize(t);
                }

                int wid = dictionary.getId(t);
                if (wid < 0) {
                    continue;
                }

                Dictionary.EntryType type = dictionary.getType(wid);
                if (type == Dictionary.EntryType.Word) {
                    lhsTokens.add(new Utils.XPair(wid, weight));
                }

                if (type == Dictionary.EntryType.Label) {
                    rhsTokens.add(new Utils.XPair(wid, weight));
                }
            }

            dictionary.addNgrams(tokens, lhsTokens);

            return check(rslts) ? rslts : null;
        }

        /**
         * 检查解析结果是否有效
         * @param example 解析结果对象
         * @return 是否有效
         */
        public boolean check(ParseResult example) {
            if (trainMode == Args.TrainMode.Mode0) {
                return !example.rhsTokens.isEmpty() && !example.lhsTokens.isEmpty();
            }
            if (trainMode == Args.TrainMode.Mode5) {
                return !example.lhsTokens.isEmpty();
            } else {
                return example.rhsTokens.size() > 1;
            }
        }
    }

    /**
     * LabelDoc 格式的数据解析器实现类
     * <p>
     * 该类扩展了 FastTextDataParser，用于解析 LabelDoc 格式的文本数据。
     * 特点是支持按制表符分隔的特征，适用于分层训练的场景。
     */
    class LayerDataParser extends FastTextDataParser {

        public LayerDataParser(Dictionary dictionary, Args args) {
            super(dictionary, args);
        }

        @Override
        public ParseResult parse(String line, Pattern sep) {
            ParseResult rslt = new ParseResult();
            
            // Split by tab
            String[] partsArray = line.split("\t");
            List<String> parts = new ArrayList<>();
            for(String p : partsArray) {
                String trimmed = p.trim();
                if(!trimmed.isEmpty()) parts.add(trimmed);
            }

            int startIdx = 0;

            if (trainMode == Args.TrainMode.Mode0) {
                if (parts.size() > 0) {
                    rslt.lhsTokens = string2Pair(parts.get(startIdx));
                    startIdx += 1;
                }
            }

            List<List<Utils.XPair>> rhsFeature = new ArrayList<>(parts.size());
            rslt.rhsFeatures = rhsFeature;

            for (int i = startIdx; i < parts.size(); i++) {
                List<Utils.XPair> feats = string2Pair(parts.get(i));
                if (!feats.isEmpty()) {
                    rhsFeature.add(feats);
                }
            }

            boolean isValid;
            if (trainMode == Args.TrainMode.Mode0) {
                isValid = !rslt.lhsTokens.isEmpty() && !rslt.rhsFeatures.isEmpty();
            } else {
                isValid = rslt.rhsFeatures.size() > 1;
            }

            return isValid ? rslt : null;
        }

        /**
         * 将字符串转换为特征对列表
         * @param s 输入字符串
         * @return 特征对列表
         */
        private List<Utils.XPair> string2Pair(String s) {
            String[] tokensArray = s.split(" ");
            List<String> tokens = new ArrayList<>();
            for(String t : tokensArray) {
                 if(!t.isEmpty()) tokens.add(t);
            }
            
            if (tokens.isEmpty()) return new ArrayList<>();

            List<Utils.XPair> feats = new ArrayList<>(tokens.size());

            int startIdx = 0;
            float exWeight = 1.0f;

            if (tokens.get(0).indexOf("__weight__") >= 0) {
                int pos = tokens.get(0).indexOf(':');
                if (pos > 0) {
                    try {
                        exWeight = Float.parseFloat(tokens.get(0).substring(pos + 1));
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
                startIdx = 1;
            }

            for (int i = startIdx; i < tokens.size(); i++) {
                String t = tokens.get(i);
                if (t.trim().isEmpty()) continue;

                float weight = 1.0f;
                if (useWeight) {
                    int pos = t.indexOf(':');
                    if (pos > 0) {
                        try {
                            weight = Float.parseFloat(t.substring(pos + 1));
                            t = t.substring(0, pos);
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    }
                }

                if (normalizeText) {
                    t = Utils.NormalizeText.normalize(t);
                }

                int wid = dictionary.getId(t);
                if (wid != -1) {
                    feats.add(new Utils.XPair(wid, weight * exWeight));
                }
            }
            
            dictionary.addNgrams(tokens, feats);

            return feats;
        }
    }
}
