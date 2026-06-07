import java.io.File

fun Settings.locateNixLibRoot(): File? {
    val fromGradleProperty = (extra.properties["nixlib.dir"] as? String)
        ?: (extra.properties["nixlib.path"] as? String)
    val fromEnv = System.getenv("NIXLIB_HOME")
    val candidates = listOfNotNull(fromGradleProperty, fromEnv, "../../Java/nixLib")
    for (raw in candidates) {
        val dir = if (File(raw).isAbsolute) File(raw) else File(rootDir, raw)
        if (File(dir, "settings.gradle.kts").isFile) {
            return dir.canonicalFile
        }
    }
    return null
}

fun Settings.linkNixLibComposite(
    compositeEnabled: Boolean = (extra.properties["nixlib.composite"] as? String)?.toBoolean() != false,
) {
    if (!compositeEnabled) return
    val nixLibRoot = locateNixLibRoot() ?: return
    includeBuild(nixLibRoot) {
        name = "nixLib"
        dependencySubstitution {
            substitute(module("dev.nixoly.nixLib:api")).using(project(":api"))
            substitute(module("dev.nixoly.nixLib:core")).using(project(":core"))
            substitute(module("dev.nixoly.nixLib:folia")).using(project(":folia"))
        }
    }
}
