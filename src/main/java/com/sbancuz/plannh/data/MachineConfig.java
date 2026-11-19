package com.sbancuz.plannh.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.sbancuz.plannh.data.effect.EffectResult;
import com.sbancuz.plannh.data.flowchart.balancer.Pin;
import com.sbancuz.plannh.data.setting.CopyCountTarget;
import com.sbancuz.plannh.data.setting.RateTarget;
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
    private String profileId;

    private final CopyCountTarget copies = new CopyCountTarget();
    private final RateTarget rates = new RateTarget();
    private Pin targetKind = Pin.NONE;
    @Getter(AccessLevel.NONE)
    private final Map<Integer, Double> inputChance = new HashMap<>();
    @Getter(AccessLevel.NONE)
    private final Map<Integer, Double> outputChance = new HashMap<>();

    /** Used by Gson so omitted fields retain their defaults. */
    private MachineConfig() {}

    public MachineConfig(final MachineProfile requested) {
        this(requested, Map.of());
    }

    /** Creates a sparse user configuration; defaults remain owned by the profile. */
    public MachineConfig(final MachineProfile requested, final Map<SettingDef<?>, Object> settings) {
        setProfile(requested);
        this.settings.putAll(settings);
    }

    public MachineConfig(final MachineConfig source) {
        profile = source.profile;
        profileId = source.profileId;
        settings.putAll(source.settings);
        copies.setCopies(source.copies.copies());
        rates.copyFrom(source.rates);
        targetKind = source.targetKind;
        inputChance.putAll(source.inputChance);
        outputChance.putAll(source.outputChance);
    }

    public boolean isCopiesTargeted() {
        return targetKind == Pin.FIXED_COPIES;
    }

    public <T> T getOrDefault(final SettingDef<T> setting) {
        return getOrDefault(setting, setting::getDefaultValue);
    }

    /** Resolves explicit settings and the active profile's derived defaults. */
    public <T> T value(final SettingDef<T> setting, final RecipeContext ctx) {
        if (profile == null) throw new IllegalStateException("Machine profile is unavailable");
        return profile.value(setting, this, ctx);
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

    public void setProfile(@Nullable final MachineProfile profile) {
        this.profile = profile;
        if (profile != null) profileId = profile.id();
    }

    public void selectProfile(final MachineProfile newProfile) {
        if (!Objects.equals(profile, newProfile)) {
            settings.clear();
            inputChance.clear();
            outputChance.clear();
        }
        setProfile(newProfile);
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

    /** The user's chance for a port, as a fraction where 1 is every craft. Null when the profile's chance applies. */
    public @Nullable Double chanceOverride(final boolean input, final int index) {
        return (input ? inputChance : outputChance).get(index);
    }

    public void overrideChance(final boolean input, final int index, @Nullable final Double chance) {
        final Map<Integer, Double> overrides = input ? inputChance : outputChance;
        if (chance == null) overrides.remove(index);
        else overrides.put(index, chance);
    }
}
