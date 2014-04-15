require 'rubygems/remote_fetcher'
require 'uri'

##
# Deps plugin for hoe.
#
# === Tasks Provided:
#
# check_extra_deps::   Install missing dependencies.
# deps:email::         Print a contact list for gems dependent on this gem
# deps:fetch::         Fetch all the dependent gems of this gem into tarballs
# deps:list::          List all the dependent gems of this gem

module Hoe::Deps
  ##
  # The main rubygems repository.

  GEMURL = URI.parse 'http://gems.rubyforge.org'

  ##
  # Define tasks for plugin.

  def define_deps_tasks
    namespace :deps do
      desc "List all the dependent gems of this gem"
      task :list do
        gems = self.get_gems_by_name
        gem  = gems[self.name]

        abort "Couldn't find gem: #{self.name}" unless gem

        deps = self.dependent_upon self.name
        max  = deps.map { |s| s.full_name.size }.max

        puts "  dependents:"
        unless deps.empty? then
          deps.sort_by { |spec| spec.full_name }.each do |spec|
            vers = spec.dependencies.find {|s| s.name == name}.requirements_list
            puts "    %-*s - %s" % [max, spec.full_name, vers.join(", ")]
          end
        else
          puts "    none"
        end
      end

      desc "Print a contact list for gems dependent on this gem"
      task :email do
        gems = self.get_gems_by_name
        gem  = gems[self.name]

        abort "Couldn't find gem: #{self.name}" unless gem

        deps = self.dependent_upon self.name
        email = deps.map { |s| s.email }.compact.flatten.sort.uniq
        email = email.map { |s| s.split(/,\s*/) }.flatten.sort.uniq

        email.map! { |s| # don't you people realize how easy this is?
          s.gsub(/ at | _at_ |\s*(atmark|@nospam@|-at?-|@at?@|<at?>|\[at?\]|\(at?\))\s*/i, '@').gsub(/\s*(dot|\[d(ot)?\]|\.dot\.)\s*/i, '.').gsub(/\s+com$/, '.com')
        }

        bad, good = email.partition { |e| e !~ /^[\w.+-]+\@[\w.+-]+$/ }

        warn "Rejecting #{bad.size} email. I couldn't unmunge them." unless
          bad.empty?

        puts good.join(", ")

        warn "Warning: couldn't extract any email addresses" if good.empty?
      end

      desc "Fetch all the dependent gems of this gem into tarballs"
      task :fetch do
        deps = self.dependent_upon self.name

        mkdir "deps" unless File.directory? "deps"
        Dir.chdir "deps" do
          begin
            deps.sort_by { |spec| spec.full_name }.each do |spec|
              full_name = spec.full_name
              tgz_name  = "#{full_name}.tgz"
              gem_name  = "#{full_name}.gem"

              next if File.exist? tgz_name
              FileUtils.rm_rf [full_name, gem_name]

              begin
                warn "downloading #{full_name}"
                Gem::RemoteFetcher.fetcher.download(spec, GEMURL, Dir.pwd)
                FileUtils.mv "cache/#{gem_name}", '.'
              rescue Gem::RemoteFetcher::FetchError
                warn "  failed"
                next
              end

              warn "converting #{gem_name} to tarball"

              system "gem unpack #{gem_name} 2> /dev/null"
              system "gem spec -l #{gem_name} > #{full_name}/gemspec.rb"
              system "tar zmcf #{tgz_name} #{full_name}"
              FileUtils.rm_rf [full_name, gem_name, "cache"]
            end
          ensure
            FileUtils.rm_rf "cache"
          end
        end
      end
    end

    desc 'Install missing dependencies.'
    task :check_extra_deps do
      # extra_deps = [["rubyforge", ">= 1.0.0"], ["rake", ">= 0.8.1"]]
      (extra_deps + extra_dev_deps).each do |dep|
        begin
          gem(*dep)
        rescue Gem::LoadError
          name, req, = dep

          install_gem name, req, false
        end
      end
    end

    desc 'Install missing plugins.'
    task :install_plugins do
      install_missing_plugins
    end
  end

  ##
  # Return the rubygems source index.

  def get_source_index
    @@index ||= nil

    return @@index if @@index

    dump = unless File.exist? '.source_index' then
             warn "Fetching full index and caching. This can take a while."
             url = GEMURL + "Marshal.#{Gem.marshal_version}.Z"
             dump = Gem::RemoteFetcher.fetcher.fetch_path url
             dump = Gem.inflate dump

             warn "stripping index to latest gems"
             ary = Marshal.load dump

             h = {}
             Hash[ary].values.sort.each { |spec| h[spec.name] = spec }
             ary = h.map { |k,v| [v.full_name, v] }

             dump = Marshal.dump ary

             open '.source_index', 'wb' do |io| io.write dump end

             dump
           else
             open '.source_index', 'rb' do |io| io.read end
           end

    @@index = Marshal.load dump
  end

  ##
  # Return the latest rubygems.

  def get_latest_gems
    @@cache ||= Hash[*get_source_index.flatten].values
  end

  ##
  # Return a hash of the latest rubygems keyed by name.

  def get_gems_by_name
    @@by_name ||= Hash[*get_latest_gems.map { |gem|
                         [gem.name, gem, gem.full_name, gem]
                       }.flatten]
  end

  ##
  # Installs plugins that aren't currently installed

  def install_missing_plugins plugins = Hoe.bad_plugins
    version = '>= 0'

    plugins.each do |name|
      dash_name = name.to_s.gsub '_', '-'

      next if have_gem?("hoe-#{name}") or
                have_gem?(name) or
                have_gem?(dash_name)

      install_gem("hoe-#{name}", version, false) or
        install_gem(name, version, false) or
        install_gem(dash_name, version, false) or
        warn "could not install gem for #{name} plugin"
    end
  end

  ##
  # Return all the dependencies on the named rubygem.

  def dependent_upon name
    get_latest_gems.find_all { |gem|
      gem.dependencies.any? { |dep| dep.name == name }
    }
  end
end
