Example Usage (Wonderstorm Additions)
====================================

Original functionality maintained with the addition of support for retrieving encryption/decryption keys directly from specific environment variables.

Also added the following alias 
- `encrypt -v <value> [-k <keyfile>]` for encrypting values.  Alias of `encrypt-config-value`

1. Ensure you have an encryption & decryption key in the specified environment variables.  This could already be set up in your $PROFILE, you can check with:

```powershell
C:\ws\game-agent> echo ${env:config.encryption.key}
RSA-PUB:ZIBIIjANBgkq...

C:\ws\game-agent> echo ${env:config.decryption.key}
RSA-PRIV:DvIEgvIBDA...
```

2. Generate a new encrypted value

```console
my-application$ ./target/game-agent-shaded.jar encrypt -v topSecretPassword
enc:V92jePHsFbT0PxdJoer+oA== 
```

3. Paste it into your config

```yaml
auth:
   username: my-user
   password: ${enc:V92jePHsFbT0PxdJoer+oA==}
```

4. Start your application (with the `config.decryption.key` environment variable set)

```console
my-application$ ./target/game-agent-shaded.jar start config.yml
```


Making Changes (w/ Wonderstorm Additions)
==========================================

1. Ensure Java 8 is set in your path (for "java -jar...") and as your JAVA_HOME (for "mvn...").  This needs to happen before launching a shell/IDE, etc.  The two version commands should return something like before in order for it to work.

```powershell
C:\ws\encrypted-config-value [develop]> java -version                                                                     openjdk version "1.8.0_232"
OpenJDK Runtime Environment Corretto-8.232.09.1 (build 1.8.0_232-b09)
OpenJDK 64-Bit Server VM Corretto-8.232.09.1 (build 25.232-b09, mixed mode)

C:\ws\encrypted-config-value [develop]> mvn -version                                                                      Apache Maven 3.6.1 (d66c9c0b3152b2e69ee9bac180bb8fcc8e6af555; 2019-04-04T12:00:29-07:00)
Maven home: C:\Program Files\JetBrains\IntelliJ IDEA 2019.2.4\plugins\maven\lib\maven3\bin\..
Java version: 1.8.0_232, vendor: Amazon.com Inc., runtime: C:\Program Files\Amazon Corretto\jdk1.8.0_232\jre
Default locale: en_US, platform encoding: Cp1252
OS name: "windows 10", version: "10.0", arch: "amd64", family: "windows"
```

2. Ensure the project is set up to use Gradle as the build system in your IDE & CLI.  For CLI, you can test by building:

```console
.\gradlew.bat clean build
```

3. Make code changes

4. build

```console
.\gradlew.bat clean build
```

6. Tag in git (package versions are driven from git tags)

```console
git tag -a 2.2.3-wonderstorm -m "I have fixed bugs"
```

5. Push to package repo

```console
.\gradlew.bat publishGprPublicationToWs-githubRepository
```

Original Repo below this line.
==============================

<hr>

<p align="right">
<a href="https://autorelease.general.dmz.palantir.tech/palantir/encrypted-config-value"><img src="https://img.shields.io/badge/Perform%20an-Autorelease-success.svg" alt="Autorelease"></a>
</p>

Encrypted Config Value
======================
[![Build Status](https://circleci.com/gh/palantir/encrypted-config-value.svg?style=shield)](https://circleci.com/gh/palantir/encrypted-config-value)
[![JCenter Release](https://img.shields.io/github/release/palantir/encrypted-config-value.svg)](
http://jcenter.bintray.com/com/palantir/config/crypto/)

This repository provides tooling for encrypting certain configuration parameter values in Dropwizard apps. This defends against accidental leaks of sensitive information such as copy/pasting a config file - unlike jetty obsfucated passwords, one would also have to share the encryption key to actually reveal the sensitive information.

encrypted-config-value-bundle
-----------------------------
A Dropwizard bundle which provides a way of using encrypted values in your Dropwizard configs (via a variable substitutor) and utility commands.

The bundle sets the `ConfigurationSourceProvider` to one capable of parsing encrypted values specified as variables.

The bundle adds the following commands:
 - `encrypt-config-value -v <value> [-k <keyfile>]` for encrypting values. In the case of non-symmetric algorithms (e.g. RSA) specify the public key.
 - `generate-random-key -a <algorithm> [-f <keyfile>]` for generating random keys with the specified algorithm. In the case of non-symmetric algorithms (e.g. RSA) the private key will have a .private extension.
 
Currently supported algorithms:
 - AES: (AES/GCM/NoPadding) with random IV
 - RSA

### Example Usage

Maven artifacts are published to JCenter. Dropwizard bundles are separated into two different packages: one for Dropwizard 1.x and one for Dropwizard 0.9.x and below. Example Gradle dependency configuration:

```groovy
repositories {
    jcenter()
}

dependencies {
    // adds EncryptedConfigValueBundle for Dropwizard 1.x apps
    compile "com.palantir.config.crypto:encrypted-config-value-bundle-dropwizard1:$version"
    // or, adds EncryptedConfigValueBundle for Drowizard <= 0.9.x apps
    compile "com.palantir.config.crypto:encrypted-config-value-bundle:$version"
}
```

To use in your app, just add the bundle:

```java
public final class Main extends Application<MyApplicationConfig> {
    @Override
    public void initialize(Bootstrap<MyApplicationConfig> bootstrap) {
        ...
        bootstrap.addBundle(new EncryptedConfigValueBundle());
    }
    ...
}
```
 
Then:

```console
my-application$ ./bin/my-dropwizard-app generate-random-key -a AES
Wrote key to var/conf/encrypted-config-value.key
my-application$ ./bin/my-dropwizard-app encrypt-config-value -v topSecretPassword
enc:V92jePHsFbT0PxdJoer+oA== 
```

Now use the encrypted value in your config file (as a variable):

```yaml
auth:
   username: my-user
   password: ${enc:INNv4cGkVF45MLWZhgVZdIsgQ4zKvbMoJ978Es3MIKgrtz5eeTuOCLM1vPbQm97ejz2EK6M=}
```

encrypted-config-value-module
-----------------------------
Not Dropwizard? You can still use encrypted values in your configuration file.

### Example Usage

```java
public final class AppConfiguration {

    private static final ObjectMapper MAPPER = new YAMLMapper()
                                                   .registerModule(new GuavaModule());

    ...

    public static AppConfiguration fromYaml(File configFile) {
        ...
        return EncryptedConfigMapperUtils.getConfig(configFile, AppConfiguration.class, MAPPER);
    }
    ...
}
```

License
-------
This repository is made available under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0).
