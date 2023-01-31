Running Admin:

First, you will need to run uberJar:

```bash
./gradlew uberJar
```

This should output an uber jar in: 

`/admin/build/libs/admin-uber.jar`

Then you can run admin with:

```bash
java --add-opens java.base/sun.nio.ch=ALL-UNNAMED -jar admin-uber.jar
```

Note that the admin is a terminal application, and cannot run inside other tools such as IntelliJ terminal or via Gradle run.