#!/usr/bin/env bash

yell() { echo "$0: $*" >&2; }
die() { yell "$*"; exit 111; }
try() { echo "$ $@" 1>&2; "$@" || die "cannot $*"; }

GOCD_PLUGIN_EXTERNAL_HOME="/godata/plugins/external"

if [ ! -e "${GOCD_PLUGIN_EXTERNAL_HOME}" ]; then
  try mkdir -p "${GOCD_PLUGIN_EXTERNAL_HOME}"
fi

(while IFS='=' read -r name value ; do
  if [[ "${name}" == "GOCD_PLUGIN_INSTALL_"* ]]; then
    plugin_url="${value}"

    # remove the `GOCD_PLUGIN_INSTALL_` prefix and grab the plugin name
    plugin_name="${name//GOCD_PLUGIN_INSTALL_/}"
    # the file where the plugin jar will be written out to
    plugin_dest="${GOCD_PLUGIN_EXTERNAL_HOME}/${plugin_name}.jar"

    if [ -e "${plugin_dest}" ]; then
      yell "Overwriting the plugin '$plugin_dest' with new jar."
      try rm "${plugin_dest}"
    fi
    try curl --silent --location --fail --retry 3 "${plugin_url}" --output "${plugin_dest}"
  fi
done) < <(env)
