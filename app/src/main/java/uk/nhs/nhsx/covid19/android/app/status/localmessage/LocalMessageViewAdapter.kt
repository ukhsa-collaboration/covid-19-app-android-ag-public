package uk.nhs.nhsx.covid19.android.app.status.localmessage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import uk.nhs.nhsx.covid19.android.app.databinding.ItemLocalMessageContentBlockBinding
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
        val itemBinding = ItemLocalMessageContentBlockBinding.inflate(inflater, parent, false)
        return LocalMessageViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: LocalMessageViewHolder, position: Int) {
        holder.bind(contentBlocks[position], openUrl)
    }
}

class LocalMessageViewHolder(private val itemBinding: ItemLocalMessageContentBlockBinding) :
    RecyclerView.ViewHolder(itemBinding.root) {
    fun bind(contentBlock: ContentBlock, openUrl: OpenUrl) = with(itemView) {
        setupDescription(contentBlock)
        setupLink(contentBlock, openUrl)
    }

    private fun setupDescription(contentBlock: ContentBlock) = with(itemBinding) {
        localMessageContentDescription.isVisible = contentBlock.text != null
        localMessageContentDescription.text = contentBlock.text
    }

    private fun setupLink(
        contentBlock: ContentBlock,
        openUrl: OpenUrl
    ) = with(itemBinding) {
        localMessageContentLinkView.isVisible = contentBlock.link != null
        if (contentBlock.link != null) {
            localMessageContentLinkView.text = contentBlock.linkText ?: contentBlock.link
            localMessageContentLinkView.setOnSingleClickListener {
                openUrl(contentBlock.link)
            }
        }
    }
}
