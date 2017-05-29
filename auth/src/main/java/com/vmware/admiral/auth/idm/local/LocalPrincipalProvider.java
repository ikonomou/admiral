/*
 * Copyright (c) 2017 VMware, Inc. All Rights Reserved.
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with separate copyright notices
 * and license terms. Your use of these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package com.vmware.admiral.auth.idm.local;

import static com.vmware.admiral.common.util.AssertUtil.assertNotNullOrEmpty;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.vmware.admiral.auth.idm.Principal;
import com.vmware.admiral.auth.idm.Principal.PrincipalType;
import com.vmware.admiral.auth.idm.PrincipalProvider;
import com.vmware.admiral.auth.idm.local.LocalPrincipalService.LocalPrincipalState;
import com.vmware.admiral.auth.idm.local.LocalPrincipalService.LocalPrincipalType;
import com.vmware.xenon.common.DeferredResult;
import com.vmware.xenon.common.Operation;
import com.vmware.xenon.common.Service;
import com.vmware.xenon.common.ServiceDocumentQueryResult;
import com.vmware.xenon.common.ServiceHost;
import com.vmware.xenon.common.UriUtils;
import com.vmware.xenon.common.Utils;

public class LocalPrincipalProvider implements PrincipalProvider {
    private static final String EXPAND_QUERY_KEY = "expand";
    private static final String FILTER_QUERY_KEY = "$filter";
    private static final String EMAIL_FILTER_QUERY_FORMAT = "email eq '*%s*'";

    private ServiceHost host;

    public void setServiceHost(ServiceHost host) {
        this.host = host;
    }

    @Override
    public DeferredResult<Principal> getPrincipal(String principalId) {
        assertNotNullOrEmpty(principalId, "principalId");
        URI uri = buildLocalPrincipalStateSelfLink(host, principalId);

        Operation get = Operation.createGet(uri)
                .setReferer(host.getUri());

        return host.sendWithDeferredResult(get, LocalPrincipalState.class)
                .thenApply(this::fromLocalPrincipalToPrincipal);
    }

    @Override
    public DeferredResult<List<Principal>> getPrincipals(String criteria) {
        String filterQuery = String.format(EMAIL_FILTER_QUERY_FORMAT, criteria);

        URI uri = UriUtils.buildUri(host, LocalPrincipalFactoryService.SELF_LINK);
        uri = UriUtils.extendUriWithQuery(uri, EXPAND_QUERY_KEY, Boolean.TRUE.toString());
        uri = UriUtils.extendUriWithQuery(uri, FILTER_QUERY_KEY, filterQuery);

        Operation get = Operation.createGet(uri)
                .setReferer(host.getUri());

        return host.sendWithDeferredResult(get, ServiceDocumentQueryResult.class)
                .thenApply(this::fromQueryResultToPrincipalList);
    }

    public DeferredResult<Principal> createPrincipal(Principal principal) {
        LocalPrincipalState stateToCreate = fromPrincipalToLocalPrincipal(principal);

        URI uri = UriUtils.buildUri(host, LocalPrincipalFactoryService.SELF_LINK);

        Operation post = Operation.createPost(uri)
                .addPragmaDirective(Operation.PRAGMA_DIRECTIVE_FORCE_INDEX_UPDATE)
                .setBody(stateToCreate)
                .setReferer(host.getUri());

        return host.sendWithDeferredResult(post, LocalPrincipalState.class)
                .thenApply(this::fromLocalPrincipalToPrincipal);
    }

    public DeferredResult<Principal> updatePrincipal(Principal principal) {
        LocalPrincipalState stateToPatch = fromPrincipalToLocalPrincipal(principal);

        URI uri = buildLocalPrincipalStateSelfLink(host, stateToPatch.id);

        Operation post = Operation.createPatch(uri)
                .addPragmaDirective(Operation.PRAGMA_DIRECTIVE_FORCE_INDEX_UPDATE)
                .setBody(stateToPatch)
                .setReferer(host.getUri());

        return host.sendWithDeferredResult(post, LocalPrincipalState.class)
                .thenApply(this::fromLocalPrincipalToPrincipal);
    }

    public DeferredResult<Principal> deletePrincipal(String principalId) {
        URI uri = buildLocalPrincipalStateSelfLink(host, principalId);

        Operation delete = Operation.createDelete(uri)
                .setReferer(host.getUri());

        return host.sendWithDeferredResult(delete, LocalPrincipalState.class)
                .thenApply(this::fromLocalPrincipalToPrincipal);
    }


    private Principal fromLocalPrincipalToPrincipal(LocalPrincipalState state) {

        if (state == null) {
            return null;
        }

        Principal principal = new Principal();
        principal.email = state.email;
        principal.name = state.name;
        principal.id = state.id;
        principal.password = state.password;
        principal.type = PrincipalType.valueOf(state.type.name());

        if (state.type == LocalPrincipalType.GROUP && state.groupMembersLinks != null && !state
                .groupMembersLinks.isEmpty()) {
            principal.groupMembers = state.groupMembersLinks.stream()
                    .map(Service::getId)
                    .collect(Collectors.toList());
        }

        return principal;
    }

    private List<Principal> fromQueryResultToPrincipalList(ServiceDocumentQueryResult queryResult) {

        List<Principal> principals = new ArrayList<>();

        for (Object serializedState : queryResult.documents.values()) {
            LocalPrincipalState state = Utils.fromJson(serializedState, LocalPrincipalState.class);
            Principal principal = fromLocalPrincipalToPrincipal(state);
            principals.add(principal);
        }

        return principals;

    }

    private LocalPrincipalState fromPrincipalToLocalPrincipal(Principal principal) {
        if (principal == null) {
            return null;
        }

        LocalPrincipalState state = new LocalPrincipalState();
        state.email = principal.email;
        state.id = principal.id;
        state.password = principal.password;
        state.name = principal.name;
        state.type = LocalPrincipalType.valueOf(principal.type.name());

        if (principal.type == PrincipalType.GROUP && principal.groupMembers != null
                && !principal.groupMembers.isEmpty()) {
            state.groupMembersLinks = principal.groupMembers.stream()
                    .map(m -> LocalPrincipalFactoryService.SELF_LINK + "/" + m)
                    .collect(Collectors.toList());
        }

        return state;
    }

    private static URI buildLocalPrincipalStateSelfLink(ServiceHost host, String id) {
        return UriUtils.buildUri(host, LocalPrincipalFactoryService.SELF_LINK + "/" + id);
    }
}
