package com.streetfood.radar.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.streetfood.radar.R

/**
 * Actividad Principal Nativa de StreetFood Radar.
 * Ejecuta la plataforma web completa de StreetFood Radar en un WebView acelerado por hardware,
 * cargado directamente desde los assets locales (file:///android_asset/index.html).
 * Incluye soporte completo para GPS nativo, Leaflet, Tailwind y los 71 locales de Chile.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    // Vistas de Splash Screen Animado
    private lateinit var splashContainer: View
    private lateinit var splashLogo: View
    private lateinit var splashGlow: View
    private lateinit var splashTitle: View
    private lateinit var splashSubtitle: View
    private var isSplashDismissed = false

    private var pendingGeoCallback: GeolocationPermissions.Callback? = null
    private var pendingGeoOrigin: String? = null

    // Manejador de permisos de ubicación en tiempo de ejecución (para GPS y Modo Estudiante)
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        pendingGeoCallback?.invoke(pendingGeoOrigin, granted, false)
        pendingGeoCallback = null
        pendingGeoOrigin = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)

        splashContainer = findViewById(R.id.splashContainer)
        splashLogo = findViewById(R.id.splashLogo)
        splashGlow = findViewById(R.id.splashGlow)
        splashTitle = findViewById(R.id.splashTitle)
        splashSubtitle = findViewById(R.id.splashSubtitle)

        startSplashAnimation()
        configureWebView()
        setupBackNavigation()

        // Carga la aplicación completa desde los assets empaquetados
        webView.loadUrl("file:///android_asset/index.html")
    }

    /**
     * Animación fluida de entrada del Logo: Escala, rebote elástico y resplandor pulsante
     */
    private fun startSplashAnimation() {
        // Estado inicial
        splashLogo.scaleX = 0.3f
        splashLogo.scaleY = 0.3f
        splashLogo.alpha = 0f

        splashGlow.scaleX = 0.5f
        splashGlow.scaleY = 0.5f
        splashGlow.alpha = 0f

        splashTitle.alpha = 0f
        splashTitle.translationY = 40f
        splashSubtitle.alpha = 0f
        splashSubtitle.translationY = 40f

        // Animación de entrada del Logo con efecto rebote
        splashLogo.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1.0f)
            .setDuration(700)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.8f))
            .start()

        // Animación del Resplandor
        splashGlow.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .alpha(0.45f)
            .setDuration(800)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        // Animación de texto
        splashTitle.animate()
            .alpha(1.0f)
            .translationY(0f)
            .setDuration(600)
            .setStartDelay(250)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        splashSubtitle.animate()
            .alpha(1.0f)
            .translationY(0f)
            .setDuration(600)
            .setStartDelay(350)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        // Transición de salida de respaldo (después de 2.2 segundos máximo para no retrasar al usuario)
        splashContainer.postDelayed({
            dismissSplashScreen()
        }, 2200)
    }

    /**
     * Desvanece suavemente la pantalla de Splash con animación alfa y escala
     */
    private fun dismissSplashScreen() {
        if (isSplashDismissed) return
        isSplashDismissed = true

        splashContainer.animate()
            .alpha(0f)
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(450)
            .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
            .withEndAction {
                splashContainer.visibility = View.GONE
            }
            .start()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        val settings = webView.settings

        // Habilitar motor de JavaScript y almacenamiento DOM para Leaflet y Tailwind
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true

        // Optimización de renderizado y escala de pantalla
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false

        // Caché local para navegación ultra fluida
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        // Aceleración por hardware
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // WebChromeClient para manejar GPS / Geolocalización
        webView.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                val hasFine = ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                val hasCoarse = ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (hasFine || hasCoarse) {
                    callback?.invoke(origin, true, false)
                } else {
                    pendingGeoCallback = callback
                    pendingGeoOrigin = origin
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress < 100) {
                    progressBar.visibility = View.VISIBLE
                } else {
                    progressBar.visibility = View.GONE
                }
            }
        }

        // WebViewClient para manejar enlaces externos (ej: Google Maps para navegación)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                // Retener 1 segundo mínimo para apreciar la animación y luego desvanecer
                splashContainer.postDelayed({
                    dismissSplashScreen()
                }, 1000)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false

                // Si el usuario presiona "Cómo llegar con GPS", abrir en la app externa de Google Maps
                if (url.startsWith("https://www.google.com/maps") || url.startsWith("geo:")) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                        return true
                    } catch (e: Exception) {
                        // Fallback: abrir en el navegador
                    }
                }

                // Mantener la navegación interna dentro del WebView
                return false
            }
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
