### Run multiple agents on the same host

## Setup new agent

- To create a second agent on the same host, run this as root:

  ```
  cp /etc/init.d/go-agent /etc/init.d/go-agent-1
  sed -i 's/# Provides: go-agent$/# Provides: go-agent-1/g' /etc/init.d/go-agent-1
  ln -s /usr/share/go-agent /usr/share/go-agent-1
  cp /etc/default/go-agent /etc/default/go-agent-1
  mkdir /var/{lib,log}/go-agent-1
  chown go:go /var/{lib,log}/go-agent-1
  ```
- To enable starting the go-agent service during system boot:
  - on Debian:
  ```
  insserv go-agent-1
  ```
  - on Ubuntu:
  ```
  update-rc.d go-agent-1 defaults
  ```
  - on Centos and Redhat:
  ```
  chkconfig go-agent-1 on
  ```
