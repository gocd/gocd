# GoCD Server Docker image

An ${distro.name()} based docker image for [GoCD server](https://www.gocd.org).

# Issues, feedback?

Please make sure to log them at https://github.com/gocd/gocd.

# Usage

Start the container with this:

```shell
docker run -d -p8153:8153 gocd/${imageName}:v${goVersion}
```

This will expose container ports 8153(http) onto your server.
You can now open `http://localhost:8153`

# Available configuration options

## Mounting volumes

The GoCD server will store all configuration, pipeline history database,
artifacts, plugins, and logs into `/godata`. If you'd like to provide secure
credentials like SSH private keys among other things, you can mount `/home/go`

```shell
docker run -v /path/to/godata:/godata -v /path/to/home-dir:/home/go gocd/${imageName}:v${goVersion}
```

> **Note:** Ensure that `/path/to/home-dir` and `/path/to/godata` is accessible by the `go` user in container (`go` user - uid `1000`).

## Installing plugins

All plugins can be installed under `/godata`.

### Installing plugins using an environment configuration

To install plugins, just add an ENV variable with the prefix `GOCD_PLUGIN_INSTALL_` and your name as `suffix`
and the value being the download URL. The plugin will only be downloaded if not yet present.

An example example would be `GOCD_PLUGIN_INSTALL_docker-elastic-agents=https://github.com/gocd-contrib/docker-elastic-agents/releases/download/v0.8.0/docker-elastic-agents-0.8.0.jar`:

```shell
docker run \
  -e GOCD_PLUGIN_INSTALL_docker-elastic-agents=https://github.com/gocd-contrib/docker-elastic-agents/releases/download/v0.8.0/docker-elastic-agents-0.8.0.jar \
  gocd/${imageName}:v${goVersion}
```

To install multiple plugins, add several `-e` arguments as such:

```shell
docker run \
  -e GOCD_PLUGIN_INSTALL_a-plugin=https://example.com/a-plugin.jar \
  -e GOCD_PLUGIN_INSTALL_b-plugin=https://example.com/b-plugin.jar \
  gocd/${imageName}:v${goVersion}
```

### Installing plugins using a custom entry-point script (see below)

```shell
mkdir -p /godata/plugins/external
curl --location --fail https://example.com/plugin.jar > /path/to/godata/plugins/external/plugin.jar
chown -R 1000 /godata/plugins/external
```

## Loading configuration from existing git repo
To load existing configuration from git repo, just add an ENV variable `CONFIG_GIT_REPO`.
Auth token may be used to access private repo. Branch `master` would be cloned by default.
To load another branch, define an ENV variable `CONFIG_GIT_BRANCH`.
If `/godata/config` already is git repo then CONFIG_GIT_REPO will be ignored.
Cloned repo **must** contain all files from `/godata/config` dir.

```shell
docker run \
  -e CONFIG_GIT_REPO=https://gocd_user:<password_or_auth_token>/config.git \
  -e CONFIG_GIT_BRANCH=branch_with_config \
  gocd/${imageName}:v${goVersion}
```
*Checkouted content would overwrite files in `/godata/config/`*.


## Running custom entrypoint scripts

To execute custom script(s) during the container boostrap, but **before** the GoCD server starts just add `-v /path/to/your/script.sh:/docker-entrypoint.d/your-script.sh` like so:

```shell
docker run -v /path/to/your/script.sh:/docker-entrypoint.d/your-script.sh ... gocd/${imageName}:v${goVersion}
```

If you have several scripts in a directory that you'd like to execute:

```shell
docker run -v /path/to/script-dir:/docker-entrypoint.d ... gocd/${imageName}:v${goVersion}
```

> **Note:** Ensure that your scripts are executable `chmod a+x` — you can add as many scripts as you like, `bash` is available on the container. If your script uses other scripting language (perl, python), please ensure that the scripting language is installed in the container.

## Installing addons

All addons can be installed under `/godata`.

```
mkdir -p /path/to/godata/addons
curl --location --fail https://example.com/addon.jar > /path/to/godata/addons/plugin.jar
chown -R 1000 /path/to/godata/addons
```

## Tweaking JVM options (memory, heap etc)

JVM options can be tweaked using the environment variable `GOCD_SERVER_JVM_OPTS`.

```shell
docker run -e GOCD_SERVER_JVM_OPTS="-Xmx4096mb -Dfoo=bar" gocd/${imageName}:v${goVersion}
```

# Under the hood

The GoCD server runs as the `go` user, the location of the various directories is:

| Directory           | Description                                                                      |
|---------------------|----------------------------------------------------------------------------------|
| `/godata/addons`    | the directory where GoCD addons are stored                                       |
| `/godata/artifacts` | the directory where GoCD artifacts are stored                                    |
| `/godata/config`    | the directory where the GoCD configuration is store                              |
| `/godata/db`        | the directory where the GoCD database and configuration change history is stored |
| `/godata/logs`      | the directory where GoCD logs will be written out to                             |
| `/godata/plugins`   | the directory containing GoCD plugins                                            |
| `/home/go`          | the home directory for the GoCD server                                           |

# Determine Server IP and Ports on Host

Once the GoCD server is up, we should be able to determine its ip address and the ports mapped onto the host by doing the following:
The IP address and ports of the GoCD server in a docker container are important to know as they will be used by the GoCD agents to connect to it.
If you have started the container with
```shell
docker run --name server -it -p8153:8153 gocd/${imageName}:v${goVersion}
```

Then, the below commands will determine to GoCD server IP, server port and ssl port
```shell
docker inspect --format='{{(index (index .NetworkSettings.IPAddress))}}' server
docker inspect --format='{{(index (index .NetworkSettings.Ports "8153/tcp") 0).HostPort}}' server
```

# Running GoCD Containers as Non Root

With release `v19.6.0`, GoCD containers will run as non-root user, by default. The Dockerized GoCD application will run with user `go` (uid: `1000`) and group `root` (gid: `0`) instead of running as user `root` (uid: `0`) and group `root` (gid: `0`). For more information, checkout [Running Dockerized GoCD Containers as Non Root](https://www.gocd.org/2019/06/25/GoCD-non-root-containers/) blog post.

# Troubleshooting

## The GoCD server does not come up

- Check if the docker container is running `docker ps -a`
- Check the STDOUT to see if there is any output that indicates failures `docker logs CONTAINER_ID`
- Check the server logs `docker exec -it CONTAINER_ID tail -f /godata/logs/go-server.log` (or check the log file in the volume mount, if you're using one)


# License

```plain
Copyright ${copyrightYear} ThoughtWorks, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

[0]: https://www.gocd.org/download/
