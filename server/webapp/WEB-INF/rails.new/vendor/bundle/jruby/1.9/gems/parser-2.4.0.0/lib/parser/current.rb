module Parser
  class << self
    def warn_syntax_deviation(feature, version)
      warn "warning: parser/current is loading #{feature}, which recognizes"
      warn "warning: #{version}-compliant syntax, but you are running #{RUBY_VERSION}."
      warn "warning: please see https://github.com/whitequark/parser#compatibility-with-ruby-mri."
    end
    private :warn_syntax_deviation
  end

  case RUBY_VERSION
  when /^1\.8\./
    current_version = '1.8.7'
    if RUBY_VERSION != current_version
      warn_syntax_deviation 'parser/ruby18', current_version
    end

    require 'parser/ruby18'
    CurrentRuby = Ruby18

  when /^1\.9\./
    current_version = '1.9.3'
    if RUBY_VERSION != current_version
      warn_syntax_deviation 'parser/ruby19', current_version
    end

    require 'parser/ruby19'
    CurrentRuby = Ruby19

  when /^2\.0\./
    current_version = '2.0.0'
    if RUBY_VERSION != current_version
      warn_syntax_deviation 'parser/ruby20', current_version
    end

    require 'parser/ruby20'
    CurrentRuby = Ruby20

  when /^2\.1\./
    current_version = '2.1.8'
    if RUBY_VERSION != current_version
      warn_syntax_deviation 'parser/ruby21', current_version
    end

    require 'parser/ruby21'
    CurrentRuby = Ruby21

  when /^2\.2\./
    current_version = '2.2.6'
    if RUBY_VERSION != current_version
      warn_syntax_deviation 'parser/ruby22', current_version
    end

    require 'parser/ruby22'
    CurrentRuby = Ruby22

  when /^2\.3\./
    current_version = '2.3.3'
    if RUBY_VERSION != current_version
      warn_syntax_deviation 'parser/ruby23', current_version
    end

    require 'parser/ruby23'
    CurrentRuby = Ruby23

  when /^2\.4\./
    current_version = '2.4.0'
    if RUBY_VERSION != current_version
      warn_syntax_deviation 'parser/ruby24', current_version
    end

    require 'parser/ruby24'
    CurrentRuby = Ruby24

  else # :nocov:
    # Keep this in sync with released Ruby.
    warn_syntax_deviation 'parser/ruby24', '2.4.x'
    require 'parser/ruby24'
    CurrentRuby = Ruby24
  end
end
