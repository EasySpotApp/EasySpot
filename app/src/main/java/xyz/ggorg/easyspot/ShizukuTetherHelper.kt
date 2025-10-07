package xyz.ggorg.easyspot

import android.annotation.SuppressLint
import android.net.IIntResultListener
import android.net.ITetheringConnector
import android.net.TetheringRequestParcel
import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

@SuppressLint("DeprecatedSinceApi")
object ShizukuTetherHelper {
    /*
     * android.net
     * ConnectivityManager / TetheringManager constants
     */

    /* TetheringType */
    private const val TETHERING_WIFI = 0

    /* TetheringManager service */
    private const val TETHERING_SERVICE = "tethering"

    /* Tether error codes */
    private const val TETHER_ERROR_NO_ERROR = 0
    private const val TETHER_ERROR_NO_CHANGE_TETHERING_PERMISSION = 14

    fun setHotspotEnabledShizuku(
        enabled: Boolean,
        exemptFromEntitlementCheck: Boolean = true,
        shouldShowEntitlementUi: Boolean = false,
        retryCount: Int = 0
    ): Boolean {
        Log.d(this.toString(), "entering setHotspotEnabledShizuku(enabled = ${enabled})...")

        if (retryCount > 5) {
            Log.e(this.toString(), "setHotspotEnabledShizuku: reached max retry count")
            return false
        }

        HiddenApiBypass.addHiddenApiExemptions("Landroid/net")

        return runCatching {
            val tetheringMgr = SystemServiceHelper.getSystemService(TETHERING_SERVICE)
                .let(::ShizukuBinderWrapper)
                .let(ITetheringConnector.Stub::asInterface)

            if (enabled) {
                val resultListener = object : IIntResultListener.Stub() {
                    override fun onResult(resultCode: Int) {
                        when (resultCode) {
                            TETHER_ERROR_NO_ERROR -> {
                                Log.d(this.toString(), "setHotspotEnabledShizuku(true) - success")
                            }

                            TETHER_ERROR_NO_CHANGE_TETHERING_PERMISSION -> {
                                setHotspotEnabledShizuku(
                                    enabled,
                                    false,
                                    shouldShowEntitlementUi,
                                    retryCount + 1
                                )
                            }

                            else -> {
                                Log.d(
                                    this.toString(),
                                    "setHotspotEnabledShizuku(true) - failed. code = $resultCode"
                                )
                            }
                        }
                    }
                }

                tetheringMgr.startTethering(
                    createTetheringRequestParcel(
                        exemptFromEntitlementCheck,
                        shouldShowEntitlementUi
                    ) as TetheringRequestParcel,
                    "com.android.shell",
                    "",
                    resultListener
                )
            } else {
                val resultListener = object : IIntResultListener.Stub() {
                    override fun onResult(resultCode: Int) {
                        when (resultCode) {
                            TETHER_ERROR_NO_ERROR -> {
                                Log.d(this.toString(), "setHotspotEnabledShizuku(false) - success")
                            }

                            else -> {
                                Log.e(
                                    this.toString(),
                                    "setHotspotEnabledShizuku(false) - failed. code = $resultCode"
                                )
                            }
                        }
                    }
                }

                tetheringMgr.stopTethering(
                    TETHERING_WIFI,
                    "com.android.shell",
                    "",
                    resultListener
                )
            }

            true
        }.getOrElse {
            Log.e(this.toString(), it.toString())
            false
        }
    }

    private fun createTetheringRequest(
        exemptFromEntitlementCheck: Boolean = true,
        shouldShowEntitlementUi: Boolean = false
    ): Any {
        return Class.forName("android.net.TetheringManager\$TetheringRequest\$Builder").run {
            val setExemptFromEntitlementCheck =
                getDeclaredMethod("setExemptFromEntitlementCheck", Boolean::class.java)
            val setShouldShowEntitlementUi =
                getDeclaredMethod("setShouldShowEntitlementUi", Boolean::class.java)
            val build = getDeclaredMethod("build")

            getConstructor(Int::class.java).run {
                this.newInstance(TETHERING_WIFI).let {
                    setExemptFromEntitlementCheck.invoke(it, exemptFromEntitlementCheck)
                    setShouldShowEntitlementUi.invoke(it, shouldShowEntitlementUi)
                    build.invoke(it)
                }
            }
        }
    }

    private fun createTetheringRequestParcel(
        exemptFromEntitlementCheck: Boolean = true,
        shouldShowEntitlementUi: Boolean = false
    ): Any {
        return getRequestParcel(
            createTetheringRequest(
                exemptFromEntitlementCheck,
                shouldShowEntitlementUi
            )
        )
    }

    private fun getRequestParcel(request: Any): Any {
        return Class.forName("android.net.TetheringManager\$TetheringRequest").run {
            val getParcel = getDeclaredMethod("getParcel")
            getParcel.invoke(request)
        }!!
    }
}