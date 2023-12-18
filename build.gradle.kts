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

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-core:2.16.0")
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.3")
    implementation("com.atlassian.oai:swagger-request-validator-core:2.39.0")
    implementation(fileTree(mapOf("dir" to "libs-7.7.0.20230830", "include" to listOf("*.jar"))))
}