package me.neko.nzhelper.core.export

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DocxReportRenderer {

    private const val ACCENT = "6750A4"
    private const val MUTED = "757575"
    private const val BORDER = "D9D9D9"
    private const val HEADER_BG = "F1ECF8"

    fun render(doc: ReportDocument): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.writeEntry("[Content_Types].xml", CONTENT_TYPES)
            zip.writeEntry("_rels/.rels", ROOT_RELS)
            zip.writeEntry("word/_rels/document.xml.rels", DOCUMENT_RELS)
            zip.writeEntry("word/styles.xml", styles())
            zip.writeEntry("word/document.xml", document(doc))
        }
        return out.toByteArray()
    }

    private fun ZipOutputStream.writeEntry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private val CONTENT_TYPES = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
        <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
        <Default Extension="xml" ContentType="application/xml"/>
        <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
        <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
        </Types>
    """.trimIndent()

    private val ROOT_RELS = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
        <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
        </Relationships>
    """.trimIndent()

    private val DOCUMENT_RELS = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
        <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
        </Relationships>
    """.trimIndent()

    private fun styles(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
        <w:docDefaults>
        <w:rPrDefault><w:rPr><w:sz w:val="20"/><w:szCs w:val="20"/></w:rPr></w:rPrDefault>
        <w:pPrDefault><w:pPr><w:spacing w:after="120" w:line="264" w:lineRule="auto"/></w:pPr></w:pPrDefault>
        </w:docDefaults>
        <w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/></w:style>
        <w:style w:type="paragraph" w:styleId="ReportTitle"><w:name w:val="Report Title"/><w:basedOn w:val="Normal"/>
        <w:pPr><w:jc w:val="center"/><w:spacing w:before="120" w:after="80"/></w:pPr>
        <w:rPr><w:b/><w:color w:val="212121"/><w:sz w:val="44"/><w:szCs w:val="44"/></w:rPr></w:style>
        <w:style w:type="paragraph" w:styleId="ReportSubTitle"><w:name w:val="Report Subtitle"/><w:basedOn w:val="Normal"/>
        <w:pPr><w:jc w:val="center"/><w:spacing w:after="240"/><w:pBdr><w:bottom w:val="single" w:sz="6" w:space="6" w:color="$BORDER"/></w:pBdr></w:pPr>
        <w:rPr><w:color w:val="$MUTED"/><w:sz w:val="18"/><w:szCs w:val="18"/></w:rPr></w:style>
        <w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="heading 1"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/>
        <w:pPr><w:keepNext/><w:outlineLvl w:val="0"/><w:spacing w:before="320" w:after="140"/></w:pPr>
        <w:rPr><w:b/><w:color w:val="212121"/><w:sz w:val="28"/><w:szCs w:val="28"/></w:rPr></w:style>
        <w:style w:type="paragraph" w:styleId="Heading2"><w:name w:val="heading 2"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/>
        <w:pPr><w:keepNext/><w:outlineLvl w:val="1"/><w:spacing w:before="220" w:after="100"/></w:pPr>
        <w:rPr><w:b/><w:color w:val="$ACCENT"/><w:sz w:val="24"/><w:szCs w:val="24"/></w:rPr></w:style>
        <w:style w:type="paragraph" w:styleId="Body"><w:name w:val="Report Body"/><w:basedOn w:val="Normal"/>
        <w:rPr><w:sz w:val="19"/><w:szCs w:val="19"/></w:rPr></w:style>
        <w:style w:type="paragraph" w:styleId="Muted"><w:name w:val="Report Muted"/><w:basedOn w:val="Normal"/>
        <w:rPr><w:color w:val="$MUTED"/><w:sz w:val="18"/><w:szCs w:val="18"/></w:rPr></w:style>
        <w:style w:type="paragraph" w:styleId="Cell"><w:name w:val="Report Cell"/><w:basedOn w:val="Normal"/>
        <w:pPr><w:spacing w:before="20" w:after="20" w:line="240" w:lineRule="auto"/></w:pPr>
        <w:rPr><w:sz w:val="18"/><w:szCs w:val="18"/></w:rPr></w:style>
        <w:style w:type="paragraph" w:styleId="CellHead"><w:name w:val="Report Cell Head"/><w:basedOn w:val="Cell"/>
        <w:rPr><w:b/></w:rPr></w:style>
        <w:style w:type="paragraph" w:styleId="Compact"><w:name w:val="Report Compact"/><w:basedOn w:val="Normal"/>
        <w:pPr><w:spacing w:before="0" w:after="0" w:line="240" w:lineRule="auto"/></w:pPr>
        <w:rPr><w:sz w:val="8"/><w:szCs w:val="8"/></w:rPr></w:style>
        </w:styles>
    """.trimIndent()

    private fun document(doc: ReportDocument): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">")
        append("<w:body>")
        append(paragraph(doc.title, "ReportTitle"))
        append(paragraph(doc.subtitle, "ReportSubTitle"))
        for (block in doc.blocks) {
            when (block) {
                is ReportBlock.Heading ->
                    append(paragraph(block.text, if (block.level == 1) "Heading1" else "Heading2"))

                is ReportBlock.Paragraph ->
                    append(paragraph(block.text, if (block.muted) "Muted" else "Body"))

                is ReportBlock.Table -> append(table(block))
            }
        }
        // A4 + 2cm 页边距
        append("<w:sectPr><w:pgSz w:w=\"11906\" w:h=\"16838\"/>")
        append("<w:pgMar w:top=\"1134\" w:right=\"1134\" w:bottom=\"1134\" w:left=\"1134\" ")
        append("w:header=\"720\" w:footer=\"720\" w:gutter=\"0\"/></w:sectPr>")
        append("</w:body></w:document>")
    }

    private fun paragraph(text: String, styleId: String): String = buildString {
        append("<w:p><w:pPr><w:pStyle w:val=\"$styleId\"/></w:pPr>")
        if (text.isNotEmpty()) {
            text.split('\n').forEachIndexed { index, line ->
                if (index > 0) append("<w:r><w:br/></w:r>")
                if (line.isNotEmpty()) {
                    append("<w:r><w:t xml:space=\"preserve\">")
                    append(xmlEscape(line))
                    append("</w:t></w:r>")
                }
            }
        }
        append("</w:p>")
    }

    private fun table(t: ReportBlock.Table): String = buildString {
        val columnCount = t.headers.size
        val totalWeight = t.weights.sum()
        // pct 单位为 1/50 百分比（5000 = 100%）
        val widths = List(columnCount) { i ->
            val weight = t.weights.getOrElse(i) { 1f }
            (weight / totalWeight * 5000f).toInt().coerceIn(100, 4800)
        }

        append("<w:tbl><w:tblPr><w:tblW w:w=\"5000\" w:type=\"pct\"/><w:tblBorders>")
        for (edge in listOf("top", "left", "bottom", "right", "insideH", "insideV")) {
            append("<w:$edge w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"$BORDER\"/>")
        }
        append("</w:tblBorders><w:tblCellMar>")
        append("<w:top w:w=\"40\" w:type=\"dxa\"/><w:left w:w=\"80\" w:type=\"dxa\"/>")
        append("<w:bottom w:w=\"40\" w:type=\"dxa\"/><w:right w:w=\"80\" w:type=\"dxa\"/>")
        append("</w:tblCellMar></w:tblPr><w:tblGrid>")
        // tblGrid 单位 dxa，仅作参考，实际宽度以 tcW pct 为准
        widths.forEach { append("<w:gridCol w:w=\"${it * 9638 / 5000}\"/>") }
        append("</w:tblGrid>")

        // 表头（tblHeader：跨页时重复）
        append("<w:tr><w:trPr><w:tblHeader/></w:trPr>")
        t.headers.forEachIndexed { i, head -> append(cell(head, widths[i], header = true)) }
        append("</w:tr>")

        for (row in t.rows) {
            append("<w:tr>")
            for (i in 0 until columnCount) {
                append(cell(row.getOrElse(i) { "" }, widths[i]))
            }
            append("</w:tr>")
        }
        append("</w:tbl>")
        // Word 不允许两个表格直接相邻，且文档不能以表格结尾
        append("<w:p><w:pPr><w:pStyle w:val=\"Compact\"/></w:pPr></w:p>")
    }

    private fun cell(text: String, widthPct: Int, header: Boolean = false): String = buildString {
        append("<w:tc><w:tcPr><w:tcW w:w=\"$widthPct\" w:type=\"pct\"/>")
        if (header) append("<w:shd w:val=\"clear\" w:color=\"auto\" w:fill=\"$HEADER_BG\"/>")
        append("</w:tcPr>")
        append(paragraph(text, if (header) "CellHead" else "Cell"))
        append("</w:tc>")
    }

    private fun xmlEscape(text: String): String {
        val sb = StringBuilder(text.length + 16)
        for (char in text) {
            when {
                char == '&' -> sb.append("&amp;")
                char == '<' -> sb.append("&lt;")
                char == '>' -> sb.append("&gt;")
                char == '\"' -> sb.append("&quot;")
                char == '\'' -> sb.append("&apos;")
                // XML 1.0 非法控制字符直接丢弃，避免整个文档打不开
                char.code < 0x20 && char != '\t' && char != '\n' && char != '\r' -> Unit
                else -> sb.append(char)
            }
        }
        return sb.toString()
    }
}
