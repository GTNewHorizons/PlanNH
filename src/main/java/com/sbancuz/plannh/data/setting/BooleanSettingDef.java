package com.sbancuz.plannh.data.setting;

import java.util.function.Consumer;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;

public class BooleanSettingDef extends SettingDef<Boolean> {

    public BooleanSettingDef(String key, Boolean defaultValue) {
        super(key, defaultValue);
    }

    @Override
    public Consumer<TooltipBuilder> tooltip(final Boolean value) {
        return value ? out -> out.flag(getLabel()) : out -> {};
    }

    @Override
    public IWidget settingsWidget(final MachineConfig cfg, final RecipeContext ctx, final Consumer<Runnable> edit) {
        return new ToggleButton().name("settings.toggle")
            .value(
                new BoolValue.Dynamic(
                    () -> cfg.getProfile()
                        .value(this, cfg, ctx),
                    val -> edit.accept(() -> cfg.set(this, val))))
            .overlay(false, IKey.str("[ ]"))
            .overlay(true, IKey.str("[✓]"));
    }

    @Override
    protected Class<?> valueType() {
        return Boolean.class;
    }
}
