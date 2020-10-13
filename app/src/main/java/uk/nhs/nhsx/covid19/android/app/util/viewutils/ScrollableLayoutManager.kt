package uk.nhs.nhsx.covid19.android.app.util.viewutils

import android.content.Context
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ScrollableLayoutManager(
    context: Context,
    private val nestedScrollView: NestedScrollView,
    private val recyclerView: RecyclerView
) : LinearLayoutManager(context) {

    fun scrollToIndex(index: Int) {
        val firstVisibleItemPosition = findFirstVisibleItemPosition()
        val lastVisibleItemPosition = findLastVisibleItemPosition()
        val itemsShown = lastVisibleItemPosition - firstVisibleItemPosition + 1

        if (itemsShown == recyclerView.adapter?.itemCount) {
            nestedScrollView.smoothScrollTo(
                0,
                (recyclerView.y + recyclerView.getChildAt(index).y).toInt()
            )
        }
    }
}
