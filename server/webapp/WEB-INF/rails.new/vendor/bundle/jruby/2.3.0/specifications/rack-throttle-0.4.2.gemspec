# -*- encoding: utf-8 -*-
# stub: rack-throttle 0.4.2 ruby lib

Gem::Specification.new do |s|
  s.name = "rack-throttle".freeze
  s.version = "0.4.2"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Arto Bendiken".freeze]
  s.date = "2016-10-04"
  s.description = "Rack middleware for rate-limiting incoming HTTP requests.".freeze
  s.email = "arto@bendiken.net".freeze
  s.homepage = "https://github.com/bendiken/rack-throttle".freeze
  s.licenses = ["Public Domain".freeze]
  s.required_ruby_version = Gem::Requirement.new(">= 1.8.2".freeze)
  s.rubygems_version = "2.6.13".freeze
  s.summary = "HTTP request rate limiter for Rack applications.".freeze

  s.installed_by_version = "2.6.13" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<bundler>.freeze, [">= 1.0.0"])
      s.add_development_dependency(%q<rack-test>.freeze, [">= 0"])
      s.add_development_dependency(%q<rspec>.freeze, [">= 0"])
      s.add_development_dependency(%q<yard>.freeze, [">= 0"])
      s.add_development_dependency(%q<timecop>.freeze, [">= 0"])
      s.add_runtime_dependency(%q<rack>.freeze, [">= 1.0.0"])
    else
      s.add_dependency(%q<bundler>.freeze, [">= 1.0.0"])
      s.add_dependency(%q<rack-test>.freeze, [">= 0"])
      s.add_dependency(%q<rspec>.freeze, [">= 0"])
      s.add_dependency(%q<yard>.freeze, [">= 0"])
      s.add_dependency(%q<timecop>.freeze, [">= 0"])
      s.add_dependency(%q<rack>.freeze, [">= 1.0.0"])
    end
  else
    s.add_dependency(%q<bundler>.freeze, [">= 1.0.0"])
    s.add_dependency(%q<rack-test>.freeze, [">= 0"])
    s.add_dependency(%q<rspec>.freeze, [">= 0"])
    s.add_dependency(%q<yard>.freeze, [">= 0"])
    s.add_dependency(%q<timecop>.freeze, [">= 0"])
    s.add_dependency(%q<rack>.freeze, [">= 1.0.0"])
  end
end
