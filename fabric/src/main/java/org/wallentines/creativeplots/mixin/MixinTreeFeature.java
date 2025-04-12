package org.wallentines.creativeplots.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.wallentines.creativeplots.Plot;
import org.wallentines.creativeplots.PlotMap;
import org.wallentines.creativeplots.Plotworld;
import org.wallentines.mdcfg.Tuples;

import java.util.Set;
import java.util.function.BiConsumer;

@Mixin(TreeFeature.class)
public class MixinTreeFeature {

    @ModifyArgs(method="place", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/levelgen/feature/TreeFeature;doPlace(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Ljava/util/function/BiConsumer;Ljava/util/function/BiConsumer;Lnet/minecraft/world/level/levelgen/feature/foliageplacers/FoliagePlacer$FoliageSetter;Lnet/minecraft/world/level/levelgen/feature/configurations/TreeConfiguration;)Z"))
    private void safePlaceTree(Args args, @Local WorldGenLevel level, @Local(ordinal = 0) Set<BlockPos> set, @Local(ordinal = 1) Set<BlockPos> set2, @Local(ordinal = 2) Set<BlockPos> set3) {

        args.set(3, (BiConsumer<BlockPos, BlockState>) (pos, state) -> {
            if(level instanceof Plotworld pw) {
                Tuples.T2<PlotMap.BlockType, Plot> info = pw.getBlockInfo(pos.getX(), pos.getZ());
                if(info.p1 == null || info.p2 != null) {
                    set.add(pos.immutable());
                    level.setBlock(pos, state, 19);
                }
            }
        });
        args.set(4, (BiConsumer<BlockPos, BlockState>) (pos, state) -> {
            if(level instanceof Plotworld pw) {
                Tuples.T2<PlotMap.BlockType, Plot> info = pw.getBlockInfo(pos.getX(), pos.getZ());
                if(info.p1 == null || info.p2 != null) {
                    set2.add(pos.immutable());
                    level.setBlock(pos, state, 19);
                }
            }
        });
        args.set(5, new FoliagePlacer.FoliageSetter() {
            public void set(BlockPos pos, BlockState state) {
                if(level instanceof Plotworld pw) {
                    Tuples.T2<PlotMap.BlockType, Plot> info = pw.getBlockInfo(pos.getX(), pos.getZ());
                    if(info.p1 == null || info.p2 != null) {
                        set3.add(pos.immutable());
                        level.setBlock(pos, state, 19);
                    }
                }
            }

            public boolean isSet(BlockPos blockPos) {
                return set3.contains(blockPos);
            }
        });

    }

}
