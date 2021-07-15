package fi.oph.koski.raportit

import java.io.OutputStream
//import java.time.{LocalDate, ZoneId}
//import java.util.Date

import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.poifs.crypt.{EncryptionInfo, EncryptionMode, Encryptor}
import org.apache.poi.poifs.crypt.temp.{EncryptedTempData, SXSSFWorkbookWithCustomZipEntrySource}
import org.apache.poi.poifs.filesystem.POIFSFileSystem
//import org.apache.poi.ss.usermodel._
//import org.apache.poi.ss.util.{CellRangeAddress, CellUtil, RegionUtil}
//import org.apache.poi.ss.util.WorkbookUtil.createSafeSheetName
import org.apache.poi.xssf.streaming.{SXSSFCell, SXSSFDrawing, SXSSFSheet, SXSSFWorkbook}
//import org.apache.poi.xssf.usermodel.XSSFSheet

//import scala.collection.JavaConverters._

object ExcelWriter {

  def writeExcel(workbookSettings: WorkbookSettings, sheets: Seq[Sheet], out: OutputStream): Unit = {

    val wb = if (workbookSettings.password.isEmpty) new SXSSFWorkbook else new SXSSFWorkbookWithCustomZipEntrySource
    try {
      val coreProps = wb.getXSSFWorkbook.getProperties.getCoreProperties
      coreProps.setTitle(workbookSettings.title)
      coreProps.setCreator("Koski")
//      sheets.foreach { sheet =>
//        val sh = createSheet(sheet, wb)   sheet match {
//          case ds: SheetWithColumnSettings => writeDataSheet(wb, sh, ds)
//          case ds: DocumentationSheet => writeDocumentationSheet(wb, sh, ds)
//        }
//      }
      if (workbookSettings.password.isEmpty) {
        wb.write(out)
      } else {
        // based on https://github.com/apache/poi/blob/f509d1deae86866ed531f10f2eba7db17e098473/src/examples/src/org/apache/poi/xssf/streaming/examples/SavePasswordProtectedXlsx.java
        val tempData = new EncryptedTempData
        try {
          wb.write(tempData.getOutputStream)
          val opc = OPCPackage.open(tempData.getInputStream)
          val fs = new POIFSFileSystem
          val enc = Encryptor.getInstance(new EncryptionInfo(EncryptionMode.agile))
          enc.confirmPassword(workbookSettings.password.get)
          opc.save(enc.getDataStream(fs))
          fs.writeFilesystem(out)
        } finally {
          tempData.dispose()
        }
      }
      out.close()
    } finally {
      // deletes temporary files from disk
      wb.dispose()
    }
  }

/**
  private def createSheet(sheet: Sheet, wb: SXSSFWorkbook) = {
    val safeUniqueSheetName = createSafeUniqueSheetName(sheet.title, wb)
    wb.createSheet(safeUniqueSheetName)
  }

  private def createSafeUniqueSheetName(sheetName: String, wb: SXSSFWorkbook, indexToAdd: Int = 2): String = {
    if (indexToAdd > 9) { throw new InterruptedException("Uniikin välilehden nimen muodostamista yritettiin liian monta kertaa onnistumatta.") }

    val safeSheetName = createSafeSheetName(sheetName)

    if (sheetNameAlreadyExists(safeSheetName, wb)) {
      val safeSheetNameWithIndex = appendIndexToSheetName(safeSheetName, indexToAdd)
      createSafeUniqueSheetName(safeSheetNameWithIndex, wb, indexToAdd + 1)
    } else {
      safeSheetName
    }
  }

  private def appendIndexToSheetName(sheetName: String, index: Int) = {
    val MAX_SHEET_NAME_LENGTH = 31
    if (sheetName.length < MAX_SHEET_NAME_LENGTH) {
      sheetName + index
    } else {
      sheetName.take(30) + index
    }
  }

  private def sheetNameAlreadyExists(sheetName: String, wb: SXSSFWorkbook) = {
    wb.sheetIterator.asScala.exists(_.getSheetName.toLowerCase == sheetName.toLowerCase)
  }

  private def writeDataSheet(wb: SXSSFWorkbook, sh: SXSSFSheet, dataSheet: SheetWithColumnSettings): Unit = {
    addIgnoredErrors(sh)

    val sheetHasGroupingHeaders = dataSheet.columnSettingsWithIndex.map(_._1).exists(_.groupingTitle.isDefined)
    val cellHeaderRownumber = if (sheetHasGroupingHeaders) secondRow else firstRow

    val headingRow = createHeadingRow(sh, cellHeaderRownumber)
    val writeCellHeader = createCellHeaderWriter(wb, sh)
    for (columnAndIndex <- dataSheet.columnSettingsWithIndex) {
      val cell = headingRow.createCell(columnAndIndex._2)
      writeCellHeader(columnAndIndex, cell)
    }

    if (sheetHasGroupingHeaders) {
      val groupingHeaderRow = sh.createRow(firstRow)
      groupingHeaderRow.setHeightInPoints(25)

      val groupingTitlePositions = dataSheet
        .columnSettingsWithIndex
        .flatMap { case (column, index) => column.groupingTitle.map(title => (title, index)) }
        .groupBy(_._1)
        .mapValues(_.map(_._2))
        .map { case (title, indexes) => { (title, indexes.min, indexes.max) }}

      val writeGroupingHeader = createGroupingHeaderWriter(wb, sh)
      for ((title, start, end) <- groupingTitlePositions) {
        val region = new CellRangeAddress(0, 0, start, end)
        sh.addMergedRegion(region)
        val cell = groupingHeaderRow.createCell(start)
        writeGroupingHeader(title, cell, region)
      }
    }

    //sets column names visible when user is scrolling through rows
    sh.createFreezePane(0, cellHeaderRownumber + 1)

    val writeDataToCell = createDataCellWriter(wb)

    for (rowNumber <- dataSheet.rows.indices) {
      val rowInExcel = sh.createRow(rowNumber + cellHeaderRownumber + 1)
      val dataRow = dataSheet.rowIterator(rowNumber)
      var colNumber = 0
      for (data <- dataRow) {
        val cell = rowInExcel.createCell(colNumber)
        writeDataToCell(data, cell)
        colNumber += 1
      }
    }

    for ((cs, col) <- dataSheet.columnSettingsWithIndex) {
      if (cs.width.isEmpty) {
        sh.autoSizeColumn(col)
      }
    }

    val creationHelper = wb.getCreationHelper
    val drawing = sh.createDrawingPatriarch
    val cellsAndColumnsWithIndex = headingRow.cellIterator.asScala.toSeq.zip(dataSheet.columnSettingsWithIndex)
    for ((cell, (column, _)) <- cellsAndColumnsWithIndex) {
      if (column.comment.isDefined) {
        val anchor = createAnchor(creationHelper, sh, cell, column.comment.get, cellHeaderRownumber)
        val comment = createComment(drawing, anchor, creationHelper, column.comment.get)
        cell.setCellComment(comment)
      }
    }
  }

  private def addIgnoredErrors(sh: SXSSFSheet) = {
    // SXSSFSheet does not expose "addIgnoredErrors" method. Hack around this.
    // based on https://stackoverflow.com/questions/47477912/apache-poi-how-to-use-addignorederrors-functionality-in-sxssfsheet
    val hiddenShField = classOf[SXSSFSheet].getDeclaredField("_sh")
    hiddenShField.setAccessible(true)
    hiddenShField.get(sh).asInstanceOf[XSSFSheet].addIgnoredErrors(new CellRangeAddress(0, 999999, 0, 999), IgnoredErrorType.NUMBER_STORED_AS_TEXT)
  }

  private def createHeadingRow(sh: SXSSFSheet, rownumber: Int) = {
    val headingRow = sh.createRow(rownumber)
    headingRow.setHeightInPoints(25)
    headingRow
  }

  type CellHeaderWriter = ((Column, Int), SXSSFCell) => Unit
  private def createCellHeaderWriter(wb: SXSSFWorkbook, sh: SXSSFSheet): CellHeaderWriter = {
    val headingStyle = wb.createCellStyle()
    headingStyle.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex)
    headingStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND)
    headingStyle.setVerticalAlignment(VerticalAlignment.CENTER)

    (columnAndIndex: (Column, Int), cell: SXSSFCell) => setHeadingCellValue(columnAndIndex, cell, headingStyle, sh)
  }

  private def setHeadingCellValue(columnAndIndex: (Column, Int), cell: SXSSFCell, headingStyle: CellStyle, sh: SXSSFSheet) = {
    val (column, columnIndex) = columnAndIndex
    cell.setCellStyle(headingStyle)
    cell.setCellValue(column.title)
    column.width match {
      case None => sh.trackColumnForAutoSizing(columnIndex)
      case Some(w) => sh.setColumnWidth(columnIndex, w)
    }
  }

  type GroupingHeaderWriter = (String, SXSSFCell, CellRangeAddress) => Unit
  private def createGroupingHeaderWriter(wb: SXSSFWorkbook, sh: SXSSFSheet): GroupingHeaderWriter = {
    val style = wb.createCellStyle()
    style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex)
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND)

    (title: String, cell: SXSSFCell, range: CellRangeAddress) => {
      cell.setCellValue(title)
      cell.setCellStyle(style)
      RegionUtil.setBorderTop(BorderStyle.MEDIUM, range, sh)
      RegionUtil.setBorderRight(BorderStyle.MEDIUM, range, sh)
      RegionUtil.setBorderBottom(BorderStyle.MEDIUM, range, sh)
      RegionUtil.setBorderLeft(BorderStyle.MEDIUM, range, sh)
      CellUtil.setAlignment(cell, HorizontalAlignment.CENTER)
      CellUtil.setVerticalAlignment(cell, VerticalAlignment.CENTER)
    }
  }

  type DataCellWriter = (Any, SXSSFCell) => Unit
  private def createDataCellWriter(wb: SXSSFWorkbook): DataCellWriter = {
    val textStyle = wb.createCellStyle()
    textStyle.setDataFormat(BuiltinFormats.getBuiltinFormat("text").toShort)

    val dateStyle = wb.createCellStyle()
    dateStyle.setDataFormat(wb.getCreationHelper.createDataFormat.getFormat("yyyy-MM-dd"))
    dateStyle.setAlignment(HorizontalAlignment.LEFT)

    val floatStyle = wb.createCellStyle()
    floatStyle.setDataFormat(wb.getCreationHelper.createDataFormat().getFormat("#.0"))

    val booleanStyle = wb.createCellStyle()
    booleanStyle.setDataFormat(wb.getCreationHelper.createDataFormat().getFormat("\"kyllä\";;\"ei\";"))
    booleanStyle.setAlignment(HorizontalAlignment.LEFT)

    (data: Any, cell: SXSSFCell) => setDataCellValue(data, cell, textStyle, dateStyle, floatStyle, booleanStyle)
  }

  private def setDataCellValue(data: Any, cell: SXSSFCell, textStyle: CellStyle, dateStyle: CellStyle, floatStyle: CellStyle, booleanStyle: CellStyle): Unit = data match {
    case Some(x) => setDataCellValue_(x, cell, textStyle, dateStyle, floatStyle, booleanStyle)
    case None => /* do nothing */
    case x => setDataCellValue_(x, cell, textStyle, dateStyle, floatStyle, booleanStyle)
  }

  private def setDataCellValue_(data: Any, cell: SXSSFCell, textStyle: CellStyle, dateStyle: CellStyle, floatStyle: CellStyle, booleanStyle: CellStyle) = data match {
    case s: String => setCellStyleAndValue(cell, s, textStyle)
    case d: LocalDate => setCellStyleAndValue(cell, d, dateStyle)
    case i: Int => cell.setCellValue(i)
    case f: Double => setCellStyleAndValue(cell, f, floatStyle)
    case b: BigDecimal => setCellStyleAndValue(cell, b.toDouble, floatStyle)
    case b: Boolean => cell.setCellStyle(booleanStyle); cell.setCellValue(if (b) 1 else 0)
    case x: Any => throw new IllegalStateException("Not handled yet? " + x.toString)
  }

  private def setCellStyleAndValue(cell: SXSSFCell, s: String, cs: CellStyle) {
    cell.setCellStyle(cs)
    cell.setCellValue(s)
  }

  private def setCellStyleAndValue(cell: SXSSFCell, d: LocalDate, cs: CellStyle) {
    cell.setCellStyle(cs)
    cell.setCellValue(Date.from(d.atStartOfDay(ZoneId.systemDefault).toInstant))
  }

  private def setCellStyleAndValue(cell: SXSSFCell, f: Double, cs: CellStyle) {
    if (f != 0) {
      cell.setCellStyle(cs)
    } else {
      // use the default style
    }
    cell.setCellValue(f)
  }

  private def createComment(drawing: SXSSFDrawing, anchor: ClientAnchor, creationHelper: CreationHelper, commentText: String) = {
    val comment = drawing.createCellComment(anchor)
    comment.setString(creationHelper.createRichTextString(commentText))
    comment
  }

  private def createAnchor(creationHelper: CreationHelper, sh: SXSSFSheet, cell: Cell, commentText: String, rownumber: Int) = {
    //Size of an Comment is reserved by allocating cells and rows
    val columnIndex = cell.getColumnIndex
    val anchor = creationHelper.createClientAnchor
    anchor.setRow1(rownumber)
    anchor.setRow2(rownumber + 5)
    anchor.setCol1(columnIndex)
    anchor.setCol2(columnIndex + 10)
    anchor
  }

  private def writeDocumentationSheet(wb: SXSSFWorkbook, sh: SXSSFSheet, documentationSheet: DocumentationSheet): Unit = {
    val documentationStyle = wb.createCellStyle()
    documentationStyle.setWrapText(true)
    documentationStyle.setVerticalAlignment(VerticalAlignment.CENTER)

    val row = sh.createRow(0)
    sh.trackColumnForAutoSizing(0)
    val cell = row.createCell(0)
    cell.setCellStyle(documentationStyle)
    cell.setCellValue(documentationSheet.text)
    sh.autoSizeColumn(0)
  }

  private lazy val firstRow = 0
  private lazy val secondRow = 1
 **/
}
