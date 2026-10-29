package com.sbancuz.plannh.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.sbancuz.plannh.data.effect.EffectResult;
import com.sbancuz.plannh.data.flowchart.balancer.Pin;
import com.sbancuz.plannh.data.setting.CopyCountTarget;
import com.sbancuz.plannh.data.setting.OutputRateTarget;
import com.sbancuz.plannh.data.setting.SettingDef;
import com.sbancuz.plannh.data.setting.Settings;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MachineConfig {

    @Getter(AccessLevel.NONE)
    private final Map<SettingDef<?>, Object> settings = new HashMap<>();
    private transient MachineProfile profile;

    private final CopyCountTarget copies = new CopyCountTarget();
    private final OutputRateTarget rates = new OutputRateTarget();
    private Pin targetKind = Pin.NONE;
    private final Map<Integer, Double> inputConsumption = new HashMap<>();
    private final Map<Integer, Double> outputProductivity = new HashMap<>();

    /** Used by Gson so omitted fields retain their defaults. */
    private MachineConfig() {}

    public MachineConfig(final MachineProfile requested) {
        this(requested, Map.of());
    }

    /** Creates a sparse user configuration; defaults remain owned by the profile. */
    public MachineConfig(final MachineProfile requested, final Map<SettingDef<?>, Object> settings) {
        profile = requested;
        this.settings.putAll(settings);
    }

    public MachineConfig(final MachineConfig source) {
        profile = source.profile;
        settings.putAll(source.settings);
        copies.setCopies(source.copies.copies());
        rates.rates()
            .putAll(source.rates.rates());
        targetKind = source.targetKind;
        inputConsumption.putAll(source.inputConsumption);
        outputProductivity.putAll(source.outputProductivity);
    }

    public boolean isCopiesTargeted() {
        return targetKind == Pin.FIXED_COPIES;
    }

    public <T> T getOrDefault(final SettingDef<T> setting) {
        return getOrDefault(setting, setting::getDefaultValue);
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(final SettingDef<T> setting, final Supplier<? extends T> fallback) {
        if (settings.containsKey(setting)) return (T) settings.get(setting);
        return fallback.get();
    }

    public <T> void set(final SettingDef<T> setting, @Nullable final T value) {
        settings.put(setting, value);
    }

    public boolean isExplicit(final SettingDef<?> setting) {
        return settings.containsKey(setting);
    }

    public void reset(final SettingDef<?> setting) {
        settings.remove(setting);
    }

    @Nonnull
    public EffectResult computeEffect(final RecipeContext ctx) {
        final MachineProfile profile = getProfile();
        if (profile == null) throw new IllegalStateException("Machine profile is unavailable");
        return profile.computeEffect(this, ctx);
    }

    public void selectProfile(final MachineProfile newProfile) {
        if (!Objects.equals(profile, newProfile)) {
            settings.clear();
            inputConsumption.clear();
            outputProductivity.clear();
        }
        profile = newProfile;
    }

    public void copySettingsFrom(final MachineConfig other) {
        if (!Objects.equals(profile, other.profile)) {
            throw new IllegalArgumentException("Cannot copy settings between different machine profiles");
        }
        final boolean machinesExplicit = isExplicit(Settings.MACHINES);
        final Object machines = settings.get(Settings.MACHINES);
        final Pin kind = targetKind;
        settings.clear();
        settings.putAll(other.settings);
        if (machinesExplicit) settings.put(Settings.MACHINES, machines);
        else settings.remove(Settings.MACHINES);
        targetKind = kind;
    }

    public double inputMultiplier(final int inputIndex) {
        final Double value = inputConsumption.get(inputIndex);
        return value == null ? 1d : value;
    }

    public double inputMultiplier(final int inputIndex, final DoubleSupplier fallback) {
        final Double value = inputConsumption.get(inputIndex);
        return value == null ? fallback.getAsDouble() : value;
    }

    public void inputMultiplier(final int index, final double value) {
        inputConsumption.put(index, value);
    }

    public double outputMultiplier(final int outputIndex) {
        final Double value = outputProductivity.get(outputIndex);
        return value == null ? 1d : value;
    }

    public double outputMultiplier(final int outputIndex, final DoubleSupplier fallback) {
        final Double value = outputProductivity.get(outputIndex);
        return value == null ? fallback.getAsDouble() : value;
    }

    public void outputMultiplier(final int index, final double value) {
        outputProductivity.put(index, value);
    }
}
