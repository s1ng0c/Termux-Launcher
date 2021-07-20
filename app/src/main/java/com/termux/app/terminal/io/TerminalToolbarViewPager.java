package com.termux.app.terminal.io;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.text.Editable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.terminal.io.extrakeys.ExtraKeysView;
import com.termux.terminal.TerminalSession;

import java.util.ArrayList;
import java.util.List;

public class TerminalToolbarViewPager {

    public static class PageAdapter extends PagerAdapter {

        final TermuxActivity mActivity;
        final List<ApplicationInfo> mPackages = new ArrayList<>();

        ApplicationInfo mReadyApplication;
        String mSavedTextInput;

        public PageAdapter(TermuxActivity activity, String savedTextInput) {
            this.mActivity = activity;
            this.mSavedTextInput = savedTextInput;
            this.updatePackagesList();
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup collection, int position) {
            LayoutInflater inflater = LayoutInflater.from(mActivity);
            View layout;
            if (position == 1) {
                layout = inflater.inflate(R.layout.view_terminal_toolbar_extra_keys, collection, false);
                ExtraKeysView extraKeysView = (ExtraKeysView) layout;
                extraKeysView.setTermuxTerminalViewClient(mActivity.getTermuxTerminalViewClient());
                extraKeysView.setTermuxTerminalSessionClient(mActivity.getTermuxTerminalSessionClient());
                mActivity.setExtraKeysView(extraKeysView);
                extraKeysView.reload(mActivity.getProperties().getExtraKeysInfo());

                // apply extra keys fix if enabled in prefs
                if (mActivity.getProperties().isUsingFullScreen() && mActivity.getProperties().isUsingFullScreenWorkAround()) {
                    FullScreenWorkAround.apply(mActivity);
                }

            } else {
                layout = inflater.inflate(R.layout.view_terminal_toolbar_text_input, collection, false);
                final EditText editText = layout.findViewById(R.id.terminal_toolbar_text_input);
                final TextView textView = layout.findViewById(R.id.terminal_toolbar_suggestion_applications_view);

                if (mSavedTextInput != null) {
                    editText.setText(mSavedTextInput);
                    mSavedTextInput = null;
                }

                textView.setMovementMethod(LinkMovementMethod.getInstance());
                editText.addTextChangedListener(new TextWatcher(){
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        textView.setText("");
                        textView.setVisibility(View.INVISIBLE);

                        SpannableStringBuilder builder = buildSuggestionList(s);
                        if ((s.length() != 0) && (builder.length() > 0)) {
                            textView.setText(builder);
                            textView.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                });

                editText.setOnEditorActionListener((v, actionId, event) -> {
                    String textToSend = editText.getText().toString();

                    if (textToSend.length() == 0) return false;

                    if (handleSpecialCommand(textToSend)
                        || tryToOpenUrl(textToSend)
                        || tryToOpenSuggestionApplication(this.mReadyApplication)
                        || sendCommandToTerminal(textToSend + '\r')) {
                        editText.setText("");
                    }

                    return true;
                });
            }

            collection.addView(layout);
            return layout;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup collection, int position, @NonNull Object view) {
            collection.removeView((View) view);
        }

        private boolean handleSpecialCommand(String command) {
            if (command == null) return false;

            // used to refresh package list for some case such as  new package installed,
            // package got uninstalled, etc.,
            if (command.contentEquals("/update")) {
                updatePackagesList();
                return true;
            }

            return false;
        }

        private boolean tryToOpenUrl(String url) {
            if (!url.startsWith("http://") && !url.startsWith("https://")
                && !url.startsWith("ftp://") && !url.startsWith("ftps://"))
                url = "http://" + url;

            if (URLUtil.isValidUrl(url) && Patterns.WEB_URL.matcher(url).matches()) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                mActivity.startActivity(browserIntent);
                return true;
            }

            return false;
        }

        private boolean tryToOpenSuggestionApplication(ApplicationInfo info) {
            if (info == null) return false;

            final PackageManager pm = mActivity.getPackageManager();
            if (pm != null) {
                final Intent intent = pm.getLaunchIntentForPackage(info.processName);
                if (intent != null) {
                    mActivity.startActivity(intent);
                    return true;
                }
            }

            return false;
        }

