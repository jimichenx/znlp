package dev.znlp.segment.impl.userdict;

import dev.znlp.base.dat.StringIntDATMap;

import java.util.function.Function;

/**
 * ResourceCustomDictionary 中可选的词典种类。
 * 用于在 {@link UserDictionaryPluginBuilder} 中选择一个或多个内置资源词典。
 *
 * @author jimichan
 */
public enum ResourceDictKind {

    /** 通用自定义词典 */
    CUSTOM_DICT(ResourceUserDictionary::getCustomDictDat),
    /** 上海地名 */
    SHANGHAI_PLACE(ResourceUserDictionary::getShanghaiPlaceNameDat),
    /** 人名词典 */
    PERSON_NAME(ResourceUserDictionary::getPersonNameDat),
    /** 全国地名大全 */
    NATIONWIDE_PLACE(ResourceUserDictionary::getNationwidePlaceNameDat),
    /** 机构名词典 */
    ORGANIZATION(ResourceUserDictionary::getOrganizationNameDat),
    /** 现代汉语补充词库 */
    MODERN_CHINESE_SUPPLEMENT(ResourceUserDictionary::getModernChineseSupplementDat);

    private final Function<ResourceUserDictionary, StringIntDATMap> getter;

    ResourceDictKind(Function<ResourceUserDictionary, StringIntDATMap> getter) {
        this.getter = getter;
    }

    StringIntDATMap getDat(ResourceUserDictionary dict) {
        return getter.apply(dict);
    }
}
