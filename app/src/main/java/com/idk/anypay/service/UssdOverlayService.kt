package com.idk.anypay.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.idk.anypay.ui.theme.AnyPayTheme
import com.idk.anypay.ui.theme.Shapes
import com.idk.anypay.ui.theme.Typography

// ─── Zinc / shadcn design tokens (hard-coded so the overlay needs no theme) ──
private val ZincBg      = Color(0xFF09090B)   // zinc-950
private val ZincSurface = Color(0xFF18181B)   // zinc-900
private val ZincBorder  = Color(0xFF27272A)   // zinc-800
private val ZincMuted   = Color(0xFF71717A)   // zinc-500
private val ZincSubtle  = Color(0xFF3F3F46)   // zinc-700
private val ZincFg      = Color(0xFFFAFAFA)   // zinc-50
private val AccentGreen = Color(0xFF22C55E)   // green-500

/**
 * Floating overlay service that covers USSD dialogs with a full-screen custom UI.
 */
class UssdOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private val messageState = mutableStateOf("Connecting to USSD...")

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    companion object {
        private const val TAG = "UssdOverlay"
        private var serviceInstance: UssdOverlayService? = null

        fun start(context: Context, initialMessage: String = "Connecting to USSD...") {
            Log.d(TAG, "Starting overlay service")
            context.startService(
                Intent(context, UssdOverlayService::class.java).apply {
                    putExtra("message", initialMessage)
                }
            )
        }

        fun updateMessage(message: String) {
            Log.d(TAG, "Updating overlay message: $message")
            serviceInstance?.messageState?.value = message
        }

        fun stop(context: Context) {
            Log.d(TAG, "Stopping overlay service")
            context.stopService(Intent(context, UssdOverlayService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        serviceInstance = this
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        createOverlayView()
        Log.d(TAG, "Overlay service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        messageState.value = intent?.getStringExtra("message") ?: "Processing..."
        return START_NOT_STICKY
    }

    private fun createOverlayView() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@UssdOverlayService)
            setViewTreeSavedStateRegistryOwner(this@UssdOverlayService)
            setContent {
                // Use MaterialTheme directly — AnyPayTheme cannot be used in a Service
                // because its SideEffect tries to cast context to Activity, which crashes.
                MaterialTheme(
                    typography = Typography,
                    shapes     = Shapes
                ) {
                    UssdOverlayContent(messageState)
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }

        windowManager?.addView(overlayView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        overlayView?.let { windowManager?.removeView(it) }
        overlayView = null
        serviceInstance = null
        Log.d(TAG, "Overlay service destroyed")
    }
}

// ─── Full-Screen Overlay UI ───────────────────────────────────────────────────

@Composable
fun UssdOverlayContent(messageState: State<String>) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ZincBg, Color(0xFF0C0C0F), ZincBg)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Top spacer ────────────────────────────────────────────────
            Spacer(Modifier.height(64.dp))

            // ── App brand icon ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(ZincSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.Lock,
                    contentDescription = null,
                    tint               = ZincFg,
                    modifier           = Modifier.size(32.dp)
                )
            }

            Spacer(Modifier.height(28.dp))

            // ── Title ─────────────────────────────────────────────────────
            Text(
                text      = "AnyPay",
                style     = MaterialTheme.typography.headlineMedium,
                color     = ZincFg,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text      = "Processing your USSD request",
                style     = MaterialTheme.typography.bodyMedium,
                color     = ZincMuted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            // ── Pulsing dots spinner ──────────────────────────────────────
            PulsingDots()

            Spacer(Modifier.height(40.dp))

            // ── Status card ───────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(ZincSurface)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text  = "STATUS",
                    style = MaterialTheme.typography.labelSmall,
                    color = ZincMuted
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text      = messageState.value,
                    style     = MaterialTheme.typography.bodyLarge,
                    color     = ZincFg,
                    textAlign = TextAlign.Start
                )
                Spacer(Modifier.height(10.dp))
                // thin animated progress bar
                AnimatedProgressBar()
            }

            Spacer(Modifier.weight(1f))

            // ── Footer ────────────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(bottom = 48.dp)
            ) {
                HorizontalDivider(
                    color    = ZincBorder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
                Text(
                    text  = "Do not press the back button",
                    style = MaterialTheme.typography.labelMedium,
                    color = ZincSubtle
                )
                Text(
                    text  = "Credentials are encrypted · Data never leaves your device",
                    style = MaterialTheme.typography.labelSmall,
                    color = ZincSubtle.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ─── Pulsing Dots Indicator ───────────────────────────────────────────────────

@Composable
private fun PulsingDots() {
    val dotCount = 3
    val baseDelay = 160

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(dotCount) { index ->
            val infiniteTransition = rememberInfiniteTransition(label = "dot$index")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue  = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation  = tween(durationMillis = 600, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(baseDelay * index)
                ),
                label = "dotScale$index"
            )
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue  = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation  = tween(durationMillis = 600, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(baseDelay * index)
                ),
                label = "dotAlpha$index"
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .scale(scale)
                    .alpha(alpha)
                    .clip(CircleShape)
                    .background(AccentGreen)
            )
        }
    }
}

// ─── Animated Indeterminate Progress Bar ─────────────────────────────────────

@Composable
private fun AnimatedProgressBar() {
    val infiniteTransition = rememberInfiniteTransition(label = "progressBar")
    val offsetFraction by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue  = 2f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progressOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(ZincBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .height(3.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(AccentGreen)
                .offset(x = (offsetFraction * 300).dp)   // slide across
        )
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(
    showBackground = true,
    showSystemUi  = true,
    name          = "USSD Overlay – Dark (default)",
    backgroundColor = 0xFF09090B
)
@Composable
private fun UssdOverlayDarkPreview() {
    AnyPayTheme(darkTheme = true, dynamicColor = false) {
        UssdOverlayContent(
            messageState = remember { mutableStateOf("Dialling *99# and navigating menu…") }
        )
    }
}

@Preview(
    showBackground  = true,
    showSystemUi    = true,
    name            = "USSD Overlay – Light bg preview",
    backgroundColor = 0xFFFFFFFF
)
@Composable
private fun UssdOverlayLightPreview() {
    // Overlay always renders dark by design; this just checks it on a light canvas
    AnyPayTheme(darkTheme = false, dynamicColor = false) {
        UssdOverlayContent(
            messageState = remember { mutableStateOf("Entering UPI PIN step…") }
        )
    }
}

@Preview(
    showBackground = true,
    name           = "USSD Overlay – Long message",
    backgroundColor = 0xFF09090B
)
@Composable
private fun UssdOverlayLongMessagePreview() {
    AnyPayTheme(darkTheme = true, dynamicColor = false) {
        UssdOverlayContent(
            messageState = remember {
                mutableStateOf("Waiting for bank server response. Please keep the screen on and do not navigate away.")
            }
        )
    }
}
