##
# Newb plugin for hoe.
#
# === Tasks Provided:
#
# newb:: Get a new developer up to speed.

module Hoe::Newb
  # define tasks for the newb plugin
  def define_newb_tasks
    desc "Install deps, generate docs, run tests/specs."
    task :newb => %w(check_extra_deps install_plugins docs default) do
      puts <<-END

        GOOD TO GO! Tests are passing, docs are generated,
        dependencies are installed. Get to hacking.

      END
    end
  end
end
