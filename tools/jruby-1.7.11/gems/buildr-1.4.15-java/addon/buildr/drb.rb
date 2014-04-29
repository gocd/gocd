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

require 'delegate'
require 'drb/drb'

module Buildr

  # This addon allows you start a DRb server hosting a buildfile, so that
  # you can later invoke tasks on it without having to load
  # the complete buildr runtime again.
  #
  # Usage:
  #
  #   buildr -r buildr/drb drb:start
  #
  # Once the server has been started you can invoke tasks using a simple script:
  #
  #   #!/usr/bin/env ruby
  #   require 'rubygems'
  #   require 'buildr/drb'
  #   Buildr::DRbApplication.run
  #
  # Save this script as 'dbuildr', make it executable and use it to invoke tasks.
  #
  #   dbuildr clean compile
  #
  # The dbuildr script will run as the server if there isn't one already running.
  # Subsequent calls to dbuildr will act as the client and invoke the tasks you
  # provide in the server.
  # If the buildfile has been modified it will be reloaded on the BuildrServer.
  #
  # JRuby users can use a nailgun client to invoke tasks as fast as possible
  # without having to incur JVM startup time.
  # See the documentation for buildr/nailgun.
  module DRbApplication

    port = ENV['DRB_PORT'] || 2111
    PORT = port.to_i

    class SavedTask #:nodoc:

      def initialize(original)
        @original = original.clone
        @prerequisites = original.prerequisites.clone if original.respond_to?(:prerequisites)
        @actions = original.actions.clone if original.respond_to?(:actions)
      end

      def name
        @original.name
      end

      def actions
        @actions ||= []
      end

      def prerequisites
        @prerequisites ||= []
      end

      def define!
        @original.class.send(:define_task, @original.name => prerequisites).tap do |task|
          task.comment = @original.comment
          actions.each { |action| task.enhance &action }
        end
      end
    end # SavedTask

    class Snapshot #:nodoc:

      attr_accessor :projects, :tasks, :rules, :layout, :options

      # save the tasks,rules,layout defined by buildr
      def initialize
        @rules = Buildr.application.instance_eval { @rules || [] }.clone
        @options = Buildr.application.options.clone
        @options.rakelib ||= ['tasks']
        @layout = Layout.default.clone
        @projects = Project.instance_eval { @projects || {} }.clone
        @tasks = Buildr.application.tasks.inject({}) do |hash, original|
          unless projects.key? original.name # don't save project definitions
            hash.update original.name => SavedTask.new(original)
          end
          hash
        end
      end

    end # Snapshot

    class << self

      attr_accessor :original, :snapshot

      def run
        begin
          client = connect
        rescue DRb::DRbConnError => e
          run_server!
        else
          run_client(client)
        end
      end

      def client_uri
        "druby://:#{PORT + 1}"
      end

      def remote_run(cfg)
        with_config(cfg) { Buildr.application.remote_run(self) }
      rescue => e
        cfg[:err].puts e.message
        e.backtrace.each { |b| cfg[:err].puts "\tfrom #{b}" }
        raise e
      end

      def save_snapshot(app)
        if app.instance_eval { @rakefile }
          @snapshot = self::Snapshot.new
          app.buildfile_reloaded!
        end
      end

    private

      def server_uri
        "druby://:#{PORT}"
      end

      def connect
        buildr = DRbObject.new(nil, server_uri)
        uri = buildr.client_uri # obtain our uri from the server
        DRb.start_service(uri)
        buildr
      end

      def run_client(client)
        client.remote_run :dir => Dir.pwd, :argv => ARGV,
                          :in  => $stdin, :out => $stdout, :err => $stderr
      end

      def setup
        unless original
          # Create the stdio delegator that can be cached (eg by fileutils)
          delegate_stdio

          # Lazily load buildr the first time it's needed
          require 'buildr'

          # Save the tasks,rules,layout defined by buildr
          # before loading any project
          @original = self::Snapshot.new

          Buildr.application.extend self
          save_snapshot(Buildr.application)
        end
      end

      def run_server
        setup
        DRb.start_service(server_uri, self)
        puts "#{self} waiting on #{server_uri}"
      end

      def run_server!
        setup
        if RUBY_PLATFORM[/java/]
          require 'buildr/nailgun'
          Buildr.application['nailgun:drb'].invoke
        else
          run_server
          DRb.thread.join
        end
      end

      def delegate_stdio
        $stdin  = SimpleDelegator.new($stdin)
        $stdout = SimpleDelegator.new($stdout)
        $stderr = SimpleDelegator.new($stderr)
      end

      def with_config(remote)
        @invoked = true
        set = lambda do |env|
          ARGV.replace env[:argv]
          $stdin.__setobj__(env[:in])
          $stdout.__setobj__(env[:out])
          $stderr.__setobj__(env[:err])
          Buildr.application.instance_variable_set :@original_dir, env[:dir]
        end
        original = {
          :dir => Buildr.application.instance_variable_get(:@original_dir),
          :argv => ARGV,
          :in => $stdin.__getobj__,
          :out => $stdout.__getobj__,
          :err => $stderr.__getobj__
        }
        begin
          set[remote]
          yield
        ensure
          set[original]
        end
      end

    end # class << DRbApplication

    def remote_run(server)
      @options = server.original.options.clone
      init 'Distributed Buildr'
      if @rakefile
        if buildfile_needs_reload?
          reload_buildfile(server, server.original)
        else
          clear_invoked_tasks(server.snapshot || server.original)
        end
      else
        reload_buildfile(server, server.original)
      end
      top_level
    end

    def buildfile_reloaded!
      @last_loaded = buildfile.timestamp if @rakefile
    end

  private

    def buildfile_needs_reload?
      !@last_loaded || @last_loaded < buildfile.timestamp
    end

    def reload_buildfile(server, snapshot)
      clear_for_reload(snapshot)
      load_buildfile
      server.save_snapshot(self)
    end

    def clear_for_reload(snapshot)
      Project.clear
      @tasks = {}
      @rules = snapshot.rules.clone
      snapshot.tasks.each_pair { |name, saved| saved.define! }
      Layout.default = snapshot.layout.clone
    end

    def clear_invoked_tasks(snapshot)
      @rules = snapshot.rules.clone
      (@tasks.keys - snapshot.projects.keys).each do |name|
        if saved = snapshot.tasks[name]
          # reenable this task, restoring its actions/prereqs
          task = @tasks[name]
          task.reenable
          task.prerequisites.replace saved.prerequisites.clone
          task.actions.replace saved.actions.clone
        else
          # tasks generated at runtime, drop it
          @tasks.delete(name)
        end
      end
    end

    task('drb:start') { run_server! } if Buildr.respond_to?(:application)

  end # DRbApplication

end

