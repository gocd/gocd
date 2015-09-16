# -*- encoding: utf-8 -*-
# stub: requirejs-rails 0.9.9 ruby lib

Gem::Specification.new do |s|
  s.name = "requirejs-rails"
  s.version = "0.9.9"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["John Whitley"]
  s.date = "2015-07-22"
  s.description = "This gem provides RequireJS support for your Rails 3 application."
  s.email = ["whitley@bangpath.org"]
  s.homepage = "http://github.com/jwhitley/requirejs-rails"
  s.require_paths = ["lib"]
  s.requirements = ["node.js is required for 'rake assets:precompile', used to run the r.js build", "If needed, jQuery should be v1.7 or greater (jquery-rails >= 1.0.17)."]
  s.rubygems_version = "2.1.9"
  s.summary = "Use RequireJS with the Rails 3+ Asset Pipeline"

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<railties>, [">= 3.1.1"])
      s.add_development_dependency(%q<rails>, [">= 3.1.1"])
      s.add_development_dependency(%q<sqlite3>, [">= 0"])
    else
      s.add_dependency(%q<railties>, [">= 3.1.1"])
      s.add_dependency(%q<rails>, [">= 3.1.1"])
      s.add_dependency(%q<sqlite3>, [">= 0"])
    end
  else
    s.add_dependency(%q<railties>, [">= 3.1.1"])
    s.add_dependency(%q<rails>, [">= 3.1.1"])
    s.add_dependency(%q<sqlite3>, [">= 0"])
  end
end
