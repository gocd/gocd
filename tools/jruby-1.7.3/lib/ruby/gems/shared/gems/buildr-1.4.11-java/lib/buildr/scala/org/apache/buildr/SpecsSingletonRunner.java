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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.io.File;

/**
 * @author Daniel Spiewak
 */
public class SpecsSingletonRunner {
  // Incompatible with JVM 1.4 target
  // @throws(classOf[Throwable])
  public static void main(String[] args) {
      boolean colors = (args.length > 1 && args[1].equals("-c"));
      String spec = colors ? args[2] : args[1];

      run(args[0], colors, spec);
  }

  // Incompatible with JVM 1.4 target
  // @throws(classOf[Throwable])
  static void run(String path, boolean colors, String spec) {
      try {
          File parent = new File(path);
          URL specURL = new File(parent, spec.replace('.', '/') + ".class").toURL();
          URLClassLoader loader = new URLClassLoader(new URL[] { specURL }, Thread.currentThread().getContextClassLoader());

          Class clazz = loader.loadClass(spec);
          Object instance = clazz.getField("MODULE$").get(null);

          Method main = clazz.getMethod("main", String[].class);

          String[] args = colors ? new String[] { "-c" } : new String[] {};
          main.invoke(instance, new Object[] { args });
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
  }
}
