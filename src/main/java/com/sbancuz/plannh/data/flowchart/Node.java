package com.sbancuz.plannh.data.flowchart;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.sbancuz.plannh.PlanNH;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.EffectResult;
import com.sbancuz.plannh.data.flowchart.resource.LongFluidStack;
import com.sbancuz.plannh.data.flowchart.resource.LongItemStack;
import com.sbancuz.plannh.data.properties.RecipeProperty;
import com.sbancuz.plannh.data.properties.ResourceProperty;
import com.sbancuz.plannh.data.setting.Settings;
import com.sbancuz.plannh.mixins.PositionedStackAccessor;

import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.Recipe;
import codechicken.nei.recipe.RecipeHandlerRef;
import codechicken.nei.recipe.StackInfo;
import lombok.Getter;
import lombok.Setter;

@Getter
public class Node extends GraphData {

    public static final String TYPE = "node";

    private final Map<Integer, ItemStack> inputConfigurations = new HashMap<>();

    // needed for coloring to be machine specific
    private final String machineName;
    private final Recipe.RecipeId recipeId;

    private MachineConfig machineConfig;

    @Setter
    private transient boolean settingsOpen = true;

    /** Whether the target rows are showing. Transient, like {@link #settingsOpen}. */
    @Setter
    private transient boolean targetOpen = true;

    /** Whether the throughput rows are showing. Transient, like {@link #targetOpen}. */
    @Setter
    private transient boolean throughputOpen = true;

    /** Whether the properties rows are showing. Transient, like {@link #throughputOpen}. */
    @Setter
    private transient boolean propertiesOpen = true;

    private transient MachineProfile selectedProfile;
    private transient List<MachineProfile> availableProfiles = List.of();
    private transient RecipeContext context = RecipeContext.builder()
        .build();
    @Nullable
    private transient RecipeHandlerRef handlerRef;
    private transient boolean initialized;

    public Node(IRecipeHandler handler, int recipeIndex) {
        super(UUID.randomUUID());
        recipeId = recipeIndex < 0 || recipeIndex >= handler.numRecipes() ? null
            : Recipe.RecipeId.of(handler, recipeIndex);
        machineName = handler.getRecipeName()
            .trim();
        machineConfig = new MachineConfig((MachineProfile) null);
        header = machineName;
        init();
    }

    private Node(final Node source) {
        super(source.id);
        machineName = source.machineName;
        recipeId = source.recipeId;
        machineConfig = source.machineConfig == null ? null : new MachineConfig(source.machineConfig);
        selectedProfile = source.selectedProfile;
        availableProfiles = source.availableProfiles;
        handlerRef = source.handlerRef;
        source.inputConfigurations.forEach((index, stack) -> inputConfigurations.put(index, stack.copy()));
    }

    /** Extracts only into a detached candidate; callers publish it after this completes. */
    private void readRecipe(final boolean restoreConfigurations) {
        if (!restoreConfigurations) inputConfigurations.clear();
        context = selectedProfile.getRecipeContext(handlerRef);
        if (!restoreConfigurations) return;

        inputConfigurations.keySet()
            .removeIf(
                index -> index < 0 || index >= context.inputs()
                    .size()
                    || context.inputs()
                        .get(index)
                        .getAllStacks()
                        .getFirst().items.length <= 1);
        inputConfigurations.forEach(this::setConfiguration);
    }

    private void applyRecipe(final Node candidate) {
        context = candidate.context;
        machineConfig = candidate.machineConfig;
        handlerRef = candidate.handlerRef;
        availableProfiles = candidate.availableProfiles;
        selectedProfile = candidate.selectedProfile;
        inputConfigurations.clear();
        inputConfigurations.putAll(candidate.inputConfigurations);
    }

    public boolean isAvailable() {
        return selectedProfile != null;
    }

    // TODO: remove
    public RecipeContext recipeContext() {
        return context;
    }

    public List<Port<?>> getInputs() {
        return context.inputs();
    }

    public List<Port<?>> getOutputs() {
        return context.outputs();
    }

    public Map<RecipeProperty<?>, Object> getProperties() {
        return context.properties();
    }

    // TODO
    public double inputMultiplier(final int index) {
        if (!isAvailable() || machineConfig.getProfile() == null)
            throw new IllegalStateException("Machine profile is unavailable");
        return machineConfig.getProfile()
            .inputMultiplier(machineConfig, recipeContext(), index);
    }

    public double outputMultiplier(final int index) {
        if (!isAvailable() || machineConfig.getProfile() == null)
            throw new IllegalStateException("Machine profile is unavailable");
        return machineConfig.getProfile()
            .outputMultiplier(machineConfig, recipeContext(), index);
    }

