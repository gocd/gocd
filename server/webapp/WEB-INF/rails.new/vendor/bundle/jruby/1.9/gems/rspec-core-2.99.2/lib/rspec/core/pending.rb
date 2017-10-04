module RSpec
  module Core
    module Pending
      class SkipDeclaredInExample < StandardError
        attr_reader :argument

        def initialize(argument)
          super(argument.to_s)
          @argument = argument
        end
      end

      # If Test::Unit is loaed, we'll use its error as baseclass, so that Test::Unit
      # will report unmet RSpec expectations as failures rather than errors.
      begin
        class PendingExampleFixedError < Test::Unit::AssertionFailedError; end
      rescue
        class PendingExampleFixedError < StandardError; end
      end

      NO_REASON_GIVEN = 'No reason given'
      NOT_YET_IMPLEMENTED = 'Not yet implemented'

      # @overload pending()
      # @overload pending(message)
      # @overload pending(message, &block)
      #
      # Stops execution of an example, and reports it as pending. Takes an
      # optional message and block.
      #
      # @param [String] message optional message to add to the summary report.
      # @param [Block] block optional block. If it fails, the example is
      #   reported as pending. If it executes cleanly the example fails.
      #
      # @example
      #
      #     describe "an example" do
      #       # reported as "Pending: no reason given"
      #       it "is pending with no message" do
      #         pending
      #         this_does_not_get_executed
      #       end
      #
      #       # reported as "Pending: something else getting finished"
      #       it "is pending with a custom message" do
      #         pending("something else getting finished")
      #         this_does_not_get_executed
      #       end
      #
      #       # reported as "Pending: something else getting finished"
      #       it "is pending with a failing block" do
      #         pending("something else getting finished") do
      #           raise "this is the failure"
      #         end
      #       end
      #
      #       # reported as failure, saying we expected the block to fail but
      #       # it passed.
      #       it "is pending with a passing block" do
      #         pending("something else getting finished") do
      #           true.should be(true)
      #         end
      #       end
      #     end
      #
      # @note `before(:each)` hooks are eval'd when you use the `pending`
      #   method within an example. If you want to declare an example `pending`
      #   and bypass the `before` hooks as well, you can pass `:pending => true`
      #   to the `it` method:
      #
      #       it "does something", :pending => true do
      #         # ...
      #       end
      #
      #   or pass `:pending => "something else getting finished"` to add a
      #   message to the summary report:
      #
      #       it "does something", :pending => "something else getting finished" do
      #         # ...
      #       end
      def pending(*args, &block)
        RSpec.warn_deprecation(<<-EOS.gsub(/^\s+\|/, ''))
          |The semantics of `RSpec::Core::Pending#pending` are changing in
          |RSpec 3.  In RSpec 2.x, it caused the example to be skipped. In
          |RSpec 3, the rest of the example will still be run but is expected
          |to fail, and will be marked as a failure (rather than as pending)
          |if the example passes.
          |
          |Any passed block will no longer be executed. This feature is being
          |removed since it was semantically inconsistent, and the behaviour it
          |offered is being made available with the other ways of marking an
          |example pending.
          |
          |To keep the same skip semantics, change `pending` to `skip`.
          |Otherwise, if you want the new RSpec 3 behavior, you can safely
          |ignore this warning and continue to upgrade to RSpec 3 without
          |addressing it.
          |
          |Called from #{CallerFilter.first_non_rspec_line}.
          |
        EOS

        pending_no_warning(*args, &block)
      end

      def pending_no_warning(*args)
        return self.class.before(:each) { pending(*args) } unless RSpec.current_example

        options = args.last.is_a?(Hash) ? args.pop : {}
        message = args.first || NO_REASON_GIVEN

        if options[:unless] || (options.has_key?(:if) && !options[:if])
          return block_given? ? yield : nil
        end

        RSpec.current_example.metadata[:pending] = true
        RSpec.current_example.metadata[:execution_result][:pending_message] = message
        RSpec.current_example.execution_result[:pending_fixed] = false
        if block_given?
          begin
            result = begin
                       yield
                       RSpec.current_example.example_group_instance.instance_eval { verify_mocks_for_rspec }
                     end
            RSpec.current_example.metadata[:pending] = false
          rescue Exception => e
            RSpec.current_example.execution_result[:exception] = e
          ensure
            teardown_mocks_for_rspec
          end
          if result
            RSpec.current_example.execution_result[:pending_fixed] = true
            raise PendingExampleFixedError.new
          end
        end
        raise SkipDeclaredInExample.new(message)
      end

      # Backport from RSpec 3 to aid in upgrading.
      #
      # Not using alias method because we explictly want to discard any block.
      def skip(*args)
        pending_no_warning(*args)
      end

      def self.const_missing(name)
        return super unless name == :PendingDeclaredInExample

        RSpec.deprecate("RSpec::Core::PendingDeclaredInExample",
          :replacement => "RSpec::Core::Pending::SkipDeclaredInExample")

        SkipDeclaredInExample
      end
    end

    # Alias the error for compatibility with extension gems (e.g. formatters)
    # that depend on the const name of the error in RSpec <= 2.8.
    def self.const_missing(name)
      return super unless name == :PendingExampleFixedError

      RSpec.deprecate("RSpec::Core::PendingExampleFixedError",
        :replacement => "RSpec::Core::Pending::PendingExampleFixedError")

      Pending::PendingExampleFixedError
    end
  end
end
