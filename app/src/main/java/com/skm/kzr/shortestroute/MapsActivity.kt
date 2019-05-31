package com.skm.kzr.shortestroute

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import org.json.JSONException
import android.graphics.Color.parseColor
import android.widget.Toast
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.Polyline
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response
import org.json.JSONObject
import org.json.JSONArray
import java.io.IOException


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    lateinit var GRAND_INDONESIA: LatLng;
    lateinit var ANCOL: LatLng;
    lateinit var ISTANA_NEGARA: LatLng;

    lateinit var WARUNG_PASTA_KEMANG: LatLng;
    lateinit var TAMAN_AYODYA: LatLng;

    private val API_KEY = "AIzaSyDSWRckJ6PowMaWuOWU9GA7fBjsAcySzd8";

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        GRAND_INDONESIA = LatLng(-6.19070923716, 106.819788387)
        ANCOL = LatLng(6.1330, 106.8267)
        ISTANA_NEGARA = LatLng(-6.16745433018, 106.821003383)

        WARUNG_PASTA_KEMANG = LatLng(-6.2703, 106.8151)
        TAMAN_AYODYA = LatLng(-6.21462, 106.84513)

        val client = OkHttpClient()
        val request = OkHttpRequest(client)

        val api_url = getMapsApiDirectionsUrl()
        Log.d("ANTA40", "Direction API: "+api_url)

        request.GET(api_url, object: Callback {

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body()?.string()
                Log.d("ANTA0", "JSON direction result: "+responseData)
                runOnUiThread {
                    if (responseData != null) {
                        this@MapsActivity.drawPathOnMap(responseData)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(applicationContext, "Exception: "+e.message, Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    fun getMapsApiDirectionsUrl(): String {
        val origin = "origin=" + WARUNG_PASTA_KEMANG.latitude + "," + WARUNG_PASTA_KEMANG.longitude;
        val waypoints = "waypoints=optimize:true|" + TAMAN_AYODYA.latitude + "," + TAMAN_AYODYA.longitude + "|"
        val destination = "destination=" + GRAND_INDONESIA.latitude + "," + GRAND_INDONESIA.longitude
        val sensor = "sensor=false"
        val key = "key="+API_KEY;
        val params = "$origin&$waypoints&$destination&$sensor&$key"
        val output = "json"

        return ("https://maps.googleapis.com/maps/api/directions/" + output + "?" + params);
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val jkt = LatLng( -6.2276235, 106.8069972)
        mMap.addMarker(MarkerOptions().position(jkt).title("Jakarta"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(jkt, 15.0f))
    }

    fun drawPathOnMap(result: String) {
        try {
            val json = JSONObject(result)
            val routeArray = json.getJSONArray("routes")
            val routes = routeArray.getJSONObject(0)
            val overviewPolylines = routes.getJSONObject("overview_polyline")
            val encodedString = overviewPolylines.getString("points")
            val list = decodePoly(encodedString)

            val line = mMap.addPolyline(
                PolylineOptions()
                    .addAll(list)
                    .width(15f)
                    .color(Color.parseColor("#443f3f"))
                    .geodesic(true)
            )

        } catch (e: JSONException) {
            runOnUiThread {
                Toast.makeText(applicationContext, "drawpPath exception: "+e.message, Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun decodePoly(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)

            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0

            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)

            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(
                lat.toDouble() / 1E5,
                lng.toDouble() / 1E5
            )
            poly.add(p)
        }

        return poly
    }
}