    // TODO: SEe this
    public Map<RecipeProperty<?>, Object> displayProperties() {
        return isAvailable() ? displayProperties(computeEffect()) : getProperties();
    }

    public Map<RecipeProperty<?>, Object> displayProperties(final EffectResult effect) {
        final Map<RecipeProperty<?>, Object> values = new HashMap<>(getProperties());
        values.putAll(effect.properties());
        return values;
    }

    public int configuredCopies() {
        return machineConfig.isCopiesTargeted() ? machineConfig.getCopies()
            .copies()
            : isAvailable() ? machineConfig.getProfile()
                .value(Settings.MACHINES, machineConfig, recipeContext())
                : machineConfig.getOrDefault(Settings.MACHINES);
    }

    public EffectResult computeEffect() {
        if (!isAvailable()) throw new IllegalStateException("Machine profile is unavailable");
        return machineConfig.computeEffect(recipeContext());
    }

    // TODO
    public String getPresetName() {
        return machineName;
    }

    @SuppressWarnings("unchecked")
    private void setConfiguration(int index, ItemStack itemStack) {
        Port<?> port = context.inputs()
            .get(index);
        ResourceProperty<?> type = port.getType();
        if (type == RecipePropertyAPI.ITEM) {
            ((Port<LongItemStack>) port).setValue(new LongItemStack(itemStack.copy(), port.getAmount()));
        }

        if (type == RecipePropertyAPI.FLUID) {
            FluidStack fluid = StackInfo.getFluid(itemStack);
            if (fluid == null) return;
            ((Port<LongFluidStack>) port).setValue(new LongFluidStack(fluid.copy(), port.getAmount()));
        }

        port.getAllStacks()
            .forEach(ps -> {
                PositionedStackAccessor psa = (PositionedStackAccessor) ps;
                psa.setPermutated(true);
                ps.setPermutationToRender(itemStack);
                psa.setPermutated(false);
            });
    }

    /** Selects an explicit picker entry, publishing the extracted recipe only after it succeeds. */
    public boolean selectProfile(final int idx) {
        if (handlerRef == null || idx < 0 || idx >= availableProfiles.size()) return false;

        final Node candidate = new Node(this);
        candidate.selectedProfile = availableProfiles.get(idx);
        candidate.machineConfig.selectProfile(candidate.selectedProfile);

        candidate.readRecipe(false);

        applyRecipe(candidate);
        return true;
    }

    public void init() {
        if (initialized) return;
        initialized = true;
        context = RecipeContext.builder()
            .build();
        availableProfiles = List.of();
        selectedProfile = null;
        handlerRef = null;
        try {
            if (recipeId == null) return;
            final RecipeHandlerRef ref = RecipeHandlerRef.of(recipeId);
            if (ref == null) return;

            availableProfiles = RecipePropertyAPI.getExtractors(ref.handler.getClass())
                .stream()
                .filter(provider -> provider.canCraft(ref.handler, ref.recipeIndex))
                .flatMap(
                    provider -> provider.getProfiles(ref.handler, ref.recipeIndex)
                        .stream())
                .toList();

            handlerRef = ref;
            if (availableProfiles.isEmpty()) return;
            final int index = machineConfig == null ? 0
                : Math.max(0, availableProfiles.indexOf(machineConfig.getProfile()));

            final Node candidate = new Node(this);
            candidate.handlerRef = ref;
            candidate.selectedProfile = availableProfiles.get(index);
            if (candidate.machineConfig == null) {
                candidate.machineConfig = new MachineConfig(candidate.selectedProfile);
            } else {
                // Resolving the transient profile on load must not reset saved user overrides.
                candidate.machineConfig.setProfile(candidate.selectedProfile);
            }
            candidate.readRecipe(true);
            applyRecipe(candidate);
        } catch (final Exception | LinkageError failure) {
            PlanNH.LOG.warn("Recipe initialization failed for node {} ({})", id, machineName, failure);
        }
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean invalid() {
        return super.invalid() || machineName == null
            || inputConfigurations == null
            || recipeId == null
            || machineConfig == null;
    }

    // TODO: Find a better way to make the tests work
    /** Creates a detached node from an already extracted recipe. */
    public Node(final UUID id, final String name, final int x, final int y, final MachineProfile profile,
        final RecipeContext context) {
        super(id);
        this.x = x;
        this.y = y;
        this.machineConfig = new MachineConfig(profile);
        this.context = context;
        selectedProfile = profile;
        availableProfiles = List.of(profile);
        recipeId = null;
        machineName = name;
        initialized = true;
    }
}
