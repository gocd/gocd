# encoding: utf-8
#
# Licensed to the Software Freedom Conservancy (SFC) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The SFC licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

module Selenium
  module WebDriver
    module PhantomJS

      #
      # @api private
      #

      class Service
        START_TIMEOUT = 20
        STOP_TIMEOUT  = 5
        DEFAULT_PORT  = 8910
        MISSING_TEXT  = "Unable to find phantomjs executable."

        attr_reader :uri

        def self.executable_path
          @executable_path ||= (
            path = PhantomJS.path
            path or raise Error::WebDriverError, MISSING_TEXT
            Platform.assert_executable path

            path
          )
        end

        def self.default_service(port = nil)
          new executable_path, port || PortProber.above(DEFAULT_PORT)
        end

        def initialize(executable_path, port)
          @uri        = URI.parse "http://#{Platform.localhost}:#{port}"
          @executable = executable_path
        end

        def start(args = [])
          if @process && @process.alive?
            raise "already started: #{@uri.inspect} #{@executable.inspect}"
          end

          @process = create_process(args)
          @process.start

          socket_poller = SocketPoller.new Platform.localhost, @uri.port, START_TIMEOUT

          unless socket_poller.connected?
            raise Error::WebDriverError, "unable to connect to phantomjs @ #{@uri} after #{START_TIMEOUT} seconds"
          end

          Platform.exit_hook { stop } # make sure we don't leave the server running
        end

        def stop
          return if @process.nil? || @process.exited?

          Net::HTTP.start(uri.host, uri.port) do |http|
            http.open_timeout = STOP_TIMEOUT / 2
            http.read_timeout = STOP_TIMEOUT / 2

            http.get("/shutdown")
          end

          @process.poll_for_exit STOP_TIMEOUT
        rescue ChildProcess::TimeoutError
          # ok, force quit
          @process.stop STOP_TIMEOUT

          if Platform.jruby? && !$DEBUG
            @process.io.close rescue nil
          end
        end

        def create_process(args)
          server_command = [@executable, "--webdriver=#{@uri.port}", *args]
          process = ChildProcess.build(*server_command.compact)

          if $DEBUG == true
            process.io.inherit!
          elsif Platform.jruby?
            # apparently we need to read the output for phantomjs to work on jruby
            process.io.stdout = process.io.stderr = File.new(Platform.null_device, 'w')
          end

          process
        end

      end # Service
    end # PhantomJS
  end # WebDriver
end # Service