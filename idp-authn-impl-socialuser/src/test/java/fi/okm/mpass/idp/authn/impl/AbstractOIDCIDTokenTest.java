/*
 * The MIT License
 * Copyright (c) 2015 CSC - IT Center for Science, http://www.csc.fi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package fi.okm.mpass.idp.authn.impl;

import java.util.Date;

import org.mockito.Mockito;
import org.springframework.webflow.execution.Event;
import org.testng.annotations.Test;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;

import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.impl.PopulateAuthenticationContextTest;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionTestingSupport;

/**
 * Abstract test case sharing tests for OIDC token validation in {@link SocialUserOpenIdConnectContext}.
 */
public abstract class AbstractOIDCIDTokenTest extends PopulateAuthenticationContextTest {

    /**
     * Returns the action to be tested.
     * @return
     */
    protected abstract AbstractProfileAction<?, ?> getAction();

    /**
     * Runs action without {@link SocialUserOpenIdConnectContext}.
     */
    @Test public void testNoContext() throws Exception {
        final AbstractProfileAction<?, ?> action = getAction();
        action.initialize();
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.NO_CREDENTIALS);
    }
    
    /**
     * Helper method for building {@link OIDCTokenResponse} with a given expiration time.
     * @param expirationTime The expiration time.
     * @return
     */
    protected OIDCTokenResponse getOidcTokenResponse(final Date expirationTime) {
        final JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("mockUser")
                .expirationTime(expirationTime)
                .claim("http://example.org/mock", true)
                .build();        
        final PlainJWT plainJwt = new PlainJWT(claimsSet);
        final AccessToken accessToken = new BearerAccessToken();
        final RefreshToken refreshToken = new RefreshToken();
        final OIDCTokens oidcTokens = new OIDCTokens(plainJwt, accessToken, refreshToken);
        final OIDCTokenResponse oidcTokenResponse = new OIDCTokenResponse(oidcTokens);
        return oidcTokenResponse;
    }
    
    /**
     * Runs action with unparseable OIDC token.
     */
    @SuppressWarnings("unchecked")
    @Test public void testUnparseable() throws Exception {
        final AccessToken accessToken = new BearerAccessToken();
        final RefreshToken refreshToken = new RefreshToken();
        final JWT jwt = Mockito.mock(JWT.class);
        Mockito.when(jwt.getJWTClaimsSet()).thenThrow(java.text.ParseException.class);
        
        final OIDCTokens oidcTokens = new OIDCTokens(jwt, accessToken, refreshToken);
        final OIDCTokenResponse oidcTokenResponse = new OIDCTokenResponse(oidcTokens);
        final AbstractProfileAction<?, ?> action = getAction();
        action.initialize();
        final SocialUserOpenIdConnectContext suCtx = new SocialUserOpenIdConnectContext();
        suCtx.setOidcTokenResponse(oidcTokenResponse);
        prc.getSubcontext(AuthenticationContext.class, false).addSubcontext(suCtx);
        final Event event = action.execute(src);
        ActionTestingSupport.assertEvent(event, AuthnEventIds.NO_CREDENTIALS);
    }
}
