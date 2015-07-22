/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Created by Stuart Douglas (sdouglas@macadamian.com) on June 11, 2015.
 * Copyright (c) 2015 Macadamian. All rights reserved.
 */

package com.macadamian.blinkup;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.electricimp.blinkup.BlinkupController;

import org.json.JSONException;
import org.json.JSONObject;

/*****************************************************
 * When the BlinkUpPlugin process completes, it executes the
 * BlinkUpCompleteIntent set in BlinkUpPlugin.java, starting
 * this activity, which requests the setup info from
 * the Electric Imp server, dismisses itself, and
 * sends the info back to the callback when received.
 *****************************************************/
public class BlinkUpCompleteActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getDeviceInfo();

        BlinkUpPluginResult pluginResult = new BlinkUpPluginResult();
        pluginResult.setState(BlinkUpPluginResult.BlinkUpPluginState.Started);
        pluginResult.setStatusCode(BlinkUpPlugin.StatusCodes.GATHERING_INFO.getCode());
        pluginResult.sendResultsToCallback();

        this.finish();
    }

    private void getDeviceInfo() {
        final BlinkupController.TokenStatusCallback tokenStatusCallback= new BlinkupController.TokenStatusCallback() {

            //---------------------------------
            // give connection info to Cordova
            //---------------------------------
            @Override public void onSuccess(JSONObject json) {
                BlinkUpPluginResult successResult = new BlinkUpPluginResult();
                successResult.setState(BlinkUpPluginResult.BlinkUpPluginState.Completed);
                successResult.setStatusCode(BlinkUpPlugin.StatusCodes.DEVICE_CONNECTED.getCode());
                successResult.setDeviceInfoAsJson(json);
                successResult.sendResultsToCallback();

                // cache planID if not development ID (see electricimp.com/docs/manufacturing/planids/)
                try {
                    String planId = json.getString("plan_id");
                    if (!planId.equals(BlinkUpPlugin.developerPlanId)) {
                        SharedPreferences preferences = getSharedPreferences(BlinkUpPlugin.PLAN_ID_CACHE_NAME, MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString(BlinkUpPlugin.PLAN_ID_CACHE_KEY, planId);
                        editor.apply();
                    }
                } catch (JSONException e) {
                    BlinkUpPlugin.sendPluginErrorToCallback(BlinkUpPlugin.ErrorCodes.JSON_ERROR);
                }
            }

            //---------------------------------
            // give error msg to Cordova
            //---------------------------------
            @Override public void onError(String errorMsg) {
                // can't use "sendPluginErrorToCallback" since this is an SDK error
                BlinkUpPluginResult sdkErrorResult = new BlinkUpPluginResult();
                sdkErrorResult.setState(BlinkUpPluginResult.BlinkUpPluginState.Error);
                sdkErrorResult.setBlinkUpError(errorMsg);
                sdkErrorResult.sendResultsToCallback();
            }

            //---------------------------------
            // give timeout message to Cordova
            //---------------------------------
            @Override public void onTimeout() {
                BlinkUpPlugin.sendPluginErrorToCallback(BlinkUpPlugin.ErrorCodes.PROCESS_TIMED_OUT);
            }
        };

        // request the device info from the server
        BlinkupController.getInstance().getTokenStatus(tokenStatusCallback, BlinkUpPlugin.timeoutMs);
    }
}