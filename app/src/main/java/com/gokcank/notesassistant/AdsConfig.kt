package com.gokcank.notesassistant

/**
 * AdMob kimlikleri local.properties'ten BuildConfig'e aktarılır
 * (bkz. app/build.gradle.kts). local.properties yoksa Google'ın test
 * kimlikleriyle derlenir; kaynak kodda gerçek kimlik bulunmaz.
 */
object AdsConfig {
    val BANNER_AD_UNIT_ID: String = BuildConfig.ADMOB_BANNER_ID
}
