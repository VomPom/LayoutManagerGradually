package com.julis.layoutmanager.series

import android.view.View
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

/**
 * Created by @juliswang on 2023/10/19 19:58
 *
 * @Description 对 scrollToPosition 方法进行支持
 *
 */
class LinearLayoutManager6 : RecyclerView.LayoutManager(){
    private val TAG = "JLayoutManager"

    private val orientationVerticalHelper = OrientationHelper.createVerticalHelper(this)

    private var mPendingScrollPosition = RecyclerView.NO_POSITION
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
        // [detachAndScrapAttachedViews]
        // 它的主要作用是将所有已附加到 RecyclerView 的子视图（即 item）从布局中分离并放入回收池（RecyclerPool）
        // 调用 detachAndScrapAttachedViews() 时，LayoutManager 会将所有当前附加的子视图从布局中分离，但不会将它们从回收池中移除。
        // 这意味着这些视图仍然可以被回收和复用，从而提高性能和内存效率。
        // 在自定义 LayoutManager 的 onLayoutChildren() 方法中，通常需要在布局新的子视图之前调用 detachAndScrapAttachedViews()。
        // 这样可以确保 RecyclerView 在重新布局时正确处理所有子视图的回收和复用。
        detachAndScrapAttachedViews(recycler)

        // 垂直方向上的的空间大小
        var remainSpace = orientationVerticalHelper.totalSpace
        //垂直方向的偏移量
        var offsetTop = 0
        var currentPosition = 0
        if (mPendingScrollPosition != RecyclerView.NO_POSITION) {
            currentPosition = mPendingScrollPosition
        }

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
        return false
    }


    override fun canScrollVertically(): Boolean {
        return true
    }

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State?): Int {
        // 填充 view
        val adjustedDy = fillView(dy, recycler)
        // 移动 view
        offsetChildrenVertical(-adjustedDy)
        // 回收 View
        recycleInvisibleView(adjustedDy, recycler)
        // 由于需要对边界进行限制，所以需要对原始的 dy 进行修正，这里不再直接返回 dy
        return adjustedDy
    }

    /**
     * 填充新进入屏幕内的 ItemView
     *     getChildCount():childCount-> 当前屏幕内RecyclerView展示的 ItemView 数量
     *     getItemCount():itemCount-> 最大的 ItemView 数量，也就是 Adapter 传递的数据的数量
     */
    private fun fillView(dy: Int, recycler: RecyclerView.Recycler): Int {
        val verticalSpace = orientationVerticalHelper.totalSpace
        var remainSpace = 0
        var nextFillPosition = 0
        //垂直方向的偏移量
        var offsetTop = 0
        var offsetLeft = 0
        val absDy = abs(dy)
        // 从下往上滑，那么需要向底部添加数据
        if (dy > 0) {
            val anchorView = getChildAt(childCount - 1) ?: return dy
            val anchorPosition = getPosition(anchorView)
            val anchorBottom = getDecoratedBottom(anchorView)
            offsetLeft = getDecoratedLeft(anchorView)
            remainSpace = verticalSpace - anchorBottom
            // 垂直可用的数据为<0，意外着这时候屏幕底部的位置刚好在最底部的 ItemView 上，还需要向上滑动一点点...我们才能添加 View
            if (remainSpace < 0) {
                return dy
            }
            nextFillPosition = anchorPosition + 1
            if (nextFillPosition >= itemCount && anchorBottom - absDy < verticalSpace) {
                // 最底下的 View 的底部位置，如果 - absDy < verticalSpace(也就是RecyclerView的最大可布局的高度)
                // 需要对齐进行边界处理：不需要滑动 absDy 这么多，而是anchorBottom - verticalSpace 的距离，否则底部最后一个元素会有底部白色
                //
                //                                    |  .B. |
                //                         |      |   |      |   |  .C. |         1、最左边小的矩形为屏幕的大小（假设这时候RecyclerView宽高都是 MatchParent）
                //                         |  .A. |   |      |   |      |         2、标记为 A 长条就是 RecyclerView的内容,与第一个矩形在高度一致的是可见的 View区域 (为方便观察，我将齐往右边移动了一部分距离)
                //                         |      |   |      |   |      |         3、标记为 B 长条则是如果按照 dy 移动之后的样子（这时候会让 RecyclerView 底部有一部分空白的）
                //                   ______|_     |   |      |   |      |         4、标记为 C 长条是我们理想的滑动距离---保持底部边界不超过屏幕最底下的距离
                //                   |     | |    |   |      |   |      |
                //   verticalSpace   |     | |    |   |      |   |      |
                //                   |     | |    |   |______|   |      |
                //                   |_____|_|    |              |______|
                //                         |______|            anchorBottom - verticalSpace
                //                        anchorBottom
                //
                return anchorBottom - verticalSpace
            }
            offsetTop = anchorBottom
        } else if (dy < 0) {  // 从上往下滑，那么需要向顶部添加数据
            val anchorView = getChildAt(0) ?: return dy
            val anchorPosition = getPosition(anchorView)
            val anchorTop = getDecoratedTop(anchorView)
            offsetLeft = getDecoratedLeft(anchorView)
            remainSpace = anchorTop
            // 垂直方向顶部可用的数据为<0，意味着这时候屏幕顶部的位置刚好在最底部的 ItemView 上，还需要向下滑动一点点...我们才能添加 View
            if (remainSpace < 0) {
                // 这时候滑动还没到有一点点空隙可以填充view的程度，所以按照原有的滑动数据进行滑动
                return dy
            }
            nextFillPosition = anchorPosition - 1
            // 已经是第0个数据了，前面没有其他数据了
            if (nextFillPosition < 0 && anchorTop + absDy > 0) {
                // 第0个view 顶部看不到的区域高度如果+滑动的数据绝对值>0,那么需要进行一次边界处理：按照最大顶部看不到的区域高度返回
                // 否则，类似扫一下 滑动的话，并按原有的滑动进行的话，会让第0个元素的顶部产生一定的空隙，试着将这里返回dy，可以看看效果
                return anchorTop
            }
            val itemHeight = getDecoratedMeasuredHeight(anchorView)
            // 新的布局的itemView 的顶部位置应该以 anchorTop - itemHeight 开始
            offsetTop = anchorTop - itemHeight
        }

        while (remainSpace > 0 &&
            ((nextFillPosition < itemCount) && (nextFillPosition >= 0))
        ) {
            // 从适配器获取与给定位置关联的视图
            val itemView = recycler.getViewForPosition(nextFillPosition)
            // 将视图添加到 RecyclerView 中k，从顶部添加的话，需要加到最前的位置
            if (dy > 0) {
                addView(itemView)
            } else {
                addView(itemView, 0)
            }
            // 测量并布局视图
            measureChildWithMargins(itemView, 0, 0)
            // 拿到宽高（包括ItemDecoration）
            val itemWidth = getDecoratedMeasuredWidth(itemView)
            val itemHeight = getDecoratedMeasuredHeight(itemView)
            // 对要添加的子 View 进行布局，相比onLayoutChildren 里面的实现添加了：offsetLeft（因为我们没有禁止掉 左右的滑动）
            // 试着把 offsetLeft 改成0，也就是最原始的样子，然后左右上下滑滑，你会有意外收获
            layoutDecorated(itemView, offsetLeft, offsetTop, itemWidth + offsetLeft, offsetTop + itemHeight)
            if (dy > 0) {
                offsetTop += itemHeight
                nextFillPosition++
            } else {
                offsetTop -= itemHeight
                nextFillPosition--
            }
            // 可用空间减少
            remainSpace -= itemHeight
        }
        return dy
    }

    /**
     * 回收掉在界面上看不到的 ItemView
     *
     * @param dy
     * @param recycler
     */
    private fun recycleInvisibleView(dy: Int, recycler: RecyclerView.Recycler) {
        val totalSpace = orientationVerticalHelper.totalSpace

        // 将要回收View的集合
        val recycleViews = hashSetOf<View>()
        // 从下往上滑
        if (dy > 0) {
            for (i in 0 until childCount) {
                val child = getChildAt(i)!!
                // 从下往上滑从最上面的 item 开始计算
                val top = getDecoratedTop(child)
                // 判断最顶部的 item 是否已经完全不可见，如何可见，那说明底下的 item 也是可见
                val height = top - getDecoratedBottom(child)
                if (height - top < 0) {
                    break
                }
                recycleViews.add(child)
            }
        } else if (dy < 0) {  // 从上往下滑
            for (i in childCount - 1 downTo 0) {
                val child = getChildAt(i)!!
                // 从上往下滑从最底部的 item 开始计算
                val bottom = getDecoratedBottom(child)
                // 判断最底部的 item 是否已经完全不可见，如何可见，那说明上面的 item 也是可见
                val height = bottom - getDecoratedTop(child)
                if (bottom - totalSpace < height) {
                    break
                }
                recycleViews.add(child)
            }
        }

        // 真正把 View 移除掉的逻辑
        for (view in recycleViews) {
            // [removeAndRecycleView]
            // 用于从视图层次结构中删除某个视图，并将其资源回收，以便在需要时重新利用
            removeAndRecycleView(view, recycler)
        }
        recycleViews.clear()
    }

    override fun scrollToPosition(position: Int) {
        super.scrollToPosition(position)
        if (position < 0 || position >= itemCount) {
            return
        }
        mPendingScrollPosition = position
        requestLayout()
    }

}