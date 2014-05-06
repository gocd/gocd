module Spec
  module Runner
    module Formatter
      # Formatter base-class, which implements all required methods as no-ops, with the exception
      class BaseFormatter
        # Formatters are initialized with <tt>options</tt> and <tt>output</tt>
        # arguments. RSpec's built-in formatters already expect this, and any
        # custom formatters should as well.
        #
        # ==== Parameters
        # options::
        #   A struct containing boolean values for colour, autospec,
        #   and dry_run
        # output::
        #   Used by RSpec's built-in formatters to determine where to
        #   write the output. Default is <tt>STDOUT</tt>, otherwise a
        #   filename is expected.
        #
        # === Example
        # If you invoke the <tt>spec</tt> command with:
        #
        #   --format progress:progress_report.txt
        #
        # ... the value of <tt>output</tt> will be progress_report.txt. If you
        # don't identify an output destination, the default is STDOUT.
        def initialize(options, output)
        end
        
        # This method is invoked before any examples are run, right after
        # they have all been collected. This can be useful for special
        # formatters that need to provide progress on feedback (graphical ones)
        #
        # This method will only be invoked once, and the next one to be invoked
        # is #example_group_started
        #
        # ==== Parameters
        # example_count:: the total number of examples to be run
        def start(example_count)
        end

        # This method is invoked at the beginning of the execution of each
        # example_group. The next method to be invoked after this is
        # #example_started
        #
        # ==== Parameters
        # example_group_proxy:: instance of Spec::Example::ExampleGroupProxy
        def example_group_started(example_group_proxy)
        end
        
        # Deprecated - use example_group_started instead
        def add_example_group(example_group_proxy)
          Spec.deprecate("BaseFormatter#add_example_group", "BaseFormatter#example_group_started")
          example_group_started(example_group_proxy)
        end

        # This method is invoked when an +example+ starts. The next method to be
        # invoked after this is #example_passed, #example_failed, or
        # #example_pending
        #
        # ==== Parameters
        # example_proxy:: instance of Spec::Example::ExampleProxy
        def example_started(example_proxy)
        end

        # This method is invoked when an +example+ passes.
        # +example_proxy+ is the same instance of Spec::Example::ExampleProxy
        # that was passed to example_started
        #
        # ==== Parameters
        # example_proxy:: instance of Spec::Example::ExampleProxy
        def example_passed(example_proxy)
        end

        # This method is invoked when an +example+ fails, i.e. an exception occurred
        # inside it (such as a failed should or other exception).
        #
        # ==== Parameters
        # example_proxy::
        #   The same instance of Spec::Example::ExampleProxy that was passed
        #   to <tt>example_started</tt>
        #
        # counter:: the sequential number of this failure
        #
        # failure:: instance of Spec::Runner::Reporter::Failure
        def example_failed(example_proxy, counter, failure)
        end
        
        # This method is invoked when an example is not yet implemented (i.e. has not
        # been provided a block), or when an ExamplePendingError is raised.
        # +message+ is the message from the ExamplePendingError, if it exists, or the
        # default value of "Not Yet Implemented". +deprecated_pending_location+ is
        # deprecated - use example_proxy.location instead
        #
        # ==== Parameters
        # example_proxy:: instance of Spec::Example::ExampleProxy
        # message::
        #   the message passed to the pending message, or an internal
        #   default
        #
        def example_pending(example_proxy, message, deprecated_pending_location=nil)
        end

        # This method is invoked after all of the examples have executed. The next method
        # to be invoked after this one is #dump_failure (once for each failed example)
        def start_dump
        end

        # Dumps detailed information about an example failure.
        # This method is invoked for each failed example after all examples have run. +counter+ is the sequence number
        # of the associated example. +failure+ is a Failure object, which contains detailed
        # information about the failure.
        #
        # ==== Parameters
        # counter:: the sequential number of this failure
        # failure:: instance of Spec::Runner::Reporter::Failure
        def dump_failure(counter, failure)
        end
      
        # This method is invoked after the dumping of examples and failures.
        #
        # ==== Parameters
        # duration:: the total time for the entire run
        # example_count:: the number of examples run
        # failure_count:: the number of examples that failed
        # pending_count:: the number of examples that are pending
        def dump_summary(duration, example_count, failure_count, pending_count)
        end
        
        # This gets invoked after the summary
        def dump_pending
        end

        # This method is invoked at the very end. Allows the formatter to clean up, like closing open streams.
        def close
        end
      end
    end
  end
end
