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


require 'uri'
require 'net/http'
require 'buildr/core/project'
require 'buildr/java'
require 'buildr/packaging'
require 'thread'


module Buildr

  # Provides a collection of tasks and methods for using Jetty, specifically as a server
  # for testing your application.
  #
  # Build files should always start Jetty by invoking the #use task, typically as
  # a prerequisite. This task will start Jetty once during the build, and shut it down
  # when the build completes.
  #
  # If you want to keep Jetty running across builds, and look at error messages, you can
  # start Jetty in a separate console with:
  #   buildr jetty:start
  # To stop this instance of Jetty, simply kill the process (Ctrl-C) or run:
  #   buildr jetty:stop
  #
  # If you start Jetty separately from the build, the #use task will connect to that
  # existing server. Since you are using Jetty across several builds, you will want to
  # cleanup any mess created by each build. You can use the #setup and #teardown tasks,
  # which are called when Jetty is first used in the build, and when the build ends.
  class Jetty

    # Which version of Jetty we're using by default (change with options.jetty.version).
    VERSION = "6.1.3"
    SLF4J_VERSION = "1.4.3"
    
    # Libraries used by Jetty.
    REQUIRES = [ "org.mortbay.jetty:jetty:jar:#{VERSION}", "org.mortbay.jetty:jetty-util:jar:#{VERSION}",
      "org.mortbay.jetty:servlet-api-2.5:jar:#{VERSION}", "org.slf4j:slf4j-api:jar:#{SLF4J_VERSION}", 
      "org.slf4j:slf4j-simple:jar:#{SLF4J_VERSION}", "org.slf4j:jcl104-over-slf4j:jar:#{SLF4J_VERSION}" ]
     
    Java.classpath <<  REQUIRES
    Java.classpath << File.dirname(__FILE__)
    
    # Default URL for Jetty (change with options.jetty.url).
    URL = "http://localhost:8080"

    class << self

      # :call-seq:
      #   instance() => Jetty
      #
      # Returns an instance of Jetty.
      def instance()
        @instance ||= Jetty.new("jetty", URL)
      end

    end

    def initialize(name, url) #:nodoc:
      @url = url
      namespace name do
        @setup = task("setup")
        @teardown = task("teardown")
        @use = task("use") { fire }
      end
    end

    # The URL for the Jetty server. Leave as is if you want to use the default server
    # (http://localhost:8080).
    attr_accessor :url

    # :call-seq:
    #    start(pipe?)
    #
    # Starts Jetty. This method does not return, it keeps the thread running until
    # Jetty is stopped. If you want to run Jetty parallel with other tasks in the build,
    # invoke the #use task instead.
    def start(sync = nil)
      begin
        puts "classpath #{Java.classpath.inspect}"
        port = URI.parse(url).port
        puts "Starting Jetty at http://localhost:#{port}" if verbose
        Java.load
        jetty = Java.org.apache.buildr.JettyWrapper.new(port)
        sync << "Started" if sync
        sleep # Forever
      rescue Interrupt # Stopped from console
      rescue Exception=>error
        puts "#{error.class}: #{error.message}"
      end
      exit! # No at_exit
    end

    # :call-seq:
    #    stop()
    #
    # Stops Jetty. Stops a server running in a separate process.
    def stop()
      uri = URI.parse(url)
      begin
        Net::HTTP.start(uri.host, uri.port) do |http|
          http.request_post "/buildr/stop", ""
        end
      rescue Errno::ECONNREFUSED
        # Expected if Jetty server not running.
      rescue EOFError
        # We get EOFError because Jetty is brutally killed.
      end
      puts "Jetty server stopped"
    end

    # :call-seq:
    #   running?() => boolean
    #
    # Returns true if it finds a running Jetty server that supports the Buildr
    # requests for deploying, stopping, etc.
    def running?()
      uri = URI.parse(url)
      begin
        Net::HTTP.start(uri.host, uri.port) do |http|
          response = http.request_get("/buildr/")
          response.is_a?(Net::HTTPSuccess) && response.body =~ /Alive/
        end
      rescue Errno::ECONNREFUSED, Errno::EBADF 
        false
      end
    end

    # :call-seq:
    #   deploy(url, webapp) => path
    #
    # Deploy a WAR in the specified URL.
    def deploy(url, webapp)
      use.invoke
      uri = URI.parse(url)
      Net::HTTP.start(uri.host, uri.port) do |http|
        response = http.request_post("/buildr/deploy", "webapp=#{webapp}&path=#{uri.path}")
        if Net::HTTPOK === response && response.body =~ /Deployed/
          path = response.body.split[1]
          puts "Deployed #{webapp}, context path #{uri.path}" if Rake.application.options.trace
          path
        else
          fail "Deployment failed: #{response}"
        end
      end
    end

    # :call-seq:
    #   undeploy(url) => boolean
    #
    # Undeploys a WAR from the specified URL.
    def undeploy(url)
      use.invoke
      uri = URI.parse(url)
      Net::HTTP.start(uri.host, uri.port) do |http|
        response = http.request_post("/buildr/undeploy", "path=#{uri.path}")
        if Net::HTTPOK === response && response.body =~ /Undeployed/
          true
        else
          fail "Deployment failed: #{response}"
        end
      end
    end

    # :call-seq:
    #   setup(*prereqs) => task
    #   setup(*prereqs) { |task| .. } => task
    #
    # This task executes when Jetty is first used in the build. You can use it to
    # deploy artifacts into Jetty.
    def setup(*prereqs, &block)
      @setup.enhance prereqs, &block
    end

    # :call-seq:
    #   teardown(*prereqs) => task
    #   teardown(*prereqs) { |task| .. } => task
    #
    # This task executes when the build is done. You can use it to undeploy artifacts
    # previously deployed into Jetty.
    def teardown(*prereqs, &block)
      @teardown.enhance prereqs, &block
    end

    # :call-seq:
    #   use(*prereqs) => task
    #   use(*prereqs) { |task| .. } => task
    #
    # If you intend to use Jetty, invoke this task. It will start a new instance of
    # Jetty and close it when the build is done. However, if you already have a server
    # running in the background (e.g. jetty:start), it will use that server and will
    # not close it down.
    def use(*prereqs, &block)
      @use.enhance prereqs, &block
    end

  protected

    # If you want to start Jetty inside the build, call this method instead of #start.
    # It will spawn a separate process that will run Jetty, and will stop Jetty when
    # the build ends. However, if you already started Jetty from the console (with
    # take jetty:start), it will use the existing instance without shutting it down.
    def fire()
      unless running?
        sync = Queue.new
        Thread.new { start sync }
        # Wait for Jetty to fire up before doing anything else.
        sync.pop == "Started" or fail "Jetty not started"
        puts "Jetty started" if verbose
        at_exit { stop }
      end
      @setup.invoke
      at_exit { @teardown.invoke }
    end

  end

  namespace "jetty" do
    desc "Start an instance of Jetty running in the background"
    task("start") { Jetty.instance.start }
    desc "Stop an instance of Jetty running in the background"
    task("stop") { Jetty.instance.stop }
  end

  # :call-seq:
  #   jetty() => Jetty
  #
  # Returns a Jetty object. You can use this to discover the Jetty#use task,
  # configure the Jetty#setup and Jetty#teardown tasks, deploy and undeploy to Jetty.
  def jetty()
    @jetty ||= Jetty.instance
  end

end
