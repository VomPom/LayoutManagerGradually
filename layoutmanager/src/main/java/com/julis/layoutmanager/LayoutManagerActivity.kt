package com.julis.layoutmanager

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.julis.layoutmanager.series.*

/**
 *
 * Created by @juliswang on 2023/10/19 10:46
 *
 * @Description
 */
class LayoutManagerActivity : Activity() {
    private lateinit var rvExampleList: RecyclerView
    private lateinit var rvLayoutOption: RecyclerView
    private lateinit var layoutAdapter: LayoutManagerAdapter

    companion object {
        const val MAX_DATA_SIZE = 50
    }

    private val dataList: MutableList<String> = mutableListOf()
    private val layoutManageDescMap: MutableMap<Class<out RecyclerView.LayoutManager>, String> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recyclerview)
        initData()
        initView()
    }

    private fun initView() {
        rvExampleList = findViewById(R.id.rv_empty_list)
        rvExampleList.layoutManager = MyLinearLayoutManager()
        val adapter = MyAdapter()
        rvExampleList.adapter = adapter
        adapter.setData(dataList)

        rvLayoutOption = findViewById(R.id.rv_layout_option)
        rvLayoutOption.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        layoutAdapter = LayoutManagerAdapter(onLayoutManagerListener)
        rvLayoutOption.adapter = layoutAdapter
        layoutAdapter.setData(layoutManageDescMap.values.toList())

        findViewById<Button>(R.id.btn_scroll_to_30).setOnClickListener {
            rvExampleList.scrollToPosition(30)
        }
        findViewById<Button>(R.id.btn_smooth_scroll_to_30).setOnClickListener {
            rvExampleList.smoothScrollToPosition(30)
        }
    }

    private fun initData() {
        for (index in 0 until MAX_DATA_SIZE) {
            dataList.add("index:${index}")
        }
        layoutManageDescMap.apply {
            put(MostSimpleLayoutManager::class.java, "0 最简单的LayoutManger")
            put(LinearLayoutManager1::class.java, "1 更合理的数据添加方式")
            put(LinearLayoutManager2::class.java, "2 对屏幕外的View回收")
            put(LinearLayoutManager3::class.java, "3 向上滑动的时View的填充")
            put(LinearLayoutManager4::class.java, "4 两个方向的View填充")
            put(LinearLayoutManager5::class.java, "5 对顶部和底部滑动边界处理")
            put(LinearLayoutManager6::class.java, "6 实现 scrollToPosition")
            put(LinearLayoutManager7::class.java, "7 实现 smoothScrollToPosition")
        }
    }

    private val onLayoutManagerListener = object : LayoutManagerAdapter.OnLayoutManagerListener {
        @SuppressLint("NotifyDataSetChanged")
        override fun onLayoutManagerSelected(position: Int) {
            val layoutClass = layoutManageDescMap.keys.toList()[position]
            val constructor = layoutClass.getConstructor()
            val layoutManager = constructor.newInstance()
            rvExampleList.layoutManager = layoutManager
            (rvExampleList.adapter as MyAdapter).notifyDataSetChanged()
        }
    }

    class MyAdapter : RecyclerView.Adapter<MyViewHolder>() {
        private val dataList: MutableList<String> = mutableListOf()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.rv_item_test, parent, false)
            return MyViewHolder(view)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.updateView(dataList[position])
        }

        override fun getItemCount(): Int {
            return dataList.size
        }

        fun setData(outerData: List<String>) {
            this.dataList.clear();
            this.dataList.addAll(outerData)
            notifyDataSetChanged()
        }
    }

    class MyViewHolder(private val rootView: View) : RecyclerView.ViewHolder(rootView) {
        fun updateView(text: String) {
            rootView.findViewById<TextView>(R.id.tv_test).text = text
        }

    }

    class LayoutManagerAdapter(private val listener: OnLayoutManagerListener) :
        RecyclerView.Adapter<LayoutManagerViewHolder>() {
        private val dataList: MutableList<String> = mutableListOf()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LayoutManagerViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.rv_item_layoutmanager, parent, false)
            return LayoutManagerViewHolder(view, listener)
        }

        override fun onBindViewHolder(holder: LayoutManagerViewHolder, position: Int) {
            holder.updateView(dataList[position], position)
        }

        override fun getItemCount(): Int {
            return dataList.size
        }

        fun setData(outerData: List<String>) {
            this.dataList.clear();
            this.dataList.addAll(outerData)
            notifyDataSetChanged()
        }

        interface OnLayoutManagerListener {
            fun onLayoutManagerSelected(position: Int)
        }
    }

    class LayoutManagerViewHolder(
        private val rootView: View,
        private val listener: LayoutManagerAdapter.OnLayoutManagerListener
    ) : RecyclerView.ViewHolder(rootView) {
        fun updateView(desc: String, position: Int) {
            rootView.findViewById<TextView>(R.id.tv_desc).apply {
                text = desc
                setOnClickListener { listener.onLayoutManagerSelected(position) }
            }
        }
    }
}