package com.mabrouk.muslimarchive

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

val Story: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
    ImageVector.Builder(
        name = "Filled.Story",
        defaultWidth = 25.dp,
        defaultHeight = 25.dp,
        viewportWidth = 512f,
        viewportHeight = 512f,
    ).apply {
        path(fill = SolidColor(Color(0xFF000000))) {
            moveTo(349.867f, 392.533f)
            curveToRelative(4.71f, 0f, 8.533f, -3.823f, 8.533f, -8.533f)
            verticalLineToRelative(-8.533f)
            horizontalLineToRelative(8.533f)
            curveToRelative(4.71f, 0f, 8.533f, -3.823f, 8.533f, -8.533f)
            reflectiveCurveToRelative(-3.823f, -8.533f, -8.533f, -8.533f)
            horizontalLineTo(358.4f)
            verticalLineToRelative(-8.533f)
            curveToRelative(0f, -4.71f, -3.823f, -8.533f, -8.533f, -8.533f)
            reflectiveCurveToRelative(-8.533f, 3.823f, -8.533f, 8.533f)
            verticalLineToRelative(8.533f)
            horizontalLineTo(332.8f)
            curveToRelative(-4.71f, 0f, -8.533f, 3.823f, -8.533f, 8.533f)
            reflectiveCurveToRelative(3.823f, 8.533f, 8.533f, 8.533f)
            horizontalLineToRelative(8.533f)
            verticalLineTo(384f)
            curveTo(341.333f, 388.71f, 345.156f, 392.533f, 349.867f, 392.533f)
            close()
        }
        path(fill = SolidColor(Color(0xFF000000))) {
            moveTo(469.333f, 59.733f)
            curveToRelative(0f, 4.71f, 3.823f, 8.533f, 8.533f, 8.533f)
            reflectiveCurveToRelative(8.533f, -3.823f, 8.533f, -8.533f)
            curveToRelative(4.71f, 0f, 8.533f, -3.823f, 8.533f, -8.533f)
            reflectiveCurveToRelative(-3.823f, -8.533f, -8.533f, -8.533f)
            curveToRelative(0f, -4.71f, -3.823f, -8.533f, -8.533f, -8.533f)
            reflectiveCurveToRelative(-8.533f, 3.823f, -8.533f, 8.533f)
            curveToRelative(-4.71f, 0f, -8.533f, 3.823f, -8.533f, 8.533f)
            reflectiveCurveTo(464.623f, 59.733f, 469.333f, 59.733f)
            close()
        }
        path(fill = SolidColor(Color(0xFF000000))) {
            moveTo(256f, 102.4f)
            curveToRelative(-65.877f, 0f, -119.467f, 53.589f, -119.467f, 119.467f)
            curveToRelative(0f, 4.71f, 3.823f, 8.533f, 8.533f, 8.533f)
            curveToRelative(4.71f, 0f, 8.533f, -3.823f, 8.533f, -8.533f)
            curveToRelative(0f, -56.465f, 45.935f, -102.4f, 102.4f, -102.4f)
            reflectiveCurveToRelative(102.4f, 45.935f, 102.4f, 102.4f)
            curveToRelative(0f, 4.71f, 3.823f, 8.533f, 8.533f, 8.533f)
            reflectiveCurveToRelative(8.533f, -3.823f, 8.533f, -8.533f)
            curveTo(375.467f, 155.989f, 321.877f, 102.4f, 256f, 102.4f)
            close()
        }
        path(fill = SolidColor(Color(0xFF000000))) {
            moveTo(332.8f, 25.6f)
            curveToRelative(0f, 4.71f, 3.823f, 8.533f, 8.533f, 8.533f)
            reflectiveCurveToRelative(8.533f, -3.823f, 8.533f, -8.533f)
            curveToRelative(4.71f, 0f, 8.533f, -3.823f, 8.533f, -8.533f)
            reflectiveCurveToRelative(-3.823f, -8.533f, -8.533f, -8.533f)
            curveToRelative(0f, -4.71f, -3.823f, -8.533f, -8.533f, -8.533f)
            reflectiveCurveTo(332.8f, 3.823f, 332.8f, 8.533f)
            curveToRelative(-4.71f, 0f, -8.533f, 3.823f, -8.533f, 8.533f)
            reflectiveCurveTo(328.09f, 25.6f, 332.8f, 25.6f)
            close()
        }
        path(fill = SolidColor(Color(0xFF000000))) {
            moveTo(76.8f, 230.4f)
            curveToRelative(4.71f, 0f, 8.533f, -3.823f, 8.533f, -8.533f)
            curveTo(85.333f, 127.761f, 161.894f, 51.2f, 256f, 51.2f)
            reflectiveCurveToRelative(170.667f, 76.561f, 170.667f, 170.667f)
            curveToRelative(0f, 4.71f, 3.823f, 8.533f, 8.533f, 8.533f)
            reflectiveCurveToRelative(8.533f, -3.823f, 8.533f, -8.533f)
            curveToRelative(0f, -103.518f, -84.215f, -187.733f, -187.733f, -187.733f)
            reflectiveCurveTo(68.267f, 118.349f, 68.267f, 221.867f)
            curveTo(68.267f, 226.577f, 72.09f, 230.4f, 76.8f, 230.4f)
            close()
        }
        path(fill = SolidColor(Color(0xFF000000))) {
            moveTo(177.92f, 299.947f)
            lineToRelative(-7.424f, -37.086f)
            curveToRelative(-0.794f, -3.994f, -4.292f, -6.861f, -8.363f, -6.861f)
            curveToRelative(-4.07f, 0f, -7.569f, 2.867f, -8.363f, 6.861f)
            lineToRelative(-7.424f, 37.086f)
            lineToRelative(-37.086f, 7.424f)
            curveToRelative(-3.994f, 0.794f, -6.861f, 4.292f, -6.861f, 8.363f)
            curveToRelative(0f, 4.07f, 2.867f, 7.569f, 6.861f, 8.363f)
            lineToRelative(37.086f, 7.424f)
            lineToRelative(7.424f, 37.086f)
            curveToRelative(0.794f, 3.994f, 4.292f, 6.861f, 8.363f, 6.861f)
            curveToRelative(4.07f, 0f, 7.569f, -2.867f, 8.363f, -6.861f)
            lineToRelative(7.424f, -37.086f)
            lineToRelative(37.086f, -7.424f)
            curveToRelative(3.994f, -0.794f, 6.861f, -4.292f, 6.861f, -8.363f)
            curveToRelative(0f, -4.07f, -2.867f, -7.569f, -6.861f, -8.363f)
            lineTo(177.92f, 299.947f)
            close()
            moveTo(168.994f, 315.904f)
            curveToRelative(-3.379f, 0.674f, -6.016f, 3.311f, -6.69f, 6.69f)
            lineToRelative(-0.171f, 0.828f)
            lineToRelative(-0.171f, -0.828f)
            curveToRelative(-0.674f, -3.379f, -3.311f, -6.016f, -6.69f, -6.69f)
            lineToRelative(-0.828f, -0.171f)
            lineToRelative(0.828f, -0.171f)
            curveToRelative(3.379f, -0.674f, 6.016f, -3.311f, 6.69f, -6.69f)
            lineToRelative(0.171f, -0.828f)
            lineToRelative(0.171f, 0.828f)
            curveToRelative(0.674f, 3.379f, 3.311f, 6.016f, 6.69f, 6.69f)
            lineToRelative(0.828f, 0.171f)
            lineTo(168.994f, 315.904f)
            close()
        }
        path(fill = SolidColor(Color(0xFF000000))) {
            moveTo(503.467f, 128f)
            curveToRelative(-4.71f, 0f, -8.533f, 3.823f, -8.533f, 8.533f)
            verticalLineToRelative(332.8f)
            curveToRelative(0f, 14.114f, -11.486f, 25.6f, -25.6f, 25.6f)
            horizontalLineToRelative(-204.8f)
            verticalLineToRelative(-19.635f)
            curveToRelative(12.442f, -4.352f, 44.843f, -14.498f, 76.8f, -14.498f)
            curveToRelative(74.325f, 0f, 124.8f, 16.461f, 125.312f, 16.631f)
            curveToRelative(2.586f, 0.862f, 5.453f, 0.418f, 7.68f, -1.178f)
            curveToRelative(2.219f, -1.604f, 3.541f, -4.181f, 3.541f, -6.921f)
            verticalLineTo(93.867f)
            curveToRelative(0f, -3.413f, -2.031f, -6.502f, -5.171f, -7.842f)
            curveToRelative(-0.759f, -0.324f, -18.91f, -7.962f, -54.349f, -10.914f)
            curveToRelative(-4.753f, -0.435f, -8.815f, 3.098f, -9.207f, 7.791f)
            curveToRelative(-0.393f, 4.702f, 3.098f, 8.823f, 7.791f, 9.216f)
            curveToRelative(21.982f, 1.826f, 36.668f, 5.572f, 43.87f, 7.808f)
            verticalLineToRelative(358.067f)
            curveToRelative(-19.337f, -5.069f, -62.276f, -14.259f, -119.467f, -14.259f)
            curveToRelative(-37.18f, 0f, -73.702f, 12.211f, -85.001f, 16.35f)
            curveToRelative(-10.044f, -4.437f, -40.405f, -16.35f, -77.133f, -16.35f)
            curveToRelative(-58.778f, 0f, -107.204f, 9.694f, -128f, 14.618f)
            verticalLineTo(100.463f)
            curveToRelative(7.987f, -1.971f, 23.287f, -5.436f, 43.409f, -8.533f)
            curveToRelative(4.659f, -0.717f, 7.859f, -5.069f, 7.142f, -9.728f)
            curveToRelative(-0.725f, -4.659f, -5.12f, -7.876f, -9.728f, -7.134f)
            curveTo(60.749f, 79.872f, 41.139f, 85.427f, 40.32f, 85.666f)
            curveToRelative(-3.661f, 1.041f, -6.187f, 4.395f, -6.187f, 8.201f)
            verticalLineToRelative(375.467f)
            curveToRelative(0f, 2.671f, 1.254f, 5.197f, 3.388f, 6.81f)
            curveToRelative(1.502f, 1.135f, 3.311f, 1.724f, 5.146f, 1.724f)
            curveToRelative(0.785f, 0f, 1.57f, -0.111f, 2.338f, -0.333f)
            curveToRelative(0.589f, -0.162f, 59.597f, -16.734f, 134.195f, -16.734f)
            curveToRelative(31.198f, 0f, 57.856f, 9.711f, 68.267f, 14.071f)
            verticalLineToRelative(20.062f)
            horizontalLineToRelative(-204.8f)
            curveToRelative(-14.114f, 0f, -25.6f, -11.486f, -25.6f, -25.6f)
            verticalLineToRelative(-332.8f)
            curveToRelative(0f, -4.71f, -3.823f, -8.533f, -8.533f, -8.533f)
            reflectiveCurveTo(0f, 131.823f, 0f, 136.533f)
            verticalLineToRelative(332.8f)
            curveTo(0f, 492.86f, 19.14f, 512f, 42.667f, 512f)
            horizontalLineToRelative(426.667f)
            curveTo(492.86f, 512f, 512f, 492.86f, 512f, 469.333f)
            verticalLineToRelative(-332.8f)
            curveTo(512f, 131.823f, 508.177f, 128f, 503.467f, 128f)
            close()
        }
        path(fill = SolidColor(Color(0xFF000000))) {
            moveTo(76.8f, 34.133f)
            horizontalLineToRelative(8.533f)
            verticalLineToRelative(8.533f)
            curveToRelative(0f, 4.71f, 3.823f, 8.533f, 8.533f, 8.533f)
            curveToRelative(4.71f, 0f, 8.533f, -3.823f, 8.533f, -8.533f)
            verticalLineToRelative(-8.533f)
            horizontalLineToRelative(8.533f)
            curveToRelative(4.71f, 0f, 8.533f, -3.823f, 8.533f, -8.533f)
            curveToRelative(0f, -4.71f, -3.823f, -8.533f, -8.533f, -8.533f)
            horizontalLineTo(102.4f)
            verticalLineTo(8.533f)
            curveTo(102.4f, 3.823f, 98.577f, 0f, 93.867f, 0f)
            curveToRelative(-4.71f, 0f, -8.533f, 3.823f, -8.533f, 8.533f)
            verticalLineToRelative(8.533f)
            horizontalLineTo(76.8f)
            curveToRelative(-4.71f, 0f, -8.533f, 3.823f, -8.533f, 8.533f)
            curveTo(68.267f, 30.31f, 72.09f, 34.133f, 76.8f, 34.133f)
            close()
        }
        path(fill = SolidColor(Color(0xFF000000))) {
            moveTo(247.467f, 145.067f)
            verticalLineTo(435.2f)
            curveToRelative(0f, 4.71f, 3.823f, 8.533f, 8.533f, 8.533f)
            reflectiveCurveToRelative(8.533f, -3.823f, 8.533f, -8.533f)
            verticalLineTo(145.067f)
            curveToRelative(0f, -4.71f, -3.823f, -8.533f, -8.533f, -8.533f)
            reflectiveCurveTo(247.467f, 140.356f, 247.467f, 145.067f)
            close()
        }
        path(fill = SolidColor(Color(0xFF000000))) {
            moveTo(401.067f, 230.4f)
            curveToRelative(4.71f, 0f, 8.533f, -3.823f, 8.533f, -8.533f)
            curveToRelative(0f, -84.693f, -68.907f, -153.6f, -153.6f, -153.6f)
            reflectiveCurveToRelative(-153.6f, 68.907f, -153.6f, 153.6f)
            curveToRelative(0f, 4.71f, 3.823f, 8.533f, 8.533f, 8.533f)
            reflectiveCurveToRelative(8.533f, -3.823f, 8.533f, -8.533f)
            curveToRelative(0f, -75.281f, 61.252f, -136.533f, 136.533f, -136.533f)
            reflectiveCurveToRelative(136.533f, 61.252f, 136.533f, 136.533f)
            curveTo(392.533f, 226.577f, 396.356f, 230.4f, 401.067f, 230.4f)
            close()
        }
    }.build()
}

@Preview(showBackground = true)
@Composable
private fun StoryPreview() {
    Box(modifier = Modifier.padding(12.dp)) {
        Image(imageVector = Story, contentDescription = null)
    }
}
