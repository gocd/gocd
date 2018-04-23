require "minitest/mock"

module ActiveSupport
  module Testing
    module MethodCallAssertions # :nodoc:
      private
        def assert_called(object, method_name, message = nil, times: 1, returns: nil)
          times_called = 0

          object.stub(method_name, proc { times_called += 1; returns }) { yield }

          error = "Expected #{method_name} to be called #{times} times, " \
            "but was called #{times_called} times"
          error = "#{message}.\n#{error}" if message
          assert_equal times, times_called, error
        end

        def assert_called_with(object, method_name, args = [], returns: nil)
          mock = Minitest::Mock.new

          if args.all? { |arg| arg.is_a?(Array) }
            args.each { |arg| mock.expect(:call, returns, arg) }
          else
            mock.expect(:call, returns, args)
          end

          object.stub(method_name, mock) { yield }

          mock.verify
        end

        def assert_not_called(object, method_name, message = nil, &block)
          assert_called(object, method_name, message, times: 0, &block)
        end

        def stub_any_instance(klass, instance: klass.new)
          klass.stub(:new, instance) { yield instance }
        end
    end
  end
end
