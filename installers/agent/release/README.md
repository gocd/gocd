### Run multiple agents on the same host

## Setup new agent
```
	ln -s /etc/init.d/go-agent /etc/init.d/go-agent-1
	ln -s /usr/share/go-agent /usr/share/go-agent-1
	cp /etc/default/go-agent /etc/default/go-agent-1
	mkdir /var/lib/go-agent-1
	chown go:go /var/lib/go-agent-1
```
