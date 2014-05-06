module Bundler
  module ParallelWorkers
    class Worker
      POISON = Object.new

      class WrappedException < StandardError
        attr_reader :exception
        def initialize(exn)
          @exception = exn
        end
      end

      # Creates a worker pool of specified size
      #
      # @param size [Integer] Size of pool
      # @param func [Proc] job to run in inside the worker pool
      def initialize(size, func)
        @request_queue = Queue.new
        @response_queue = Queue.new
        prepare_workers size, func
        prepare_threads size
        trap("INT") { @threads.each {|i| i.exit }; stop_workers; exit 1 }
      end

      # Enqueue a request to be executed in the worker pool
      #
      # @param obj [String] mostly it is name of spec that should be downloaded
      def enq(obj)
        @request_queue.enq obj
      end

      # Retrieves results of job function being executed in worker pool
      def deq
        result = @response_queue.deq
        if result.is_a?(WrappedException)
          raise result.exception
        end
        result
      end

      # Stop the forked workers and started threads
      def stop
        stop_threads
        stop_workers
      end

      private
      # Stop the worker threads by sending a poison object down the request queue
      # so as worker threads after retrieving it, shut themselves down
      def stop_threads
        @threads.each do
          @request_queue.enq POISON
        end
        @threads.each do |thread|
          thread.join
        end
      end

      # To be overridden by child classes
      def prepare_threads(size)
      end

      # To be overridden by child classes
      def stop_workers
      end

    end
  end
end
