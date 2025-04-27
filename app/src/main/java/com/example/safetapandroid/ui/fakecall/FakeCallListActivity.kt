package com.example.safetapandroid.ui.fakecall

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.safetapandroid.R
import com.example.safetapandroid.network.FakeCall
import com.example.safetapandroid.ui.fakecall.FakeCallSchedulerActivity
import com.example.safetapandroid.utils.FakeCallStorage

class FakeCallListActivity : AppCompatActivity() {

    private lateinit var adapter: FakeCallAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fake_call_list)

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view_calls)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = FakeCallAdapter(
            calls = FakeCallStorage.getCalls(this).toMutableList(),
            onDelete = { call -> deleteFakeCall(call) },
            onEdit = { call -> editFakeCall(call) }
        )
        recyclerView.adapter = adapter
    }

    private fun deleteFakeCall(fakeCall: FakeCall) {
        val calls = FakeCallStorage.getCalls(this).toMutableList()
        calls.removeIf { it.id == fakeCall.id }
        FakeCallStorage.saveCalls(this, calls)
        adapter.updateList(calls)
        Toast.makeText(this, "Фейковый звонок удален", Toast.LENGTH_SHORT).show()
    }

    private fun editFakeCall(fakeCall: FakeCall) {
        val intent = Intent(this, FakeCallSchedulerActivity::class.java)
        intent.putExtra("edit_call_id", fakeCall.id)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        reloadFakeCalls()
    }

    private fun reloadFakeCalls() {
        val calls = FakeCallStorage.getCalls(this).toMutableList()
        adapter.updateList(calls)
    }

}