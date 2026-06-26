import org.gradle.api.Plugin
import org.gradle.api.Project

import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.findByType(com.android.build.api.dsl.ApplicationExtension::class.java)?.apply {
                buildFeatures {
                    compose = true
                }
            }

            extensions.findByType(com.android.build.api.dsl.LibraryExtension::class.java)?.apply {
                buildFeatures {
                    compose = true
                }
            }

            val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

            dependencies {
                // Compose BOM
                libs.findLibrary("androidx-compose-bom").ifPresent { bom ->
                    add("implementation", platform(bom))
                }

                // Compose 핵심 라이브러리
                libs.findLibrary("androidx-compose-ui").ifPresent {
                    add("implementation", it)
                }
                libs.findLibrary("androidx-compose-ui-graphics").ifPresent {
                    add("implementation", it)
                }
                libs.findLibrary("androidx-compose-ui-tooling-preview").ifPresent {
                    add("implementation", it)
                }
                libs.findLibrary("androidx-compose-material3").ifPresent {
                    add("implementation", it)
                }
                libs.findLibrary("androidx-compose-material-icons-extended").ifPresent {
                    add("implementation", it)
                }
                libs.findLibrary("androidx-compose-ui-tooling").ifPresent {
                    add("debugImplementation", it)
                }
            }

            pluginManager.withPlugin("com.android.application") {
                dependencies {
                    libs.findLibrary("androidx-compose-bom").ifPresent { bom ->
                        add("androidTestImplementation", platform(bom))
                    }
                }
            }

            pluginManager.withPlugin("com.android.library") {
                dependencies {
                    libs.findLibrary("androidx-compose-bom").ifPresent { bom ->
                        add("androidTestImplementation", platform(bom))
                    }
                }
            }
        }
    }
}
