package com.sbancuz.plannh.data;

import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.sbancuz.plannh.data.effect.EffectResult;
import com.sbancuz.plannh.data.flowchart.balancer.Pin;
import com.sbancuz.plannh.data.setting.CopyCountTarget;
import com.sbancuz.plannh.data.setting.RateTarget;
import com.sbancuz.plannh.data.setting.SettingDef;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MachineConfig {

    /** The key of {@link #profileConfigs} while the config doesn't have a profile id. */
    public static final String NO_PROFILE_ID = "";

    private transient MachineProfile profile;
    @Setter(AccessLevel.NONE)
    private String profileId;

    private final CopyCountTarget copies = new CopyCountTarget();
    private final RateTarget rates = new RateTarget();
    private Pin targetKind = Pin.NONE;
    /** The key is a profile id that the player has selected on this machine. The value has the edits of the profile. */
    @Getter(AccessLevel.NONE)
    private final SortedMap<String, ProfileConfig> profileConfigs = new TreeMap<>();
    /** The entry of {@link #profileConfigs} for {@link #profileId}. {@link #current()} looks the entry up. */
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private transient @Nullable ProfileConfig current;

    /** Used by Gson so omitted fields retain their defaults. */
    private MachineConfig() {}

    public MachineConfig(final MachineProfile requested) {
        this(requested, Map.of());
    }

    /** Creates a sparse user configuration; defaults remain owned by the profile. */
    public MachineConfig(final MachineProfile requested, final Map<SettingDef<?>, Object> settings) {
        setProfile(requested);
        current().setAll(settings);
    }

    public MachineConfig(final MachineConfig source) {
        profile = source.profile;
        profileId = source.profileId;
        copies.setCopies(source.copies.copies());
        rates.copyFrom(source.rates);
        targetKind = source.targetKind;
        for (final Map.Entry<String, ProfileConfig> entry : source.profileConfigs.entrySet()) {
            profileConfigs.put(entry.getKey(), new ProfileConfig(entry.getValue()));
        }
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

    public <T> T getOrDefault(final SettingDef<T> setting, final Supplier<? extends T> fallback) {
        return current().getOrDefault(setting, fallback);
    }

    public <T> void set(final SettingDef<T> setting, @Nullable final T value) {
        current().set(setting, value);
    }

    public boolean isExplicit(final SettingDef<?> setting) {
        return current().isExplicit(setting);
    }

    public void reset(final SettingDef<?> setting) {
        current().reset(setting);
    }

    @Nonnull
    public EffectResult computeEffect(final RecipeContext ctx) {
        final MachineProfile profile = getProfile();
        if (profile == null) throw new IllegalStateException("Machine profile is unavailable");
        return profile.computeEffect(this, ctx);
    }

    /**
     * Resolve the transient profile. When the config has another profile id or no profile id, the edits move to the id
     * of {@code profile}.
     */
    public void setProfile(@Nullable final MachineProfile profile) {
        this.profile = profile;
        if (profile == null || profile.id()
            .equals(profileId)) return;

        final ProfileConfig edits = current();
        profileConfigs.remove(profileId == null ? NO_PROFILE_ID : profileId);
        profileId = profile.id();
        profileConfigs.put(profileId, edits);
    }

    /** Select {@code newProfile}. The edits of the profile before the switch stay in {@link #profileConfigs}. */
    public void selectProfile(final MachineProfile newProfile) {
        profile = newProfile;
        profileId = newProfile.id();
        current = null;
    }

    public void copySettingsFrom(final MachineConfig other) {
        if (!Objects.equals(profile, other.profile)) {
            throw new IllegalArgumentException("Cannot copy settings between different machine profiles");
        }
        current().copySettingsFrom(other.current());
    }

    public @Nullable Double chanceOverride(final boolean input, final int index) {
        return current().chanceOverride(input, index);
    }

    public void overrideChance(final boolean input, final int index, @Nullable final Double chance) {
        current().overrideChance(input, index, chance);
    }

    /** Return the config of {@link #profileId}, and create the config when the profile doesn't have one. */
    private ProfileConfig current() {
        if (current == null) current = profileConfigs
            .computeIfAbsent(profileId == null ? NO_PROFILE_ID : profileId, _ -> new ProfileConfig());
        return current;
    }
}
