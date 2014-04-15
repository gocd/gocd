#*************************GO-LICENSE-START********************************
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
#*************************GO-LICENSE-END**********************************

function has_line_begining_in {
    grep -q "^$1" "$2"
}

function does_not_have_line_begining_in {
    ! has_line_begining_in "$1" "$2"
}

function go_owned {
    if [ -e "$1" ]; then
        chown -RL go:go "$1"
        if [ -L "$1" ]; then
            chown -h go:go "$1"
        fi
    fi
}

function go_owned_toplevel {
    for var in "$@"
    do
        chown -L go:go "$var"
        if [ -L "$var" ]; then
            chown -h go:go "$var"
        fi
    done
}

function create_if_does_not_exist {
    [ -d "$1" ] || mkdir "$1"
    go_owned "$1"
}

function link {
    if [ -e "$2" ]; then
        backup_name="$2.go.backup"
        echo "Found $2(which needs to be linked to $1), backing it up as $backup_name"
        mv "$2" $backup_name || echo "Failed to backup $2, please fix the symlink of '$2 to $1' manually"
    fi
    ln -s "$1" "$2" || echo "$2 could not be symlinked to $1, please fix manually"
}

function link_dir_if_exists {
    if [ -d "$1" ]; then
        link "$1" "$2"
    fi
}

function mv_dir_if_exists {
    if [ -d "$1" ]; then
        if [ -d "$2" ]; then
            rm -rf "$2"
        fi
        #preserves symlinks and ensures that $1 is not moved under $2
        mv "$1" "$2"
    fi
}

function set_go_agents_defaults_path {
    GO_AGENT_DEFAULTS=/etc/default/go-agent
}

function link_and_fix_agent_files_to_upgrade_cruise_agent_to_go {
    CRUISE_AGENT_DEFAULTS=/etc/default/cruise-agent

    if [ -e $CRUISE_AGENT_DEFAULTS ]; then
        ((has_line_begining_in CRUISE_SERVER= $CRUISE_AGENT_DEFAULTS) && (does_not_have_line_begining_in GO_SERVER= $CRUISE_AGENT_DEFAULTS) && (echo 'GO_SERVER=$CRUISE_SERVER' >> $CRUISE_AGENT_DEFAULTS)) || true
        ((has_line_begining_in CRUISE_SERVER_PORT= $CRUISE_AGENT_DEFAULTS) && (does_not_have_line_begining_in GO_SERVER_PORT= $CRUISE_AGENT_DEFAULTS) && (echo 'GO_SERVER_PORT=$CRUISE_SERVER_PORT' >> $CRUISE_AGENT_DEFAULTS)) || true
        sed -i s@AGENT_WORK_DIR=/var/lib/cruise-agent@AGENT_WORK_DIR=/var/lib/go-agent@g $CRUISE_AGENT_DEFAULTS
        mv $CRUISE_AGENT_DEFAULTS $GO_AGENT_DEFAULTS
        go_owned $GO_AGENT_DEFAULTS

    fi

    mv_dir_if_exists /var/log/cruise-agent /var/log/go-agent
    mv_dir_if_exists /var/lib/cruise-agent /var/lib/go-agent
}

function fix_agent_defaults_ownership {
    go_owned $GO_AGENT_DEFAULTS || ( echo "user 'go' and group 'go' must exist" && exit 1 )
}

function fix_java_home_declaration {
    grep -q -E '^((\s)*export(\s)+JAVA_HOME=)|^((\s)*JAVA_HOME=)' $1 || JAVA_HOME_SET=false
    if [ "$JAVA_HOME_SET" = false ]; then
        determine_java_home_path
        if [ "$JAVA_HOME_PATH" != "" ]; then
            echo "JAVA_HOME=$JAVA_HOME_PATH" >> $1
            echo "export JAVA_HOME" >> $1
        fi
    fi
}

function determine_java_home_path {
    JAVA_HOME_PATH=$JAVA_HOME
    if [ -z "$JAVA_HOME_PATH" ]; then
        path_to_java="`which java`" || echo "Path to java not found in JAVA_HOME or PATH."
        if [ -n "$path_to_java" ]; then
            JAVA_HOME_PATH=`readlink -f "$path_to_java" | sed "s/\/bin\/java//"`
            echo "Found Java $JAVA_HOME_PATH in PATH, using it."
        fi
    else
        echo "Found Java $JAVA_HOME_PATH in environment variable JAVA_HOME, using it."
    fi
}

function fix_agent_java_home_declaration {
    fix_java_home_declaration $GO_AGENT_DEFAULTS
}

