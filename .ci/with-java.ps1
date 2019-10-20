$mod=($env:GO_PIPELINE_COUNTER % 3)

function use_jdk() {
    jabba use "$($args[0])"
}

if ($mod -eq "0") {
    use_jdk "openjdk@1.11"
} elseif ($mod -eq "1") {
    use_jdk "openjdk@1.12"
} else {
    use_jdk "openjdk@1.13.0"
}

$command = "$($args[0])"

# Write-Host "Executing command: ${command}"
# Write-Host "With args: ${commandArgs}"

if ($args.Length -eq 0) {
    Write-Host "Exiting because no command line args specified"
    exit 1
} elseif ($args.Length -eq 1) {
    $process = Start-Process -Wait -PassThru -NoNewWindow -FilePath "${command}"
} else {
    $commandArgs = $args[1..($args.Length-1)]
    $process = Start-Process -Wait -PassThru -NoNewWindow -FilePath "${command}" -ArgumentList $commandArgs
}

exit $process.ExitCode
