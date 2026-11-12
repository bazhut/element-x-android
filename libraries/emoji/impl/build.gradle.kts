/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

import extension.setupDependencyInjection
import extension.testCommonDependencies

plugins {
    id("io.element.android-compose-library")
}

android {
    namespace = "io.element.android.libraries.emoji.impl"
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

setupDependencyInjection()

dependencies {
    api(projects.libraries.emoji.api)
    implementation(projects.appconfig)
    implementation(projects.libraries.architecture)
    implementation(projects.libraries.core)
    implementation(projects.libraries.designsystem)
    implementation(projects.libraries.di)
    implementation(projects.libraries.matrix.api)
    implementation(projects.libraries.matrixmedia.api)
    implementation(projects.services.toolbox.api)

    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.matrix.emojibase.bindings)
    implementation(libs.coil.compose)
    implementation(libs.serialization.json)
    implementation(platform(libs.network.okhttp.bom))
    implementation(libs.network.okhttp.okhttp)

    testImplementation(projects.libraries.matrix.test)
    testImplementation(projects.services.toolbox.test)
    testImplementation(libs.network.mockwebserver)
    testCommonDependencies(libs, includeTestComposeView = true)
}
