package com.itis.android.firebasesimple.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.itis.android.firebasesimple.model.User
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AuthRepository(
        private val firebaseAuth: FirebaseAuth,
) {

    fun signInRx(email: String, pass: String): Single<User> = Single.create<FirebaseUser> { emitter ->
        firebaseAuth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener {
                    it.user?.let {
                        emitter.onSuccess(it)
                    } ?: run {
                        emitter.onError(NullPointerException("User is Null"))
                    }

                }.addOnFailureListener {
                    emitter.onError(it)
                }
    }.observeOn(Schedulers.io())
            .map { User(it.displayName ?: "Test") }
            .map {
//                userDao.insert(it)
                it
            }

    suspend fun signIn(email: String, pass: String): FirebaseUser {
        val user = suspendCancellableCoroutine<FirebaseUser> { cor ->
            firebaseAuth.signInWithEmailAndPassword(email, pass)
                    .addOnSuccessListener {
                        it.user?.let { user ->
                            cor.resume(user)
                        } ?: run {
                            cor.resumeWithException(NullPointerException("User is Null"))
                        }

                    }.addOnFailureListener {
                        cor.resumeWithException(NullPointerException("User is Null"))
                    }
        }
//        dao.insert(user)
        return user
    }

    fun signUp() {

    }

    fun signOut() {
    }
}
