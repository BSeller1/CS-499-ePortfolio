package com.example.brookesellerinventoryapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat

object Notifications {
    const val CHANNEL_ID: String = "stock_alerts"
    private const val CHANNEL_IMPORTANCE = NotificationManager.IMPORTANCE_DEFAULT

    // Make a channel so notifications can show
    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService<NotificationManager?>(NotificationManager::class.java)
        if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
            val name: CharSequence = "Inventory Alerts"
            val desc = "Stock and inventory notifications"
            val ch = NotificationChannel(CHANNEL_ID, name, CHANNEL_IMPORTANCE)
            ch.setDescription(desc)
            nm.createNotificationChannel(ch)
        }
    }

    private fun toNotifId(id: Long): Int {
        return ((id xor (id ushr 32)) and 0x7fffffffL).toInt()
    }

    // Show an out of stock notifications
    fun notifyZeroStock(ctx: Context, itemId: Long, sku: String?, name: String?) {
        ensureChannel(ctx)
        val detail = Intent(ctx, ItemProductActivity::class.java)
            .putExtra("EXTRA_ID", itemId)
            .putExtra("EXTRA_SKU", sku)
            .putExtra("EXTRA_NAME", name)

        // Build a proper back stack so Back goes to MainActivity
        val stack = TaskStackBuilder.create(ctx)
        stack.addParentStack(ItemProductActivity::class.java)
        stack.addNextIntent(detail)
        val contentPI = stack.getPendingIntent(
            toNotifId(itemId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Text shown on the notification
        val title = "Out of stock"
        val shortText = (name ?: "") +
                (if (sku == null || sku.isEmpty()) "" else " (SKU: $sku)") +
                " has reached 0."
        val bigText = ("“" + (name ?: "Unknown item") + "”"
                + (if (sku == null || sku.isEmpty()) "" else " [SKU: $sku]")
                + " is out of stock. Tap to view the item.")

        // Notification
        val builder = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_inventory)
            .setContentTitle(title)
            .setContentText(shortText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(contentPI)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setColor(ContextCompat.getColor(ctx, R.color.brown))

        // runtime permission
        if (ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // if there is not permission don't do anything
            return
        }

        NotificationManagerCompat.from(ctx).notify(toNotifId(itemId), builder.build())
    }
}
