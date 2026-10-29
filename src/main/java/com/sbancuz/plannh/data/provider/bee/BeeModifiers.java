package com.sbancuz.plannh.data.provider.bee;

import java.lang.reflect.Proxy;
import java.util.List;

import forestry.api.apiculture.BeeManager;
import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.IBeeModifier;

/** Shared access to Forestry's native housing modifier combiner. */
public final class BeeModifiers {

    private BeeModifiers() {}

    public static IBeeModifier combine(final List<? extends IBeeModifier> modifiers) {
        final List<IBeeModifier> snapshot = List.copyOf(modifiers);

        // Forestry's combiner only needs the modifier list, not a live housing or world.
        final IBeeHousing housing = (IBeeHousing) Proxy.newProxyInstance(
            IBeeHousing.class.getClassLoader(),
            new Class<?>[] { IBeeHousing.class },
            (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return switch (method.getName()) {
                        case "equals" -> proxy == (args == null ? null : args[0]);
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "BeeModifiers housing proxy";
                        default -> throw new UnsupportedOperationException(method.toString());
                    };
                }
                if ("getBeeModifiers".equals(method.getName())) return snapshot;
                throw new UnsupportedOperationException("No world or inventory in a bee modifier combiner");
            });

        return BeeManager.beeRoot.createBeeHousingModifier(housing);
    }
}
