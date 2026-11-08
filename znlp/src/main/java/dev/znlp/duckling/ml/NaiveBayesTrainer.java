package dev.znlp.duckling.ml;

import dev.znlp.duckling.DucklingParser;
import dev.znlp.duckling.rule.Rule;
import dev.znlp.duckling.support.Dimension;
import dev.znlp.duckling.support.DkNode;
import dev.znlp.duckling.support.FinalNodeValue;
import dev.znlp.duckling.support.NodeValue;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用语法 {@link Example}（及规则自带例子）训练消歧分类器。
 */
public class NaiveBayesTrainer {

    public MlModel train(List<Rule> rules, DucklingParser parser) {
        // Keep existing logic for backward compatibility or Rule.examples usage
        // But implementation below will shadow it or we can mix them.
        // Let's implement the new logic and if no examples are passed, fallback to Rule.examples?
        // Actually, user wants to use Example objects. Let's add an overload.
        return train(rules, Collections.emptyList(), parser, LocalDateTime.now());
    }

    public MlModel train(List<Rule> rules, List<Example> examples, DucklingParser parser, LocalDateTime clock) {
        Map<String, ClassifierStats> stats = new HashMap<>();

        // Initialize stats for all rules
        for (Rule rule : rules) {
            stats.put(rule.getName(), new ClassifierStats());
        }
        
        // 1. Process Rule.examples (Legacy string-only examples)
        // Only if we want to support mixed mode. For now, let's keep the old loop but adapt it.
        // Actually, better to separate or just iterate both.
        
        for (Rule rule : rules) {
            String targetName = rule.getName();
            for (String exampleText : rule.getExamples()) {
                 // ... existing logic ...
                 processExample(stats, parser, exampleText, targetName);
            }
        }

        // 2. Process Example objects (with predicates)
        for (Example example : examples) {
            processExampleObject(stats, parser, example, clock);
        }

        Map<String, Classifier> classifiers = new HashMap<>();
        for (Map.Entry<String, ClassifierStats> entry : stats.entrySet()) {
            classifiers.put(entry.getKey(), entry.getValue().build());
        }

        return new MlModel(classifiers);
    }
    
    private void processExample(Map<String, ClassifierStats> stats, DucklingParser parser, String exampleText, String targetName) {
        List<DkNode<NodeValue>> nodes = parser.generateAll(exampleText);
        List<DkNode<NodeValue>> fullMatches = filterFullMatches(nodes, exampleText.length());

        for (DkNode<?> node : fullMatches) {
            String predictedName = node.getRuleSym();
            FeatureInput input = node.getFeatureInput();
            ClassifierStats stat = stats.computeIfAbsent(predictedName, k -> new ClassifierStats());

            stat.add(input, targetName.equals(predictedName));
        }
    }

    private void processExampleObject(Map<String, ClassifierStats> stats, DucklingParser parser, Example example, LocalDateTime clock) {
        String exampleText = example.text();
        List<DkNode<NodeValue>> nodes = parser.generateAll(exampleText);
        List<DkNode<NodeValue>> fullMatches = filterFullMatches(nodes, exampleText.length());

        Set<DkNode<?>> positiveNodes = new HashSet<>();
        Set<DkNode<?>> negativeNodes = new HashSet<>();

        // Identify positive and negative nodes
        for (DkNode<NodeValue> node : fullMatches) {
            boolean isPositive = checkPredicate(node, example, clock);
            if (isPositive) {
                collectNodes(node, positiveNodes);
            } else {
                collectNodes(node, negativeNodes);
            }
        }
        
        // Remove positive nodes from negative set (correction)
        negativeNodes.removeAll(positiveNodes);

        // Update stats
        for (DkNode<?> node : positiveNodes) {
            ClassifierStats stat = stats.computeIfAbsent(node.getRuleSym(), k -> new ClassifierStats());
            stat.add(node.getFeatureInput(), true);
        }
        for (DkNode<?> node : negativeNodes) {
            ClassifierStats stat = stats.computeIfAbsent(node.getRuleSym(), k -> new ClassifierStats());
            stat.add(node.getFeatureInput(), false);
        }
    }

    private boolean checkPredicate(DkNode<NodeValue> node, Example example, LocalDateTime clock) {
        NodeValue value = node.getValue();
        Dimension dim = null;
        if (value instanceof FinalNodeValue) {
            dim = ((FinalNodeValue) value).output(clock);
        } else if (value instanceof Dimension) {
            dim = (Dimension) value;
        }
        
        if (dim != null) {
            return example.predicate().test(dim);
        }
        return false;
    }

    private void collectNodes(DkNode<?> node, Set<DkNode<?>> set) {
        set.add(node);
        if (node.getChildren() != null) {
            for (DkNode<?> child : node.getChildren()) {
                collectNodes(child, set);
            }
        }
    }

    private List<DkNode<NodeValue>> filterFullMatches(List<DkNode<NodeValue>> nodes, int length) {
        return nodes.stream()
                .filter(n -> n.getRange().start() == 0 && n.getRange().end() == length - 1)
                .collect(Collectors.toList());
    }

    private static class ClassifierStats {
        int posDocs = 0;
        int negDocs = 0;
        Map<String, Integer> posFeats = new HashMap<>();
        Map<String, Integer> negFeats = new HashMap<>();
        int posTotal = 0;
        int negTotal = 0;
        Set<String> vocab = new HashSet<>();

        void add(FeatureInput input, boolean positive) {
            if (positive) {
                posDocs++;
                for (String f : input.features()) {
                    posFeats.put(f, posFeats.getOrDefault(f, 0) + 1);
                    posTotal++;
                    vocab.add(f);
                }
            } else {
                negDocs++;
                for (String f : input.features()) {
                    negFeats.put(f, negFeats.getOrDefault(f, 0) + 1);
                    negTotal++;
                    vocab.add(f);
                }
            }
        }

        Classifier build() {
            ClassifierInfo trueInfo = buildInfo(posDocs, posFeats, posTotal, vocab.size());
            ClassifierInfo falseInfo = buildInfo(negDocs, negFeats, negTotal, vocab.size());
            return new Classifier(trueInfo, falseInfo);
        }

        private ClassifierInfo buildInfo(int docCount, Map<String, Integer> featCounts, int totalFeats, int vocabSize) {
            if (docCount == 0) return null;

            double alpha = 1.0; // Laplace smoothing
            double denom = totalFeats + alpha * vocabSize;
            double unkProbalog = Math.log(alpha / denom);

            // Prior P(c)
            // Note: In Duckling, we usually just store the class probability based on document counts?
            // Actually, Classifier.scores calculates trueScore and falseScore.
            // If we have posDocs and negDocs, P(pos) = posDocs / (posDocs + negDocs).
            // But ClassifierInfo stores `classProbalog`.
            // We calculate it relative to the dataset seen by THIS classifier.
            
            int totalDocs = posDocs + negDocs;
            double classProbalog = Math.log((double) docCount / totalDocs);

            Map<String, Double> featProbalog = new HashMap<>();
            for (Map.Entry<String, Integer> entry : featCounts.entrySet()) {
                double prob = (entry.getValue() + alpha) / denom;
                featProbalog.put(entry.getKey(), Math.log(prob));
            }

            return new ClassifierInfo(docCount, unkProbalog, classProbalog, featProbalog);
        }
    }
}
