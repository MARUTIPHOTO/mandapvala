package com.example.util

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.R
import com.example.data.model.Bill
import com.example.data.model.BillItem
import com.example.data.model.BusinessProfile
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfInvoiceGenerator {

    fun generateInvoicePdf(
        context: Context,
        bill: Bill,
        business: BusinessProfile,
        items: List<BillItem>
    ): File {
        val pdfDocument = PdfDocument()

        // A4 page dimensions in PostScript points: 595 x 842
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Background
        canvas.drawColor(Color.WHITE)

        val royalMaroon = Color.parseColor("#831843")
        val amberGold = Color.parseColor("#D97706")
        val darkGray = Color.parseColor("#262626")
        val lightGray = Color.parseColor("#F3F4F6")
        val borderGray = Color.parseColor("#E5E7EB")

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Top Royal Header Band
        paint.color = royalMaroon
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 95f, paint)

        // Accent Gold Strip
        paint.color = amberGold
        canvas.drawRect(0f, 95f, pageWidth.toFloat(), 100f, paint)

        // Logo / Icon placeholder
        try {
            val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_mandap_logo)
            if (logoBitmap != null) {
                val destRect = RectF(24f, 15f, 94f, 85f)
                canvas.drawBitmap(logoBitmap, null, destRect, paint)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Business Name & Tagline
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 20f
        canvas.drawText(business.name, 105f, 38f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 10f
        paint.color = Color.parseColor("#FCE7F3")
        canvas.drawText("${business.address}, ${business.city}", 105f, 54f, paint)
        canvas.drawText("Phone: ${business.mobile} | WhatsApp: ${business.whatsapp} | GST: ${business.gstNumber}", 105f, 68f, paint)
        canvas.drawText("Email: ${business.email}", 105f, 82f, paint)

        // Invoice Header Title
        paint.color = royalMaroon
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val titleText = "ESTIMATE / TAX INVOICE"
        val titleWidth = paint.measureText(titleText)
        canvas.drawText(titleText, (pageWidth - titleWidth) / 2f, 128f, paint)

        // Two Cards: Customer Info & Invoice Meta
        val cardTop = 145f
        val cardHeight = 85f
        val cardWidth = 260f

        // Left Card: Customer Details
        paint.color = lightGray
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(24f, cardTop, 24f + cardWidth, cardTop + cardHeight), 8f, 8f, paint)
        paint.color = borderGray
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(RectF(24f, cardTop, 24f + cardWidth, cardTop + cardHeight), 8f, 8f, paint)

        paint.style = Paint.Style.FILL
        paint.color = royalMaroon
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BILL TO (CUSTOMER DETAILS)", 36f, cardTop + 18f, paint)

        paint.color = darkGray
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(bill.customerName, 36f, cardTop + 34f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9.5f
        canvas.drawText("Contact: ${bill.customerMobile}", 36f, cardTop + 48f, paint)
        canvas.drawText("Venue / Address: ${bill.customerAddress.take(40)}", 36f, cardTop + 62f, paint)
        canvas.drawText("Event: ${bill.eventType} on ${bill.eventDate}", 36f, cardTop + 76f, paint)

        // Right Card: Invoice Details
        val rightCardLeft = pageWidth - 24f - cardWidth
        paint.color = lightGray
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(rightCardLeft, cardTop, rightCardLeft + cardWidth, cardTop + cardHeight), 8f, 8f, paint)
        paint.color = borderGray
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(RectF(rightCardLeft, cardTop, rightCardLeft + cardWidth, cardTop + cardHeight), 8f, 8f, paint)

        paint.style = Paint.Style.FILL
        paint.color = royalMaroon
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("INVOICE PARTICULARS", rightCardLeft + 12f, cardTop + 18f, paint)

        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(bill.createdAt))
        paint.color = darkGray
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Invoice Number: ${bill.billNumber}", rightCardLeft + 12f, cardTop + 34f, paint)
        canvas.drawText("Invoice Date: $dateStr", rightCardLeft + 12f, cardTop + 48f, paint)
        canvas.drawText("Payment Status: ${bill.status}", rightCardLeft + 12f, cardTop + 62f, paint)
        canvas.drawText("Created By: ${business.ownerName}", rightCardLeft + 12f, cardTop + 76f, paint)

        // Table Header
        val tableTop = 245f
        val tableLeft = 24f
        val tableRight = pageWidth - 24f
        val tableWidth = tableRight - tableLeft

        paint.color = royalMaroon
        paint.style = Paint.Style.FILL
        canvas.drawRect(tableLeft, tableTop, tableRight, tableTop + 24f, paint)

        paint.color = Color.WHITE
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("#", tableLeft + 8f, tableTop + 16f, paint)
        canvas.drawText("ITEM DESCRIPTION", tableLeft + 30f, tableTop + 16f, paint)
        canvas.drawText("CATEGORY", tableLeft + 250f, tableTop + 16f, paint)
        canvas.drawText("QTY", tableLeft + 340f, tableTop + 16f, paint)
        canvas.drawText("RATE (₹)", tableLeft + 400f, tableTop + 16f, paint)
        canvas.drawText("AMOUNT (₹)", tableLeft + 475f, tableTop + 16f, paint)

        // Table Rows
        var currentY = tableTop + 24f
        paint.color = darkGray
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        val displayItems = if (items.isNotEmpty()) items else JsonHelper.jsonToItems(bill.itemsJson)

        displayItems.forEachIndexed { index, item ->
            // Alternating zebra row
            if (index % 2 == 1) {
                paint.color = Color.parseColor("#F9FAFB")
                paint.style = Paint.Style.FILL
                canvas.drawRect(tableLeft, currentY, tableRight, currentY + 22f, paint)
            }

            paint.color = darkGray
            paint.style = Paint.Style.FILL
            paint.textSize = 9f
            canvas.drawText("${index + 1}", tableLeft + 8f, currentY + 15f, paint)
            canvas.drawText(item.title.take(34), tableLeft + 30f, currentY + 15f, paint)
            canvas.drawText(item.category, tableLeft + 250f, currentY + 15f, paint)
            canvas.drawText("${item.quantity}", tableLeft + 345f, currentY + 15f, paint)
            canvas.drawText("₹%,.0f".format(item.price), tableLeft + 400f, currentY + 15f, paint)
            canvas.drawText("₹%,.0f".format(item.lineTotal), tableLeft + 475f, currentY + 15f, paint)

            paint.color = borderGray
            paint.style = Paint.Style.STROKE
            canvas.drawLine(tableLeft, currentY + 22f, tableRight, currentY + 22f, paint)

            currentY += 22f
        }

        // Summary & Totals Block
        val summaryTop = currentY + 15f

        // Payment / Bank / QR Box on Left
        val payBoxWidth = 260f
        paint.color = Color.parseColor("#FEF3C7")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(tableLeft, summaryTop, tableLeft + payBoxWidth, summaryTop + 130f), 8f, 8f, paint)

        paint.color = Color.parseColor("#78350F")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BANK & UPI PAYMENT DETAILS", tableLeft + 12f, summaryTop + 18f, paint)

        paint.color = darkGray
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Bank: ${business.bankName}", tableLeft + 12f, summaryTop + 34f, paint)
        canvas.drawText("A/C No: ${business.bankAccount}", tableLeft + 12f, summaryTop + 48f, paint)
        canvas.drawText("IFSC: ${business.bankIfsc}", tableLeft + 12f, summaryTop + 62f, paint)
        canvas.drawText("UPI ID: ${business.upiId}", tableLeft + 12f, summaryTop + 76f, paint)

        // Draw generated UPI QR code inside payment box
        try {
            val upiUri = "upi://pay?pa=${business.upiId}&pn=${business.name.replace(" ", "%20")}&am=${bill.balance}&cu=INR"
            val qrBitmap = QrCodeGenerator.generateUpiQrBitmap(upiUri, 150)
            val qrRect = RectF(tableLeft + payBoxWidth - 65f, summaryTop + 24f, tableLeft + payBoxWidth - 10f, summaryTop + 79f)
            canvas.drawBitmap(qrBitmap, null, qrRect, paint)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        paint.textSize = 8.5f
        paint.color = Color.parseColor("#92400E")
        canvas.drawText("Scan QR Code to pay via GPay / PhonePe / Paytm", tableLeft + 12f, summaryTop + 95f, paint)
        canvas.drawText("Notes: ${bill.notes.take(45)}", tableLeft + 12f, summaryTop + 115f, paint)

        // Financial Calculation Table on Right
        val totalsLeft = pageWidth - 24f - 240f
        var totalY = summaryTop + 12f

        fun drawTotalLine(label: String, value: String, isBold: Boolean = false, color: Int = darkGray) {
            paint.style = Paint.Style.FILL
            paint.textSize = if (isBold) 11f else 9.5f
            paint.typeface = if (isBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = color
            canvas.drawText(label, totalsLeft, totalY, paint)
            val valWidth = paint.measureText(value)
            canvas.drawText(value, tableRight - valWidth, totalY, paint)
            totalY += 18f
        }

        drawTotalLine("Items Subtotal:", "₹%,.2f".format(bill.subtotal))
        if (bill.discount > 0) {
            drawTotalLine("Discount:", "- ₹%,.2f".format(bill.discount), color = Color.parseColor("#DC2626"))
        }
        if (bill.gstEnabled) {
            drawTotalLine("GST (${bill.gstRate.toInt()}%):", "+ ₹%,.2f".format(bill.gstAmount))
        }

        paint.color = borderGray
        paint.style = Paint.Style.STROKE
        canvas.drawLine(totalsLeft, totalY - 6f, tableRight, totalY - 6f, paint)

        drawTotalLine("Grand Total:", "₹%,.2f".format(bill.grandTotal), isBold = true, color = royalMaroon)
        drawTotalLine("Advance Paid:", "₹%,.2f".format(bill.advance), color = Color.parseColor("#16A34A"))
        drawTotalLine("Balance Due:", "₹%,.2f".format(bill.balance), isBold = true, color = Color.parseColor("#B91C1C"))

        // Footer Section
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#FDF2F8")
        canvas.drawRect(0f, pageHeight - 55f, pageWidth.toFloat(), pageHeight.toFloat(), paint)

        paint.color = royalMaroon
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val footerMsg = "Thank you for choosing ${business.name}!"
        val footerWidth = paint.measureText(footerMsg)
        canvas.drawText(footerMsg, (pageWidth - footerWidth) / 2f, pageHeight - 34f, paint)

        paint.color = darkGray
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val subFooter = "This is a computer-generated estimate/invoice • Powered by Mandap Vala"
        val subFooterWidth = paint.measureText(subFooter)
        canvas.drawText(subFooter, (pageWidth - subFooterWidth) / 2f, pageHeight - 18f, paint)

        pdfDocument.finishPage(page)

        // Save PDF to cache dir
        val outputDir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val sanitizedNum = bill.billNumber.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val outputFile = File(outputDir, "invoice_${sanitizedNum}.pdf")

        val fileOutputStream = FileOutputStream(outputFile)
        pdfDocument.writeTo(fileOutputStream)
        fileOutputStream.close()
        pdfDocument.close()

        return outputFile
    }
}
