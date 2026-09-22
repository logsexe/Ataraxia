package app.ataraxia.social

import android.content.Context
import android.util.AttributeSet
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
private fun ScreenFrame(
    eyebrow: String,
    title: String,
    body: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(eyebrow, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(title, modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.displayMedium)
        Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable
private fun FullButton(label: String, onClick: () -> Unit, secondary: Boolean = false) {
    if (secondary) OutlinedButton(
        onClick = onClick, modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) { Text(label) }
    else Button(
        onClick = onClick, modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = MaterialTheme.shapes.medium
    ) { Text(label) }
}

@Composable
private fun ValueCard(label: String, value: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(icon, null, Modifier.padding(10.dp).size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(label, style = MaterialTheme.typography.titleLarge)
                Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ArrowForward, "Change $label", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

class AtaraxiaSettingsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {
    private var focused by mutableStateOf(true)
    private var posts by mutableStateOf(10)
    private var session by mutableStateOf(5)
    private var daily by mutableStateOf(15)
    private var actions: List<Runnable> = emptyList()

    fun update(focusedMode: Boolean, postLimit: Int, sessionMinutes: Int, dailyMinutes: Int) {
        focused = focusedMode; posts = postLimit; session = sessionMinutes; daily = dailyMinutes
    }

    fun setActions(
        postsAction: Runnable, sessionAction: Runnable, dailyAction: Runnable,
        modeAction: Runnable, privacyAction: Runnable, clearAction: Runnable,
        exportAction: Runnable, importAction: Runnable, philosophyAction: Runnable
    ) { actions = listOf(postsAction, sessionAction, dailyAction, modeAction, privacyAction, clearAction, exportAction, importAction, philosophyAction) }

    @Composable override fun Content() = AtaraxiaTheme {
        ScreenFrame(
            "YOUR PRACTICE", "Boundaries,\nchosen by you.",
            "Adjust the edges of the experience. Ataraxia supports your intention without pretending to control you."
        ) {
            ValueCard("Mode", if (focused) "Focused · inbox only" else "Balanced · following feed included", Icons.Filled.Settings) { actions.getOrNull(3)?.run() }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric("POSTS", "$posts", Modifier.weight(1f))
                Metric("SESSION", "$session min", Modifier.weight(1f))
                Metric("DAILY", "$daily min", Modifier.weight(1f))
            }
            ValueCard("Posts per session", "$posts posts", Icons.Filled.List) { actions.getOrNull(0)?.run() }
            ValueCard("Session length", "$session minutes", Icons.Filled.Email) { actions.getOrNull(1)?.run() }
            ValueCard("Daily allowance", "$daily minutes", Icons.Filled.Home) { actions.getOrNull(2)?.run() }
            Text("PRIVACY & PORTABILITY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
            ValueCard("Privacy and limitations", "What Ataraxia can—and cannot—protect", Icons.Filled.Info) { actions.getOrNull(4)?.run() }
            ValueCard("Clear Instagram login", "Remove cookies, website storage and cache", Icons.Filled.Delete) { actions.getOrNull(5)?.run() }
            ValueCard("Export settings", "Readable file; login and history excluded", Icons.Filled.Share) { actions.getOrNull(6)?.run() }
            ValueCard("Import settings", "Review before replacing boundaries", Icons.Filled.Refresh) { actions.getOrNull(7)?.run() }
            TextButton(onClick = { actions.getOrNull(8)?.run() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Philosophy and purpose")
            }
            Text("Free forever · No developer ads · No analytics", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }

    @Composable private fun Metric(label: String, value: String, modifier: Modifier) {
        Surface(modifier, color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.small) {
            Column(Modifier.padding(vertical = 14.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(value, style = MaterialTheme.typography.titleLarge)
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

class AtaraxiaPhilosophyView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {
    private var page by mutableStateOf(0)
    private var onboarding by mutableStateOf(false)
    private var next: Runnable? = null
    private var back: Runnable? = null
    private var focused: Runnable? = null
    private var balanced: Runnable? = null
    fun update(pageIndex: Int, isOnboarding: Boolean) { page = pageIndex; onboarding = isOnboarding }
    fun setActions(nextAction: Runnable, backAction: Runnable, focusedAction: Runnable, balancedAction: Runnable) {
        next = nextAction; back = backAction; focused = focusedAction; balanced = balancedAction
    }

    @Composable override fun Content() = AtaraxiaTheme {
        AnimatedContent(page, transitionSpec = {
            androidx.compose.animation.fadeIn(tween(AtaraxiaTokens.GentleMotion)) togetherWith
                androidx.compose.animation.fadeOut(tween(AtaraxiaTokens.QuickMotion))
        }, label = "philosophyPage") { current ->
            when (current) {
                0 -> ScreenFrame("ABOUT 4,000 WEEKS", "Time is the one thing\nyou cannot replace.",
                    "An 80-year life is roughly 4,174 weeks. The point is not fear; it is remembering that attention is how life is spent.") {
                    Thought("A FINITE LIFE", "Infinite feeds behave as though your time has no edge. Your life does.")
                    Thought("THE AIM", "Connect, create and respond—then return to the life beyond the screen.")
                    FullButton("Continue · What is Ataraxia?", onClick = { next?.run() })
                    if (!onboarding) FullButton("Back home", onClick = { back?.run() }, secondary = true)
                }
                1 -> ScreenFrame("ATARAXIA", "Freedom from\nunnecessary disturbance.",
                    "The ancient Greek ideal was not withdrawal. It was a steadier mind—less governed by noise, impulse and manufactured urgency.") {
                    Thought("ATTENTION", "Notice what is asking for your mind.")
                    Thought("INTENTION", "Open with a purpose.")
                    Thought("MODERATION", "Enough is a complete experience.")
                    Thought("AGENCY", "The boundary belongs to you.")
                    FullButton("Continue · Choose how to enter", onClick = { next?.run() })
                    FullButton("Back", onClick = { back?.run() }, secondary = true)
                }
                else -> ScreenFrame("CHOOSE WITH INTENTION", "What are you\nhere to do?",
                    "Both modes keep Reels and Explore blocked. Change modes and boundaries whenever you choose.") {
                    Thought("FOCUSED", "Messages only. The home feed stays unavailable.")
                    FullButton("Begin in Focused mode", onClick = { focused?.run() })
                    Thought("BALANCED", "Messages and a short following feed, contained by your limits.")
                    FullButton("Begin in Balanced mode", onClick = { balanced?.run() })
                    FullButton("Back", onClick = { back?.run() }, secondary = true)
                    Text("Ataraxia is free and has no developer ads or analytics. Meta still processes activity inside Instagram's website.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    @Composable private fun Thought(title: String, body: String) {
        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
            Column(Modifier.padding(20.dp)) {
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(7.dp))
                Text(body, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

class AtaraxiaEndView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {
    private var daily by mutableStateOf(false)
    private var inbox: Runnable? = null
    private var home: Runnable? = null
    fun update(dailyLimit: Boolean) { daily = dailyLimit }
    fun setActions(inboxAction: Runnable, homeAction: Runnable) { inbox = inboxAction; home = homeAction }

    @Composable override fun Content() = AtaraxiaTheme {
        ScreenFrame("A DELIBERATE END", "Enough\nfor now.",
            if (daily) "You have used today's browsing allowance. Your inbox is still available."
            else "You have reached the boundary you chose. Take ten minutes and return to what matters beyond the screen."
        ) {
            Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.CheckCircle, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(14.dp))
                    Text("Notice the impulse to continue.\nYou do not have to obey it.", style = MaterialTheme.typography.headlineMedium)
                }
            }
            FullButton("Go to inbox", onClick = { inbox?.run() })
            FullButton("Return home", onClick = { home?.run() }, secondary = true)
            Text("The aim is not less life online. It is more life chosen.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}