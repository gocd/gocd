module RSpec::Core
  class Reporter
    NOTIFICATIONS = %W[start message example_group_started example_group_finished example_started
                       example_passed example_failed example_pending start_dump dump_pending
                       dump_failures dump_summary seed close stop deprecation deprecation_summary].map { |n| n.to_sym }

    def initialize(*formatters)
      @listeners = Hash.new { |h,k| h[k] = [] }
      formatters.each do |formatter|
        register_listener(formatter, *NOTIFICATIONS)
      end
      @example_count = @failure_count = @pending_count = 0
      @duration = @start = nil
    end

    # @api
    # @param [Object] An obect that wishes to be notified of reporter events
    # @param [Array] Array of symbols represents the events a listener wishes to subscribe too
    #
    # Registers a listener to a list of notifications. The reporter will send notification of
    # events to all registered listeners
    def register_listener(listener, *notifications)
      notifications.each do |notification|
        @listeners[notification.to_sym] << listener if listener.respond_to?(notification)
      end
      true
    end

    def registered_listeners(notification)
      @listeners[notification]
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
      @start = RSpec::Core::Time.now
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

    def deprecation(message)
      notify :deprecation, message
    end

    def finish(seed)
      begin
        stop
        notify :start_dump
        notify :dump_pending
        notify :dump_failures
        notify :dump_summary, @duration, @example_count, @failure_count, @pending_count
        notify :deprecation_summary
        notify :seed, seed if seed
      ensure
        notify :close
      end
    end

    alias_method :abort, :finish

    def stop
      @duration = (RSpec::Core::Time.now - @start).to_f if @start
      notify :stop
    end

    def notify(event, *args, &block)
      registered_listeners(event).each do |formatter|
        formatter.send(event, *args, &block)
      end
    end

  end
end
