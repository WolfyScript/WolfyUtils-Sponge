package com.wolfyscript.utilities.sponge.scheduler;

import com.google.common.base.Preconditions;
import com.wolfyscript.utilities.WolfyUtils;
import com.wolfyscript.utilities.platform.scheduler.Task;
import com.wolfyscript.utilities.sponge.WolfyUtilsSponge;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.util.Ticks;

import java.util.function.Consumer;

class TaskImpl implements Task {

    private final WolfyUtils wolfyUtils;
    private final ScheduledTask scheduledTask;

    TaskImpl(WolfyUtils wolfyUtils, ScheduledTask scheduledTask) {
        this.wolfyUtils = wolfyUtils;
        this.scheduledTask = scheduledTask;
    }

    @Override
    public void cancel() {
        scheduledTask.cancel();
    }

    @Override
    public WolfyUtils api() {
        return wolfyUtils;
    }

    static class BuilderImpl implements Builder {

        private final WolfyUtils wolfyUtils;
        private boolean async = false;
        private org.spongepowered.api.scheduler.Task.Builder builder;
        private Consumer<Task> executor;

        BuilderImpl(WolfyUtils wolfyUtils) {
            this.wolfyUtils = wolfyUtils;
            builder = org.spongepowered.api.scheduler.Task.builder();
            builder.plugin(((WolfyUtilsSponge) wolfyUtils).getPluginContainer());
        }

        @Override
        public Builder async() {
            this.async = true;
            return this;
        }

        @Override
        public Builder delay(long l) {
            builder.delay(Ticks.of(l));
            return this;
        }

        @Override
        public Builder interval(long l) {
            builder.interval(Ticks.of(l));
            return this;
        }

        @Override
        public Builder execute(Runnable runnable) {
            builder.execute(runnable);
            return this;
        }

        @Override
        public Builder execute(Consumer<Task> consumer) {
            this.executor = consumer;
            return this;
        }

        @Override
        public Task build() {
            if (executor != null) {
                ScheduledTaskConsumerWrapper scheduledTaskConsumerWrapper = new ScheduledTaskConsumerWrapper(wolfyUtils, executor);
                builder.execute(scheduledTaskConsumerWrapper);
            }
            org.spongepowered.api.scheduler.Task spongeTask = builder.build();

            ScheduledTask scheduledTask;
            if (async) {
                scheduledTask = Sponge.asyncScheduler().submit(spongeTask);
            } else {
                scheduledTask = Sponge.server().scheduler().submit(spongeTask);
            }
            return new TaskImpl(wolfyUtils, scheduledTask);
        }
    }

    private static class ScheduledTaskConsumerWrapper implements Consumer<ScheduledTask> {

        private final WolfyUtils wolfyUtils;
        private Task task;
        private final Consumer<Task> executor;

        public ScheduledTaskConsumerWrapper(WolfyUtils wolfyUtils, Consumer<Task> executor) {
            Preconditions.checkArgument(executor != null, "Invalid Runnable: Must provide a executor!");
            this.wolfyUtils = wolfyUtils;
            this.executor = executor;
        }

        public void setTask(Task task) {
            this.task = task;
        }

        public Task getTask() {
            return task;
        }

        @Override
        public void accept(ScheduledTask scheduledTask) {
            if (task == null) {
                task = new TaskImpl(wolfyUtils, scheduledTask);
            }
            executor.accept(task);
        }
    }
}
