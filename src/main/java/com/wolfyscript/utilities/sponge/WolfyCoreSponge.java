package com.wolfyscript.utilities.sponge;

import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.wolfyscript.jackson.dataformat.hocon.HoconMapper;
import com.wolfyscript.utilities.Platform;
import com.wolfyscript.utilities.common.WolfyCore;
import com.wolfyscript.utilities.common.WolfyUtils;
import com.wolfyscript.utilities.common.chat.Chat;
import com.wolfyscript.utilities.common.gui.ComponentBuilder;
import com.wolfyscript.utilities.common.registry.Registries;
import com.wolfyscript.utilities.eval.value_provider.*;
import com.wolfyscript.utilities.json.KeyedTypeIdResolver;
import com.wolfyscript.utilities.json.annotations.OptionalKeyReference;
import com.wolfyscript.utilities.json.annotations.OptionalValueDeserializer;
import com.wolfyscript.utilities.json.annotations.OptionalValueSerializer;
import com.wolfyscript.utilities.json.jackson.JacksonUtil;
import com.wolfyscript.utilities.sponge.gui.WindowImpl;
import com.wolfyscript.utilities.sponge.gui.components.*;
import com.wolfyscript.utilities.sponge.gui.example.TestGUI;
import com.wolfyscript.utilities.sponge.gui.listeners.GUIListeners;
import com.wolfyscript.utilities.sponge.registry.SpongeRegistries;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.LinearComponents;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.ConfigManager;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StartingEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The main class of your Sponge plugin.
 *
 * <p>All methods are optional -- some common event registrations are included as a jumping-off point.</p>
 */
@Plugin("wolfyutils")
public class WolfyCoreSponge implements WolfyCore {

    protected Reflections reflections;
    protected SpongeRegistries registries;
    private WolfyUtilsSponge wolfyUtils;
    private final Map<String, WolfyUtilsSponge> wolfyUtilsInstances = new HashMap<>();
    private final List<SimpleModule> jsonMapperModules = new ArrayList<>();
    private final PluginContainer container;
    private final Logger logger;
    private final Path configDir;

    @Inject
    WolfyCoreSponge(final PluginContainer container, final Logger logger, @ConfigDir(sharedRoot = false) final Path configDir) {
        this.container = container;
        this.configDir = configDir;
        this.logger = logger;
    }

    @Listener
    public void onConstructPlugin(final ConstructPluginEvent event) {
        // Perform any one-time setup
        this.logger.info("Constructing WolfyUtils Core");

        Injector injector = Guice.createInjector(Stage.DEVELOPMENT, binder -> {
            binder.bind(WolfyCore.class).toInstance(this);
            binder.requestStaticInjection(KeyedTypeIdResolver.class);
        });
        this.wolfyUtils = getOrCreate(container, configDir);
        this.reflections = initReflections();
        this.registries = new SpongeRegistries(this);

        EventManager eventManager = Sponge.eventManager();
        eventManager.registerListeners(container, new GUIListeners(container));

        // De-/Serializer Modifiers that handle type references in JSON
        var keyReferenceModule = new SimpleModule();
        keyReferenceModule.setSerializerModifier(new OptionalKeyReference.SerializerModifier());
        keyReferenceModule.setDeserializerModifier(new OptionalKeyReference.DeserializerModifier(this));
        jsonMapperModules.add(keyReferenceModule);
        JacksonUtil.registerModule(keyReferenceModule);

        var valueReferenceModule = new SimpleModule();
        valueReferenceModule.setSerializerModifier(new OptionalValueSerializer.SerializerModifier());
        valueReferenceModule.setDeserializerModifier(new OptionalValueDeserializer.DeserializerModifier());
        jsonMapperModules.add(valueReferenceModule);
        JacksonUtil.registerModule(valueReferenceModule);

        // Create Global WUCore Mapper and apply modules
        HoconMapper mapper = applyWolfyUtilsJsonMapperModules(new HoconMapper());
        wolfyUtils.getJacksonMapperUtil().applyWolfyUtilsInjectableValues(mapper, new InjectableValues.Std());
        wolfyUtils.getJacksonMapperUtil().setGlobalMapper(mapper);

        // Register data
        var componentReg = registries.getGuiComponents();
        componentReg.register(ButtonImpl.class);
        componentReg.register(StackInputSlotImpl.class);
        componentReg.register(ComponentClusterImpl.class);

        var componentBuilderReg = registries.getGuiComponentBuilders();
        componentBuilderReg.register(ButtonBuilderImpl.class);
        componentBuilderReg.register(StackInputSlotBuilderImpl.class);
        componentBuilderReg.register(ComponentClusterBuilderImpl.class);

        logger.info("Register JSON Value Providers");
        var valueProviders = getRegistries().getValueProviders();
        // Custom
        valueProviders.register((Class<ValueProviderConditioned<?>>) (Object) ValueProviderConditioned.class);
        // Primitive
        valueProviders.register(ValueProviderShortConst.class);
        valueProviders.register(ValueProviderShortVar.class);
        valueProviders.register(ValueProviderIntegerConst.class);
        valueProviders.register(ValueProviderIntegerVar.class);
        valueProviders.register(ValueProviderLongConst.class);
        valueProviders.register(ValueProviderLongVar.class);
        valueProviders.register(ValueProviderFloatConst.class);
        valueProviders.register(ValueProviderFloatVar.class);
        valueProviders.register(ValueProviderDoubleConst.class);
        valueProviders.register(ValueProviderDoubleVar.class);
        valueProviders.register(ValueProviderStringConst.class);
        valueProviders.register(ValueProviderStringVar.class);
        // Arrays
        valueProviders.register(ValueProviderByteArrayConst.class);
        valueProviders.register(ValueProviderIntArrayConst.class);

        KeyedTypeIdResolver.registerTypeRegistry((Class<ValueProvider<?>>) (Object) ValueProvider.class, valueProviders);
        KeyedTypeIdResolver.registerTypeRegistry((Class<ComponentBuilder<?, ?>>) (Object) ComponentBuilder.class, componentBuilderReg);

        this.logger.info("Instance: " + event.plugin().instance().getClass().getName());
        this.logger.info("Config PAth: " + event.game().configManager().pluginConfig(container).directory().toString());

        TestGUI testGUI = new TestGUI(this);
        testGUI.initWithConfig();

    }

