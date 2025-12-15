package com.example.brookesellerinventoryapp;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.app.TaskStackBuilder;

public final class Notifications {
    public static final String CHANNEL_ID = "stock_alerts";
    private static final int CHANNEL_IMPORTANCE = NotificationManager.IMPORTANCE_DEFAULT;
    private Notifications() {}

    // Make a channel so notifications can show
    public static void ensureChannel(Context ctx) {
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
            CharSequence name = "Inventory Alerts";
            String desc = "Stock and inventory notifications";
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, name, CHANNEL_IMPORTANCE);
            ch.setDescription(desc);
            nm.createNotificationChannel(ch);
        }
    }
    private static int toNotifId(long id) {
        return (int)((id ^ (id >>> 32)) & 0x7fffffff);
    }

    // Show an out of stock notifications
    public static void notifyZeroStock(Context ctx, long itemId, String sku, String name) {
        ensureChannel(ctx);
        Intent detail = new Intent(ctx, ItemProductActivity.class)
                .putExtra("EXTRA_ID", itemId)
                .putExtra("EXTRA_SKU", sku)
                .putExtra("EXTRA_NAME", name);

        // Build a proper back stack so Back goes to MainActivity
        TaskStackBuilder stack = TaskStackBuilder.create(ctx);
        stack.addParentStack(ItemProductActivity.class);
        stack.addNextIntent(detail);
        PendingIntent contentPI = stack.getPendingIntent(
                toNotifId(itemId),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Text shown on the notification
        String title = "Out of stock";
        String shortText = (name == null ? "" : name) +
                ((sku == null || sku.isEmpty()) ? "" : " (SKU: " + sku + ")") +
                " has reached 0.";
        String bigText = "“" + (name == null ? "Unknown item" : name) + "”"
                + ((sku == null || sku.isEmpty()) ? "" : " [SKU: " + sku + "]")
                + " is out of stock. Tap to view the item.";

        // Notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_inventory)
                .setContentTitle(title)
                .setContentText(shortText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setContentIntent(contentPI)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setColor(ContextCompat.getColor(ctx, R.color.brown));

        // runtime permission
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // if there is not permission don't do anything
            return;
        }

        NotificationManagerCompat.from(ctx).notify(toNotifId(itemId), builder.build());
    }
}
