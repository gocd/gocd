# -*- encoding: utf-8 -*-
$:.push File.expand_path("../jar", __FILE__)
require 'rhino/jar_path'

Gem::Specification.new do |s|
  s.name = %q{therubyrhino_jar}
  s.version = "#{Rhino::JAR_VERSION}"
  s.authors = ["Charles Lowell", "Karol Bucek"]
  s.email = ['cowboyd@thefrontside.net', 'self@kares.org']

  s.summary     = "Rhino's jars packed for therubyrhino"
  s.description = %q{Rhino's js.jar classes packaged as a JRuby gem.}
  s.license = "MPL-2.0"

  s.homepage = %q{http://github.com/cowboyd/therubyrhino}
  s.rubyforge_project = %q{therubyrhino}

  s.require_paths = ["jar"]
  s.files = `git ls-files`.split("\n").sort.
    select { |file| file == 'therubyrhino_jar.gemspec' || file =~ /^jar\// }

end