package com.wolfyscript.utilities.sponge.scheduler;

import com.wolfyscript.utilities.WolfyUtils;
import com.wolfyscript.utilities.platform.scheduler.Scheduler;
import com.wolfyscript.utilities.platform.scheduler.Task;

public class SchedulerImpl implements Scheduler {

    @Override
    public Task.Builder task(WolfyUtils wolfyUtils) {
        return new TaskImpl.BuilderImpl(wolfyUtils);
    }

    @Override
    public Task syncTask(WolfyUtils wolfyUtils, Runnable runnable, int i) {
        return task(wolfyUtils).execute(runnable).delay(i).build();
    }

    @Override
    public Task asyncTask(WolfyUtils wolfyUtils, Runnable runnable, int i) {
        return task(wolfyUtils).async().execute(runnable).delay(i).build();
    }

    @Override
    public Task syncTimerTask(WolfyUtils wolfyUtils, Runnable runnable, int i, int i1) {
        return task(wolfyUtils).execute(runnable).delay(i).interval(i1).build();
    }

    @Override
    public Task asyncTimerTask(WolfyUtils wolfyUtils, Runnable runnable, int i, int i1) {
        return task(wolfyUtils).async().execute(runnable).delay(i).interval(i1).build();
    }
}