    private Command.Parameterized buildTestGUICommand() {
        return Command.builder()
                .executor(context -> {
                    context.cause().context().get(EventContextKeys.PLAYER).ifPresent(player -> {
                        wolfyUtils.getGUIManager().createViewAndOpen("example_counter", player.uniqueId());
                    });
                    return CommandResult.success();
                })
                .permission("wolfyutils.command.gui")
                .executionRequirements(cause -> cause.context().get(EventContextKeys.PLAYER).isPresent())
                .build();

    }

    public Path getConfigDir() {
        return configDir;
    }

    /**
     * Gets or create the {@link WolfyUtilsSponge} instance for the specified plugin.<br>
     * In case init is enabled it will directly initialize the event listeners and possibly other things.<br>
     *
     * @param pluginContainer The plugin to get the instance for.
     * @return The WolfyUtilities instance for the plugin.
     */
    public WolfyUtilsSponge getOrCreate(PluginContainer pluginContainer, Path configDir) {
        return wolfyUtilsInstances.computeIfAbsent(pluginContainer.metadata().id(), s -> new WolfyUtilsSponge(this, pluginContainer, configDir));
    }

    private Reflections initReflections() {
        return new Reflections(new ConfigurationBuilder().forPackages("com.wolfyscript").addClassLoaders(getClass().getClassLoader()).addScanners(Scanners.TypesAnnotated, Scanners.SubTypes, Scanners.Resources));
    }

    @Listener
    public void onServerStarting(final StartingEngineEvent<Server> event) {
        // Any setup per-game instance. This can run multiple times when
        // using the integrated (singleplayer) server.

    }

    @Listener
    public void onServerStopping(final StoppingEngineEvent<Server> event) {
        // Any tear down per-game instance. This can run multiple times when
        // using the integrated (singleplayer) server.
    }

    @Listener
    public void onRegisterCommands(final RegisterCommandEvent<Command.Parameterized> event) {
        // Register a simple command
        // When possible, all commands should be registered within a command register event
        event.register(this.container, Command.builder()
                .executor(context -> {
                    context.cause().first(ServerPlayer.class).ifPresent(serverPlayer -> {
                        wolfyUtils.getGUIManager().createViewAndOpen("example_counter", serverPlayer.uniqueId());
                    });
                    return CommandResult.success();
                })
                .permission("wolfyutils.command.gui")
                //.executionRequirements(cause -> cause.context().get(EventContextKeys.PLAYER).isPresent())
                .build(), "gui_example");

        final Parameter.Value<String> nameParam = Parameter.string().key("name").build();
        event.register(this.container, Command.builder()
                .addParameter(nameParam)
                .permission("wolfyutils-sponge.command.greet")
                .executor(ctx -> {
                    final String name = ctx.requireOne(nameParam);
                    ctx.sendMessage(Identity.nil(), LinearComponents.linear(
                            NamedTextColor.AQUA,
                            Component.text("Hello "),
                            Component.text(name, Style.style(TextDecoration.BOLD)),
                            Component.text("! How are you?")
                    ));

                    return CommandResult.success();
                })
                .build(), "greet", "wave");
    }

    @Override
    public Chat getChat() {
        return null;
    }

    @Override
    public <M extends ObjectMapper> M applyWolfyUtilsJsonMapperModules(M mapper) {
        mapper.registerModules(jsonMapperModules);
        return mapper;
    }

    @Override
    public WolfyUtils getWolfyUtils() {
        return wolfyUtils;
    }

    @Override
    public org.reflections.Reflections getReflections() {
        return reflections;
    }

    @Override
    public Registries getRegistries() {
        return registries;
    }

    @Override
    public Platform getPlatform() {
        return null;
    }

    public Logger getLogger() {
        return logger;
    }
}
