package net.blay09.mods.waystones.requirement;

import net.blay09.mods.waystones.Waystones;
import net.blay09.mods.waystones.api.WaystoneTeleportContext;
import net.blay09.mods.waystones.api.TeleportFlags;
import net.blay09.mods.waystones.api.WaystoneTypes;
import net.blay09.mods.waystones.api.WaystoneVisibility;
import net.blay09.mods.waystones.api.requirement.*;
import net.blay09.mods.waystones.config.WaystonesConfig;
import net.blay09.mods.waystones.core.WaystoneTeleportManager;
import net.blay09.mods.waystones.tag.ModItemTags;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class WarpModifierRegistry {

    private static final Map<ResourceLocation, RequirementType<?>> requirementTypes = new HashMap<>();
    private static final Map<ResourceLocation, RequirementFunction<?, ?>> requirementFunctions = new HashMap<>();
    private static final Map<Class<?>, ParameterSerializer<?>> parameterSerializers = new HashMap<>();
    private static final Map<ResourceLocation, VariableResolver> variableResolvers = new HashMap<>();
    private static final Map<ResourceLocation, ConditionResolver<?>> conditionResolvers = new HashMap<>();

    public record NoParameter() {
        public static final NoParameter INSTANCE = new NoParameter();
    }

    public record IntParameter(int value) {
    }

    public record FloatParameter(float value) {
    }

    public record IdParameter(ResourceLocation value) {
    }

    public record VariableScaledParameter(IdParameter id, FloatParameter scale) {
    }

    public record CooldownParameter(IdParameter id, FloatParameter seconds) {
    }

    public record VariableScaledCooldownParameter(IdParameter variable, IdParameter cooldown, FloatParameter seconds) {
    }

    public static void registerDefaults() {
        final var experiencePoints = new ExperiencePointsRequirementType();
        final var levels = new ExperienceLevelRequirementType();
        final var cooldown = new CooldownRequirementType();

        register(experiencePoints);
        register(levels);
        register(cooldown);

        registerModifier("add_level_cost", levels, FloatParameter.class, (cost, context, parameters) -> {
            cost.setLevels((int) (cost.getLevels() + parameters.value));
            return cost;
        }, () -> WaystonesConfig.getActive().teleports.enableCosts);
        registerModifier("multiply_level_cost", levels, FloatParameter.class, (cost, context, parameters) -> {
            cost.setLevels((int) (cost.getLevels() * parameters.value));
            return cost;
        }, () -> WaystonesConfig.getActive().teleports.enableCosts);
        registerModifier("scaled_add_level_cost", levels, VariableScaledParameter.class, (cost, context, parameters) -> {
            final var sourceValue = context.getContextValue(parameters.id.value);
            cost.setLevels((int) (cost.getLevels() + sourceValue * parameters.scale.value));
            return cost;
        }, () -> WaystonesConfig.getActive().teleports.enableCosts);
        registerModifier("multiply_level_cost", levels, FloatParameter.class, (cost, context, parameters) -> {
            cost.setLevels((int) (cost.getLevels() * parameters.value));
            return cost;
        }, () -> WaystonesConfig.getActive().teleports.enableCosts);
        registerModifier("min_level_cost", levels, IntParameter.class, (cost, context, parameters) -> {
            cost.setLevels(Math.max(cost.getLevels(), parameters.value));
            return cost;
        }, () -> WaystonesConfig.getActive().teleports.enableCosts);
        registerModifier("max_level_cost", levels, IntParameter.class, (cost, context, parameters) -> {
            cost.setLevels(Math.min(cost.getLevels(), parameters.value));
            return cost;
        }, () -> WaystonesConfig.getActive().teleports.enableCosts);

        registerModifier("add_xp_cost", experiencePoints, IntParameter.class, (cost, context, parameters) -> {
            cost.setPoints(cost.getPoints() + parameters.value);
            return cost;
        }, () -> WaystonesConfig.getActive().teleports.enableCosts);
        registerModifier("multiply_xp_cost", experiencePoints, FloatParameter.class, (cost, context, parameters) -> {
            cost.setPoints((int) (cost.getPoints() * parameters.value));
            return cost;
        }, () -> WaystonesConfig.getActive().teleports.enableCosts);
        registerModifier("scaled_add_xp_cost", experiencePoints, VariableScaledParameter.class, (cost, context, parameters) -> {
            final var sourceValue = context.getContextValue(parameters.id.value);
            cost.setPoints((int) (cost.getPoints() + sourceValue * parameters.scale.value));
            return cost;
        }, () -> WaystonesConfig.getActive().teleports.enableCosts);
        registerModifier("min_xp_cost", experiencePoints, IntParameter.class, (cost, context, parameters) -> {
            cost.setPoints(Math.max(cost.getPoints(), parameters.value));
            return cost;
        }, () -> WaystonesConfig.getActive().teleports.enableCosts);
        registerModifier("max_xp_cost", experiencePoints, IntParameter.class, (cost, context, parameters) -> {
            cost.setPoints(Math.min(cost.getPoints(), parameters.value));
            return cost;
        }, () -> WaystonesConfig.getActive().teleports.enableCosts);

        registerModifier("add_cooldown", cooldown, CooldownParameter.class, (cost, context, parameters) -> {
            cost.setCooldown(parameters.id.value, (int) ((float) cost.getCooldownSeconds() + parameters.seconds.value));
            return cost;
        }, () -> WaystonesConfig.getActive().teleports.enableCooldowns);
        registerModifier("multiply_cooldown", cooldown, CooldownParameter.class, (cost, context, parameters) -> {
            cost.setCooldown(parameters.id.value, (int) ((float) cost.getCooldownSeconds() * parameters.seconds.value));
            return cost;
        }, () -> WaystonesConfig.getActive().teleports.enableCooldowns);
        registerModifier("scaled_add_cooldown", cooldown, VariableScaledCooldownParameter.class, (cost, context, parameters) -> {
            final var sourceValue = context.getContextValue(parameters.variable.value);
            cost.setCooldown(parameters.cooldown.value, (int) ((float) cost.getCooldownSeconds() + sourceValue * parameters.seconds.value));
            return cost;
        }, () -> WaystonesConfig.getActive().teleports.enableCooldowns);
        registerModifier("min_cooldown", cooldown, CooldownParameter.class, (cost, context, parameters) -> {
            cost.setCooldown(parameters.id.value, (int) Math.max(cost.getCooldownSeconds(), parameters.seconds.value));
            return cost;
        }, () -> WaystonesConfig.getActive().teleports.enableCooldowns);
        registerModifier("max_cooldown", cooldown, CooldownParameter.class, (cost, context, parameters) -> {
            cost.setCooldown(parameters.id.value, (int) Math.min(cost.getCooldownSeconds(), parameters.seconds.value));
            return cost;
        }, () -> WaystonesConfig.getActive().teleports.enableCooldowns);

        registerSerializer(NoParameter.class, it -> NoParameter.INSTANCE);
        registerSerializer(IntParameter.class, it -> new IntParameter(Integer.parseInt(it)));
        registerSerializer(FloatParameter.class, it -> new FloatParameter(Float.parseFloat(it)));
        registerSerializer(IdParameter.class, it -> new IdParameter(RequirementModifierParser.waystonesResourceLocation(it)));
        registerDefaultSerializer(VariableScaledParameter.class);
        registerDefaultSerializer(CooldownParameter.class);
        registerDefaultSerializer(VariableScaledCooldownParameter.class);

        registerConditionResolver("is_interdimensional", NoParameter.class, (context, parameters) -> context.isDimensionalTeleport());
        registerConditionResolver("source_is_warp_plate", NoParameter.class,
                (context, parameters) -> context.getFromWaystone().map(waystone -> waystone.getWaystoneType().equals(WaystoneTypes.WARP_PLATE)).orElse(false));
        registerConditionResolver("source_is_portstone", NoParameter.class,
                (context, parameters) -> context.getFromWaystone().map(waystone -> waystone.getWaystoneType().equals(WaystoneTypes.PORTSTONE)).orElse(false));
        registerConditionResolver("source_is_waystone", NoParameter.class,
                (context, parameters) -> context.getFromWaystone().map(waystone -> waystone.getWaystoneType().equals(WaystoneTypes.WAYSTONE)).orElse(false));
        registerConditionResolver("source_is_sharestone", NoParameter.class,
                (context, parameters) -> context.getFromWaystone().map(waystone -> WaystoneTypes.isSharestone(waystone.getWaystoneType())).orElse(false));
        registerConditionResolver("source_is_inventory_button",
                NoParameter.class,
                (context, parameters) -> context.getFlags().contains(TeleportFlags.INVENTORY_BUTTON));
        registerConditionResolver("source_is_scroll", NoParameter.class, (context, parameters) -> context.getWarpItem().is(ModItemTags.SCROLLS));
        registerConditionResolver("source_is_bound_scroll", NoParameter.class, (context, parameters) -> context.getWarpItem().is(ModItemTags.BOUND_SCROLLS));
        registerConditionResolver("source_is_return_scroll", NoParameter.class, (context, parameters) -> context.getWarpItem().is(ModItemTags.RETURN_SCROLLS));
        registerConditionResolver("source_is_warp_scroll", NoParameter.class, (context, parameters) -> context.getWarpItem().is(ModItemTags.WARP_SCROLLS));
        registerConditionResolver("source_is_warp_stone", NoParameter.class, (context, parameters) -> context.getWarpItem().is(ModItemTags.WARP_STONES));
        registerConditionResolver("target_is_warp_plate",
                NoParameter.class,
                (context, parameters) -> context.getTargetWaystone().getWaystoneType().equals(WaystoneTypes.WARP_PLATE));
        registerConditionResolver("target_is_global",
                NoParameter.class,
                (context, parameters) -> context.getTargetWaystone().getVisibility() == WaystoneVisibility.GLOBAL);
        registerConditionResolver("target_is_sharestone",
                NoParameter.class,
                (context, parameters) -> WaystoneTypes.isSharestone(context.getTargetWaystone().getWaystoneType()));
        registerConditionResolver("target_is_waystone",
                NoParameter.class,
                (context, parameters) -> context.getTargetWaystone().getWaystoneType().equals(WaystoneTypes.WAYSTONE));
        registerConditionResolver("target_is_landing_stone",
                NoParameter.class,
                (context, parameters) -> context.getTargetWaystone().getWaystoneType().equals(WaystoneTypes.LANDING_STONE));
        registerConditionResolver("is_with_pets", NoParameter.class, (context, parameters) -> !WaystoneTeleportManager.findPets(context.getEntity()).isEmpty());
        registerConditionResolver("is_with_leashed",
                NoParameter.class,
                (context, parameters) -> !WaystoneTeleportManager.findLeashedAnimals(context.getEntity()).isEmpty());

        registerVariableResolver("distance", it -> (float) Math.sqrt(it.getEntity().distanceToSqr(it.getTargetWaystone().getPos().getCenter())));
        registerVariableResolver("leashed", it -> (float) WaystoneTeleportManager.findLeashedAnimals(it.getEntity()).size());
        registerVariableResolver("pets", it -> (float) WaystoneTeleportManager.findPets(it.getEntity()).size());
    }

    public static void register(RequirementType<?> requirementType) {
        requirementTypes.put(requirementType.getId(), requirementType);
    }

    public static void register(RequirementFunction<?, ?> requirementFunction) {
        requirementFunctions.put(requirementFunction.getId(), requirementFunction);
    }

    public static void register(ParameterSerializer<?> parameterSerializer) {
        parameterSerializers.put(parameterSerializer.getType(), parameterSerializer);
    }

    public static void register(VariableResolver variableResolver) {
        variableResolvers.put(variableResolver.getId(), variableResolver);
    }

    public static void register(ConditionResolver<?> conditionResolver) {
        conditionResolvers.put(conditionResolver.getId(), conditionResolver);
    }

    public static void registerVariableResolver(String name, Function<WaystoneTeleportContext, Float> resolver) {
        register(new VariableResolver() {
            @Override
            public ResourceLocation getId() {
                return new ResourceLocation(Waystones.MOD_ID, name);
            }

            @Override
            public float resolve(WaystoneTeleportContext context) {
                return resolver.apply(context);
            }
        });
    }

    public static <P> void registerConditionResolver(String name, Class<P> parameterType, BiFunction<WaystoneTeleportContext, P, Boolean> resolver) {
        register(new ConditionResolver<P>() {
            @Override
            public ResourceLocation getId() {
                return new ResourceLocation(Waystones.MOD_ID, name);
            }

            @Override
            public Class<P> getParameterType() {
                return parameterType;
            }

            @Override
            public boolean matches(WaystoneTeleportContext context, P parameters) {
                return resolver.apply(context, parameters);
            }
        });

        final var index = name.indexOf("is_");
        final var notName = index != -1 ? name.substring(0, index + 3) + "not_" + name.substring(index + 3) : "not_" + name;
        register(new ConditionResolver<P>() {
            @Override
            public ResourceLocation getId() {
                return new ResourceLocation(Waystones.MOD_ID, notName);
            }

            @Override
            public Class<P> getParameterType() {
                return parameterType;
            }

            @Override
            public boolean matches(WaystoneTeleportContext context, P parameters) {
                return !resolver.apply(context, parameters);
            }
        });
    }

    public static <T> void registerDefaultSerializer(Class<T> type) {
        registerSerializer(type, it -> RequirementModifierParser.deserializeParameterList(type, it));
    }

    public static <T> void registerSerializer(Class<T> type, Function<String, T> deserializer) {
        register(new ParameterSerializer<T>() {
            @Override
            public Class<T> getType() {
                return type;
            }

            @Override
            public T deserialize(String value) {
                return deserializer.apply(value);
            }
        });
    }

    private static <T extends WarpRequirement, P> void registerModifier(String name, RequirementType<T> requirementType, Class<P> parameterType, WarpRequirementModifierFunction<T, P> function, Supplier<Boolean> predicate) {
        register(new RequirementFunction<T, P>() {
            @Override
            public ResourceLocation getId() {
                return new ResourceLocation(Waystones.MOD_ID, name);
            }

            @Override
            public ResourceLocation getRequirementType() {
                return requirementType.getId();
            }

            @Override
            public Class<P> getParameterType() {
                return parameterType;
            }

            @Override
            public T apply(T requirement, WarpRequirementsContext context, P parameters) {
                return function.apply(requirement, context, parameters);
            }

            @Override
            public boolean isEnabled() {
                return predicate.get();
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static <T extends WarpRequirement> RequirementType<T> getRequirementType(ResourceLocation id) {
        return (RequirementType<T>) requirementTypes.get(id);
    }

    @SuppressWarnings("unchecked")
    public static <T extends WarpRequirement, P> RequirementFunction<T, P> getRequirementFunction(ResourceLocation id) {
        return (RequirementFunction<T, P>) requirementFunctions.get(id);
    }

    public static VariableResolver getVariableResolver(ResourceLocation id) {
        return variableResolvers.get(id);
    }

    public static ConditionResolver<?> getConditionResolver(ResourceLocation id) {
        return conditionResolvers.get(id);
    }

    @SuppressWarnings("unchecked")
    public static <T> ParameterSerializer<T> getParameterSerializer(Class<T> type) {
        return (ParameterSerializer<T>) parameterSerializers.get(type);
    }
}
