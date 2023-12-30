package com.wolfyscript.utilities.sponge.registry;

import com.wolfyscript.utilities.WolfyCore;
import com.wolfyscript.utilities.gui.Component;
import com.wolfyscript.utilities.registry.Registries;
import com.wolfyscript.utilities.registry.RegistryGUIComponentBuilders;
import com.wolfyscript.utilities.registry.TypeRegistry;
import com.wolfyscript.utilities.sponge.SpongeNamespacedKey;
import org.jetbrains.annotations.NotNull;

public class SpongeRegistries extends Registries {

    public static final SpongeNamespacedKey GUI_COMPONENTS = SpongeNamespacedKey.wolfyutilties("gui/components");
    public static final SpongeNamespacedKey GUI_COMPONENT_BUILDERS = SpongeNamespacedKey.wolfyutilties("gui/component_builders");

    private final TypeRegistry<Component> guiComponents;
    private final RegistryGUIComponentBuilders guiComponentBuilders;

    public SpongeRegistries(@NotNull WolfyCore core) {
        super(core);
        guiComponents = new RegistryGUIComponent(GUI_COMPONENTS, this);
        guiComponentBuilders = new RegistryGUIComponentBuilder(GUI_COMPONENT_BUILDERS, this);
    }

    @Override
    public TypeRegistry<Component> getGuiComponents() {
        return guiComponents;
    }

    @Override
    public RegistryGUIComponentBuilders getGuiComponentBuilders() {
        return guiComponentBuilders;
    }
}
