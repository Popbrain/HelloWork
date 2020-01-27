rootProject.name = "HelloWork"
include("library", "Sample:app", "Sample:sdkA", "Sample:sdkB")
findProject(":Sample:app")?.name = "app"
findProject(":Sample:sdkA")?.name = "sdkA"
findProject(":Sample:sdkB")?.name = "sdkB"
