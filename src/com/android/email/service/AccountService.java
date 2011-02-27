/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.email.service;

import com.android.email.AccountBackupRestore;
import com.android.email.Email;
import com.android.email.ExchangeUtils;
import com.android.email.NotificationController;
import com.android.email.ResourceHelper;
import com.android.email.VendorPolicyLoader;
import com.android.emailcommon.Configuration;
import com.android.emailcommon.Device;
import com.android.emailcommon.service.IAccountService;
import com.android.emailcommon.utility.Utility;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import java.io.IOException;

public class AccountService extends Service {

    // Save context
    private Context mContext;

    private final IAccountService.Stub mBinder = new IAccountService.Stub() {

        @Override
        public void notifyLoginFailed(long accountId) throws RemoteException {
            NotificationController.getInstance(mContext).showLoginFailedNotification(accountId);
        }

        @Override
        public void notifyLoginSucceeded(long accountId) throws RemoteException {
            NotificationController.getInstance(mContext).cancelLoginFailedNotification(accountId);
        }

        @Override
        public void notifyNewMessages(long accountId) throws RemoteException {
            MailService.actionNotifyNewMessages(mContext, accountId);
        }

        @Override
        public void restoreAccountsIfNeeded() throws RemoteException {
            AccountBackupRestore.restoreAccountsIfNeeded(mContext);
        }

        @Override
        public void accountDeleted() throws RemoteException {
            MailService.accountDeleted(mContext);
        }

        @Override
        public int getAccountColor(long accountId) throws RemoteException {
            return ResourceHelper.getInstance(mContext).getAccountColor(accountId);
        }

        @Override
        public Bundle getConfigurationData(String accountType) throws RemoteException {
            Bundle bundle = new Bundle();
            bundle.putBoolean(Configuration.EXCHANGE_CONFIGURATION_USE_ALTERNATE_STRINGS,
                    VendorPolicyLoader.getInstance(mContext).useAlternateExchangeStrings());
            return bundle;
        }

        @Override
        public String getDeviceId() throws RemoteException {
            try {
                Utility.runAsync(new Runnable() {
                    @Override
                    public void run() {
                        // Make sure the service is properly running (re: lifecycle)
                        ExchangeUtils.startExchangeService(mContext);
                        // Send current logging flags
                        Email.updateLoggingFlags(mContext);
                    }});
                return Device.getDeviceId(mContext);
            } catch (IOException e) {
                return null;
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        if (mContext == null) {
            mContext = this;
        }
        // Make sure we have a valid deviceId (just retrieves a static String except first time)
        try {
            Device.getDeviceId(this);
        } catch (IOException e) {
        }
        return mBinder;
    }
}