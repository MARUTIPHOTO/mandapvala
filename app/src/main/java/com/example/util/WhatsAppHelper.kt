package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.Bill
import com.example.data.model.BusinessProfile
import java.io.File
import java.net.URLEncoder

object WhatsAppHelper {

    fun shareBillViaWhatsApp(
        context: Context,
        bill: Bill,
        business: BusinessProfile,
        pdfFile: File?
    ) {
        val message = """
            ✨ *INVOICE / ESTIMATE* ✨
            *${business.name}*
            
            📄 *Bill No:* ${bill.billNumber}
            👤 *Customer:* ${bill.customerName}
            📞 *Contact:* ${bill.customerMobile}
            🗓️ *Event:* ${bill.eventType} on ${bill.eventDate}
            📍 *Venue:* ${bill.customerAddress}
            
            ━━━━━━━━━━━━━━━━━━━━
            💰 *Total Amount:* ₹%,.2f
            ${if (bill.advance > 0) "✅ *Advance Paid:* ₹%,.2f\n" else ""}⚠️ *Balance Due:* ₹%,.2f
            ━━━━━━━━━━━━━━━━━━━━
            
            🏦 *UPI ID for Payment:* ${business.upiId}
            📞 *Call/WhatsApp:* ${business.whatsapp}
            
            _Thank you for choosing ${business.name}!_
        """.trimIndent().format(bill.grandTotal, bill.advance, bill.balance)

        if (pdfFile != null && pdfFile.exists()) {
            sharePdfWithText(context, pdfFile, message, bill.customerMobile)
        } else {
            sendTextMessage(context, bill.customerMobile, message)
        }
    }

    fun shareCustomerGalleryLink(
        context: Context,
        customerName: String,
        customerMobile: String,
        business: BusinessProfile,
        customerId: Long
    ) {
        val liveLink = "https://ais-pre-phbombsc476himodsizzja-294621985383.asia-southeast1.run.app/view/catalog/"
        val message = """
            🙏 *Namaste $customerName ji!*
            
            Greetings from *${business.name}*!
            
            Please browse our exclusive Mandap, Stage, Sofa & Flower Decoration catalog and select your favorite designs:
            
            👉 *Open Online Catalog:*
            $liveLink
            
            🌐 *Official Website:* https://mandapvala.in
            
            Browse photos, select your preferred setups, and send choices directly on WhatsApp.
            
            📞 *Contact:* ${business.mobile}
        """.trimIndent()

        sendTextMessage(context, customerMobile, message)
    }

    fun sendTextMessage(context: Context, phone: String, message: String) {
        try {
            val cleanPhone = phone.replace("[^0-9]".toRegex(), "")
            val formattedPhone = when {
                cleanPhone.length == 10 -> "91$cleanPhone"
                cleanPhone.length > 10 -> cleanPhone
                else -> ""
            }
            val url = if (formattedPhone.isNotEmpty()) {
                "https://api.whatsapp.com/send?phone=$formattedPhone&text=${URLEncoder.encode(message, "UTF-8")}"
            } else {
                "https://api.whatsapp.com/send?text=${URLEncoder.encode(message, "UTF-8")}"
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to generic share
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, message)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share via WhatsApp"))
        }
    }

    private fun sharePdfWithText(context: Context, pdfFile: File, message: String, phone: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // Attempt WhatsApp package if installed
                setPackage("com.whatsapp")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // WhatsApp not installed or failed, open generic chooser
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    pdfFile
                )
                val chooserIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, message)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(chooserIntent, "Share Invoice PDF"))
            } catch (ex: Exception) {
                Toast.makeText(context, "Could not share PDF: ${ex.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
