package com.sbancuz.plannh.data.provider.bee;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.setting.ChoiceSettingDef;
import com.sbancuz.plannh.data.setting.EnumSettingDef;
import com.sbancuz.plannh.data.setting.SettingDef;
import com.sbancuz.plannh.gui.GuiHelper;
import com.sbancuz.plannh.gui.setting.AlvearyStimulatorWidget;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;
import com.sbancuz.plannh.gui.tooltips.TooltipTheme;

import binnie.core.machines.power.PowerSystem;
import binnie.extrabees.apiary.machine.stimulator.StimulatorCircuit;
import forestry.api.apiculture.BeeManager;
import forestry.api.apiculture.DefaultBeeModifier;
import forestry.api.apiculture.IBeeGenome;
import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.IBeeModifier;
import forestry.api.circuits.ChipsetManager;
import forestry.api.circuits.ICircuit;
import forestry.api.circuits.ICircuitBoard;
import forestry.api.circuits.ICircuitLayout;
import forestry.core.circuits.EnumCircuitBoardType;
import forestry.core.circuits.ItemCircuitBoard;
import forestry.core.circuits.SolderManager;
import forestry.core.items.EnumElectronTube;
import forestry.plugins.PluginCore;

/** Extra Bees stimulator boards that can be installed as alveary modules. */
public final class AlvearyStimulators {

    // TODO: Open an upstream Forestry PR to expose the circuit board socket limit.
    private static final int SOCKETS = 4;
    private static final String STIMULATOR_LAYOUT = "binnie.circuitLayoutStimulator";

    public static final List<List<ChoiceSettingDef<String>>> CIRCUITS = List.of(sockets(0), sockets(1), sockets(2));
    public static final List<EnumSettingDef<EnumCircuitBoardType>> BOARD_TYPES = List
        .of(boardType(0), boardType(1), boardType(2));

    public record Resolved(List<IBeeModifier> modifiers, double rfPerWorkCycle) {}

    private AlvearyStimulators() {}

    private static EnumSettingDef<EnumCircuitBoardType> boardType(final int index) {
        return new EnumSettingDef<>(
            "bee_alveary_stimulator_" + (index + 1) + "_board_type",
            EnumCircuitBoardType.INTRICATE,
            EnumCircuitBoardType.class) {

            @Override
            public IWidget settingsWidget(final MachineConfig cfg, final RecipeContext ctx,
                final Consumer<Runnable> edit) {
                return new AlvearyStimulatorWidget(cfg, ctx, index, edit);
            }

            @Override
            public String getEditorLabel() {
                return "";
            }

            @Override
            public boolean isFullWidthEditor() {
                return true;
            }

            @Override
            public List<? extends SettingDef<?>> editedSettings() {
                final List<ChoiceSettingDef<String>> circuits = CIRCUITS.get(index);
                final List<SettingDef<?>> edited = new ArrayList<>(1 + circuits.size());
                edited.add(this);
                edited.addAll(circuits);
                return edited;
            }
        };
    }

    private static List<ChoiceSettingDef<String>> sockets(final int board) {
        return IntStream.range(0, SOCKETS)
            .mapToObj(socket -> circuit(board, socket))
            .toList();
    }

    private static ChoiceSettingDef<String> circuit(final int board, final int socket) {
        return new ChoiceSettingDef<>(
            "bee_alveary_stimulator_" + (board + 1) + "_circuit_" + (socket + 1),
            "",
            String.class,
            AlvearyStimulators::circuitChoices,
            AlvearyStimulators::circuitDisplay) {

            @Override
            public Consumer<TooltipBuilder> tooltip(final String uid) {
                return out -> {
                    if (uid == null || uid.isEmpty()) super.tooltip(uid).accept(out);
                    else circuitTooltip(out, uid);
                };
            }
        };
    }

    public static List<String> circuitChoices() {
        if (ChipsetManager.circuitRegistry == null) return List.of("");
        final List<String> choices = new ArrayList<>();
        choices.add("");
        for (final ICircuit circuit : ChipsetManager.circuitRegistry.getRegisteredCircuits()
            .values()) {
            if (circuit instanceof StimulatorCircuit) choices.add(circuit.getUID());
        }
        return choices.stream()
            .distinct()
            .sorted()
            .toList();
    }

    public static String circuitDisplay(final String uid) {
        if (uid == null || uid.isEmpty()) return StatCollector.translateToLocal("plannh.settings.bee_no_stimulator");
        final ICircuit circuit = registeredCircuit(uid);
        if (circuit == null) return uid;
        final String translation = "for." + circuit.getName();
        final String label = StatCollector.translateToLocal(translation);
        return label.equals(translation) ? circuit.getName() : label;
    }

