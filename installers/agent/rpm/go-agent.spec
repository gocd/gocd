%define __spec_install_pre %{___build_pre}

# use md5 for digest algorithm because it works on most platforms
%define _binary_filedigest_algorithm 1
%define _build_binary_file_digest_algo 1

# use gzip for compression because it works on most platforms
%define _binary_payload w9.gzdio

Name: go-agent
Version: @VERSION@
Release: @RELEASE@
Summary: Go Agent Component
License: Apache License Version 2.0
Group: Development/Build Tools
BuildRoot: @ROOT@
Obsoletes: go-agent
Url: http://www.go.cd
%description
Next generation continuous integration and release management server from ThoughtWorks

%prep

%build

%install

%clean

%pre
#!/bin/bash
APP_NAME="'Go Agent'"

@rpm_pre@
@pre@

if [ "$1" = "1" ]; then #fresh install
    warn_about_existing_user_or_group_go
    (rpm -q cruise-agent | grep -qF 'is not installed') || if [ true ]; then
        if [ -x /etc/init.d/cruise-agent ]; then
            (/etc/init.d/cruise-agent stop || true)
        fi
        rename_cruise_user_and_group_to_go
    fi
    create_group_go_if_none
    create_user_go_if_none
    ensure_go_user_belongs_to_go_group
fi

if [ "$1" = "2" ] ; then  # new package upgrade
    /etc/init.d/go-agent stop
fi

%post
#!/bin/bash

@rpm_post@
@post@

set_go_agents_defaults_path

if [ -f /etc/default/go-agent.rpmsave ]; then
    mv /etc/default/go-agent /etc/default/go-agent.rpmnew
    mv /etc/default/go-agent.rpmsave /etc/default/go-agent
fi

if [ "$1" = "1" ] ; then  # first install
    (rpm -q cruise-agent | grep -qF 'is not installed') || if [ true ]; then
        link_and_fix_agent_files_to_upgrade_cruise_agent_to_go
        remove_from_init cruise-agent
        fix_agent_log4j_properties
    fi

    fix_agent_java_home_declaration_for_rpm

    fix_agent_defaults_ownership
    create_necessary_agent_directories

    chkconfig --add go-agent

    echo "Installation of Go Agent completed."

    print_agent_configuration_suggestions
fi

go_owned_toplevel /var/lib/go-agent

%preun

if [ "$1" = "0" ] ; then # last uninstall
    /etc/init.d/go-agent stop
    chkconfig --del go-agent
fi

%postun

if [ "$1" = "0" ] ; then # last uninstall
    rm -fr /var/run/go-agent
    rm -fr /usr/share/go-agent
    rm -fr /var/lib/go-agent
    rm -fr /var/log/go-agent
fi

%files
/usr/share/go-agent/
%doc /usr/share/doc/go-agent/changelog.gz
%doc /usr/share/doc/go-agent/copyright
/etc/init.d/go-agent
%config /etc/default/go-agent
%config /var/lib/go-agent/log4j.properties
