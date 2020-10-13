package uk.nhs.nhsx.covid19.android.app.fieldtests.ui.participants

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import uk.nhs.nhsx.covid19.android.app.R

class ParticipantListActivity : AppCompatActivity(R.layout.participant_list) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val participants = intent.getStringArrayListExtra(PARTICIPANTS) ?: listOf<String>()
        val viewAdapter = ParticipantViewAdapter(participants)

        val linearLayoutManager = LinearLayoutManager(this)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            // setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = linearLayoutManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(menuItem)
    }

    companion object {
        const val PARTICIPANTS = "participants"

        fun newInstance(context: Context, participants: List<String>): Intent {
            val intent = Intent(context, ParticipantListActivity::class.java)
            intent.putStringArrayListExtra(PARTICIPANTS, ArrayList(participants))
            return intent
        }
    }
}
