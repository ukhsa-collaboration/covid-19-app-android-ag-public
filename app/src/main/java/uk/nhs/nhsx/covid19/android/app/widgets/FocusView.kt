package uk.nhs.nhsx.covid19.android.app.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.util.dpToPx

class FocusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var internalCanvas: Canvas? = null
    private var bitmap: Bitmap? = null
    private val backgroundPaint = Paint().apply {
        isAntiAlias = true
    }
    private val transparentPaint = Paint()
    private val paint = Paint()
    private val rect = RectF()
    private val focusPaddingTop: Int
    private val focusPaddingBottom: Int
    private val focusPaddingHorizontal: Int
    private val focusBorderRadius: Float

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.FocusView,
            0, 0
        ).apply {
            try {
                focusPaddingTop =
                    getDimensionPixelSize(
                        R.styleable.FocusView_focusPaddingTop,
                        0.dpToPx.toInt()
                    )
                focusPaddingBottom =
                    getDimensionPixelSize(
                        R.styleable.FocusView_focusPaddingBottom,
                        0.dpToPx.toInt()
                    )
                focusPaddingHorizontal =
                    getDimensionPixelSize(
                        R.styleable.FocusView_focusPaddingHorizontal,
                        0.dpToPx.toInt()
                    )
                focusBorderRadius =
                    getDimension(R.styleable.FocusView_focusBorderRadius, 14.dpToPx)
                backgroundPaint.color = getColor(
                    R.styleable.FocusView_dimColor,
                    context.getColor(R.color.qr_code_overlay)
                )
                backgroundPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)

                transparentPaint.color =
                    ContextCompat.getColor(context, android.R.color.transparent)
                transparentPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            } finally {
                recycle()
            }
        }
    }

    @SuppressLint("CanvasSize")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (bitmap == null || internalCanvas == null) {
            bitmap?.recycle()

            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            internalCanvas = Canvas(bitmap!!)
        }

        val c = internalCanvas ?: return
        c.drawRect(0f, 0f, c.width.toFloat(), c.height.toFloat(), backgroundPaint)
        rect.set(
            focusPaddingHorizontal.toFloat(),
            focusPaddingTop.toFloat(),
            (width - focusPaddingHorizontal).toFloat(),
            (height - focusPaddingBottom).toFloat()
        )
        c.drawRoundRect(rect, focusBorderRadius, focusBorderRadius, transparentPaint)

        canvas.drawBitmap(bitmap!!, 0f, 0f, paint)
    }
}
