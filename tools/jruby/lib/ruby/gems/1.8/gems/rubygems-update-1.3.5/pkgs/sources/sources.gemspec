module Gem
  def self.sources_spec
    @sources_spec ||= Gem::Specification.new do |s|
      s.name = 'sources'
      s.version = '0.0.2'
      s.platform = Gem::Platform::RUBY
      s.required_rubygems_version = '> 0.9.4.3'
      s.summary = "This package provides download sources for remote gem installation"
      s.files = %w[lib/sources.rb]
      s.require_path = 'lib'
    end
  end
end

