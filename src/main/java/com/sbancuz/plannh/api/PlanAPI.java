package com.sbancuz.plannh.api;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.sbancuz.plannh.PlanNH;
import com.sbancuz.plannh.client.MessageHandler;
import com.sbancuz.plannh.data.flowchart.Graph;
import com.sbancuz.plannh.data.flowchart.Plan;
import com.sbancuz.plannh.data.flowchart.UndoHistory;
import com.sbancuz.plannh.data.serialization.Serializer;

import codechicken.nei.NEIClientConfig;
import codechicken.nei.NEIClientUtils;

public final class PlanAPI {

    /** NBT key used to store the encoded graph in the share ItemStack. */
    public static final String PLANNH_DATA_KEY = "plannh_data";

    private static final boolean DEBUG_MODE = true;
    private static boolean primaryUnreadable;

    /** Shows a localized message in chat and, while the planner is open, beside the cursor. */
    public static void showMessage(final String key, final Object... args) {
        showMessage(IKey.lang(key, args));
    }

    /** Accepts literal, localized, or composed MUI2 text, formatted once on the client thread for both destinations. */
    public static void showMessage(final IKey message) {
        MessageHandler.show(message);
    }

    public static UndoHistory undoHistory() {
        return Plan.getActiveGraph()
            .getUndoHistory();
    }

    /** Runs {@code edit} as one undo step; no-op edits leave no trace. */
    public static void recordEdit(final Graph graph, final Runnable edit) {
        final UndoHistory history = graph.getUndoHistory();
        final String before = history.beginEdit(graph);
        edit.run();
        history.commitEdit(before, graph);
    }

    /**
     * Encodes the given graph and sends it as an NEI item-link chat message.
     * Other PlanNH clients see an import link; vanilla clients see a dirt-item
     * tooltip with a bookmark prompt.
     */
    public static void shareGraph(final Graph graph) {
        try {
            final String encoded = Serializer.encodeGraph(graph);
            final ItemStack stack = createShareStack();
            stack.getTagCompound()
                .setString(PLANNH_DATA_KEY, encoded);
            NEIClientUtils.sendChatItemLink(stack.writeToNBT(new NBTTagCompound()));
        } catch (final Exception | LinkageError failure) {
            PlanNH.LOG.error("Failed to share graph", failure);
            showMessage("plannh.error.export");
        }
    }

    /** Copies the serialised graph to the system clipboard. */
    public static boolean copyToClipboard(final Graph graph) {
        try {
            GuiScreen.setClipboardString(Serializer.encodeGraph(graph));
            return true;
        } catch (final Exception | LinkageError failure) {
            PlanNH.LOG.error("Failed to copy graph to clipboard", failure);
            showMessage("plannh.error.export");
            return false;
        }
    }

    /**
     * Reads graph data from the system clipboard and deserialises it.
     *
     * @return the deserialised graph, or {@code null} if clipboard is empty
     *         or the data is invalid.
     */
    @Nullable
    public static Graph importFromClipboard() {
        try {
            final String data = GuiScreen.getClipboardString();
            if (data == null || data.isEmpty()) return null;
            final Graph graph = Serializer.decodeGraph(data);
            if (graph == null) showMessage("plannh.error.import");
            return graph;
        } catch (final Exception | LinkageError e) {
            PlanNH.LOG.error("Failed to import graph from clipboard", e);
            showMessage("plannh.error.import");
            return null;
        }
    }

    /**
     * Extracts PlanNH share data from a serialised ItemStack NBT string and
     * deserialises it. This is the receiver side of {@link #shareGraph(Graph)}.
     *
     * @param nbtString the full serialised NBT from the /nei_bookmark command
     * @return the deserialised graph, or {@code null} if the NBT doesn't
     *         contain a {@code plannh_data} tag or the data is invalid.
     */
    @Nullable
    public static Graph importFromNBT(final String nbtString) {
        try {
            final NBTTagCompound nbt = (NBTTagCompound) JsonToNBT.func_150315_a(nbtString);
            // if (!nbt.hasKey("tag")) return null;
            // final NBTTagCompound tag = nbt.getCompoundTag("tag");
            if (!nbt.hasKey(PLANNH_DATA_KEY)) return null;
            return Serializer.decodeGraph(nbt.getString(PLANNH_DATA_KEY));
        } catch (final Exception | LinkageError e) {
            PlanNH.LOG.error("Failed to import graph from NBT", e);
            showMessage("plannh.error.import");
            return null;
        }
    }

    /**
     * Checks whether a serialised ItemStack NBT string contains PlanNH share
     * data, without fully deserialising the graph.
     */
    public static boolean containsPlanNHData(@Nonnull final String nbtString) {
        try {
            final NBTTagCompound nbt = (NBTTagCompound) JsonToNBT.func_150315_a(nbtString);
            return nbt.hasKey("tag") && nbt.getCompoundTag("tag")
                .hasKey(PLANNH_DATA_KEY);
        } catch (final Exception | LinkageError e) {
            return false;
        }
    }

    /**
     * Wraps a deserialised graph in a new "Imported" slot, switches to it,
     * and persists. Does not open any GUI.
     */
    public static void importGraph(@Nonnull final Graph graph) {
        final Plan plan = Plan.getInstance();
        graph.setName(StatCollector.translateToLocal("plannh.share.slot_imported"));
        plan.getGraphs()
            .add(graph);
        plan.setActiveIndex(
            plan.getGraphs()
                .size() - 1);
        save();
    }

