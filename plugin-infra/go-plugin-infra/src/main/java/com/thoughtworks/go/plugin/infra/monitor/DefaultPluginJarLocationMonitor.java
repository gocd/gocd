/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import org.apache.commons.collections4.Closure;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.thoughtworks.go.util.SystemEnvironment.*;

@Component
public class DefaultPluginJarLocationMonitor implements PluginJarLocationMonitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPluginJarLocationMonitor.class);


    private List<WeakReference<PluginJarChangeListener>> pluginJarChangeListener = new CopyOnWriteArrayList<>();

    private File bundledPluginDirectory;
    private final File externalPluginDirectory;
    private PluginLocationMonitorThread monitorThread;
    private SystemEnvironment systemEnvironment;

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
        pluginJarChangeListener.add(new WeakReference<>(listener));
        removeClearedWeakReferences();
    }

    @Override
    public void removePluginJarChangeListener(final PluginJarChangeListener listener) {
        WeakReference<PluginJarChangeListener> referenceOfListenerToBeRemoved = IterableUtils.find(pluginJarChangeListener, listenerWeakReference -> {
            PluginJarChangeListener registeredListener = listenerWeakReference.get();
            return registeredListener != null && registeredListener == listener;
        });
        pluginJarChangeListener.remove(referenceOfListenerToBeRemoved);
        removeClearedWeakReferences();
    }

    @Override
    public void start() {
        initializeMonitorThread();
        monitorThread.start();
    }

    @Override
    public boolean hasRunAtLeastOnce() {
        return getLastRun() > 0;
    }

    long getLastRun() {
        if (monitorThread == null) {
            return 0;
        }
        return monitorThread.getLastRun();
    }

    private void initializeMonitorThread() {
        if (monitorThread != null) {
            throw new IllegalStateException("Cannot start the monitor multiple times.");
        }
        monitorThread = new PluginLocationMonitorThread(bundledPluginDirectory, externalPluginDirectory, pluginJarChangeListener, systemEnvironment);
        monitorThread.setDaemon(true);
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
        Iterator<WeakReference<PluginJarChangeListener>> iterator = pluginJarChangeListener.iterator();
        while (iterator.hasNext()) {
            WeakReference<PluginJarChangeListener> next = iterator.next();
            if (next.get() == null) {
                iterator.remove();
            }
        }
    }

    private static class PluginLocationMonitorThread extends Thread {
        private Set<PluginFileDetails> knownBundledPluginFileDetails = new HashSet<>();
        private Set<PluginFileDetails> knownExternalPluginFileDetails = new HashSet<>();
        private File bundledPluginDirectory;
        private File externalPluginDirectory;
        private List<WeakReference<PluginJarChangeListener>> pluginJarChangeListener;
        private SystemEnvironment systemEnvironment;
        private long lastRun; //used for tests

        public PluginLocationMonitorThread(File bundledPluginDirectory, File externalPluginDirectory, List<WeakReference<PluginJarChangeListener>> pluginJarChangeListener, SystemEnvironment systemEnvironment) {
            this.bundledPluginDirectory = bundledPluginDirectory;
            this.externalPluginDirectory = externalPluginDirectory;
            this.pluginJarChangeListener = pluginJarChangeListener;
            this.systemEnvironment = systemEnvironment;
        }

        @Override
        public void run() {
            do {
                oneShot();

                int interval = systemEnvironment.get(PLUGIN_LOCATION_MONITOR_INTERVAL_IN_SECONDS);
                if (interval <= 0) {
                    break;
                }
                waitForMonitorInterval(interval);
            } while (!Thread.currentThread().isInterrupted());
        }


        //Added synchronized because the compiler can change the order of instructions, meaning that the lastRun can be
        //updated before the listeners are notified.
        public synchronized void oneShot() {
            knownBundledPluginFileDetails = loadAndNotifyPluginsFrom(bundledPluginDirectory, knownBundledPluginFileDetails, true);
            knownExternalPluginFileDetails = loadAndNotifyPluginsFrom(externalPluginDirectory, knownExternalPluginFileDetails, false);
            lastRun = System.currentTimeMillis();
        }

        synchronized long getLastRun() {
            return lastRun;
        }

        private Set<PluginFileDetails> loadAndNotifyPluginsFrom(File pluginDirectory, Set<PluginFileDetails> knownPluginFiles, boolean isBundledPluginsLocation) {
            Set<PluginFileDetails> currentPluginFiles = getDetailsOfCurrentPluginFilesFrom(pluginDirectory, isBundledPluginsLocation);
            notifyListenersOfRemovedPlugins(currentPluginFiles, knownPluginFiles);
            notifyListenersOfUpdatedPlugins(currentPluginFiles, knownPluginFiles);
            notifyListenersOfAddedPlugins(currentPluginFiles, knownPluginFiles);
            return currentPluginFiles;
        }

        private void notifyListenersOfAddedPlugins(Set<PluginFileDetails> currentPluginFiles, Set<PluginFileDetails> previouslyKnownPluginFiles) {
            HashSet<PluginFileDetails> currentPlugins = new HashSet<>(currentPluginFiles);
            currentPlugins.removeAll(previouslyKnownPluginFiles);

            for (PluginFileDetails newlyAddedPluginFile : currentPlugins) {
                doOnAllListeners().pluginJarAdded(newlyAddedPluginFile);
            }
        }

        private void notifyListenersOfRemovedPlugins(Set<PluginFileDetails> currentPluginFiles, Set<PluginFileDetails> previouslyKnownPluginFiles) {
            HashSet<PluginFileDetails> previouslyKnownPlugins = new HashSet<>(previouslyKnownPluginFiles);
            previouslyKnownPlugins.removeAll(currentPluginFiles);

            for (PluginFileDetails removedPluginFile : previouslyKnownPlugins) {
                doOnAllListeners().pluginJarRemoved(removedPluginFile);
            }
        }

        private void notifyListenersOfUpdatedPlugins(Set<PluginFileDetails> currentPluginFiles, Set<PluginFileDetails> knownPluginFileDetails) {
            final ArrayList<PluginFileDetails> updatedPlugins = findUpdatedPlugins(currentPluginFiles, knownPluginFileDetails);

            for (PluginFileDetails updatedPlugin : updatedPlugins) {
                doOnAllListeners().pluginJarUpdated(updatedPlugin);
            }
        }

        private PluginJarChangeListener doOnAllListeners() {
            return new DoOnAllListeners(pluginJarChangeListener);
        }

        private void waitForMonitorInterval(int interval) {
            try {
                Thread.sleep(interval * 1000);
            } catch (InterruptedException e) {
                this.interrupt();
            }
        }

        private Set<PluginFileDetails> getDetailsOfCurrentPluginFilesFrom(File directory, boolean isBundledPluginsLocation) {
            Set<PluginFileDetails> currentPluginFileDetails = new HashSet<>();
            for (Object fileOfPlugin : FileUtils.listFiles(directory, new String[]{"jar"}, false)) {
                currentPluginFileDetails.add(new PluginFileDetails((File) fileOfPlugin, isBundledPluginsLocation));
            }
            return currentPluginFileDetails;
        }

        private ArrayList<PluginFileDetails> findUpdatedPlugins(Set<PluginFileDetails> currentPluginFiles, Set<PluginFileDetails> knownPluginFileDetails) {
            final ArrayList<PluginFileDetails> currentPlugins = new ArrayList<>(currentPluginFiles);
            final ArrayList<PluginFileDetails> knownPlugins = new ArrayList<>(knownPluginFileDetails);

            CollectionUtils.filter(knownPlugins, object -> {
                PluginFileDetails knownPlugin = (PluginFileDetails) object;
                int i = currentPlugins.indexOf(knownPlugin);
                if (i == -1) {
                    return false;
                }
                PluginFileDetails plugin = currentPlugins.get(i);
                return plugin.doesTimeStampDiffer(knownPlugin);
            });
            return knownPlugins;
        }

        public static class DoOnAllListeners implements PluginJarChangeListener {
            private List<WeakReference<PluginJarChangeListener>> listeners;

            public DoOnAllListeners(List<WeakReference<PluginJarChangeListener>> listeners) {
                this.listeners = listeners;
            }

            @Override
            public void pluginJarAdded(final PluginFileDetails pluginFileDetails) {
                doOnAllPluginJarChangeListener(o -> o.pluginJarAdded(pluginFileDetails));
            }

            @Override
            public void pluginJarUpdated(final PluginFileDetails pluginFileDetails) {
                doOnAllPluginJarChangeListener(o -> o.pluginJarUpdated(pluginFileDetails));
            }

            @Override
            public void pluginJarRemoved(final PluginFileDetails pluginFileDetails) {
                doOnAllPluginJarChangeListener(o -> o.pluginJarRemoved(pluginFileDetails));
            }

            private void doOnAllPluginJarChangeListener(Closure<PluginJarChangeListener> closure) {
                for (WeakReference<PluginJarChangeListener> listener : listeners) {
                    PluginJarChangeListener changeListener = listener.get();
                    if (changeListener == null) {
                        continue;
                    }

                    try {
                        closure.execute(changeListener);
                    } catch (Exception e) {
                        LOGGER.warn("Plugin listener failed", e);
                    }
                }
            }
        }

    }
}
