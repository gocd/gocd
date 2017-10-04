# -*- encoding: utf-8 -*-
# stub: activerecord-deprecated_finders 1.0.4 ruby lib

Gem::Specification.new do |s|
  s.name = "activerecord-deprecated_finders"
  s.version = "1.0.4"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Jon Leighton"]
  s.date = "2015-04-01"
  s.description = "Deprecated finder APIs extracted from Active Record."
  s.email = ["j@jonathanleighton.com"]
  s.homepage = "https://github.com/rails/activerecord-deprecated_finders"
  s.licenses = ["MIT"]
  s.rubygems_version = "2.4.8"
  s.summary = "This gem contains deprecated finder APIs extracted from Active Record."

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<minitest>, [">= 3"])
      s.add_development_dependency(%q<activerecord>, ["< 5", ">= 4.0.0.beta"])
      s.add_development_dependency(%q<sqlite3>, ["~> 1.3"])
    else
      s.add_dependency(%q<minitest>, [">= 3"])
      s.add_dependency(%q<activerecord>, ["< 5", ">= 4.0.0.beta"])
      s.add_dependency(%q<sqlite3>, ["~> 1.3"])
    end
  else
    s.add_dependency(%q<minitest>, [">= 3"])
    s.add_dependency(%q<activerecord>, ["< 5", ">= 4.0.0.beta"])
    s.add_dependency(%q<sqlite3>, ["~> 1.3"])
  end
end
