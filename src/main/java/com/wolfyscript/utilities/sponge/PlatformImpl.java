package com.wolfyscript.utilities.sponge;

import com.wolfyscript.utilities.WolfyCore;
import com.wolfyscript.utilities.platform.Audiences;
import com.wolfyscript.utilities.platform.Platform;
import com.wolfyscript.utilities.platform.gui.GuiUtils;
import com.wolfyscript.utilities.platform.scheduler.Scheduler;
import com.wolfyscript.utilities.platform.world.items.Items;
import com.wolfyscript.utilities.sponge.gui.GuiUtilsImpl;
import com.wolfyscript.utilities.sponge.scheduler.SchedulerImpl;
import com.wolfyscript.utilities.sponge.world.items.ItemsImpl;

public class PlatformImpl implements Platform {

    private final WolfyCore core;
    private final Items items = new ItemsImpl();
    private final Audiences audiences = new AudiencesImpl();
    private final Scheduler scheduler;
    private final GuiUtils guiUtils = new GuiUtilsImpl();

    public PlatformImpl(WolfyCore core) {
        this.core = core;
        this.scheduler = new SchedulerImpl();
    }

    @Override
    public Scheduler scheduler() {
        return scheduler;
    }

    @Override
    public Items items() {
        return items;
    }

    @Override
    public Audiences adventure() {
        return audiences;
    }

    @Override
    public GuiUtils guiUtils() {
        return guiUtils;
    }
}