    /**
     * Creates a dirt ItemStack with PlanNH share display properties
     * (localised name and lore). Both the sender (share button) and
     * receiver (chat hover tooltip) build the same item so the visual
     * is consistent on both sides.
     */
    @Nonnull
    public static ItemStack createShareStack() {
        final ItemStack stack = new ItemStack(Blocks.dirt);
        final NBTTagCompound display = new NBTTagCompound();
        display.setString("Name", StatCollector.translateToLocal("plannh.share.item_name"));
        final NBTTagList lore = new NBTTagList();
        lore.appendTag(new NBTTagString(StatCollector.translateToLocal("plannh.share.lore_shared")));
        lore.appendTag(new NBTTagString(StatCollector.translateToLocal("plannh.share.click_to_import")));
        display.setTag("Lore", lore);
        stack.setTagInfo("display", display);
        return stack;
    }

    public static boolean save() {
        try {
            final Plan plan = Plan.getInstance();
            final File saveFile = getSaveFile(plan);
            final String encoded = Serializer.encodePlan(plan);
            atomicWrite(saveFile, encoded, primaryUnreadable ? null : getBackupSaveFile(plan));
            primaryUnreadable = false;
            if (DEBUG_MODE) {
                try {
                    atomicWrite(getDebugSaveFile(plan), Serializer.encodePlanDebug(plan), null);
                } catch (final Exception | LinkageError e) {
                    PlanNH.LOG.error("Failed to write debug save", e);
                    showMessage("plannh.error.debug_save");
                }
            }
            return true;
        } catch (final Exception | LinkageError e) {
            PlanNH.LOG.error("Failed to save plan", e);
            showMessage("plannh.error.save");
            return false;
        }
    }

    public static File getSaveFile() {
        final Minecraft mc = Minecraft.getMinecraft();
        final String worldName = NEIClientConfig.getWorldPath();
        if (worldName != null && !worldName.isEmpty()) {
            return new File(mc.mcDataDir, "saves/NEI/" + worldName + "/plannh/plannh.dat");
        }
        return new File(mc.mcDataDir, "plannh/plannh.dat");
    }

    public static File getSaveFile(final Plan plan) {
        final String boundPath = plan.getPersistencePath();
        return boundPath == null ? getSaveFile() : new File(boundPath);
    }

    public static File getDebugSaveFile() {
        final Minecraft mc = Minecraft.getMinecraft();
        final String worldName = NEIClientConfig.getWorldPath();
        if (worldName != null && !worldName.isEmpty()) {
            return new File(mc.mcDataDir, "saves/NEI/" + worldName + "/plannh/plannh_debug.json");
        }
        return new File(mc.mcDataDir, "plannh/plannh_debug.json");
    }

    public static File getDebugSaveFile(final Plan plan) {
        final File saveFile = getSaveFile(plan);
        return new File(saveFile.getParentFile(), "plannh_debug.json");
    }

    public static File getBackupSaveFile() {
        return new File(getSaveFile().getPath() + ".bak");
    }

    public static File getBackupSaveFile(final Plan plan) {
        return new File(getSaveFile(plan).getPath() + ".bak");
    }

    public static File getBackupSaveFile(final File saveFile) {
        return new File(saveFile.getPath() + ".bak");
    }

    private static File getCorruptSaveFile(final File saveFile) {
        return new File(saveFile.getPath() + ".corrupt");
    }

    /** Called by Plan before trying primary and backup files. */
    public static void beginLoad() {
        primaryUnreadable = false;
    }

    /** Keeps an unreadable primary available for manual recovery instead of moving or deleting it. */
    public static void preserveUnreadablePrimary() {
        preserveUnreadablePrimary(getSaveFile());
    }

    public static void preserveUnreadablePrimary(final File primary) {
        primaryUnreadable = true;
        if (!primary.isFile()) return;
        try {
            preserveUnreadable(primary, getCorruptSaveFile(primary));
        } catch (final Exception | LinkageError e) {
            PlanNH.LOG.error("Failed to preserve unreadable save", e);
            showMessage("plannh.error.load");
        }
    }

    static void atomicWrite(final File target, final String content, @Nullable final File backup) throws IOException {
        final File parent = target.getParentFile();
        if (parent == null || (!parent.exists() && !parent.mkdirs()) || !parent.isDirectory()) {
            throw new IOException("Unable to create save directory " + parent);
        }

        if (backup != null && target.isFile()) {
            atomicCopy(target.toPath(), backup.toPath());
        }

        final Path temporary = Files.createTempFile(parent.toPath(), target.getName() + ".", ".tmp");
        try {
            Files.writeString(
                temporary,
                content,
                StandardCharsets.UTF_8,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);
            force(temporary);
            moveReplacing(temporary, target.toPath());
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    static void atomicCopy(final Path source, final Path target) throws IOException {
        final Path temporary = Files.createTempFile(
            target.getParent(),
            target.getFileName()
                .toString() + ".",
            ".tmp");
        try {
            Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
            force(temporary);
            moveReplacing(temporary, target);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void force(final Path path) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE)) {
            channel.force(true);
        }
    }

    private static void moveReplacing(final Path source, final Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (final java.nio.file.AtomicMoveNotSupportedException e) {
            showMessage("plannh.warning.non_atomic_save");
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static void preserveUnreadable(final File primary, final File corrupt) throws IOException {
        Files.copy(primary.toPath(), corrupt.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
