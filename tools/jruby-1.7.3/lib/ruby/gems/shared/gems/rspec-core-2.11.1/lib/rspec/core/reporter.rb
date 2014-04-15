module RSpec::Core
  class Reporter
    def initialize(*formatters)
      @formatters = formatters
      @example_count = @failure_count = @pending_count = 0
      @duration = @start = nil
    end

    # @api
    # @overload report(count, &block)
    # @overload report(count, seed, &block)
    # @param [Integer] count the number of examples being run
    # @param [Integer] seed the seed used to randomize the spec run
    # @param [Block] block yields itself for further reporting.
    #
    # Initializes the report run and yields itself for further reporting. The
    # block is required, so that the reporter can manage cleaning up after the
    # run.
    #
    # ### Warning:
    #
    # The `seed` argument is an internal API and is not guaranteed to be
    # supported in the future.
    #
    # @example
    #
    #     reporter.report(group.examples.size) do |r|
    #       example_groups.map {|g| g.run(r) }
    #     end
    #
    def report(expected_example_count, seed=nil)
      start(expected_example_count)
      begin
        yield self
      ensure
        finish(seed)
      end
    end

    def start(expected_example_count)
      @start = Time.now
      notify :start, expected_example_count
    end

    def message(message)
      notify :message, message
    end

    def example_group_started(group)
      notify :example_group_started, group unless group.descendant_filtered_examples.empty?
    end

    def example_group_finished(group)
      notify :example_group_finished, group unless group.descendant_filtered_examples.empty?
    end

    def example_started(example)
      @example_count += 1
      notify :example_started, example
    end

    def example_passed(example)
      notify :example_passed, example
    end

    def example_failed(example)
      @failure_count += 1
      notify :example_failed, example
    end

    def example_pending(example)
      @pending_count += 1
      notify :example_pending, example
    end

    def finish(seed)
      begin
        stop
        notify :start_dump
        notify :dump_pending
        notify :dump_failures
        notify :dump_summary, @duration, @example_count, @failure_count, @pending_count
        notify :seed, seed if seed
      ensure
        notify :close
      end
    end

    alias_method :abort, :finish

    def stop
      @duration = Time.now - @start if @start
      notify :stop
    end

    def notify(method, *args, &block)
      @formatters.each do |formatter|
        formatter.send method, *args, &block
      end
    end
  end
end
