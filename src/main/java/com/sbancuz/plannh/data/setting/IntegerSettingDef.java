package com.sbancuz.plannh.data.setting;

import java.util.function.Consumer;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.text.TextRenderer;
import com.cleanroommc.modularui.value.IntValue;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.gui.tooltips.TooltipTheme;

import lombok.Getter;

@Getter
public class IntegerSettingDef extends SettingDef<Integer> {

    private static final int PADDING = 10;

    private final int min;
    private final int max;

    public IntegerSettingDef(final String key, final int def, final int min, final int max) {
        super(key, def);
        this.min = min;
        this.max = max;
    }

    @Override
    protected TooltipTheme.Role valueRole() {
        return TooltipTheme.Role.TUNABLE;
    }

    public static int fieldWidth(final int min, final int max) {
        return fieldWidth(Long.toString(Math.max(Math.abs((long) min), Math.abs((long) max))));
    }

    /** The same field, measured against a value that is not an int - a rate, say. */
    public static int fieldWidth(final String widest) {
        return TextRenderer.getFontRenderer()
            .getStringWidth(widest) + PADDING;
    }

    @Override
    public IWidget settingsWidget(final MachineConfig cfg, final RecipeContext ctx, final Consumer<Runnable> edit) {
        final int minimum = cfg.getProfile()
            .minimum(this);
        final int maximum = cfg.getProfile()
            .maximum(this);
        return new TextFieldWidget().name("settings.int")
            .width(fieldWidth(minimum, maximum))
            .value(
                new IntValue.Dynamic(
                    () -> cfg.getProfile()
                        .value(this, cfg, ctx),
                    val -> edit.accept(() -> cfg.set(this, val))))
            .numbersInt(minimum, maximum)
            .formatAsInteger(true);
    }

    @Override
    protected Class<?> valueType() {
        return Integer.class;
    }
}
