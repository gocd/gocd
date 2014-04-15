# -*- ruby -*-

require './lib/hoe.rb'

Hoe.add_include_dirs("../../minitest/dev/lib")

Hoe.new("hoe", Hoe::VERSION) do |hoe|
  hoe.rubyforge_name = "seattlerb"

  hoe.developer("Ryan Davis", "ryand-ruby@zenspider.com")

  hoe.testlib = :minitest
  hoe.blog_categories << "Seattle.rb" << "Ruby"
end

desc "Generate a list of tasks for doco. RDOC=1 for commented output"
task :tasks do
  tasks = `rake -T`.scan(/rake (\w+)\s+# (.*)/)
  tasks.reject! { |t,d| t =~ /^(clobber|tasks|re(package|docs))/ }
  max   = tasks.map { |x,y| x.size }.max

  tasks.each do |t,d|
    if ENV['RDOC'] then
      puts "# %-#{max+2}s %s" % [t + "::", d]
    else
      puts "* %-#{max}s - %s" % [t, d]
    end
  end
end

# vim: syntax=Ruby
