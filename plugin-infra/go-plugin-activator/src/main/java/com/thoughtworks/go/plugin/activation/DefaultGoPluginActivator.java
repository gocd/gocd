/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.activation;

import com.thoughtworks.go.plugin.api.GoPluginApiMarker;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.annotation.Load;
import com.thoughtworks.go.plugin.api.annotation.UnLoad;
import com.thoughtworks.go.plugin.api.info.PluginContext;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.internal.api.LoggingService;
import com.thoughtworks.go.plugin.internal.api.PluginHealthService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;

/* Added as a part of the go-plugin-activator dependency JAR into each plugin.
 * Responsible for loading all classes in the plugin which are not in the dependency directory and registering services for the plugin extensions. */
public class DefaultGoPluginActivator implements GoPluginActivator {
    private List<String> errors = new ArrayList<>();
    private List<UnloadMethodInvoker> unloadMethodInvokers = new ArrayList<>();
    private PluginHealthService pluginHealthService;
    private static String pluginId;
    private static PluginContext DUMMY_PLUGIN_CONTEXT = new PluginContext() {
    };

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        Bundle bundle = bundleContext.getBundle();
        pluginId = bundle.getSymbolicName();
        pluginHealthService = bundleContext.getService(bundleContext.getServiceReference(PluginHealthService.class));

        LoggingService loggingService = bundleContext.getService(bundleContext.getServiceReference(LoggingService.class));
        Logger.initialize(loggingService);

        getImplementersAndRegister(bundleContext, bundle);

