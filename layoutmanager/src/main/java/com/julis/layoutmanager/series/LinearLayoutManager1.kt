package com.julis.layoutmanager.series

import androidx.recyclerview.widget.RecyclerView

/**
 * Created by @juliswang on 2023/10/19 19:58
 *
 * @Description
 *              实现只添加展示可见范围的的 itemView ，而不是将所有的 View 一次性全部加载完
 */
class LinearLayoutManager1 : RecyclerView.LayoutManager() {
    private val TAG = "JLayoutManager"

    override fun isAutoMeasureEnabled(): Boolean {
        return true
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.MATCH_PARENT,
            RecyclerView.LayoutParams.MATCH_PARENT
        )
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        // 垂直方向上的的空间大小
        var remainSpace = height - paddingTop
        //垂直方向的偏移量
        var offsetTop = 0
        var currentPosition = 0
        while (remainSpace > 0 && currentPosition < state.itemCount) {
            // 从适配器获取与给定位置关联的视图
            val itemView = recycler.getViewForPosition(currentPosition)
            // 将视图添加到 RecyclerView 中
            addView(itemView)
            // 测量并布局视图
            measureChildWithMargins(itemView, 0, 0)
            // 拿到宽高（包括ItemDecoration）
            val itemWidth = getDecoratedMeasuredWidth(itemView)
            val itemHeight = getDecoratedMeasuredHeight(itemView)
            // 对要添加的子 View 进行布局
            layoutDecorated(itemView, 0, offsetTop, itemWidth, offsetTop + itemHeight)
            offsetTop += itemHeight
            currentPosition++
            // 可用空间减少
            remainSpace -= itemHeight
        }
    }

    override fun canScrollHorizontally(): Boolean {
        return true
    }

    override fun scrollHorizontallyBy(dx: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        // 在这里处理左右滚动逻辑，dx 表示滚动的距离
        // 平移所有子视图
        offsetChildrenHorizontal(-dx)
        // 如果实际滚动距离与 dx 相同，返回 dx；如果未滚动，返回 0
        return dx
    }

    override fun canScrollVertically(): Boolean {
        return true
    }

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        // 在这里处理上下的滚动逻辑，dy 表示滚动的距离
        // 平移所有子视图
        offsetChildrenVertical(-dy)
        // 如果实际滚动距离与 dy 相同，返回 dy；如果未滚动，返回 0
        return dy
    }


}