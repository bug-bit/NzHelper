package me.neko.nzhelper.core.achievement

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation
import me.neko.nzhelper.core.model.AchievementProgress
import java.io.ByteArrayOutputStream

object AchievementShareRenderer {

    const val IMAGE_WIDTH = 1080

    private const val PADDING_X = 96f
    private const val TOP_PADDING = 108f
    private const val BOTTOM_PADDING = 96f
    private const val CONTENT_WIDTH = IMAGE_WIDTH - PADDING_X * 2f

    private const val EMBLEM_RADIUS = 126f
    private const val EMBLEM_TEXT_SIZE = 128f
    private const val EMBLEM_STROKE_WIDTH = 6f

    private const val GAP_AFTER_EMBLEM = 56f
    private const val GAP_AFTER_TITLE = 22f
    private const val GAP_AFTER_DESC = 64f
    private const val GAP_AFTER_CARD = 44f
    private const val GAP_AFTER_META = 26f

    private const val TITLE_TEXT_SIZE = 76f
    private const val BODY_TEXT_SIZE = 40f
    private const val META_TEXT_SIZE = 36f
    private const val WATERMARK_TEXT_SIZE = 30f
    private const val LABEL_TEXT_SIZE = 36f
    private const val VALUE_TEXT_SIZE = 40f

    private const val CARD_PADDING = 44f
    private const val CARD_RADIUS = 40f
    private const val BAR_HEIGHT = 16f
    private const val GAP_LABEL_TO_BAR = 30f
    private const val GAP_BAR_TO_DATE = 30f

    private const val GRADIENT_HEIGHT_RATIO = 0.62f

    data class Palette(
        val background: Int,
        val card: Int,
        val title: Int,
        val body: Int,
        val accent: Int
    )

    fun render(progress: AchievementProgress, palette: Palette): ByteArray {
        val achievement = progress.achievement

        val titlePaint = textPaint(TITLE_TEXT_SIZE, bold = true, color = palette.title)
        val bodyPaint = textPaint(BODY_TEXT_SIZE, bold = false, color = palette.body)
        val metaPaint = textPaint(META_TEXT_SIZE, bold = false, color = palette.body)
        val labelPaint = textPaint(LABEL_TEXT_SIZE, bold = false, color = palette.body)
        val valuePaint = textPaint(VALUE_TEXT_SIZE, bold = true, color = palette.accent)
        val watermarkPaint = textPaint(
            WATERMARK_TEXT_SIZE,
            bold = false,
            color = withAlpha(palette.body, 0x99)
        )
        val emblemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.accent
            textSize = EMBLEM_TEXT_SIZE
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val labelLeftPaint = Paint(labelPaint).apply { textAlign = Paint.Align.LEFT }
        val valueRightPaint = Paint(valuePaint).apply { textAlign = Paint.Align.RIGHT }
        val metaCenterPaint = Paint(metaPaint).apply { textAlign = Paint.Align.CENTER }
        val watermarkCenterPaint = Paint(watermarkPaint).apply { textAlign = Paint.Align.CENTER }

        val titleLayout = centerLayout(achievement.title, titlePaint, CONTENT_WIDTH.toInt())
        val descLayout = centerLayout(achievement.description, bodyPaint, CONTENT_WIDTH.toInt())

        val valueText =
            "${achievement.formatValue(progress.current)} / " +
                    "${achievement.formatValue(achievement.target)} ${achievement.unit}"
        val dateText = progress.unlockedDate?.let { "解锁于 $it" } ?: "已解锁"
        val metaText = "${achievement.category.label} · ${achievement.tier.label}级"

        val labelLineHeight = labelPaint.descent() - labelPaint.ascent()
        val cardHeight = CARD_PADDING + labelLineHeight + GAP_LABEL_TO_BAR + BAR_HEIGHT +
                GAP_BAR_TO_DATE + labelLineHeight + CARD_PADDING

        var cursor = TOP_PADDING
        val emblemTop = cursor
        cursor += EMBLEM_RADIUS * 2f + GAP_AFTER_EMBLEM
        val titleTop = cursor
        cursor += titleLayout.height + GAP_AFTER_TITLE
        val descTop = cursor
        cursor += descLayout.height + GAP_AFTER_DESC
        val cardTop = cursor
        cursor += cardHeight + GAP_AFTER_CARD
        val metaBaseline = cursor - metaPaint.ascent()
        cursor = metaBaseline + metaPaint.descent() + GAP_AFTER_META
        val watermarkBaseline = cursor - watermarkPaint.ascent()
        val height = (watermarkBaseline + watermarkPaint.descent() + BOTTOM_PADDING).toInt()

        val bitmap = createBitmap(IMAGE_WIDTH, height)
        try {
            val canvas = Canvas(bitmap)
            canvas.drawColor(palette.background)
            canvas.drawRect(
                0f,
                0f,
                IMAGE_WIDTH.toFloat(),
                height * GRADIENT_HEIGHT_RATIO,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f,
                        0f,
                        0f,
                        height * GRADIENT_HEIGHT_RATIO,
                        withAlpha(palette.accent, 0x55),
                        withAlpha(palette.accent, 0x00),
                        Shader.TileMode.CLAMP
                    )
                }
            )

