package com.example.practica1

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.practica1.R

class Fragment4 : Fragment() {
    private val items = listOf("Elemento 1", "Elemento 2", "Elemento 3", "Elemento 4")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment4, container, false)
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerView)
        recycler.layoutManager = LinearLayoutManager(context)
        recycler.adapter = SimpleAdapter(items)
        return view
    }
}

class SimpleAdapter(private val data: List<String>) : RecyclerView.Adapter<SimpleAdapter.ViewHolder>() {
    class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(TextView(parent.context).apply { textSize = 18f; setPadding(16,16,16,16) })
    override fun onBindViewHolder(holder: ViewHolder, position: Int) { holder.textView.text = data[position] }
    override fun getItemCount() = data.size
}
