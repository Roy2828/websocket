package com.nofish.staggeredgridlayoutmanager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        val rvMain: RecyclerView =
        findViewById<RecyclerView>(R.id.rv_main).apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
//                StaggeredGridLayoutManager(3, RecyclerView.HORIZONTAL)
            adapter = StAdapter()
        }
    }

}

class StAdapter : RecyclerView.Adapter<StViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StViewHolder {
        val itemView: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_layout, parent, false)
        return StViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return 4
    }

    override fun onBindViewHolder(holder: StViewHolder, position: Int) {
        holder.itemView.run {
            findViewById<RecyclerView>(R.id.rv_child).run {
                StaggeredGridLayoutManager(3, RecyclerView.HORIZONTAL)

//                var layoutParams: StaggeredGridLayoutManager.LayoutParams? = layoutParams as StaggeredGridLayoutManager.LayoutParams?
//                if (layoutParams == null) {
//                    layoutParams = StaggeredGridLayoutManager.LayoutParams(
//                        ViewGroup.LayoutParams.MATCH_PARENT,
//                        ViewGroup.LayoutParams.MATCH_PARENT
//                    )
//                }
//                layoutParams.isFullSpan = true
//                setLayoutParams(layoutParams)
            }
        }
    }
}

class StViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

}