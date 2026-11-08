package dev.znlp.segment.pipeline;

/**
 * 分词组件需要有个Name和设置是否启用的
 * <p>
 * Name : 组件的名称
 * Enable : 是否启用
 * Order ： 排序。 越小越靠前。
 *
 * @author jimichan
 */
public interface PipelineComponent extends Comparable<PipelineComponent> {


    /**
     * return component name
     *
     * @return name
     */
    String getName();

    /**
     * 组件是否启用。默认返回true，启用
     *
     * @return enabled
     */
    boolean isEnabled();

    void setEnabled(boolean enable);

    void enable();

    void disable();

    int getOrder();

    void setOrder(int order);

    @Override
    default int compareTo(PipelineComponent o) {
        return Integer.compare(this.getOrder(), o.getOrder());
    }
}
