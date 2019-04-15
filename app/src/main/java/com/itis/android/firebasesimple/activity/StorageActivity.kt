package com.itis.android.firebasesimple.activity

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.itis.android.firebasesimple.R
import com.itis.android.firebasesimple.model.Human
import kotlinx.android.synthetic.main.activity_storage.*
import java.lang.StringBuilder

class StorageActivity : AppCompatActivity() {

    private lateinit var storage: FirebaseFirestore
    private lateinit var collection: CollectionReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_storage)
        storage = FirebaseFirestore.getInstance()
        collection = storage.collection(COLLECTION_EXAMPLE)
        setListeners()
    }

    private fun setListeners() {
        btn_add.setOnClickListener {
            val name = et_name.text.toString()
            val surname = et_surname.text.toString()
            val human = Human(name, surname)
            collection.add(human)
        }
        btn_search.setOnClickListener {
            val searchText = et_search.text.toString()
            collection.whereEqualTo("name", searchText).get().addOnCompleteListener {
                if (it.isSuccessful) {
                    var resultString = ""
                    it.result?.let { result ->
                        for (document in result) {
                            val name = document.getString("name")
                            val surname = document.getString("surname")
                            resultString = resultString.plus("$name $surname\n")
                        }
                        tv_search_result.text = resultString
                    }
                } else {
                    tv_search_result.text = getString(R.string.search_error)
                }
            }
        }
    }

    companion object {
        private const val COLLECTION_EXAMPLE = "sample"
    }

}
