module Bundler
  # Returns current version of Ruby
  #
  # @return [CurrentRuby] Current version of Ruby
  def self.current_ruby
    @current_ruby ||= CurrentRuby.new
  end

  class CurrentRuby
    def on_18?
      RUBY_VERSION =~ /^1\.8/
    end

    def on_19?
      RUBY_VERSION =~ /^1\.9/
    end

    def on_20?
      RUBY_VERSION =~ /^2\.0/
    end

    def on_21?
      RUBY_VERSION =~ /^2\.1/
    end

    def ruby?
      !mswin? && (!defined?(RUBY_ENGINE) || RUBY_ENGINE == "ruby" || RUBY_ENGINE == "rbx" || RUBY_ENGINE == "maglev")
    end

    def ruby_18?
      ruby? && on_18?
    end

    def ruby_19?
      ruby? && on_19?
    end

    def ruby_20?
      ruby? && on_20?
    end

    def ruby_21?
      ruby? && on_21?
    end

    def mri?
      !mswin? && (!defined?(RUBY_ENGINE) || RUBY_ENGINE == "ruby")
    end

    def mri_18?
      mri? && on_18?
    end

    def mri_19?
      mri? && on_19?
    end

    def mri_20?
      mri? && on_20?
    end

    def mri_21?
      mri? && on_21?
    end

    def rbx?
      ruby? && defined?(RUBY_ENGINE) && RUBY_ENGINE == "rbx"
    end

    def jruby?
      defined?(RUBY_ENGINE) && RUBY_ENGINE == "jruby"
    end

    def jruby_18?
      jruby? && on_18?
    end

    def jruby_19?
      jruby? && on_19?
    end

    def maglev?
      defined?(RUBY_ENGINE) && RUBY_ENGINE == "maglev"
    end

    def mswin?
      Bundler::WINDOWS
    end

    def mingw?
      Bundler::WINDOWS && Gem::Platform.local.os == "mingw32" && Gem::Platform.local.cpu != 'x64'
    end

    def mingw_18?
      mingw? && on_18?
    end

    def mingw_19?
      mingw? && on_19?
    end

    def mingw_20?
      mingw? && on_20?
    end

    def mingw_21?
      mingw? && on_21?
    end

    def x64_mingw?
      Bundler::WINDOWS && Gem::Platform.local.os == "mingw32" && Gem::Platform.local.cpu == 'x64'
    end

    def x64_mingw_20?
      x64_mingw? && on_20?
    end

    def x64_mingw_21?
      x64_mingw? && on_21?
    end

  end
end
