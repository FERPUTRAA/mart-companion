package com.mart.companion

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import fi.iki.elonen.NanoHTTPD
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MartAccessibilityService : AccessibilityService() {

    private var server: LocalServer? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        server = LocalServer(this)
        try {
            server?.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        server?.stop()
    }

    // ==== Aksi: baca UI tree jadi JSON ====
    fun dumpUiTree(): JSONArray {
        val result = JSONArray()
        val root = rootInActiveWindow ?: return result
        collectNodes(root, result)
        return result
    }

    private fun collectNodes(node: AccessibilityNodeInfo, out: JSONArray) {
        if (node.text != null || node.contentDescription != null || node.isClickable) {
            val obj = JSONObject()
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            obj.put("text", node.text?.toString() ?: "")
            obj.put("desc", node.contentDescription?.toString() ?: "")
            obj.put("clickable", node.isClickable)
            obj.put("bounds", "[${bounds.left},${bounds.top}][${bounds.right},${bounds.bottom}]")
            out.put(obj)
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectNodes(it, out) }
        }
    }

    // ==== Aksi: tap di koordinat ====
    fun tapAt(x: Float, y: Float) {
        val path = Path()
        path.moveTo(x, y)
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        dispatchGesture(gesture, null, null)
    }

    // ==== Aksi: buka aplikasi lewat package name ====
    fun openApp(packageName: String): Boolean {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        return true
    }

    // ==== Server HTTP lokal, jembatan ke OpenJarvis di Termux ====
    inner class LocalServer(private val service: MartAccessibilityService) :
        NanoHTTPD(8765) {

        override fun serve(session: IHTTPSession): Response {
            return try {
                when (session.uri) {
                    "/ui" -> {
                        val tree = service.dumpUiTree()
                        newFixedLengthResponse(Response.Status.OK, "application/json", tree.toString())
                    }
                    "/tap" -> {
                        val params = session.parms
                        val x = params["x"]?.toFloatOrNull() ?: 0f
                        val y = params["y"]?.toFloatOrNull() ?: 0f
                        service.tapAt(x, y)
                        newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"ok\"}")
                    }
                    "/open" -> {
                        val pkg = session.parms["package"] ?: ""
                        val ok = service.openApp(pkg)
                        newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"$ok\"}")
                    }
                    else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "not found")
                }
            } catch (e: Exception) {
                newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", e.message ?: "error")
            }
        }
    }
}
