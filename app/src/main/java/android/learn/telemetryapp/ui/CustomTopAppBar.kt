package android.learn.telemetryapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class CustomTopAppBarColors(
   val containerColor: Color,
   val titleContentColor: Color,
   val navigationIconContentColor: Color,
   val actionIconContentColor: Color
)

object CustomTopAppBarDefaults {
   @Composable
   fun colors(
      containerColor: Color = Color.Transparent,
      titleContentColor: Color = Color.White,
      navigationIconContentColor: Color = Color.White,
      actionIconContentColor: Color = Color.White
   ): CustomTopAppBarColors {
      return CustomTopAppBarColors(
         containerColor = containerColor,
         titleContentColor = titleContentColor,
         navigationIconContentColor = navigationIconContentColor,
         actionIconContentColor = actionIconContentColor
      )
   }
}

@Composable
fun CustomTopAppBar(
   title: @Composable () -> Unit,
   modifier: Modifier = Modifier,
   navigationIcon: @Composable (() -> Unit)? = null,
   actions: @Composable (RowScope.() -> Unit)? = null,
   colors: CustomTopAppBarColors = CustomTopAppBarDefaults.colors()
) {
   Row(
      modifier = modifier
         .fillMaxWidth()
         .height(64.dp)
         .background(colors.containerColor)
         .padding(horizontal = 16.dp, vertical = 2.dp),

      verticalAlignment = Alignment.CenterVertically
   ) {
      if (navigationIcon != null) {
         CompositionLocalProvider(LocalContentColor provides colors.navigationIconContentColor) {
            navigationIcon()
         }
      }
      Box(
         modifier = Modifier.padding(start = if (navigationIcon != null) 12.dp else 0.dp)
      ) {
         CompositionLocalProvider(LocalContentColor provides colors.titleContentColor) {
            title()
         }
      }
      Spacer(modifier = Modifier.weight(1f))
      if (actions != null) {
         Row(
            verticalAlignment = Alignment.CenterVertically
         ) {
            CompositionLocalProvider(LocalContentColor provides colors.actionIconContentColor) {
               actions()
            }
         }
      }
   }
}
