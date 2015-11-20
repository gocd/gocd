# this file is maven DSL and used by maven via jars/maven_exec.rb

specfile = java.lang.System.getProperty('jars.specfile')

# needed since the gemspec does not allow absolute files
basedir( File.dirname( specfile ) )

# add jruby as provided for compilation when used vi Jars::Classpath#classpath
jar 'org.jruby:jruby-core', JRUBY_VERSION, :scope => :provided

# get ALL dependencies from the specfile
gemspec File.basename( specfile )

# we do not want those gem dependencies, each gem takes care of its
# own jar dependencies
gems = model.dependencies.select do |d|
  d.group_id == 'rubygems'
end
gems.each do |d|
  model.dependencies.remove( d )
end

# some output
model.dependencies.each do |d|
  puts "      " + d.group_id + ':' + d.artifact_id + (d.classifier ? ":" + d.classifier : "" ) + ":" + d.version unless d.artifact_id == 'jruby-core'
end
