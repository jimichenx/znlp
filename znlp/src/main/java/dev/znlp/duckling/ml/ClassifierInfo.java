package dev.znlp.duckling.ml;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ClassifierInfo {
    private final int exampleCount;
    private final double unkProbalog;
    private final double classProbalog;
    private final Map<String, Double> featProbalog;

    public ClassifierInfo(int exampleCount, double unkProbalog, double classProbalog, Map<String, Double> featProbalog) {
        this.exampleCount = exampleCount;
        this.unkProbalog = unkProbalog;
        this.classProbalog = classProbalog;
        this.featProbalog = featProbalog;
    }

    public static ClassifierInfo read(DataInputStream ins) throws IOException {
        int eC = ins.readInt();
        double unkProbalog = ins.readDouble();
        double classProbalog = ins.readDouble();
        int size = ins.readInt();
        Map<String, Double> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            map.put(ins.readUTF(), ins.readDouble());
        }
        return new ClassifierInfo(eC, unkProbalog, classProbalog, map);
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeInt(exampleCount);
        out.writeDouble(unkProbalog);
        out.writeDouble(classProbalog);
        out.writeInt(featProbalog.size());
        for (Map.Entry<String, Double> entry : featProbalog.entrySet()) {
            out.writeUTF(entry.getKey());
            out.writeDouble(entry.getValue());
        }
    }

    public double getUnkProbalog() {
        return unkProbalog;
    }

    public double getClassProbalog() {
        return classProbalog;
    }

    public Map<String, Double> getFeatProbalog() {
        return featProbalog;
    }
}
