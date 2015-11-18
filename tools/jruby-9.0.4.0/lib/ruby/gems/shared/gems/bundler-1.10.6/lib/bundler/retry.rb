module Bundler
  # General purpose class for retrying code that may fail
  class Retry
    attr_accessor :name, :total_runs, :current_run

    class << self
      def default_attempts
        default_retries + 1
      end
      alias_method :attempts, :default_attempts

      def default_retries
        Bundler.settings[:retry]
      end
    end

    def initialize(name, exceptions = nil, retries = self.class.default_retries)
      @name        = name
      @retries   = retries
      @exceptions = Array(exceptions) || []
      @total_runs =  @retries + 1 # will run once, then upto attempts.times
    end

    def attempt(&block)
      @current_run = 0
      @failed      = false
      @error       = nil
      while keep_trying? do
        run(&block)
      end
      @result
    end
    alias :attempts :attempt

  private
    def run(&block)
      @failed      = false
      @current_run += 1
      @result = block.call
    rescue => e
      fail(e)
    end

    def fail(e)
      @failed = true
      raise e if last_attempt? || @exceptions.any?{ |k| e.is_a?(k) }
      return true unless name
      Bundler.ui.warn "Retrying#{" #{name}" if name} due to error (#{current_run.next}/#{total_runs}): #{e.class} #{e.message}"
    end

    def keep_trying?
      return true  if current_run.zero?
      return false if last_attempt?
      return true  if @failed
    end

    def last_attempt?
      current_run >= total_runs
    end
  end
end