            val centerX = IMAGE_WIDTH / 2f
            val emblemCenterY = emblemTop + EMBLEM_RADIUS
            canvas.drawCircle(
                centerX,
                emblemCenterY,
                EMBLEM_RADIUS,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(palette.accent, 0x26) }
            )
            canvas.drawCircle(
                centerX,
                emblemCenterY,
                EMBLEM_RADIUS,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = withAlpha(palette.accent, 0x9A)
                    style = Paint.Style.STROKE
                    strokeWidth = EMBLEM_STROKE_WIDTH
                }
            )
            canvas.drawText(
                achievement.tier.label,
                centerX,
                emblemCenterY - (emblemPaint.descent() + emblemPaint.ascent()) / 2f,
                emblemPaint
            )

            canvas.withTranslation(PADDING_X, titleTop) { titleLayout.draw(this) }
            canvas.withTranslation(PADDING_X, descTop) { descLayout.draw(this) }

            val cardRect = RectF(PADDING_X, cardTop, IMAGE_WIDTH - PADDING_X, cardTop + cardHeight)
            canvas.drawRoundRect(
                cardRect,
                CARD_RADIUS,
                CARD_RADIUS,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.card }
            )

            val innerLeft = cardRect.left + CARD_PADDING
            val innerRight = cardRect.right - CARD_PADDING
            val labelBaseline = cardRect.top + CARD_PADDING - labelPaint.ascent()
            canvas.drawText("进度", innerLeft, labelBaseline, labelLeftPaint)
            canvas.drawText(valueText, innerRight, labelBaseline, valueRightPaint)

            val barTop = labelBaseline + labelPaint.descent() + GAP_LABEL_TO_BAR
            val barRect = RectF(innerLeft, barTop, innerRight, barTop + BAR_HEIGHT)
            canvas.drawRoundRect(
                barRect,
                BAR_HEIGHT / 2f,
                BAR_HEIGHT / 2f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(palette.accent, 0x33) }
            )
            canvas.drawRoundRect(
                barRect,
                BAR_HEIGHT / 2f,
                BAR_HEIGHT / 2f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.accent }
            )

            canvas.drawText(
                dateText,
                innerLeft,
                barRect.bottom + GAP_BAR_TO_DATE - labelPaint.ascent(),
                labelLeftPaint
            )

            canvas.drawText(metaText, centerX, metaBaseline, metaCenterPaint)
            canvas.drawText("NzHelper", centerX, watermarkBaseline, watermarkCenterPaint)

            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            return out.toByteArray()
        } finally {
            bitmap.recycle()
        }
    }

    private fun textPaint(size: Float, bold: Boolean, color: Int): TextPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

    private fun centerLayout(text: String, paint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.2f)
            .setIncludePad(false)
            .build()

    private fun withAlpha(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or ((alpha.coerceIn(0, 255)) shl 24)
}
