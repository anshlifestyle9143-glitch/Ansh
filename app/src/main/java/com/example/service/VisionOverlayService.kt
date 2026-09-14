package com.example.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.savedstate.ViewTreeSavedStateRegistryOwner
import androidx.activity.ComponentActivity
import com.example.ui.overlay.VisionWakeOverlay
import com.example.ui.theme.VisionTheme
import com.example.ui.viewmodel.VisionViewModel

class VisionOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    private var viewModel: VisionViewModel? = null

    override fun onCreate() {
        super.onCreate()

        showOverlay()
    }

    private fun showOverlay() {

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        if (overlayView != null) {
            return
        }

        windowManager =
            getSystemService(
                WINDOW_SERVICE
            ) as WindowManager

        viewModel =
            VisionViewModel(
                application
            )

        val composeView =
            ComposeView(this)

        /*
         * Compose lifecycle owners.
         *
         * The overlay is not hosted by MainActivity,
         * so we provide the required owners manually.
         */
        ViewTreeLifecycleOwner
            .set(
                composeView,
                OverlayLifecycleOwner()
            )

        ViewTreeViewModelStoreOwner
            .set(
                composeView,
                OverlayViewModelStoreOwner()
            )

        ViewTreeSavedStateRegistryOwner
            .set(
                composeView,
                OverlaySavedStateOwner()
            )

        composeView.setContent {

            VisionTheme {

                VisionWakeOverlay(

                    context = this@VisionOverlayService,

                    viewModel =
                        viewModel
                            ?: return@VisionTheme,

                    onDismiss = {
                        stopOverlay()
                    }
                )
            }
        }

        overlayView =
            composeView

        val layoutType =
            if (
                Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.O
            ) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

        val params =
            WindowManager.LayoutParams(

                360,

                380,

                layoutType,

                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,

                PixelFormat.TRANSLUCENT
            )

        params.gravity =
            Gravity.BOTTOM or
                Gravity.CENTER_HORIZONTAL

        params.y = 0

        try {

            windowManager?.addView(
                composeView,
                params
            )

        } catch (_: Exception) {

            overlayView = null
            viewModel = null

            stopSelf()
        }
    }

    private fun stopOverlay() {

        try {

            overlayView?.let {
                windowManager?.removeView(it)
            }

        } catch (_: Exception) {
        }

        overlayView = null

        viewModel = null

        stopSelf()
    }

    override fun onDestroy() {

        try {

            overlayView?.let {
                windowManager?.removeView(it)
            }

        } catch (_: Exception) {
        }

        overlayView = null
        windowManager = null
        viewModel = null

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {
        return null
    }

    /*
     * ---------------------------------------------------------
     * Minimal lifecycle owners for ComposeView.
     * ---------------------------------------------------------
     *
     * These are intentionally kept inside this service so
     * MainActivity is never involved in the wake-word overlay.
     */

    private class OverlayLifecycleOwner :
        androidx.lifecycle.LifecycleOwner {

        private val registry =
            androidx.lifecycle.LifecycleRegistry(this)

        init {
            registry.handleLifecycleEvent(
                androidx.lifecycle.Lifecycle.Event.ON_CREATE
            )

            registry.handleLifecycleEvent(
                androidx.lifecycle.Lifecycle.Event.ON_START
            )

            registry.handleLifecycleEvent(
                androidx.lifecycle.Lifecycle.Event.ON_RESUME
            )
        }

        override val lifecycle:
            androidx.lifecycle.Lifecycle
            get() = registry
    }

    private class OverlayViewModelStoreOwner :
        androidx.lifecycle.ViewModelStoreOwner {

        private val store =
            androidx.lifecycle.ViewModelStore()

        override val viewModelStore:
            androidx.lifecycle.ViewModelStore
            get() = store
    }

    private class OverlaySavedStateOwner :
        androidx.savedstate.SavedStateRegistryOwner {

        private val lifecycleOwner =
            OverlayLifecycleOwner()

        private val controller =
            androidx.savedstate.SavedStateRegistryController
                .create(this)

        init {

            controller.performAttach()

            controller.performRestore(null)

            lifecycleOwner.lifecycle
        }

        override val lifecycle:
            androidx.lifecycle.Lifecycle
            get() = lifecycleOwner.lifecycle

        override val savedStateRegistry:
            androidx.savedstate.SavedStateRegistry
            get() = controller.savedStateHandlesController.savedStateRegistry
    }
}
