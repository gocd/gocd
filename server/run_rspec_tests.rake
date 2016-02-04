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
  dependencies_for_test_file = File.join(File.dirname(__FILE__), 'target/server-test-dependencies')

  cd File.dirname(__FILE__) do
    sh("mvn dependency:build-classpath -DincludeScope=test -Dmdep.outputFile=#{dependencies_for_test_file}")
  end

  [
    File.join(File.dirname(__FILE__), 'target/classes'),
    File.join(File.dirname(__FILE__), 'target/test-classes'),
    File.read(dependencies_for_test_file).split(File::PATH_SEPARATOR),
    File.join(File.dirname(__FILE__), 'webapp'),
  ].flatten
end

def property_file_path
  File.join(File.dirname(__FILE__), 'properties', 'test')
end

RAILS_WORKING_DIR = File.join(File.dirname(__FILE__), 'webapp', 'WEB-INF', 'rails.new')
SPEC_SERVER_DIR = File.join(File.dirname(__FILE__), 'target', 'rails', 'spec_server')

def copy_config_and_db
  mkpath db_dir # create SPEC_SERVER_DIR & db_dir so that SPEC_SERVER_DIR/config, SPEC_SERVER_DIR/db/h2db & SPEC_SERVER_DIR/db/h2deltas get copied correctly.
  cp_r(File.join(File.dirname(__FILE__), 'config'), config_dir)
  cp_r(File.join(File.dirname(__FILE__), 'db', 'dbtemplate', 'h2db'), db_dir)
  cp_r(File.join(File.dirname(__FILE__), 'db', 'migrate', 'h2deltas'), deltas_dir)
end

def config_dir
  File.join(SPEC_SERVER_DIR, 'config')
end

def db_dir
  File.join(SPEC_SERVER_DIR, 'db')
end

def h2db_dir
  File.join(db_dir, 'h2db')
end

def deltas_dir
  File.join(db_dir, 'h2deltas')
end

def bundled_plugins_dir
  File.join(SPEC_SERVER_DIR, 'plugins', 'bundled')
end

def external_plugins_dir
  File.join(SPEC_SERVER_DIR, 'plugins', 'external')
end

def plugins_work_dir
  File.join(SPEC_SERVER_DIR, 'plugins', 'plugins_work')
end

def log4j_properties
  File.join(File.dirname(__FILE__), 'properties', 'test', 'log4j.properties')
end

def go_system_properties
  properties = {
    'log4j.configuration' => log4j_properties,
    'always.reload.config.file' => true,
    'cruise.i18n.cache.life' => 0,
    'cruise.config.dir' => config_dir,
    'cruise.database.dir' => h2db_dir,
    'plugins.go.provided.path' => bundled_plugins_dir,
    'plugins.external.provided.path' => external_plugins_dir,
    'plugins.work.path' => plugins_work_dir,
    'rails.use.compressed.js' => false
  }

  if running_tests?
    properties.merge!('go.enforce.serverId.immutability' => 'N')
  end

  properties
end

def execute_under_rails command
  copy_config_and_db

  jruby = File.join(File.dirname(__FILE__), '..', 'tools', 'rails', 'bin', 'jruby')

  java_opts = []
  java_opts += %W(
    -J-cp #{[server_class_path, property_file_path].flatten.join(File::PATH_SEPARATOR)}
    -J-XX:MaxPermSize=400m
  )

  java_opts += go_system_properties.collect {|k, v| "-J-D#{k}=#{v}"}

  java_opts = java_opts.flatten

  cd RAILS_WORKING_DIR do
    sh_with_environment("#{jruby} -S #{command}", {'JRUBY_OPTS' => java_opts.join(' ')})
  end
end

def sh_with_environment(cmd, env={})
  original_env = ENV.clone.to_hash
  begin
    export_or_set = Gem.win_platform? ? 'set' : 'export'
    env.each do |k, v|
      $stderr.puts "#{export_or_set} #{k}=#{v}"
    end

    ENV.replace(ENV.clone.to_hash.merge(env))
    sh(cmd)
  ensure
    ENV.replace(original_env)
  end
end

def rspec_executable
  File.join(File.dirname(__FILE__), '..', 'tools', 'bin', 'go.rspec')
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

