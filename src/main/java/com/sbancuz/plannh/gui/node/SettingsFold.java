package com.sbancuz.plannh.gui.node;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.flowchart.Node;
import com.sbancuz.plannh.data.setting.SettingDef;
import com.sbancuz.plannh.gui.GuiIcon;
import com.sbancuz.plannh.gui.LangScope;
import com.sbancuz.plannh.gui.common.FlowchartFlow;
import com.sbancuz.plannh.gui.common.FlowchartTextWidget;
import com.sbancuz.plannh.gui.common.MiddleClickReset;
import com.sbancuz.plannh.gui.theme.PlannhThemes;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;
import com.sbancuz.plannh.gui.tooltips.TooltipTheme;

public class SettingsFold extends NodeFold {

    private static final LangScope SETTINGS = LangScope.ROOT.child("gui.node.settings");
    private static final LangScope OVERRIDES = SETTINGS.child("overrides");
    // the copied settings stay while the client runs, so the player can paste on a node of another chart too
    private static @Nullable MachineConfig copiedSettings;
    private static final int OVERRIDES_TOGGLE = 12;

    private final Node data;
    private boolean overridesOpen;
    private boolean overridesWereExplicit;
    private MachineConfig savedConfig;
    private MachineProfile savedProfile;
    private long savedVersion = Long.MIN_VALUE;

    public SettingsFold(final NodeWidget node) {
        super(
            node,
            () -> node.getData()
                .isSettingsOpen());
        name("fold.settings");

        data = node.getData();
        savedConfig = data.getMachineConfig();
        savedProfile = savedConfig.getProfile();

        onUpdateListener(w -> {
            final MachineConfig currentConfig = data.getMachineConfig();
            final MachineProfile currentProfile = currentConfig.getProfile();
            final long currentVersion = node.getCanvas()
                .getGraph()
                .getVersion();
            if (savedConfig != currentConfig || savedProfile != currentProfile || savedVersion != currentVersion) {
                savedConfig = currentConfig;
                savedProfile = currentProfile;
                savedVersion = currentVersion;
                markStale();
            }
        }, true);
    }

    @Override
    protected void rebuild() {
        removeAll();

        final MachineConfig cfg = data.getMachineConfig();
        final RecipeContext ctx = data.recipeContext();
        final List<SettingDef<?>> overrides = new ArrayList<>();
        final List<SettingDef<?>> edited = new ArrayList<>();
        final List<MachineProfile.Entry> visible = cfg.getProfile()
            .visibleEntries(cfg, ctx)
            .toList();
        for (final MachineProfile.Entry entry : visible) {
            final SettingDef<?> def = entry.def();
            if (entry.override()) overrides.add(def);
            else child(createSettingRow(cfg, ctx, def));
            edited.addAll(def.editedSettings());
        }

        if (!overrides.isEmpty()) addOverrides(cfg, ctx, overrides);
        final boolean hasExplicit = edited.stream()
            .anyMatch(cfg::isExplicit);
        child(createActionRow(cfg, edited, hasExplicit));

        scheduleResize();
    }

    /**
     * Return the row of {@code def}. The row has a marker while a setting of the row is explicit, the editor, and a
     * tooltip with the description and the reset rows.
     */
    private <T> IWidget createSettingRow(final MachineConfig cfg, final RecipeContext ctx, final SettingDef<T> def) {
        final IWidget editor = def.settingsWidget(cfg, ctx, change -> {
            commitEdit(change);
            markStale();
        });
        final List<? extends SettingDef<?>> edited = def.editedSettings();
        final String descriptionKey = "plannh.settings." + def.getKey() + ".desc";
        final SettingsRow row = new SettingsRow(node, cfg, edited, () -> {
            commitEdit(() -> edited.forEach(cfg::reset));
            markStale();
        });
        row.name(def.isFullWidthEditor() ? "settings.composite" : "settings.row")
            .fullWidth()
            .marginBottom(ROW_GAP)
            .coverChildrenHeight()
            .childPadding(2)
            .tooltipBuilder(TooltipBuilder.build(out -> {
                if (StatCollector.canTranslate(descriptionKey))
                    out.line(TooltipTheme.BODY.style(IKey.lang(descriptionKey)));
                out.resetHint(row.isExplicit());
            }))
            .tooltipAutoUpdate(true);

        if (row.isExplicit()) row.child(
            new FlowchartTextWidget(SETTINGS.lang("changed"), node).name("settings.row.changed")
                .widgetTheme(PlannhThemes.CARD_CHANGED));
        final String label = def.getEditorLabel();
        if (!def.isFullWidthEditor() || !label.isEmpty()) row.child(
            new FlowchartTextWidget(IKey.str(label), node).name("settings.row.label")
                .expanded());
        return row.child(editor);
    }

