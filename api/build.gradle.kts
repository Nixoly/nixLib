dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.zaxxer:HikariCP:5.1.0")
    compileOnly("org.xerial:sqlite-jdbc:3.46.0.0")
    compileOnly("com.mysql:mysql-connector-j:8.4.0")

    api("org.yaml:snakeyaml:2.2")

    testImplementation("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    testImplementation("com.h2database:h2:2.2.224")
    testImplementation("com.zaxxer:HikariCP:5.1.0")
    testImplementation("org.xerial:sqlite-jdbc:3.46.0.0")
}
