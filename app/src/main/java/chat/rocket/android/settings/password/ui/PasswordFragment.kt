package chat.rocket.android.settings.password.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import chat.rocket.android.R
import chat.rocket.android.analytics.AnalyticsManager
import chat.rocket.android.analytics.event.ScreenViewEvent
import chat.rocket.android.settings.password.presentation.PasswordPresenter
import chat.rocket.android.settings.password.presentation.PasswordView
import chat.rocket.android.util.extension.asObservable
import chat.rocket.android.util.extensions.inflate
import chat.rocket.android.util.extensions.textContent
import chat.rocket.android.util.extensions.ui
import dagger.android.support.AndroidSupportInjection
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Observables
import kotlinx.android.synthetic.main.fragment_password.*
import javax.inject.Inject

internal const val TAG_PASSWORD_FRAGMENT = "PasswordFragment"

class PasswordFragment : Fragment(), PasswordView, ActionMode.Callback {
    @Inject
    lateinit var presenter: PasswordPresenter
    @Inject
    lateinit var analyticsManager: AnalyticsManager
    private var actionMode: ActionMode? = null
    private val disposables = CompositeDisposable()

    companion object {
        fun newInstance() = PasswordFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.fragment_password)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        disposables.add(listenToChanges())

        analyticsManager.logScreenView(ScreenViewEvent.Password)
    }

    override fun onDestroyView() {
        disposables.clear()
        super.onDestroyView()
    }

    override fun hideLoading() {
        ui {
            layout_new_password.visibility = View.VISIBLE
            layout_confirm_password.visibility = View.VISIBLE
            view_loading.visibility = View.GONE
        }
    }

    override fun onActionItemClicked(mode: ActionMode, menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_password -> {
                presenter.updatePassword(text_new_password.textContent)
                mode.finish()
                return true
            }
            else -> {
                false
            }
        }
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu?): Boolean {
        mode.menuInflater.inflate(R.menu.password, menu)
        mode.title = resources.getString(R.string.action_confirm_password)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

    override fun onDestroyActionMode(mode: ActionMode?) {
        actionMode = null
    }

    override fun showLoading() {
        ui {
            layout_new_password.visibility = View.GONE
            layout_confirm_password.visibility = View.GONE
            view_loading.visibility = View.VISIBLE
        }
    }

    override fun showPasswordFailsUpdateMessage(error: String?) {
        showToast("Password fails to update: " + error)
    }

    override fun showPasswordSuccessfullyUpdatedMessage() {
        showToast("Password was successfully updated!")
    }

    private fun finishActionMode() = actionMode?.finish()

    private fun listenToChanges(): Disposable {
        return Observables.combineLatest(
            text_new_password.asObservable(),
            text_confirm_password.asObservable()
        ).subscribe {
            val textPassword = text_new_password.textContent
            val textConfirmPassword = text_confirm_password.textContent

            if (textPassword.length > 5 && textConfirmPassword.length > 5 && textPassword.equals(
                    textConfirmPassword
                )
            )
                startActionMode()
            else
                finishActionMode()
        }
    }

    private fun showToast(msg: String?) {
        ui {
            Toast.makeText(it, msg, Toast.LENGTH_LONG).show()
        }
    }

    private fun startActionMode() {
        if (actionMode == null) {
            actionMode = (activity as PasswordActivity).startSupportActionMode(this)
        }
    }
}