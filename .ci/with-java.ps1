$mod=($env:GO_PIPELINE_COUNTER % 4)

function use_jdk() {
    jabba install "$($args[0])=$($args[1])"
    jabba use "$($args[0])"
}

if ($mod -eq "0") {
    Write-Host "Using system JVM"
} elseif ($mod -eq "1") {
    use_jdk "oracle@1.9.0-4" "exe+https://nexus.gocd.io/repository/s3-mirrors/local/jdk/jdk-9.0.4_windows-x64_bin.exe"
} elseif ($mod -eq "2") {
    use_jdk "oracle@1.10.0-2" "exe+https://nexus.gocd.io/repository/s3-mirrors/local/jdk/jdk-10.0.2_windows-x64_bin.exe"
} elseif ($mod -eq "3") {
    use_jdk "oracle@1.11.0-28" "exe+https://nexus.gocd.io/repository/s3-mirrors/local/jdk/jdk-11-28_windows-x64_bin.exe"
}

cmd /c "$args"
