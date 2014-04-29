##
# Debug plugin for hoe.
#
# === Tasks Provided:
#
# check_manifest::     Verify the manifest.
# config_hoe::         Create a fresh ~/.hoerc file.
# debug_gem::          Show information about the gem.

module Hoe::Debug

  include Rake::DSL if defined? ::Rake::DSL

  Hoe::DEFAULT_CONFIG["exclude"] = /tmp$|CVS|TAGS|\.(svn|git|DS_Store)/

  # :stopdoc:

  DIFF = if Hoe::WINDOZE
           'diff.exe'
         else
           if system("gdiff", __FILE__, __FILE__)
             'gdiff' # solaris and kin suck
           else
             'diff'
           end
         end unless defined? DIFF

  # :startdoc:

  ##
  # Define tasks for plugin.

  def define_debug_tasks
    desc 'Create a fresh ~/.hoerc file.'
    task :config_hoe do
      with_config do |config, path|
        File.open(path, "w") do |f|
          YAML.dump(Hoe::DEFAULT_CONFIG.merge(config), f)
        end

        editor = ENV['EDITOR'] || 'vi'
        system "#{editor} #{path}" if ENV['SHOW_EDITOR'] != 'no'
      end
    end

    desc 'Verify the manifest.'
    task :check_manifest => :clean do
      check_manifest
    end

    desc 'Show information about the gem.'
    task :debug_gem do
      puts spec.to_ruby
    end
  end

  ##
  # Verifies your Manifest.txt against the files in your project.

  def check_manifest
    f = "Manifest.tmp"
    require 'find'
    files = []
    with_config do |config, _|
      exclusions = config["exclude"]

      Find.find '.' do |path|
        next unless File.file? path
        next if path =~ exclusions
        files << path[2..-1]
      end

      files = files.sort.join "\n"

      File.open f, 'w' do |fp| fp.puts files end

      verbose = { :verbose => Rake.application.options.verbose }

      begin
        sh "#{DIFF} -du Manifest.txt #{f}", verbose
      ensure
        rm f, verbose
      end
    end
  end
end
