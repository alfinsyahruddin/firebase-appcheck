package dev.alfin.iFirebase

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize
import dev.alfin.iFirebase.databinding.ActivityMainBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        initDebug()

        binding.btnGetSecret.setOnClickListener {
            onClickBtnGetSecret()
        }
    }

    private fun init() {
        Firebase.initialize(context = this)
        Firebase.appCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance(),
        )
    }

    private fun initDebug() {
        Firebase.initialize(context = this)
        Firebase.appCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance(),
        )
    }

    private fun onClickBtnGetSecret() {
        binding.progressBar.visibility = View.VISIBLE

        Firebase.appCheck.getAppCheckToken(true)
            .addOnSuccessListener { appCheckToken ->
                Log.d("AppCheck", "Token: ${appCheckToken.token}")

                HttpClient.secretService.getSecret(appCheckToken.token)
                    .enqueue(object : Callback<SecretResponse> {
                        override fun onResponse(
                            call: Call<SecretResponse>,
                            response: Response<SecretResponse>
                        ) {
                            if (response.isSuccessful) {
                                alert("✅ Success", "Secret: ${response.body()?.secret ?: ""}")
                            } else {
                                alert("❌ Failed", response.message())
                            }
                            binding.progressBar.visibility = View.INVISIBLE
                        }

                        override fun onFailure(call: Call<SecretResponse>, t: Throwable) {
                            alert("❌ Failed", t.localizedMessage ?: "")
                            binding.progressBar.visibility = View.INVISIBLE
                        }
                    })
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = View.INVISIBLE
                Log.e("AppCheck", "Error retrieving App Check token", exception)
            }
    }
}

fun AppCompatActivity.alert(title: String, message: String) {
    val alertDialog = AlertDialog.Builder(this)

    alertDialog.apply {
        setTitle(title)
        setMessage(message)
        setPositiveButton("OK") { _, _ -> }
    }.create().show()
}
