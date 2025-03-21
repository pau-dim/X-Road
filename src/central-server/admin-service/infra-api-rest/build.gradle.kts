plugins {
  id("xroad.java-conventions")
}

dependencies {
  annotationProcessor(libs.mapstructProcessor)
  annotationProcessor(libs.lombokMapstructBinding)

  implementation(project(":central-server:admin-service:core-api"))
  implementation(project(":central-server:openapi-model"))
  implementation(project(":common:common-domain"))
  implementation(project(":lib:globalconf-core"))
  implementation(project(":service:signer:signer-api"))
  implementation(project(":common:common-admin-api"))

  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-cache")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation(libs.mapstruct)

  testImplementation(project(":common:common-test"))
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.security:spring-security-test")
}

sourceSets {
  main {
    java.srcDirs(
      layout.buildDirectory.dir("generated/sources/annotationProcessor/java/main")
    )
  }
}
