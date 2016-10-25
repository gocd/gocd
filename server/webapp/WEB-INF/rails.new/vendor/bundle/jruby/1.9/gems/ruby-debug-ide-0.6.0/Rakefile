require 'bundler/gem_tasks'
require 'rake/testtask'

desc 'Default: run unit tests.'
task :default => [:test]

# Unit tests
Rake::TestTask.new do |t|
  t.libs << "test"
  t.libs << "test-base"
  t.pattern = 'test/**/*_test.rb'
  t.verbose = true
  t.warning = false
end

desc "Create a ChangeLog"
# simple rake task to output a changelog between two commits, tags ...
# output is formatted simply, commits are grouped under each author name
desc "generate changelog with nice clean output"
task :changelog, :since_c, :until_c do |t,args|
  since_c = args[:since_c] || `git tag | head -1`.chomp
  until_c = args[:until_c]
  cmd=`git log --pretty='format:%ci::%an <%ae>::%s::%H' #{since_c}..#{until_c}`

  entries = Hash.new
  changelog_content = String.new

  cmd.split("\n").each do |entry|
    date, author, subject, hash = entry.chomp.split("::")
    entries[author] = Array.new unless entries[author]
    day = date.split(" ").first
    entries[author] << "#{subject} (#{hash})" unless subject =~ /Merge/
  end

  # generate clean output
  entries.keys.each do |author|
    changelog_content += author + "\n"
    entries[author].reverse.each { |entry| changelog_content += "  * #{entry}\n" }
  end

  puts changelog_content
end