    public static void circuitTooltip(final TooltipBuilder out, final String uid) {
        final ICircuit circuit = registeredCircuit(uid);
        if (!(circuit instanceof StimulatorCircuit stimulator)) return;

        out.entry(IKey.lang("plannh.gui.bees.circuit"), TooltipTheme.Role.IDENTITY, circuitDisplay(uid));
        out.entry(
            IKey.lang("plannh.bees.stimulator_energy"),
            TooltipTheme.Role.POWER,
            IKey.lang("plannh.bees.mj_per_cycle", stimulator.getPowerUsage()));

        if (GuiHelper.shiftHeld()) {
            final List<String> effects = new ArrayList<>();
            stimulator.addTooltip(effects);
            out.group(
                "plannh.gui.bees.circuit_effects",
                rows -> { for (final String line : effects) rows.row(IKey.str(line)); });
        } else out.langRow("plannh.bees.details_hint");
    }

    private static ICircuit registeredCircuit(final String uid) {
        return ChipsetManager.circuitRegistry == null ? null : ChipsetManager.circuitRegistry.getCircuit(uid);
    }

    private static StimulatorCircuit selectedCircuit(final String uid) {
        final ICircuit circuit = registeredCircuit(uid);
        if (!(circuit instanceof StimulatorCircuit stimulator))
            throw new IllegalArgumentException("Invalid alveary stimulator circuit: " + uid);
        return stimulator;
    }

    public static ItemStack boardStack(final MachineConfig cfg, final RecipeContext ctx, final int group) {
        if (PluginCore.items == null || PluginCore.items.circuitboards == null) return null;
        final EnumCircuitBoardType type = boardType(cfg, ctx, group);
        final ICircuitLayout layout = stimulatorLayout();
        if (layout == null) return null;
        return ItemCircuitBoard.createCircuitboard(type, layout, boardCircuitArray(cfg, ctx, group));
    }

    public static Map<String, ItemStack> tubeChoices() {
        if (PluginCore.items == null || PluginCore.items.tubes == null) return Map.of();

        final Map<String, ItemStack> choices = new LinkedHashMap<>();
        for (final EnumElectronTube type : EnumElectronTube.VALUES) {
            final ItemStack stack = PluginCore.items.tubes.get(type, 1);
            final ICircuit circuit = tubeCircuit(stack);
            if (circuit instanceof StimulatorCircuit) choices.putIfAbsent(circuit.getUID(), stack);
        }
        return Collections.unmodifiableMap(choices);
    }

    public static boolean canSetTube(final MachineConfig cfg, final RecipeContext ctx, final int group,
        final int socket, final ItemStack tube) {
        try {
            final ICircuit circuit = tubeCircuit(tube);
            return circuit instanceof StimulatorCircuit && canSetCircuit(cfg, ctx, group, socket, circuit.getUID());
        } catch (final RuntimeException failure) {
            return false;
        }
    }

    public static void setTube(final MachineConfig cfg, final RecipeContext ctx, final int group, final int socket,
        final ItemStack tube) {
        final ICircuit circuit;
        try {
            circuit = tubeCircuit(tube);
        } catch (final RuntimeException failure) {
            throw new IllegalArgumentException("Invalid alveary stimulator tube", failure);
        }
        if (!(circuit instanceof StimulatorCircuit))
            throw new IllegalArgumentException("Invalid alveary stimulator tube");
        setCircuit(cfg, ctx, group, socket, circuit.getUID());
    }

    public static boolean canSetBoard(final MachineConfig cfg, final int group, final ItemStack stack) {
        try {
            return cfg != null && group >= 0 && group < BOARD_TYPES.size() && decodeBoard(stack) != null;
        } catch (final RuntimeException failure) {
            return false;
        }
    }

    public static void setBoard(final MachineConfig cfg, final int group, final ItemStack stack) {
        final BoardSelection board;
        try {
            board = decodeBoard(stack);
        } catch (final RuntimeException failure) {
            throw new IllegalArgumentException("Invalid alveary stimulator board", failure);
        }
        if (cfg == null || group < 0 || group >= BOARD_TYPES.size() || board == null)
            throw new IllegalArgumentException("Invalid alveary stimulator board");

        cfg.set(BOARD_TYPES.get(group), board.type());
        final List<ChoiceSettingDef<String>> sockets = CIRCUITS.get(group);
        for (int socket = 0; socket < SOCKETS; socket++) {
            cfg.set(sockets.get(socket), board.uids()[socket]);
        }
    }

    public static boolean canSetCircuit(final MachineConfig cfg, final RecipeContext ctx, final int group,
        final int socket, final String uid) {
        try {
            return validCircuitChange(cfg, ctx, group, socket, uid);
        } catch (final RuntimeException failure) {
            return false;
        }
    }

