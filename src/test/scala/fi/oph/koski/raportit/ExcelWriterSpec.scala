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

    "Excelin avaus vaatii salasanan" in {
      an[EncryptedDocumentException] should be thrownBy (withExcel(dataSheetAndDocumentationSheetTestCase(password = "wrong_password")) { _ => 1 should equal(1) })
    }

    "Erikoismerkit muutetaan välilyönneiksi välilehtien nimessä ja eivät aiheuta muualla virhettä" in {
      noException shouldBe thrownBy(
        withExcel(erikoismerkkienKäsittelyTestCase()) { wb =>
          wb.getNumberOfSheets should equal(1)
          wb.getSheetAt(0).getSheetName should equal("       datasheet_title")
        }
      )
    }

    "Saman nimiset välilehdet" - {
      "liian pitkä sheetname, lisätään indeksi loppuun virheen välttämiseksi" in {
        withExcel(yhteentörmäysTestCase) { wb =>
          wb.getNumberOfSheets should equal(5)
          wb.sheetIterator.asScala.map(_.getSheetName).toSeq should equal(Seq(
            "       datasheet_title",
            "       datasheet_title2",
            "todella_pitka_valilehden_nimi_j",
            "todella_pitka_valilehden_nimi_2",
            "uniikki"
          ))
        }
      }

      "case insensitiivisyys, lisätään indeksi loppuun virheen välttämiseksi" in {
        withExcel(yhteentörmäysTestCase2) { wb =>
          wb.getNumberOfSheets should equal(2)
          wb.sheetIterator.asScala.map(_.getSheetName).toSeq should equal(Seq("EW p English Worldwide", "EW P ENGLISH WORLDWIDE2"))
        }
      }

      "Jos uniikkia välilehteä ei löydy" in {
        an[InterruptedException] should be thrownBy(withExcel(uniikinVälilehdenLuontiEpäonnistuuTestCase) {_ => Unit})
      }
    }

    "Ryhmittelevät sarakkeet" - {
      withExcel(ryhmittelevatSarakkeet) { wb =>
        val sheet = wb.getSheetAt(0)
        val rows = sheet.iterator().asScala.toSeq

        "Varsinaisten sarakkeiden nimien yläpuolelle on luoutu ryhmittelevät sarakkeet määriteltyyn kohtaan" in {
          rows(0).getCell(3) shouldBe(null)
          rows(0).getCell(4).getStringCellValue shouldBe("Ints")
          rows(0).getCell(6).getStringCellValue shouldBe("Doubles")
          rows(0).getCell(8) shouldBe(null)
        }
        "Sarakkeiden nimet on toisella rivillä" in {
          rows(1).getCell(4).getStringCellValue shouldBe("Int")
          rows(1).getCell(5).getStringCellValue shouldBe("OptionInt")
        }
        "Varsinainen data on kolmannella" in {
          rows(2).getCell(4).getNumericCellValue shouldBe(1)
          rows(2).getCell(5).getNumericCellValue shouldBe(2)
        }
      }
    }


    "Excelin luonti, DataSheet ja DocumentationSheet" - {
      withExcel(dataSheetAndDocumentationSheetTestCase()) { wb =>
        "Luo data ja dokumentaatio välilehdet" in {
          wb.getNumberOfSheets should equal(2)
          wb.getSheetAt(0).getSheetName should equal("data_sheet_title")
          wb.getSheetAt(1).getSheetName should equal("documentation_sheet_title")
        }
        "DataSheet" - {
          "otsikko kolumnit" in {
            val headingRow = wb.getSheet("data_sheet_title").iterator.next

            headingRow.getPhysicalNumberOfCells should equal(10)

            headingRow.getCell(0).getCellType should equal(CellType.STRING)
            headingRow.getCell(0).getStringCellValue should equal("Str")

            headingRow.getCell(1).getCellType should equal(CellType.STRING)
            headingRow.getCell(1).getStringCellValue should equal("OptionStr")

            headingRow.getCell(2).getCellType should equal(CellType.STRING)
            headingRow.getCell(2).getStringCellValue should equal("LocalDate")

            headingRow.getCell(3).getCellType should equal(CellType.STRING)
            headingRow.getCell(3).getStringCellValue should equal("OptionLocalDate")

            headingRow.getCell(4).getCellType should equal(CellType.STRING)
            headingRow.getCell(4).getStringCellValue should equal("Int")

            headingRow.getCell(5).getCellType should equal(CellType.STRING)
            headingRow.getCell(5).getStringCellValue should equal("OptionInt")

            headingRow.getCell(6).getCellType should equal(CellType.STRING)
            headingRow.getCell(6).getStringCellValue should equal("Double")

            headingRow.getCell(7).getCellType should equal(CellType.STRING)
            headingRow.getCell(7).getStringCellValue should equal("OptionDouble")

            headingRow.getCell(8).getCellType should equal(CellType.STRING)
            headingRow.getCell(8).getStringCellValue should equal("Boolean")

            headingRow.getCell(9).getCellType should equal(CellType.STRING)
            headingRow.getCell(9).getStringCellValue should equal("OptionBoolean")
          }
          "Jos kommentti on määritelty, lisää kolumnin nimen kommentin alkuun" in {
            val headingRow = wb.getSheet("data_sheet_title").iterator.next
            headingRow.getCell(0).getCellComment.getString.getString should equal("Str: kommentti")
          }
          "Jos kommenttia ei ole määritelty, luo kommentin jossa solun nimi" in {
            val headingRow = wb.getSheet("data_sheet_title").iterator.next
            headingRow.getCell(1).getCellComment.getString.getString should equal("OptionStr")
          }
          "Data solujen formatointi" - {
            val dataSheetIterator = wb.getSheet("data_sheet_title").iterator
            val _ = dataSheetIterator.next //skip heading row
            val firstDataRow = dataSheetIterator.next
            val secondDataRow = dataSheetIterator.next

            "Stringit" in {
              firstDataRow.getCell(0).getStringCellValue should equal("foo")
              firstDataRow.getCell(1).getStringCellValue should equal("bar")

              secondDataRow.getCell(0).getStringCellValue should equal("foo")
              secondDataRow.getCell(1).getCellType should equal(CellType.BLANK)
            }
            "Päivämäärät" in {
              firstDataRow.getCell(2).getCellStyle.getDataFormatString should equal("yyyy-MM-dd")
              firstDataRow.getCell(2).getDateCellValue should equal(Date.valueOf(date(2000, 1, 1)))
              firstDataRow.getCell(3).getCellStyle.getDataFormatString should equal("yyyy-MM-dd")
              firstDataRow.getCell(3).getDateCellValue should equal(Date.valueOf(date(2000, 2, 2)))

              secondDataRow.getCell(2).getCellStyle.getDataFormatString should equal("yyyy-MM-dd")
              secondDataRow.getCell(2).getDateCellValue should equal(Date.valueOf(date(2000, 1, 1)))
              secondDataRow.getCell(3).getCellType should equal(CellType.BLANK)
            }
            "Intit" in {
              firstDataRow.getCell(4).getNumericCellValue should equal(1)
              firstDataRow.getCell(5).getNumericCellValue should equal(2)

              secondDataRow.getCell(4).getNumericCellValue should equal(1)
              secondDataRow.getCell(5).getCellType should equal(CellType.BLANK)
            }
            "Doublet" in {
              firstDataRow.getCell(6).getCellStyle.getDataFormatString should equal("#.0")
              firstDataRow.getCell(6).getNumericCellValue should equal(2.0)
              firstDataRow.getCell(7).getCellStyle.getDataFormatString should equal("#.0")
              firstDataRow.getCell(7).getNumericCellValue should equal(3.0)

              secondDataRow.getCell(6).getCellStyle.getDataFormatString should equal("General")
              secondDataRow.getCell(6).getNumericCellValue should equal(0)
              secondDataRow.getCell(7).getCellStyle.getDataFormatString should equal("General")
              secondDataRow.getCell(7).getCellType should equal(CellType.BLANK)
            }
            "Booleanit" in {
              firstDataRow.getCell(8).getCellStyle.getDataFormatString should equal("\"kyllä\";;\"ei\";")
              firstDataRow.getCell(8).getNumericCellValue should equal(1)
              firstDataRow.getCell(9).getCellStyle.getDataFormatString should equal("\"kyllä\";;\"ei\";")
              firstDataRow.getCell(9).getNumericCellValue should equal(0)

              secondDataRow.getCell(8).getCellStyle.getDataFormatString should equal("\"kyllä\";;\"ei\";")
              secondDataRow.getCell(8).getNumericCellValue should equal(1)
              secondDataRow.getCell(9).getCellType should equal(CellType.BLANK)
            }
          }
        }
        "Kirjoittaa dokumentaation" in {
          val firstRow = wb.getSheet("documentation_sheet_title").iterator.next
          firstRow.getCell(0).getStringCellValue should equal("dokumentaatio")
        }
      }
    }
    "DynamicDataSheet" - {
      withExcel(dynamicDataSheetAndDocumentationSheet()) { wb =>
        "Luo data ja dokumentaatio välilehdet" in {
          wb.getNumberOfSheets should equal(2)
          wb.getSheetAt(0).getSheetName should equal("data_sheet_title")
          wb.getSheetAt(1).getSheetName should equal("documentation_sheet_title")
        }
        "Datat" - {
          "otsikko kolumnit" in {
            val headingRow = wb.getSheet("data_sheet_title").iterator.next

            headingRow.getPhysicalNumberOfCells should equal(10)

            headingRow.getCell(0).getCellType should equal(CellType.STRING)
            headingRow.getCell(0).getStringCellValue should equal("Str")

            headingRow.getCell(1).getCellType should equal(CellType.STRING)
            headingRow.getCell(1).getStringCellValue should equal("OptionStr")

            headingRow.getCell(2).getCellType should equal(CellType.STRING)
            headingRow.getCell(2).getStringCellValue should equal("LocalDate")

            headingRow.getCell(3).getCellType should equal(CellType.STRING)
            headingRow.getCell(3).getStringCellValue should equal("OptionLocalDate")

            headingRow.getCell(4).getCellType should equal(CellType.STRING)
            headingRow.getCell(4).getStringCellValue should equal("Int")

            headingRow.getCell(5).getCellType should equal(CellType.STRING)
            headingRow.getCell(5).getStringCellValue should equal("OptionInt")

            headingRow.getCell(6).getCellType should equal(CellType.STRING)
            headingRow.getCell(6).getStringCellValue should equal("Double")

            headingRow.getCell(7).getCellType should equal(CellType.STRING)
            headingRow.getCell(7).getStringCellValue should equal("OptionDouble")

            headingRow.getCell(8).getCellType should equal(CellType.STRING)
            headingRow.getCell(8).getStringCellValue should equal("Boolean")

            headingRow.getCell(9).getCellType should equal(CellType.STRING)
            headingRow.getCell(9).getStringCellValue should equal("OptionBoolean")
          }
          "Data solujen formatointi" - {
            val dataSheetIterator = wb.getSheet("data_sheet_title").iterator
            val _ = dataSheetIterator.next //skip heading row
            val firstDataRow = dataSheetIterator.next
            val secondDataRow = dataSheetIterator.next

            "Stringit" in {
              firstDataRow.getCell(0).getStringCellValue should equal("foo")
              firstDataRow.getCell(1).getStringCellValue should equal("bar")

              secondDataRow.getCell(0).getStringCellValue should equal("foo")
              secondDataRow.getCell(1).getCellType should equal(CellType.BLANK)
            }
            "Päivämäärät" in {
              firstDataRow.getCell(2).getCellStyle.getDataFormatString should equal("yyyy-MM-dd")
              firstDataRow.getCell(2).getDateCellValue should equal(Date.valueOf(date(2000, 1, 1)))
              firstDataRow.getCell(3).getCellStyle.getDataFormatString should equal("yyyy-MM-dd")
              firstDataRow.getCell(3).getDateCellValue should equal(Date.valueOf(date(2000, 2, 2)))

              secondDataRow.getCell(2).getCellStyle.getDataFormatString should equal("yyyy-MM-dd")
              secondDataRow.getCell(2).getDateCellValue should equal(Date.valueOf(date(2000, 1, 1)))
              secondDataRow.getCell(3).getCellType should equal(CellType.BLANK)
            }
            "Intit" in {
              firstDataRow.getCell(4).getNumericCellValue should equal(1)
              firstDataRow.getCell(5).getNumericCellValue should equal(2)

              secondDataRow.getCell(4).getNumericCellValue should equal(1)
              secondDataRow.getCell(5).getCellType should equal(CellType.BLANK)
            }
            "Doublet" in {
              firstDataRow.getCell(6).getCellStyle.getDataFormatString should equal("#.0")
              firstDataRow.getCell(6).getNumericCellValue should equal(2.0)
              firstDataRow.getCell(7).getCellStyle.getDataFormatString should equal("#.0")
              firstDataRow.getCell(7).getNumericCellValue should equal(3.0)

              secondDataRow.getCell(6).getCellStyle.getDataFormatString should equal("General")
              secondDataRow.getCell(6).getNumericCellValue should equal(0)
              secondDataRow.getCell(7).getCellStyle.getDataFormatString should equal("General")
              secondDataRow.getCell(7).getCellType should equal(CellType.BLANK)
            }
            "Booleanit" in {
              firstDataRow.getCell(8).getCellStyle.getDataFormatString should equal("\"kyllä\";;\"ei\";")
              firstDataRow.getCell(8).getNumericCellValue should equal(1)
              firstDataRow.getCell(9).getCellStyle.getDataFormatString should equal("\"kyllä\";;\"ei\";")
              firstDataRow.getCell(9).getNumericCellValue should equal(0)

              secondDataRow.getCell(8).getCellStyle.getDataFormatString should equal("\"kyllä\";;\"ei\";")
              secondDataRow.getCell(8).getNumericCellValue should equal(1)
              secondDataRow.getCell(9).getCellType should equal(CellType.BLANK)
            }
          }
          "Kirjoittaa dokumentaation" in {
            val firstRow = wb.getSheet("documentation_sheet_title").iterator.next
            firstRow.getCell(0).getStringCellValue should equal("dokumentaatio")
          }
        }
      }
    }
  }

  private def dataSheetAndDocumentationSheetTestCase(password: String = excelPassword): (WorkbookSettings, Seq[Sheet]) = {
    val workbookSettings = WorkbookSettings(expectedExcelTitle, Some(password))
    val mockDataRows = Seq(
      MockDataRow("foo", Some("bar"), date(2000, 1, 1), Some(date(2000, 2, 2)), 1, Some(2), 2.0, Some(3.0), true, Some(false)),
      MockDataRow("foo", None, date(2000, 1, 1), None, 1, None, 0.0, None, true, None)
    )

    val dataSheet = DataSheet("data_sheet_title", mockDataRows, mockDataColumnSettings)
    val documentationSheet = DocumentationSheet("documentation_sheet_title", "dokumentaatio")

    (workbookSettings, Seq(dataSheet, documentationSheet))
  }

  private def dynamicDataSheetAndDocumentationSheet(password: String = excelPassword): (WorkbookSettings, Seq[Sheet]) = {
    val workbookSettings = WorkbookSettings(expectedExcelTitle, Some(password))
    val dynamicRows= Seq(
      Seq("foo", Some("bar"), date(2000, 1, 1), Some(date(2000, 2, 2)), 1, Some(2), 2.0, Some(3.0), true, Some(false)),
      Seq("foo", None, date(2000, 1, 1), None, 1, None, 0.0, None, true, None)
    )
    val dynamicColumnSettings = Seq(Column("Str"), Column("OptionStr"), Column("LocalDate"), Column("OptionLocalDate"), Column("Int"), Column("OptionInt"), Column("Double"), Column("OptionDouble"), Column("Boolean"), Column("OptionBoolean"))

    val dataSheet = DynamicDataSheet("data_sheet_title", dynamicRows, dynamicColumnSettings)
    val documentationSheet = DocumentationSheet("documentation_sheet_title", "dokumentaatio")

    (workbookSettings, Seq(dataSheet, documentationSheet))
  }

  private lazy val erikoismerkit = "[]?*\\/:"

  private def erikoismerkkienKäsittelyTestCase() = {
    val workbookSettings = WorkbookSettings(expectedExcelTitle, Some(excelPassword))
    val dynamicRows= Seq(Seq("foo"))
    val dynamicColumnSettings = Seq(Column(s"${erikoismerkit}bar", comment = Some(erikoismerkit)))
    val dataSheet = DynamicDataSheet(s"${erikoismerkit}datasheet_title", dynamicRows, dynamicColumnSettings)
    (workbookSettings, Seq(dataSheet))
  }

  private def yhteentörmäysTestCase = {
    val workbookSettings = WorkbookSettings(expectedExcelTitle, Some(excelPassword))
    val dataRow= Seq(Seq("foo"))
    val columnSettings= Seq(Column(s"${erikoismerkit}bar", comment = Some(erikoismerkit)))
    val dataSheet_A = DynamicDataSheet(s"${erikoismerkit}datasheet_title", dataRow, columnSettings)
    val dataSheet_B = DynamicDataSheet("       datasheet_title", dataRow, columnSettings)
    val dataSheet_C = DynamicDataSheet("todella_pitka_valilehden_nimi_joka_nimi_ei_mahdu_kokonaisena", dataRow, columnSettings)
    val dataSheet_D = DynamicDataSheet("todella_pitka_valilehden_nimi_joka_nimi_ei_mahdu_kokonaisena", dataRow, columnSettings)
    val dataSheet_E = DynamicDataSheet("uniikki", dataRow, columnSettings)
    (workbookSettings, Seq(dataSheet_A, dataSheet_B, dataSheet_C, dataSheet_D, dataSheet_E))
  }

  private def yhteentörmäysTestCase2 = {
    val workbookSettings = WorkbookSettings(expectedExcelTitle, Some(excelPassword))
    val dataRow= Seq(Seq("foo"))
    val columnSettings= Seq(Column(s"${erikoismerkit}bar", comment = Some(erikoismerkit)))
    (workbookSettings, Seq(DynamicDataSheet("EW p English Worldwide", dataRow, columnSettings), DynamicDataSheet("EW P ENGLISH WORLDWIDE", dataRow, columnSettings)))
  }

  private def uniikinVälilehdenLuontiEpäonnistuuTestCase = {
    val workbookSettings = WorkbookSettings(expectedExcelTitle, Some(excelPassword))
    val dataRow = Seq(Seq("foo"))
    val columnSettings = Seq(Column(s"${erikoismerkit}bar", comment = Some(erikoismerkit)))
    val sheet = DynamicDataSheet("todella_pitka_valilehden_nimi_j", dataRow, columnSettings)
    val sheetsWithIndex = (0 to 10).map(i => DynamicDataSheet(s"todella_pitka_valilehden_nimi_$i", dataRow, columnSettings))
    val duplicate = DynamicDataSheet("todella_pitka_valilehden_nimi_j", dataRow, columnSettings)
    val sheets = (sheet +: sheetsWithIndex) :+ duplicate
    (workbookSettings, sheets)
  }

  private def ryhmittelevatSarakkeet = {
    val workbookSettings = WorkbookSettings(expectedExcelTitle, Some(excelPassword))
    val mockDataRows = Seq(
      MockDataRow("foo", Some("bar"), date(2000, 1, 1), Some(date(2000, 2, 2)), 1, Some(2), 2.0, Some(3.0), true, Some(false)),
      MockDataRow("foo", None, date(2000, 1, 1), None, 1, None, 0.0, None, true, None)
    )

    val dataSheet = DataSheet("data_sheet_title", mockDataRows, mockDataColumnSettingsWithGrouping)

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
    optionStr: Option[String],
    localdate: LocalDate,
    optionLocaldate: Option[LocalDate],
    int: Int,
    optionInt: Option[Int],
    double: Double,
    optionDouble: Option[Double],
    boolean: Boolean,
    optionBoolean: Option[Boolean]
  )

  lazy val expectedExcelTitle = "expected_excel_title"
  lazy val excelPassword = "kalasana"
  lazy val mockDataColumnSettings: Seq[(String, Column)] = Seq(
    "str" -> Column("Str", comment = Some("kommentti")),
    "optionStr" -> Column("OptionStr"),
    "localdate" -> Column("LocalDate"),
    "optionLocaldate" -> Column("OptionLocalDate"),
    "int" -> Column("Int"),
    "optionInt" -> Column("OptionInt"),
    "double" -> Column("Double"),
    "optionDouble" -> Column("OptionDouble"),
    "boolean" -> Column("Boolean"),
    "optionBoolean" -> Column("OptionBoolean")
  )

  lazy val mockDataColumnSettingsWithGrouping: Seq[(String, Column)] = Columns.flattenGroupingColumns(List(
    "str" -> Column("Str", comment = Some("kommentti")),
    "optionStr" -> Column("OptionStr"),
    "localdate" -> Column("LocalDate"),
    "optionLocaldate" -> Column("OptionLocalDate"),
    "Ints" -> GroupColumnsWithTitle(List(
      "int" -> Column("Int"),
      "optionInt" -> Column("OptionInt")
    )),
    "Doubles" -> GroupColumnsWithTitle(List(
      "double" -> Column("Double"),
      "optionDouble" -> Column("OptionDouble")
    )),
    "boolean" -> Column("Boolean"),
    "optionBoolean" -> Column("OptionBoolean")
  ))
}
