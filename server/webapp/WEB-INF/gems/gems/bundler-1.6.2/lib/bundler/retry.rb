module Bundler
  # General purpose class for retrying code that may fail
  class Retry
    DEFAULT_ATTEMPTS = 2
    attr_accessor :name, :total_runs, :current_run

    class << self
      attr_accessor :attempts
    end

    def initialize(name, exceptions = nil, attempts = nil)
      @name        = name
      attempts    ||= default_attempts
      @exceptions = Array(exceptions) || []
      @total_runs =  attempts.next # will run once, then upto attempts.times
    end

    def default_attempts
      return Integer(self.class.attempts) if self.class.attempts
      DEFAULT_ATTEMPTS
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
