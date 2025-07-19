// app/build.gradle.kts

dependencies {
    // Media3 dependencies for version 1.7.1
    implementation("androidx.media3:media3-exoplayer:1.7.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.7.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.7.1")
    implementation("androidx.media3:media3-exoplayer-smoothstreaming:1.7.1")
    implementation("androidx.media3:media3-ui:1.7.1")
    implementation("androidx.media3:media3-common:1.7.1")
    implementation("androidx.media3:media3-datasource:1.7.1")
    implementation("androidx.media3:media3-datasource-okhttp:1.7.1") // Optional: for OkHttp support
    
    // Jetpack Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    
    // Optional: For better HTTP networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}