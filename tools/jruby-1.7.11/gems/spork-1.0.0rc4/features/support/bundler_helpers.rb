module BundlerHelpers
  extend self
  def install_bundle(dir)
    Dir.chdir(dir) do
      command = "bundle install --gemfile=#{Dir.pwd}/Gemfile --path=#{Dir.pwd}/.bundle"
      Bundler.with_clean_env do
        system(command)
      end
      $?.exitstatus
    end
  end

  def ensure_installed(dir)
    gemfile_lock = dir + "/Gemfile.lock"
    gemfile = dir + "/Gemfile"
    bundle_environment = dir + "/.bundle/environment.rb"
    case
    when File.exist?(gemfile_lock) && File.mtime(gemfile) > File.mtime(gemfile_lock)
      puts "Gemfile #{gemfile} has changed since it was locked. Re-locking..."
      FileUtils.rm(gemfile_lock)
      FileUtils.rm_rf(dir + "/.bundle")
    when ! File.exist?(bundle_environment)
      puts "Installing bundle #{gemfile}..."
    when File.mtime(bundle_environment) < File.mtime(gemfile_lock)
      puts "#{gemfile_lock} is newer than #{bundle_environment}.  Reinstalling"
    else
      return false
    end
    install_bundle(dir)
  end

  def set_gemfile(gemfile)
    gemfile = File.expand_path(gemfile)
    ensure_installed(File.dirname(gemfile))
    ENV["BUNDLE_GEMFILE"] = gemfile.to_s
  end
end
