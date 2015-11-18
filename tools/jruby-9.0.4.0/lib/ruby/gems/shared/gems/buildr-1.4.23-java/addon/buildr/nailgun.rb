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


require 'jruby'
require 'rbconfig'
require 'tmpdir'
require 'buildr/drb'


module Buildr

  # This addon is provided for fast interaction with a DRb BuildrServer (buildr/drb).
  #
  # This module delegates task invocation to the BuildrServer, it only implements
  # nailgun required logic (server/client).
  #
  # Usage:
  #
  #   buildr -r buildr/nailgun nailgun:start
  #
  # Once the server has been started you can invoke tasks using the nailgun client
  # installed on $JRUBY_HOME/tool/nailgun. It's recommended to add this path to
  # your PATH environment variable, so that the ng command is available at any dir.
  #
  #   ng build # invoke the build task
  #
  module Nailgun
    extend self

    VERSION = '0.7.1'
    NAME = "nailgun-#{VERSION}"
    URL = "http://downloads.sourceforge.net/nailgun/#{NAME}.zip"
    ARTIFACT_SPEC = "com.martiansoftware:nailgun:jar:#{VERSION}"
    PORT = DRbApplication::PORT + 2
    ADDON_BIN = File.dirname(__FILE__)

    # Returns the path to JRUBY_HOME.
    def jruby_home
      ENV['JRUBY_HOME'] || RbConfig::CONFIG['prefix']
    end

    # Returns the path to NAILGUN_HOME.
    def nailgun_home
      ENV['NAILGUN_HOME'] || File.expand_path('tool/nailgun', jruby_home)
    end

    def tmp_path(*paths)
      File.join(Dir.tmpdir, 'nailgun', *paths)
    end

    module Util
      extend self

      def add_to_sysloader(path)
        sysloader = java.lang.ClassLoader.getSystemClassLoader
        add_url_method = java.lang.Class.forName('java.net.URLClassLoader').
          getDeclaredMethod('addURL', [java.net.URL.java_class].to_java(java.lang.Class))
        add_url_method.setAccessible(true)
        add_url_method.invoke(sysloader, [java.io.File.new(path).toURI.toURL].to_java(java.net.URL))
      end

      # invoke a java constructor
      def ctor(on_class, *args)
        parameters = []
        classes = []
        args.each do |obj|
          case obj
          when nil
            classes.push(nil)
            parameters.push(nil)
          when Hash
            vclass = obj.keys.first
            value = obj[vclass]
            classes.push(vclass.java_class)
            parameters.push(value)
          else
            parameters.push obj
            classes.push obj.class.java_class
          end
        end
        on_class = [on_class.java_class].to_java(java.lang.Class)[0]
        ctor = on_class.getDeclaredConstructor(classes.to_java(java.lang.Class))
        ctor.setAccessible(true)
        ctor.newInstance(parameters.to_java(java.lang.Object))
      end

    end # Util

    module Client

      def main(nail)
        nail.out.println "Connected to #{nail.getNGServer}"

        runtime = JRuby.runtime

        stdout = Util.ctor(org.jruby.RubyIO, runtime, java.io.OutputStream => nail.out)
        stderr = Util.ctor(org.jruby.RubyIO, runtime, java.io.OutputStream => nail.err)
        stdin = Util.ctor(org.jruby.RubyIO, runtime, java.io.InputStream => nail.in)

        dir = nail.getWorkingDirectory
        argv = [nail.command] + nail.args

        DRbApplication.remote_run :dir => dir, :argv => argv,
                                  :in => stdin, :out => stdout, :err => stderr
      rescue => e
        nail.err.println e unless SystemExit === e
        nail.exit 1
      end

    end # Client

    module Server
      def initialize(host, port)
        @host = host || "*"
        @port = port
        super(host, port)
      end

      def start
        self.allow_nails_by_class_name = false

        NGClient::Main.nail = NGClient.new
        self.default_nail_class = NGClient::Main

        @thread = java.lang.Thread.new(self)
        @thread.setName(to_s)
        @thread.start

        sleep 1 while getPort == 0
        info "#{self} Started."
      end

      def stop
        @thread.kill
      end

      def to_s
        version = "Buildr #{Buildr::VERSION} #{RUBY_PLATFORM[/java/] && '(JRuby '+ (Buildr.settings.build['jruby'] || JRUBY_VERSION) +')'}"
        self.class.name+'('+[version, @host, @port].join(', ')+')'
      end
    end # Server

    namespace(:nailgun) do

      dist_zip = Buildr.download(tmp_path(NAME + '.zip') => URL)
      dist_dir = Buildr.unzip(tmp_path(NAME) => dist_zip)

      nailgun_jar = file(tmp_path(NAME, NAME, NAME + '.jar'))
      nailgun_jar.enhance [dist_dir] unless File.exist?(nailgun_jar.to_s)

      attr_reader :artifact
      @artifact = Buildr.artifact(ARTIFACT_SPEC).from(nailgun_jar)

      compiled_bin = file(tmp_path(NAME, NAME, 'ng' + RbConfig::CONFIG['EXEEXT']) => dist_dir.target) do |task|
        unless task.to_s.pathmap('%x') == '.exe'
          Dir.chdir(task.to_s.pathmap('%d')) do
            info "Compiling #{task.to_s}"
            system('make', task.to_s.pathmap('%f')) or
              fail "Nailgun binary compilation failed."
          end
        end
      end

      attr_reader :installed_bin
      @installed_bin = file(File.expand_path(compiled_bin.to_s.pathmap('%f'), nailgun_home) => compiled_bin) do |task|
        mkpath task.to_s.pathmap('%d'), :verbose => false
        cp compiled_bin.to_s, task.to_s, :verbose => false
      end

      task('drb-notice') do
        info ''
        info 'Running in JRuby, a nailgun server will be started so that'
        info 'you can use your nailgun client to invoke buildr tasks: '
        info ''
        info '  '+Nailgun.installed_bin.to_s
        info ''
      end

      task('drb' => ['drb-notice', 'start'])

      desc 'Start the nailgun server'
      task('start' => [installed_bin, 'setup']) do |task|
        server = NGServer.new(nil, PORT)
        server.start
      end

      task('setup' => artifact) do
        module Util
          include Buildr::Util
        end

        Util.add_to_sysloader artifact.to_s
        Util.add_to_sysloader ADDON_BIN

        class NGClient
          include org.apache.buildr.BuildrNail
          include Client
        end

        class NGServer < com.martiansoftware.nailgun.NGServer
          include Server
        end
      end

    end # ng_tasks

  end # module Nailgun
end
