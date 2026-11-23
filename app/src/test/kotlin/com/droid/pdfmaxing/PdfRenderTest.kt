package com.droid.pdfmaxing

import org.junit.Test
import java.io.File

class PdfRenderTest {
    @Test
    fun testAssetsExist() {
        val file = File("src/main/assets/sample_guide.pdf")
        println("Sample guide size: ${file.length()}")
        assert(file.exists())
    }
}
