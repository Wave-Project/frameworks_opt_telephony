/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.internal.telephony.ims;

import junit.framework.AssertionFailedError;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.RemoteException;
import android.telephony.SubscriptionManager;
import android.telephony.ims.RcsContactUceCapability;
import android.telephony.ims.aidl.IImsCapabilityCallback;
import android.telephony.ims.aidl.IImsRcsFeature;
import android.telephony.ims.aidl.IRcsFeatureListener;
import android.telephony.ims.feature.CapabilityChangeRequest;
import android.telephony.ims.feature.ImsFeature;
import android.test.suitebuilder.annotation.SmallTest;

import com.android.ims.RcsFeatureConnection;
import com.android.ims.internal.IImsServiceFeatureCallback;
import com.android.internal.telephony.TelephonyTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.concurrent.Executor;

public class RcsFeatureConnectionTest extends TelephonyTest {

    private Executor mSimpleExecutor = new Executor() {
        @Override
        public void execute(Runnable r) {
            r.run();
        }
    };

    private IImsRcsFeature mTestImsRcsFeatureBinder = new IImsRcsFeature.Stub() {
        @Override
        public void setListener(IRcsFeatureListener listener) {
        }

        @Override
        public int queryCapabilityStatus() {
            return 1;
        }

        @Override
        public int getFeatureState() {
            return ImsFeature.STATE_READY;
        }

        @Override
        public void addCapabilityCallback(IImsCapabilityCallback c) {
        }

        @Override
        public void removeCapabilityCallback(IImsCapabilityCallback c) {
        }

        @Override
        public void changeCapabilitiesConfiguration(CapabilityChangeRequest r,
                IImsCapabilityCallback c) {
        }

        @Override
        public void queryCapabilityConfiguration(int capability, int radioTech,
                IImsCapabilityCallback c) {
        }

        @Override
        public void requestCapabilities(List<Uri> uris, int operationToken) {
        }

        @Override
        public void updateCapabilities(RcsContactUceCapability capabilities,
                int operationToken) {
        }

        @Override
        public void sendCapabilityRequest(Uri contactUri,
                RcsContactUceCapability capabilities, int operationToken) {
        }

        @Override
        public void respondToCapabilityRequest(String contactUri,
                RcsContactUceCapability ownCapabilities, int operationToken) {
        }

        @Override
        public void respondToCapabilityRequestWithError(Uri contactUri, int code,
                String reason, int operationToken) {
        }
    };

    private int mPhoneId;
    private SubscriptionManager mSubscriptionManager;
    private RcsFeatureConnection mRcsFeatureConnection;

    @Mock
    RcsFeatureConnection.RcsFeatureManagerProxy mRcsFeatureManagerProxy;
    @Mock
    RcsFeatureConnection.IRcsFeatureUpdate mCallback;

