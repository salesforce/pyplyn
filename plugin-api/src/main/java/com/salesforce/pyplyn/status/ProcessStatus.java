/*
 * Copyright, 1999-2018, SALESFORCE.com
 * All Rights Reserved
 * Company Confidential
 */
package com.salesforce.pyplyn.status;

/**
 * ProcessStatus
 *
 * @author chris.coraggio
 * @since 11.0.0
 */
public enum ProcessStatus {
        ExtractSuccess,
        ExtractFailure,
        ExtractNoDataReturned,
        LoadSuccess,
        LoadFailure,
        AuthenticationFailure,
        ConfigurationUpdateFailure
}
