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
  # Initialize variables for plugin.

  def initialize_flog
    self.flog_threshold ||= timebomb 1500, 1000 # 80% of average :(
  end

  ##
  # Define tasks for plugin.

  def define_flog_tasks
    begin
      require 'flog_task'
      FlogTask.new :flog, self.flog_threshold
    rescue Exception
      # skip
    end
  end
end
