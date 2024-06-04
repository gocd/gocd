#!/bin/bash

set -eu
trap 'echo "ERROR $?"' ERR
# tag our output and send it to gcloud-logging, too:
gcloud_logging=/proc/1/fd/2  # stderr of our root process
exec >& >(logger -st "[$(basename $0)]" | tee "$gcloud_logging")
HERE="$(dirname "$0")"

. /docker-entrypoint.d/.tfvars
PATH="$PATH:$HOME/prefix/google-cloud-sdk/bin"
dest="$tf_backup_bucket/gocd-artifact-archive"

main() {
    set -x

    : "Collecting the following artifacts belonging to pipeline runs older than 30 days:"
    # rather than -exec zip archive.zip {} + which can run into exec length limits
    # we assume no linebreaks in file names and just go with piping -print
    # maxdepth 2 and -type d is because there's a massive amount of files if you only do -type f:
    # /godata/artifacts/pipelines/example/9388/pipeline-complete/1/pipeline-complete/cruise-output/console.log
    # /godata/artifacts/pipelines/example/9388/deploy-primary/1/deploy/cruise-output/console.log
    # and just having /godata/artifacts/pipelines/example/9388 makes things go a lot faster
    dt="$(date -u +'%Y-%m-%dT%H:%M:%SZ')"
    /bin/busybox find /godata/artifacts/pipelines/ -maxdepth 2 -type d -mtime +30 -print > dirs.txt
    /bin/busybox tar c -z -f "${dt}.tgz" -T dirs.txt

    # gsutil is deprecated: https://cloud.google.com/storage/docs/gsutil#should-you-use
    : "Uploading to GCS bucket ${dest}."
    gcloud storage cp "${dt}.tgz" "${dest}/${dt}.tgz" && rm "${dt}.tgz"

    : "Deleting from disk:"
    xargs rm -rv < dirs.txt
}

main "$@"
