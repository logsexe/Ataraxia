package app.ataraxia.social

import android.content.Context
import android.util.AttributeSet
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AtaraxiaColors = darkColorScheme(
    primary = Color(0xFF7FD29B),
    onPrimary = Color(0xFF0B1411),
    primaryContainer = Color(0xFF20332A),
    onPrimaryContainer = Color(0xFFBCE8C9),
    background = Color(0xFF0B1411),
    onBackground = Color(0xFFF3F0E7),
    surface = Color(0xFF111E19),
    onSurface = Color(0xFFF3F0E7),
    surfaceVariant = Color(0xFF182721),
    onSurfaceVariant = Color(0xFF9FB1A7),
    outline = Color(0xFF2D4137)
)

@Composable
private fun AtaraxiaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AtaraxiaColors, content = content)
}

class AtaraxiaTopBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {
    private var mode by mutableStateOf("FOCUSED")
    private var summary by mutableStateOf("Ready")

    fun update(modeLabel: String, summaryText: String) {
        mode = modeLabel
        summary = summaryText
    }

    @Composable
    override fun Content() = AtaraxiaTheme {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).semantics { heading() }) {
                    Text(
                        text = "ATARAXIA",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        letterSpacing = 2.4.sp
                    )
                    Text(
                        text = "SOCIAL MEDIA, IN MEASURE",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        letterSpacing = 1.7.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        text = mode,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.1.sp
                    )
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(15.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Text(
                    text = summary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

class AtaraxiaBottomBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {
    private var selected by mutableStateOf("Home")
    private var homeAction: Runnable? = null
    private var inboxAction: Runnable? = null
    private var feedAction: Runnable? = null
    private var settingsAction: Runnable? = null

    fun setActions(home: Runnable, inbox: Runnable, feed: Runnable, settings: Runnable) {
        homeAction = home
        inboxAction = inbox
        feedAction = feed
        settingsAction = settings
    }

    fun select(label: String) {
        selected = label
    }

    @Composable
    override fun Content() = AtaraxiaTheme {
        val destinations = listOf(
            Triple("Home", "H", homeAction),
            Triple("Inbox", "I", inboxAction),
            Triple("Feed", "F", feedAction),
            Triple("Settings", "S", settingsAction)
        )
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                modifier = Modifier.height(72.dp)
            ) {
                destinations.forEach { (label, glyph, action) ->
                    val active = selected == label
                    val iconColor by animateColorAsState(
                        if (active) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "navigationColor"
                    )
                    NavigationBarItem(
                        selected = active,
                        onClick = { action?.run() },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(
                                        if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(glyph, color = iconColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        label = { Text(label, fontSize = 11.sp, maxLines = 1) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = Color.Transparent,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}
