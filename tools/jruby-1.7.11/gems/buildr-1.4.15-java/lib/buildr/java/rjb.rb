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

if RbConfig::CONFIG['host_os'] =~ /darwin/i
  # On OS X we attempt to guess where JAVA_HOME is, if not set
  # We set JAVA_HOME early so we can use it without calling Java.load first.
  ENV['JAVA_HOME'] ||= '/System/Library/Frameworks/JavaVM.framework/Home'
end

require 'rjb'


# Equivalent to Java system properties.  For example:
#   ENV_JAVA['java.version']
#   ENV_JAVA['java.class.version']
ENV_JAVA = {}


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

  module Package #:nodoc:

    def method_missing(sym, *args, &block)
      raise ArgumentError, 'No arguments expected' unless args.empty?
      name = "#{@name}.#{sym}"
      return ::Rjb.import(name) if sym.to_s =~ /^[[:upper:]]/
      ::Java.send :__package__, name
    end

  end

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
      @tools_jar ||= begin
        home = ENV['JAVA_HOME'] or fail 'Are we forgetting something? JAVA_HOME not set.'
        ['lib/tools.jar', '../lib/tools.jar'].map { |path| File.expand_path(path, home) }.
          find { |path| File.exist?(path) }
      end
    end

    # Loads the JVM and all the libraries listed on the classpath.  Call this
    # method before accessing any Java class, but only call it from methods
    # used in the build, giving the Buildfile a chance to load all extensions
    # that append to the classpath and specify which remote repositories to use.
    def load
      return self if @loaded
      classpath << tools_jar if tools_jar

      classpath.map! { |path| Proc === path ? path.call : path }
      cp = Buildr.artifacts(classpath).map(&:to_s).each { |path| file(path).invoke }
      java_opts = (ENV['JAVA_OPTS'] || ENV['JAVA_OPTIONS']).to_s.split

      # Prepend the JDK bin directory to the path under windows as RJB can have issues if it picks
      # up jvm dependencies from other products installed on the system
      if Buildr::Util.win_os?
        ENV["PATH"] = "#{ENV['JAVA_HOME']}#{File::SEPARATOR}bin#{File::PATH_SEPARATOR}#{ENV["PATH"]}"
      end
      ::Rjb.load cp.join(File::PATH_SEPARATOR), java_opts

      props = ::Rjb.import('java.lang.System').getProperties
      enum = props.propertyNames
      while enum.hasMoreElements
        name = enum.nextElement.toString
        ENV_JAVA[name] = props.getProperty(name)
      end
      @loaded = true
      self
    end

    def method_missing(sym, *args, &block) #:nodoc:
      raise ArgumentError, 'No arguments expected' unless args.empty?
      Java.load # need to load RJB's classpath now!
      name = sym.to_s
      return ::Rjb.import(name) if name =~ /^[[:upper:]]/
      __package__ name
    end

  private

    def __package__(name) #:nodoc:
      Module.new.tap do |m|
        m.extend Package
        m.instance_variable_set :@name, name
      end
    end

  end

end

class Array #:nodoc:
  # Converts a Ruby array into a typed Java array, argument specifies the element type.
  # This is necessary for JRuby and causes no harm on RJB.
  def to_java(cls)
    map { |item| cls.new(item) }
  end
end
