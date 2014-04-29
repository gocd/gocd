require 'rspec/mocks'

module RSpec
  module Core
    # Because rspec-core dog-foods itself, rspec-core's spec suite has
    # examples that define example groups and examples and run them. The
    # usual lifetime of an RSpec::Mocks::Proxy is for one example
    # (the proxy cache gets cleared between each example), but since the
    # specs in rspec-core's suite sometimes create test doubles and pass
    # them to examples a spec defines and runs, the test double's proxy
    # must live beyond the inner example: it must live for the scope
    # of wherever it got defined. Here we implement the necessary semantics
    # for rspec-core's specs:
    #
    # - #verify_all and #reset_all affect only mocks that were created
    #   within the current scope.
    # - Mock proxies live for the duration of the scope in which they are
    #   created.
    #
    # Thus, mock proxies created in an inner example live for only that
    # example, but mock proxies created in an outer example can be used
    # in an inner example but will only be reset/verified when the outer
    # example completes.
    class SandboxedMockSpace < ::RSpec::Mocks::Space
      def self.sandboxed
        orig_space = RSpec::Mocks.space
        RSpec::Mocks.space = RSpec::Core::SandboxedMockSpace.new

        RSpec::Core::Example.class_eval do
          alias_method :orig_run, :run
          def run(*args)
            RSpec::Mocks.space.sandboxed do
              orig_run(*args)
            end
          end
        end

        yield
      ensure
        RSpec::Core::Example.class_eval do
          remove_method :run
          alias_method :run, :orig_run
          remove_method :orig_run
        end

        RSpec::Mocks.space = orig_space
      end

      class Sandbox
        attr_reader :proxies

        def initialize
          @proxies = Set.new
        end

        def verify_all
          @proxies.each { |p| p.verify }
        end

        def reset_all
          @proxies.each { |p| p.reset }
        end
      end

      def initialize
        @sandbox_stack = []
        super
      end

      def sandboxed
        @sandbox_stack << Sandbox.new
        yield
      ensure
        @sandbox_stack.pop
      end

      def verify_all
        return super unless sandbox = @sandbox_stack.last
        sandbox.verify_all
      end

      def reset_all
        return super unless sandbox = @sandbox_stack.last
        sandbox.reset_all
      end

      def proxy_for(object)
        new_proxy = !proxies.has_key?(object.__id__)
        proxy = super

        if new_proxy && sandbox = @sandbox_stack.last
          sandbox.proxies << proxy
        end

        proxy
      end
    end
  end
end

