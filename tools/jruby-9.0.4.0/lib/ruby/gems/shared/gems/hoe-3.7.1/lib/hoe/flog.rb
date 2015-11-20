##
# Flog plugin for hoe.
#
# === Tasks Provided:
#
# flog::               Analyze code complexity.

module Hoe::Flog
  ##
  # Optional: flog threshold to determine threshold failure. [default: 1500-200]

  attr_accessor :flog_threshold

  ##
  # Optional: flog method to run to determine threshold. [default: :max_method]

  attr_accessor :flog_method

  ##
  # Initialize variables for plugin.

  def initialize_flog
    self.flog_method    ||= :max_method
    self.flog_threshold ||= 20 # 2x industry avg
  end

  ##
  # Define tasks for plugin.

  def define_flog_tasks
    begin
      require 'flog_task'
      FlogTask.new :flog, self.flog_threshold, nil, self.flog_method
    rescue LoadError
      # skip
    end
  end
end
