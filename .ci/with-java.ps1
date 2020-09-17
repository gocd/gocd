$jdk = if ($env:BUILD_ON_JDK) { $env:BUILD_ON_JDK } else { "14" }

function use_jdk() {
    jabba use "$($args[0])"
}

use_jdk "openjdk@1.$($jdk)"

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