        reportErrorsToHealthService();
    }

    private void reportErrorsToHealthService() {
        if (!errors.isEmpty()) {
            pluginHealthService.reportErrorAndInvalidate(pluginId, errors);
        }
    }

    private void reportWarningToHealthService(String message) {
        pluginHealthService.warning(pluginId, message);
    }

    //invoked using reflection
    boolean hasErrors() {
        return !errors.isEmpty();
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        for (UnloadMethodInvoker unloadMethodInvoker : unloadMethodInvokers) {
            try {
                unloadMethodInvoker.invokeUnloadMethod();
            } catch (InvocationTargetException e) {
                errors.add(String.format("Invocation of unload method [%s]. Reason: %s.",
                        unloadMethodInvoker.unloadMethod, e.getTargetException().toString()));
            } catch (Throwable e) {
                errors.add(String.format("Invocation of unload method [%s]. Reason: [%s].",
                        unloadMethodInvoker.unloadMethod, e.toString()));
            }
            reportErrorsToHealthService();
        }
    }

    void getImplementersAndRegister(BundleContext bundleContext, Bundle bundle) throws ClassNotFoundException {
        List<HashMap<Class, List<Object>>> toRegister = new ArrayList<>();
        for (Class candidateGoExtensionClass : getCandidateGoExtensionClasses(bundle)) {
            HashMap<Class, List<Object>> interfaceToImplementations = getAllInterfaceToImplementationsMap(candidateGoExtensionClass);
            if (!interfaceToImplementations.isEmpty()) {
                toRegister.add(interfaceToImplementations);
            }
        }
        informIfNoExtensionFound(toRegister);
        registerAllServicesImplementedBy(bundleContext, toRegister);
    }

    private void informIfNoExtensionFound(List<HashMap<Class, List<Object>>> toRegister) {
        if(toRegister.isEmpty()){
            errors.add("No extensions found in this plugin.Please check for @Extension annotations");
        }
    }

    private void registerAllServicesImplementedBy(BundleContext bundleContext, List<HashMap<Class, List<Object>>> toRegister) {
        for (HashMap<Class, List<Object>> classListHashMap : toRegister) {
            for (Map.Entry<Class, List<Object>> entry : classListHashMap.entrySet()) {
                Class serviceInterface = entry.getKey();
                for (Object serviceImplementation : entry.getValue()) {
                    Hashtable<String, String> serviceProperties = new Hashtable<>();
                    serviceProperties.put(Constants.BUNDLE_SYMBOLICNAME, pluginId);
                    bundleContext.registerService(serviceInterface, serviceImplementation, serviceProperties);
                }
            }
        }
    }

    private HashMap<Class, List<Object>> getAllInterfaceToImplementationsMap(Class candidateGoExtensionClass) {
        HashMap<Class, List<Object>> interfaceAndItsImplementations = new HashMap<>();
        Set<Class> interfaces = findAllInterfacesInHierarchy(candidateGoExtensionClass);
        Object implementation = createImplementationOf(candidateGoExtensionClass);
        if (implementation == null) {
            return interfaceAndItsImplementations;
        }
        for (Class anInterface : interfaces) {
            if (isGoExtensionPointInterface(anInterface)) {
                List<Object> implementations = interfaceAndItsImplementations.get(anInterface);
                if (implementations == null) {
                    implementations = new ArrayList<>();
                    interfaceAndItsImplementations.put(anInterface, implementations);
                }
                implementations.add(implementation);
            }
        }
        return interfaceAndItsImplementations;
    }

    private Object createImplementationOf(Class candidateGoExtensionClass) {
        Object implementation = null;

        try {
            implementation = createInstance(candidateGoExtensionClass);
        } catch (InvocationTargetException e) {
            errors.add(String.format("Class [%s] is annotated with @Extension but cannot be constructed. Reason: %s.",
                    candidateGoExtensionClass.getSimpleName(), e.getTargetException().toString()));
        } catch (Throwable e) {
            errors.add(String.format("Class [%s] is annotated with @Extension but cannot be constructed. Reason: [%s].",
                    candidateGoExtensionClass.getSimpleName(), e.getCause()));
        }

        if (implementation != null) {
            validateAndLoad(candidateGoExtensionClass, implementation);
        }

        return implementation;
    }

    private void validateAndLoad(Class candidateGoExtensionClass, Object implementation) {
        try {
            processLoadAndUnloadAnnotatedMethods(implementation);
        } catch (InvocationTargetException e) {
            errors.add(String.format("Class [%s] is annotated with @Extension but cannot be registered. Reason: %s.",
                    candidateGoExtensionClass.getSimpleName(), e.getTargetException().toString()));
        } catch (IllegalAccessException e) {
            errors.add(String.format("Class [%s] is annotated with @Extension will not be registered. Reason: %s.",
                    candidateGoExtensionClass.getSimpleName(), e.toString()));
        } catch (RuntimeException e) {
            errors.add(String.format("Class [%s] is annotated with @Extension will not be registered. Reason: %s.",
                    candidateGoExtensionClass.getSimpleName(), e.toString()));
        } catch (Throwable e) {
            errors.add(String.format("Class [%s] is annotated with @Extension but cannot be constructed or registered. Reason: [%s].",
                    candidateGoExtensionClass.getSimpleName(), e.getCause()));
        }
    }

    private void processLoadAndUnloadAnnotatedMethods(Object implementation) throws InvocationTargetException, IllegalAccessException {
        Method loadAnnotatedMethod = getAnnotatedMethod(implementation, Load.class);
        Method unloadAnnotatedMethod = getAnnotatedMethod(implementation, UnLoad.class);
        if (loadAnnotatedMethod != null) {
            loadAnnotatedMethod.invoke(implementation, DUMMY_PLUGIN_CONTEXT);
        }
        if (unloadAnnotatedMethod != null) {
            this.unloadMethodInvokers.add(new UnloadMethodInvoker(implementation, unloadAnnotatedMethod));
        }
    }

    private Method getAnnotatedMethod(Object extensionObject, Class<? extends Annotation> annotation) {
        Method[] methods = getMethodsWithAnnotation(extensionObject, annotation);
        if (methods.length == 0) {
            return null;
        }
        if (methods.length > 1) {
            throw new RuntimeException("More than one method with @" + annotation.getSimpleName()
                    + " annotation not allowed. Methods Found: " + Arrays.toString(methods));
        }
        return methods[0];
    }

    private Method[] getMethodsWithAnnotation(Object extensionObject, Class<? extends Annotation> annotation) {
        // public,non-static,non-inherited zero-argument with @Load annotation
        Class<? extends Object> extnPointClass = extensionObject.getClass();
        ArrayList<Method> methodsWithLoadAnnotation = new ArrayList<>();
        for (Method method : extnPointClass.getDeclaredMethods()) {
            boolean annotated = hasAnnotation(annotation, method);
            if (annotated
                    && isPublic(method)
                    && isNonStatic(method)
                    && hasOneArgOfPluginContextType(method)) {
                methodsWithLoadAnnotation.add(method);
            } else if (annotated) {
                reportWarningsForAnnotatedMethod(method, annotation);
            }
        }
        return methodsWithLoadAnnotation.toArray(new Method[methodsWithLoadAnnotation.size()]);
    }

    private void reportWarningsForAnnotatedMethod(Method method, Class<? extends Annotation> annotation) {
        if (!isPublic(method)) {
            reportWarningToHealthService(
                    String.format("Ignoring method [%s] tagged with @%s since its not 'public'",
                            method, annotation.getSimpleName()));
            return;
        }
        if (!isNonStatic(method)) {
            reportWarningToHealthService(
                    String.format("Ignoring method [%s] tagged with @%s since its 'static' method",
                            method, annotation.getSimpleName()));
            return;
        }
        if (!hasOneArgOfPluginContextType(method)) {
            reportWarningToHealthService(
                    String.format("Ignoring method [%s] tagged with @%s since it does not have one argument of type PluginContext. Argument Type: []",
                            method, annotation.getSimpleName(), Arrays.toString(method.getParameterTypes())));
            return;
        }
    }

    private boolean hasOneArgOfPluginContextType(Method method) {
        return method.getParameterTypes().length == 1 && method.getParameterTypes()[0] == PluginContext.class;
    }

    private boolean isNonStatic(Method method) {
        return !Modifier.isStatic(method.getModifiers());
    }

    private boolean isPublic(Method method) {
        return Modifier.isPublic(method.getModifiers());
    }

    private boolean hasAnnotation(Class<? extends Annotation> annotation, Method method) {
        return method.getAnnotation(annotation) != null;
    }

    private List<Class> getCandidateGoExtensionClasses(Bundle bundle) throws ClassNotFoundException {
        List<Class> candidateClasses = new ArrayList<>();
        Enumeration<URL> entries = bundle.findEntries("/", "*.class", true);

        while (entries.hasMoreElements()) {
            String entryPath = entries.nextElement().getFile();
            if (isInvalidPath(entryPath)) {
                continue;
            }

            Class<?> candidateClass = loadClass(bundle, entryPath);
            if (candidateClass != null && isValidClass(candidateClass)) {
                candidateClasses.add(candidateClass);
            }
        }

        return candidateClasses;
    }

    Class<?> loadClass(Bundle bundle, String classFilePath) throws ClassNotFoundException {
        String className = classFilePath.replaceFirst("^/", "").replace('/', '.').replaceFirst(".class$", "");
        try {
            return bundle.loadClass(className);
        } catch (Throwable e) {
            errors.add(String.format("Class [%s] could not be loaded. Message: [%s].", className, e.getMessage()));
        }
        return null;
    }

    private boolean isInvalidPath(String entryPath) {
        return entryPath.startsWith("/lib/") || entryPath.startsWith("/META-INF/");
    }

    private boolean isValidClass(Class<?> candidateClass) {
        try {
            boolean doesNotHaveExtensionAnnotation = candidateClass.getAnnotation(Extension.class) == null;
            if (doesNotHaveExtensionAnnotation) {
                return false;
            }

            boolean isAbstract = Modifier.isAbstract(candidateClass.getModifiers());
            if (isAbstract) {
                errors.add(String.format("Class [%s] is annotated with @Extension but is abstract.", candidateClass.getSimpleName()));
                return false;
            }

            boolean isNotPublic = !Modifier.isPublic(candidateClass.getModifiers());
            if (isNotPublic) {
                errors.add(String.format("Class [%s] is annotated with @Extension but is not public.", candidateClass.getSimpleName()));
                return false;
            }

            return isInstantiable(candidateClass);
        } catch (NoSuchMethodException e) {
            errors.add(String.format(
                    "Class [%s] is annotated with @Extension but cannot be constructed. Make sure it and all of its parent classes have a default constructor.", candidateClass.getSimpleName()));
            return false;
        }
    }

    private boolean isInstantiable(Class<?> candidateClass) throws NoSuchMethodException {
        if (!isANonStaticInnerClass(candidateClass)) {
            boolean hasPublicDefaultConstructor = candidateClass.getConstructor() != null;
            return hasPublicDefaultConstructor;
        }

        boolean hasAConstructorWhichTakesMyOuterClass = candidateClass.getConstructor(candidateClass.getDeclaringClass()) != null;
        return hasAConstructorWhichTakesMyOuterClass && isInstantiable(candidateClass.getDeclaringClass());
    }

    private boolean isANonStaticInnerClass(Class<?> candidateClass) {
        return candidateClass.isMemberClass() && !Modifier.isStatic(candidateClass.getModifiers());
    }

    private Set<Class> findAllInterfacesInHierarchy(Class candidateGoExtensionClass) {
        Stack<Class> classesInHierarchy = new Stack<>();
        classesInHierarchy.add(candidateGoExtensionClass);

        Set<Class> interfaces = new HashSet<>();
        while (!classesInHierarchy.empty()) {
            Class classToCheckFor = classesInHierarchy.pop();
            if (classToCheckFor.isInterface()) {
                interfaces.add(classToCheckFor);
            }
            classesInHierarchy.addAll(Arrays.asList(classToCheckFor.getInterfaces()));

            if (classToCheckFor.getSuperclass() != null) {
                classesInHierarchy.add(classToCheckFor.getSuperclass());
            }
        }

        return interfaces;
    }

    private Object createInstance(Class candidateGoExtensionClass) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        if (isANonStaticInnerClass(candidateGoExtensionClass)) {
            Class declaringClass = candidateGoExtensionClass.getDeclaringClass();
            Object declaringClassInstance = createInstance(candidateGoExtensionClass.getDeclaringClass());
            return candidateGoExtensionClass.getConstructor(declaringClass).newInstance(declaringClassInstance);
        }
        Constructor constructor = candidateGoExtensionClass.getConstructor();
        return constructor.newInstance();
    }

    private boolean isGoExtensionPointInterface(Class anInterface) {
        boolean hasGoPluginApiMarkerAnnotation = anInterface.getAnnotation(GoPluginApiMarker.class) != null;
        boolean isAnInterfaceWhichHasBeenLeakedFromGoSystemBundle = GoPluginApiMarker.class.getClassLoader() == anInterface.getClassLoader();
        return isAnInterfaceWhichHasBeenLeakedFromGoSystemBundle && hasGoPluginApiMarkerAnnotation;
    }

    private static class UnloadMethodInvoker {
        private final Object object;
        private final Method unloadMethod;

        UnloadMethodInvoker(Object object, Method unloadMethod) {
            this.object = object;
            this.unloadMethod = unloadMethod;
        }

        void invokeUnloadMethod() throws InvocationTargetException, IllegalAccessException {
            this.unloadMethod.invoke(this.object, DUMMY_PLUGIN_CONTEXT);
        }
    }

}
