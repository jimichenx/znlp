package dev.znlp.duckling.grammar.duration;

import dev.znlp.duckling.grammar.time.Grain;
import dev.znlp.duckling.support.Dimension;
import dev.znlp.duckling.support.DimensionKind;
import dev.znlp.duckling.support.NodeValue;

public class DurationDimension extends Dimension implements NodeValue {
    private final float value;
    private final Grain grain;
    private boolean latent = false;

    public DurationDimension(float value, Grain grain) {
        this.value = value;
        this.grain = grain;
    }

    @Override
    public DimensionKind getKind() {
        return DimensionKind.Duration;
    }

    public float getValue() {
        return value;
    }

    public Grain getGrain() {
        return grain;
    }

    public DurationDimension setLatent(boolean latent) {
        this.latent = latent;
        return this;
    }

    @Override
    public boolean isLatent() {
        return latent;
    }

    public DurationDimension copy(float value) {
        return new DurationDimension(value, this.grain);
    }

    @Override
    public String toString() {
        return "Duration(" + value + " " + grain + ")";
    }
}
