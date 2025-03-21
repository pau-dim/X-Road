plugins {
  id("xroad.java-conventions")
}

dependencies {
  implementation(platform(libs.springBoot.bom))

  implementation(project(":lib:globalconf-core"))
  implementation(project(":common:common-scheduler"))
  implementation(project(":common:common-jetty"))
  implementation(project(":common:common-rpc"))
  implementation("org.springframework:spring-context-support")

  implementation(libs.commons.cli)

  testImplementation(project(":common:common-test"))
  testImplementation(libs.wiremock.standalone)
}
