package com.sbancuz.plannh.data.provider;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFire;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.stats.StatList;
import net.minecraft.util.RegistryNamespaced;

import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import sun.misc.Unsafe;

/** Vanilla registry initialization following Galaxia's headless test bootstrap. */
public final class ProviderTestFixture {

    private static final Unsafe UNSAFE = unsafe();
    private static boolean initialized;

    private ProviderTestFixture() {}

    public static synchronized void bootstrap() {
        if (initialized) return;
        try {
            final Loader loader = allocate(Loader.class);
            for (final var entry : java.util.Map
                .of(
                    "mods",
                    new ArrayList<>(),
                    "namedMods",
                    new HashMap<>(),
                    "modController",
                    new LoadController(loader))
                .entrySet()) {
                final Field field = Loader.class.getDeclaredField(entry.getKey());
                field.setAccessible(true);
                field.set(loader, entry.getValue());
            }
            final Field instance = Loader.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, loader);

            final Field logSide = Class.forName("cpw.mods.fml.relauncher.FMLRelaunchLog")
                .getDeclaredField("side");
            logSide.setAccessible(true);
            if (logSide.get(null) == null) logSide.set(null, Side.SERVER);

            if (Block.blockRegistry.getObject("fire") == null) Block.registerBlocks();
            repairRegistryFields(Blocks.class, Block.blockRegistry);
            BlockFire.func_149843_e();

            if (Item.itemRegistry.getObject("diamond") == null) Item.registerItems();
            repairRegistryFields(Items.class, Item.itemRegistry);
            StatList.func_151178_a();

            final Method dispensers = Bootstrap.class.getDeclaredMethod("func_151353_a");
            dispensers.setAccessible(true);
            dispensers.invoke(null);
            final Field bootstrapped = Bootstrap.class.getDeclaredField("field_151355_a");
            bootstrapped.setAccessible(true);
            bootstrapped.setBoolean(null, true);
            initialized = true;
        } catch (final ReflectiveOperationException failure) {
            throw new AssertionError("Could not initialize Minecraft registries", failure);
        }
    }

    public static Item item(final String name) {
        bootstrap();
        final Item vanilla = (Item) Item.itemRegistry.getObject("minecraft:" + name);
        if (vanilla != null) return vanilla;
        final String id = "plannh_test:" + name;
        final Item existing = (Item) Item.itemRegistry.getObject(id);
        if (existing != null) return existing;
        final Item item = new Item().setUnlocalizedName(name);
        Item.itemRegistry.addObject(-1, id, item);
        return item;
    }

    public static <T> T allocate(final Class<T> type) {
        try {
            return type.cast(UNSAFE.allocateInstance(type));
        } catch (final InstantiationException failure) {
            throw new AssertionError("Could not allocate fixture " + type.getName(), failure);
        }
    }

    private static void repairRegistryFields(final Class<?> holder, final RegistryNamespaced registry) {
        for (final Field field : holder.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;
            final Object value = registry.getObject(field.getName());
            if (field.getType()
                .isInstance(value)) {
                // Earlier headless tests can load the holders before vanilla registers their contents.
                UNSAFE.putObjectVolatile(UNSAFE.staticFieldBase(field), UNSAFE.staticFieldOffset(field), value);
            }
        }
    }

    private static Unsafe unsafe() {
        try {
            final Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (final ReflectiveOperationException failure) {
            throw new AssertionError("Could not access the test fixture runtime", failure);
        }
    }
}
