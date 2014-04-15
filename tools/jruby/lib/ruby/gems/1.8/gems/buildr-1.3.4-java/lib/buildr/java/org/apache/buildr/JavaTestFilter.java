/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership.  The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */


package org.apache.buildr;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.Vector;

public class JavaTestFilter {

  private ClassLoader _loader;
  private Vector methodAnnotations = new Vector();
  private Vector classAnnotations = new Vector();
  private Vector interfaces = new Vector();

  public JavaTestFilter(String[] paths) throws IOException {
    URL[] urls = new URL[paths.length];
    for (int i = 0 ; i < paths.length ; ++i) {
      File file = new File(paths[i]).getCanonicalFile();
      if (file.exists())
        urls[i] = file.toURL();
      else
        throw new IOException("No file or directory with the name " + file);
    }
    _loader = new URLClassLoader(urls, getClass().getClassLoader());
  }

  public JavaTestFilter addInterfaces(String[] names) throws ClassNotFoundException {
    for (int i = names.length; i -- > 0;) {
      String name = names[i];
      interfaces.add(_loader.loadClass(name));
    }
    return this;
  }

  public JavaTestFilter addClassAnnotations(String[] names) throws ClassNotFoundException {
    for (int i = names.length; i -- > 0;) {
      String name = names[i];
      classAnnotations.add(_loader.loadClass(name));
    }
    return this;
  }

  public JavaTestFilter addMethodAnnotations(String[] names) throws ClassNotFoundException {
    for (int i = names.length; i -- > 0;) {
      String name = names[i];
      methodAnnotations.add(_loader.loadClass(name));
    }
    return this;
  }
  
  private boolean isTest(Class cls) {
    if (Modifier.isAbstract(cls.getModifiers()) || !Modifier.isPublic(cls.getModifiers()))
      return false;
    if (interfaces != null) {
      for (Iterator it = interfaces.iterator(); it.hasNext(); ) {
        Class iface = (Class) it.next();
        if (iface.isAssignableFrom(cls)) { return true; }
      }
    }
    if (classAnnotations != null) { 
      for (Iterator it = classAnnotations.iterator(); it.hasNext(); ) {
        Class annotation = (Class) it.next();
        if (cls.isAnnotationPresent(annotation)) { return true; }
      }
    } 
    if (methodAnnotations != null) {
      Method[] methods = cls.getMethods();
      for (int j = methods.length ; j-- > 0 ;) {
        for (Iterator it = methodAnnotations.iterator(); it.hasNext(); ) {
          Class annotation = (Class) it.next();
          if (methods[j].isAnnotationPresent(annotation)) { return true; }
        }
      }
    }
    return false;
  }


  public String[] filter(String[] names) throws ClassNotFoundException {
    Vector testCases = new Vector();
    for (int i = names.length ; i-- > 0 ;) {
      Class cls = _loader.loadClass(names[i]);
      if (isTest(cls)) { testCases.add(names[i]); }
    }
    String[] result = new String[testCases.size()];
    testCases.toArray(result);
    return result;
  }

}

/* 
 * Local Variables:
 * indent-tabs-mode: nil
 * c-basic-offset: 2
 * End:
 */
