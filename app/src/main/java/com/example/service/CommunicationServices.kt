package com.example.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder

interface ISmsService {
    fun sendDueReminderSms(context: Context, customerName: String, phone: String, dueAmount: Double, shopName: String)
    fun sendCustomSms(context: Context, phone: String, message: String)
}

class SmsService : ISmsService {
    override fun sendDueReminderSms(
        context: Context,
        customerName: String,
        phone: String,
        dueAmount: Double,
        shopName: String
    ) {
        val message = "প্রিয় $customerName, আপনার বর্তমান বকেয়া ৳${dueAmount.toInt()}। দ্রুত পরিশোধের অনুরোধ রইল। - $shopName"
        sendCustomSms(context, phone, message)
    }

    override fun sendCustomSms(context: Context, phone: String, message: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${phone.trim()}")
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "SMS অ্যাপ চালু করা যায়নি: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}

interface IWhatsAppService {
    fun sendDueReminder(context: Context, customerName: String, phone: String, dueAmount: Double, shopName: String)
    fun sendInvoice(context: Context, phone: String, invoiceText: String)
}

class WhatsAppService : IWhatsAppService {
    override fun sendDueReminder(
        context: Context,
        customerName: String,
        phone: String,
        dueAmount: Double,
        shopName: String
    ) {
        val text = "প্রিয় $customerName,\n\nআপনার $shopName দোকানে বর্তমান বকেয়া ৳${dueAmount.toInt()}।\nসুবিধাজনক সময়ে পরিশোধের অনুরোধ করছি। ধন্যবাদ!"
        sendWhatsAppMessage(context, phone, text)
    }

    override fun sendInvoice(context: Context, phone: String, invoiceText: String) {
        sendWhatsAppMessage(context, phone, invoiceText)
    }

    private fun sendWhatsAppMessage(context: Context, rawPhone: String, message: String) {
        try {
            var formattedPhone = rawPhone.replace(" ", "").replace("-", "").replace("+", "")
            if (formattedPhone.startsWith("01")) {
                formattedPhone = "88$formattedPhone"
            }
            val encodedMessage = URLEncoder.encode(message, "UTF-8")
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=$encodedMessage")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp চালু করা যায়নি। অনুগ্রহ করে ইনস্টল করা আছে কিনা চেক করুন।", Toast.LENGTH_SHORT).show()
        }
    }
}
