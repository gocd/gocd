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

    : "Collecting the following artifact files older than 30 days:"
    # rather than -exec zip archive.zip {} + which can run into exec length limits
    # we assume no linebreaks in file names and just go with piping -print
    dt="$(date -u +'%Y-%m-%dT%H:%M:%SZ')"
    find /godata/artifacts/pipelines/ -type f -mtime +30 -print > files.txt
    zip "${dt}.zip" -@ < files.txt

    # gsutil is deprecated: https://cloud.google.com/storage/docs/gsutil#should-you-use
    : "Uploading to GCS bucket ${dest}."
    gcloud storage cp "${dt}.zip" "$dest" && rm "${dt}.zip"

    : "Deleting from disk:"
    xargs rm -v < files.txt
    find /godata/artifacts/pipelines/ -type d -empty -delete
}

main "$@"
