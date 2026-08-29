package me.neko.nzhelper.core.export

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.withClip
import androidx.core.graphics.withTranslation
import java.io.ByteArrayOutputStream

object PdfReportRenderer {

    // A4 @ 72dpi
    private const val PAGE_WIDTH = 595f
    private const val PAGE_HEIGHT = 842f
    private const val MARGIN_X = 42f
    private const val MARGIN_TOP = 46f
    private const val MARGIN_BOTTOM = 54f
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN_X * 2f
    private const val CONTENT_HEIGHT = PAGE_HEIGHT - MARGIN_TOP - MARGIN_BOTTOM

    private const val ACCENT = 0xFF6750A4.toInt()
    private const val TEXT_COLOR = 0xFF212121.toInt()
    private const val MUTED_COLOR = 0xFF757575.toInt()
    private const val BORDER_COLOR = 0xFFD9D9D9.toInt()
    private const val HEADER_BG = 0xFFF1ECF8.toInt()

    private const val TABLE_FONT_SIZE = 8.6f
    private const val CELL_PADDING_X = 5f
    private const val CELL_PADDING_Y = 4f
    private const val MIN_ROW_HEIGHT = 19f

    fun render(doc: ReportDocument): ByteArray {
        val pages = Paginator(doc).paginate()
        val pdf = PdfDocument()
        try {
            pages.forEachIndexed { index, page ->
                val info = PdfDocument.PageInfo.Builder(
                    PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), index + 1
                ).create()
                val pageObject = pdf.startPage(info)
                page.draw(pageObject.canvas)
                drawFooter(pageObject.canvas, index + 1, pages.size)
                pdf.finishPage(pageObject)
            }
            val out = ByteArrayOutputStream()
            pdf.writeTo(out)
            return out.toByteArray()
        } finally {
            pdf.close()
        }
    }

    private fun drawFooter(canvas: Canvas, page: Int, total: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = MUTED_COLOR
            textSize = 8f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "第 $page / $total 页",
            PAGE_WIDTH / 2f,
            PAGE_HEIGHT - 26f,
            paint
        )
    }

    private fun textPaint(size: Float, bold: Boolean, color: Int) =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

    private fun makeLayout(text: String, paint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.15f)
            .setIncludePad(false)
            .build()

    private class PageContent {
        val ops = mutableListOf<(Canvas) -> Unit>()
        fun draw(canvas: Canvas) = ops.forEach { it(canvas) }
    }

    private class Paginator(private val doc: ReportDocument) {

        private val pages = mutableListOf<PageContent>()
        private var current = PageContent()
        private var y = MARGIN_TOP

        private val bottom: Float get() = PAGE_HEIGHT - MARGIN_BOTTOM

        fun paginate(): List<PageContent> {
            drawTitle()
            for (block in doc.blocks) {
                when (block) {
                    is ReportBlock.Heading -> heading(block)
                    is ReportBlock.Paragraph -> paragraph(block)
                    is ReportBlock.Table -> table(block)
                }
            }
            pages += current
            return pages
        }

        private fun newPage() {
            pages += current
            current = PageContent()
            y = MARGIN_TOP
        }

        /// 剩余空间不足时换页；返回是否换页（换页后不再补段前间距）
        private fun ensure(height: Float): Boolean {
            if (y + height <= bottom) return false
            newPage()
            return true
        }

        private fun drawTitle() {
            val titlePaint = textPaint(22f, bold = true, color = TEXT_COLOR).apply {
                textAlign = Paint.Align.CENTER
            }
            val fm = titlePaint.fontMetrics
            val titleBaseY = y - fm.ascent
            current.ops += { c ->
                c.drawText(doc.title, PAGE_WIDTH / 2f, titleBaseY, titlePaint)
            }
            y += fm.descent - fm.ascent + 4f

            val subPaint = textPaint(9f, bold = false, color = MUTED_COLOR).apply {
                textAlign = Paint.Align.CENTER
            }
            val subFm = subPaint.fontMetrics
            val subBaseY = y - subFm.ascent
            current.ops += { c ->
                c.drawText(doc.subtitle, PAGE_WIDTH / 2f, subBaseY, subPaint)
            }
            y += subFm.descent - subFm.ascent + 10f

            val linePaint = Paint().apply {
                color = BORDER_COLOR
                strokeWidth = 1f
            }
            val lineY = y
            current.ops += { c ->
                c.drawLine(MARGIN_X, lineY, PAGE_WIDTH - MARGIN_X, lineY, linePaint)
            }
            y += 16f
        }

        private fun heading(b: ReportBlock.Heading) {
            val isH1 = b.level == 1
            val paint = textPaint(
                size = if (isH1) 14f else 11.5f,
                bold = true,
                color = if (isH1) TEXT_COLOR else ACCENT
            )
            val fm = paint.fontMetrics
            val height = fm.descent - fm.ascent
            val before = if (isH1) 18f else 10f
            val after = if (isH1) 8f else 5f

            if (!ensure(before + height + after)) y += before

            if (isH1) {
                val barPaint = Paint().apply { color = ACCENT }
                val barTop = y + 1.5f
                val barBottom = y + height - 2f
                current.ops += { c ->
                    c.drawRect(RectF(MARGIN_X, barTop, MARGIN_X + 3.5f, barBottom), barPaint)
                }
            }
            val textX = if (isH1) MARGIN_X + 10f else MARGIN_X
            val baseY = y - fm.ascent
            current.ops += { c -> c.drawText(b.text, textX, baseY, paint) }
            y += height + after
        }

        private fun paragraph(b: ReportBlock.Paragraph) {
            val paint = textPaint(9.6f, bold = false, if (b.muted) MUTED_COLOR else TEXT_COLOR)
            val layout = makeLayout(b.text, paint, CONTENT_WIDTH.toInt())
            ensure(layout.height + 9f)
            val top = y
            current.ops += { c ->
                c.withTranslation(MARGIN_X, top) {
                    layout.draw(this)
                }
            }
            y += layout.height + 9f
        }

        private fun table(t: ReportBlock.Table) {
            val totalWeight = t.weights.sum()
            val colWidths = t.weights.map { it / totalWeight * CONTENT_WIDTH }
            val headPaint = textPaint(TABLE_FONT_SIZE, bold = true, color = TEXT_COLOR)
            val cellPaint = textPaint(TABLE_FONT_SIZE, bold = false, color = TEXT_COLOR)
            val columnCount = t.headers.size

            // 避免表头孤立在页尾
            ensure(6f + MIN_ROW_HEIGHT * 2f)

            fun headerHeight(): Float {
                val layouts = cellLayouts(t.headers, colWidths, headPaint, capHeight = null)
                return rowHeight(layouts)
            }

            fun drawHeader() {
                val layouts = cellLayouts(t.headers, colWidths, headPaint, capHeight = null)
                val height = rowHeight(layouts)
                ensure(height)
                drawRow(layouts, colWidths, height, HEADER_BG)
            }

            y += 6f
            drawHeader()

            // 单行高度上限：换页后也要放得下「表头 + 该行」
            val maxCellHeight = CONTENT_HEIGHT - headerHeight() - 2 * CELL_PADDING_Y - 4f

            for (rawRow in t.rows) {
                val row = rawRow.take(columnCount).let {
                    if (it.size < columnCount) it + List(columnCount - it.size) { "" } else it
                }
                val layouts = cellLayouts(row, colWidths, cellPaint, capHeight = maxCellHeight)
                val height = rowHeight(layouts)
                if (y + height > bottom) {
                    newPage()
                    drawHeader()
                }
                drawRow(layouts, colWidths, height, null)
            }
            y += 8f
        }

        private fun cellLayouts(
            cells: List<String>,
            colWidths: List<Float>,
            paint: TextPaint,
            capHeight: Float?
        ): List<StaticLayout> = cells.mapIndexed { index, text ->
            val width = (colWidths[index] - 2 * CELL_PADDING_X).toInt().coerceAtLeast(8)
            var content = text
            var layout = makeLayout(content, paint, width)
            if (capHeight != null) {
                while (layout.height > capHeight && content.length > 8) {
                    content = content
                        .dropLast(minOf(24, content.length / 2))
                        .trimEnd() + "…"
                    layout = makeLayout(content, paint, width)
                }
            }
            layout
        }

        private fun rowHeight(layouts: List<StaticLayout>): Float =
            ((layouts.maxOfOrNull { it.height } ?: 0) + 2 * CELL_PADDING_Y)
                .coerceAtLeast(MIN_ROW_HEIGHT)

        private fun drawRow(
            layouts: List<StaticLayout>,
            colWidths: List<Float>,
            rowHeight: Float,
            backgroundColor: Int?
        ) {
            val top = y
            if (backgroundColor != null) {
                val fill = Paint().apply { color = backgroundColor }
                current.ops += { c ->
                    c.drawRect(
                        RectF(MARGIN_X, top, MARGIN_X + CONTENT_WIDTH, top + rowHeight),
                        fill
                    )
                }
            }
            val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = BORDER_COLOR
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
            }
            current.ops += { c ->
                var x = MARGIN_X
                layouts.forEachIndexed { index, layout ->
                    c.drawRect(RectF(x, top, x + colWidths[index], top + rowHeight), border)
                    c.withClip(
                        x + 0.8f, top + 0.8f,
                        x + colWidths[index] - 0.8f, top + rowHeight - 0.8f
                    ) {
                        val offset =
                            ((rowHeight - 2 * CELL_PADDING_Y - layout.height) / 2f).coerceAtLeast(0f)
                        translate(x + CELL_PADDING_X, top + CELL_PADDING_Y + offset)
                        layout.draw(this)
                    }
                    x += colWidths[index]
                }
            }
            y += rowHeight
        }
    }
}
