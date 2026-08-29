package me.neko.nzhelper.core.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

class DocxReportRendererTest {

    private fun sampleDoc() = ReportDocument(
        title = "NzHelper 数据报告",
        subtitle = "导出时间：2026-08-30 12:00:00 · 由 NzHelper 生成",
        blocks = listOf(
            ReportBlock.Heading("概览"),
            ReportBlock.Heading("分类统计", 2),
            ReportBlock.Paragraph("含特殊字符 <tag> & \"引号\" 以及换行\n第二行"),
            ReportBlock.Table(
                headers = listOf("指标", "数值"),
                rows = listOf(
                    listOf("记录总数", "3 次"),
                    listOf("备注", "第一行\n第二行")
                ),
                weights = listOf(1f, 2f)
            )
        )
    )

    private fun unzip(bytes: ByteArray): Map<String, ByteArray> {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entries[entry.name] = zis.readBytes()
                entry = zis.nextEntry
            }
        }
        return entries
    }

    @Test
    fun `docx 包含必需的 OOXML 条目`() {
        val entries = unzip(DocxReportRenderer.render(sampleDoc()))
        val names = entries.keys
        assertTrue("[Content_Types].xml" in names)
        assertTrue("_rels/.rels" in names)
        assertTrue("word/_rels/document.xml.rels" in names)
        assertTrue("word/styles.xml" in names)
        assertTrue("word/document.xml" in names)
    }

    @Test
    fun `所有 XML 条目均为良构 XML`() {
        val factory = DocumentBuilderFactory.newInstance()
        unzip(DocxReportRenderer.render(sampleDoc())).forEach { (name, content) ->
            if (name.endsWith(".xml") || name.endsWith(".rels")) {
                factory.newDocumentBuilder().parse(ByteArrayInputStream(content))
            }
        }
    }

    @Test
    fun `特殊字符被正确转义`() {
        val entries = unzip(DocxReportRenderer.render(sampleDoc()))
        val document = String(entries.getValue("word/document.xml"), Charsets.UTF_8)
        assertTrue(document.contains("&lt;tag&gt;"))
        assertTrue(document.contains("&amp;"))
        assertTrue(document.contains("&quot;引号&quot;"))
        assertTrue("未转义的 '<tag>' 不应出现" !in document)
    }

    @Test
    fun `表格包含表头重复与单元格内容`() {
        val entries = unzip(DocxReportRenderer.render(sampleDoc()))
        val document = String(entries.getValue("word/document.xml"), Charsets.UTF_8)
        assertTrue(document.contains("<w:tblHeader/>"))
        assertTrue(document.contains("记录总数"))
        assertTrue(document.contains("指标"))
    }

    @Test
    fun `行数据列数不足时自动补齐`() {
        val doc = ReportDocument(
            title = "t",
            subtitle = "s",
            blocks = listOf(
                ReportBlock.Table(
                    headers = listOf("A", "B", "C"),
                    rows = listOf(listOf("only-one")),
                    weights = listOf(1f, 1f, 1f)
                )
            )
        )
        val entries = unzip(DocxReportRenderer.render(doc))
        val document = String(entries.getValue("word/document.xml"), Charsets.UTF_8)
        // 每行 3 个 tc + 表头 3 个 tc = 6
        assertEquals(6, Regex("<w:tc>").findAll(document).count())
    }
}
