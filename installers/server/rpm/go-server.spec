%define __spec_install_pre %{___build_pre}

# use md5 for digest algorithm because it works on most platforms
%define _binary_filedigest_algorithm 1
%define _build_binary_file_digest_algo 1

# use gzip for compression because it works on most platforms
%define _binary_payload w9.gzdio

Name: go-server
Version: @VERSION@
Release: @RELEASE@
Summary: Go Server Component
License: Apache License Version 2.0
Group: Development/Build Tools
BuildRoot: @ROOT@
Obsoletes: go-server
Url: http://www.go.cd
%description
Next generation continuous integration and release management server from ThoughtWorks

%prep

%build

%install

%clean

%pre
#!/bin/bash
APP_NAME="'Go Server'"

@rpm_pre@
@pre@

if [ "$1" = "1" ]; then #fresh install
    warn_about_existing_user_or_group_go
    (rpm -q cruise-server | grep -qF 'is not installed') || if [ true ]; then
        if [ -x /etc/init.d/cruise-server ]; then
            (/etc/init.d/cruise-server stop || true)
        fi
        rename_cruise_user_and_group_to_go
    fi
    create_group_go_if_none
    create_user_go_if_none
    ensure_go_user_belongs_to_go_group
fi

if [ "$1" = "2" ] ; then  # new package upgrade
    /etc/init.d/go-server stop
fi

%post
#!/bin/bash

@rpm_post@
@post@

set_go_server_defaults_path

if [ -f /etc/default/go-server.rpmsave ]; then
    mv /etc/default/go-server /etc/default/go-server.rpmnew
    mv /etc/default/go-server.rpmsave /etc/default/go-server
fi

if [ "$1" = "1" ] ; then  # first install
    ((rpm -q cruise-server | grep -qF 'is not installed') && (chmod -R go-rwx /etc/go)) || if [ true ]; then
        link_and_fix_server_files_to_upgrade_cruise_server_to_go
        remove_from_init cruise-server
        fix_server_log4j_properties
    fi

    fix_server_defaults_ownership
    create_necessary_server_directories_and_fix_ownership

    chkconfig --add go-server
fi

fix_server_java_home_declaration_for_rpm
fix_go_server_lib_ownership
go_owned /etc/go

%preun
if [ "$1" = "0" ] ; then # this means we are in uninstall
    /etc/init.d/go-server stop
    chkconfig --del go-server
fi

%postun
if [ "$1" = "0" ] ; then # this means we are in uninstall
    rm -fr /usr/share/go-server
    rm -fr /var/run/go-server
    rm -fr /var/log/go-server
fi

#posttrans only support rpm 4.4 and later, scripts in this block will be executed when install or upgrade
%posttrans
if [ -f /etc/go/cruise-config.xml.rpmsave ]; then
    if [ -f /etc/go/cruise-config.xml ]; then
        echo 'Warning: Cruise found both /etc/go/cruise-config.xml.rpmsave and /etc/go/cruise-config.xml.'
        echo 'Using /etc/go/cruise-config.xml'
    else
        mv /etc/go/cruise-config.xml.rpmsave /etc/go/cruise-config.xml
    fi
fi

#this is for upgrade hsqldb (cruise 1.0), we need to change the cruise.script.rpmsave back to cruise.script
if [ -f /var/lib/go-server/db/hsqldb/cruise.script.rpmsave ]; then
    if [ -f /var/lib/go-server/db/hsqldb/cruise.script ]; then
        echo 'Warning: Go found both /var/lib/go-server/db/hsqldb/cruise.script.rpmsave and /var/lib/go-server/db/hsqldb/cruise.script.'
        echo 'Using /var/lib/go-server/db/hsqldb/cruise.script to upgrade'
    else
        mv /var/lib/go-server/db/hsqldb/cruise.script.rpmsave /var/lib/go-server/db/hsqldb/cruise.script
    fi
fi

echo "Installation of Go Server completed."

if [ "$DO_NOT_START_SERVICE" = "Y" ] ; then
    echo "Installation is not starting the go-server service as DO_NOT_START_SERVICE is set to $DO_NOT_START_SERVICE. Please start the service manually by executing '/etc/init.d/go-server start'"
else
    /etc/init.d/go-server start
fi


%files
/usr/share/go-server/
/var/lib/go-server/
%doc /usr/share/doc/go-server/changelog.gz
%doc /usr/share/doc/go-server/copyright
/etc/init.d/go-server
%config(noreplace) /etc/default/go-server
%dir /etc/go
/etc/go/log4j.properties
%dir /var/lib/go-server
