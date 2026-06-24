import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * 순수 Kotlin 모듈(domain 등)을 위한 Convention Plugin.
 *
 * - kotlin("jvm") 적용
 * - JVM toolchain 21 설정
 * - javax.inject 의존성 추가 (Hilt @Inject 어노테이션용)
 * - kotlinx-coroutines-core 의존성 추가 (Flow 사용)
 */
class KotlinLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.jvm")
            }

            extensions.configure<JavaPluginExtension> {
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(21))
                }
            }

            val libs = extensions.getByType(
                org.gradle.api.artifacts.VersionCatalogsExtension::class.java
            ).named("libs")

            dependencies {
                add("implementation", libs.findLibrary("javax-inject").get())
                add("implementation", libs.findLibrary("kotlinx-coroutines-core").get())
            }
        }
    }
}
