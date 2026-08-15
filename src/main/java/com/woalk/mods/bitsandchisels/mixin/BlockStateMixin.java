// TODO(Ravel): Failed to fully resolve file: null cannot be cast to non-null type com.intellij.psi.PsiJavaCodeReferenceElement
package com.woalk.mods.bitsandchisels.mixin;

import java.util.List;

import com.woalk.mods.bitsandchisels.duck.CubeRenderStuff;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;

@Mixin(BlockState.class)
public class BlockStateMixin implements CubeRenderStuff {
    @Unique
    BakedModel oldModel = null;
    @Unique
    BakedQuad[][] quads = null;
    @Unique
    RenderMaterial[][] materials = null;

    @Unique
    private void recomputeIfNeeded() {
        BlockState thiz = (BlockState)(Object)this;
        BlockRenderDispatcher renderman = Minecraft.getInstance().getBlockRenderer();
        BakedModel newModel = renderman.getBlockModel(thiz);
        if (oldModel == null || oldModel != newModel) {
            oldModel = newModel;
            quads = new BakedQuad[7][];
            materials = new RenderMaterial[7][];
            for (Direction d : Direction.values()) {
                ModelManager modelman = Minecraft.getInstance().getModelManager();
                List<BakedQuad> dQuads = newModel.getQuads(thiz, d, new LegacyRandomSource(0));
                if (dQuads == null || dQuads.isEmpty()) dQuads = modelman.getMissingModel().getQuads(thiz, d, new LegacyRandomSource(0));
                BakedQuad[] localquads = dQuads.toArray(new BakedQuad[0]);
                quads[d.ordinal()] = localquads;
                materials[d.ordinal()] = new RenderMaterial[localquads.length];
                for (int i = 0; i < localquads.length; i++) {
                    materials[d.ordinal()][i] = RendererAccess.INSTANCE.getRenderer().materialFinder().blendMode(0, BlendMode.fromRenderLayer(ItemBlockRenderTypes.getChunkRenderType(thiz))).find();
                }
            }
        }
    }

    @Override
    public BakedQuad[] getQuads(int direction) {
        recomputeIfNeeded();
        return quads[direction];
    }

    @Override
    public RenderMaterial[] getMaterials(int direction) {
        recomputeIfNeeded();
        return materials[direction];
    }
    
}
