module RSpec
  module Core
    module Formatters
      # This class isn't loaded at runtime but serves to document all of the
      # notifications implemented as part of the standard interface. The
      # reporter will issue these during a normal test suite run, but a
      # formatter will only receive those notifications it has registered
      # itself to receive. To register a formatter call:
      #
      # `::RSpec::Core::Formatters.register class, :list, :of, :notifications`
      #
      # e.g.
      #
      # `::RSpec::Core::Formatters.register self, :start, :example_started`
      #
      # @see RSpec::Core::Formatters::BaseFormatter
      # @see RSpec::Core::Formatters::BaseTextFormatter
      # @see RSpec::Core::Reporter
      class Protocol
        # @method initialize
        # @api public
        #
        # @param output [IO] the formatter output

        # @method start
        # @api public
        # @group Suite Notifications
        #
        # This method is invoked before any examples are run, right after
        # they have all been collected. This can be useful for special
        # formatters that need to provide progress on feedback (graphical ones).
        #
        # This will only be invoked once, and the next one to be invoked
        # is {#example_group_started}.
        #
        # @param notification [StartNotification]

        # @method example_group_started
        # @api public
        # @group Group Notifications
        #
        # This method is invoked at the beginning of the execution of each
        # example group.
        #
        # The next method to be invoked after this is {#example_passed},
        # {#example_pending}, or {#example_group_finished}.
        #
        # @param notification [GroupNotification] containing example_group
        #   subclass of `RSpec::Core::ExampleGroup`

        # @method example_group_finished
        # @api public
        # @group Group Notifications
        #
        # Invoked at the end of the execution of each example group.
        #
        # @param notification [GroupNotification] containing example_group
        #   subclass of `RSpec::Core::ExampleGroup`

        # @method example_started
        # @api public
        # @group Example Notifications
        #
        # Invoked at the beginning of the execution of each example.
        #
        # @param notification [ExampleNotification] containing example subclass
        #   of `RSpec::Core::Example`

        # @method example_finished
        # @api public
        # @group Example Notifications
        #
        # Invoked at the end of the execution of each example.
        #
        # @param notification [ExampleNotification] containing example subclass
        #   of `RSpec::Core::Example`

        # @method example_passed
        # @api public
        # @group Example Notifications
        #
        # Invoked when an example passes.
        #
        # @param notification [ExampleNotification] containing example subclass
        #   of `RSpec::Core::Example`

        # @method example_pending
        # @api public
        # @group Example Notifications
        #
        # Invoked when an example is pending.
        #
        # @param notification [ExampleNotification] containing example subclass
        #   of `RSpec::Core::Example`

        # @method example_failed
        # @api public
        # @group Example Notifications
        #
        # Invoked when an example fails.
        #
        # @param notification [ExampleNotification] containing example subclass
        #   of `RSpec::Core::Example`

        # @method message
        # @api public
        # @group Suite Notifications
        #
        # Used by the reporter to send messages to the output stream.
        #
        # @param notification [MessageNotification] containing message

        # @method stop
        # @api public
        # @group Suite Notifications
        #
        # Invoked after all examples have executed, before dumping post-run
        # reports.
        #
        # @param notification [NullNotification]

        # @method start_dump
        # @api public
        # @group Suite Notifications
        #
        # This method is invoked after all of the examples have executed. The
        # next method to be invoked after this one is {#dump_failures}
        # (BaseTextFormatter then calls {#dump_failures} once for each failed
        # example).
        #
        # @param notification [NullNotification]

        # @method dump_failures
        # @api public
        # @group Suite Notifications
        #
        # Dumps detailed information about each example failure.
        #
        # @param notification [NullNotification]

        # @method dump_summary
        # @api public
        # @group Suite Notifications
        #
        # This method is invoked after the dumping of examples and failures.
        # Each parameter is assigned to a corresponding attribute.
        #
        # @param summary [SummaryNotification] containing duration,
        #   example_count, failure_count and pending_count

        # @method dump_profile
        # @api public
        # @group Suite Notifications
        #
        # This method is invoked after the dumping the summary if profiling is
        # enabled.
        #
        # @param profile [ProfileNotification] containing duration,
        #   slowest_examples and slowest_example_groups

        # @method dump_pending
        # @api public
        # @group Suite Notifications
        #
        # Outputs a report of pending examples. This gets invoked
        # after the summary if option is set to do so.
        #
        # @param notification [NullNotification]

        # @method close
        # @api public
        # @group Suite Notifications
        #
        # Invoked at the very end, `close` allows the formatter to clean
        # up resources, e.g. open streams, etc.
        #
        # @param notification [NullNotification]
      end
    end
  end
end