        private boolean sendCommandToTerminal(String command) {
            TerminalSession session = mActivity.getCurrentSession();
            if (session != null) {
                if (session.isRunning()) {
                    if (command.length() == 0) command = "\r";
                        session.write(command);
                } else {
                    mActivity.getTermuxTerminalSessionClient().removeFinishedSession(session);
                }

                return true;
            }

            return false;
        }

        private void updatePackagesList() {
            // avoid block UI thread, let's scan packages on other thread
            new Thread(() -> {
                final PackageManager pm = mActivity.getPackageManager();
                if (pm != null) {
                    mPackages.clear();
                    pm.getInstalledApplications(PackageManager.GET_META_DATA).forEach(info -> {
                        if (pm.getLaunchIntentForPackage(info.processName) != null) {
                            mPackages.add(info);
                        }
                    });
                }
            }).start();
        }

        private SpannableStringBuilder buildSuggestionList(CharSequence str) {
            SpannableStringBuilder labelBuilder = new SpannableStringBuilder();
            List<ApplicationInfo> packages = filterPackagesBy(str.toString());

            mReadyApplication = null;
            packages.forEach(info -> {
                final PackageManager pm = mActivity.getPackageManager();
                if (pm != null) {
                    CharSequence name = pm.getApplicationLabel(info);
                    SpannableString label = new SpannableString(name);

                    label.setSpan(
                        new ClickableSpan() {
                            @Override
                            public void onClick(@NonNull View widget) {
                                tryToOpenSuggestionApplication(info);
                            }

                            @Override
                            public void updateDrawState(TextPaint ds) {
                                super.updateDrawState(ds);
                                ds.setUnderlineText(false);
                            }
                        }, 0,
                        name.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    // highlight first item
                    if (labelBuilder.length() == 0) {
                        label.setSpan(new BackgroundColorSpan(Color.YELLOW), 0,
                            name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                        label.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0,
                            name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                        // pick the first matched application to launch when Enter gets hit
                        mReadyApplication = info;
                    } else {
                        label.setSpan(new BackgroundColorSpan(Color.WHITE), 0,
                            name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        labelBuilder.append("/");
                    }

                    labelBuilder.append(label);
                }
            });

            return labelBuilder;
        }

        /*
         * implements simple package name filter
         */
        private List<ApplicationInfo> filterPackagesBy(String prefix) {
            List<ApplicationInfo> packages = new ArrayList<>();
            String purePrefix = prefix.toLowerCase().replaceAll("[*$]+", "");

            mPackages.forEach(info -> {
                final PackageManager pm = mActivity.getPackageManager();
                if (pm != null) {
                    String name = pm.getApplicationLabel(info).toString().toLowerCase();

                    if ((prefix.startsWith("*")) && (prefix.endsWith("$"))) {
                        if (name.endsWith(purePrefix)) packages.add(info);
                        return;
                    }

                    if (prefix.startsWith("*")) {
                        if (name.contains(purePrefix)) packages.add(info);
                        return;
                    }

                    if (prefix.endsWith("$")) {
                        if (name.contentEquals(purePrefix)) packages.add(info);
                        return;
                    }

                    if(name.startsWith(purePrefix)) packages.add(info);
                }
            });

            return packages;
        }
    }

    public static class OnPageChangeListener extends ViewPager.SimpleOnPageChangeListener {

        final TermuxActivity mActivity;
        final ViewPager mTerminalToolbarViewPager;

        public OnPageChangeListener(TermuxActivity activity, ViewPager viewPager) {
            this.mActivity = activity;
            this.mTerminalToolbarViewPager = viewPager;
        }

        @Override
        public void onPageSelected(int position) {
            if (position == 1) {
                mActivity.getTerminalView().requestFocus();
            } else {
                final EditText editText = mTerminalToolbarViewPager.findViewById(R.id.terminal_toolbar_text_input);
                if (editText != null) editText.requestFocus();
            }
        }

    }

}
