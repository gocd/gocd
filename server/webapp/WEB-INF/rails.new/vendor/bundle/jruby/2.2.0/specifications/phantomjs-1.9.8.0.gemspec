# -*- encoding: utf-8 -*-
# stub: phantomjs 1.9.8.0 ruby lib

Gem::Specification.new do |s|
  s.name = "phantomjs"
  s.version = "1.9.8.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Christoph Olszowka"]
  s.date = "2015-01-20"
  s.description = "Auto-install phantomjs on demand for current platform. Comes with poltergeist integration."
  s.email = ["christoph at olszowka.de"]
  s.homepage = "https://github.com/colszowka/phantomjs-gem"
  s.licenses = ["MIT"]
  s.rubygems_version = "2.4.8"
  s.summary = "Auto-install phantomjs on demand for current platform. Comes with poltergeist integration."

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<poltergeist>, ["~> 1.5"])
      s.add_development_dependency(%q<capybara>, ["~> 2.4"])
      s.add_development_dependency(%q<rspec>, ["~> 2.99"])
      s.add_development_dependency(%q<simplecov>, [">= 0"])
      s.add_development_dependency(%q<rake>, [">= 0"])
    else
      s.add_dependency(%q<poltergeist>, ["~> 1.5"])
      s.add_dependency(%q<capybara>, ["~> 2.4"])
      s.add_dependency(%q<rspec>, ["~> 2.99"])
      s.add_dependency(%q<simplecov>, [">= 0"])
      s.add_dependency(%q<rake>, [">= 0"])
    end
  else
    s.add_dependency(%q<poltergeist>, ["~> 1.5"])
    s.add_dependency(%q<capybara>, ["~> 2.4"])
    s.add_dependency(%q<rspec>, ["~> 2.99"])
    s.add_dependency(%q<simplecov>, [">= 0"])
    s.add_dependency(%q<rake>, [">= 0"])
  end
end
