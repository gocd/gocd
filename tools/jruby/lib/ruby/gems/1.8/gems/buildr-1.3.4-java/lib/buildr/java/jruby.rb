# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with this
# work for additional information regarding copyright ownership.  The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations under
# the License.


require 'java'
require 'jruby'


# Buildr runs along side a JVM, using either RJB or JRuby.  The Java module allows
# you to access Java classes and create Java objects.
#
# Java classes are accessed as static methods on the Java module, for example:
#   str = Java.java.lang.String.new('hai!')
#   str.toUpperCase
#   => 'HAI!'
#   Java.java.lang.String.isInstance(str)
#   => true
#   Java.com.sun.tools.javac.Main.compile(args)
#
# The classpath attribute allows Buildr to add JARs and directories to the classpath,
# for example, we use it to load Ant and various Ant tasks, code generators, test
# frameworks, and so forth.
#
# When using an artifact specification, Buildr will automatically download and
# install the artifact before adding it to the classpath.
#
# For example, Ant is loaded as follows:
#   Java.classpath << 'org.apache.ant:ant:jar:1.7.0'
#
# Artifacts can only be downloaded after the Buildfile has loaded, giving it
# a chance to specify which remote repositories to use, so adding to classpath
# does not by itself load any libraries.  You must call Java.load before accessing
# any Java classes to give Buildr a chance to load the libraries specified in the
# classpath.
#
# When building an extension, make sure to follow these rules:
# 1. Add to the classpath when the extension is loaded (i.e. in module or class
#    definition), so the first call to Java.load anywhere in the code will include
#    the libraries you specify.
# 2. Call Java.load once before accessing any Java classes, allowing Buildr to
#    set up the classpath.
# 3. Only call Java.load when invoked, otherwise you may end up loading the JVM
#    with a partial classpath, or before all remote repositories are listed.
# 4. Check on a clean build with empty local repository.
module Java

  # Since we already have a JVM loaded, we can use it to guess where JAVA_HOME is.
  # We set JAVA_HOME early so we can use it without calling Java.load first.
  ENV['JAVA_HOME'] ||= java.lang.System.getProperty("java.home")

  class << self

    # Returns the classpath, an array listing directories, JAR files and
    # artifacts.  Use when loading the extension to add any additional
    # libraries used by that extension.
    #
    # For example, Ant is loaded as follows:
    #   Java.classpath << 'org.apache.ant:ant:jar:1.7.0'
    def classpath
      @classpath ||= []
    end
    
    # Most platforms requires tools.jar to be on the classpath, tools.jar contains the
    # Java compiler (OS X and AIX are two exceptions we know about, may be more).
    # Guess where tools.jar is from JAVA_HOME, which hopefully points to the JDK,
    # but maybe the JRE.  Return nil if not found.
    def tools_jar #:nodoc:
      @tools_jar ||= ['lib/tools.jar', '../lib/tools.jar'].map { |path| File.expand_path(path, ENV['JAVA_HOME']) }.
        find { |path| File.exist?(path) }
    end

    # Loads the JVM and all the libraries listed on the classpath.  Call this
    # method before accessing any Java class, but only call it from methods
    # used in the build, giving the Buildfile a chance to load all extensions
    # that append to the classpath and specify which remote repositories to use.
    def load
      return self if @loaded

      # Adding jars to the jruby's $CLASSPATH should be the correct thing, however it
      # seems like some tools require their jars on system class loader (javadoc, junit, etc)
      # cp.each { |path| $CLASSPATH << path }

      # Use system ClassLoader to add classpath
      sysloader = java.lang.ClassLoader.getSystemClassLoader
      add_url_method = java.lang.Class.forName('java.net.URLClassLoader').
        getDeclaredMethod('addURL', [java.net.URL.java_class].to_java(java.lang.Class))
      add_url_method.setAccessible(true)
      add_path = lambda { |path| add_url_method.invoke(sysloader, [java.io.File.new(path).toURI.toURL].to_java(java.net.URL)) }

      # Most platforms requires tools.jar to be on the classpath.
      add_path[tools_jar] if tools_jar
      
      classpath.map! { |path| Proc === path ? path.call : path }      
      Buildr.artifacts(classpath).map(&:to_s).each do |path|
        file(path).invoke
        add_path[path]
      end
      
      @loaded = true
      self
    end

  end

end
