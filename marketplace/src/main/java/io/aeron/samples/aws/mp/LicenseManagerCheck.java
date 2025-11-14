/*
 * Copyright 2023 Adaptive Financial Consulting
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.aeron.samples.aws.mp;

import software.amazon.awssdk.services.licensemanager.LicenseManagerClient;
import software.amazon.awssdk.services.licensemanager.model.CheckoutType;
import software.amazon.awssdk.services.licensemanager.model.EntitlementData;
import software.amazon.awssdk.services.licensemanager.model.EntitlementDataUnit;

import java.util.UUID;

/**
 * Utility class for AWS Marketplace verification.
 */
public class LicenseManagerCheck
{
    private static final String PRODUCT_SKU = "prod-tx5dfocxoxi44";
    private static final String ISSUER = "709825985650";

    private enum Outcome
    {
        PremiumTierEnabled(0),
        NoEntitlements(1),
        Failure(2);

        private final int exitCode;

        Outcome(final int exitCode)
        {
            this.exitCode = exitCode;
        }

        void quit()
        {
            System.exit(exitCode);
        }
    }

    /**
     * Entry point.
     * @param args Command line arguments.
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public static void main(final String[] args)
    {
        final var outcome = verifyLicense();
        outcome.quit();
    }

    static Outcome verifyLicense()
    {
        try (var cli = LicenseManagerClient.builder().build())
        {
            final var response = cli
                .checkoutLicense(req -> req
                .productSKU(PRODUCT_SKU)
                .checkoutType(CheckoutType.PROVISIONAL)
                .keyFingerprint("aws:%s:AWS/Marketplace:issuer-fingerprint".formatted(ISSUER))
                .entitlements(EntitlementData.builder().name("ReadOnly").unit(EntitlementDataUnit.NONE).build())
                .clientToken(UUID.randomUUID().toString())
                .build()
            );
            if (response.hasEntitlementsAllowed() &&
                !response.entitlementsAllowed().isEmpty() &&
                response.entitlementsAllowed().stream().anyMatch(LicenseManagerCheck::isPremiumTier)
            )
            {
                return Outcome.PremiumTierEnabled;
            }
            return Outcome.NoEntitlements;
        }
        catch (final Exception e)
        {
            System.err.println("Failed to contact License Manager: " + e.getMessage());
            return Outcome.Failure;
        }
    }

    static boolean isPremiumTier(final EntitlementData value)
    {
        return "AeronPremium".equals(value.name());
    }
}