task 'export-system-properties-file-for-idea' => RAILS_DEPENDENCIES do
  # major fudjing of classpaths to be able to run tests in intellij without compiling jars -

  # when intellij runs jruby/rspec, it adds dependencies to jruby via -J-cp
  # the deps that intellij adds does not contain the "provided" dependencies

  # this method dumps the 3rd party dependencies of the server module, and the jruby binary picks them up
  running_tests!
  copy_config_and_db
  exports_file = File.join(File.dirname(__FILE__), 'target', 'go-system-properties')
  File.open(exports_file, 'w') do |f|
    go_system_properties.each do |k, v|
      f.puts(%Q{export JRUBY_OPTS="-J-D#{k}=#{v} ${JRUBY_OPTS}"})
    end

    unless File.exist?(File.join(File.dirname(__FILE__), 'target', 'dependency.classpath'))
      cd File.dirname(__FILE__) do
        sh('mvn dependency:build-classpath -DexcludeGroupIds=com.thoughtworks.go -Dmdep.outputFile=target/dependency.classpath')
      end
    end

    f.puts(%Q{GO_DEPENDENCY_CLASSPATH=$(cat #{File.join(File.dirname(__FILE__), 'target', 'dependency.classpath')})})
  end
end

task "spec" => RAILS_DEPENDENCIES do
  running_tests!
  rm_rf SPEC_SERVER_DIR
  reports_dir = File.join(File.dirname(__FILE__), 'target', 'reports', 'spec')
  puts "reports directory: " + reports_dir
  ENV['REPORTS_DIR'] = reports_dir

  str = "#{rspec_executable} spec"
  str = str + ' --pattern ' + ENV['spec_module'] +'/**/*_spec.rb' if ENV['spec_module']
  execute_under_rails(str)
end

task 'parallel-spec' => RAILS_DEPENDENCIES do
  if ENV['GO_JOB_RUN_COUNT'].to_i == 0 || ENV['GO_JOB_RUN_INDEX'].to_i == 0
    puts 'Must define `GO_JOB_RUN_COUNT` and `GO_JOB_RUN_INDEX`'
    exit -1
  end


  def partition(options={})
    files                = options[:files]
    total_workers        = options[:total_workers]
    current_worker_index = options[:current_worker_index]

    return [] if files.nil? || files.empty?

    files = files.sort

    result = []

    # pick up things on a round-robin basis to distribute them evenly
    index  = current_worker_index - 1
    while index <= files.count do
      result << files[index]
      index += total_workers
    end
    result.compact!

    result
  end

  def find_all_tests(tests, options = {})
    (tests || []).map do |file_or_folder|
      if File.directory?(file_or_folder)
        files = Dir[File.join(file_or_folder, '**{,/*/**}/*')].uniq
        files.grep(options[:test_suffix])
      else
        file_or_folder
      end
    end.flatten.uniq
  end

  files = []

  cd RAILS_WORKING_DIR do
    files = find_all_tests(['spec'], :test_suffix => /_spec\.rb$/)
    files = partition(files: files, total_workers: ENV['GO_JOB_RUN_COUNT'].to_i, current_worker_index: ENV['GO_JOB_RUN_INDEX'].to_i)
  end


  running_tests!
  rm_rf SPEC_SERVER_DIR
  reports_dir = File.join(File.dirname(__FILE__), 'target', 'reports', 'spec')
  puts "reports directory: " + reports_dir
  ENV['REPORTS_DIR'] = reports_dir

  execute_under_rails([rspec_executable, *files].join(' '))
end

task "spec_module", [:spec_module_path] => RAILS_DEPENDENCIES do |t, args|
  raise "specify spec file to run. format: spec_file=<some_spec.rb> ./tools/bin/jruby -S rake --rakefile server/run_rspec_tests.rake spec_module" if args[:spec_module_path] == nil

  path = args[:spec_module_path].split('rails.new/spec/')[1]

  running_tests!
  rm_rf SPEC_SERVER_DIR
  reports_dir = File.join(File.dirname(__FILE__), 'target', 'reports', 'spec')
  puts "reports directory: " + reports_dir
  ENV['REPORTS_DIR'] = reports_dir

  str = "#{rspec_executable} spec --pattern #{path}/**/*_spec.rb"
  execute_under_rails(str)
end

task "spec_file", [:spec_file_path, :spec_file_line] => RAILS_DEPENDENCIES do |t, args|
  raise "specify spec file to run. format: spec_file=<some_spec.rb> ./tools/bin/jruby -S rake --rakefile server/run_rspec_tests.rake spec_file" if args[:spec_file_path] == nil

  running_tests!
  rm_rf SPEC_SERVER_DIR
  reports_dir = File.join(File.dirname(__FILE__), 'target', 'reports', 'spec')
  ENV['REPORTS_DIR'] = reports_dir

  str = "#{rspec_executable} #{args[:spec_file_path]}"
  str += " --line #{args[:spec_file_line]}" unless args[:spec_file_line] == nil
  execute_under_rails(str)
end

task "exec" => RAILS_DEPENDENCIES do
  not_running_tests!
  execute_under_rails ENV['command']
end
