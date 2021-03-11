# Manifest Config
[![Maven Central](https://img.shields.io/maven-central/v/io.github.rakutentech.manifestconfig/manifest-config-processor)](https://search.maven.org/search?q=io.github.rakutentech.manifestconfig)
[![CircleCI](https://circleci.com/gh/rakutentech/android-manifest-config/tree/master.svg?style=svg)](https://circleci.com/gh/rakutentech/android-manifest-config/tree/master)

Annotation processor that generates a class to lookup manifest metadata.

## Example
1. Add annotation processor to your project

```groovy
repositories {
  mavenCentral()
}
dependencies {
  implementation        "io.github.rakutentech.manifestconfig:manifest-config-annotations:$version"
  annotationProcessor   "io.github.rakutentech.manifestconfig:manifest-config-processor:$version"
}
```

2. Define the config interface

```java
@ManifestConfig
public interface ApiClient {
  int retries();
  String apiEndpoint();
}
```

3. Configure manifest meta-data

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest>
  <application>
    <meta-data
      android:name="Retries"
      android:value="1"/>
    <meta-data
      android:name="ApiEndpoint"
      android:value="https://example.com/"/>
  </application>
</manifest>
```

4. Read config from generated class `ApiClientManifestConfig` (which looks up `"Retries"` and `"ApiEndpoint"` from the manifest meta data).

```java
ApiClient config = new ApiClientManifestConfig(context);
config.retries();
config.apiEndpoint();
```

**Why?**: We use manifest metadata to configure many of our libraries with static values. To reduce duplication in both source and test code we generate that repetitive code.

## Advanced Features
For more customization regarding meta keys and fallback values you can use the `@MetaData` annotation on interface methods, e.g.

```java
@ManifestConfig
public interface ApiClient {
  @MetaData(key = "my.package.prefix.Retries", value = "4")
  int retries();
  @MetaData(key = "my.package.prefix.ApiEndpoint", value = "https://example.com")
  String apiEndpoint();
}
// Will generate lookup calls like this:
public interface ApiClientManifestconfig {
  // ... boilerplate
  @Override
  public int retries() {
    return metaData.getInt("my.package.prefix.Retries", 4);
  }
  @Override
  public String apiEndpoint() {
    return metaData.getString("my.package.prefix.ApiEndpoint", "https://example.com");
  }
}
```

### Supported types & fallback value parsing

type                            | default value | parsing of `MetaData.value()`
------------------------------- | ------------- | -----------------------------
`int`, `java.lang.Integer`      | `-1`          | Kotlin's [`String.toInt`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/to-int.html)
`float`, `java.lang.Float`      | `-1.0f`       | Kotlin's [`String.toFloat`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/to-float.html)
`boolean`, `java.lang.Boolean`  | `false`       | only `"true"` and `"false"` convert (ignoring case)
`java.lang.String`              | `""``         | taken as is

## Contributions

Found a bug? Please [file an issue](https://github.com/rakutentech/android-manifest-config/issues/new) or send a [pull request](https://github.com/rakutentech/android-manifest-config/compare) 🙏

### Project structure

* `manifest-config-annotations`: Java annotations to mark source code for generation
* `manifest-config-processor`: Annotation processor that consumes the annotations and generates implementations
* `manifest-config-sample`: Example project

## Changelog

### 0.2.0 (2021-03-09)

- Changed Maven Group ID to `io.github.rakutentech.manifestconfig`. You must upudate your dependency declarations to the following:
    - `io.github.rakutentech.manifestconfig:manifest-config-annotations:0.2.0`
    - `io.github.rakutentech.manifestconfig:manifest-config-processor:0.2.0`
- Migrated publishing to Maven Central due to Bintray/JCenter being [shutdown](https://jfrog.com/blog/into-the-sunset-bintray-jcenter-gocenter-and-chartcenter/). You must add `mavenCentral()` to your `repositories``.


### 0.1.0 (2018-08-14)

- Initial release.
