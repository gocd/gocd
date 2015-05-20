module RSpec
  module Mocks
    module Matchers
      class Receive
        def initialize(message, block)
          @message                 = message
          @block                   = block
          @recorded_customizations = []

          # MRI, JRuby and RBX report the caller inconsistently; MRI
          # reports an extra "in `new'" line in the backtrace that the
          # others do not include. The safest way to find the right
          # line is to search for the first line BEFORE rspec/mocks/syntax.rb.
          @backtrace_line          = caller.find do |line|
            !line.split(':').first.end_with?('rspec/mocks/syntax.rb')
          end
        end

        def setup_expectation(subject, &block)
          setup_mock_proxy_method_substitute(subject, :add_message_expectation, block)
        end
        alias matches? setup_expectation

        def setup_negative_expectation(subject, &block)
          # ensure `never` goes first for cases like `never.and_return(5)`,
          # where `and_return` is meant to raise an error
          @recorded_customizations.unshift Customization.new(:never, [], nil)

          setup_expectation(subject, &block)
        end
        alias does_not_match? setup_negative_expectation

        def setup_allowance(subject, &block)
          setup_mock_proxy_method_substitute(subject, :add_stub, block)
        end

        def setup_any_instance_expectation(subject, &block)
          setup_any_instance_method_substitute(subject, :should_receive, block)
        end

        def setup_any_instance_negative_expectation(subject, &block)
          setup_any_instance_method_substitute(subject, :should_not_receive, block)
        end

        def setup_any_instance_allowance(subject, &block)
          setup_any_instance_method_substitute(subject, :stub, block)
        end

        MessageExpectation.public_instance_methods(false).each do |method|
          next if method_defined?(method)

          class_eval(<<-RUBY)
            def #{method}(*args, &block)
              @recorded_customizations << Customization.new(#{method.inspect}, args, block)
              self
            end
          RUBY
        end

      private

        def setup_mock_proxy_method_substitute(subject, method, block)
          proxy = ::RSpec::Mocks.proxy_for(subject)
          setup_method_substitute(proxy, method, block, @backtrace_line)
        end

        def setup_any_instance_method_substitute(subject, method, block)
          any_instance_recorder = ::RSpec::Mocks.any_instance_recorder_for(subject)
          setup_method_substitute(any_instance_recorder, method, block)
        end

        def setup_method_substitute(host, method, block, *args)
          args << @message.to_sym
          expectation = host.__send__(method, *args, &(@block || block))

          @recorded_customizations.each do |customization|
            customization.playback_onto(expectation)
          end
          expectation
        end

        class Customization
          def initialize(method_name, args, block)
            @method_name = method_name
            @args        = args
            @block       = block
          end

          def playback_onto(expectation)
            expectation.__send__(@method_name, *@args, &@block)
          end
        end
      end
    end
  end
end

