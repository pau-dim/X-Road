plugins {
  id("xroad.java-conventions")
}

dependencies {
  api(project(":common:common-core"))
  api(project(":common:common-message"))

  api(libs.jetty.server)
}
