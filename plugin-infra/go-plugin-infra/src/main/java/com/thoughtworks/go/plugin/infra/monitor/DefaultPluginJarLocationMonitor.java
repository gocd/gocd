/*
 * Copyright 2024 Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.plugin.infra.monitor;

import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.thoughtworks.go.util.SystemEnvironment.*;

@Component
public class DefaultPluginJarLocationMonitor implements PluginJarLocationMonitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPluginJarLocationMonitor.class);
    private final List<WeakReference<PluginJarChangeListener>> pluginJarChangeListeners = new CopyOnWriteArrayList<>();

    private final File bundledPluginDirectory;
    private final File externalPluginDirectory;
    private final SystemEnvironment systemEnvironment;
    private volatile PluginLocationMonitorThread monitorThread;

    @Autowired
    public DefaultPluginJarLocationMonitor(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
        this.bundledPluginDirectory = new File(this.systemEnvironment.get(PLUGIN_GO_PROVIDED_PATH));
        this.externalPluginDirectory = new File(this.systemEnvironment.get(PLUGIN_EXTERNAL_PROVIDED_PATH));
    }

    public void initialize() {
        validateBundledPluginDirectory();
        validateExternalPluginDirectory();
    }

    @Override
    public void addPluginJarChangeListener(PluginJarChangeListener listener) {
        pluginJarChangeListeners.add(new WeakReference<>(listener));
        removeClearedWeakReferences();
    }

    @Override
    public void removePluginJarChangeListener(final PluginJarChangeListener listener) {
        WeakReference<PluginJarChangeListener> referenceOfListenerToBeRemoved = IterableUtils.find(pluginJarChangeListeners, listenerWeakReference -> {
            PluginJarChangeListener registeredListener = listenerWeakReference.get();
            return registeredListener != null && registeredListener == listener;
        });
        pluginJarChangeListeners.remove(referenceOfListenerToBeRemoved);
        removeClearedWeakReferences();
    }

    @Override
    public void start() {
        initializeMonitorThread();
        monitorThread.start();
    }

    @Override
    public boolean hasRunAtLeastOnce() {
        return hasRunSince(0);
    }

    public boolean hasRunSince(long timestamp) {
        return Optional.ofNullable(monitorThread).map(t -> t.hasRunSince(timestamp)).orElse(false);
    }

    private void initializeMonitorThread() {
        if (monitorThread != null) {
            throw new IllegalStateException("Cannot start the monitor multiple times.");
        }
        monitorThread = new PluginLocationMonitorThread(bundledPluginDirectory, externalPluginDirectory, pluginJarChangeListeners, systemEnvironment);
    }

    @Override
    public void oneShot() {
        initializeMonitorThread();
        monitorThread.oneShot();
    }

    @Override
    public void stop() {
        if (monitorThread == null) {
            return;
        }

        monitorThread.interrupt();
        try {
            monitorThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        monitorThread = null;
    }

    public void validateBundledPluginDirectory() {
        if (bundledPluginDirectory.exists()) {
            return;
        }
        try {
            LOGGER.debug("Force creating the plugins jar directory as it does not exist {}", bundledPluginDirectory.getAbsolutePath());
            FileUtils.forceMkdir(bundledPluginDirectory);
        } catch (IOException e) {
            String message = "Failed to create plugins folder in location " + bundledPluginDirectory.getAbsolutePath();
            LOGGER.warn(message, e);
            throw new RuntimeException(message, e);
        }
    }

    private void validateExternalPluginDirectory() {
        if (externalPluginDirectory.exists()) {
            return;
        }
        try {
            LOGGER.debug("Force creating the plugins jar directory as it does not exist {}", externalPluginDirectory.getAbsolutePath());
            FileUtils.forceMkdir(externalPluginDirectory);
        } catch (IOException e) {
            String message = "Failed to create external plugins folder in location " + externalPluginDirectory.getAbsolutePath();
            LOGGER.warn(message, e);
            throw new RuntimeException(message, e);
        }
    }

    private void removeClearedWeakReferences() {
        pluginJarChangeListeners.removeIf(next -> next.get() == null);
    }

    private static class PluginLocationMonitorThread extends Thread {
        private Set<BundleOrPluginFileDetails> knownBundledBundleOrPluginFileDetails = new HashSet<>();
        private Set<BundleOrPluginFileDetails> knownExternalBundleOrPluginFileDetails = new HashSet<>();
        private final PluginChangeNotifier pluginChangeNotifier = new PluginChangeNotifier();
        private final File bundledPluginDirectory;
        private final File externalPluginDirectory;
        private final List<WeakReference<PluginJarChangeListener>> pluginJarChangeListener;
        private final SystemEnvironment systemEnvironment;
        private volatile long lastRun;

        public PluginLocationMonitorThread(File bundledPluginDirectory,
                                           File externalPluginDirectory,
                                           List<WeakReference<PluginJarChangeListener>> pluginJarChangeListener,
                                           SystemEnvironment systemEnvironment) {
            super("goPluginLocationMonitor");
            setDaemon(true);
            this.bundledPluginDirectory = bundledPluginDirectory;
            this.externalPluginDirectory = externalPluginDirectory;
            this.pluginJarChangeListener = pluginJarChangeListener;
            this.systemEnvironment = systemEnvironment;
        }

        @Override
        public void run() {
            do {
                oneShot();

                long interval = systemEnvironment.getPluginLocationMonitorIntervalInMillis();
                if (interval <= 0) {
                    break;
                }
                waitForMonitorInterval(interval);
            } while (!Thread.currentThread().isInterrupted());
        }


        //Added synchronized because the compiler can change the order of instructions, meaning that the lastRun can be
        //updated before the listeners are notified.
        public synchronized void oneShot() {
            knownBundledBundleOrPluginFileDetails = loadAndNotifyPluginsFrom(bundledPluginDirectory, knownBundledBundleOrPluginFileDetails, true);
            knownExternalBundleOrPluginFileDetails = loadAndNotifyPluginsFrom(externalPluginDirectory, knownExternalBundleOrPluginFileDetails, false);
            lastRun = System.currentTimeMillis();
        }

        boolean hasRunSince(long timestamp) {
            return lastRun > timestamp;
        }

        private Set<BundleOrPluginFileDetails> loadAndNotifyPluginsFrom(File pluginDirectory,
                                                                        Set<BundleOrPluginFileDetails> previouslyKnownPlugins,
                                                                        boolean isBundledPluginsLocation) {
            Set<BundleOrPluginFileDetails> currentPluginFiles = getDetailsOfCurrentPluginFilesFrom(pluginDirectory, isBundledPluginsLocation);
            pluginChangeNotifier.notify(doOnAllListeners(), previouslyKnownPlugins, currentPluginFiles);
            return currentPluginFiles;
        }

        private PluginJarChangeListener doOnAllListeners() {
            return new DoOnAllListeners(pluginJarChangeListener);
        }

        private void waitForMonitorInterval(long intervalMillis) {
            try {
                Thread.sleep(intervalMillis);
            } catch (InterruptedException e) {
                this.interrupt();
            }
        }

        private Set<BundleOrPluginFileDetails> getDetailsOfCurrentPluginFilesFrom(File directory,
                                                                                  boolean isBundledPluginsLocation) {
            File pluginWorkDir = new File(systemEnvironment.get(PLUGIN_WORK_DIR));
            return FileUtils.listFiles(directory, new String[]{"jar"}, false).stream()
                    .map(file -> new BundleOrPluginFileDetails(file, isBundledPluginsLocation, pluginWorkDir))
                    .collect(Collectors.toSet());
        }

        public static class DoOnAllListeners implements PluginJarChangeListener {
            private final List<WeakReference<PluginJarChangeListener>> listeners;

            public DoOnAllListeners(List<WeakReference<PluginJarChangeListener>> listeners) {
                this.listeners = listeners;
            }

            @Override
            public void pluginJarAdded(final BundleOrPluginFileDetails bundleOrPluginFileDetails) {
                doOnAllPluginJarChangeListener(o -> o.pluginJarAdded(bundleOrPluginFileDetails));
            }

            @Override
            public void pluginJarUpdated(final BundleOrPluginFileDetails bundleOrPluginFileDetails) {
                doOnAllPluginJarChangeListener(o -> o.pluginJarUpdated(bundleOrPluginFileDetails));
            }

            @Override
            public void pluginJarRemoved(final BundleOrPluginFileDetails bundleOrPluginFileDetails) {
                doOnAllPluginJarChangeListener(o -> o.pluginJarRemoved(bundleOrPluginFileDetails));
            }

            private void doOnAllPluginJarChangeListener(Consumer<PluginJarChangeListener> closure) {
                for (WeakReference<PluginJarChangeListener> listener : listeners) {
                    PluginJarChangeListener changeListener = listener.get();
                    if (changeListener == null) {
                        continue;
                    }

                    try {
                        closure.accept(changeListener);
                    } catch (Exception e) {
                        LOGGER.warn("Plugin listener failed", e);
                    }
                }
            }
        }

    }
}
