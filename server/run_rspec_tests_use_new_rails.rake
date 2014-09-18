RUNNING_TESTS = 'running_tests'
ENV["RAILS_ENV"]="test"

def running_tests?
  ENV[RUNNING_TESTS] == 'true'
end

def running_tests!
  ENV[RUNNING_TESTS] = 'true'
end

def not_running_tests!
  ENV[RUNNING_TESTS] = 'false'
end

def server_class_path
  server_test_dependencies_file = File.join(File.dirname(__FILE__), 'target', 'server-test-dependencies')
  raise "#{server_test_dependencies_file} not found! did you run ./bn clean cruise:prepare?" unless File.exist?(server_test_dependencies_file)

  classpath = File.read(server_test_dependencies_file).split(':')
  classpath << File.join(File.dirname(__FILE__), 'webapp')
  classpath
end

def property_file_path
  File.join(File.dirname(__FILE__), 'properties', 'test')
end

RAILS_WORKING_DIR = File.join(File.dirname(__FILE__), 'webapp', 'WEB-INF', 'rails.new')
SPEC_SERVER_DIR = File.join(File.dirname(__FILE__), 'target', 'rails', 'spec_server')

def execute_under_rails command
  config_dir = File.join(SPEC_SERVER_DIR, 'config')

  db_dir = File.join(SPEC_SERVER_DIR, 'db')
  h2db_dir = File.join(db_dir, 'h2db')
  deltas_dir = File.join(db_dir, 'h2deltas')

  bundled_plugins_dir = File.join(SPEC_SERVER_DIR, 'plugins', 'bundled')
  external_plugins_dir = File.join(SPEC_SERVER_DIR, 'plugins', 'external')
  plugins_work_dir = File.join(SPEC_SERVER_DIR, 'plugins', 'plugins_work')

  log4j_properties = File.join(File.dirname(__FILE__), 'properties', 'test', 'log4j.properties')

  jruby = File.join(File.dirname(__FILE__), '..', 'tools', 'bin', 'go.jruby')

  mkpath db_dir # create SPEC_SERVER_DIR & db_dir so that SPEC_SERVER_DIR/config, SPEC_SERVER_DIR/db/h2db & SPEC_SERVER_DIR/db/h2deltas get copied correctly.

  cp_r(File.join(File.dirname(__FILE__), 'config'), config_dir)
  cp_r(File.join(File.dirname(__FILE__), 'db', 'dbtemplate', 'h2db'), db_dir)
  cp_r(File.join(File.dirname(__FILE__), 'db', 'migrate', 'h2deltas'), deltas_dir)

  # if windows
  if (/cygwin|mswin|mingw|bccwin|wince|emx/ =~ RUBY_PLATFORM) != nil
    #
    # This is necessary since -J-cp does not work on Windows in bat files.
    # It is allegedly fixed in 1.4.0
    # See: http://jira.codehaus.org/browse/JRUBY-2937
    #
    cmd = "cd #{RAILS_WORKING_DIR} && " +
            'set CP=' + [server_class_path, property_file_path].flatten.join(File::PATH_SEPARATOR) + " && " +
            jruby + ' -J-XX:MaxPermSize=400m ' + '-J-Dlog4j.configuration=' + log4j_properties +
            ' -J-Dalways.reload.config.file=true ' +
            ' -J-Dcruise.i18n.cache.life=0 ' +
            ' -J-Dcruise.config.dir=' + config_dir +
            ' -J-Dcruise.database.dir=' + h2db_dir +
            ' -J-Dplugins.go.provided.path=' + bundled_plugins_dir +
            ' -J-Dplugins.external.provided.path=' + external_plugins_dir +
            ' -J-Dplugins.work.path=' + plugins_work_dir +
            ' -J-Drails.use.compressed.js=false ' +
            (running_tests? ? ' -J-Dgo.enforce.serverId.immutability=N ' : '') +
            ' -S ' + command

    sh cmd
  else
    cmd = "cd #{RAILS_WORKING_DIR} &&" +
            ' ' + jruby +
            ' -J-Xmx1024m -J-XX:MaxPermSize=400m -J-Dlog4j.configuration=' + log4j_properties +
            ' -J-cp ' + [server_class_path, property_file_path].flatten.join(File::PATH_SEPARATOR) +
            ' -J-Dalways.reload.config.file=true' +
            ' -J-Dcruise.i18n.cache.life=0' +
            ' -J-Dcruise.config.dir=' + config_dir +
            ' -J-Dcruise.database.dir=' + h2db_dir +
            ' -J-Dplugins.go.provided.path=' + bundled_plugins_dir +
            ' -J-Dplugins.external.provided.path=' + external_plugins_dir +
            ' -J-Dplugins.work.path=' + plugins_work_dir +
            ' -J-Drails.use.compressed.js=false ' +
            (running_tests? ? ' -J-Dgo.enforce.serverId.immutability=N ' : '') +
            ' -S ' + command

    sh cmd
  end
