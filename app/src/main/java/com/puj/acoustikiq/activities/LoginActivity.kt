package com.puj.acoustikiq.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.puj.acoustikiq.R
import com.puj.acoustikiq.databinding.ActivityLoginBinding
import com.puj.acoustikiq.util.Alerts

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private var alerts = Alerts(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.loginButton.setOnClickListener {
            login()
        }

        binding.signupButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.forgotButton.setOnClickListener {
            recoverPassword()
        }
    }

    private fun validateFields(): Boolean {
        if (binding.loginEmail.editText?.text.toString()
                .isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(binding.loginEmail.editText?.text.toString())
                .matches()
        ) {
            binding.loginEmail.error = getString(R.string.mail_error_label)
            return false
        } else binding.loginEmail.isErrorEnabled = false

        if (binding.loginPass.editText?.text.toString().isEmpty()) {
            binding.loginPass.error = getString(R.string.error_pass_label)
            return false
        } else binding.loginPass.isErrorEnabled = false

        return true
    }

    private fun login() {
        if (validateFields()) {
            disableFields()
            auth.signInWithEmailAndPassword(
                binding.loginEmail.editText?.text.toString(),
                binding.loginPass.editText?.text.toString()
            ).addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    binding.animationView.setAnimation(R.raw.success_animation)
                    binding.animationView.setRepeatCount(0)
                    binding.animationView.playAnimation()
                    binding.animationView.addAnimatorListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        }
                    })
                } else {
                    binding.animationView.setAnimation(R.raw.fail_animation)
                    binding.animationView.setRepeatCount(0)
                    binding.animationView.playAnimation()
                    task.exception?.localizedMessage?.let {
                        alerts.indefiniteSnackbar(
                            binding.root, it
                        )
                    }
                }
                enableFields()
            }
        }
    }

    private fun recoverPassword() {
        if (binding.loginEmail.editText?.text.toString()
                .isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(binding.loginEmail.editText?.text.toString())
                .matches()
        ) {
            binding.loginEmail.error = getString(R.string.mail_error_label)
            return
        } else binding.loginEmail.isErrorEnabled = false

        auth.sendPasswordResetEmail(
            binding.loginEmail.editText?.text.toString()
        ).addOnCompleteListener(this) { task ->
            var msg: String = ""
            if (task.isSuccessful) {
                msg = "Email enviado, revise su correo."
            } else {
                task.exception?.localizedMessage?.let {
                    msg = it
                }
            }
            alerts.indefiniteSnackbar(
                binding.root, msg
            )
        }
    }

    private fun disableFields() {
        binding.loginEmail.isEnabled = false
        binding.loginPass.isEnabled = false
        binding.loginButton.isEnabled = false
        binding.signupButton.isEnabled = false
        binding.forgotButton.isEnabled = false
    }

    private fun enableFields() {
        binding.loginEmail.isEnabled = true
        binding.loginPass.isEnabled = true
        binding.loginButton.isEnabled = true
        binding.signupButton.isEnabled = true
        binding.forgotButton.isEnabled = true
    }
}