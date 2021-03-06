package com.mapzen.open.login;

import android.content.Intent;

import com.mapzen.open.MapController;
import com.mapzen.open.R;
import com.mapzen.open.TestMapzenApplication;
import com.mapzen.android.lost.LocationClient;
import com.mapzen.open.activity.BaseActivity;
import com.mapzen.open.support.MapzenTestRunner;

import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;
import org.scribe.model.Token;

import static com.mapzen.open.util.MixpanelHelper.Event.LOGIN_BUTTON_CLICK;
import static com.mapzen.open.util.MixpanelHelper.Event.LOGIN_PAGE;
import static org.fest.assertions.api.ANDROID.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;
import android.net.Uri;

import javax.inject.Inject;

import static com.mapzen.open.support.TestHelper.initLoginActivity;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.robolectric.Robolectric.buildActivity;
import static org.robolectric.Robolectric.getShadowApplication;
import static org.robolectric.Robolectric.shadowOf;
import static org.mockito.Mockito.mock;

@Config(emulateSdk = 18)
@RunWith(MapzenTestRunner.class)
public class LoginActivityTest {
    private LoginActivity activity;
    @Inject LocationClient locationClient;
    @Inject MapController mapController;
    @Inject MixpanelAPI mixpanelAPI;

    @Before
    public void setUp() throws Exception {
        ((TestMapzenApplication) Robolectric.application).inject(this);
        activity = initLoginActivity();
        mapController.setActivity(new BaseActivity());
    }

    @Test
    public void shouldSendMixpanelEventsForPages() throws Exception {
        for (int i = 0; i < 6; i++) {
            activity.onPageSelected(i);
            verify(mixpanelAPI).track(eq(LOGIN_PAGE + String.valueOf(i)), any(JSONObject.class));
        }
    }

    @Test
    public void doLogin_shouldRecordMixpanelEvent() throws Exception {
        activity.doLogin();
        verify(mixpanelAPI).track(eq(LOGIN_BUTTON_CLICK), any(JSONObject.class));
    }

    @Test
    public void shouldNotBeNull() throws Exception {
        assertThat(activity).isNotNull();
    }

    @Test
    public void shouldNotHaveActionBar() throws Exception {
        assertThat(activity.getActionBar()).isNull();
    }

    @Test
    public void shouldOpenLoginPage() {
        Token testToken = new Token("Bogus_key", "Bogus_verfier");
        activity.openLoginPage(testToken);
        String urlOpened = shadowOf(activity).getNextStartedActivity().getDataString();
        assertThat(urlOpened)
                .contains("https://www.openstreetmap.org/oauth/authorize?oauth_token=");
    }

    @Test
    public void shouldNotStoreLoginPageInHistory() {
        Token testToken = new Token("Bogus_key", "Bogus_verfier");
        activity.openLoginPage(testToken);
        assertThat(shadowOf(activity).getNextStartedActivity().getFlags()
                & Intent.FLAG_ACTIVITY_NO_HISTORY).isEqualTo(Intent.FLAG_ACTIVITY_NO_HISTORY);
    }

    @Test
    public void shouldStartBaseActivityOnTokenReturn() {
        Uri.Builder oauthTokenBuilder = new Uri.Builder();
        oauthTokenBuilder.appendQueryParameter(LoginActivity.OSM_VERIFIER_KEY, "Bogus verifier");
        Uri oauthToken = oauthTokenBuilder.build();
        Intent intent = new Intent();
        intent.setData(oauthToken);
        activity.onNewIntent(intent);
        String componentOpened = shadowOf(activity).getNextStartedActivity()
                .getComponent().toString();
        assertThat(componentOpened)
                .isEqualTo("ComponentInfo{com.mapzen.open/com.mapzen.open.activity.BaseActivity}");
    }

    @Test
    public void shouldForwardGeoIntentDataToBaseActivityAfterLogin() throws Exception {
        String data = "http://maps.example.com/";
        Intent geoIntent = new Intent();
        geoIntent.setData(Uri.parse(data));
        LoginActivity activity = buildActivity(LoginActivity.class)
                .withIntent(geoIntent)
                .create()
                .start()
                .resume()
                .visible()
                .get();

        Uri.Builder oauthTokenBuilder = new Uri.Builder();
        oauthTokenBuilder.appendQueryParameter(LoginActivity.OSM_VERIFIER_KEY, "Bogus verifier");
        Uri oauthToken = oauthTokenBuilder.build();
        Intent oauthIntent = new Intent();
        oauthIntent.setData(oauthToken);
        activity.onNewIntent(oauthIntent);
        assertThat(getShadowApplication().getNextStartedActivity()).hasData(data);
    }

    @Test
    public void shouldShowMapOnLoginError() {
        activity.unableToLogInAction();
        String activityStarted = shadowOf(activity).getNextStartedActivity()
                .getComponent().toString();
        assertThat(activityStarted)
                .isEqualTo("ComponentInfo{com.mapzen.open/com.mapzen.open.activity.BaseActivity}");
    }

    @Test
    public void shouldShowToastOnLoginError() {
        activity.unableToLogInAction();
        boolean showedToast = ShadowToast.showedToast(activity.getString(R.string.login_error));
        assertThat(showedToast).isTrue();
    }

    @Test
    public void shouldNotDisplayLocationError() {
        mapController.setActivity(new BaseActivity());
        LocationClient mock = mock(LocationClient.class);
        Mockito.when(mock.getLastLocation()).thenReturn(null);
        assertThat(ShadowToast.getTextOfLatestToast()).isNull();
    }

    @Test
    public void onResume_shouldConnectLocationClient() {
        assertThat(locationClient.isConnected()).isTrue();
    }

    @Test
    public void onPause_shouldDisConnectLocationClient() {
        activity.onPause();
        assertThat(locationClient.isConnected()).isFalse();
    }

    @Test
    public void loginFlowShouldHaveViewPagerWithCountFour() throws Exception {
        assertThat(activity.viewPager.getAdapter()).hasCount(4);
    }
}
