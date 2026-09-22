package app.ataraxia.social

import android.content.Context
import android.util.AttributeSet
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun Eyebrow(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 1.8.sp
    )
}

@Composable
internal fun PrimaryAction(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
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
                        "ATARAXIA",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        letterSpacing = 2.4.sp
                    )
                    Text(
                        "SOCIAL MEDIA, IN MEASURE",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        mode,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.1.sp
                    )
                }
            }
            Text(
                text = summary,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 5.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                // Keep the WebView viewport stable as the counter/timer wraps.
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .7f))
        }
    }
}

class AtaraxiaLandingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {
    private var enterAction: Runnable? = null
    private var philosophyAction: Runnable? = null

    fun setActions(enter: Runnable, philosophy: Runnable) {
        enterAction = enter
        philosophyAction = philosophy
    }

    @Composable
    override fun Content() = AtaraxiaTheme {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 44.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Eyebrow("A FINITE LIFE")
            Text(
                text = "You have about\n4,000 weeks.",
                modifier = Modifier.semantics { heading() },
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Light,
                fontSize = 43.sp,
                lineHeight = 48.sp,
                letterSpacing = (-1).sp
            )
            Text(
                text = "Attention is how those weeks are spent. Pause before entering, choose why you are here, and leave when the purpose is complete.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 17.sp,
                lineHeight = 26.sp
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Eyebrow("ATARAXIA · ἀταραξία")
                    Text(
                        text = "A calm, untroubled mind—freedom from unnecessary disturbance.",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        lineHeight = 23.sp
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Principle("ATTENTION", "Notice what is asking for your mind.")
            Principle("INTENTION", "Enter for connection, not compulsion.")
            Principle("ENOUGH", "A complete moment does not need to continue forever.")
            Spacer(Modifier.height(8.dp))
            PrimaryAction("Enter with intention") { enterAction?.run() }
            TextButton(
                onClick = { philosophyAction?.run() },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Explore the philosophy", color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    @Composable
    private fun Principle(title: String, body: String) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                Modifier.padding(top = 7.dp).size(7.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
            Column {
                Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 21.sp)
            }
        }
    }
}

class AtaraxiaHomeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {
    private var focused by mutableStateOf(true)
    private var summary by mutableStateOf("")
    private var progress by mutableStateOf(0f)
    private var postLimit by mutableStateOf(10)
    private var sessionMinutes by mutableStateOf(5)
    private var dailyMinutes by mutableStateOf(15)
    private var inboxAction: Runnable? = null
    private var feedAction: Runnable? = null
    private var modeAction: Runnable? = null
    private var philosophyAction: Runnable? = null

    fun update(
        focusedMode: Boolean,
        summaryText: String,
        dailyProgress: Float,
        posts: Int,
        session: Int,
        daily: Int
    ) {
        focused = focusedMode
        summary = summaryText
        progress = dailyProgress.coerceIn(0f, 1f)
        postLimit = posts
        sessionMinutes = session
        dailyMinutes = daily
    }

    fun setActions(inbox: Runnable, feed: Runnable, mode: Runnable, philosophy: Runnable) {
        inboxAction = inbox
        feedAction = feed
        modeAction = mode
        philosophyAction = philosophy
    }

    @Composable
    override fun Content() = AtaraxiaTheme {
        val animatedProgress by animateFloatAsState(progress, label = "dailyAllowance")
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 36.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Eyebrow("BEGIN WITH A PURPOSE")
            Text(
                text = "What are you\nhere to do?",
                modifier = Modifier.semantics { heading() },
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Light,
                fontSize = 38.sp,
                lineHeight = 43.sp,
                letterSpacing = (-.7).sp
            )
            Text(
                text = if (focused) "Take one breath. Messages are available; the feed can wait."
                else "Take one breath. Choose connection, then honour the ending you set.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
                lineHeight = 24.sp
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Text(summary, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxWidth().height(5.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outline
                    )
                }
            }
            IntentAction(
                title = "Messages",
                body = "Connect without using your feed allowance.",
                action = "Open inbox",
                onClick = { inboxAction?.run() }
            )
            AnimatedVisibility(visible = !focused) {
                IntentAction(
                    title = "Following feed",
                    body = "A finite view of people you follow. Recognised suggestions are hidden.",
                    action = "Open feed",
                    onClick = { feedAction?.run() }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("$postLimit posts · $sessionMinutes min", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                    Text("$dailyMinutes min daily boundary", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                TextButton(onClick = { modeAction?.run() }) {
                    Text(if (focused) "Focused" else "Balanced", color = MaterialTheme.colorScheme.primary)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Text(
                text = "“Enough” is not a failure to continue. It is the freedom to return to your life.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )
            TextButton(onClick = { philosophyAction?.run() }) {
                Text("Why Ataraxia?", color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    @Composable
    private fun IntentAction(title: String, body: String, action: String, onClick: () -> Unit) {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(5.dp))
                Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp)
                Spacer(Modifier.height(12.dp))
                Text("$action  →", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

class AtaraxiaBottomBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {
    private var selected by mutableStateOf("Inbox")
    private var inboxAction: Runnable? = null
    private var feedAction: Runnable? = null
    private var settingsAction: Runnable? = null

    fun setActions(inbox: Runnable, feed: Runnable, settings: Runnable) {
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
            NavItem("Inbox", Icons.Filled.Email, inboxAction),
            NavItem("Feed", Icons.Filled.List, feedAction),
            NavItem("Settings", Icons.Filled.Settings, settingsAction)
        )
        Surface(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                destinations.forEach { destination ->
                    val active = selected == destination.label
                    val itemColor by animateColorAsState(
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "navigationColor"
                    )
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight()
                            .clickable { destination.action?.run() },
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        ) {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp).size(22.dp),
                                tint = itemColor
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            destination.label,
                            color = itemColor,
                            fontSize = 11.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    private data class NavItem(val label: String, val icon: ImageVector, val action: Runnable?)
}
