package com.layercake.rxexamples.retrywhen;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.Unbinder;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.query)
    EditText queryEntry;
    @BindView(R.id.avatarImage)
    ImageView avatarImage;

    private Unbinder unbinder;

    private GitHubService gitHubService;
    private Disposable disposable;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        unbinder = ButterKnife.bind(this);

        gitHubService = ServiceFactory.create(GitHubService.class);

        Timber.plant(new Timber.DebugTree());
    }

    @OnEditorAction(R.id.query)
    boolean editorSearch() {
        searchGitHub();

        return true;
    }

    @OnClick(R.id.searchButton)
    void buttonSearch() {
        searchGitHub();
    }

    private void searchGitHub() {
        hideKeyboard();

        if (disposable != null && !disposable.isDisposed()) {
            Timber.i("Disposing previous search");
            disposable.dispose();
        }

        disposable = gitHubService.user(queryEntry.getText().toString())
                                  .observeOn(AndroidSchedulers.mainThread())
                                  .retryWhen(new RetryWhenObservable())
                                  .subscribe(this::bindData,
                                          throwable -> Timber.e(throwable.getLocalizedMessage(), throwable));
    }

    private void hideKeyboard() {
        final View view = this.getCurrentFocus();
        if (view != null) {
            final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void bindData(final User user) {
        Picasso.with(this)
               .load(user.getAvatarUrl())
               .into(avatarImage);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Dispose of any searches that are currently in progress
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (unbinder != null) {
            unbinder.unbind();
        }
    }
}
