##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##########################GO-LICENSE-END##################################

DB_PORT = 9002
DB_URL = "jdbc:h2:tcp://localhost:#{DB_PORT}/cruise"
DRIVER='org.h2.Driver'
USER='sa'
PASSWORD=''


def sql(src, onerror = "abort")
  ant('exec-sql') do |ant|
    ant.sql :driver => DRIVER,
            :url => DB_URL,
            :userid => USER,
            :password => PASSWORD,
            :src => src,
            :onerror => onerror do
      ant.classpath do
        jars_at('h2').each do |j|
          ant.fileset :file => j
        end
      end
    end
  end
end

def db_server(*args)
  args.unshift 'org.h2.tools.Server'
  args.push({:classpath => jars_at('h2')})
  Java::Commands.java *args
end

def generate_delta_script
  generated = _("migrate/generated")
  rm_rf generated
  mkdir generated

  ant('dbdeploy') do |dbd|
    dbd.taskdef :name => 'dbdeploy',
                :classname => 'net.sf.dbdeploy.AntTarget',
                :classpath => jars_at('dbdeploy', 'h2').join(File::PATH_SEPARATOR)

    dbd.dbdeploy( :driver => DRIVER,
                  :url => DB_URL,
                  :userid =>USER,
                  :password =>PASSWORD,
                  :dir => _("migrate/h2deltas/"),
                  :outputfile => File.join(generated, "db-deltas-hsql.sql"),
                  :dbms =>"hsql",
                  :deltaset =>"DDL",
                  :undoOutputfile => File.join(generated, "db-deltas-hsql-UNDO.sql"))
  end
end

def start_db
  stop_db if db_running?

  fork do
    db_server '-tcp','-baseDir', _('h2db'), '-tcpPort', DB_PORT.to_s
  end

  20.times do
    break if db_running?
    sleep 0.5
  end

  raise 'Db Server not started' if !db_running?
end

def db_running?
  port_open?('127.0.0.1', DB_PORT)
end

def stop_db
  begin
    sql _('migrate/schema/shutdown.sql'), "continue"
    db_server '-tcpShutdown', "tcp://localhost:#{DB_PORT}"
  rescue Exception
    #ignore
  end
end

def build_db_baseline
  sql _('migrate/schema/create_schema.sql')
  sql _('migrate/schema/createSchemaVersionTable.hsql.sql')
end

def execute_delta_script
  sql _('migrate/generated/db-deltas-hsql.sql')
end
