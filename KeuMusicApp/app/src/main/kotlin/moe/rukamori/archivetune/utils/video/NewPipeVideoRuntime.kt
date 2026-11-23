/*
 * KeuMusic video extraction runtime.
 * Uses the GPL-3.0 NewPipe Extractor library.
 */
package moe.rukamori.archivetune.utils.video

import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import java.util.Locale

internal object NewPipeVideoRuntime {
    @Volatile
    private var initialized = false

    fun initialize() {
        if (initialized) return

        synchronized(this) {
            if (initialized) return

            val locale = Locale.getDefault()
            val language = locale.language.takeIf(String::isNotBlank) ?: "en"
            val country = locale.country
            val localization = Localization(language, country)
            val contentCountry = if (country.isBlank()) {
                ContentCountry.DEFAULT
            } else {
                ContentCountry(country)
            }

            NewPipe.init(NewPipeVideoDownloader(), localization, contentCountry)
            initialized = true
        }
    }
}
