#!/bin/bash

. go-common-scripts.sh

[[ -f "/init.sh" ]] && /bin/bash /init.sh

show_msg "Starting Go Server ..."

/bin/cp -va /go-addons/. /var/lib/go-server/addons/
chown -R go:go /var/lib/go-server/addons/
/sbin/setuser go /etc/init.d/go-server start &

wait_for_go_server

# Setup auto registration for agents.
AGENT_KEY="${AGENT_KEY:-123456789abcdef}"
[[ -n "$AGENT_KEY" ]] && sed -i -e 's/agentAutoRegisterKey="[^"]*" *//' -e 's#\(<server\)\(.*artifactsdir.*\)#\1 agentAutoRegisterKey="'$AGENT_KEY'"\2#' /etc/go/cruise-config.xml

cat <<EOF


${COLOR_START}----------------------------------------------------------${COLOR_END}
Go Server has started on port 8153 inside this container ($HOSTNAME)!

To be able to connect to it in a browser, you need to find the port which has been mapped to port 8153 for this container.

If you're using docker on a Linux box, you can do this:
${COLOR_START}echo http://localhost:\$(docker inspect --format='{{(index (index .NetworkSettings.Ports "8153/tcp") 0).HostPort}}' $HOSTNAME)${COLOR_END}

If you're using docker through boot2docker, on a Mac, do this:
${COLOR_START}echo http://\$(boot2docker ip):\$(docker inspect --format='{{(index (index .NetworkSettings.Ports "8153/tcp") 0).HostPort}}' $HOSTNAME)${COLOR_END}

That command will output the URL through which you should be able to access the Go Server.
${COLOR_START}----------------------------------------------------------${COLOR_END}


EOF

wait_for_msg_time "$(date): This message will stay for $MSG_TIME seconds and then the Go Server logs will be shown. Run docker with the option: '-e MSG_TIME=0' to disable this wait time."

tail --follow=name /var/log/go-server/go-server.log | sed -u -e 's/.*/LOG: &/'
