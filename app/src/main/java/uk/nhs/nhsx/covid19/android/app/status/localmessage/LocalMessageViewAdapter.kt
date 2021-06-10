package uk.nhs.nhsx.covid19.android.app.status.localmessage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_local_message_content_block.view.localMessageContentDescription
import kotlinx.android.synthetic.main.item_local_message_content_block.view.localMessageContentLinkView
import uk.nhs.nhsx.covid19.android.app.R.layout
import uk.nhs.nhsx.covid19.android.app.remote.data.ContentBlock
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener

private typealias OpenUrl = (String) -> Unit

class LocalMessageViewAdapter(
    private val contentBlocks: List<ContentBlock>,
    private val openUrl: OpenUrl
) : RecyclerView.Adapter<LocalMessageViewHolder>() {

    override fun getItemCount(): Int = contentBlocks.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocalMessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return LocalMessageViewHolder(inflater.inflate(layout.item_local_message_content_block, parent, false))
    }

    override fun onBindViewHolder(holder: LocalMessageViewHolder, position: Int) {
        holder.bind(contentBlocks[position], openUrl)
    }
}

class LocalMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(contentBlock: ContentBlock, openUrl: OpenUrl) = with(itemView) {
        setupDescription(contentBlock)
        setupLink(contentBlock, openUrl)
    }

    private fun View.setupDescription(contentBlock: ContentBlock) {
        localMessageContentDescription.isVisible = contentBlock.text != null
        localMessageContentDescription.text = contentBlock.text
    }

    private fun View.setupLink(
        contentBlock: ContentBlock,
        openUrl: OpenUrl
    ) {
        localMessageContentLinkView.isVisible = contentBlock.link != null
        if (contentBlock.link != null) {
            localMessageContentLinkView.text = contentBlock.linkText ?: contentBlock.link
            localMessageContentLinkView.setOnSingleClickListener {
                openUrl(contentBlock.link)
            }
        }
    }
}
