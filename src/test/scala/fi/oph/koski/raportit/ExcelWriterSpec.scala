package fi.oph.koski.raportit

import java.io.FileOutputStream

import org.scalatest.{FreeSpec, Matchers}
import org.apache.poi.ss.usermodel._
import java.io.File
import java.sql.Date
import java.time.LocalDate
import java.time.LocalDate.{of => date}

import org.apache.poi.EncryptedDocumentException

import scala.collection.JavaConverters._


class ExcelWriterSpec extends FreeSpec with Matchers {

  "ExcelWriter" - {

    "Erikoismerkit muutetaan välilyönneiksi välilehtien nimessä ja eivät aiheuta muualla virhettä" in {
      noException shouldBe thrownBy(
        withExcel(erikoismerkkienKäsittelyTestCase()) { wb =>
          wb.getNumberOfSheets should equal(1)
          wb.getSheetAt(0).getSheetName should equal("       datasheet_title")
        }
      )
    }
  }

  private lazy val erikoismerkit = "[]?*\\/:"

  private def erikoismerkkienKäsittelyTestCase() = {
    val workbookSettings = WorkbookSettings(expectedExcelTitle, Some(""))
    val dynamicRows= Seq(Seq("foo"))
    val dynamicColumnSettings = Seq(Column(s"${erikoismerkit}bar", comment = Some(erikoismerkit)))
    val dataSheet = DynamicDataSheet(s"${erikoismerkit}datasheet_title", dynamicRows, dynamicColumnSettings)
    (workbookSettings, Seq(dataSheet))
  }


  private def withExcel(params: (WorkbookSettings, Seq[Sheet]))(tests: (Workbook => Unit)): Unit = {
    val (workbookSettings, sheets) = params
    val file = new File("excel_file_for_tests.xlsx")
    val outputStream = new FileOutputStream(file)
    try {
      ExcelWriter.writeExcel(workbookSettings, sheets, outputStream)
      val wb: Workbook = WorkbookFactory.create(file, excelPassword)
      tests(wb)
    } finally {
      outputStream.close
      file.delete
    }
  }

  private case class MockDataRow
  (
    str: String,
  )

  lazy val expectedExcelTitle = "expected_excel_title"
  lazy val excelPassword = "kalasana"
  lazy val mockDataColumnSettings: Seq[(String, Column)] = Seq(
    "str" -> Column("Str", comment = Some("kommentti"))
  )

  lazy val mockDataColumnSettingsWithGrouping: Seq[(String, Column)] = Columns.flattenGroupingColumns(List(
    "str" -> Column("Str", comment = Some("kommentti"))
  ))
}
