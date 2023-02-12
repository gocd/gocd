# Security Policy

## Supported Versions

The GoCD community only actively maintains and fixes security issues on top of the most recent released version.

Since breaking changes are rare, and generally sign-posted well in advance, we encourage users to stay on a recent or current version to allow for upgrade as easily as possible in the event of a security defect.

Having said this, wherever possible we will try and provide suggested mitigations or workarounds for older versions.

## Reporting a Vulnerability

Please report any issues to https://hackerone.com/gocd according to the listed policy.

## How do I know if I am using a release with known vulnerabilities?

In more recent years, an effort has been made to publish and request CVEs for responsibly disclosed & fixed issues to increase transparency and help users assess risk of running older versions.

While many are available as [GitHub Security Advisories](https://github.com/gocd/gocd/security/advisories), you can generally use the [NIST NVD database query tools](https://nvd.nist.gov/vuln/search?results_type=overview&query=cpe%3A2.3%3Aa%3Athoughtworks%3Agocd%3A22.3.0%3A*%3A*%3A*%3A*%3A*%3A*%3A*&search_type=all&form_type=Basic&isCpeNameSearch=true) to search for those affecting your specific version by replacing the version `22.3.0` with your own  and clicking "_Search_".

Note that this unlikely to be a complete listing of _all_ reported, responsibly disclosed and fixed issues. If there is a _publicly disclosed_ historical issue that is missing, please [raise an issue](https://github.com/gocd/gocd/issues/new) to let us know, and we will endeavour to document it properly.
