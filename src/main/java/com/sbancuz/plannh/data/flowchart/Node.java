package com.sbancuz.plannh.data.flowchart;

import java.util.ArrayList;
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
import com.sbancuz.plannh.data.MachineProfileRegistry;
import com.sbancuz.plannh.data.flowchart.resource.LongFluidStack;
import com.sbancuz.plannh.data.flowchart.resource.LongItemStack;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.properties.RecipeProperty;
import com.sbancuz.plannh.data.properties.ResourceProperty;
import com.sbancuz.plannh.data.provider.DefaultProvider;
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

    private transient List<Port<?>> inputs = new ArrayList<>();
    private transient List<Port<?>> outputs = new ArrayList<>();
    private final Map<Integer, ItemStack> inputConfigurations = new HashMap<>();

    // needed for coloring to be machine specific
    private final String machineName;
    private final Recipe.RecipeId recipeId;

    private MachineConfig machineConfig;
    private transient Map<RecipeProperty<?>, Object> properties = new HashMap<>();

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

    @Nullable
    private transient PropertyProvider extractor;
    private transient List<PropertyProvider> availableExtractors = List.of();
    @Nullable
    private transient RecipeHandlerRef handlerRef;
    private transient boolean initialized;
    @Setter
    private int extractorIndex;

    public Node(IRecipeHandler handler, int recipeIndex) {
        super(UUID.randomUUID());
        recipeId = recipeIndex < 0 || recipeIndex >= handler.numRecipes() ? null
            : Recipe.RecipeId.of(handler, recipeIndex);
        machineName = handler.getRecipeName()
            .trim();
        header = machineName;
        init();
    }

    private Node(final Node source) {
        super(source.id);
        machineName = source.machineName;
        recipeId = source.recipeId;
        machineConfig = source.machineConfig == null ? null : new MachineConfig(source.machineConfig);
        availableExtractors = source.availableExtractors;
        extractorIndex = source.extractorIndex;
        extractor = source.extractor;
        handlerRef = source.handlerRef;
        source.inputConfigurations.forEach((index, stack) -> inputConfigurations.put(index, stack.copy()));
    }

    /** Extracts only into a detached candidate; callers publish it after this completes. */
    private void readRecipe(final boolean restoreConfigurations) {
        if (!restoreConfigurations) inputConfigurations.clear();
        final Map<RecipeProperty<?>, Object> extracted = extractor
            .extract(this, handlerRef.handler, handlerRef.recipeIndex);
        if (extracted != null) properties.putAll(extracted);
        deduplicate(inputs);
        deduplicate(outputs);
        machineConfig.seedRouteDefaults(properties);
        if (!restoreConfigurations) return;

        inputConfigurations.keySet()
            .removeIf(
                index -> index < 0 || index >= inputs.size()
                    || inputs.get(index)
                        .getAllStacks()
                        .getFirst().items.length <= 1);
        inputConfigurations.forEach(this::setConfiguration);
    }

    private void applyRecipe(final Node candidate) {
        inputs = candidate.inputs;
        outputs = candidate.outputs;
        properties = candidate.properties;
        machineConfig = candidate.machineConfig;
        handlerRef = candidate.handlerRef;
        availableExtractors = candidate.availableExtractors;
        extractorIndex = candidate.extractorIndex;
        inputConfigurations.clear();
        inputConfigurations.putAll(candidate.inputConfigurations);
        extractor = candidate.extractor;
    }

    public boolean isAvailable() {
        return extractor != null;
    }

    @SuppressWarnings("unchecked")
    private void setConfiguration(int index, ItemStack itemStack) {
        Port<?> port = inputs.get(index);
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

    private static void deduplicate(List<Port<?>> ports) {
        for (int i = 0; i < ports.size(); i++) {
            final Port<?> port = ports.get(i);
            for (int j = i + 1; j < ports.size();) {
                if (!port.canConnect(ports.get(j))) {
                    j++;
                    continue;
                }
                port.merge(ports.remove(j));
            }
        }
    }

    public boolean switchExtractor(final int delta) {
        if (!isAvailable() || availableExtractors.size() < 2) return false;
        final Node candidate = new Node(this);
        candidate.extractorIndex = Math.floorMod(extractorIndex + delta, availableExtractors.size());
        candidate.extractor = availableExtractors.get(candidate.extractorIndex);
        final String pid = candidate.extractor.getProfileId(handlerRef.handler, handlerRef.recipeIndex);
        candidate.machineConfig.setProfileId(pid == null ? MachineProfileRegistry.defaultId() : pid);
        candidate.readRecipe(false);
        applyRecipe(candidate);
        return true;
    }

    private static int pickBestExtractor(final List<PropertyProvider> providers, final RecipeHandlerRef ref) {
        for (int i = 0; i < providers.size(); i++) {
            if (providers.get(i)
                .canCraft(ref.handler, ref.recipeIndex)) return i;
        }
        return 0;
    }

    public void init() {
        if (initialized) return;
        initialized = true;
        inputs = new ArrayList<>();
        outputs = new ArrayList<>();
        properties = new HashMap<>();
        availableExtractors = List.of();
        extractor = null;
        handlerRef = null;
        try {
            final RecipeHandlerRef ref = RecipeHandlerRef.of(recipeId);
            if (ref == null) return;
            final List<PropertyProvider> providers = RecipePropertyAPI.getExtractors(ref.handler.getClass());
            int index = extractorIndex;
            if (index < 0 || index >= providers.size()) index = 0;
            if (machineConfig == null) index = pickBestExtractor(providers, ref);

            final Node candidate = new Node(this);
            candidate.handlerRef = ref;
            candidate.availableExtractors = providers;
            candidate.extractorIndex = index;
            candidate.extractor = providers.isEmpty() ? DefaultProvider.INSTANCE : providers.get(index);
            if (candidate.machineConfig == null) {
                final String pid = candidate.extractor.getProfileId(ref.handler, ref.recipeIndex);
                candidate.machineConfig = new MachineConfig(
                    MachineProfileRegistry.get(pid == null ? MachineProfileRegistry.defaultId() : pid));
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
    public Node(final UUID id, String name, final int x, final int y) {
        super(id);
        this.x = x;
        this.y = y;
        this.machineConfig = new MachineConfig();
        recipeId = null;
        machineName = name;
        extractor = DefaultProvider.INSTANCE;
        availableExtractors = List.of(DefaultProvider.INSTANCE);
        initialized = true;
    }
}
