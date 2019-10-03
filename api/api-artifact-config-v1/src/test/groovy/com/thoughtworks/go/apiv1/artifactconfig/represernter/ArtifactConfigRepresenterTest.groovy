package com.thoughtworks.go.apiv1.artifactconfig.represernter

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.config.ArtifactConfig
import com.thoughtworks.go.config.ArtifactDirectory
import com.thoughtworks.go.config.PurgeSettings
import com.thoughtworks.go.config.PurgeStart
import com.thoughtworks.go.config.PurgeUpto
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.assertj.core.api.Assertions.assertThat

class ArtifactConfigRepresenterTest {
  @Test
  void 'renders artifacts config'() {
    ArtifactConfig artifactConfig = new ArtifactConfig()
    artifactConfig.setArtifactsDir(new ArtifactDirectory("some-directory"))
    PurgeSettings purgeSettings = new PurgeSettings()
    purgeSettings.setPurgeStart(new PurgeStart(50.9))
    purgeSettings.setPurgeUpto(new PurgeUpto(101.23))
    artifactConfig.setPurgeSettings(purgeSettings)
    def json = toObjectString({
      ArtifactConfigRepresenter.toJSON(it, artifactConfig)
    })

    def expectedJSON = [
      "_links"        : [
        "self": [
          "href": "http://test.host/go/api/admin/config/server/artifact_config"
        ],
        "doc" : [
          "href": apiDocsUrl("#artifact_config")
        ]
      ],
      "artifacts_dir" : "some-directory",
      "purge_settings": [
        "purge_start_disk_space": 50.9,
        "purge_upto_disk_space" : 101.23
      ]
    ]

    assertThatJson(json).isEqualTo(expectedJSON)
  }

  @Test
  void 'renders artifacts config if purge start and purge upto settings are not set'() {
    ArtifactConfig artifactConfig = new ArtifactConfig()
    artifactConfig.setArtifactsDir(new ArtifactDirectory("some-directory"))
    def json = toObjectString({
      ArtifactConfigRepresenter.toJSON(it, artifactConfig)
    })

    def expectedJSON = [
      "_links"       : [
        "self": [
          "href": "http://test.host/go/api/admin/config/server/artifact_config"
        ],
        "doc" : [
          "href": apiDocsUrl("#artifact_config")
        ]
      ],
      "artifacts_dir": "some-directory"
    ]

    assertThatJson(json).isEqualTo(expectedJSON)
  }

  @Test
  void 'load artifacts config from json'() {
    def artifactsJSON = [
      "artifacts_dir" : "some-directory",
      "purge_settings": [
        "purge_start_disk_space": 50.9,
        "purge_upto_disk_space" : 101.23
      ]
    ]
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(artifactsJSON)
    def actualArtifactsConfig = ArtifactConfigRepresenter.fromJSON(jsonReader)
    assertThat(actualArtifactsConfig.getArtifactsDir(), is(artifactsJSON.artifacts_dir))
    assertThat(actualArtifactsConfig.getPurgeSettings().purgeStart.purgeStartDiskSpace, is(artifactsJSON.purge_settings.purge_start_disk_space))
    assertThat(actualArtifactsConfig.getPurgeSettings().purgeUpto.purgeUptoDiskSpace, is(artifactsJSON.purge_settings.purge_upto_disk_space))
  }

  @Test
  void 'renders artifacts config with errors'() {
    ArtifactConfig artifactConfig = new ArtifactConfig()

    def artifactDirectory = new ArtifactDirectory("")
    artifactDirectory.addError("artifactDir", "artifact-dir-error")
    artifactConfig.setArtifactsDir(artifactDirectory)

    PurgeSettings purgeSettings = new PurgeSettings()
    purgeSettings.setPurgeStart(new PurgeStart(20))
    purgeSettings.setPurgeUpto(new PurgeUpto(10))
    purgeSettings.addError("purgeStart", "purge-start-error")
    purgeSettings.addError("purgeUpto", "purge-upto-error")
    artifactConfig.setPurgeSettings(purgeSettings)

    def json = toObjectString({
      ArtifactConfigRepresenter.toJSON(it, artifactConfig)
    })

    def expectedJSON = [
      "_links"        : [
        "self": [
          "href": "http://test.host/go/api/admin/config/server/artifact_config"
        ],
        "doc" : [
          "href": apiDocsUrl("#artifact_config")
        ]
      ],
      "artifacts_dir" : "",
      "purge_settings": [
        "purge_start_disk_space": new Double(20.0),
        "purge_upto_disk_space" : new Double(10.0),
        "errors"                : [
          "purge_start_disk_space": ["purge-start-error"],
          "purge_upto_disk_space" : ["purge-upto-error"]
        ]
      ],
      "errors"        : [
        "artifactDir": ["artifact-dir-error"]
      ]
    ]

    assertThatJson(json).isEqualTo(expectedJSON)
  }
}
