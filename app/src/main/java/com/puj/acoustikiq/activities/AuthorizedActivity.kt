package com.puj.acoustikiq.activities

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.puj.acoustikiq.model.UserProfile
import com.puj.acoustikiq.util.Alerts

open class AuthorizedActivity : AppCompatActivity() {
    private var auth: FirebaseAuth = Firebase.auth
    protected var currentUser = auth.currentUser
    protected lateinit var user: UserProfile
    protected val database = Firebase.database
    protected val refData = database.getReference("users/${currentUser?.uid}")

    protected var alerts = Alerts(this)

    override fun onResume() {
        super.onResume()
        if (auth.currentUser == null) {
            logout()
        } else {
            loadUserData {  }
        }
    }


    protected fun loadUserData(onUserLoaded: () -> Unit) {
        refData.get().addOnSuccessListener { data ->
            data.getValue(UserProfile::class.java)?.let {
                user = it
                onUserLoaded()
            } ?: run {
                alerts.showErrorDialog("Error", "No se pudo cargar la información del usuario.")
            }
        }.addOnFailureListener {
            alerts.showErrorDialog("Error", "Fallo al obtener la información del usuario.")
        }
    }

    protected fun logout() {
        auth.signOut()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }
}