# -*- encoding: utf-8 -*-
$:.push File.expand_path("../lib", __FILE__)
require "rhino/version"

Gem::Specification.new do |s|
  s.name = %q{therubyrhino}
  s.version = Rhino::VERSION
  s.authors = ["Charles Lowell"]
  s.email = %q{cowboyd@thefrontside.net}

  s.summary = %q{Embed the Rhino JavaScript interpreter into JRuby}
  s.description = %q{Call javascript code and manipulate javascript objects from ruby. Call ruby code and manipulate ruby objects from javascript.}
  s.homepage = %q{http://github.com/cowboyd/therubyrhino}
  s.rubyforge_project = %q{therubyrhino}
  s.extra_rdoc_files = ["README.md"]
  s.license = "MIT"

  s.require_paths = ["lib"]
  s.files = `git ls-files`.split("\n").sort.
    reject { |file| file == 'therubyrhino_jar.gemspec' || file =~ /^jar\// }

  s.add_dependency "therubyrhino_jar", '>= 1.7.3'

  s.add_development_dependency "rspec", "~> 2.14.1"
  s.add_development_dependency "mocha", "~> 0.13.3"
end
