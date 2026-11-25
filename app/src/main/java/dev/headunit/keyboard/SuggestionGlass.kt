package dev.headunit.keyboard

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresApi
import java.util.function.Consumer
import kotlin.math.roundToInt

/** A non-interactive child surface behind the IME lets the compositor blur just the pill. */
@RequiresApi(Build.VERSION_CODES.S)
class SuggestionGlass(private val strip: View, private val onBlur: (Boolean) -> Unit) :
    ViewTreeObserver.OnPreDrawListener {
    private val manager = strip.context.getSystemService(WindowManager::class.java)
    private val location = IntArray(2)
    private var layer: Dialog? = null
    private var enabled = false
    private val listener = Consumer<Boolean> {
        enabled = it
        update()
    }

    fun attach() {
        strip.viewTreeObserver.addOnPreDrawListener(this)
        manager.addCrossWindowBlurEnabledListener(listener)
    }

    fun detach() {
        manager.removeCrossWindowBlurEnabledListener(listener)
        strip.viewTreeObserver.removeOnPreDrawListener(this)
        hide()
    }

    override fun onPreDraw(): Boolean {
        update()
        return true
    }

    private fun hide() {
        if (layer != null) {
            layer?.dismiss()
            layer = null
            onBlur(false)
        }
    }

    fun update() {
        if (!enabled || !strip.isAttachedToWindow || !strip.isShown ||
            strip.windowVisibility != View.VISIBLE || strip.width == 0 || strip.height == 0) {
            hide()
            return
        }
        strip.getLocationOnScreen(location)
        val dialog = layer ?: Dialog(strip.context, android.R.style.Theme_Translucent_NoTitleBar).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(View(strip.context))
            window!!.apply {
                setType(WindowManager.LayoutParams.TYPE_APPLICATION_MEDIA)
                addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
                clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                setGravity(Gravity.TOP or Gravity.LEFT)
                setDecorFitsSystemWindows(false)
                attributes = attributes.apply {
                    token = strip.windowToken
                    setFitInsetsTypes(0)
                    windowAnimations = 0
                    title = "Suggestion glass"
                }
                setBackgroundDrawable(GradientDrawable().apply {
                    setColor(Color.argb(1, 0, 0, 0))
                    cornerRadius = strip.height / 2f
                })
                setBackgroundBlurRadius((32 * strip.resources.displayMetrics.density).roundToInt())
            }
        }
        val window = dialog.window!!
        val p = window.attributes
        if (p.x != location[0] || p.y != location[1] || p.width != strip.width || p.height != strip.height) {
            p.x = location[0]
            p.y = location[1]
            p.width = strip.width
            p.height = strip.height
            window.attributes = p
        }
        if (layer == null) {
            try {
                dialog.show()
                layer = dialog
                onBlur(true)
            } catch (_: WindowManager.BadTokenException) {
                // The editor can close between traversal and window attachment.
            }
        }
    }
}
