package com.sbancuz.plannh.data.setting;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.Consumer;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.text.TextRenderer;
import com.cleanroommc.modularui.value.EnumValue;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.RecipeContext;

import lombok.Getter;

@Getter
public class EnumSettingDef<E extends Enum<E>> extends SettingDef<E> {

    private final Class<E> type;

    public EnumSettingDef(String key, E defaultValue, Class<E> type) {
        super(key, defaultValue);
        this.type = type;
    }

    private int getMaxWidth() {
        return Arrays.stream(type.getEnumConstants())
            .map(this::displayValue)
            .mapToInt(
                s -> TextRenderer.getFontRenderer()
                    .getStringWidth(s))
            .max()
            .orElseThrow() + 10;
    }

    String displayValue(final E constant) {
        final String name = constant.name()
            .toLowerCase(Locale.ROOT);
        final String translationKey = "plannh.settings." + getKey() + "." + name;
        return StatCollector.canTranslate(translationKey) ? StatCollector.translateToLocal(translationKey)
            : constant.name();
    }

    @Override
    public IWidget settingsWidget(final MachineConfig cfg, final RecipeContext ctx, final Consumer<Runnable> edit) {
        final CycleButtonWidget button = new CycleButtonWidget().name("settings.cycle")
            .value(new EnumValue.Dynamic<>(type, () -> {
                E val = cfg.getProfile()
                    .value(this, cfg, ctx);
                return val != null ? val : type.getEnumConstants()[0];
            }, val -> edit.accept(() -> cfg.set(this, val))))
            .width(getMaxWidth());
        for (final E constant : type.getEnumConstants()) {
            button.stateOverlay(constant, IKey.dynamic(() -> displayValue(constant)));
        }
        return button;
    }

    @Override
    protected Class<?> valueType() {
        return type;
    }
}
