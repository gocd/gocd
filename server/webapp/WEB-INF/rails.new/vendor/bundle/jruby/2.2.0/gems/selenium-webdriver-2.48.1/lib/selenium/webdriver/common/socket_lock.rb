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
    class SocketLock

      def initialize(port, timeout)
        @port    = port
        @timeout = timeout
      end

      #
      # Attempt to acquire a lock on the given port. Control is yielded to an
      # execution block if the lock could be successfully obtained.
      #

      def locked(&blk)
        lock

        begin
          yield
        ensure
          release
        end
      end

      private

      def lock
        max_time = Time.now + @timeout

        until can_lock? || Time.now >= max_time
          sleep 0.1
        end

        unless did_lock?
          raise Error::WebDriverError, "unable to bind to locking port #{@port} within #{@timeout} seconds"
        end
      end

      def release
        @server && @server.close
      end

      def can_lock?
        @server = TCPServer.new(Platform.localhost, @port)
        ChildProcess.close_on_exec @server

        true
      rescue SocketError, Errno::EADDRINUSE, Errno::EBADF => ex
        $stderr.puts "#{self}: #{ex.message}" if $DEBUG
        false
      end

      def did_lock?
        !!@server
      end

    end # SocketLock
  end # WebDriver
end # Selenium
