// Shares the root version catalog (gradle/libs.versions.toml) with the build logic
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
