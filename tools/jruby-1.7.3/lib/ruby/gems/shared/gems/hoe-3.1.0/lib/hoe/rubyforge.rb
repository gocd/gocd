##
# RubyForge plugin for hoe.
#
# Adds a release dependency that cleans, packages, performs sanity
# checks, and releases to RubyForge.
#
# === Tasks Provided:
#
# release_to_rubyforge:: Release to rubyforge when release task is run.

module Hoe::RubyForge
  def initialize_rubyforge
    require 'rubyforge'

    dependency_target << ['rubyforge', ">= #{::RubyForge::VERSION}"]
  end

  def define_rubyforge_tasks # :nodoc:
    # no doco, invisible hook
    task :release_to => :release_to_rubyforge

    desc 'Release to rubyforge.'
    task :release_to_rubyforge => [:clean, :package, :release_sanity] do
      rf = RubyForge.new.configure
      puts "Logging in"
      rf.login

      c = rf.userconfig
      c["release_notes"]   = description if description
      c["release_changes"] = changes     if changes
      c["preformatted"]    = true

      files = [(@need_tar ? Dir["pkg/*.tgz"] : nil),
               (@need_zip ? Dir["pkg/*.zip"] : nil),
               Dir["pkg/*.gem"]].flatten.compact

      puts "Releasing #{name} v. #{version}"

      rf.add_release rubyforge_name, name, version, *files
    end

    if Hoe.plugins.include? :publish then
      path   = File.expand_path("~/.rubyforge/user-config.yml")
      config = YAML.load(File.read(path)) rescue nil
      if config then
        base = "/var/www/gforge-projects"
        dir  = "#{base}/#{rubyforge_name}/#{remote_rdoc_dir}"

        rdoc_locations << "#{config["username"]}@rubyforge.org:#{dir}"
      else
        warn "Couldn't read #{path}. Run `rubyforge setup`."
      end

      desc 'Post announcement to rubyforge.'
      task :post_news do
        require 'rubyforge'
        subject, title, body, urls = announcement

        rf = RubyForge.new.configure
        rf.login
        rf.post_news(rubyforge_name, subject, "#{title}\n\n#{body}")
        puts "Posted to rubyforge"
      end
    end
  end
end
