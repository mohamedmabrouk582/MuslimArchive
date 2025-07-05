package com.mabrouk.muslimarchive

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffset
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

data class ButtonData(val text: String, val icon: ImageVector)

@Composable
private fun Circle(
    modifier: Modifier = Modifier,
    color: Color,
    radius: Dp,
    bottomNavItem: BottomNavItem,
    iconColor: Color,
){
    Card(
        modifier = modifier
            .size(radius * 2)
            .clip(CircleShape)
            .background(color),
        elevation = CardDefaults.elevatedCardElevation(100.dp),
        colors = CardDefaults.cardColors(containerColor = color),
    ) {
        Box(modifier = Modifier.fillMaxSize().border(width = 1.dp, color = Color.Gray, shape = CircleShape), contentAlignment = Alignment.Center){
            AnimatedContent(
                targetState = bottomNavItem.icon, label = "Bottom bar circle icon",
            ) { targetIcon ->
                Icon(targetIcon, bottomNavItem.screenRoute, tint = iconColor)
            }
        }
    }
}


private class BarShape(
    private val offset: Float,
    private val circleRadius: Dp,
    private val cornerRadius: Dp,
    private val circleGap: Dp = 5.dp,
    private val cutoutEdgeOffsetFactor: Float = 1.2f
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(getPath(size, density,layoutDirection))
    }

    private fun getPath(size: Size, density: Density, layoutDirection: LayoutDirection): Path {
        val isRtl = layoutDirection == LayoutDirection.Rtl

        val cutoutCenterX = if (isRtl) {
            size.width - offset
        } else {
            offset
        }

        val cutoutRadius = density.run { (circleRadius + circleGap).toPx() }
        val cornerRadiusPx = density.run { cornerRadius.toPx() }

        return Path().apply {
            val cutoutEdgeOffset = cutoutRadius * cutoutEdgeOffsetFactor
            val cutoutLeftX = cutoutCenterX - cutoutEdgeOffset
            val cutoutRightX = cutoutCenterX + cutoutEdgeOffset

//            val leftCornerRadius = cornerRadiusPx.coerceAtMost(cutoutLeftX)
//            val rightCornerRadius = cornerRadiusPx.coerceAtMost(size.width - cutoutRightX)

            moveTo(0f, size.height)

            arcTo(
                rect = Rect(0f, 0f, 0f, 0f),
                startAngleDegrees = 180.0f,
                sweepAngleDegrees = 90.0f,
                forceMoveTo = false
            )

            lineTo(cutoutLeftX, 0f)

            // cutout
            cubicTo(
                cutoutCenterX - cutoutRadius, 0f,
                cutoutCenterX - cutoutRadius, cutoutRadius,
                cutoutCenterX, cutoutRadius
            )
            cubicTo(
                cutoutCenterX + cutoutRadius, cutoutRadius,
                cutoutCenterX + cutoutRadius, 0f,
                cutoutRightX, 0f
            )

            arcTo(
                rect = Rect(0f, 0f, size.width, 0f),
                startAngleDegrees = -90.0f,
                sweepAngleDegrees = 90.0f,
                forceMoveTo = false
            )

            lineTo(size.width, size.height)
            close()
        }
    }
}


@Composable
fun AnimatedNavigationBar(
    navHostController: NavController,
    barColor: Color,
    circleColor: Color,
    selectedColor: Color,
    unselectedColor: Color,
) {
    val circleRadius = 34.dp

    var selectedItem by rememberSaveable { mutableIntStateOf(0) }
    var barSize by remember { mutableStateOf(IntSize(0, 0)) }
    val offsetStep = remember(barSize) {
        barSize.width.toFloat() / (bottomNavItems.size * 2)
    }
    val offset = remember(selectedItem, offsetStep) {
        offsetStep + selectedItem * 2 * offsetStep
    }
    val circleRadiusPx = LocalDensity.current.run { circleRadius.toPx().toInt() }
    val offsetTransition = updateTransition(offset, "offset transition")
    val animation = spring<Float>(dampingRatio = 0.5f, stiffness = Spring.StiffnessVeryLow)
    val cutoutOffset by offsetTransition.animateFloat(
        transitionSpec = {
            if (this.initialState == 0f) {
                snap()
            } else {
                animation
            }
        },
        label = "cutout offset"
    ) { it }
    val circleOffset by offsetTransition.animateIntOffset(
        transitionSpec = {
            if (this.initialState == 0f) {
                snap()
            } else {
                spring(animation.dampingRatio, animation.stiffness)
            }
        },
        label = "circle offset"
    ) {
        IntOffset(it.toInt() - circleRadiusPx, -circleRadiusPx)
    }
    val barShape = remember(cutoutOffset) {
        BarShape(
            offset = cutoutOffset,
            circleRadius = circleRadius,
            cornerRadius = 34.dp,
        )
    }

    Box {
        Circle(
            modifier = Modifier
                .offset { circleOffset }
                .zIndex(1f),
            color = circleColor,
            radius = circleRadius,
            bottomNavItem = bottomNavItems[selectedItem],
            iconColor = selectedColor,
        )

        NavigationBar(
            modifier = Modifier
                .onPlaced { barSize = it.size }
                .graphicsLayer {
                    shape = barShape
                    clip = true
                }
                .fillMaxWidth()
        ) {
            val navBackStackEntry by navHostController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination


            bottomNavItems.forEachIndexed { index, item ->
                val isSelected =
                    currentDestination?.hierarchy?.any { it.route == item.screenRoute } == true
                if (isSelected){
                    selectedItem = index
                }
                NavigationBarItem(
                    selected = isSelected,
                    onClick = {
                        selectedItem = index
                        navHostController.navigate(item.screenRoute){
                            popUpTo(navHostController.graph.findStartDestination().id){
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                              },
                    alwaysShowLabel = false,
                    icon = {
                        val iconAlpha by animateFloatAsState(
                            targetValue = if (isSelected) 0f else 1f,
                            label = "Navbar item icon"
                        )
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.screenRoute,
                            modifier = Modifier.alpha(iconAlpha)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors().copy(
                        selectedIconColor = selectedColor,
                        selectedTextColor = selectedColor,
                        unselectedIconColor = unselectedColor,
                        unselectedTextColor = unselectedColor,
                        selectedIndicatorColor = Color.Transparent,
                    )
                )
            }
        }
    }
}