    /**
     * Add the toggle of the override rows, and the rows below the toggle while the part is open. When an override
     * becomes explicit, this method opens the part. The player can fold the part again.
     */
    private void addOverrides(final MachineConfig cfg, final RecipeContext ctx, final List<SettingDef<?>> overrides) {
        final boolean explicit = overrides.stream()
            .anyMatch(cfg::isExplicit);
        if (explicit && !overridesWereExplicit) overridesOpen = true;
        overridesWereExplicit = explicit;

        final CycleButtonWidget toggle = new CycleButtonWidget().size(OVERRIDES_TOGGLE)
            .name("settings.overrides.toggle")
            .stateCount(2)
            .stateOverlay(false, GuiIcon.FOLDED.asIcon())
            .stateOverlay(true, GuiIcon.UNFOLDED.asIcon())
            .value(new BoolValue.Dynamic(() -> overridesOpen, open -> {
                overridesOpen = open;
                markStale();
            }))
            .tooltipBuilder(TooltipBuilder.build(OVERRIDES, out -> {
                out.text("hint");
                out.hint(overridesOpen ? "hide_hint" : "show_hint");
            }));
        child(
            FlowchartFlow.row(node)
                .name("settings.overrides")
                .fullWidth()
                .coverChildrenHeight()
                .childPadding(2)
                .child(toggle)
                .child(new FlowchartTextWidget(SETTINGS.lang("overrides"), node).widgetTheme(PlannhThemes.CARD_MUTED)));
        if (!overridesOpen) return;

        for (final SettingDef<?> def : overrides) {
            child(createSettingRow(cfg, ctx, def));
        }
    }

    /**
     * Return the row of the buttons below the settings. The player copies and pastes between machines of one profile.
     * After a paste, the machine count of this machine is unchanged. A paste and a reset are one undo step each. The
     * reset button is in the row while a setting of {@code edited} is explicit.
     */
    private IWidget createActionRow(final MachineConfig cfg, final List<SettingDef<?>> edited,
        final boolean hasExplicit) {
        final Runnable copy = () -> {
            copiedSettings = new MachineConfig(cfg);
            markStale();
        };
        final Runnable paste = () -> {
            final MachineConfig copied = copiedSettings;
            if (!(copied != null && Objects.equals(copied.getProfile(), cfg.getProfile()))) return;
            commitEdit(() -> cfg.copySettingsFrom(copied));
            markStale();
        };
        final Runnable reset = () -> {
            commitEdit(() -> edited.forEach(cfg::reset));
            markStale();
        };

        final Flow row = FlowchartFlow.row(node)
            .name("settings.actions")
            .fullWidth()
            .coverChildrenHeight()
            .childPadding(2);
        row.child(createActionButton("copy", () -> "copy.hint", copy));
        row.child(createActionButton("paste", () -> readPasteHint(cfg), paste));
        if (!hasExplicit) return row;
        return row.child(createActionButton("reset", () -> "reset.hint", reset));
    }

    private IWidget createActionButton(final String name, final Supplier<String> hintName, final Runnable click) {
        final int height = 12;
        return new ButtonWidget<>().name("settings.action")
            .expanded()
            .height(height)
            .overlay(SETTINGS.lang(name))
            .onMousePressed(clicked -> {
                if (clicked != 0) return false;
                click.run();
                return true;
            })
            .tooltipBuilder(TooltipBuilder.build(SETTINGS, out -> out.text(hintName.get())))
            .tooltipAutoUpdate(true);
    }

    private static String readPasteHint(final MachineConfig cfg) {
        final MachineConfig copied = copiedSettings;
        if (copied == null) return "paste.empty";
        final boolean sameProfile = Objects.equals(copied.getProfile(), cfg.getProfile());
        return sameProfile ? "paste.hint" : "paste.other";
    }

    /** A row of the settings fold that a middle-click resets. */
    public static final class SettingsRow extends FlowchartFlow implements MiddleClickReset {

        private final MachineConfig cfg;
        private final List<? extends SettingDef<?>> edited;
        private final Runnable resetAction;

        /** The row of the settings {@code edited}. {@code resetAction} resets those settings. */
        public SettingsRow(final NodeWidget node, final MachineConfig cfg, final List<? extends SettingDef<?>> edited,
            final Runnable resetAction) {
            super(GuiAxis.X, node);
            this.cfg = cfg;
            this.edited = edited;
            this.resetAction = resetAction;
        }

        @Override
        public boolean isExplicit() {
            return edited.stream()
                .anyMatch(cfg::isExplicit);
        }

        @Override
        public void reset() {
            resetAction.run();
        }
    }

}
