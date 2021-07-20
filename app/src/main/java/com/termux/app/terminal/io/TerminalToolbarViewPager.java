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
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.terminal.io.extrakeys.ExtraKeysView;
import com.termux.terminal.TerminalSession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TerminalToolbarViewPager {

    public static class PageAdapter extends PagerAdapter {

        final TermuxActivity mActivity;
        final List<ApplicationInfo> mPackages = new ArrayList<>();

        ApplicationInfo mSuggestedApplication;
        String mSavedTextInput;

        public PageAdapter(TermuxActivity activity, String savedTextInput) {
            this.mActivity = activity;
            this.mSavedTextInput = savedTextInput;
            this.scanPackages();
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
                mActivity.setExtraKeysView(extraKeysView);
                extraKeysView.reload(mActivity.getProperties().getExtraKeysInfo());

                // apply extra keys fix if enabled in prefs
                if (mActivity.getProperties().isUsingFullScreen() && mActivity.getProperties().isUsingFullScreenWorkAround()) {
                    FullScreenWorkAround.apply(mActivity);
                }

            } else {
                layout = inflater.inflate(R.layout.view_terminal_toolbar_text_input, collection, false);
                final EditText source = layout.findViewById(R.id.terminal_toolbar_text_input);
                final TextView dest = layout.findViewById(R.id.terminal_toolbar_text_input1);

                if (mSavedTextInput != null) {
                    source.setText(mSavedTextInput);
                    mSavedTextInput = null;
                }

                dest.setMovementMethod(LinkMovementMethod.getInstance());
                dest.setHighlightColor(Color.TRANSPARENT);
                dest.setTextColor(Color.RED);
                dest.setVisibility(View.INVISIBLE);

                source.addTextChangedListener(new TextWatcher(){
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (s.length() <= 0) {
                            dest.setText("");
                            dest.setVisibility(View.INVISIBLE);
                            return;
                        }

                        updateSuggestionList(s, source, dest);
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                });

                source.setOnEditorActionListener((v, actionId, event) -> {
                    String textToSend = source.getText().toString();

                    if (handleSpecialCommand(textToSend)
                        || tryToOpenUrl(textToSend)
                        || tryToOpenSuggestionApplication(this.mSuggestedApplication)
                        || sendCommandToTerminal(textToSend)) {
                        source.setText("");
                    }

                    return true;
                });

                source.requestFocus();
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

            if (command.contentEquals("/update")) {
                scanPackages();
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
            if (info == null)
                return false;

            Log.w(getClass().getCanonicalName(), "-- try to open: " + info.processName);
            final PackageManager pm = mActivity.getPackageManager();
            if (pm != null) {
                final Intent intent = pm.getLaunchIntentForPackage(info.processName);
                if (intent != null) {
                    mActivity.startActivity(intent);
                    return true;
                }
                Log.w(getClass().getCanonicalName(), "-- no intent found for: " + info.processName);
            }

            return false;
        }

        private boolean sendCommandToTerminal(String command) {
            TerminalSession session = mActivity.getCurrentSession();

            Log.i(getClass().getCanonicalName(), "-- send text to terminal");
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

        private void scanPackages() {
            Log.i(getClass().getCanonicalName(), "-- scan packages");
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

        private void updateSuggestionList(CharSequence str, EditText source, TextView dest) {
            SpannableStringBuilder hints = new SpannableStringBuilder();

            List<ApplicationInfo> packages = filterPackagesBy(str.toString());

            mSuggestedApplication = null;
            packages.forEach(info -> {
                final PackageManager pm = mActivity.getPackageManager();
                if (pm != null) {
                    CharSequence name = pm.getApplicationLabel(info);
                    SpannableString spannableName = new SpannableString(name);

                    spannableName.setSpan(
                        new ClickableSpan() {
                            @Override
                            public void onClick(@NonNull View widget) {
                                source.setText("");
                                tryToOpenSuggestionApplication(info);
                            }

                            @Override
                            public void updateDrawState(TextPaint ds) {
                                super.updateDrawState(ds);
                                ds.setUnderlineText(false);
                            }
                        }, 0, name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    spannableName.setSpan(new BackgroundColorSpan(Color.YELLOW), 0,
                        name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    if (hints.length() > 0)
                        hints.append(" / ");

                    // pick the first matched application to launch when Enter gets hit
                    if (mSuggestedApplication == null)
                        mSuggestedApplication = info;

                    hints.append(spannableName);
                }
            });

            dest.setText(hints);
            dest.setVisibility(View.VISIBLE);
        }

        /*
         * implements simple regular expression package name filter
         */
        private List<ApplicationInfo> filterPackagesBy(String prefix) {
            List<ApplicationInfo> packages = new ArrayList<>();
            String finalPrefix = prefix.toLowerCase().replaceAll("[\\*$]+", "");

            Log.w(getClass().getCanonicalName(), "-- prefix: '" + finalPrefix + "'");
            mPackages.forEach(info -> {
                final PackageManager pm = mActivity.getPackageManager();
                if (pm != null) {
                    String name = pm.getApplicationLabel(info).toString().toLowerCase();

                    if ((prefix.startsWith("*")) && (prefix.endsWith("$"))) {
                        if (name.endsWith(finalPrefix))
                            packages.add(info);
                        return;
                    }

                    if (prefix.startsWith("*")) {
                        if (name.contains(finalPrefix))
                            packages.add(info);
                        return;
                    }

                    if (prefix.endsWith("$")) {
                        if (name.contentEquals(finalPrefix))
                            packages.add(info);
                        return;
                    }

                    if(name.startsWith(finalPrefix)) {
                        packages.add(info);
                        return;
                    }
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
