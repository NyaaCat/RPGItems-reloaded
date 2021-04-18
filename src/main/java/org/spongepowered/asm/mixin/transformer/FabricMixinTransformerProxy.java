package org.spongepowered.asm.mixin.transformer;

public class FabricMixinTransformerProxy {
    private final MixinTransformer transformer = new MixinTransformer();

    public IMixinTransformer getTransformer() {
        return transformer;
    }
}