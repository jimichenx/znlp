package dev.znlp.segment.impl.userdict;

import dev.znlp.base.dat.StringIntDATMap;

/**
 * 将 ResourceCustomDictionary 的某一类词典适配为 CustomDictionary。
 *
 * @author jimichan
 */
final class ResourceDictAdapter implements UserDictionary {

    private final ResourceUserDictionary resourceDict;
    private final ResourceDictKind kind;

    ResourceDictAdapter(ResourceUserDictionary resourceDict, ResourceDictKind kind) {
        this.resourceDict = resourceDict;
        this.kind = kind;
    }

    @Override
    public StringIntDATMap getTrie() {
        return kind.getDat(resourceDict);
    }
}
