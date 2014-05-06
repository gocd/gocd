module Bundler
  module ParallelWorkers
    class ThreadWorker < Worker

      private

      # On platforms where fork is not available
      # use Threads for parallely downloading gems
      #
      # @param size [Integer] Size of thread worker pool
      # @param func [Proc] Job to be run inside thread worker pool
      def prepare_workers(size, func)
        @threads = size.times.map do |i|
          Thread.start do
            loop do
              obj = @request_queue.deq
              break if obj.equal? POISON
              begin
                @response_queue.enq func.call(obj, i)
              rescue Exception => e
                @response_queue.enq(WrappedException.new(e))
              end
            end
          end
        end
      end

    end
  end
end