    public static void setCircuit(final MachineConfig cfg, final RecipeContext ctx, final int group, final int socket,
        final String uid) {
        if (!canSetCircuit(cfg, ctx, group, socket, uid))
            throw new IllegalArgumentException("Invalid alveary stimulator circuit: " + uid);

        final List<ChoiceSettingDef<String>> sockets = CIRCUITS.get(group);
        cfg.set(sockets.get(socket), uid);
    }

    public static void resetBoard(final MachineConfig cfg, final int group) {
        if (cfg == null || group < 0 || group >= BOARD_TYPES.size())
            throw new IllegalArgumentException("Invalid alveary stimulator board");
        cfg.reset(BOARD_TYPES.get(group));
        for (final ChoiceSettingDef<String> setting : CIRCUITS.get(group)) cfg.reset(setting);
    }

    public static Resolved resolve(final MachineConfig cfg, final RecipeContext ctx) {
        final List<IBeeModifier> modifiers = new ArrayList<>();
        long totalMj = 0;

        for (int board = 0; board < CIRCUITS.size(); board++) {
            final List<StimulatorCircuit> circuits = boardCircuits(cfg, ctx, board);
            if (circuits.isEmpty()) continue;

            // Each board has its own bonus caps; power usage is summed across all boards.
            modifiers.add(boardModifier(circuits));
            for (final StimulatorCircuit circuit : circuits) totalMj += circuit.getPowerUsage();
        }

        final double rf = PowerSystem.RF.convertTo(PowerSystem.MJ.convertFrom(totalMj));
        return new Resolved(List.copyOf(modifiers), rf);
    }

    public static long mjPerWorkCycle(final MachineConfig cfg, final RecipeContext ctx) {
        long total = 0;
        for (int board = 0; board < CIRCUITS.size(); board++) {
            for (final StimulatorCircuit circuit : boardCircuits(cfg, ctx, board)) total += circuit.getPowerUsage();
        }
        return total;
    }

    private static List<StimulatorCircuit> boardCircuits(final MachineConfig cfg, final RecipeContext ctx,
        final int board) {
        final List<StimulatorCircuit> circuits = new ArrayList<>();
        for (final ICircuit circuit : boardCircuitArray(cfg, ctx, board)) {
            if (circuit != null) circuits.add((StimulatorCircuit) circuit);
        }
        return circuits;
    }

    private static EnumCircuitBoardType boardType(final MachineConfig cfg, final RecipeContext ctx, final int board) {
        if (cfg == null || board < 0 || board >= BOARD_TYPES.size())
            throw new IllegalArgumentException("Invalid alveary stimulator board");
        final EnumSettingDef<EnumCircuitBoardType> def = BOARD_TYPES.get(board);
        final EnumCircuitBoardType type = cfg.value(def, ctx);
        if (type == null) throw new IllegalArgumentException("Invalid alveary stimulator board type");
        return type;
    }

    private static ICircuit[] boardCircuitArray(final MachineConfig cfg, final RecipeContext ctx, final int board) {
        final EnumCircuitBoardType type = boardType(cfg, ctx, board);
        final ICircuit[] circuits = new ICircuit[SOCKETS];
        final Map<String, Integer> counts = new HashMap<>();

        for (int socket = 0; socket < SOCKETS; socket++) {
            final ChoiceSettingDef<String> def = CIRCUITS.get(board)
                .get(socket);
            final String uid = cfg.value(def, ctx);
            if (uid == null || uid.isEmpty()) continue;

            if (socket >= type.getSockets())
                throw new IllegalArgumentException("Alveary stimulator circuit exceeds board sockets: " + uid);
            final StimulatorCircuit circuit = selectedCircuit(uid);
            if (counts.merge(uid, 1, Integer::sum) > circuit.getLimit())
                throw new IllegalArgumentException("Alveary stimulator circuit exceeds its limit: " + uid);

            circuits[socket] = circuit;
        }

        return circuits;
    }

    private static boolean validCircuitChange(final MachineConfig cfg, final RecipeContext ctx, final int group,
        final int socket, final String uid) {
        if (cfg == null || group < 0 || group >= CIRCUITS.size() || socket < 0 || socket >= SOCKETS || uid == null)
            return false;
        final EnumCircuitBoardType type = boardType(cfg, ctx, group);
        if (socket >= type.getSockets()) return false;

        final Map<String, Integer> counts = new HashMap<>();
        for (int currentSocket = 0; currentSocket < SOCKETS; currentSocket++) {
            final ChoiceSettingDef<String> def = CIRCUITS.get(group)
                .get(currentSocket);
            final String currentUid = currentSocket == socket ? uid : cfg.value(def, ctx);
            if (currentUid == null || currentUid.isEmpty()) continue;
            if (currentSocket >= type.getSockets()) return false;
            final StimulatorCircuit circuit = selectedCircuit(currentUid);
            if (counts.merge(currentUid, 1, Integer::sum) > circuit.getLimit()) return false;
        }
        return true;
    }

