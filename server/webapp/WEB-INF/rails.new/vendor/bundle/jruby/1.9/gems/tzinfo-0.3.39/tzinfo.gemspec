Gem::Specification.new do |s|
  s.name = 'tzinfo'
  s.version = '0.3.39'
  s.author = 'Philip Ross'
  s.email = 'phil.ross@gmail.com'
  s.homepage = 'http://tzinfo.github.io'
  s.license = 'MIT'
  s.platform = Gem::Platform::RUBY
  s.summary = 'Daylight-savings aware timezone library'
  s.description = 'TZInfo is a Ruby library that uses the standard tz (Olson) database to provide daylight savings aware transformations between times in different time zones.'
  s.files = %w(CHANGES LICENSE Rakefile README tzinfo.gemspec .yardopts) +
            Dir['lib/**/*'].delete_if {|f| f.include?('.svn')}
  s.test_files = Dir['test/**/*'].delete_if {|f| f.include?('.svn')}
  s.require_path = "lib"
  s.has_rdoc = true
  s.extra_rdoc_files = %w[README CHANGES]
  s.rdoc_options = %w[--exclude definitions --exclude indexes]
  s.rubyforge_project = "tzinfo"
end
