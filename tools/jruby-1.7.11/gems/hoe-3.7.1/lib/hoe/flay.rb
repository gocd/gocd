##
# Flay plugin for hoe.
#
# === Tasks Provided:
#
# flay::               Analyze for code duplication.

module Hoe::Flay
  ##
  # Optional: flay threshold to determine threshold failure. [default: 1200-100]

  attr_accessor :flay_threshold

  ##
  # Initialize variables for plugin.

  def initialize_flay
    self.flay_threshold ||= timebomb 1200, 100  # 80% of average :(
  end

  ##
  # Define tasks for plugin.

  def define_flay_tasks
    begin
      require 'flay_task'
      FlayTask.new :flay, self.flay_threshold
    rescue Exception
      # skip
    end
  end
end
