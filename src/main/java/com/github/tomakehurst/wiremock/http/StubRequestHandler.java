/*
 * Copyright (C) 2011 Thomas Akehurst
 *
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
 */
package com.github.tomakehurst.wiremock.http;

import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.core.StubServer;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.PostServeAction;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.RequestJournal;

import java.util.Map;

import static com.github.tomakehurst.wiremock.common.LocalNotifier.notifier;

public class StubRequestHandler extends AbstractRequestHandler {
	
	private final StubServer stubServer;
    private final Admin admin;
    private final Map<String, PostServeAction> postServeActions;
    private final RequestJournal requestJournal;

	public StubRequestHandler(StubServer stubServer,
                              ResponseRenderer responseRenderer,
                              Admin admin,
                              Map<String, PostServeAction> postServeActions,
                              RequestJournal requestJournal) {
		super(responseRenderer);
		this.stubServer = stubServer;
        this.admin = admin;
        this.postServeActions = postServeActions;
        this.requestJournal = requestJournal;
    }

	@Override
	public ServeEvent handleRequest(Request request) {
		return stubServer.serveStubFor(request);
	}

	@Override
	protected boolean logRequests() {
		return true;
	}

    @Override
    protected void beforeResponseSent(ServeEvent serveEvent, Response response) {
        requestJournal.requestReceived(serveEvent.complete(response));
    }

    @Override
    protected void afterResponseSent(ServeEvent serveEvent, Response response) {
        for (PostServeAction postServeAction: postServeActions.values()) {
            postServeAction.doGlobalAction(serveEvent, admin);
        }

        Map<String, Parameters> postServeActionRefs = serveEvent.getPostServeActions();
        for (Map.Entry<String, Parameters> postServeActionEntry: postServeActionRefs.entrySet()) {
            PostServeAction action = postServeActions.get(postServeActionEntry.getKey());
            if (action != null) {
                Parameters parameters = postServeActionEntry.getValue();
                action.doAction(serveEvent, admin, parameters);
            } else {
                notifier().error("No extension was found named \"" + postServeActionEntry.getKey() + "\"");
            }
        }
    }
}
