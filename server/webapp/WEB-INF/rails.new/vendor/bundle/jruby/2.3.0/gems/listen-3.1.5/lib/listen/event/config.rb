module Listen
  module Event
    class Config
      def initialize(
        listener,
        event_queue,
        queue_optimizer,
        wait_for_delay,
        &block)

        @listener = listener
        @event_queue = event_queue
        @queue_optimizer = queue_optimizer
        @min_delay_between_events = wait_for_delay
        @block = block
      end

      def sleep(*args)
        Kernel.sleep(*args)
      end

      def call(*args)
        @block.call(*args) if @block
      end

      def timestamp
        Time.now.to_f
      end

      attr_reader :event_queue

      def callable?
        @block
      end

      def optimize_changes(changes)
        @queue_optimizer.smoosh_changes(changes)
      end

      attr_reader :min_delay_between_events

      def stopped?
        listener.state == :stopped
      end

      def paused?
        listener.state == :paused
      end

      private

      attr_reader :listener
    end
  end
end
