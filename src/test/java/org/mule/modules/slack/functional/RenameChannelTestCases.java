/**
 * (c) 2003-2015 MuleSoft, Inc. The software in this package is published under the terms of the CPAL v1.0 license,
 * a copy of which has been included with this distribution in the LICENSE.md file.
 */
package org.mule.modules.slack.functional;

import org.junit.Test;
import org.mule.modules.slack.runner.AbstractSlackTestCase;

import static org.junit.Assert.assertEquals;

/**
 * Created by estebanwasinger on 8/8/15.
 */
public class  RenameChannelTestCases extends AbstractSlackTestCase {

    @Test
    public void testRenameChannel() throws InterruptedException {
        Thread.sleep(2000);
        String actualDate = getDateString();
        getConnector().renameChannel(CHANNEL_RENAMING, actualDate);
        assertEquals(actualDate,getConnector().getChannelInfo(CHANNEL_RENAMING).getName());
    }

}
