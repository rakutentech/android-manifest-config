# Manifest Config
Annotation processor that generates a class to lookup manifest metadata.

## Example
1. Add annotation processor to your project

```groovy
dependencies {
  implementation        "com.rakuten.tech.mobile.util:manifest-config-annotations:$version"
  annotationProcessor   "com.rakuten.tech.mobile.util:manifest-config-processor:$version"
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

**Why?**: We use manifest metadata to configure many of our libraries with static values. To reduce duplication in both source and test code we generate that dumb code.

## Project structure
* `manifest-config-annotations`: Java annotations to mark source code for generation
* `manifest-config-processor`: Annotation processor that consumes the annotations and generates implementations
* `manifest-config-sample`: Example project
* 