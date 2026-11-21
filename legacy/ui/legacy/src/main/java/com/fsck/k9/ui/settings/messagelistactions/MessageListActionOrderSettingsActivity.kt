package com.fsck.k9.ui.settings.messagelistactions

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.BaseActivity
import com.fsck.k9.ui.base.extensions.fragmentTransaction

/**
 * Hosts the screen for ordering the screen reader actions offered on message list items.
 */
class MessageListActionOrderSettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLayout(R.layout.general_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.message_list_action_order_settings_title)

        if (savedInstanceState == null) {
            fragmentTransaction {
                replace(
                    R.id.generalSettingsContainer,
                    MessageListActionOrderSettingsFragment(),
                )
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, MessageListActionOrderSettingsActivity::class.java))
        }
    }
}