end

task 'copy_historical_jars' do
  cp_r(File.join(File.dirname(__FILE__), 'historical_jars'), RAILS_WORKING_DIR)
end

task "clean-shine" do
  rm_rf File.join(File.dirname(__FILE__), 'tdb')
end

task 'clean_rails' do
  db = File.join(RAILS_WORKING_DIR, "db")
  [File.join(db, "h2db"), File.join(db, "config.git"), File.join(db, "shine")].each do |path|
    rm_rf(path)
  end
end

RAILS_DEPENDENCIES = ['copy_historical_jars', 'clean-shine', 'clean_rails']

task "spec" => RAILS_DEPENDENCIES do
  running_tests!
  rm_rf SPEC_SERVER_DIR
  reports_dir = File.join(File.dirname(__FILE__), 'target', 'reports', 'spec')
  puts "reports directory: " + reports_dir
  str = File.join(File.dirname(__FILE__), '..', 'tools', 'bin', 'go.rspec') +
          ' --require rspec-extra-formatters' +
          ' --format progress' +
          ' --format TapFormatter -o ' + reports_dir + '/spec_full_report' +
          ' --format JUnitFormatter -o ' + reports_dir + '/spec_full_report.xml' +
          ' --drb' +
          ' spec '
  str = str + "--pattern " + ENV['spec_module'] +'/**/*_spec.rb' if ENV.has_key? 'spec_module'
  execute_under_rails(str)
end

task "spec_module", [:spec_module_path] => RAILS_DEPENDENCIES do |t, args|
  raise "specify spec file to run. format: spec_file=<some_spec.rb> ./tools/bin/go.jruby -S rake --rakefile server/run_rspec_tests_use_new_rails.rake spec_module" if args[:spec_module_path] == nil

  path = args[:spec_module_path].split('rails.new/spec/')[1]

  running_tests!
  rm_rf SPEC_SERVER_DIR
  reports_dir = File.join(File.dirname(__FILE__), 'target', 'reports', 'spec')
  puts "reports directory: " + reports_dir
  str = File.join(File.dirname(__FILE__), '..', 'tools', 'bin', 'go.rspec') +
          ' --require rspec-extra-formatters' +
          ' --format progress' +
          ' --format TapFormatter -o ' + reports_dir + '/spec_full_report' +
          ' --format JUnitFormatter -o ' + reports_dir + '/spec_full_report.xml' +
          ' --drb' +
          ' spec' +
          ' --pattern ' + "#{path}/**/*_spec.rb"
  execute_under_rails(str)

  puts File.read(File.join(reports_dir, 'spec_full_report'))
end

task "spec_file", [:spec_file_path, :spec_file_line] => RAILS_DEPENDENCIES do |t, args|
  raise "specify spec file to run. format: spec_file=<some_spec.rb> ./tools/bin/go.jruby -S rake --rakefile server/run_rspec_tests_use_new_rails.rake spec_file" if args[:spec_file_path] == nil

  running_tests!
  rm_rf SPEC_SERVER_DIR
  reports_dir = File.join(File.dirname(__FILE__), 'target', 'reports', 'spec')
  str = File.join(File.dirname(__FILE__), '..', 'tools', 'bin', 'go.rspec') +
          ' --require rspec-extra-formatters' +
          ' --format progress' +
          ' --format TapFormatter -o ' + reports_dir + '/spec_full_report' +
          ' --format JUnitFormatter -o ' + reports_dir + '/spec_full_report.xml' +
          ' --drb' +
          " #{args[:spec_file_path]}"
  str += " --line #{args[:spec_file_line]}" unless args[:spec_file_line] == nil
  execute_under_rails(str)

  puts File.read(File.join(reports_dir, 'spec_full_report'))
end

task "exec" => RAILS_DEPENDENCIES do
  not_running_tests!
  execute_under_rails ENV['command']
end

task "spork" => RAILS_DEPENDENCIES do
  not_running_tests!
  rm_rf SPEC_SERVER_DIR
  execute_under_rails "spork"
end

task "spec_server" => RAILS_DEPENDENCIES do
  running_tests!
  rm_rf SPEC_SERVER_DIR
  execute_under_rails "script/spec_server -J-Xdebug -J-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5678"
end
