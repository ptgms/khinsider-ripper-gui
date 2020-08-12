package xyz.ptgms.khinsiderripper

import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.log10

object progressBar {
    fun printProgress(startTime: Long, total: Long, current: Long) {
        val eta =
            if (current == 0L) 0 else (total - current) * (System.currentTimeMillis() - startTime) / current
        val etaHms = if (current == 0L) "N/A" else String.format(
            "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(eta),
            TimeUnit.MILLISECONDS.toMinutes(eta) % TimeUnit.HOURS.toMinutes(1),
            TimeUnit.MILLISECONDS.toSeconds(eta) % TimeUnit.MINUTES.toSeconds(1)
        )
        val string = StringBuilder(140)
        val percent = (current * 100 / total).toInt()
        string
            .append('\r')
            .append(
                java.lang.String.join(
                    "",
                    Collections.nCopies(
                        if (percent == 0) 2 else 2 - log10(percent.toDouble()).toInt(),
                        " "
                    )
                )
            )
            .append(String.format(" %d%% [", percent))
            .append(java.lang.String.join("", Collections.nCopies(percent, "=")))
            .append('>')
            .append(java.lang.String.join("", Collections.nCopies(100 - percent, " ")))
            .append(']')
            .append(
                java.lang.String.join(
                    "",
                    Collections.nCopies(
                        log10(total.toDouble()).toInt() - log10(current.toDouble()).toInt(),
                        " "
                    )
                )
            )
            .append(String.format(" %d/%d, ETA: %s", current, total, etaHms))
        print(string)
    }
}