function create_necessary_agent_directories {
    create_if_does_not_exist /var/log/go-agent

    create_if_does_not_exist /var/run/go-agent

    create_if_does_not_exist /var/lib/go-agent
}

function print_agent_configuration_suggestions {
    echo "Now please edit $GO_AGENT_DEFAULTS and set GO_SERVER to the IP address of your Go Server."
    echo "Once that is done start the Go Agent with '/etc/init.d/go-agent start'"
}

function set_go_server_defaults_path {
    GO_SERVER_DEFAULTS=/etc/default/go-server
}

function link_and_fix_server_files_to_upgrade_cruise_server_to_go {
    CRUISE_SERVER_DEFAULTS=/etc/default/cruise-server

    if [ -e $CRUISE_SERVER_DEFAULTS ]; then
        link $CRUISE_SERVER_DEFAULTS $GO_SERVER_DEFAULTS
        ((has_line_begining_in CRUISE_SERVER_PORT= $CRUISE_SERVER_DEFAULTS) && (does_not_have_line_begining_in GO_SERVER_PORT= $CRUISE_SERVER_DEFAULTS) && (echo 'GO_SERVER_PORT=$CRUISE_SERVER_PORT' >> $CRUISE_SERVER_DEFAULTS)) || true
        ((has_line_begining_in CRUISE_SERVER_SSL_PORT= $CRUISE_SERVER_DEFAULTS) && (does_not_have_line_begining_in GO_SERVER_SSL_PORT= $CRUISE_SERVER_DEFAULTS) && (echo 'GO_SERVER_SSL_PORT=$CRUISE_SERVER_SSL_PORT' >> $CRUISE_SERVER_DEFAULTS)) || true
        ((has_line_begining_in CRUISE_CONFIG_DIR= $CRUISE_SERVER_DEFAULTS) && (does_not_have_line_begining_in GO_CONFIG_DIR= $CRUISE_SERVER_DEFAULTS) && (echo 'GO_CONFIG_DIR=$CRUISE_CONFIG_DIR' >> $CRUISE_SERVER_DEFAULTS)) || true
        ((has_line_begining_in CRUISE_SERVER_SYSTEM_PROPERTIES= $CRUISE_SERVER_DEFAULTS) && (does_not_have_line_begining_in GO_SERVER_SYSTEM_PROPERTIES= $CRUISE_SERVER_DEFAULTS) && (echo 'GO_SERVER_SYSTEM_PROPERTIES=$CRUISE_SERVER_SYSTEM_PROPERTIES' >> $CRUISE_SERVER_DEFAULTS)) || true
        ((has_line_begining_in CRUISE_WORK_DIR= $CRUISE_SERVER_DEFAULTS) && (does_not_have_line_begining_in GO_WORK_DIR= $CRUISE_SERVER_DEFAULTS) && (echo 'GO_WORK_DIR=$CRUISE_WORK_DIR' >> $CRUISE_SERVER_DEFAULTS)) || true
    fi

    link_dir_if_exists /etc/cruise /etc/go
    link_dir_if_exists /var/log/cruise-server /var/log/go-server
    link_dir_if_exists /var/lib/cruise-server /var/lib/go-server
}

function fix_server_defaults_ownership {
    go_owned $GO_SERVER_DEFAULTS || ( echo "user 'go' and group 'go' must exist" && exit 1 )
}

function fix_server_java_home_declaration {
    fix_java_home_declaration $GO_SERVER_DEFAULTS
}

function fix_go_server_lib_ownership {
    go_owned_toplevel /var/lib/go-server
    go_owned_toplevel /var/lib/go-server/**
    go_owned /var/lib/go-server/db/config.git
    go_owned /var/lib/go-server/db/deltas
    go_owned /var/lib/go-server/db/h2deltas
    go_owned /var/lib/go-server/db/h2db
    go_owned /var/lib/go-server/db/command_repository/default
}

function create_necessary_server_directories_and_fix_ownership {
    go_owned /etc/go

    create_if_does_not_exist /var/log/go-server

    create_if_does_not_exist /var/run/go-server
}

function fix_log4j_properties_file_name {
    if [ -e $1 ]; then
        OLD_FILE=$1.`date +%s`
        mv $1 $OLD_FILE
        cat $OLD_FILE | sed -e s#File=$2#File=$3#g > $1
    fi
}

function fix_server_log4j_properties {
    fix_log4j_properties_file_name /etc/go/log4j.properties /var/log/cruise-server/cruise- /var/log/go-server/go-
}

function fix_agent_log4j_properties {
    fix_log4j_properties_file_name /var/lib/go-agent/config/log4j.properties cruise- go-
}
