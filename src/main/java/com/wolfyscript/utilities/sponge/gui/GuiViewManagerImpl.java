package com.wolfyscript.utilities.sponge.gui;

import com.wolfyscript.utilities.common.WolfyUtils;
import com.wolfyscript.utilities.common.gui.*;
import com.wolfyscript.utilities.common.gui.impl.AbstractComponentImpl;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.type.CarriedInventory;

import java.util.*;

public class GuiViewManagerImpl extends GuiViewManagerCommonImpl {

    private static long NEXT_ID = Long.MIN_VALUE;

    private final long id;
    private final Map<Integer, Component> leaveNodes = new HashMap<>();
    private final Map<UUID, RenderContextImpl> viewerContexts = new HashMap<>();
    private final Set<SignalledObject> updatedSignalsSinceLastUpdate = new HashSet<>();

    protected GuiViewManagerImpl(WolfyUtils wolfyUtils, Router rootRouter, Set<UUID> viewers) {
        super(wolfyUtils, rootRouter, viewers);
        id = NEXT_ID++;
    }

    Optional<Component> getLeaveNode(int slot) {
        return Optional.ofNullable(leaveNodes.get(slot));
    }

    void updateObjects(Set<SignalledObject> objects) {
        updatedSignalsSinceLastUpdate.addAll(objects);
    }

    public void updateLeaveNodes(Component state, int... slots) {
        for (int slot : slots) {
            updateLeaveNodes(state, slot);
        }
    }

    void updateLeaveNodes(Component state, int slot) {
        if (state == null) {
            leaveNodes.remove(slot);
        } else {
            leaveNodes.put(slot, state);
        }
    }

    @Override
    public Optional<RenderContext> getRenderContext(UUID viewer) {
        return Optional.ofNullable(viewerContexts.get(viewer));
    }

    @Override
    public void openNew(String... path) {
        Window window = getRouter().open(this, path);
        setCurrentRoot(window);
        for (UUID viewer : getViewers()) {
            Sponge.server().player(viewer).ifPresent(serverPlayer -> {
                RenderContextImpl context = (RenderContextImpl) window.createContext(this, viewer);
                renderFor(serverPlayer, context);
            });
        }
    }

    void renderFor(ServerPlayer player, RenderContextImpl context) {
        if (!Objects.equals(player.openInventory().flatMap(Container::currentMenu).orElse(null), context.getInventory())) {
            viewerContexts.put(player.uniqueId(), context);
            context.getInventory().open(player);
            getCurrentMenu().ifPresent(window -> window.construct(this).render(((CarriedInventory<GUIHolder>) context.getInventory().inventory()).carrier().orElse(null), context));
        }
        GUIHolder holder = ((CarriedInventory<GUIHolder>) context.getInventory().inventory()).carrier().orElse(null);
        for (SignalledObject signalledObject : updatedSignalsSinceLastUpdate) {
            if (signalledObject instanceof AbstractComponentImpl component) {
                context.enterNode(component);
                for (int slot : component.getSlots()) {
                    context.setSlotOffsetToParent(slot);
                    signalledObject.update(this, holder, context);
                }
            } else {
                signalledObject.update(this, holder, context);
            }
        }
        updatedSignalsSinceLastUpdate.clear();

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GuiViewManagerImpl that = (GuiViewManagerImpl) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