    @Before
    public void setUp() throws Exception {
        super.setUp("RcsFeatureConnectionTest");
        mPhoneId = mPhone.getPhoneId();

        mSubscriptionManager = (SubscriptionManager) mContext.getSystemService(
                Context.TELEPHONY_SUBSCRIPTION_SERVICE);

        doReturn(null).when(mContext).getMainLooper();
        doReturn(true).when(mPackageManager).hasSystemFeature(PackageManager.FEATURE_TELEPHONY_IMS);

        mRcsFeatureConnection = RcsFeatureConnection.create(mContext, mPhoneId, null);
        mRcsFeatureConnection.mExecutor = mSimpleExecutor;
        mRcsFeatureConnection.setBinder(mTestImsRcsFeatureBinder.asBinder());
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test that RcsFeatureConnection is ready when RCS UCE is supported by device and carrier.
     */
    @Test
    @SmallTest
    public void testServiceIsReady() {
        // RCS UCE is supported by carrier
        setRcsUceIsSupportedByCarrier(true);
        try {
            mRcsFeatureConnection.checkServiceIsReady();
        } catch (RemoteException e) {
            throw new AssertionFailedError("Exception in checkServiceIsReady: " + e);
        }
    }

    /**
     * Test that RcsFeatureConnection is not ready when RCS UCE is not supported carrier.
     */
    @Test
    @SmallTest
    public void testServiceIsNotSupportedByCarrier() {
        // RCS UCE feature is NOT supported by carrier
        setRcsUceIsSupportedByCarrier(false);
        try {
            mRcsFeatureConnection.checkServiceIsReady();
            throw new AssertionFailedError("Exception in testServiceIsNotSupportedByCarrier");
        } catch (RemoteException e) {
            //expected result
        }
    }

    private void setRcsUceIsSupportedByCarrier(boolean isSupported) {
        when(mRcsFeatureManagerProxy.isRcsUceSupportedByCarrier(mContext, 0))
                .thenReturn(isSupported);
        RcsFeatureConnection.setRcsFeatureManagerProxy(mRcsFeatureManagerProxy);
    }

    /**
     * Test that service is not ready after IMS feature is removed.
     */
    @Test
    @SmallTest
    public void testImsFeatureRemoved() {
        setRcsUceIsSupportedByCarrier(true);
        IImsServiceFeatureCallback imsServiceCallback = mRcsFeatureConnection.getListener();
        try {
            imsServiceCallback.imsFeatureRemoved(0, ImsFeature.FEATURE_RCS);
            mRcsFeatureConnection.checkServiceIsReady();
            throw new AssertionFailedError("testImsFeatureRemoved");
        } catch (RemoteException e) {
            //expected result
        }
    }

    /**
     * Test that service is not ready after the status of IMS feature is unavailable.
     */
    @Test
    @SmallTest
    public void testImsStatusIsUnavailable() {
        setRcsUceIsSupportedByCarrier(true);
        IImsServiceFeatureCallback imsServiceCallback = mRcsFeatureConnection.getListener();
        try {
            imsServiceCallback.imsStatusChanged(0, ImsFeature.FEATURE_RCS,
                    ImsFeature.STATE_UNAVAILABLE);
            mRcsFeatureConnection.checkServiceIsReady();
            throw new AssertionFailedError("testImsStatusIsUnavailable");
        } catch (RemoteException e) {
            //expected result
        }
    }

    /**
     * Test that service is ready when the status is unavailable on different slot.
     */
    @Test
    @SmallTest
    public void testImsStatusUnavailableOnDifferentSlot() {
        setRcsUceIsSupportedByCarrier(true);
        IImsServiceFeatureCallback imsServiceCallback = mRcsFeatureConnection.getListener();
        try {
            imsServiceCallback.imsFeatureRemoved(1, ImsFeature.FEATURE_RCS);
            mRcsFeatureConnection.checkServiceIsReady();
        } catch (RemoteException e) {
            throw new AssertionFailedError("testImsStatusUnavailableOnDifferentSlot: " + e);
        }
    }

    /**
     * Test that service is ready when the status is unavailable on different ImsFeature.
     */
    @Test
    @SmallTest
    public void testImsStatusUnavailableOnDifferentFeature() {
        setRcsUceIsSupportedByCarrier(true);
        IImsServiceFeatureCallback imsServiceCallback = mRcsFeatureConnection.getListener();
        try {
            imsServiceCallback.imsFeatureRemoved(1, ImsFeature.FEATURE_MMTEL);
            mRcsFeatureConnection.checkServiceIsReady();
        } catch (RemoteException e) {
            throw new AssertionFailedError("testImsStatusUnavailableOnDifferentFeature: " + e);
        }
    }

    @Test
    @SmallTest
    public void testRetrieveFeatureState() {
        assertNotNull(mRcsFeatureConnection.retrieveFeatureState());
    }

    @Test
    @SmallTest
    public void testFeatureStatusCallback() {
        mRcsFeatureConnection.setStatusCallback(mCallback);

        mRcsFeatureConnection.handleImsFeatureCreatedCallback(mPhoneId, ImsFeature.FEATURE_RCS);
        verify(mCallback).notifyFeatureCreated();

        mRcsFeatureConnection.handleImsFeatureRemovedCallback(mPhoneId, ImsFeature.FEATURE_RCS);
        verify(mCallback).notifyUnavailable();

        mRcsFeatureConnection.handleImsStatusChangedCallback(mPhoneId, ImsFeature.FEATURE_RCS,
                ImsFeature.STATE_READY);
        verify(mCallback).notifyStateChanged();
    }
}
