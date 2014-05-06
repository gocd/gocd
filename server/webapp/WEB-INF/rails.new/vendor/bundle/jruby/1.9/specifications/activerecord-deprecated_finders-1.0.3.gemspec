# -*- encoding: utf-8 -*-
# stub: activerecord-deprecated_finders 1.0.3 ruby lib

Gem::Specification.new do |s|
  s.name = "activerecord-deprecated_finders"
  s.version = "1.0.3"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Jon Leighton"]
  s.date = "2013-06-11"
  s.description = "Deprecated finder APIs extracted from Active Record."
  s.email = ["j@jonathanleighton.com"]
  s.homepage = "https://github.com/rails/activerecord-deprecated_finders"
  s.require_paths = ["lib"]
  s.rubygems_version = "2.1.9"
  s.summary = "This gem contains deprecated finder APIs extracted from Active Record."

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
