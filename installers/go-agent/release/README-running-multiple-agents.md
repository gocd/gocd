# Run multiple agents on the same host

It is possible to run multiple GoCD agents on the same host. 

Each agent needs to be configured to run in its own separate directory so that each agent can build independently of the other, and also keep their config files separate. Follow the instructions below to configure multiple GoCD agents on the same host.

## Linux package installed using apt/dpkg/rpm/yum

  - First, verify if you installed the package using this method.
  
    On debian/ubuntu:
    
      ```bash
      $ dpkg -s go-agent | grep Status
      Status: install ok installed
      ```

    On CentOS/RedHat/Fedora and other RPM based distros:
  
      ```bash
      $ rpm -qa | grep go-agent
      go-agent-{VERSION}.noarch
      ```

  - If the package is not installed, make sure to download and install the GoCD agent from [https://gocd.org/download](https://gocd.org/download).
  - If the package is installed, run the following script. Be sure to substitute the environment variables `AGENT_COUNT`, `RUN_AS_SERVICE` and `START_SERVICE_NOW` as appropriate.
  
      ```bash
      # spin up 5 additional GoCD agents.
      AGENT_COUNT=5

      # if you want to run the GoCD agent as a service
      RUN_AS_SERVICE=true

      # if you want to start the service immediately
      # only works if `RUN_AS_SERVICE` is true
      START_SERVICE_NOW=true

      for AGENT_INDEX in $(seq 1 "${AGENT_COUNT}")
      do
        AGENT_ID="go-agent-${AGENT_INDEX}"

        # create the directories for agent data and binaries
        mkdir -p /usr/share/${AGENT_ID} /var/{lib,log}/${AGENT_ID} /var/lib/${AGENT_ID}/run

        # copy over all config and shell scripts and wrapper configs
        cp -arf /usr/share/go-agent/{bin,wrapper-config} /usr/share/${AGENT_ID}

        # symlink the wrapper binaries
        ln -sf /usr/share/go-agent/wrapper /usr/share/${AGENT_ID}

        # change ownership and mode, so that the `go` user, and only that user
        # can write to these directories
        chown -R go:go /var/{lib,log}/${AGENT_ID}
        chmod -R 0750 /var/{lib,log}/${AGENT_ID}
        chmod 0755 /usr/share/${AGENT_ID}

        # tweak the scripts and configs to use the correct directories
        sed -i -e "s@go-agent@${AGENT_ID}@g" \
            /usr/share/${AGENT_ID}/bin/go-agent
        sed -i -e "s@=go-agent\$@=${AGENT_ID}@g" \
               -e "s@/var/lib/go-agent@/var/lib/${AGENT_ID}@g" \
               -e "s@/var/log/go-agent@/var/log/${AGENT_ID}@g" \
               -e "s@../wrapper-config/wrapper-properties.conf@/usr/share/${AGENT_ID}/wrapper-config/wrapper-properties.conf@g" \
            /usr/share/${AGENT_ID}/wrapper-config/wrapper.conf
        sed -i -e "s@/var/lib/go-agent@/var/lib/${AGENT_ID}@g" \
            /usr/share/${AGENT_ID}/wrapper-config/wrapper-properties.conf

        if [ "${RUN_AS_SERVICE}" == "true" ]; then
          if [ "${START_SERVICE_NOW}" == "true" ]; then
            /usr/share/${AGENT_ID}/bin/go-agent installstart
          else
            /usr/share/${AGENT_ID}/bin/go-agent install
          fi
        fi
      done

      ```
 
## macOS installers 

  - To run multiple macOS installers, run the following script. Be sure to substitute the environment variables `AGENT_COUNT`, `RUN_AS_SERVICE` and `START_SERVICE_NOW` as appropriate.

    ```bash
    # spin up 2 additional GoCD agents.
    AGENT_COUNT=2
    
    # path to existing agent dir
    AGENT_PATH="/path/to/go-agent-X.Y.Z"
    
    for AGENT_INDEX in $(seq 1 "${AGENT_COUNT}")
    do
      AGENT_ID="go-agent-${AGENT_INDEX}"

      /bin/mkdir -p "${AGENT_ID}" "${AGENT_ID}/run"
    
      # use the same jre and GoCD jars from main agent
      /bin/ln -sf "${AGENT_PATH}/jre" "${AGENT_ID}/jre"
      /bin/ln -sf "${AGENT_PATH}/lib" "${AGENT_ID}/lib"
    
      # copy over the wrapper shell scripts, because they seem to resolve symlinks
      /bin/cp -rf "${AGENT_PATH}/bin" "${AGENT_ID}"
      /bin/cp -rf "${AGENT_PATH}/wrapper" "${AGENT_ID}"
      /bin/cp -rf "${AGENT_PATH}/wrapper-config" "${AGENT_ID}"
    done
    ```

## Package installed on other OSes using the generic ".zip" installer

  - To run multiple agent instances, extract a copy of the agent zip in a new location and execute it.
  - If you would like to run these agent processes as windows or Linux/Unix services that start when the machine boots up:

    On windows:

      - Edit the `wrapper-config/wrapper.conf` file to set the properties `wrapper.name`, `wrapper.displayname`, `wrapper.console.title` and `wrapper.description` and set different values for each agent process

    On Linux/Unix:

      - Edit the `wrapper-config/wrapper.conf` file to set the properties `wrapper.name`, `wrapper.displayname`, `wrapper.console.title` and `wrapper.description` and set different values for each agent process
      - Edit the file `bin/go-agent` and replace all occurrences of `go-agent` with a different value for each agent process. Set the envionment variable `RUN_AS_USER=go` to ensure that the service runs as the `go` user.
