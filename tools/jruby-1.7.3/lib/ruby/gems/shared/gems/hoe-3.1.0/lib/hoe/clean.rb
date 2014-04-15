##
# Clean plugin for hoe.
#
# === Tasks Provided:
#
# clean::              Clean up all the extras.

module Hoe::Clean
  ##
  # Optional: An array of file patterns to delete on clean.

  attr_accessor :clean_globs

  ##
  # Initialize variables for plugin.

  def initialize_clean
    self.clean_globs ||= %w(diff diff.txt TAGS ri deps .source_index
                            *.gem **/*~ **/.*~ **/*.rbc coverage*)
  end

  ##
  # Define tasks for plugin.

  def define_clean_tasks
    desc 'Clean up all the extras.'
    task :clean => [ :clobber_docs, :clobber_package ] do
      clean_globs.each do |pattern|
        files = Dir[pattern]
        rm_rf files, :verbose => true unless files.empty?
      end
    end
  end
end
