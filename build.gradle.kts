plugins {
    id("java")
}

group = "com.axway.library"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_11
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.compileClasspath.map { config -> config.map { if (it.isDirectory) it else zipTree(it) } })
}
repositories {
    mavenCentral()
}

object DependencyVersions {
    const val JACKSON_CORE = "2.16.0"
    const val LOMBOK = "1.18.30"
    const val HTTPCLIENT5 = "5.3"
    const val SWAGGER_REQUEST_VALIDATOR_CORE = "2.39.0"
    const val APIGW_DEPENDENCY = "7.7.0.20230830"
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-core:${DependencyVersions.JACKSON_CORE}")
    compileOnly("org.projectlombok:lombok:${DependencyVersions.LOMBOK}")
    annotationProcessor("org.projectlombok:lombok:${DependencyVersions.LOMBOK}")
    implementation("org.apache.httpcomponents.client5:httpclient5:${DependencyVersions.HTTPCLIENT5}")
    implementation("com.atlassian.oai:swagger-request-validator-core:${DependencyVersions.SWAGGER_REQUEST_VALIDATOR_CORE}")
    implementation(fileTree(mapOf("dir" to "apigw-dependencies/${DependencyVersions.APIGW_DEPENDENCY}", "include" to listOf("*.jar"))))
}