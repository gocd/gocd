/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.plugin.infra.monitor;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.collections.Closure;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_EXTERNAL_PROVIDED_PATH;
import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_GO_PROVIDED_PATH;
import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_LOCATION_MONITOR_INTERVAL_IN_SECONDS;

@Component
public class DefaultPluginJarLocationMonitor implements PluginJarLocationMonitor {
    private static final Logger LOGGER = Logger.getLogger(DefaultPluginJarLocationMonitor.class);


    private List<WeakReference<PluginJarChangeListener>> listeners = new CopyOnWriteArrayList<WeakReference<PluginJarChangeListener>>();

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
        listeners.add(new WeakReference<PluginJarChangeListener>(listener));
        removeClearedWeakReferences();
    }

    @Override
    public void removePluginJarChangeListener(final PluginJarChangeListener listener) {
        Object referenceOfListenerToBeRemoved = CollectionUtils.find(listeners, new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                WeakReference<PluginJarChangeListener> listenerWeakReference = (WeakReference<PluginJarChangeListener>) object;
                PluginJarChangeListener registeredListener = listenerWeakReference.get();
                return registeredListener != null && registeredListener == listener;
            }
        });
        listeners.remove(referenceOfListenerToBeRemoved);
        removeClearedWeakReferences();
    }

    @Override
    public void start() {
        if (monitorThread != null) {
            throw new IllegalStateException("Cannot start the monitor multiple times.");
        }
        monitorThread = new PluginLocationMonitorThread(bundledPluginDirectory, externalPluginDirectory, listeners, systemEnvironment);
        monitorThread.setDaemon(true);
        monitorThread.start();
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
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Force creating the plugins jar directory as it does not exist " + bundledPluginDirectory.getAbsolutePath());
            }
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
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Force creating the plugins jar directory as it does not exist " + externalPluginDirectory.getAbsolutePath());
            }
            FileUtils.forceMkdir(externalPluginDirectory);
        } catch (IOException e) {
            String message = "Failed to create external plugins folder in location " + externalPluginDirectory.getAbsolutePath();
            LOGGER.warn(message, e);
            throw new RuntimeException(message, e);
        }
    }

    private void removeClearedWeakReferences() {
        Iterator<WeakReference<PluginJarChangeListener>> iterator = listeners.iterator();
        while (iterator.hasNext()) {
            WeakReference<PluginJarChangeListener> next = iterator.next();
            if (next.get() == null) {
                iterator.remove();
            }
        }
    }

    private static class PluginLocationMonitorThread extends Thread {
        private Set<PluginFileDetails> knownBundledPluginFileDetails = new HashSet<PluginFileDetails>();
        private Set<PluginFileDetails> knownExternalPluginFileDetails = new HashSet<PluginFileDetails>();
        private File bundledPluginDirectory;
        private File externalPluginDirectory;
        private List<WeakReference<PluginJarChangeListener>> listeners;
        private SystemEnvironment systemEnvironment;

        public PluginLocationMonitorThread(File bundledPluginDirectory, File externalPluginDirectory, List<WeakReference<PluginJarChangeListener>> listeners, SystemEnvironment systemEnvironment) {
            this.bundledPluginDirectory = bundledPluginDirectory;
            this.externalPluginDirectory = externalPluginDirectory;
            this.listeners = listeners;
            this.systemEnvironment = systemEnvironment;
        }

        @Override
        public void run() {
            do {
                knownBundledPluginFileDetails = loadAndNotifyPluginsFrom(bundledPluginDirectory, knownBundledPluginFileDetails, true);
                knownExternalPluginFileDetails = loadAndNotifyPluginsFrom(externalPluginDirectory, knownExternalPluginFileDetails, false);

                int interval = systemEnvironment.get(PLUGIN_LOCATION_MONITOR_INTERVAL_IN_SECONDS);
                if (interval <= 0) {
                    break;
                }
                waitForMonitorInterval(interval);
            } while (!Thread.currentThread().isInterrupted());
        }

        private Set<PluginFileDetails> loadAndNotifyPluginsFrom(File pluginDirectory, Set<PluginFileDetails> knownPluginFiles, boolean isBundledPluginsLocation) {
            Set<PluginFileDetails> currentPluginFiles = getDetailsOfCurrentPluginFilesFrom(pluginDirectory, isBundledPluginsLocation);
            notifyListenersOfRemovedPlugins(currentPluginFiles, knownPluginFiles);
            notifyListenersOfUpdatedPlugins(currentPluginFiles, knownPluginFiles);
            notifyListenersOfAddedPlugins(currentPluginFiles, knownPluginFiles);
            return currentPluginFiles;
        }

        private void notifyListenersOfAddedPlugins(Set<PluginFileDetails> currentPluginFiles, Set<PluginFileDetails> previouslyKnownPluginFiles) {
            HashSet<PluginFileDetails> currentPlugins = new HashSet<PluginFileDetails>(currentPluginFiles);
            currentPlugins.removeAll(previouslyKnownPluginFiles);

            for (PluginFileDetails newlyAddedPluginFile : currentPlugins) {
                doOnAllListeners().pluginJarAdded(newlyAddedPluginFile);
            }
        }

        private void notifyListenersOfRemovedPlugins(Set<PluginFileDetails> currentPluginFiles, Set<PluginFileDetails> previouslyKnownPluginFiles) {
            HashSet<PluginFileDetails> previouslyKnownPlugins = new HashSet<PluginFileDetails>(previouslyKnownPluginFiles);
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
            return new DoOnAllListeners(listeners);
        }

        private void waitForMonitorInterval(int interval) {
            try {
                Thread.sleep(interval * 1000);
            } catch (InterruptedException e) {
                this.interrupt();
            }
        }

        private Set<PluginFileDetails> getDetailsOfCurrentPluginFilesFrom(File directory, boolean isBundledPluginsLocation) {
            Set<PluginFileDetails> currentPluginFileDetails = new HashSet<PluginFileDetails>();
            for (Object fileOfPlugin : FileUtils.listFiles(directory, new String[]{"jar"}, false)) {
                currentPluginFileDetails.add(new PluginFileDetails((File) fileOfPlugin, isBundledPluginsLocation));
            }
            return currentPluginFileDetails;
        }

        private ArrayList<PluginFileDetails> findUpdatedPlugins(Set<PluginFileDetails> currentPluginFiles, Set<PluginFileDetails> knownPluginFileDetails) {
            final ArrayList<PluginFileDetails> currentPlugins = new ArrayList<PluginFileDetails>(currentPluginFiles);
            final ArrayList<PluginFileDetails> knownPlugins = new ArrayList<PluginFileDetails>(knownPluginFileDetails);

            CollectionUtils.filter(knownPlugins, new Predicate() {
                @Override
                public boolean evaluate(Object object) {
                    PluginFileDetails knownPlugin = (PluginFileDetails) object;
                    int i = currentPlugins.indexOf(knownPlugin);
                    if (i == -1) {
                        return false;
                    }
                    PluginFileDetails plugin = currentPlugins.get(i);
                    return plugin.doesTimeStampDiffer(knownPlugin);
                }
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
                doOnAll(new Closure() {
                    public void execute(Object o) {
                        ((PluginJarChangeListener) o).pluginJarAdded(pluginFileDetails);
                    }
                });
            }

            @Override
            public void pluginJarUpdated(final PluginFileDetails pluginFileDetails) {
                doOnAll(new Closure() {
                    public void execute(Object o) {
                        ((PluginJarChangeListener) o).pluginJarUpdated(pluginFileDetails);
                    }
                });
            }

            @Override
            public void pluginJarRemoved(final PluginFileDetails pluginFileDetails) {
                doOnAll(new Closure() {
                    public void execute(Object o) {
                        ((PluginJarChangeListener) o).pluginJarRemoved(pluginFileDetails);
                    }
                });
            }

            private void doOnAll(Closure closure) {
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
