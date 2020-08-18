package org.janusgraph.diskstorage.es.rest.util.kerberos;

import org.apache.http.Header;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.KerberosCredentials;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.config.Lookup;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.impl.auth.SPNegoSchemeFactory;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClientBuilder;
import org.ietf.jgss.Oid;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.GSSException;

import javax.security.auth.Subject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;

/**
 * author shekhar.bansal
 **/
public class KerberosAuthHttpClientConfigCallback  implements RestClientBuilder.HttpClientConfigCallback {

    private static final Logger logger = LoggerFactory.getLogger(KerberosAuthHttpClientConfigCallback.class);
    private String keytabFilePath;
    private String principal;

    public static final Oid SPNEGO;
    public static final Oid KRB5MECH;
    private final Oid[] desiredMechs;

    static {
        Oid spnegoTmp = null;
        Oid krbTmp = null;
        try {
            spnegoTmp = new Oid("1.3.6.1.5.5.2");
            krbTmp = new Oid("1.2.840.113554.1.2.2");
        } catch (final GSSException e) {

        }
        SPNEGO = spnegoTmp;
        KRB5MECH = krbTmp;
    }

    public KerberosAuthHttpClientConfigCallback(String principal, String keytabFilePath){
        this.principal = principal;
        this.keytabFilePath = keytabFilePath;
        desiredMechs = new Oid[] {SPNEGO, KRB5MECH};
    }

    @Override
    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpAsyncClientBuilder) {
        try {

            if(false){
                new UserGroupInformationWrapper().getSubject(principal, keytabFilePath);
            }

            Lookup<AuthSchemeProvider> authSchemeProviderLookup = RegistryBuilder.<AuthSchemeProvider>create()
                .register(AuthSchemes.SPNEGO, new SPNegoSchemeFactory()).build();

            Subject subject = new UserGroupInformationWrapper().getSubjectFromKeyTabAndPrincipal(principal, keytabFilePath);
            GSSManager manager = GSSManager.getInstance();
            GSSName clientName = manager.createName(principal, GSSName.NT_USER_NAME);
            AccessControlContext accessControlContext = AccessController.getContext();

            Subject.doAsPrivileged(subject, new PrivilegedExceptionAction<GSSCredential>() {
                @Override
                public GSSCredential run() throws Exception {
                    logger.info("dummy logger");
                    return null;
                }
            }, accessControlContext);

            GSSCredential clientCreds = Subject.doAsPrivileged(subject, new PrivilegedExceptionAction<GSSCredential>() {
                @Override
                public GSSCredential run() throws Exception {
                    return manager.createCredential(clientName,
                        8*3600, SPNEGO, GSSCredential.INITIATE_ONLY);
                }
            }, accessControlContext);

            KerberosCredentialsProvider credentialsProvider = new KerberosCredentialsProvider();
            credentialsProvider.setCredentials(
                new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM, AuthSchemes.SPNEGO),
                new KerberosCredentials(clientCreds));

            httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            httpAsyncClientBuilder.setDefaultAuthSchemeRegistry(authSchemeProviderLookup);

        } catch (GSSException | PrivilegedActionException | KerbrosLoginException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return httpAsyncClientBuilder;
    }


}
