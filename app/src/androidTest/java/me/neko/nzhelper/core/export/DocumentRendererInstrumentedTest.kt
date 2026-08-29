package me.neko.nzhelper.core.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipInputStream

/// 渲染产物用系统 PdfRenderer 反向打开，验证是合法可读的文档
@RunWith(AndroidJUnit4::class)
class DocumentRendererInstrumentedTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    /// 加 publishArtifacts=true 运行时才把产物写入公共 Downloads（供 adb pull 视觉检查）
    private val publishArtifacts =
        InstrumentationRegistry.getArguments()?.getString("publishArtifacts") == "true"

    private fun publishToDownloads(name: String, mimeType: String, write: (OutputStream) -> Unit) {
        if (!publishArtifacts) return
        context.contentResolver.delete(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            "${MediaStore.Downloads.DISPLAY_NAME} = ?",
            arrayOf(name)
        )
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("无法插入 MediaStore")
        context.contentResolver.openOutputStream(uri)?.use(write)
            ?: throw IllegalStateException("无法打开输出流")
    }

    private fun sampleDoc(): ReportDocument {
        val rows = (1..120).map { i ->
            listOf(
                "2026-%02d-%02d 1%d:30".format(i % 12 + 1, i % 28 + 1, i % 10),
                if (i % 5 == 0) "1小时23分" else "${i % 30 + 1}分钟",
                "%.1f".format(i % 6 + 0.5f),
                if (i % 2 == 0) "是" else "否",
                "分类${i % 3 + 1}",
                "标签一、标签二、标签三",
                if (i % 7 == 0) "一条相当长的备注内容，用来验证单元格内文本自动换行之后行高是否正确扩展，以及跨页时是否正常分页。第 $i 条。"
                else "备注 $i"
            )
        }
        return ReportDocument(
            title = "NzHelper 数据报告",
            subtitle = "导出时间：2026-08-30 12:00:00 · 由 NzHelper 生成",
            blocks = listOf(
                ReportBlock.Heading("概览"),
                ReportBlock.Table(
                    headers = listOf("指标", "数值"),
                    rows = listOf(
                        listOf("记录总数", "120 次"),
                        listOf("时间范围", "2026-01-01 00:00 ~ 2026-08-30 12:00"),
                        listOf("总时长", "50小时20分"),
                        listOf("平均时长", "25 分钟"),
                        listOf("平均评分", "3.5")
                    ),
                    weights = listOf(1f, 2.2f)
                ),
                ReportBlock.Heading("分类统计", 2),
                ReportBlock.Table(
                    headers = listOf("分类", "次数", "总时长", "占比"),
                    rows = listOf(
                        listOf("分类1", "40 次", "16小时40分", "33.3%"),
                        listOf("分类2", "40 次", "16小时40分", "33.3%"),
                        listOf("分类3", "40 次", "16小时40分", "33.4%")
                    ),
                    weights = listOf(2f, 1f, 1.4f, 0.9f)
                ),
                ReportBlock.Heading("记录明细"),
                ReportBlock.Table(
                    headers = listOf("日期", "时长", "评分", "高潮", "分类", "标签", "备注"),
                    rows = rows,
                    weights = listOf(86f, 44f, 34f, 32f, 46f, 98f, 116f)
                )
            )
        )
    }

    @Test
    fun pdf多页渲染并可被系统PdfRenderer打开() {
        val bytes = PdfReportRenderer.render(sampleDoc())
        assertTrue("应为 PDF 魔数开头", String(bytes, 0, 5, Charsets.US_ASCII) == "%PDF-")

        publishToDownloads("test_report.pdf", "application/pdf") { it.write(bytes) }
        val file = File(context.cacheDir, "test_report.pdf")
        file.writeBytes(bytes)

        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                assertTrue("120 行记录应产生多页", renderer.pageCount >= 2)
                for (pageIndex in 0 until minOf(2, renderer.pageCount)) {
                    renderer.openPage(pageIndex).use { page ->
                        val bitmap = Bitmap.createBitmap(
                            page.width, page.height, Bitmap.Config.ARGB_8888
                        )
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        publishToDownloads("test_page$pageIndex.png", "image/png") { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        bitmap.recycle()
                    }
                }
            }
        }
    }

    @Test
    fun docx包结构完整() {
        val bytes = DocxReportRenderer.render(sampleDoc())
        val names = mutableSetOf<String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                names += entry.name
                entry = zis.nextEntry
            }
        }
        assertTrue("[Content_Types].xml" in names)
        assertTrue("word/document.xml" in names)
        assertTrue("word/styles.xml" in names)

        publishToDownloads("test_report.docx", DOCX_MIME) { it.write(bytes) }
    }

    private val DOCX_MIME =
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

    @Test
    fun 真实数据端到端导出() {
        // 走真实数据库的完整管线（仅内存，不落盘）
        val modules = me.neko.nzhelper.core.model.BackupModules(
            sessions = true, recycleBin = true, taxonomy = true, aiConfig = false
        )
        val pdf = kotlinx.coroutines.runBlocking {
            DocumentExporter.export(context, DocumentExporter.Format.PDF, modules)
        }
        assertTrue(String(pdf, 0, 5, Charsets.US_ASCII) == "%PDF-")

        val docx = kotlinx.coroutines.runBlocking {
            DocumentExporter.export(context, DocumentExporter.Format.DOCX, modules)
        }
        assertTrue(docx.size > 500)
    }
}
