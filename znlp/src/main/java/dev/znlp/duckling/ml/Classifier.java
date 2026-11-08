package dev.znlp.duckling.ml;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

/** 单个规则名上的二分类朴素贝叶斯（true/false 两类）。 */
public class Classifier {
    private final ClassifierInfo trueInfo;
    private final ClassifierInfo falseInfo;

    private static final ClassifierResult GOOD_IF_EMPTY = new ClassifierResult(0.0, Double.NEGATIVE_INFINITY);

    public Classifier(ClassifierInfo trueInfo, ClassifierInfo falseInfo) {
        this.trueInfo = trueInfo;
        this.falseInfo = falseInfo;
    }

    public ClassifierResult scores(Map<String, Integer> bagOfFeatures) {
        if (bagOfFeatures.isEmpty()) {
            return GOOD_IF_EMPTY;
        }

        Double trueScore = score(trueInfo, bagOfFeatures);
        Double falseScore = score(falseInfo, bagOfFeatures);

        double temp = 0.0;
        if (trueScore != null) {
            temp += Math.exp(trueScore);
        }
        if (falseScore != null) {
            temp += Math.exp(falseScore);
        }

        double normlog = Math.log(temp);

        if (trueScore != null) {
            trueScore -= normlog;
        }
        if (falseScore != null) {
            falseScore -= normlog;
        }

        return new ClassifierResult(
                trueScore != null ? trueScore : Double.NEGATIVE_INFINITY,
                falseScore != null ? falseScore : Double.NEGATIVE_INFINITY
        );
    }

    private Double score(ClassifierInfo info, Map<String, Integer> bagOfFeatures) {
        if (info == null) return null;

        double probalog = 0.0;
        for (Map.Entry<String, Integer> entry : bagOfFeatures.entrySet()) {
            String feat = entry.getKey();
            int count = entry.getValue();
            probalog += count * info.getFeatProbalog().getOrDefault(feat, info.getUnkProbalog());
        }

        return probalog + info.getClassProbalog();
    }

    public static Classifier load(DataInputStream ins) throws IOException {
        String hasTrueStr = ins.readUTF();
        boolean hasTrue = "hasTrue".equals(hasTrueStr);
        String hasFalseStr = ins.readUTF();
        boolean hasFalse = "hasFalse".equals(hasFalseStr);

        ClassifierInfo trueInfo = hasTrue ? ClassifierInfo.read(ins) : null;
        ClassifierInfo falseInfo = hasFalse ? ClassifierInfo.read(ins) : null;

        return new Classifier(trueInfo, falseInfo);
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(trueInfo != null ? "hasTrue" : "noTrue");
        out.writeUTF(falseInfo != null ? "hasFalse" : "noFalse");

        if (trueInfo != null) {
            trueInfo.write(out);
        }
        if (falseInfo != null) {
            falseInfo.write(out);
        }
    }
}
