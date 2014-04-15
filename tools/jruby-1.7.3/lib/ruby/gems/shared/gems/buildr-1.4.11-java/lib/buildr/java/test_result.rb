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

# It is necessary to require these files here as the bdd plugin directly includes this file
require 'yaml'
require 'rspec/core/formatters/base_formatter'

module Buildr #:nodoc:
  module TestFramework

    # A class used by buildr for jruby based frameworks, so that buildr can know
    # which tests succeeded/failed.
    class TestResult

      class Error < ::Exception
        attr_reader :message, :backtrace
        def initialize(message, backtrace)
          @message = message
          @backtrace = backtrace
          set_backtrace backtrace
        end

        def self.dump_yaml(file, e)
          FileUtils.mkdir_p File.dirname(file)
          File.open(file, 'w') { |f| f.puts(YAML.dump(Error.new(e.message, e.backtrace))) }
        end

        def self.guard(file)
          begin
            yield
          rescue => e
            dump_yaml(file, e)
          end
        end
      end

      attr_accessor :failed, :succeeded

      def initialize
        @failed, @succeeded = [], []
      end

      # An Rspec formatter used by buildr
      class YamlFormatter  < ::RSpec::Core::Formatters::BaseFormatter
        attr_reader :result

        def initialize(output)
          super(output)
          @result = Hash.new
          @result[:succeeded] = []
          @result[:failed] = []
        end

        def example_passed(example)
          super(example)
          result.succeeded << example_name(example)
        end

        def example_pending(example)
          super(example)
          result.succeeded << example_name(example)
        end

        def example_failed(example)
          super(example)
          result.failed << example_name(example)
        end

        def start(example_count)
          super(example_count)
          @result = TestResult.new
        end

        def close
          super
          result.succeeded = result.succeeded - result.failed
          output.puts YAML.dump(result)
        end

      private
        def example_name(example)
          example.file_path
        end
      end # YamlFormatter

    end # TestResult
  end
end
