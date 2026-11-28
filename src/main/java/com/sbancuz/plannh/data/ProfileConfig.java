package com.sbancuz.plannh.data;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.sbancuz.plannh.data.setting.SettingDef;
import com.sbancuz.plannh.data.setting.Settings;

/** The settings and port chances that the player set for one profile of a machine. */
public final class ProfileConfig {

    private final Map<SettingDef<?>, Object> settings = new HashMap<>();
    private final SortedMap<Integer, Double> inputChance = new TreeMap<>();
    private final SortedMap<Integer, Double> outputChance = new TreeMap<>();

    public ProfileConfig() {}

    public ProfileConfig(final ProfileConfig source) {
        settings.putAll(source.settings);
        inputChance.putAll(source.inputChance);
        outputChance.putAll(source.outputChance);
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(final SettingDef<T> setting, final Supplier<? extends T> fallback) {
        if (settings.containsKey(setting)) return (T) settings.get(setting);
        return fallback.get();
    }

    public <T> void set(final SettingDef<T> setting, @Nullable final T value) {
        settings.put(setting, value);
    }

    public void setAll(final Map<SettingDef<?>, Object> values) {
        settings.putAll(values);
    }

    public boolean isExplicit(final SettingDef<?> setting) {
        return settings.containsKey(setting);
    }

    public void reset(final SettingDef<?> setting) {
        settings.remove(setting);
    }

    /** Take the settings of {@code other}, and keep the machine count of this config. */
    public void copySettingsFrom(final ProfileConfig other) {
        final boolean machinesExplicit = settings.containsKey(Settings.MACHINES);
        final Object machines = settings.get(Settings.MACHINES);
        settings.clear();
        settings.putAll(other.settings);
        if (machinesExplicit) settings.put(Settings.MACHINES, machines);
        else settings.remove(Settings.MACHINES);
    }

    public @Nullable Double chanceOverride(final boolean input, final int index) {
        return (input ? inputChance : outputChance).get(index);
    }

    public void overrideChance(final boolean input, final int index, @Nullable final Double chance) {
        final Map<Integer, Double> overrides = input ? inputChance : outputChance;
        if (chance == null) overrides.remove(index);
        else overrides.put(index, chance);
    }
}
