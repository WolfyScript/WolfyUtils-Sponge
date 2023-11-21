package com.wolfyscript.utilities.sponge.gui;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.wolfyscript.utilities.NamespacedKey;
import com.wolfyscript.utilities.common.WolfyUtils;
import com.wolfyscript.utilities.common.gui.*;
import com.wolfyscript.utilities.common.gui.functions.SerializableConsumer;
import com.wolfyscript.utilities.common.gui.functions.SerializableFunction;
import com.wolfyscript.utilities.common.gui.functions.SerializableSupplier;
import com.wolfyscript.utilities.common.gui.signal.Signal;
import com.wolfyscript.utilities.common.gui.signal.Store;
import com.wolfyscript.utilities.common.registry.RegistryGUIComponentBuilders;
import com.wolfyscript.utilities.tuple.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.spongepowered.api.item.inventory.Container;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class WindowDynamicConstructorImpl implements WindowDynamicConstructor {

    final WolfyUtils wolfyUtils;
    final GuiViewManager viewManager;
    final GuiHolder holder;
    final WindowImpl window;
    final Multimap<ComponentBuilder<?, ?>, Integer> componentBuilderPositions = ArrayListMultimap.create();
    final Set<ComponentBuilder<?, ?>> componentRenderSet = new HashSet<>();
    final Map<String, Signal<?>> usedSignals = new HashMap<>();
    private final List<TagResolver> titleTagResolvers = new ArrayList<>();
    private SerializableSupplier<Component> titleFunction;
    private final Set<Signal<?>> titleSignals = new HashSet<>();

    public WindowDynamicConstructorImpl(@JacksonInject WolfyUtils wolfyUtils, GuiHolder holder, WindowImpl window) {
        this.wolfyUtils = wolfyUtils;
        this.window = window;
        this.holder = holder;
        this.viewManager = holder.getViewManager();

        this.componentBuilderPositions.putAll(window.nonRenderedComponents);
    }

    @Override
    public WindowDynamicConstructorImpl title(SerializableSupplier<net.kyori.adventure.text.Component> titleSupplier) {
        this.titleFunction = titleSupplier;
        return this;
    }

    @Override
    public WindowDynamicConstructorImpl titleSignals(Signal<?>... signals) {
        titleTagResolvers.addAll(Arrays.stream(signals)
                .map(signal -> TagResolver.resolver(signal.key(), (argumentQueue, context) -> Tag.inserting(net.kyori.adventure.text.Component.text(String.valueOf(signal.get())))))
                .toList());
        titleSignals.addAll(Arrays.stream(signals).toList());
        return this;
    }

    @JsonSetter("placement")
    private void setPlacement(List<ComponentBuilder<?, ?>> componentBuilders) {
        for (ComponentBuilder<?, ?> componentBuilder : componentBuilders) {
            componentBuilderPositions.putAll(componentBuilder, componentBuilder.getSlots());
        }
    }

    @Override
    public GuiViewManager viewManager() {
        return null;
    }

    @Override
    public GuiHolder holder() {
        return null;
    }

    @Override
    public <T> Signal<T> signal(String s, Class<T> aClass, Supplier<T> defaultValueFunction) {
        if (usedSignals.containsKey(s)) {
            Signal<?> usedSignal = usedSignals.get(s);
            Preconditions.checkState(usedSignal.valueType().equals(aClass), String.format("Failed to use existing state '%s'! Incompatible types: expected '%s' but got '%s'", s, usedSignal.valueType(), aClass));
            return (Signal<T>) usedSignal;
        }
        var signal = new SignalImpl<>(s, viewManager, aClass, defaultValueFunction);
        usedSignals.put(s, signal);
        return signal;
    }

    @Override
    public <T> Store<T> syncStore(String s, Class<T> aClass, Supplier<T> supplier, Consumer<T> consumer) {
        if (usedSignals.containsKey(s)) {
            Signal<?> usedSignal = usedSignals.get(s);
            Preconditions.checkState(usedSignal.valueType().equals(aClass), String.format("Failed to use existing store '%s'! Incompatible types: expected '%s' but got '%s'", s, usedSignal.valueType(), aClass));
            Preconditions.checkState(usedSignal instanceof Store<?>, String.format("Failed to use existing signal '%s' as store!", s));
            return (Store<T>) usedSignal;
        }
        var store = new StoreImpl<>(s, viewManager, aClass, supplier, consumer);
        usedSignals.put(s, store);
        return store;
    }

    @Override
    public WindowDynamicConstructorImpl reactive(SerializableFunction<ReactiveRenderBuilder, ReactiveRenderBuilder.ReactiveResult> consumer) {
        SignalledObject signalledObject = new SignalledObject() {

            private com.wolfyscript.utilities.common.gui.Component previousComponent = null;

            @Override
            public void update(GuiViewManager guiViewManager, GuiHolder guiHolder, RenderContext context) {
                if (!(context instanceof RenderContextImpl renderContext)) return;
                ReactiveRenderBuilderImpl builder = new ReactiveRenderBuilderImpl(wolfyUtils, componentBuilderPositions);
                ReactiveRenderBuilder.ReactiveResult result = consumer.apply(builder);
                com.wolfyscript.utilities.common.gui.Component component = result == null ? null : result.construct().construct(guiHolder, viewManager);
                if (Objects.equals(previousComponent, component)) return;

                if (previousComponent != null) {
                    previousComponent.remove(guiHolder, guiViewManager, context);
                }

                previousComponent = component;
                if (component == null) {
                    return;
                }

                renderContext.enterNode(component);
                for (int slot : component.getSlots()) {
                    renderContext.setSlotOffsetToParent(slot);
                    component.executeForAllSlots(slot, internalSlot -> ((GuiViewManagerImpl) guiHolder.getViewManager()).updateLeaveNodes(component, internalSlot));
                    if (component instanceof SignalledObject signalledObject) {
                        signalledObject.update(viewManager, guiHolder, renderContext);
                    }
                }
                renderContext.exitNode();

            }
        };
        for (Signal<?> signal : consumer.getSignalsUsed()) {
            signal.linkTo(signalledObject);
        }
        return this;
    }

    @Override
    public <B extends ComponentBuilder<? extends com.wolfyscript.utilities.common.gui.Component, com.wolfyscript.utilities.common.gui.Component>> WindowDynamicConstructorImpl ifThenRender(SerializableSupplier<Boolean> condition, String id, Class<B> builderType, SerializableConsumer<B> builderConsumer) {
        Pair<NamespacedKey, Class<B>> builderTypeInfo = getBuilderType(wolfyUtils, id, builderType);
        B builder = findExistingComponentBuilder(id, builderTypeInfo.getValue(), builderTypeInfo.getKey())
                .orElseThrow(() -> new IllegalStateException(String.format("Failed to link to component '%s'! Cannot find existing placement", id)));
        builderConsumer.accept(builder);

        com.wolfyscript.utilities.common.gui.Component component = builder.create(null);

        SignalledObject signalledObject = new SignalledObject() {

            private boolean previousResult = false;

            @Override
            public void update(GuiViewManager guiViewManager, GuiHolder guiHolder, RenderContext context) {
                if (!(context instanceof RenderContextImpl renderContext)) return;
                boolean result = condition.get();
                if (result != previousResult) {
                    previousResult = result;
                    if (result) {
                        renderContext.enterNode(component);
                        for (int slot : component.getSlots()) {
                            renderContext.setSlotOffsetToParent(slot);
                            if (component.construct(guiHolder, viewManager) instanceof SignalledObject signalledObject) {
                                signalledObject.update(viewManager, guiHolder, renderContext);
                            }
                            component.executeForAllSlots(slot, slot2 -> ((GuiViewManagerImpl) guiHolder.getViewManager()).updateLeaveNodes(component, slot2));
                        }
                        renderContext.exitNode();
                    } else {
                        for (int slot : component.getSlots()) {
                            component.executeForAllSlots(slot, slot2 -> {
                                renderContext.setNativeStack(slot2, null);
                                ((GuiViewManagerImpl) guiHolder.getViewManager()).updateLeaveNodes(null, slot2);
                            });
                        }
                    }
                }
            }
        };

        for (Signal<?> signal : condition.getSignalsUsed()) {
            signal.linkTo(signalledObject);
        }
        return this;
    }

    @Override
    public <BV extends ComponentBuilder<? extends com.wolfyscript.utilities.common.gui.Component, com.wolfyscript.utilities.common.gui.Component>, BI extends ComponentBuilder<? extends com.wolfyscript.utilities.common.gui.Component, com.wolfyscript.utilities.common.gui.Component>> WindowDynamicConstructorImpl ifThenRenderOr(SerializableSupplier<Boolean> serializableSupplier, Class<BV> validBuilderType, Consumer<BV> validBuilder, Class<BI> invalidBuilderType, SerializableConsumer<BI> invalidBuilder) {
        return null;
    }

    @Override
    public <B extends ComponentBuilder<? extends com.wolfyscript.utilities.common.gui.Component, com.wolfyscript.utilities.common.gui.Component>> WindowDynamicConstructorImpl position(int slot, String id, Class<B> builderType, SerializableConsumer<B> builderConsumer) {
        Pair<NamespacedKey, Class<B>> builderTypeInfo = getBuilderType(wolfyUtils, id, builderType);

        findExistingComponentBuilder(id, builderTypeInfo.getValue(), builderTypeInfo.getKey()).ifPresentOrElse(builderConsumer, () -> {
            Injector injector = Guice.createInjector(Stage.PRODUCTION, binder -> {
                binder.bind(WolfyUtils.class).toInstance(wolfyUtils);
                binder.bind(String.class).toInstance(id);
            });
            B builder = injector.getInstance(builderTypeInfo.getValue());
            builderConsumer.accept(builder);
            componentBuilderPositions.put(builder, slot);
        });
        return this;
    }

    private <B extends ComponentBuilder<? extends com.wolfyscript.utilities.common.gui.Component, com.wolfyscript.utilities.common.gui.Component>> Optional<B> findExistingComponentBuilder(String id, Class<B> builderImplType, NamespacedKey builderKey) {
        return componentBuilderPositions.keySet().stream()
                .filter(componentBuilder -> componentBuilder.getID().equals(id) && componentBuilder.getType().equals(builderKey))
                .findFirst()
                .map(builderImplType::cast);
    }

    @Override
    public <B extends ComponentBuilder<? extends com.wolfyscript.utilities.common.gui.Component, com.wolfyscript.utilities.common.gui.Component>> WindowDynamicConstructorImpl render(String id, Class<B> builderType, SerializableConsumer<B> builderConsumer) {
        Pair<NamespacedKey, Class<B>> builderTypeInfo = getBuilderType(wolfyUtils, id, builderType);
        B builder = findExistingComponentBuilder(id, builderTypeInfo.getValue(), builderTypeInfo.getKey())
                .orElseThrow(() -> new IllegalStateException(String.format("Failed to link to component '%s'! Cannot find existing placement", id)));
        builderConsumer.accept(builder);
        componentRenderSet.add(builder);
        return this;
    }

    @Override
    public <B extends ComponentBuilder<? extends com.wolfyscript.utilities.common.gui.Component, com.wolfyscript.utilities.common.gui.Component>> WindowDynamicConstructorImpl renderAt(int slot, String id, Class<B> builderType, SerializableConsumer<B> builderConsumer) {
        Pair<NamespacedKey, Class<B>> builderTypeInfo = getBuilderType(wolfyUtils, id, builderType);
        findExistingComponentBuilder(id, builderTypeInfo.getValue(), builderTypeInfo.getKey()).ifPresentOrElse(builderConsumer, () -> {
            Injector injector = Guice.createInjector(Stage.PRODUCTION, binder -> {
                binder.bind(WolfyUtils.class).toInstance(wolfyUtils);
                binder.bind(String.class).toInstance(id);
            });
            B builder = injector.getInstance(builderTypeInfo.getValue());
            builderConsumer.accept(builder);
            componentBuilderPositions.put(builder, slot);
            componentRenderSet.add(builder);
        });
        return this;
    }

    static <B extends ComponentBuilder<? extends com.wolfyscript.utilities.common.gui.Component, com.wolfyscript.utilities.common.gui.Component>> Pair<NamespacedKey, Class<B>> getBuilderType(WolfyUtils wolfyUtils, String id, Class<B> builderType) {
        RegistryGUIComponentBuilders registry = wolfyUtils.getRegistries().getGuiComponentBuilders();
        NamespacedKey key = registry.getKey(builderType);
        Preconditions.checkArgument(key != null, "Failed to create component '%s'! Cannot find builder '%s' in registry!", id, builderType.getName());
        @SuppressWarnings("unchecked")
        Class<B> builderImplType = (Class<B>) registry.get(key); // We can be sure that the cast is valid, because the key is only non-null if and only if the type matches!
        Preconditions.checkNotNull(builderImplType, "Failed to create component '%s'! Cannot find implementation type of builder '%s' in registry!", id, builderType.getName());
        return new Pair<>(key, builderImplType);
    }

    public Window create(Window staticWindow) {
        Multimap<com.wolfyscript.utilities.common.gui.Component, Integer> finalPostions = ArrayListMultimap.create();
        Multimap<ComponentBuilder<?, ?>, Integer> nonRenderedComponents = ArrayListMultimap.create();

        for (var entry : componentBuilderPositions.asMap().entrySet()) {
            ComponentBuilder<?, ?> componentBuilder = entry.getKey();
            Collection<Integer> slots = entry.getValue();
            if (componentRenderSet.contains(componentBuilder)) {
                com.wolfyscript.utilities.common.gui.Component component = componentBuilder.create(null);
                finalPostions.putAll(component, slots);
                continue;
            }
            nonRenderedComponents.putAll(componentBuilder, slots);
        }

        if (titleFunction == null && !titleTagResolvers.isEmpty()) {
            titleFunction = () -> wolfyUtils.getChat().getMiniMessage().deserialize(((WindowImpl) staticWindow).getStaticTitle(), TagResolver.resolver(titleTagResolvers));
        }
        if (titleFunction != null) {
            SignalledObject signalledObject = (viewManager, guiHolder, renderContext) -> {
                ((GUIHolder) guiHolder).getNativePlayer().openInventory().flatMap(Container::currentMenu).ifPresent(inventoryMenu -> {
                    inventoryMenu.setTitle(titleFunction.get());
                });
            };
            for (Signal<?> signal : titleFunction.getSignalsUsed()) {
                signal.linkTo(signalledObject);
            }
            for (Signal<?> signal : titleSignals) {
                signal.linkTo(signalledObject);
            }
        }

        return ((WindowImpl) staticWindow).dynamicCopy(finalPostions, nonRenderedComponents, titleFunction);
    }

}
