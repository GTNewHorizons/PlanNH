package com.sbancuz.plannh.data.provider;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockOldLeaf;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.RegistryDelegate;
import sun.misc.Unsafe;

/** Minimal Minecraft state needed by provider tests without bootstrapping the game. */
public final class ProviderTestFixture {

    private static final Unsafe UNSAFE = unsafe();

    private ProviderTestFixture() {}

    static void bootstrap() {
        installBlock("dirt");
        installBlock("water");
        installBlock("lava");
        installMissingBlocks();

        installItemField("coal");
        installItemField("iron_ingot");
        installItemField("redstone");
        installItemField("water_bucket");
        installMissingItems();
        installLoader();

        try {
            final Item bucket = createItem("bucket");
            final Item waterBucket = itemField("water_bucket");
            waterBucket.setContainerItem(bucket);
        } catch (final ReflectiveOperationException failure) {
            throw new AssertionError("Could not initialize the container item fixture", failure);
        }
    }

    public static Item item(final String name) {
        bootstrap();
        return createItem(name);
    }

    public static <T extends Item> T item(final Class<T> type, final String name) {
        bootstrap();
        return createItem(type, name);
    }

    public static <T> T allocate(final Class<T> type) {
        try {
            return type.cast(UNSAFE.allocateInstance(type));
        } catch (final InstantiationException failure) {
            throw new AssertionError("Could not allocate fixture " + type.getName(), failure);
        }
    }

    private static void installLoader() {
        try {
            final Field instance = Loader.class.getDeclaredField("instance");
            instance.setAccessible(true);
            if (instance.get(null) == null) setStatic(instance, UNSAFE.allocateInstance(Loader.class));
            final Field namedMods = Loader.class.getDeclaredField("namedMods");
            namedMods.setAccessible(true);
            if (namedMods.get(instance.get(null)) == null) namedMods.set(instance.get(null), java.util.Map.of());
        } catch (final ReflectiveOperationException failure) {
            throw new AssertionError("Could not initialize the Forge loader fixture", failure);
        }
    }

    private static void installBlock(final String fieldName) {
        try {
            final Field field = Blocks.class.getField(fieldName);
            field.setAccessible(true);
            if (field.get(null) == null) {
                final Block block = allocateBlock(field.getType(), fieldName);
                setStatic(field, block);
            }
        } catch (final ReflectiveOperationException failure) {
            throw new AssertionError("Could not initialize block fixture " + fieldName, failure);
        }
    }

    private static void installMissingBlocks() {
        try {
            for (final Field field : Blocks.class.getFields()) {
                if (!Modifier.isStatic(field.getModifiers()) || !Block.class.isAssignableFrom(field.getType()))
                    continue;
                field.setAccessible(true);
                if (field.get(null) == null && !Modifier.isAbstract(
                    field.getType()
                        .getModifiers()))
                    setStatic(field, allocateBlock(field.getType(), field.getName()));
                else if (field.get(null) == null && field.getType() == BlockLeaves.class)
                    setStatic(field, allocateBlock(BlockOldLeaf.class, field.getName()));
                else if (field.get(null) == null && field.getType() == BlockSlab.class)
                    setStatic(field, allocateBlock(FixtureSlab.class, field.getName()));
            }
        } catch (final ReflectiveOperationException failure) {
            throw new AssertionError("Could not initialize vanilla block fixtures", failure);
        }
    }

    private static Block allocateBlock(final Class<?> type, final String name) {
        try {
            final Block block = (Block) UNSAFE.allocateInstance(type);
            final Field unlocalizedName = Block.class.getDeclaredField("unlocalizedName");
            unlocalizedName.setAccessible(true);
            UNSAFE.putObject(block, UNSAFE.objectFieldOffset(unlocalizedName), name);
            return block;
        } catch (final ReflectiveOperationException failure) {
            throw new AssertionError("Could not allocate block fixture " + name, failure);
        }
    }

    private static void installItemField(final String fieldName) {
        try {
            final Field field = Items.class.getField(fieldName);
            field.setAccessible(true);
            if (field.get(null) == null) setStatic(field, createItem(fieldName));
        } catch (final ReflectiveOperationException failure) {
            throw new AssertionError("Could not initialize item fixture " + fieldName, failure);
        }
    }

    private static void installMissingItems() {
        try {
            final Field delegate = Item.class.getField("delegate");
            for (final Field field : Items.class.getFields()) {
                if (!Modifier.isStatic(field.getModifiers()) || !Item.class.isAssignableFrom(field.getType())) continue;
                field.setAccessible(true);
                if (field.get(null) != null) continue;

                final Item item = (Item) UNSAFE.allocateInstance(field.getType());
                final Item named = createItem(field.getName());
                UNSAFE.putObject(item, UNSAFE.objectFieldOffset(delegate), named.delegate);
                setStatic(field, item);
            }
        } catch (final ReflectiveOperationException failure) {
            throw new AssertionError("Could not initialize vanilla item fixtures", failure);
        }
    }

    private static Item itemField(final String fieldName) throws ReflectiveOperationException {
        final Field field = Items.class.getField(fieldName);
        field.setAccessible(true);
        return (Item) field.get(null);
    }

    private static Item createItem(final String name) {
        return createItem(Item.class, name);
    }

    private static <T extends Item> T createItem(final Class<T> type, final String name) {
        try {
            final T item = type.cast(UNSAFE.allocateInstance(type));
            final Field delegate = Item.class.getField("delegate");
            final Field unlocalizedName = Item.class.getDeclaredField("unlocalizedName");
            unlocalizedName.setAccessible(true);
            UNSAFE.putObject(item, UNSAFE.objectFieldOffset(unlocalizedName), name);
            UNSAFE
                .putObject(item, UNSAFE.objectFieldOffset(delegate), new RegistryDelegate.Delegate<>(item, Item.class));
            return item;
        } catch (final ReflectiveOperationException failure) {
            throw new AssertionError("Could not initialize item fixture " + name, failure);
        }
    }

    private static void setStatic(final Field field, final Object value) {
        UNSAFE.putObject(UNSAFE.staticFieldBase(field), UNSAFE.staticFieldOffset(field), value);
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

    private static final class FixtureSlab extends BlockSlab {

        private FixtureSlab() {
            super(false, Material.wood);
        }

        @Override
        public String func_150002_b(final int metadata) {
            return "wooden_slab";
        }
    }
}
