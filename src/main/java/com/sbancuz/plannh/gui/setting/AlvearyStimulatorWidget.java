package com.sbancuz.plannh.gui.setting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.provider.bee.AlvearyStimulators;
import com.sbancuz.plannh.data.setting.ChoiceSettingDef;
import com.sbancuz.plannh.data.setting.EnumSettingDef;
import com.sbancuz.plannh.gui.GuiHelper;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;
import com.sbancuz.plannh.gui.tooltips.TooltipTheme;

import forestry.api.circuits.ICircuit;
import forestry.core.circuits.EnumCircuitBoardType;

/** Compact native item editors for the Extra Bees alveary upgrades. */
public final class AlvearyStimulatorWidget extends Flow {

    private static final int SLOT = 18;
    private static final int GAP = 3;

    private final MachineConfig cfg;
    private final RecipeContext ctx;
    private final int group;
    private final Consumer<Runnable> edit;
    private final EnumSettingDef<EnumCircuitBoardType> boardDef;
    private Map<String, ItemStack> tubes;
    private final String[] previewCircuits = new String[EnumCircuitBoardType.INTRICATE.getSockets()];
    private EnumCircuitBoardType previewType;
    private ItemStack previewStack;

    public AlvearyStimulatorWidget(final MachineConfig cfg, final RecipeContext ctx, final int group,
        final Consumer<Runnable> edit) {
        super(GuiAxis.X);
        this.cfg = cfg;
        this.ctx = ctx;
        this.group = group;
        this.edit = edit;
        this.boardDef = AlvearyStimulators.BOARD_TYPES.get(group);
        this.tubes = AlvearyStimulators.tubeChoices();

        name("alveary.stimulator." + (group + 1)).childPadding(GAP)
            .expanded()
            .coverChildrenHeight()
            .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN);
        child(
            new ItemChoiceWidget(this::preview, this::acceptsBoardOrTube, this::selectBoardOrTube, edit).picker(
                () -> Arrays.stream(EnumCircuitBoardType.VALUES)
                    .filter(this::boardFits)
                    .map(type -> {
                        final MachineConfig candidate = new MachineConfig(cfg);
                        candidate.set(boardDef, type);
                        return AlvearyStimulators.boardStack(candidate, ctx, group);
                    })
                    .filter(Objects::nonNull)
                    .toList(),
                () -> AlvearyStimulators.resetBoard(cfg, group))
                .name("alveary.stimulator.board")
                .size(SLOT)
                .hint("plannh.gui.bees.board_hint")
                .tooltipBuilder(tooltip -> {
                    final TooltipBuilder out = TooltipBuilder.create(tooltip);
                    final EnumCircuitBoardType type = cfg.value(boardDef, ctx);
                    final long used = AlvearyStimulators.CIRCUITS.get(group)
                        .stream()
                        .map(setting -> cfg.value(setting, ctx))
                        .filter(uid -> uid != null && !uid.isEmpty())
                        .count();

                    out.entry(
                        IKey.lang("plannh.gui.bees.stimulator"),
                        TooltipTheme.Role.IDENTITY,
                        Integer.toString(group + 1));
                    out.entry(
                        IKey.lang("plannh.gui.bees.sockets"),
                        TooltipTheme.Role.TUNABLE,
                        used + " / " + (type == null ? 0 : type.getSockets()));
                    out.entry(
                        IKey.lang("plannh.gui.bees.total_stimulator_energy"),
                        TooltipTheme.Role.POWER,
                        IKey.lang("plannh.bees.mj_per_cycle", AlvearyStimulators.mjPerWorkCycle(cfg, ctx)));
                    out.langRows("plannh.gui.bees.board_drop", "plannh.gui.bees.board_reset");

                    if (GuiHelper.shiftHeld()) {
                        out.group(
                            "plannh.bees.calculation",
                            rows -> rows.langRows("plannh.gui.bees.board_powered", "plannh.gui.bees.board_caps"));
                    } else out.langRow("plannh.bees.details_hint");
                    out.flush();
                }));
        final EnumCircuitBoardType type = cfg.value(boardDef, ctx);
        final int sockets = type == null ? 0 : type.getSockets();
        final Flow tubes = new Flow(GuiAxis.X).childPadding(GAP)
            .coverChildren();
        for (int socket = 0; socket < sockets; socket++) tubes.child(tubeSlot(socket));
        child(tubes);
    }

    private ItemChoiceWidget tubeSlot(final int socket) {
        final ChoiceSettingDef<String> circuitDef = AlvearyStimulators.CIRCUITS.get(group)
            .get(socket);
        return new ItemChoiceWidget(
            () -> tubes.get(cfg.value(circuitDef, ctx)),
            stack -> AlvearyStimulators.canSetTube(cfg, ctx, group, socket, stack),
            stack -> AlvearyStimulators.setTube(cfg, ctx, group, socket, stack),
            edit).picker(this::tubeChoices, () -> cfg.reset(circuitDef))
                .pickerTooltip((tooltip, tube) -> {
                    final ICircuit circuit = AlvearyStimulators.tubeCircuit(tube);
                    if (circuit == null) return;
                    final TooltipBuilder out = TooltipBuilder.create(tooltip);
                    out.separator();
                    AlvearyStimulators.circuitTooltip(out, circuit.getUID());
                    out.flush();
                })
                .name("alveary.stimulator." + (group + 1) + ".tubes.socket" + (socket + 1))
                .size(SLOT)
                .hint("plannh.gui.bees.tube_hint")
                .tooltipBuilder(tooltip -> {
                    final String uid = cfg.value(circuitDef, ctx);
                    final TooltipBuilder out = TooltipBuilder.create(tooltip);
                    if (uid == null || uid.isEmpty()) out.langRow("plannh.gui.bees.circuit_empty");
                    else AlvearyStimulators.circuitTooltip(out, uid);
                    out.langRow("plannh.gui.bees.tube_reset");
                    out.flush();
                });
    }

    private List<ItemStack> tubeChoices() {
        // Refresh on opening so late circuit registration cannot leave the picker incomplete.
        tubes = AlvearyStimulators.tubeChoices();
        return new ArrayList<>(tubes.values());
    }

    private boolean acceptsBoardOrTube(final ItemStack stack) {
        if (AlvearyStimulators.canSetBoard(cfg, group, stack)) return true;
        return firstTubeSocket(stack) >= 0;
    }

    private void selectBoardOrTube(final ItemStack stack) {
        if (AlvearyStimulators.canSetBoard(cfg, group, stack)) {
            AlvearyStimulators.setBoard(cfg, group, stack);
        } else {
            final int socket = firstTubeSocket(stack);
            if (socket < 0) return;
            AlvearyStimulators.setTube(cfg, ctx, group, socket, stack);
        }
    }

    private int firstTubeSocket(final ItemStack tube) {
        final EnumCircuitBoardType type = cfg.value(boardDef, ctx);
        if (type == null) return -1;
        for (int socket = 0; socket < type.getSockets(); socket++) {
            final ChoiceSettingDef<String> circuitDef = AlvearyStimulators.CIRCUITS.get(group)
                .get(socket);
            final String uid = cfg.value(circuitDef, ctx);
            if ((uid == null || uid.isEmpty()) && AlvearyStimulators.canSetTube(cfg, ctx, group, socket, tube))
                return socket;
        }
        return -1;
    }

    private boolean boardFits(final EnumCircuitBoardType type) {
        final MachineConfig candidate = new MachineConfig(cfg);
        candidate.set(boardDef, type);
        for (int socket = 0; socket < AlvearyStimulators.CIRCUITS.get(group)
            .size(); socket++) {
            final ChoiceSettingDef<String> circuitDef = AlvearyStimulators.CIRCUITS.get(group)
                .get(socket);
            final String uid = cfg.value(circuitDef, ctx);
            if (uid != null && !uid.isEmpty() && !AlvearyStimulators.canSetCircuit(candidate, ctx, group, socket, uid))
                return false;
        }
        return true;
    }

    private ItemStack preview() {
        final EnumCircuitBoardType currentType = cfg.value(boardDef, ctx);
        boolean changed = previewType != currentType;
        for (int socket = 0; socket < previewCircuits.length; socket++) {
            final ChoiceSettingDef<String> circuitDef = AlvearyStimulators.CIRCUITS.get(group)
                .get(socket);
            final String current = cfg.value(circuitDef, ctx);
            changed |= !Objects.equals(previewCircuits[socket], current);
            previewCircuits[socket] = current;
        }
        if (changed) {
            previewType = currentType;
            previewStack = null;
        }
        if (changed || previewStack == null) previewStack = AlvearyStimulators.boardStack(cfg, ctx, group);
        return previewStack;
    }
}
