module Bundler
  class Env

    def write(io)
      io.write(report)
    end

    def report
      out = "Bundler #{Bundler::VERSION}\n"

      out << "Ruby #{RUBY_VERSION} (#{RUBY_RELEASE_DATE}"
      out << " patchlevel #{RUBY_PATCHLEVEL}" if defined? RUBY_PATCHLEVEL
      out << ") [#{RUBY_PLATFORM}]\n"

      out << "Rubygems #{Gem::VERSION}\n"

      out << "rvm #{ENV['rvm_version']}\n" if ENV['rvm_version']

      out << "GEM_HOME #{ENV['GEM_HOME']}\n"

      out << "GEM_PATH #{ENV['GEM_PATH']}\n" unless ENV['GEM_PATH'] == ENV['GEM_HOME']

      %w(rubygems-bundler open_gem).each do |name|
        specs = Gem::Specification.find_all{|s| s.name == name }
        out << "#{name} (#{specs.map(&:version).join(',')})\n" unless specs.empty?
      end

      out << "\nBundler settings\n" unless Bundler.settings.all.empty?
      Bundler.settings.all.each do |setting|
        out << "  #{setting}\n"
        Bundler.settings.pretty_values_for(setting).each do |line|
          out << "    " << line << "\n"
        end
      end

      out << "\n\n" << "Gemfile\n"
      out << read_file("Gemfile") << "\n"

      out << "\n\n" << "Gemfile.lock\n"
      out << read_file("Gemfile.lock") << "\n"

      out
    end

  private

    def read_file(filename)
      File.read(filename).strip
    rescue Errno::ENOENT
      "<No #{filename} found>"
    rescue => e
      "#{e.class}: #{e.message}"
    end

  end
end
