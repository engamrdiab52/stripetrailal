package com.amrabdelhamiddiab.stripeclientkotlin2

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var paymentIntentClientSecret: String
    private lateinit var publishableKey: String
    private lateinit var paymentSheet: PaymentSheet

    private lateinit var payButton: Button
    private lateinit var goShopping: Button

    private val httpClient = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Hook up the pay button
        payButton = findViewById(R.id.pay_button)
        goShopping = findViewById(R.id.shopping_btn)
        payButton.setOnClickListener(::onPayClicked)
        goShopping.setOnClickListener{
            fetchPaymentIntent()
        }
        payButton.isEnabled = false
        goShopping.isEnabled = false

        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)

        fetchPublishableKey()

    }



    //fetch publishable key from the server
    private fun fetchPublishableKey() {
        val request = Request.Builder().url(BACKEND_URL + "config").build()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                displayAlert("Request faild.......", "Error: $e")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    displayAlert("Request not successful", "Error: $response")
                } else {
                    val responseData = response.body?.string()
                    val responseJson = responseData?.let { JSONObject(it) } ?: JSONObject()
                    publishableKey = responseJson.getString("publishableKey")
                    Log.d(TAG, publishableKey)
                    PaymentConfiguration.init(applicationContext, publishableKey)
                  runOnUiThread { goShopping.isEnabled = true }
                }
            }
        })
    }

    private fun onPayClicked(view: View) {

        val configuration = PaymentSheet.Configuration("Pharmacy")
        //fetchPaymentIntent()
        // Present Payment Sheet
        paymentSheet.presentWithPaymentIntent(paymentIntentClientSecret, configuration)
    }

     private fun fetchPaymentIntent() {
         val url = "${BACKEND_URL}create-payment-intent"

         val shoppingCartContent = """
             {
                 "items": [
                     {"id":"xl-tshirt"}
                 ]
             }
         """

         val mediaType = "application/json; charset=utf-8".toMediaType()

         val body = shoppingCartContent.toRequestBody(mediaType)
         val request = Request.Builder()
             .url(url)
             .post(body)
             .build()

         OkHttpClient()
             .newCall(request)
             .enqueue(object: Callback {
                 override fun onFailure(call: Call, e: IOException) {
                     displayAlert("Failed to load data", "Error: $e")
                 }

                 override fun onResponse(call: Call, response: Response) {
                     if (!response.isSuccessful) {
                         displayAlert("Failed to load page", "Error: $response")
                     } else {
                         val responseData = response.body?.string()
                         val responseJson = responseData?.let { JSONObject(it) } ?: JSONObject()
                         paymentIntentClientSecret = responseJson.getString("clientSecret")
                         runOnUiThread { payButton.isEnabled = true }
                         Log.i(Companion.TAG, "Retrieved PaymentIntent")
                     }
                 }
             })
     }
    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun displayAlert(title: String, message: String? = null) {
        runOnUiThread {
            val builder = AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
            builder.setPositiveButton("Ok", null)
            builder.create().show()
        }
    }

    private fun onPaymentSheetResult(paymentResult: PaymentSheetResult) {
        when (paymentResult) {
            is PaymentSheetResult.Completed -> {
                showToast("Payment complete!")
            }
            is PaymentSheetResult.Canceled -> {
                Log.i(Companion.TAG, "Payment canceled!")
            }
            is PaymentSheetResult.Failed -> {
                displayAlert("Payment failed", paymentResult.error.localizedMessage)
            }
        }
    }

    companion object {
        const val TAG = "MainActivity"
        const val BACKEND_URL = "http://10.0.2.2:3000/"
    }
}