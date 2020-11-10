package uk.nhs.nhsx.covid19.android.app.availability

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity

fun AppCompatActivity.openAppStore() {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=uk.nhs.covid19.production")))
    } catch (exception: ActivityNotFoundException) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=uk.nhs.covid19.production")))
    }
}
