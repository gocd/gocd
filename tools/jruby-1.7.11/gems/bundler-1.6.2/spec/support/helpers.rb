module Spec
  module Helpers
    def reset!
      @in_p, @out_p, @err_p = nil, nil, nil
      Dir["#{tmp}/{gems/*,*}"].each do |dir|
        next if %(base remote1 gems rubygems).include?(File.basename(dir))
        unless ENV['BUNDLER_SUDO_TESTS']
          FileUtils.rm_rf(dir)
        else
          `sudo rm -rf #{dir}`
        end
      end
      FileUtils.mkdir_p(tmp)
      FileUtils.mkdir_p(home)
    end

    attr_reader :out, :err, :exitstatus

    def in_app_root(&blk)
      Dir.chdir(bundled_app, &blk)
    end

    def in_app_root2(&blk)
      Dir.chdir(bundled_app2, &blk)
    end

    def in_app_root_custom(root, &blk)
      Dir.chdir(root, &blk)
    end

    def run(cmd, *args)
      opts = args.last.is_a?(Hash) ? args.pop : {}
      expect_err = opts.delete(:expect_err)
      env = opts.delete(:env)
      groups = args.map {|a| a.inspect }.join(", ")
      setup = "require 'rubygems' ; require 'bundler' ; Bundler.setup(#{groups})\n"
      @out = ruby(setup + cmd, :expect_err => expect_err, :env => env)
    end

    def load_error_run(ruby, name, *args)
      cmd = <<-RUBY
        begin
          #{ruby}
        rescue LoadError => e
          $stderr.puts "ZOMG LOAD ERROR" if e.message.include?("-- #{name}")
        end
      RUBY
      opts = args.last.is_a?(Hash) ? args.pop : {}
      opts.merge!(:expect_err => true)
      args += [opts]
      run(cmd, *args)
    end

    def lib
      File.expand_path('../../../lib', __FILE__)
    end

    def bundle(cmd, options = {})
      expect_err = options.delete(:expect_err)
      exitstatus = options.delete(:exitstatus)
      sudo       = "sudo" if options.delete(:sudo)
      options["no-color"] = true unless options.key?("no-color") || %w(exec conf).include?(cmd.to_s[0..3])

      bundle_bin = File.expand_path('../../../bin/bundle', __FILE__)

      requires = options.delete(:requires) || []
      requires << File.expand_path('../fakeweb/'+options.delete(:fakeweb)+'.rb', __FILE__) if options.key?(:fakeweb)
      requires << File.expand_path('../artifice/'+options.delete(:artifice)+'.rb', __FILE__) if options.key?(:artifice)
      requires_str = requires.map{|r| "-r#{r}"}.join(" ")

      env = (options.delete(:env) || {}).map{|k,v| "#{k}='#{v}'"}.join(" ")
      args = options.map do |k,v|
        v == true ? " --#{k}" : " --#{k} #{v}" if v
      end.join

      cmd = "#{env} #{sudo} #{Gem.ruby} -I#{lib} #{requires_str} #{bundle_bin} #{cmd}#{args}"

      if exitstatus
        sys_status(cmd)
      else
        sys_exec(cmd, expect_err){|i| yield i if block_given? }
      end
    end

    def bundle_ruby(options = {})
      expect_err  = options.delete(:expect_err)
      exitstatus = options.delete(:exitstatus)
      options["no-color"] = true unless options.key?("no-color")

      bundle_bin = File.expand_path('../../../bin/bundle_ruby', __FILE__)

      requires = options.delete(:requires) || []
      requires << File.expand_path('../fakeweb/'+options.delete(:fakeweb)+'.rb', __FILE__) if options.key?(:fakeweb)
      requires << File.expand_path('../artifice/'+options.delete(:artifice)+'.rb', __FILE__) if options.key?(:artifice)
      requires_str = requires.map{|r| "-r#{r}"}.join(" ")

      env = (options.delete(:env) || {}).map{|k,v| "#{k}='#{v}' "}.join
      cmd = "#{env}#{Gem.ruby} -I#{lib} #{requires_str} #{bundle_bin}"

      if exitstatus
        sys_status(cmd)
      else
        sys_exec(cmd, expect_err){|i| yield i if block_given? }
      end
    end

    def ruby(ruby, options = {})
      expect_err = options.delete(:expect_err)
      env = (options.delete(:env) || {}).map{|k,v| "#{k}='#{v}' "}.join
      ruby.gsub!(/["`\$]/) {|m| "\\#{m}" }
      lib_option = options[:no_lib] ? "" : " -I#{lib}"
      sys_exec(%{#{env}#{Gem.ruby}#{lib_option} -e "#{ruby}"}, expect_err)
    end

    def load_error_ruby(ruby, name, opts = {})
      cmd = <<-R
        begin
          #{ruby}
        rescue LoadError => e
          $stderr.puts "ZOMG LOAD ERROR"# if e.message.include?("-- #{name}")
        end
      R
      ruby(cmd, opts.merge(:expect_err => true))
    end

    def gembin(cmd)
      lib = File.expand_path("../../../lib", __FILE__)
      old, ENV['RUBYOPT'] = ENV['RUBYOPT'], "#{ENV['RUBYOPT']} -I#{lib}"
      cmd = bundled_app("bin/#{cmd}") unless cmd.to_s.include?("/")
      sys_exec(cmd.to_s)
    ensure
      ENV['RUBYOPT'] = old
    end

    def sys_exec(cmd, expect_err = false)
      Open3.popen3(cmd.to_s) do |stdin, stdout, stderr|
        @in_p, @out_p, @err_p = stdin, stdout, stderr

        yield @in_p if block_given?
        @in_p.close

        @out = @out_p.read_available_bytes.strip
        @err = @err_p.read_available_bytes.strip
      end

      puts @err unless expect_err || @err.empty? || !$show_err
      @out
    end

    def sys_status(cmd)
      @err = nil
      @out = %x{#{cmd}}.strip
      @exitstatus = $?.exitstatus
    end

    def config(config = nil)
      path = bundled_app('.bundle/config')
      return YAML.load_file(path) unless config
      FileUtils.mkdir_p(File.dirname(path))
      File.open(path, 'w') do |f|
        f.puts config.to_yaml
      end
      config
    end

    def gemfile(*args)
      path = bundled_app("Gemfile")
      path = args.shift if args.first.is_a?(Pathname)
      str  = args.shift || ""
      path.dirname.mkpath
      File.open(path.to_s, 'w') do |f|
        f.puts strip_whitespace(str)
      end
    end

    def lockfile(*args)
      path = bundled_app("Gemfile.lock")
      path = args.shift if args.first.is_a?(Pathname)
      str  = args.shift || ""
      File.open(path.to_s, 'w') do |f|
        f.puts strip_whitespace(str)
      end
    end

    def strip_whitespace(str)
      # Trim the leading spaces
      spaces = str[/\A\s+/, 0] || ""
      str.gsub(/^#{spaces}/, '')
    end

    def install_gemfile(*args)
      gemfile(*args)
      opts = args.last.is_a?(Hash) ? args.last : {}
      opts[:retry] ||= 0
      bundle :install, opts
    end

    def install_gems(*gems)
      gems.each do |g|
        path = "#{gem_repo1}/gems/#{g}.gem"

        raise "OMG `#{path}` does not exist!" unless File.exist?(path)

        gem_command :install, "--no-rdoc --no-ri --ignore-dependencies #{path}"
      end
    end

    alias install_gem install_gems

    def with_gem_path_as(path)
      gem_home, gem_path = ENV['GEM_HOME'], ENV['GEM_PATH']
      ENV['GEM_HOME'], ENV['GEM_PATH'] = path.to_s, path.to_s
      yield
    ensure
      ENV['GEM_HOME'], ENV['GEM_PATH'] = gem_home, gem_path
    end

    def break_git!
      FileUtils.mkdir_p(tmp("broken_path"))
      File.open(tmp("broken_path/git"), "w", 0755) do |f|
        f.puts "#!/usr/bin/env ruby\nSTDERR.puts 'This is not the git you are looking for'\nexit 1"
      end

      ENV["PATH"] = "#{tmp("broken_path")}:#{ENV["PATH"]}"
    end

    def fake_man!
      FileUtils.mkdir_p(tmp("fake_man"))
      File.open(tmp("fake_man/man"), "w", 0755) do |f|
        f.puts "#!/usr/bin/env ruby\nputs ARGV.inspect\n"
      end

      ENV["PATH"] = "#{tmp("fake_man")}:#{ENV["PATH"]}"
    end

    def kill_path!
      ENV["PATH"] = ""
    end

    def system_gems(*gems)
      gems = gems.flatten

      FileUtils.rm_rf(system_gem_path)
      FileUtils.mkdir_p(system_gem_path)

      Gem.clear_paths

      gem_home, gem_path, path = ENV['GEM_HOME'], ENV['GEM_PATH'], ENV['PATH']
      ENV['GEM_HOME'], ENV['GEM_PATH'] = system_gem_path.to_s, system_gem_path.to_s

      install_gems(*gems)
      if block_given?
        begin
          yield
        ensure
          ENV['GEM_HOME'], ENV['GEM_PATH'] = gem_home, gem_path
          ENV['PATH'] = path
        end
      end
    end

    def realworld_system_gems(*gems)
      gems = gems.flatten

      FileUtils.rm_rf(system_gem_path)
      FileUtils.mkdir_p(system_gem_path)

      Gem.clear_paths

      gem_home, gem_path, path = ENV['GEM_HOME'], ENV['GEM_PATH'], ENV['PATH']
      ENV['GEM_HOME'], ENV['GEM_PATH'] = system_gem_path.to_s, system_gem_path.to_s

      gems.each do |gem|
        gem_command :install, "--no-rdoc --no-ri #{gem}"
      end
      if block_given?
        begin
          yield
        ensure
          ENV['GEM_HOME'], ENV['GEM_PATH'] = gem_home, gem_path
          ENV['PATH'] = path
        end
      end
    end

    def cache_gems(*gems)
      gems = gems.flatten

      FileUtils.rm_rf("#{bundled_app}/vendor/cache")
      FileUtils.mkdir_p("#{bundled_app}/vendor/cache")

      gems.each do |g|
        path = "#{gem_repo1}/gems/#{g}.gem"
        raise "OMG `#{path}` does not exist!" unless File.exist?(path)
        FileUtils.cp(path, "#{bundled_app}/vendor/cache")
      end
    end

    def simulate_new_machine
      system_gems []
      FileUtils.rm_rf default_bundle_path
      FileUtils.rm_rf bundled_app('.bundle')
    end

    def simulate_platform(platform)
      old, ENV['BUNDLER_SPEC_PLATFORM'] = ENV['BUNDLER_SPEC_PLATFORM'], platform.to_s
      yield if block_given?
    ensure
      ENV['BUNDLER_SPEC_PLATFORM'] = old if block_given?
    end

    def simulate_ruby_engine(engine, version = "1.6.0")
      return if engine == local_ruby_engine

      old, ENV['BUNDLER_SPEC_RUBY_ENGINE'] = ENV['BUNDLER_SPEC_RUBY_ENGINE'], engine
      old_version, ENV['BUNDLER_SPEC_RUBY_ENGINE_VERSION'] = ENV['BUNDLER_SPEC_RUBY_ENGINE_VERSION'], version
      yield if block_given?
    ensure
      ENV['BUNDLER_SPEC_RUBY_ENGINE'] = old if block_given?
      ENV['BUNDLER_SPEC_RUBY_ENGINE_VERSION'] = old_version if block_given?
    end

    def simulate_bundler_version(version)
      old, ENV['BUNDLER_SPEC_VERSION'] = ENV['BUNDLER_SPEC_VERSION'], version.to_s
      yield if block_given?
    ensure
      ENV['BUNDLER_SPEC_VERSION'] = old if block_given?
    end

    def revision_for(path)
      Dir.chdir(path) { `git rev-parse HEAD`.strip }
    end

    def capture_output
      fake_stdout = StringIO.new
      actual_stdout = $stdout
      $stdout = fake_stdout
      yield
      fake_stdout.rewind
      fake_stdout.read
    ensure
      $stdout = actual_stdout
    end
  end
end
