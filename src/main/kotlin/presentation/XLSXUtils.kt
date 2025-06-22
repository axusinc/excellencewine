package presentation

import io.github.evanrupert.excelkt.ExcelElement
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors

object XLSXUtils {
    fun ExcelElement.redBackgroundStyle() = createCellStyle {
        fillForegroundColor = IndexedColors.RED.index // Red color
        fillPattern = FillPatternType.SOLID_FOREGROUND
    }
    fun ExcelElement.aquaBackgroundStyle() = createCellStyle {
        fillForegroundColor = IndexedColors.AQUA.index // Red color
        fillPattern = FillPatternType.SOLID_FOREGROUND
    }
    fun ExcelElement.greenBackgroundStyle() = createCellStyle {
        fillForegroundColor = IndexedColors.LIGHT_GREEN.index // Green color
        fillPattern = FillPatternType.SOLID_FOREGROUND
    }
    fun ExcelElement.redFontStyle() = createFont {
        color = IndexedColors.RED.index
    }
    fun ExcelElement.redFontCellStyle() = createCellStyle {
        setFont(redFontStyle())
    }
    fun ExcelElement.boldFontStyle() = createFont {
        bold = true
    }
    fun ExcelElement.boldFontCellStyle() = createCellStyle {
        setFont(boldFontStyle())
    }
}