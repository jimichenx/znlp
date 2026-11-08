package dev.znlp.duckling.support;

import java.time.LocalDateTime;

/**
 * 可落地为 {@link Dimension} 的节点值。相对时间在 {@link #output(LocalDateTime)} 里用参考时钟计算。
 */
public abstract class FinalNodeValue implements NodeValue {
    private final DimensionKind kind;
    private boolean latent = false;

    public FinalNodeValue(DimensionKind kind) {
        this.kind = kind;
    }

    public abstract Dimension output(LocalDateTime clock);

    public DimensionKind getKind() {
        return kind;
    }

    @Override
    public boolean isLatent() {
        return latent;
    }

    public FinalNodeValue makeLatent() {
        this.latent = true;
        return this;
    }
    
    public FinalNodeValue makeNotLatent() {
        this.latent = false;
        return this;
    }
}
