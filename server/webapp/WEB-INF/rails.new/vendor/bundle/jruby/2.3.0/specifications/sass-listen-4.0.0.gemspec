# -*- encoding: utf-8 -*-
# stub: sass-listen 4.0.0 ruby lib

Gem::Specification.new do |s|
  s.name = "sass-listen".freeze
  s.version = "4.0.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["Thibaud Guillaume-Gentil".freeze]
  s.date = "2017-07-13"
  s.description = "This fork of guard/listen provides a stable API for users of the ruby Sass CLI".freeze
  s.email = "thibaud@thibaud.gg".freeze
  s.homepage = "https://github.com/sass/listen".freeze
  s.licenses = ["MIT".freeze]
  s.required_ruby_version = Gem::Requirement.new(">= 1.9.3".freeze)
  s.rubygems_version = "2.6.13".freeze
  s.summary = "Fork of guard/listen".freeze

  s.installed_by_version = "2.6.13" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<rb-fsevent>.freeze, [">= 0.9.4", "~> 0.9"])
      s.add_runtime_dependency(%q<rb-inotify>.freeze, [">= 0.9.7", "~> 0.9"])
      s.add_development_dependency(%q<bundler>.freeze, [">= 1.3.5"])
    else
      s.add_dependency(%q<rb-fsevent>.freeze, [">= 0.9.4", "~> 0.9"])
      s.add_dependency(%q<rb-inotify>.freeze, [">= 0.9.7", "~> 0.9"])
      s.add_dependency(%q<bundler>.freeze, [">= 1.3.5"])
    end
  else
    s.add_dependency(%q<rb-fsevent>.freeze, [">= 0.9.4", "~> 0.9"])
    s.add_dependency(%q<rb-inotify>.freeze, [">= 0.9.7", "~> 0.9"])
    s.add_dependency(%q<bundler>.freeze, [">= 1.3.5"])
  end
end
