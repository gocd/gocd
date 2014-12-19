# -*- encoding: utf-8 -*-
# stub: sprockets-rails 2.0.1 ruby lib

Gem::Specification.new do |s|
  s.name = "sprockets-rails"
  s.version = "2.0.1"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Joshua Peek"]
  s.date = "2013-10-16"
  s.email = "josh@joshpeek.com"
  s.homepage = "https://github.com/rails/sprockets-rails"
  s.licenses = ["MIT"]
  s.require_paths = ["lib"]
  s.rubygems_version = "2.1.9"
  s.summary = "Sprockets Rails integration"

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<sprockets>, ["~> 2.8"])
      s.add_runtime_dependency(%q<actionpack>, [">= 3.0"])
      s.add_runtime_dependency(%q<activesupport>, [">= 3.0"])
      s.add_development_dependency(%q<rake>, [">= 0"])
    else
      s.add_dependency(%q<sprockets>, ["~> 2.8"])
      s.add_dependency(%q<actionpack>, [">= 3.0"])
      s.add_dependency(%q<activesupport>, [">= 3.0"])
      s.add_dependency(%q<rake>, [">= 0"])
    end
  else
    s.add_dependency(%q<sprockets>, ["~> 2.8"])
    s.add_dependency(%q<actionpack>, [">= 3.0"])
    s.add_dependency(%q<activesupport>, [">= 3.0"])
    s.add_dependency(%q<rake>, [">= 0"])
  end
end
