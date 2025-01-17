package io.github.foundationgames.sandwichable.util;

import com.google.common.collect.ImmutableMap;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import io.github.foundationgames.sandwichable.blocks.entity.SyncedBlockEntity;
import io.github.foundationgames.sandwichable.config.ConfigInABarrel;
import io.github.foundationgames.sandwichable.config.SandwichableConfig;
import io.github.foundationgames.sandwichable.mixin.StructurePoolAccess;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.structure.processor.StructureProcessorLists;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.TradeOffers;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public class Util {

    public static String MOD_ID = "sandwichable";

    public static Identifier id(String name) {
        return new Identifier(MOD_ID, name);
    }

    public static void scatterBlockDust(World world, BlockPos pos, Block block, int intensity, int density) {
        Random random = new Random();
        for (int i = 0; i < density; i++) {
            double ox, oy, oz, vx, vy, vz;
            ox = (double)(random.nextInt(intensity * 2) - intensity) / 10;
            oy = (double)(random.nextInt(intensity * 2) - intensity) / 10;
            oz = (double)(random.nextInt(intensity * 2) - intensity) / 10;
            vx = (double)(random.nextInt(intensity * 2) - intensity) / 10;
            vy = (double)(random.nextInt(intensity * 2) - intensity) / 10;
            vz = (double)(random.nextInt(intensity * 2) - intensity) / 10;
            world.addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, block.getDefaultState()), pos.getX()+0.5+ox, pos.getY()+0.5+oy, pos.getZ()+0.5+oz, 0.0D+vx, 0.0D+vy, 0.0D+vz);
        }
    }

    public static void scatterDroppedBlockDust(World world, BlockPos pos, Block block, int intensity, int density) {
        Random random = new Random();
        for (int i = 0; i < density; i++) {
            double ox, oy, oz, vx, vy, vz;
            ox = (double)(random.nextInt(intensity * 2) - intensity) / 10;
            oy = (double)(random.nextInt(intensity * 2) - intensity) / 10;
            oz = (double)(random.nextInt(intensity * 2) - intensity) / 10;
            world.addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, block.getDefaultState()), pos.getX()+0.5+ox, pos.getY()+0.5+oy, pos.getZ()+0.5+oz, 0.0D, -0.3D, 0.0D);
        }
    }

    public static Int2ObjectMap<TradeOffers.Factory[]> copyToFastUtilMap(ImmutableMap<Integer, TradeOffers.Factory[]> immutableMap) {
        return new Int2ObjectOpenHashMap(immutableMap);
    }

    public static void tryAddElementToPool(Identifier targetPool, StructurePool pool, String elementId, StructurePool.Projection projection, int weight) {
        if(targetPool.equals(pool.getId())) {
            StructurePoolElement element = StructurePoolElement.ofProcessedLegacySingle(elementId, StructureProcessorLists.EMPTY).apply(projection);
            for (int i = 0; i < weight; i++) {
                ((StructurePoolAccess)pool).sandwichable$getElements().add(element);
            }
            ((StructurePoolAccess)pool).sandwichable$getElementCounts().add(Pair.of(element, weight));
        }
    }

    public static void sync(SyncedBlockEntity be) {
        be.sync();
    }

    public static int floatToIntWithBounds(float input, int bounds) {
        return (int)(input*bounds);
    }

    public static void appendInfoTooltip(List<Text> tooltip, String itemTranslationKey) {
        SandwichableConfig config = Util.getConfig();
        if(config.showInfoTooltips) {
            if (config.infoTooltipKeyBind.isPressed()) {
                tooltip.add(new TranslatableText("sandwichable.tooltip.infoheader").formatted(Formatting.GREEN));
                char[] infoChars = I18n.translate(itemTranslationKey + ".info").toCharArray();
                int lineLength = I18n.translate(itemTranslationKey).length();
                if (lineLength < Math.sqrt(infoChars.length) * 1.5) {
                    lineLength = (int) Math.sqrt(infoChars.length * 1.5);
                }
                StringBuilder ln = new StringBuilder();
                for (char c : infoChars) {
                    ln.append(c);
                    if (c == ' ' && ln.toString().length() > lineLength) {
                        tooltip.add(new LiteralText(ln.toString()).formatted(Formatting.GRAY));
                        ln = new StringBuilder();
                    }
                }
                if (!ln.toString().isEmpty()) {
                    tooltip.add(new LiteralText(ln.toString()).formatted(Formatting.GRAY));
                }
            } else {
                tooltip.add(new TranslatableText("sandwichable.tooltip."+config.infoTooltipKeyBind.getName()).formatted(Formatting.GREEN));
            }
        }
    }

    public static ItemStack itemFromString(String str, Supplier<ItemStack> orElse) {
        var nbtBegin = str.indexOf("{");
        var itemStr = str.substring(0, nbtBegin > -1 ? nbtBegin : str.length());
        var nbtStr = ((nbtBegin > -1) && str.contains("}")) ? str.substring(nbtBegin, str.lastIndexOf("}") + 1) : null;

        var itemId = Identifier.tryParse(itemStr);
        if (itemId != null) {
            var entry = Registry.ITEM.getOrEmpty(itemId);
            if (entry.isPresent()) {
                var item = entry.get();

                var stack = new ItemStack(item);
                if (nbtStr != null) {
                    try {
                        var nbt = StringNbtReader.parse(nbtStr);
                        stack.setNbt(nbt);
                    } catch (CommandSyntaxException ignored) {}
                }

                return stack;
            }
        }

        return orElse.get();
    }

    public static <T> T create(Supplier<T> creator) {
        return creator.get();
    }

    public static int getSaltyWaterColor() {
        return 0x6ce0eb;
    }

    public static SandwichableConfig getConfig() {
        return ConfigInABarrel.config(MOD_ID, SandwichableConfig.class, SandwichableConfig::new);
    }
}