    private static ICircuitLayout stimulatorLayout() {
        return ChipsetManager.circuitRegistry == null ? null
            : ChipsetManager.circuitRegistry.getLayout(STIMULATOR_LAYOUT);
    }

    public static ICircuit tubeCircuit(final ItemStack tube) {
        final ICircuitLayout layout = stimulatorLayout();
        if (layout == null || tube == null) return null;
        return SolderManager.getCircuit(layout, tube);
    }

    private static BoardSelection decodeBoard(final ItemStack stack) {
        if (stack == null || ChipsetManager.circuitRegistry == null) return null;

        final var registry = ChipsetManager.circuitRegistry;
        if (!registry.isChipset(stack)) return null;

        // Validate metadata before calling Forestry's permissive NBT decoder.
        final int itemDamage = stack.getItemDamage();
        if (itemDamage < 0 || itemDamage >= EnumCircuitBoardType.VALUES.length) return null;
        final EnumCircuitBoardType itemType = EnumCircuitBoardType.VALUES[itemDamage];

        final String[] uids = new String[SOCKETS];
        Arrays.fill(uids, "");
        final var tag = stack.getTagCompound();
        if (tag == null) return new BoardSelection(itemType, uids);
        if (!tag.hasKey("T", 99) || !tag.hasKey("LY", 8) || !STIMULATOR_LAYOUT.equals(tag.getString("LY"))) return null;

        final short rawType = tag.getShort("T");
        if (rawType < 0 || rawType >= EnumCircuitBoardType.VALUES.length
            || EnumCircuitBoardType.VALUES[rawType] != itemType) return null;

        final ICircuitBoard decoded;
        try {
            decoded = registry.getCircuitboard(stack);
        } catch (final RuntimeException failure) {
            return null;
        }
        if (decoded == null) return null;
        final ICircuit[] circuits = decoded.getCircuits();
        if (circuits == null || circuits.length != SOCKETS) return null;

        // Imported boards must obey the same socket and circuit limits as the editor.
        final Map<String, Integer> counts = new HashMap<>();
        for (int socket = 0; socket < SOCKETS; socket++) {
            final String key = "CA.I" + socket;
            if (!tag.hasKey(key)) continue;
            if (!tag.hasKey(key, 8)) return null;
            final String uid = tag.getString(key);
            final ICircuit circuit = circuits[socket];
            if (uid.isEmpty() || !(circuit instanceof StimulatorCircuit stimulator) || !uid.equals(circuit.getUID()))
                return null;
            if (socket >= itemType.getSockets()) return null;
            if (counts.merge(uid, 1, Integer::sum) > stimulator.getLimit()) return null;
            uids[socket] = uid;
        }

        return new BoardSelection(itemType, uids);
    }

    private record BoardSelection(EnumCircuitBoardType type, String[] uids) {}

    /** Start each board at production 0 and lifespan 1, regardless of preceding frames or boards. */
    static IBeeModifier boardModifier(final List<? extends IBeeModifier> circuits) {
        final List<IBeeModifier> snapshot = List.copyOf(circuits);
        // Forestry's combiner only needs the modifier list, not a live housing or world.
        final IBeeHousing housing = (IBeeHousing) Proxy.newProxyInstance(
            IBeeHousing.class.getClassLoader(),
            new Class<?>[] { IBeeHousing.class },
            (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return switch (method.getName()) {
                        case "equals" -> proxy == (args == null ? null : args[0]);
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "BeeModifiers housing proxy";
                        default -> throw new UnsupportedOperationException(method.toString());
                    };
                }
                if ("getBeeModifiers".equals(method.getName())) return snapshot;
                throw new UnsupportedOperationException("No world or inventory in a bee modifier combiner");
            });
        final IBeeModifier inner = BeeManager.beeRoot.createBeeHousingModifier(housing);
        return new DefaultBeeModifier() {

            @Override
            public float getProductionModifier(final IBeeGenome genome, final float currentModifier) {
                return inner.getProductionModifier(genome, 0);
            }

            @Override
            public float getLifespanModifier(final IBeeGenome genome, final IBeeGenome mate,
                final float currentModifier) {
                return inner.getLifespanModifier(genome, mate, 1);
            }
        };
    }
}
