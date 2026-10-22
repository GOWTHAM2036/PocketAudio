package com.gowtham.pocketaudiobook.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object AppIcons {

    val Book: ImageVector by lazy {
        ImageVector.Builder(
            name = "Book",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(18f, 2f)
            lineTo(6f, 2f)
            curveTo(4.9f, 2f, 4f, 2.9f, 4f, 4f)
            lineTo(4f, 20f)
            curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f)
            lineTo(18f, 22f)
            curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
            lineTo(20f, 4f)
            curveTo(20f, 2.9f, 19.1f, 2f, 18f, 2f)
            close()
            moveTo(6f, 4f)
            lineTo(11f, 4f)
            lineTo(11f, 12f)
            lineTo(8.5f, 10.5f)
            lineTo(6f, 12f)
            lineTo(6f, 4f)
            close()
        }.build()
    }

    val Library: ImageVector by lazy {
        ImageVector.Builder(
            name = "Library",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(4f, 6f)
            horizontalLineTo(2f)
            verticalLineTo(20f)
            curveTo(2f, 21.1f, 2.9f, 22f, 4f, 22f)
            horizontalLineTo(18f)
            verticalLineTo(20f)
            horizontalLineTo(4f)
            verticalLineTo(6f)
            close()
            moveTo(20f, 2f)
            horizontalLineTo(8f)
            curveTo(6.9f, 2f, 6f, 2.9f, 6f, 4f)
            verticalLineTo(16f)
            curveTo(6f, 17.1f, 6.9f, 18f, 8f, 18f)
            horizontalLineTo(20f)
            curveTo(21.1f, 18f, 22f, 17.1f, 22f, 16f)
            verticalLineTo(4f)
            curveTo(22f, 2.9f, 21.1f, 2f, 20f, 2f)
            close()
            moveTo(12f, 14.5f)
            verticalLineTo(5.5f)
            lineTo(18f, 10f)
            lineTo(12f, 14.5f)
            close()
        }.build()
    }

    val Play: ImageVector by lazy {
        ImageVector.Builder(
            name = "Play",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(8f, 5f)
            lineTo(19f, 12f)
            lineTo(8f, 19f)
            close()
        }.build()
    }

    val Pause: ImageVector by lazy {
        ImageVector.Builder(
            name = "Pause",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(6f, 19f)
            horizontalLineTo(10f)
            verticalLineTo(5f)
            horizontalLineTo(6f)
            verticalLineTo(19f)
            close()
            moveTo(14f, 5f)
            verticalLineTo(19f)
            horizontalLineTo(18f)
            verticalLineTo(5f)
            horizontalLineTo(14f)
            close()
        }.build()
    }

    val Stop: ImageVector by lazy {
        ImageVector.Builder(
            name = "Stop",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(6f, 6f)
            horizontalLineTo(18f)
            verticalLineTo(18f)
            horizontalLineTo(6f)
            close()
        }.build()
    }

    val Restart: ImageVector by lazy {
        ImageVector.Builder(
            name = "Restart",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 5f)
            verticalLineTo(1f)
            lineTo(7f, 6f)
            lineTo(12f, 11f)
            verticalLineTo(7f)
            curveTo(15.31f, 7f, 18f, 9.69f, 18f, 13f)
            curveTo(18f, 16.31f, 15.31f, 19f, 12f, 19f)
            curveTo(8.69f, 19f, 6f, 16.31f, 6f, 13f)
            horizontalLineTo(4f)
            curveTo(4f, 17.42f, 7.58f, 21f, 12f, 21f)
            curveTo(16.42f, 21f, 20f, 17.42f, 20f, 13f)
            curveTo(20f, 8.58f, 16.42f, 5f, 12f, 5f)
            close()
        }.build()
    }

    val SkipBack15: ImageVector by lazy {
        ImageVector.Builder(
            name = "SkipBack15",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(11f, 18f)
            verticalLineTo(6f)
            lineTo(2.5f, 12f)
            lineTo(11f, 18f)
            close()
            moveTo(11.5f, 12f)
            lineTo(20f, 18f)
            verticalLineTo(6f)
            lineTo(11.5f, 12f)
            close()
        }.build()
    }

    val SkipForward30: ImageVector by lazy {
        ImageVector.Builder(
            name = "SkipForward30",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(13f, 6f)
            verticalLineTo(18f)
            lineTo(21.5f, 12f)
            lineTo(13f, 6f)
            close()
            moveTo(12.5f, 12f)
            lineTo(4f, 6f)
            verticalLineTo(18f)
            lineTo(12.5f, 12f)
            close()
        }.build()
    }

    val SkipPrevious: ImageVector by lazy {
        ImageVector.Builder(
            name = "SkipPrevious",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(6f, 6f)
            horizontalLineTo(8f)
            verticalLineTo(18f)
            horizontalLineTo(6f)
            verticalLineTo(6f)
            close()
            moveTo(9.5f, 12f)
            lineTo(18f, 18f)
            verticalLineTo(6f)
            lineTo(9.5f, 12f)
            close()
        }.build()
    }

    val SkipNext: ImageVector by lazy {
        ImageVector.Builder(
            name = "SkipNext",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(6f, 18f)
            lineTo(14.5f, 12f)
            lineTo(6f, 6f)
            verticalLineTo(18f)
            close()
            moveTo(16f, 6f)
            horizontalLineTo(18f)
            verticalLineTo(18f)
            horizontalLineTo(16f)
            verticalLineTo(6f)
            close()
        }.build()
    }

    val Headset: ImageVector by lazy {
        ImageVector.Builder(
            name = "Headset",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 3f)
            curveTo(7.03f, 3f, 3f, 7.03f, 3f, 12f)
            verticalLineTo(19f)
            curveTo(3f, 20.66f, 4.34f, 22f, 6f, 22f)
            horizontalLineTo(9f)
            verticalLineTo(14f)
            horizontalLineTo(5f)
            verticalLineTo(12f)
            curveTo(5f, 8.13f, 8.13f, 5f, 12f, 5f)
            curveTo(15.87f, 5f, 19f, 8.13f, 19f, 12f)
            verticalLineTo(14f)
            horizontalLineTo(15f)
            verticalLineTo(22f)
            horizontalLineTo(18f)
            curveTo(19.66f, 22f, 21f, 20.66f, 21f, 19f)
            verticalLineTo(12f)
            curveTo(21f, 7.03f, 16.97f, 3f, 12f, 3f)
            close()
        }.build()
    }

    val Mic: ImageVector by lazy {
        ImageVector.Builder(
            name = "Mic",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 14f)
            curveTo(13.66f, 14f, 15f, 12.66f, 15f, 11f)
            verticalLineTo(5f)
            curveTo(15f, 3.34f, 13.66f, 2f, 12f, 2f)
            curveTo(10.34f, 2f, 9f, 3.34f, 9f, 5f)
            verticalLineTo(11f)
            curveTo(9f, 12.66f, 10.34f, 14f, 12f, 14f)
            close()
            moveTo(17f, 11f)
            curveTo(17f, 13.76f, 14.76f, 16f, 12f, 16f)
            curveTo(9.24f, 16f, 7f, 13.76f, 7f, 11f)
            horizontalLineTo(5f)
            curveTo(5f, 14.42f, 7.72f, 17.23f, 11f, 17.72f)
            verticalLineTo(21f)
            horizontalLineTo(13f)
            verticalLineTo(17.72f)
            curveTo(16.28f, 17.23f, 19f, 14.42f, 19f, 11f)
            horizontalLineTo(17f)
            close()
        }.build()
    }

    val ChevronRight: ImageVector by lazy {
        ImageVector.Builder(
            name = "ChevronRight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(10f, 6f)
            lineTo(8.59f, 7.41f)
            lineTo(13.17f, 12f)
            lineTo(8.59f, 16.59f)
            lineTo(10f, 18f)
            lineTo(16f, 12f)
            close()
        }.build()
    }

    val CheckCircle: ImageVector by lazy {
        ImageVector.Builder(
            name = "CheckCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 2f)
            curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
            curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
            curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
            curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
            close()
            moveTo(10f, 17f)
            lineTo(5f, 12f)
            lineTo(6.41f, 10.59f)
            lineTo(10f, 14.17f)
            lineTo(17.59f, 6.58f)
            lineTo(19f, 8f)
            lineTo(10f, 17f)
            close()
        }.build()
    }

    val RadioButtonUnchecked: ImageVector by lazy {
        ImageVector.Builder(
            name = "RadioButtonUnchecked",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 2f)
            curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
            curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
            curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
            curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
            close()
            moveTo(12f, 20f)
            curveTo(7.58f, 20f, 4f, 16.42f, 4f, 12f)
            curveTo(4f, 7.58f, 7.58f, 4f, 12f, 4f)
            curveTo(16.42f, 4f, 20f, 7.58f, 20f, 12f)
            curveTo(20f, 16.42f, 16.42f, 20f, 12f, 20f)
            close()
        }.build()
    }

    val Check: ImageVector by lazy {
        ImageVector.Builder(
            name = "Check",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(9f, 16.17f)
            lineTo(4.83f, 12f)
            lineTo(3.41f, 13.41f)
            lineTo(9f, 19f)
            lineTo(21f, 7f)
            lineTo(19.59f, 5.59f)
            lineTo(9f, 16.17f)
            close()
        }.build()
    }

    val FileUpload: ImageVector by lazy {
        ImageVector.Builder(
            name = "FileUpload",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(9f, 16f)
            horizontalLineTo(15f)
            verticalLineTo(10f)
            horizontalLineTo(19f)
            lineTo(12f, 3f)
            lineTo(5f, 10f)
            horizontalLineTo(9f)
            verticalLineTo(16f)
            close()
            moveTo(5f, 18f)
            horizontalLineTo(19f)
            verticalLineTo(20f)
            horizontalLineTo(5f)
            close()
        }.build()
    }

    val Info: ImageVector by lazy {
        ImageVector.Builder(
            name = "Info",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 2f)
            curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
            curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
            curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
            curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
            close()
            moveTo(13f, 17f)
            horizontalLineTo(11f)
            verticalLineTo(11f)
            horizontalLineTo(13f)
            verticalLineTo(17f)
            close()
            moveTo(13f, 9f)
            horizontalLineTo(11f)
            verticalLineTo(7f)
            horizontalLineTo(13f)
            verticalLineTo(9f)
            close()
        }.build()
    }

    val Close: ImageVector by lazy {
        ImageVector.Builder(
            name = "Close",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(19f, 6.41f)
            lineTo(17.59f, 5f)
            lineTo(12f, 10.59f)
            lineTo(6.41f, 5f)
            lineTo(5f, 6.41f)
            lineTo(10.59f, 12f)
            lineTo(5f, 17.59f)
            lineTo(6.41f, 19f)
            lineTo(12f, 13.41f)
            lineTo(17.59f, 19f)
            lineTo(19f, 17.59f)
            lineTo(13.41f, 12f)
            close()
        }.build()
    }

    val Delete: ImageVector by lazy {
        ImageVector.Builder(
            name = "Delete",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(6f, 19f)
            curveTo(6f, 20.1f, 6.9f, 21f, 8f, 21f)
            horizontalLineTo(16f)
            curveTo(17.1f, 21f, 18f, 20.1f, 18f, 19f)
            verticalLineTo(7f)
            horizontalLineTo(6f)
            verticalLineTo(19f)
            close()
            moveTo(19f, 4f)
            horizontalLineTo(15.5f)
            lineTo(14.5f, 3f)
            horizontalLineTo(9.5f)
            lineTo(8.5f, 4f)
            horizontalLineTo(5f)
            verticalLineTo(6f)
            horizontalLineTo(19f)
            verticalLineTo(4f)
            close()
        }.build()
    }

    val Generate: ImageVector by lazy {
        ImageVector.Builder(
            name = "Generate",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 3f)
            lineTo(4f, 7f)
            verticalLineTo(17f)
            lineTo(12f, 21f)
            lineTo(20f, 17f)
            verticalLineTo(7f)
            lineTo(12f, 3f)
            close()
            moveTo(10f, 15f)
            verticalLineTo(9f)
            lineTo(15f, 12f)
            lineTo(10f, 15f)
            close()
        }.build()
    }

    val History: ImageVector by lazy {
        ImageVector.Builder(
            name = "History",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(13f, 3f)
            curveTo(8.03f, 3f, 4f, 7.03f, 4f, 12f)
            horizontalLineTo(1f)
            lineTo(4.89f, 15.89f)
            lineTo(5f, 16f)
            lineTo(9f, 12f)
            horizontalLineTo(6f)
            curveTo(6f, 8.13f, 9.13f, 5f, 13f, 5f)
            curveTo(16.87f, 5f, 20f, 8.13f, 20f, 12f)
            curveTo(20f, 15.87f, 16.87f, 19f, 13f, 19f)
            curveTo(11.07f, 19f, 9.32f, 18.21f, 8.06f, 16.94f)
            lineTo(6.64f, 18.36f)
            curveTo(8.27f, 20f, 10.51f, 21f, 13f, 21f)
            curveTo(17.97f, 21f, 22f, 16.97f, 22f, 12f)
            curveTo(22f, 7.03f, 17.97f, 3f, 13f, 3f)
            close()
            moveTo(12f, 8f)
            verticalLineTo(13f)
            lineTo(16.25f, 15.55f)
            lineTo(17f, 14.33f)
            lineTo(13.5f, 12.25f)
            verticalLineTo(8f)
            horizontalLineTo(12f)
            close()
        }.build()
    }

    val MoreVert: ImageVector by lazy {
        ImageVector.Builder(
            name = "MoreVert",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 8f)
            curveTo(13.1f, 8f, 14f, 7.1f, 14f, 6f)
            curveTo(14f, 4.9f, 13.1f, 4f, 12f, 4f)
            curveTo(10.9f, 4f, 10f, 4.9f, 10f, 6f)
            curveTo(10f, 7.1f, 10.9f, 8f, 12f, 8f)
            close()
            moveTo(12f, 10f)
            curveTo(10.9f, 10f, 10f, 10.9f, 10f, 12f)
            curveTo(10f, 13.1f, 10.9f, 14f, 12f, 14f)
            curveTo(13.1f, 14f, 14f, 13.1f, 14f, 12f)
            curveTo(14f, 10.9f, 13.1f, 10f, 12f, 10f)
            close()
            moveTo(12f, 16f)
            curveTo(10.9f, 16f, 10f, 16.9f, 10f, 18f)
            curveTo(10f, 19.1f, 10.9f, 20f, 12f, 20f)
            curveTo(13.1f, 20f, 14f, 19.1f, 14f, 18f)
            curveTo(14f, 16.9f, 13.1f, 16f, 12f, 16f)
            close()
        }.build()
    }
}
