package uk.nhs.nhsx.covid19.android.app.scenariodialog

import android.annotation.SuppressLint
import android.content.Context
import android.database.DataSetObserver
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SpinnerAdapter
import android.widget.TextView

class SimpleSpinnerAdapter(val context: Context, val stringList: List<String>) : SpinnerAdapter {
    override fun getItemViewType(position: Int) = 0
    override fun getViewTypeCount() = 1
    override fun hasStableIds(): Boolean = true
    override fun getCount() = stringList.size
    override fun getItem(position: Int) = stringList[position]
    override fun getItemId(position: Int): Long = position.toLong()
    override fun isEmpty(): Boolean = stringList.isEmpty()

    private val observerList: MutableList<DataSetObserver> = mutableListOf()

    override fun registerDataSetObserver(observer: DataSetObserver?) {
        if (observer != null) observerList.add(observer)
    }

    override fun unregisterDataSetObserver(observer: DataSetObserver?) {
        if (observer != null) observerList.remove(observer)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View = constructView(position, convertView, parent)
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?) = constructView(position, convertView, parent)

    @SuppressLint("SetTextI18n")
    private fun constructView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val resultView = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_expandable_list_item_1, parent, false)

        val textView: TextView = resultView.findViewById(android.R.id.text1)
        textView.text = "#${position + 1}: ${stringList[position]}"

        return resultView
    }
}
