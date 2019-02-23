/*
 * Copyright (c) 2011, 2014 Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.chesapeake.technology.excel;

import com.chesapeake.technology.JiraRestClient;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import net.rcarz.jiraclient.BasicCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Generates an excel report providing summaries of elements across JIRA as well as metrics across an individual
 * developer's work history.
 *
 * @since 1.0.0
 */
public class JiraReportGenerator
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static void main(String[] args) throws Exception
    {
        if (args.length > 0)
        {
            String username = System.getenv(("NEXUS_USERNAME"));
            String password = System.getenv(("NEXUS_PASSWORD"));

            Config config = ConfigFactory.load(args[0]);
            String baseUrl = config.getString("jira.baseUrl");
            boolean includeChangeLogs = config.getBoolean("jira.importFullHistory");

            JiraRestClient requestClient = new JiraRestClient(baseUrl, new BasicCredentials(username, password), includeChangeLogs);


            requestClient.addIssueListener(new HeadlessReportGenerator(config));
            requestClient.loadJiraIssues(config);
        } else
        {
            logger.warn("Failed to build jira report: please specify a configuration file");
        }
    }
}
